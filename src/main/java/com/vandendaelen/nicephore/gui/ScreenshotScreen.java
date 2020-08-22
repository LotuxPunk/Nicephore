package com.vandendaelen.nicephore.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Optional;

public class ScreenshotScreen extends Screen {
    public static final TranslationTextComponent TITLE = new TranslationTextComponent("nicephore.gui.screenshots");
    private static ResourceLocation image = new ResourceLocation("screenshot");

    public ScreenshotScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.minecraft.getTextureManager().bindTexture(Util.fileTotexture(new File(Minecraft.getInstance().gameDir, "screenshots\\2020-08-21_11.11.20.png")));

        float asp = 1.77777F;
        int centerX = 50 + ((100 - 25) / 2);

        int width = 150;
        int height = (int)(width / asp);
        blit(matrixStack, centerX - width / 2, 100, 0, 0, width, height, width, height);

    }

    @Override
    public Optional<IGuiEventListener> getEventListenerForPos(double mouseX, double mouseY) {
        return Optional.empty();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int p_231044_5_) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double p_231045_1_, double p_231045_3_, int p_231045_5_, double p_231045_6_, double p_231045_8_) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double p_231043_5_) {
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char p_231042_1_, int p_231042_2_) {
        return false;
    }

    @Override
    public void setFocusedDefault(@Nullable IGuiEventListener eventListener) {

    }

    @Override
    public void setListenerDefault(@Nullable IGuiEventListener eventListener) {

    }

    @Override
    public boolean changeFocus(boolean p_231049_1_) {
        return false;
    }
}
