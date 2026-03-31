package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FileUtils
import java.awt.Color
import java.io.File
import java.text.DecimalFormat
import java.text.MessageFormat
import java.text.NumberFormat
import java.util.Comparator

class ScreenshotScreen @JvmOverloads constructor(
    private var index: Int = 0,
    private val galleryScreenPage: Int = -1,
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

            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy")) {
                val screenshot = screenshots[index]
                if (CopyImageToClipBoard.copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }.bounds(centerX - 52, bottomLine, 50, BUTTON_HEIGHT).build()

            copyButton.active = OperatingSystems.getOS().manager != null
            this.addRenderableWidget(copyButton)

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.screenshots.delete")) { deleteScreenshot(screenshots[index]) }
                    .bounds(centerX + 3, bottomLine, 50, BUTTON_HEIGHT).build()
            )
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val maxImageWidth = this.width - 2 * SIDE_PADDING
        val pictureMidWidth = maxImageWidth.coerceAtMost(MAX_IMAGE_WIDTH)
        val pictureHeight = (pictureMidWidth / aspectRatio).toInt()

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
                Component.literal(MessageFormat.format("{0} ({1})", currentScreenshot.name, getFileSizeMegaBytes(currentScreenshot))),
                centerX, TOOLBAR_HEIGHT + 12, Color.WHITE.rgb
            )

            // Tooltip for disabled copy button
            val copyButtonX = centerX - 52
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
                if (galleryScreenPage > -1) GalleryScreen(galleryScreenPage) else ScreenshotScreen(index)
            )
        )
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private const val SIDE_PADDING = 40
        private const val MAX_IMAGE_WIDTH = 800
        private const val IMAGE_TOP = 50

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists() && (SCREENSHOTS_DIR.list()?.isNotEmpty() == true)
        }

        private fun getFileSizeMegaBytes(file: File): String {
            val size = FileUtils.sizeOf(file).toDouble()
            val formatter: NumberFormat = DecimalFormat("#0.00")
            val mbSize = 1024 * 1024
            val kbSize = 1024

            return if (size > mbSize) {
                MessageFormat.format("{0} MB", formatter.format(size / mbSize))
            } else {
                MessageFormat.format("{0} KB", formatter.format(size / kbSize))
            }
        }
    }
}
