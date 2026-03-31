package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

abstract class AbstractNicephoreScreen(title: Component) : Screen(title) {

    override fun extractBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.extractTransparentBackground(guiGraphics)
    }

    protected fun closeScreen(textComponentId: String) {
        this.onClose()
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId))
    }

    protected fun openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(SettingsScreen())
    }

    protected fun readAspectRatio(file: File): Float {
        try {
            ImageIO.createImageInputStream(file).use { imageIn ->
                val readers = ImageIO.getImageReaders(imageIn)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    try {
                        reader.setInput(imageIn)
                        return reader.getWidth(0) / reader.getHeight(0).toFloat()
                    } finally {
                        reader.dispose()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 16f / 9f
    }

    protected fun wrapIndex(currentIndex: Int, delta: Int, max: Int): Int {
        val newIndex = currentIndex + delta
        return when {
            newIndex in 0 until max -> newIndex
            newIndex < 0 -> max - 1
            else -> 0
        }
    }

    protected fun clampIndex(currentIndex: Int, max: Int): Int {
        return if (currentIndex >= max || currentIndex < 0) max - 1 else currentIndex
    }

    protected fun cycleFilter(listener: FilterListener? = null) {
        val nextFilter = NicephoreConfig.Client.getScreenshotFilter().next()
        NicephoreConfig.Client.setScreenshotFilter(nextFilter)
        init()
        listener?.onFilterChange(nextFilter)
    }

    protected fun addToolbarButtons(onFilterChange: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name)) { onFilterChange() }
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
    }

    protected fun addNavigationButtons(centerX: Int, bottomLine: Int, onPrev: () -> Unit, onNext: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.literal("<")) { onPrev() }
                .bounds(centerX - 80, bottomLine, 20, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.literal(">")) { onNext() }
                .bounds(centerX + 60, bottomLine, 20, 20).build()
        )
    }

    companion object {
        val SCREENSHOTS_DIR: File = File(Minecraft.getInstance().gameDirectory, "screenshots")
    }
}
