package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File

class GalleryScreen(private var index: Int = 0) : AbstractNicephoreScreen(TITLE), FilterListener {
    private var screenshots: List<File> = emptyList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    private var columns: Int = 4
    private var rows: Int = 3
    private val imagesToDisplay: Int get() = rows * columns

    private fun computeGrid() {
        val configColumns = NicephoreConfig.Client.getGalleryColumns()
        val availableWidth = this.width - 2 * PADDING
        val availableHeight = this.height - TOOLBAR_HEIGHT - BOTTOM_BAR_HEIGHT - PADDING

        columns = if (configColumns in 2..6) {
            configColumns
        } else {
            (availableWidth / (TARGET_THUMBNAIL_WIDTH + PADDING)).coerceIn(2, 6)
        }

        val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
        val imageHeight = (imageWidth / aspectRatio).toInt()
        val slotHeight = imageHeight + BUTTON_HEIGHT + PADDING

        rows = ((availableHeight) / slotHeight).coerceIn(1, 4)
    }

    private fun getNumberOfPages(): Long {
        return kotlin.math.ceil(Util.getNumberOfFiles(SCREENSHOTS_DIR) / imagesToDisplay.toDouble()).toLong()
    }

    override fun init() {
        super.init()

        computeGrid()
        screenshots = Util.getBatchOfFiles((imagesToDisplay.toLong() * index), imagesToDisplay.toLong(), SCREENSHOTS_DIR)
        index = clampIndex(index, getNumberOfPages().toInt().coerceAtLeast(1))
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[0]) else DEFAULT_ASPECT_RATIO

        if (screenshots.isNotEmpty()) {
            loader.setOnLoadComplete { refreshWidgets() }
            loader.loadBatch(screenshots, "gallery")
        }

        refreshWidgets()
    }

    override fun buildWidgets() {
        addToolbarButtons { cycleFilter() }

        if (screenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns

            screenshots.forEachIndexed { imageIndex, file ->
                val col = imageIndex % columns
                val row = imageIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * ((imageWidth / aspectRatio).toInt() + BUTTON_HEIGHT + PADDING)

                val name = file.name
                val text = Component.literal(StringUtils.abbreviate(name, imageWidth / 6))

                this.addRenderableWidget(
                    Button.builder(text) { openScreenshotScreen(imageIndex) }
                        .bounds(x, y + (imageWidth / aspectRatio).toInt() + 2, imageWidth, BUTTON_HEIGHT).build()
                )
            }
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val bottomLine = this.height - BOTTOM_BAR_HEIGHT

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + 20, Color.RED.rgb
            )
        } else {
            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            screenshots.forEachIndexed { imageIndex, file ->
                val col = imageIndex % columns
                val row = imageIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * (imageHeight + BUTTON_HEIGHT + PADDING)

                val slot = loader.getSlotState(imageIndex)
                when (slot.state) {
                    ScreenshotLoader.LoadState.LOADED -> {
                        slot.loaded?.let {
                            guiGraphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                it.textureId,
                                x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight
                            )
                        }
                        drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x, y + PADDING)
                    }
                    ScreenshotLoader.LoadState.LOADING -> {
                        guiGraphics.centeredText(
                            Minecraft.getInstance().font,
                            Component.translatable("nicephore.screenshots.loading"),
                            x + imageWidth / 2, y + imageHeight / 2, Color.GRAY.rgb
                        )
                    }
                    ScreenshotLoader.LoadState.ERROR -> {
                        guiGraphics.centeredText(
                            Minecraft.getInstance().font,
                            Component.translatable("nicephore.screenshots.error"),
                            x + imageWidth / 2, y + imageHeight / 2, Color.RED.rgb
                        )
                    }
                }
            }

            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.gui.gallery.pages", index + 1, getNumberOfPages()),
                centerX, bottomLine + 5, Color.WHITE.rgb
            )
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun drawExtensionBadge(guiGraphics: GuiGraphicsExtractor, extension: String, x: Int, y: Int) {
        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
            guiGraphics.text(Minecraft.getInstance().font, extension.uppercase(), x + 2, y - 12, Color.WHITE.rgb)
        }
    }

    private fun modIndex(value: Int) {
        index = wrapIndex(index, value, getNumberOfPages().toInt())
        init()
    }

    private fun openScreenshotScreen(value: Int) {
        Minecraft.getInstance().pushGuiLayer(ScreenshotScreen(value, index, this))
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    override fun onFilterChange(filter: ScreenshotFilter) {
        NicephoreConfig.Client.setScreenshotFilter(filter)
        init()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
