package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class DeleteConfirmScreen(
    private val file: File,
    private val instanceToOpenIfDeleted: Screen?
) : AbstractNicephoreScreen(Component.translatable("nicephore.gui.delete")) {

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.yes")) {
                deleteScreenshot()
                if (instanceToOpenIfDeleted != null) {
                    Minecraft.getInstance().setScreen(instanceToOpenIfDeleted)
                } else {
                    onClose()
                }
            }.bounds(this.width / 2 - 35, this.height / 2 + 30, 30, BUTTON_HEIGHT).build()
        )

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.no")) {
                onClose()
            }.bounds(this.width / 2 + 5, this.height / 2 + 30, 30, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.delete.question", file.name),
            this.width / 2, this.height / 2 - 20, Color.RED.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun deleteScreenshot() {
        if (file.exists() && file.delete()) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.success", file.name))
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.error", file.name))
        }
    }
}
