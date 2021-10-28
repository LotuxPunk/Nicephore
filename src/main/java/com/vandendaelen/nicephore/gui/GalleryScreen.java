package com.vandendaelen.nicephore.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.enums.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GalleryScreen extends Screen {
    private static final TranslatableComponent TITLE = new TranslatableComponent("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDirectory, "screenshots");
    private static final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    private static ArrayList<ResourceLocation> SCREENSHOT_TEXTURES = new ArrayList<>();
    private ArrayList<File> screenshots;
    private ArrayList<List<File>> pagesOfScreenshots;
    private int index;
    private float aspectRatio;

    private static final int ROW = 2;
    private static final int COLUMN = 4;
    private static final int IMAGES_TO_DISPLAY = ROW * COLUMN;

    public GalleryScreen(int index) {
        super(TITLE);
        this.index = index;
    }

    public GalleryScreen(){
        this(0);
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();

        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());
        pagesOfScreenshots = (ArrayList<List<File>>) Util.batches(screenshots,IMAGES_TO_DISPLAY).collect(Collectors.toList());
        index = getIndex();
        aspectRatio = 1.7777F;

        if (!screenshots.isEmpty()) {
            try {
                BufferedImage bimg = ImageIO.read(screenshots.get(index));
                final int width = bimg.getWidth();
                final int height = bimg.getHeight();
                bimg.getGraphics().dispose();
                aspectRatio = (float) (width / (double) height);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SCREENSHOT_TEXTURES.forEach(textureManager::release);
            SCREENSHOT_TEXTURES.clear();

            List <File> filesToLoad = pagesOfScreenshots.get(index);
            if (!filesToLoad.isEmpty()) {
                filesToLoad.forEach(file -> SCREENSHOT_TEXTURES.add( Util.fileToTexture(file)));
            } else {
                closeScreen("nicephore.screenshots.loading.error");
                return;
            }
        }

        this.clearWidgets();
        this.addRenderableWidget(new Button(10, 10, 100, 20, new TranslatableComponent("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()), button -> changeFilter()));

        if (!screenshots.isEmpty()) {
            this.addRenderableWidget(new Button(this.width / 2 - 80, this.height / 2 + 100, 20, 20, new TextComponent("<"), button -> modIndex(-1)));
            this.addRenderableWidget(new Button(this.width / 2 + 50, this.height / 2 + 100, 20, 20, new TextComponent(">"), button -> modIndex(1)));
        }
    }
    private void changeFilter(){
        ScreenshotFilter nextFilter = NicephoreConfig.Client.getScreenshotFilter().next();
        NicephoreConfig.Client.setScreenshotFilter(nextFilter);
        init();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        final int centerX = this.width / 2;
        final int imageWidth = (int) (this.width * 1.0/5);
        final int imageHeight = (int) (imageWidth / aspectRatio);

        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (pagesOfScreenshots.isEmpty()){
            drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslatableComponent("nicephore.screenshots.empty"), centerX, 20, Color.RED.getRGB());
            return;
        }

        final List<File> currentPage = pagesOfScreenshots.get(index);
        if (currentPage.stream().allMatch(File::exists)){
            SCREENSHOT_TEXTURES.forEach(TEXTURE -> {
                final int imageIndex = SCREENSHOT_TEXTURES.indexOf(TEXTURE);
                final String name = currentPage.get(imageIndex).getName();
                final TextComponent text = new TextComponent(StringUtils.abbreviate(name, 13));

                RenderSystem.setShaderTexture(0, TEXTURE);

                switch (imageIndex) {
                    case 0 -> {
                        blit(matrixStack, centerX - 15 - 2 * imageWidth, 50, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX - 15 - 2 * imageWidth, 55 + imageHeight, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 1 -> {
                        blit(matrixStack, centerX - 5 - imageWidth, 50, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX - 5 - imageWidth, 55 + imageHeight, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 2 -> {
                        blit(matrixStack, centerX + 5, 50, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX + 5, 55 + imageHeight, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 3 -> {
                        blit(matrixStack, centerX + 15 + imageWidth, 50, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX + 15 + imageWidth, 55 + imageHeight, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 4 -> {
                        blit(matrixStack, centerX - 15 - 2 * imageWidth, imageHeight + 80, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX - 15 - 2 * imageWidth, 2 * imageHeight + 85, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 5 -> {
                        blit(matrixStack, centerX - 5 - imageWidth, imageHeight + 80, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX - 5 - imageWidth, 2 * imageHeight + 85, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 6 -> {
                        blit(matrixStack, centerX + 5, imageHeight + 80, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX + 5, 2 * imageHeight + 85, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                    case 7 -> {
                        blit(matrixStack, centerX + 15 + imageWidth, imageHeight + 80, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
                        this.addRenderableWidget(new Button(centerX + 15 + imageWidth, 2 * imageHeight + 85, imageWidth, 20, text, button -> openScreenshotScreen(screenshots.indexOf(currentPage.get(imageIndex)))));
                    }
                }
            });

            drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslatableComponent("nicephore.gui.gallery.pages", index + 1, pagesOfScreenshots.size()), centerX, this.height / 2 + 85, Color.WHITE.getRGB());
        }
    }

    private void modIndex(int value){
        final int max = pagesOfScreenshots.size();
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

    private void openScreenshotScreen(int value){
        Minecraft.getInstance().pushGuiLayer(new ScreenshotScreen(value, index));
    }

    private int getIndex(){
        if (index >= pagesOfScreenshots.size() || index < 0){
            index = pagesOfScreenshots.size() - 1;
        }
        return index;
    }

    private void closeScreen(String textComponentId) {
        this.onClose();
        PlayerHelper.sendHotbarMessage(new TranslatableComponent(textComponentId));
    }

    public static boolean canBeShow(){
        return SCREENSHOTS_DIR.exists() && SCREENSHOTS_DIR.list().length > 0;
    }
}
