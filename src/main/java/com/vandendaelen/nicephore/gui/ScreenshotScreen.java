package com.vandendaelen.nicephore.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.enums.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ScreenshotScreen extends Screen {
    private static final TranslatableComponent TITLE = new TranslatableComponent("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDirectory, "screenshots");
    private static DynamicTexture SCREENSHOT_TEXTURE;
    private ArrayList<File> screenshots;
    private int index;
    private float aspectRatio;
    private final int galleryScreenPage;

    public ScreenshotScreen(int index, int galleryScreenPage) {
        super(TITLE);
        this.index = index;
        this.galleryScreenPage = galleryScreenPage;
    }

    public ScreenshotScreen(int index) {
        this(index, -1);
    }

    public ScreenshotScreen() {
        this(0, -1);
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();
        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());

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

            if (SCREENSHOT_TEXTURE != null){
                SCREENSHOT_TEXTURE.close();
            }

            File fileToLoad = screenshots.get(index);
            if (fileToLoad.exists()) {
                SCREENSHOT_TEXTURE = Util.fileToTexture(screenshots.get(index));
            } else {
                closeScreen("nicephore.screenshots.loading.error");
                return;
            }
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
        final int width = (int) (this.width * 0.5);
        final int height = (int) (width / aspectRatio);

        this.renderBackground(matrixStack);

        this.clearWidgets();
        this.addRenderableWidget(new Button(10, 10, 100, 20, new TranslatableComponent("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()), button -> changeFilter()));
        this.addRenderableWidget(new Button(this.width - 60, 10, 50, 20, new TranslatableComponent("nicephore.screenshot.exit"), button -> onClose()));

        if (!screenshots.isEmpty()) {
            this.addRenderableWidget(new Button(this.width / 2 - 80, this.height / 2 + 75, 20, 20, new TextComponent("<"), button -> modIndex(-1)));
            this.addRenderableWidget(new Button(this.width / 2 + 60, this.height / 2 + 75, 20, 20, new TextComponent(">"), button -> modIndex(1)));
            this.addRenderableWidget(new Button(this.width / 2 - 52, this.height / 2 + 75, 50, 20, new TranslatableComponent("nicephore.gui.screenshots.copy"), button -> {
                final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
                try {
                    imageToClipBoard.copyImage(ImageIO.read(screenshots.get(index)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            this.addRenderableWidget(new Button(this.width / 2 + 3, this.height / 2 + 75, 50, 20, new TranslatableComponent("nicephore.gui.screenshots.delete"), button -> deleteScreenshot(screenshots.get(index))));
        }

        if (screenshots.isEmpty()){
            drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslatableComponent("nicephore.screenshots.empty"), centerX, 20, Color.RED.getRGB());
        }
        else {
            final File currentScreenshot = screenshots.get(index);
            if (currentScreenshot.exists()){

                RenderSystem.setShaderTexture(0, SCREENSHOT_TEXTURE.getId());
                RenderSystem.enableBlend();
                blit(matrixStack, centerX - width / 2, 50, 0, 0, width, height, width, height);
                RenderSystem.disableBlend();

                drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslatableComponent("nicephore.gui.screenshots.pages", index + 1, screenshots.size()), centerX, 20, Color.WHITE.getRGB());
                drawCenteredString(matrixStack, Minecraft.getInstance().font, new TextComponent(MessageFormat.format("{0} ({1})", currentScreenshot.getName(), getFileSizeMegaBytes(currentScreenshot))).getContents(), centerX, 35, Color.WHITE.getRGB());
            }
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void modIndex(int value){
        final int max = screenshots.size();
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

    private void deleteScreenshot(File file){
        Minecraft.getInstance().pushGuiLayer(new DeleteConfirmScreen(file, galleryScreenPage > -1 ? new GalleryScreen(this.galleryScreenPage) : new ScreenshotScreen(index)));
    }

    private int getIndex(){
        if (index >= screenshots.size() || index < 0){
            index = screenshots.size() - 1;
        }
        return index;
    }

    private void closeScreen(String textComponentId) {
        if (SCREENSHOT_TEXTURE != null){
            SCREENSHOT_TEXTURE.close();
        }

        this.onClose();
        PlayerHelper.sendHotbarMessage(new TranslatableComponent(textComponentId));
    }

    public static boolean canBeShow(){
        return SCREENSHOTS_DIR.exists() && SCREENSHOTS_DIR.list().length > 0;
    }

    private static String getFileSizeMegaBytes(File file) {
        final double size = FileUtils.sizeOf(file);
        final NumberFormat formatter = new DecimalFormat("#0.00");
        final int MB_SIZE = 1024 * 1024;
        final int KB_SIZE = 1024;

        if (size > MB_SIZE){
            return MessageFormat.format("{0} MB", formatter.format((double) FileUtils.sizeOf(file) / MB_SIZE));
        }
        return MessageFormat.format("{0} KB", formatter.format((double) FileUtils.sizeOf(file) / KB_SIZE));
    }
}
