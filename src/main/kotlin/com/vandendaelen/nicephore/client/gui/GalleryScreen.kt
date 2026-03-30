package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class GalleryScreen(private var index: Int = 0) : Screen(TITLE), FilterListener {
    private val row: Int = getRows()
    private val column: Int = 4
    private val imagesToDisplay: Int = row * column
    private var screenshots: List<File> = emptyList()
    private var aspectRatio: Float = 1.7777f

    private fun getNumberOfPages(): Long {
        return kotlin.math.ceil(Util.getNumberOfFiles(SCREENSHOTS_DIR) / imagesToDisplay.toDouble()).toLong()
    }

    override fun init() {
        super.init()

        screenshots = Util.getBatchOfFiles((imagesToDisplay.toLong() * index), imagesToDisplay.toLong(), SCREENSHOTS_DIR)
        index = getIndex()
        aspectRatio = 1.7777f

        if (screenshots.isNotEmpty()) {
            try {
                ImageIO.createImageInputStream(screenshots[0]).use { imageIn ->
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

    private fun changeFilter() {
        val nextFilter = NicephoreConfig.Client.getScreenshotFilter().next()
        NicephoreConfig.Client.setScreenshotFilter(nextFilter)
        init()
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val imageWidth = (this.width * 1.0 / 5).toInt()
        val imageHeight = (imageWidth / aspectRatio).toInt()
        val bottomLine = this.minecraft!!.window.guiScaledHeight - 30

        this.extractBackground(guiGraphics, mouseX, mouseY, partialTicks)

        this.clearWidgets()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name)) { changeFilter() }
                .bounds(10, 10, 100, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - 60, 10, 50, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.settings")) { openSettingsScreen() }
                .bounds(this.width - 120, 10, 50, 20).build()
        )

        if (screenshots.isNotEmpty()) {
            this.addRenderableWidget(
                Button.builder(Component.literal("<")) { modIndex(-1) }
                    .bounds(this.width / 2 - 80, bottomLine, 20, 20).build()
            )
            this.addRenderableWidget(
                Button.builder(Component.literal(">")) { modIndex(1) }
                    .bounds(this.width / 2 + 60, bottomLine, 20, 20).build()
            )
        }

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, 50, Color.RED.rgb
            )
        } else {
            if (screenshots.all { it.exists() }) {
                val tm = this.minecraft!!.textureManager
                screenshotTextures.forEachIndexed { imageIndex, texture ->
                    val name = screenshots[imageIndex].name
                    val text = Component.literal(StringUtils.abbreviate(name, 13))

                    val x = centerX - (15 - (imageIndex % 4) * 10) - (2 - (imageIndex % 4)) * imageWidth
                    val y = 50 + (imageIndex / 4 * (imageHeight + 30))

                    guiGraphics.blit(
                        tm.register("screenshot_${texture.id}", texture),
                        x, y, 0f, 0f, imageWidth, imageHeight
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
        val max = getNumberOfPages()
        if (index + value >= 0 && index + value < max) {
            index += value
        } else {
            index = if (index + value < 0) (max - 1).toInt() else 0
        }
        init()
    }

    private fun openScreenshotScreen(value: Int) {
        Minecraft.getInstance().pushGuiLayer(ScreenshotScreen(value, index, this))
    }

    private fun openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(SettingsScreen())
    }

    private fun getIndex(): Int {
        val numberOfPages = getNumberOfPages()
        if (index > numberOfPages || index < 0) {
            index = (numberOfPages - 1).toInt()
        }
        return index
    }

    private fun closeScreen(textComponentId: String) {
        this.onClose()
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId))
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
        private val SCREENSHOTS_DIR = File(Minecraft.getInstance().gameDirectory, "screenshots")
        private val screenshotTextures = ArrayList<DynamicTexture>()

        private fun getRows(): Int {
            return if (Minecraft.getInstance().window.guiScale >= 3.0) 2 else 3
        }

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
