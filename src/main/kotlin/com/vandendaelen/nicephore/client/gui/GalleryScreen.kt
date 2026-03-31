package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File

class GalleryScreen(private var index: Int = 0) : AbstractNicephoreScreen(TITLE), FilterListener {
    private val row: Int = getRows()
    private val column: Int = 4
    private val imagesToDisplay: Int = row * column
    private var screenshots: List<File> = emptyList()
    private var aspectRatio: Float = 16f / 9f

    private fun getNumberOfPages(): Long {
        return kotlin.math.ceil(Util.getNumberOfFiles(SCREENSHOTS_DIR) / imagesToDisplay.toDouble()).toLong()
    }

    override fun init() {
        super.init()

        screenshots = Util.getBatchOfFiles((imagesToDisplay.toLong() * index), imagesToDisplay.toLong(), SCREENSHOTS_DIR)
        index = clampIndex(index, getNumberOfPages().toInt())
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[0]) else 16f / 9f

        if (screenshots.isNotEmpty()) {
            screenshotTextures.forEach { it.close() }
            screenshotTextures.clear()

            if (screenshots.isNotEmpty()) {
                screenshots.forEach { file -> screenshotTextures.add(Util.fileToTexture(file)) }
            } else {
                closeScreen("nicephore.screenshots.loading.error")
                return
            }
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val imageWidth = (this.width * 1.0 / 5).toInt()
        val imageHeight = (imageWidth / aspectRatio).toInt()
        val bottomLine = this.minecraft.window.guiScaledHeight - 30

        this.clearWidgets()
        addToolbarButtons { cycleFilter() }

        if (screenshots.isNotEmpty()) {
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })
        }

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, 50, Color.RED.rgb
            )
        } else {
            if (screenshots.all { it.exists() }) {
                val tm = this.minecraft.textureManager
                screenshotTextures.forEachIndexed { imageIndex, texture ->
                    val name = screenshots[imageIndex].name
                    val text = Component.literal(StringUtils.abbreviate(name, 13))

                    val x = centerX - (15 - (imageIndex % 4) * 10) - (2 - (imageIndex % 4)) * imageWidth
                    val y = 50 + (imageIndex / 4 * (imageHeight + 30))

                    val textureId = Identifier.withDefaultNamespace("nicephore_gallery_$imageIndex")
                    tm.register(textureId, texture)
                    guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        textureId,
                        x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight
                    )

                    drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(name), x - 10, y + 14)
                    this.addRenderableWidget(
                        Button.builder(text) { openScreenshotScreen(screenshots.indexOf(screenshots[imageIndex])) }
                            .bounds(x, y + 5 + imageHeight, imageWidth, 20).build()
                    )
                }

                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.translatable("nicephore.gui.gallery.pages", index + 1, getNumberOfPages()),
                    centerX, bottomLine + 5, Color.WHITE.rgb
                )
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun drawExtensionBadge(guiGraphics: GuiGraphicsExtractor, extension: String, x: Int, y: Int) {
        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
            guiGraphics.text(Minecraft.getInstance().font, extension.uppercase(), x + 12, y - 12, Color.WHITE.rgb)
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
        screenshotTextures.forEach { it.close() }
        screenshotTextures.clear()
        super.onClose()
    }

    override fun onFilterChange(filter: ScreenshotFilter) {
        NicephoreConfig.Client.setScreenshotFilter(filter)
        init()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private val screenshotTextures = ArrayList<DynamicTexture>()

        private fun getRows(): Int {
            return if (Minecraft.getInstance().window.guiScale >= 3.0) 2 else 3
        }

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
