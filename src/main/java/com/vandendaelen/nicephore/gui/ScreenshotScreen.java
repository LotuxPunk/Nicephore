package com.vandendaelen.nicephore.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ScreenshotScreen extends Screen {
    private static final TranslationTextComponent TITLE = new TranslationTextComponent("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDir, "screenshots");
    private static final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    private static ResourceLocation SCREENSHOT_TEXTURE;
    private ArrayList<File> screenshots;
    private int index;
    private float aspectRatio;

    public ScreenshotScreen() {
        super(TITLE);

        FilenameFilter filter = (dir, name) -> name.endsWith(".jpg") || name.endsWith(".png");

        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).collect(Collectors.toList());
        index = screenshots.size() - 1;
        aspectRatio = 1.7777F;
    }

    @Override
    protected void init() {
        super.init();

        BufferedImage bimg = null;
        try {
            bimg = ImageIO.read(screenshots.get(index));
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            bimg.getGraphics().dispose();
            aspectRatio = (float)(width/(double)height);
        } catch (IOException e) {
            e.printStackTrace();
        }

        textureManager.deleteTexture(SCREENSHOT_TEXTURE);
        SCREENSHOT_TEXTURE = Util.fileTotexture(screenshots.get(index));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        this.renderBackground(matrixStack);

        textureManager.bindTexture(SCREENSHOT_TEXTURE);

        int centerX = this.width / 2;
        int width = 150;
        int height = (int)(width / aspectRatio);
        blit(matrixStack, centerX - width / 2, 50, 0, 0, width, height, width, height);

        this.addButton(new Button(this.width / 2 + 50, this.height / 2 + 75, 20, 20, new StringTextComponent(">"), button -> modIndex(1)));
        this.addButton(new Button(this.width / 2 - 80, this.height / 2 + 75, 20, 20, new StringTextComponent("<"), button -> modIndex(-1)));

        drawCenteredString(matrixStack, Minecraft.getInstance().fontRenderer, TITLE.getUnformattedComponentText(), this.width / 2, (int) (this.height * 0.1), Color.WHITE.getRGB());
        drawCenteredString(matrixStack, Minecraft.getInstance().fontRenderer, new StringTextComponent(screenshots.get(index).getName()).getUnformattedComponentText(), this.width / 2 - 3, (int) (this.height * 0.7), Color.WHITE.getRGB());
    }

    private void modIndex(int value){
        int max = screenshots.size();
        if (index + value >= 0 && index + value < max){
            index += value;
        }
        init();
    }
}
