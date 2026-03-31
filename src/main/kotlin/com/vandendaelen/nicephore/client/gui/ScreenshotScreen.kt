package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
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
    private var aspectRatio: Float = 16f / 9f

    override fun init() {
        super.init()

        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        screenshots = ArrayList(
            SCREENSHOTS_DIR.listFiles(filter)
                ?.sortedWith(Comparator.comparingLong(File::lastModified).reversed())
                ?: emptyList()
        )

        index = clampIndex(index, screenshots.size)
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[index]) else 16f / 9f

        if (screenshots.isNotEmpty()) {
            screenshotTexture?.close()

            val fileToLoad = screenshots[index]
            if (fileToLoad.exists()) {
                screenshotTexture = Util.fileToTexture(screenshots[index])
            } else {
                closeScreen("nicephore.screenshots.loading.error")
                return
            }
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val pictureMidWidth = (this.width * 0.5 * 1.2).toInt()
        val pictureHeight = (pictureMidWidth / aspectRatio).toInt()
        val bottomLine = this.minecraft.window.guiScaledHeight - 30

        this.clearWidgets()
        addToolbarButtons { cycleFilter(listener) }

        if (screenshots.isNotEmpty()) {
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy")) {
                val screenshot = screenshots[index]
                if (CopyImageToClipBoard.copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }.bounds(centerX - 52, bottomLine, 50, 20).build()

            copyButton.active = OperatingSystems.getOS().manager != null
            if (!copyButton.isActive &&
                mouseX >= copyButton.x && mouseY >= copyButton.y &&
                mouseX < copyButton.x + copyButton.width && mouseY < copyButton.y + copyButton.height
            ) {
                guiGraphics.setComponentTooltipForNextFrame(
                    Minecraft.getInstance().font,
                    listOf(Component.translatable("nicephore.gui.screenshots.copy.unable").withStyle(ChatFormatting.RED)),
                    mouseX, mouseY
                )
            }
            this.addRenderableWidget(copyButton)

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.screenshots.delete")) { deleteScreenshot(screenshots[index]) }
                    .bounds(centerX + 3, bottomLine, 50, 20).build()
            )
        }

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, 20, Color.RED.rgb
            )
        } else {
            val currentScreenshot = screenshots[index]
            if (currentScreenshot.exists()) {
                val tm = this.minecraft.textureManager
                val textureId = Identifier.withDefaultNamespace("nicephore_screenshot")
                tm.register(textureId, screenshotTexture!!)
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    textureId,
                    centerX - pictureMidWidth / 2, 50, 0f, 0f,
                    pictureMidWidth, pictureHeight, pictureMidWidth, pictureHeight
                )

                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.translatable("nicephore.gui.screenshots.pages", index + 1, screenshots.size),
                    centerX, 20, Color.WHITE.rgb
                )
                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.literal(MessageFormat.format("{0} ({1})", currentScreenshot.name, getFileSizeMegaBytes(currentScreenshot))),
                    centerX, 35, Color.WHITE.rgb
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
        screenshotTexture?.close()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private var screenshotTexture: DynamicTexture? = null

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists() && (SCREENSHOTS_DIR.list()?.isNotEmpty() == true)
        }

        private fun getFileSizeMegaBytes(file: File): String {
            val size = FileUtils.sizeOf(file).toDouble()
            val formatter: NumberFormat = DecimalFormat("#0.00")
            val mbSize = 1024 * 1024
            val kbSize = 1024

            return if (size > mbSize) {
                MessageFormat.format("{0} MB", formatter.format(FileUtils.sizeOf(file).toDouble() / mbSize))
            } else {
                MessageFormat.format("{0} KB", formatter.format(FileUtils.sizeOf(file).toDouble() / kbSize))
            }
        }
    }
}
