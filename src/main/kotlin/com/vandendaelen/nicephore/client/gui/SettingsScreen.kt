package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.awt.Color

class SettingsScreen : AbstractNicephoreScreen(TITLE) {

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        val startingLine = this.width / 2 - 150

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.showOptimisationStatus", if (NicephoreConfig.Client.getShouldShowOptStatus()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setShouldShowOptStatus(!NicephoreConfig.Client.getShouldShowOptStatus()) } }
                .bounds(startingLine, 60, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.makeJPEGs", if (NicephoreConfig.Client.getJPEGToggle()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setJPEGToggle(!NicephoreConfig.Client.getJPEGToggle()) } }
                .bounds(startingLine, 90, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.screenshotCustomMessage", if (NicephoreConfig.Client.getScreenshotCustomMessage()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setScreenshotCustomMessage(!NicephoreConfig.Client.getScreenshotCustomMessage()) } }
                .bounds(startingLine, 120, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.setScreenshotToClipboard", if (NicephoreConfig.Client.getScreenshotToClipboard()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setScreenshotToClipboard(!NicephoreConfig.Client.getScreenshotToClipboard()) } }
                .bounds(startingLine, 150, 300, BUTTON_HEIGHT).build()
        )

        val currentColumns = NicephoreConfig.Client.getGalleryColumns()
        val label = if (currentColumns == 0) "Auto" else "$currentColumns"
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.settings.galleryColumns", label)
            ) { cycleGalleryColumns() }
                .bounds(startingLine, 180, 300, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2

        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.settings"),
            centerX, 35, Color.WHITE.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun toggleSetting(action: () -> Unit) {
        action()
        refreshWidgets()
    }

    private fun cycleGalleryColumns() {
        val current = NicephoreConfig.Client.getGalleryColumns()
        val next = when (current) {
            0 -> 2
            in 2..5 -> current + 1
            else -> 0
        }
        NicephoreConfig.Client.setGalleryColumns(next)
        refreshWidgets()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.settings")
    }
}
