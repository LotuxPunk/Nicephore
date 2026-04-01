package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.ThumbnailCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class RenameScreen(
    private val file: File,
    private val galleryScreenPage: Int = -1
) : AbstractNicephoreScreen(Component.translatable("nicephore.gui.rename.title")) {

    private var nameField: EditBox? = null

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        val centerX = this.width / 2

        val baseName = file.nameWithoutExtension
        val editBox = EditBox(Minecraft.getInstance().font, centerX - 100, this.height / 2 - 10, 200, BUTTON_HEIGHT, Component.literal(baseName))
        editBox.value = baseName
        editBox.setMaxLength(255)
        nameField = editBox
        this.addRenderableWidget(editBox)

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.rename.save")) { doRename() }
                .bounds(centerX - 52, this.height / 2 + 20, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.rename.cancel")) { onClose() }
                .bounds(centerX + 3, this.height / 2 + 20, 50, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.rename.title"),
            this.width / 2, this.height / 2 - 30, Color.WHITE.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun doRename() {
        val newName = nameField?.value?.trim() ?: return
        if (newName.isEmpty() || newName.contains(Regex("[/\\\\:*?\"<>|]"))) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.rename.invalid"))
            return
        }

        val extension = file.extension
        val newFile = File(file.parentFile, "$newName.$extension")

        if (newFile.exists()) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.rename.exists"))
            return
        }

        if (file.renameTo(newFile)) {
            ThumbnailCache.removeThumbnail(file.name)
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.rename.success", newFile.name))
            Minecraft.getInstance().setScreen(
                if (galleryScreenPage > -1) GalleryScreen(galleryScreenPage) else ScreenshotScreen()
            )
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.rename.error"))
        }
    }
}
