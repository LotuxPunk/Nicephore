package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

import java.awt.*;
import java.io.File;

public class DeleteConfirmScreen extends Screen {

    private final File file;
    private final Screen instanceToOpenIfDeleted;

    protected DeleteConfirmScreen(File file, Screen instanceToOpenIfDeleted) {
        super(new TranslatableComponent("nicephore.gui.delete"));
        this.file = file;
        this.instanceToOpenIfDeleted = instanceToOpenIfDeleted;
    }

    @Override
    protected void init() {
        super.init();

        this.clearWidgets();
        this.addRenderableWidget(new Button(this.width / 2 - 35, this.height / 2 + 30, 30, 20, new TranslatableComponent("nicephore.gui.delete.yes"), button -> {
            deleteScreenshot();

            if (instanceToOpenIfDeleted != null) {
                Minecraft.getInstance().setScreen(instanceToOpenIfDeleted);
            } else {
                onClose();
            }
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 2 + 30, 30, 20, new TranslatableComponent("nicephore.gui.delete.no"), button -> {
            onClose();
        }));

    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslatableComponent("nicephore.gui.delete.question", file.getName()), this.width / 2, this.height / 2 - 20, Color.RED.getRGB());
    }

    private void deleteScreenshot() {
        if (this.file.exists() && this.file.delete()) {
            PlayerHelper.sendMessage(new TranslatableComponent("nicephore.screenshot.deleted.success", file.getName()));
        } else {
            PlayerHelper.sendMessage(new TranslatableComponent("nicephore.screenshot.deleted.error", file.getName()));
        }
    }
}
