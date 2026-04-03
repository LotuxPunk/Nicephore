package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.TrashManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class DeleteConfirmScreen(
    private val files: List<File>,
    private val instanceToOpenAfter: Screen?
) : AbstractNicephoreScreen(Component.translatable("nicephore.gui.delete")) {

    constructor(file: File, instanceToOpenAfter: Screen?) : this(listOf(file), instanceToOpenAfter)

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.yes")) {
                moveToTrash()
                if (instanceToOpenAfter != null) {
                    Minecraft.getInstance().setScreen(instanceToOpenAfter)
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
        val message = if (files.size == 1) {
            Component.translatable("nicephore.gui.trash.move.single")
        } else {
            Component.translatable("nicephore.gui.trash.move.confirm", files.size)
        }

        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            message,
            this.width / 2, this.height / 2 - 20, Color.RED.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun moveToTrash() {
        val count = TrashManager.moveToTrash(files)
        if (count > 0) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.success", count))
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.error", files.firstOrNull()?.name ?: ""))
        }
    }
}
