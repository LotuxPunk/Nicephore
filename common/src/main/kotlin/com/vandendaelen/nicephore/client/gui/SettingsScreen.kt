package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.platform.Services
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.awt.Color

class SettingsScreen(private val onSettingsClosed: () -> Unit = {}) : AbstractNicephoreScreen(TITLE) {

    private var titleY: Int = 35

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        val startingLine = this.width / 2 - 150
        val itemCount = 5
        val itemHeight = BUTTON_HEIGHT + PADDING  // 30
        val totalContentHeight = itemCount * itemHeight - PADDING  // 140
        val titleHeight = 9 + PADDING  // font height + gap
        val topY = ((this.height - titleHeight - totalContentHeight) / 2).coerceAtLeast(TOOLBAR_HEIGHT)
        titleY = topY
        val contentStartY = topY + titleHeight

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.showOptimisationStatus", if (Services.config.getShouldShowOptStatus()) "ON" else "OFF")
            ) { toggleSetting { Services.config.setShouldShowOptStatus(!Services.config.getShouldShowOptStatus()) } }
                .bounds(startingLine, contentStartY, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.makeJPEGs", if (Services.config.getJPEGToggle()) "ON" else "OFF")
            ) { toggleSetting { Services.config.setJPEGToggle(!Services.config.getJPEGToggle()) } }
                .bounds(startingLine, contentStartY + itemHeight, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.screenshotCustomMessage", if (Services.config.getScreenshotCustomMessage()) "ON" else "OFF")
            ) { toggleSetting { Services.config.setScreenshotCustomMessage(!Services.config.getScreenshotCustomMessage()) } }
                .bounds(startingLine, contentStartY + 2 * itemHeight, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.setScreenshotToClipboard", if (Services.config.getScreenshotToClipboard()) "ON" else "OFF")
            ) { toggleSetting { Services.config.setScreenshotToClipboard(!Services.config.getScreenshotToClipboard()) } }
                .bounds(startingLine, contentStartY + 3 * itemHeight, 300, BUTTON_HEIGHT).build()
        )

        val currentColumns = Services.config.getGalleryColumns()
        val label = if (currentColumns == 0) "Auto" else "$currentColumns"
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.settings.galleryColumns", label)
            ) { cycleGalleryColumns() }
                .bounds(startingLine, contentStartY + 4 * itemHeight, 300, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2

        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.settings"),
            centerX, titleY, Color.WHITE.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun toggleSetting(action: () -> Unit) {
        action()
        refreshWidgets()
    }

    private fun cycleGalleryColumns() {
        val current = Services.config.getGalleryColumns()
        val next = when (current) {
            0 -> 2
            in 2..5 -> current + 1
            else -> 0
        }
        Services.config.setGalleryColumns(next)
        refreshWidgets()
    }

    override fun onClose() {
        super.onClose()
        onSettingsClosed()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.settings")
    }
}
