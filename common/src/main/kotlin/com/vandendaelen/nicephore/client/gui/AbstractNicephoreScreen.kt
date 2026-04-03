package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.Services
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

    protected fun refreshWidgets() {
        this.clearWidgets()
        buildWidgets()
    }

    protected open fun buildWidgets() {
        // Subclasses override to add their widgets
    }

    protected fun closeScreen(textComponentId: String) {
        this.onClose()
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId))
    }

    protected fun openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(SettingsScreen { init() })
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
            Nicephore.LOGGER.warn("Failed to read aspect ratio from {}", file.name, e)
        }
        return DEFAULT_ASPECT_RATIO
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
        val nextFilter = Services.config.getScreenshotFilter().next()
        Services.config.setScreenshotFilter(nextFilter)
        init()
        listener?.onFilterChange(nextFilter)
    }

    protected fun addToolbarButtons(onFilterChange: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", Services.config.getScreenshotFilter().name)) { onFilterChange() }
                .bounds(PADDING, PADDING, 100, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.settings")) { openSettingsScreen() }
                .bounds(this.width - PADDING - 110, PADDING, 50, BUTTON_HEIGHT).build()
        )
    }

    protected fun addNavigationButtons(centerX: Int, bottomLine: Int, onPrev: () -> Unit, onNext: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.literal("<")) { onPrev() }
                .bounds(centerX - 80, bottomLine, 20, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.literal(">")) { onNext() }
                .bounds(centerX + 60, bottomLine, 20, BUTTON_HEIGHT).build()
        )
    }

    data class GridLayout(
        val columns: Int,
        val rows: Int,
        val imageWidth: Int,
        val imageHeight: Int,
        val slotHeight: Int,
        val verticalOffset: Int
    ) {
        val imagesToDisplay: Int get() = rows * columns

        fun slotX(slotIndex: Int): Int = PADDING + (slotIndex % columns) * (imageWidth + PADDING)

        fun slotY(slotIndex: Int, baseY: Int = TOOLBAR_HEIGHT): Int {
            val row = slotIndex / columns
            return baseY + verticalOffset + row * slotHeight
        }
    }

    protected fun computeGrid(
        screenWidth: Int,
        screenHeight: Int,
        aspectRatio: Float,
        configColumns: Int = 0
    ): GridLayout {
        val availableWidth = screenWidth - 2 * PADDING
        val availableHeight = screenHeight - TOOLBAR_HEIGHT - BOTTOM_BAR_HEIGHT - PADDING

        val columns = if (configColumns in 2..6) {
            configColumns
        } else {
            (availableWidth / (TARGET_THUMBNAIL_WIDTH + PADDING)).coerceIn(2, 6)
        }

        val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
        val maxImageHeight = (availableHeight - BUTTON_HEIGHT - PADDING).coerceAtLeast(1)
        val imageHeight = (imageWidth / aspectRatio).toInt().coerceAtMost(maxImageHeight)
        val slotHeight = imageHeight + BUTTON_HEIGHT + PADDING
        val rows = (availableHeight / slotHeight).coerceIn(1, 4)
        val verticalOffset = if (rows == 1) ((availableHeight - slotHeight) / 2).coerceAtLeast(0) else 0

        return GridLayout(columns, rows, imageWidth, imageHeight, slotHeight, verticalOffset)
    }

    companion object {
        val SCREENSHOTS_DIR: File = File(Minecraft.getInstance().gameDirectory, "screenshots")

        const val DEFAULT_ASPECT_RATIO = 16f / 9f
        const val PADDING = 10
        const val TOOLBAR_HEIGHT = 40
        const val BUTTON_HEIGHT = 20
        const val BOTTOM_BAR_HEIGHT = 30
        const val PAGE_TEXT_HEIGHT = 14
        const val TARGET_THUMBNAIL_WIDTH = 150
    }
}
