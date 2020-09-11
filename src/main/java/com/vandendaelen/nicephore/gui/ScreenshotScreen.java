package com.vandendaelen.nicephore.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
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
    private static int index;
    private float aspectRatio;

    public ScreenshotScreen() {
        super(TITLE);

        FilenameFilter filter = (dir, name) -> name.endsWith(".jpg") || name.endsWith(".png");

        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).collect(Collectors.toList());
        index = getIndex();
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

        this.buttons.clear();
        this.addButton(new Button(this.width / 2 + 50, this.height / 2 + 75, 20, 20, new StringTextComponent(">"), button -> modIndex(1)));
        this.addButton(new Button(this.width / 2 - 80, this.height / 2 + 75, 20, 20, new StringTextComponent("<"), button -> modIndex(-1)));
        this.addButton(new Button(this.width / 2 - 30, this.height / 2 + 75, 50, 20, new TranslationTextComponent("nicephore.gui.screenshots.copy"), new Button.IPressable() {
            @Override
            public void onPress(Button button) {
                final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
                try {
                    imageToClipBoard.copyImage(ImageIO.read(screenshots.get(index)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        textureManager.bindTexture(SCREENSHOT_TEXTURE);

        int centerX = this.width / 2;
        int width = (int) (this.width * 0.5);
        int height = (int)(width / aspectRatio);
        blit(matrixStack, centerX - width / 2, 50, 0, 0, width, height, width, height);

        drawCenteredString(matrixStack, Minecraft.getInstance().fontRenderer, new TranslationTextComponent("nicephore.gui.screenshots.pages", index + 1, screenshots.size()), this.width / 2, 30, Color.WHITE.getRGB());
        drawCenteredString(matrixStack, Minecraft.getInstance().fontRenderer, new TranslationTextComponent("nicephore.gui.screenshots"), this.width / 2, 20, Color.WHITE.getRGB());
        drawCenteredString(matrixStack, Minecraft.getInstance().fontRenderer, new StringTextComponent(screenshots.get(index).getName()).getUnformattedComponentText(), this.width / 2, (int) (this.height * 0.9), Color.WHITE.getRGB());
    }

    private void modIndex(int value){
        int max = screenshots.size();
        if (index + value >= 0 && index + value < max){
            index += value;
        }
        else {
            if (index + value < 0){
                index = max - 1;
            }
            else {
                index = 0;
            }
        }

        init();
    }

    private int getIndex(){
        if (index >= screenshots.size()){
            index = screenshots.size() - 1;
        }
        return index;
    }

    public static boolean canBeShow(){
        return SCREENSHOTS_DIR.list().length > 0;
    }
}