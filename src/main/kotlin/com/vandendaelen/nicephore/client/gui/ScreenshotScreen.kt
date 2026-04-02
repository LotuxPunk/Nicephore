package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File
import java.util.Comparator

class ScreenshotScreen @JvmOverloads constructor(
    private var index: Int = 0,
    private val galleryScrollOffset: Float = -1f,
    private val listener: FilterListener? = null
) : AbstractNicephoreScreen(TITLE) {

    private var screenshots: ArrayList<File> = ArrayList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    override fun init() {
        super.init()

        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        screenshots = ArrayList(
            SCREENSHOTS_DIR.listFiles(filter)
                ?.sortedWith(Comparator.comparingLong(File::lastModified).reversed())
                ?: emptyList()
        )

        index = clampIndex(index, screenshots.size)
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[index]) else DEFAULT_ASPECT_RATIO

        if (screenshots.isNotEmpty()) {
            loader.setOnLoadComplete { /* texture ready, next frame picks it up */ }
            loader.loadSingle(screenshots[index], "screenshot")
        }

        refreshWidgets()
    }

    override fun buildWidgets() {
        addToolbarButtons { cycleFilter(listener) }

        if (screenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT

            val navW = 20
            val actionW = 50
            val gap = 5
            val totalW = 2 * navW + 3 * actionW + 4 * gap  // 210
            val startX = centerX - totalW / 2

            this.addRenderableWidget(
                Button.builder(Component.literal("<")) { modIndex(-1) }
                    .bounds(startX, bottomLine, navW, BUTTON_HEIGHT).build()
            )

            val copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy")) {
                val screenshot = screenshots[index]
                if (CopyImageToClipBoard.copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }.bounds(startX + navW + gap, bottomLine, actionW, BUTTON_HEIGHT).build()

            copyButton.active = OperatingSystems.getOS().manager != null
            this.addRenderableWidget(copyButton)

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.screenshots.delete")) { deleteScreenshot(screenshots[index]) }
                    .bounds(startX + navW + actionW + 2 * gap, bottomLine, actionW, BUTTON_HEIGHT).build()
            )
            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.rename")) { renameScreenshot(screenshots[index]) }
                    .bounds(startX + navW + 2 * actionW + 3 * gap, bottomLine, actionW, BUTTON_HEIGHT).build()
            )

            this.addRenderableWidget(
                Button.builder(Component.literal(">")) { modIndex(1) }
                    .bounds(startX + totalW - navW, bottomLine, navW, BUTTON_HEIGHT).build()
            )
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val maxAvailableHeight = this.height - IMAGE_TOP - BOTTOM_BAR_HEIGHT - PADDING
        val widthFromScreen = this.width - 2 * SIDE_PADDING
        val heightFromWidth = (widthFromScreen / aspectRatio).toInt()
        val pictureHeight = heightFromWidth.coerceAtMost(maxAvailableHeight)
        val pictureMidWidth = if (heightFromWidth > maxAvailableHeight) {
            (maxAvailableHeight * aspectRatio).toInt()
        } else {
            widthFromScreen
        }

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + PADDING, Color.RED.rgb
            )
        } else {
            val slot = loader.getSlotState(0)
            when (slot.state) {
                ScreenshotLoader.LoadState.LOADED -> {
                    slot.loaded?.let {
                        guiGraphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            it.textureId,
                            centerX - pictureMidWidth / 2, IMAGE_TOP, 0f, 0f,
                            pictureMidWidth, pictureHeight, pictureMidWidth, pictureHeight
                        )
                    }
                }
                ScreenshotLoader.LoadState.LOADING -> {
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.translatable("nicephore.screenshots.loading"),
                        centerX, IMAGE_TOP + pictureHeight / 2, Color.GRAY.rgb
                    )
                }
                ScreenshotLoader.LoadState.ERROR -> {
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.translatable("nicephore.screenshots.error"),
                        centerX, IMAGE_TOP + pictureHeight / 2, Color.RED.rgb
                    )
                }
            }

            val currentScreenshot = screenshots[index]
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.gui.screenshots.pages", index + 1, screenshots.size),
                centerX, TOOLBAR_HEIGHT, Color.WHITE.rgb
            )
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.literal("${currentScreenshot.name} (${Util.formatFileSize(currentScreenshot)})"),
                centerX, TOOLBAR_HEIGHT + 12, Color.WHITE.rgb
            )

            // Tooltip for disabled copy button
            val totalW = 2 * 20 + 3 * 50 + 4 * 5  // 210
            val copyButtonX = centerX - totalW / 2 + 20 + 5
            val copyButtonY = this.height - BOTTOM_BAR_HEIGHT
            if (OperatingSystems.getOS().manager == null &&
                mouseX >= copyButtonX && mouseY >= copyButtonY &&
                mouseX < copyButtonX + 50 && mouseY < copyButtonY + BUTTON_HEIGHT
            ) {
                guiGraphics.setComponentTooltipForNextFrame(
                    Minecraft.getInstance().font,
                    listOf(Component.translatable("nicephore.gui.screenshots.copy.unable").withStyle(ChatFormatting.RED)),
                    mouseX, mouseY
                )
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun modIndex(value: Int) {
        index = wrapIndex(index, value, screenshots.size)
        init()
    }

    private fun deleteScreenshot(file: File) {
        Minecraft.getInstance().pushGuiLayer(
            DeleteConfirmScreen(
                file,
                if (galleryScrollOffset >= 0f) GalleryScreen(galleryScrollOffset) else ScreenshotScreen(index)
            )
        )
    }

    private fun renameScreenshot(file: File) {
        Minecraft.getInstance().pushGuiLayer(RenameScreen(file, galleryScrollOffset))
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private const val SIDE_PADDING = 40
        private const val IMAGE_TOP = 65

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists() && (SCREENSHOTS_DIR.list()?.isNotEmpty() == true)
        }
    }
}
