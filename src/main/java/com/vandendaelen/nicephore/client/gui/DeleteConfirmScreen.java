package com.vandendaelen.nicephore.client.gui;

import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;

public class DeleteConfirmScreen extends Screen {

    private final File file;
    private final Screen instanceToOpenIfDeleted;

    protected DeleteConfirmScreen(File file, Screen instanceToOpenIfDeleted) {
        super(Component.translatable("nicephore.gui.delete"));
        this.file = file;
        this.instanceToOpenIfDeleted = instanceToOpenIfDeleted;
    }

    @Override
    protected void init() {
        super.init();

        var confirmButton = Button.builder(Component.translatable("nicephore.gui.delete.yes"), button -> {
            deleteScreenshot();

            if (instanceToOpenIfDeleted != null) {
                Minecraft.getInstance().setScreen(instanceToOpenIfDeleted);
            } else {
                onClose();
            }
        }).bounds(this.width / 2 - 35, this.height / 2 + 30, 30, 20).build();

        var denyButton = Button.builder(Component.translatable("nicephore.gui.delete.no"), button -> onClose()).bounds(this.width / 2 + 5, this.height / 2 + 30, 30, 20).build();

        this.clearWidgets();
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(denyButton);

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("nicephore.gui.delete.question", file.getName()), this.width / 2, this.height / 2 - 20, Color.RED.getRGB());
    }

    private void deleteScreenshot() {
        if (this.file.exists() && this.file.delete()) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.success", file.getName()));
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.error", file.getName()));
        }
    }
}
