package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;
import java.io.File;

public class DeleteConfirmScreen extends Screen {

    private final File file;
    private final Screen instanceToOpenIfDeleted;

    protected DeleteConfirmScreen(File file, Screen instanceToOpenIfDeleted) {
        super(new TranslationTextComponent(  "nicephore.gui.delete"));
        this.file = file;
        this.instanceToOpenIfDeleted = instanceToOpenIfDeleted;
    }

    @Override
    protected void init() {
        super.init();

        this.buttons.clear();
        this.addButton(new Button(this.width / 2 - 35, this.height / 2 + 30, 30, 20, new TranslationTextComponent("nicephore.gui.delete.yes"), button -> {
            deleteScreenshot();

            if (instanceToOpenIfDeleted != null){
                Minecraft.getInstance().setScreen(instanceToOpenIfDeleted);
            }
            else {
                onClose();
            }
        }));
        this.addButton(new Button(this.width / 2 + 5, this.height / 2 + 30, 30, 20, new TranslationTextComponent("nicephore.gui.delete.no"), button -> {
            onClose();
        }));

    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslationTextComponent("nicephore.gui.delete.question", file.getName()), this.width / 2, this.height / 2 - 20, Color.RED.getRGB());
    }

    private void deleteScreenshot(){
        if (this.file.delete()){
            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.deleted.success", file.getName()));
        }
        else{
            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.deleted.error", file.getName()));
        }
    }
}
