package com.vandendaelen.nicephore.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO

class GalleryScreen(private var index: Int = 0) : Screen(TITLE) {

    private val screenshots = mutableListOf<File>()
    private val pagesOfScreenshots = mutableListOf<List<File>>()
    private var aspectRatio = 1.7777f

    init {
        if (screenshots.isNotEmpty()) {
            try {
                val bimg = ImageIO.read(screenshots[index])
                val width = bimg.width
                val height = bimg.height
                bimg.graphics.dispose()
                aspectRatio = (width / height.toDouble()).toFloat()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun init() {
        super.init()

        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        index = getIndex()

        screenshots.clear()
        SCREENSHOTS_DIR.listFiles(filter)
            ?.sortedBy { it.lastModified() }
            ?.reversed()
            ?.let {
                screenshots.addAll(it)
            }

        pagesOfScreenshots.clear()
        pagesOfScreenshots.addAll(screenshots.chunked(IMAGES_TO_DISPLAY))

        if (pagesOfScreenshots.isNotEmpty()) {
            SCREENSHOT_TEXTURES.forEach(DynamicTexture::close)
            SCREENSHOT_TEXTURES.clear()

            val filesToLoad = pagesOfScreenshots[index]
            if (filesToLoad.isNotEmpty()) {
                filesToLoad.forEach { file: File? ->
                    SCREENSHOT_TEXTURES.add(Util.fileToTexture(file))
                }
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

    override fun render(matrixStack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = width / 2
        val imageWidth = (width * 1.0 / 5).toInt()
        val imageHeight = (imageWidth / aspectRatio).toInt()

        this.renderBackground(matrixStack)
        clearWidgets()

        addRenderableWidget(Button(10,10,100,20, TranslatableComponent("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name)) { changeFilter() })
        addRenderableWidget(Button(width - 60, 10, 50, 20, TranslatableComponent("nicephore.screenshot.exit")) { onClose() })

        if (screenshots.isNotEmpty()) {
            addRenderableWidget(Button(width / 2 - 80, height / 2 + 100, 20, 20, TextComponent("<")) { modIndex(-1) })
            addRenderableWidget(Button(width / 2 + 60, height / 2 + 100, 20, 20, TextComponent(">")) { modIndex(1) })
        }

        if (pagesOfScreenshots.isNotEmpty()) {
            pagesOfScreenshots[index]
                .takeIf { it.all(File::exists) }
                ?.let { currentPage ->
                    SCREENSHOT_TEXTURES.forEachIndexed { imageIndex, dynamicTexture ->
                        val name = currentPage[imageIndex].name
                        val text = TextComponent(StringUtils.abbreviate(name, 13))
                        val x = centerX - (15 - imageIndex % 4 * 10) - (2 - imageIndex % 4) * imageWidth
                        val y = 50 + imageIndex / 4 * (imageHeight + 30)

                        RenderSystem.setShaderTexture(0, dynamicTexture.id)
                        RenderSystem.enableBlend()
                        blit(matrixStack, x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight)
                        RenderSystem.disableBlend()
                        drawExtensionBadge(matrixStack, FilenameUtils.getExtension(name), x - 10, y + 14)
                        addRenderableWidget(Button(x, y + 5 + imageHeight, imageWidth, 20, text) { openScreenshotScreen(screenshots.indexOf(currentPage[imageIndex])) })
                    }

                    drawCenteredString(matrixStack, Minecraft.getInstance().font, TranslatableComponent("nicephore.gui.gallery.pages", index + 1, pagesOfScreenshots.size), centerX, height / 2 + 105, Color.WHITE.rgb)
                }
        }
        else {
            drawCenteredString(matrixStack, Minecraft.getInstance().font, TranslatableComponent("nicephore.screenshots.empty"), centerX, 50, Color.RED.rgb)
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks)
    }

    private fun drawExtensionBadge(matrixStack: PoseStack, extension: String, x: Int, y: Int) {
        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
            GuiComponent.drawString(matrixStack, Minecraft.getInstance().font, extension.uppercase(Locale.getDefault()), x + 12, y - 12, Color.WHITE.rgb)
        }
    }

    private fun modIndex(value: Int) {
        val max = pagesOfScreenshots.size

        index = when {
            index + value in 0 until max -> index + value
            index + value < 0 -> max - 1
            else -> 0
        }

        init()
    }

    private fun openScreenshotScreen(value: Int) {
        Minecraft.getInstance().pushGuiLayer(ScreenshotScreen(value, index))
    }

    private fun getIndex(): Int {
        return when{
            pagesOfScreenshots.isNotEmpty() && ( index >= pagesOfScreenshots.size || index < 0 ) -> pagesOfScreenshots.size - 1
            else -> index
        }
    }

    private fun closeScreen(textComponentId: String) {
        SCREENSHOT_TEXTURES.forEach(DynamicTexture::close)
        SCREENSHOT_TEXTURES.clear()
        onClose()
        PlayerHelper.sendHotbarMessage(TranslatableComponent(textComponentId))
    }

    companion object {
        private val TITLE = TranslatableComponent("nicephore.gui.screenshots")
        private val SCREENSHOTS_DIR = File(Minecraft.getInstance().gameDirectory, "screenshots")
        private val SCREENSHOT_TEXTURES = mutableListOf<DynamicTexture>()
        private const val ROW = 2
        private const val COLUMN = 4
        private const val IMAGES_TO_DISPLAY = ROW * COLUMN

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists() && !SCREENSHOTS_DIR.list().isNullOrEmpty()
        }
    }
}