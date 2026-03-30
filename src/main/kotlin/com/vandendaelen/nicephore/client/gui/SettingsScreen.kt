package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color

class SettingsScreen : Screen(TITLE) {

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks)
        val centerX = this.width / 2
        val startingLine = this.width / 2 - 150

        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.settings"),
            centerX, 35, Color.WHITE.rgb
        )

        this.clearWidgets()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - 60, 10, 50, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.showOptimisationStatus", if (NicephoreConfig.Client.getShouldShowOptStatus()) "ON" else "OFF")
            ) { changeShowOptimisationStatus(!NicephoreConfig.Client.getShouldShowOptStatus()) }
                .bounds(startingLine, 60, 300, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.makeJPEGs", if (NicephoreConfig.Client.getJPEGToggle()) "ON" else "OFF")
            ) { changeMakeJPEGs(!NicephoreConfig.Client.getJPEGToggle()) }
                .bounds(startingLine, 90, 300, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.screenshotCustomMessage", if (NicephoreConfig.Client.getScreenshotCustomMessage()) "ON" else "OFF")
            ) { changeScreenshotCustomMessage(!NicephoreConfig.Client.getScreenshotCustomMessage()) }
                .bounds(startingLine, 120, 300, 20).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.setScreenshotToClipboard", if (NicephoreConfig.Client.getScreenshotToClipboard()) "ON" else "OFF")
            ) { changeScreenshotToClipboard(!NicephoreConfig.Client.getScreenshotToClipboard()) }
                .bounds(startingLine, 150, 300, 20).build()
        )

        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun changeShowOptimisationStatus(value: Boolean) {
        NicephoreConfig.Client.setShouldShowOptStatus(value)
    }

    private fun changeMakeJPEGs(value: Boolean) {
        NicephoreConfig.Client.setJPEGToggle(value)
    }

    private fun changeScreenshotCustomMessage(value: Boolean) {
        NicephoreConfig.Client.setScreenshotCustomMessage(value)
    }

    private fun changeScreenshotToClipboard(value: Boolean) {
        NicephoreConfig.Client.setScreenshotToClipboard(value)
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.settings")
    }
}
