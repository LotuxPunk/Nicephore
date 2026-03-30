package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import org.apache.commons.io.FileUtils
import java.awt.Color
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.text.MessageFormat
import java.text.NumberFormat
import java.util.Comparator
import java.util.stream.Collectors
import javax.imageio.ImageIO

class ScreenshotScreen @JvmOverloads constructor(
    private var index: Int = 0,
    private val galleryScreenPage: Int = -1,
    private val listener: FilterListener? = null
) : Screen(TITLE) {

    private var screenshots: ArrayList<File> = ArrayList()
    private var aspectRatio: Float = 1.7777f

    override fun init() {
        super.init()

        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        screenshots = ArrayList(
            SCREENSHOTS_DIR.listFiles(filter)
                ?.sortedWith(Comparator.comparingLong(File::lastModified).reversed())
                ?: emptyList()
        )

        index = getIndex()
        aspectRatio = 1.7777f

        if (screenshots.isNotEmpty()) {
            try {
                ImageIO.createImageInputStream(screenshots[index]).use { imageIn ->
                    val readers = ImageIO.getImageReaders(imageIn)
                    if (readers.hasNext()) {
                        val reader = readers.next()
                        try {
                            reader.setInput(imageIn)
                            aspectRatio = reader.getWidth(0) / reader.getHeight(0).toFloat()
                        } finally {
                            reader.dispose()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

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

    private fun changeFilter() {
        val nextFilter = NicephoreConfig.Client.getScreenshotFilter().next()
        NicephoreConfig.Client.setScreenshotFilter(nextFilter)
        init()
        listener?.onFilterChange(nextFilter)
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val mc = this.minecraft!!
        val centerX = mc.window.guiScaledWidth / 2
        val pictureMidWidth = (mc.window.guiScaledWidth * 0.5 * 1.2).toInt()
        val pictureHeight = (pictureMidWidth / aspectRatio).toInt()
        val bottomLine = mc.window.guiScaledHeight - 30

        this.extractBackground(guiGraphics, mouseX, mouseY, partialTicks)

        this.clearWidgets()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name)) { changeFilter() }
                .bounds(10, 10, 100, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(mc.window.guiScaledWidth - 60, 10, 50, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.settings")) { openSettingsScreen() }
                .bounds(this.width - 120, 10, 50, 20).build()
        )

        if (screenshots.isNotEmpty()) {
            this.addRenderableWidget(
                Button.builder(Component.literal("<")) { modIndex(-1) }
                    .bounds(mc.window.guiScaledWidth / 2 - 80, bottomLine, 20, 20).build()
            )
            this.addRenderableWidget(
                Button.builder(Component.literal(">")) { modIndex(1) }
                    .bounds(mc.window.guiScaledWidth / 2 + 60, bottomLine, 20, 20).build()
            )

            val copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy")) {
                val screenshot = screenshots[index]
                if (CopyImageToClipBoard.copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }.bounds(mc.window.guiScaledWidth / 2 - 52, bottomLine, 50, 20).build()

            copyButton.active = OperatingSystems.getOS().manager != null
            if (!copyButton.isActive &&
                mouseX >= copyButton.x && mouseY >= copyButton.y &&
                mouseX < copyButton.x + copyButton.width && mouseY < copyButton.y + copyButton.height
            ) {
                guiGraphics.componentTooltip(
                    Minecraft.getInstance().font,
                    listOf(Component.translatable("nicephore.gui.screenshots.copy.unable").withStyle(ChatFormatting.RED)),
                    mouseX, mouseY
                )
            }
            this.addRenderableWidget(copyButton)

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.screenshots.delete")) { deleteScreenshot(screenshots[index]) }
                    .bounds(mc.window.guiScaledWidth / 2 + 3, bottomLine, 50, 20).build()
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
                val tm = mc.textureManager
                guiGraphics.blit(
                    tm.register("screenshot", screenshotTexture!!),
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
        val max = screenshots.size
        if (index + value in 0 until max) {
            index += value
        } else {
            index = if (index + value < 0) max - 1 else 0
        }
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

    private fun openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(SettingsScreen())
    }

    private fun getIndex(): Int {
        if (index >= screenshots.size || index < 0) {
            index = screenshots.size - 1
        }
        return index
    }

    private fun closeScreen(textComponentId: String) {
        this.onClose()
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId))
    }

    override fun onClose() {
        screenshotTexture?.close()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private val SCREENSHOTS_DIR = File(Minecraft.getInstance().gameDirectory, "screenshots")
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
