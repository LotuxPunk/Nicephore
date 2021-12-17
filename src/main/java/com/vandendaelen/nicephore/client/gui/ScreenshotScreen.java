package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.FilterListener;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScreenshotScreen extends Screen {
    private static final TranslationTextComponent TITLE = new TranslationTextComponent("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDirectory, "screenshots");
    private static DynamicTexture SCREENSHOT_TEXTURE;
    private ArrayList<File> screenshots;
    private int index;
    private float aspectRatio;
    private final int galleryScreenPage;
    private FilterListener listener;

    public ScreenshotScreen(int index, int galleryScreenPage, FilterListener listener) {
        super(TITLE);
        this.index = index;
        this.galleryScreenPage = galleryScreenPage;
        this.listener = listener;
    }

    public ScreenshotScreen(int index) {
        this(index, -1, null);
    }

    public ScreenshotScreen() {
        this(0, -1, null);
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();
        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());

        index = getIndex();
        aspectRatio = 1.7777F;

        if (!screenshots.isEmpty()) {
            try(ImageInputStream in = ImageIO.createImageInputStream(screenshots.get(index))){
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        aspectRatio = (float)(reader.getWidth(0)/ (float) reader.getHeight(0));
                    } finally {
                        reader.dispose();
                    }
                }
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

        if (listener != null){
            listener.onFilterChange(nextFilter);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        final int centerX = this.width / 2;
        final int width = (int) (this.width * 0.5);
        final int height = (int) (width / aspectRatio);

        this.renderBackground(matrixStack);

        this.buttons.clear();
        this.addButton(new Button(10, 10, 100, 20, new TranslationTextComponent("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()), button -> changeFilter()));
        this.addButton(new Button(this.width - 60, 10, 50, 20, new TranslationTextComponent("nicephore.screenshot.exit"), button -> onClose()));

        if (!screenshots.isEmpty()) {
            this.addButton(new Button(this.width / 2 - 80, this.height / 2 + 75, 20, 20, new StringTextComponent("<"), button -> modIndex(-1)));
            this.addButton(new Button(this.width / 2 + 60, this.height / 2 + 75, 20, 20, new StringTextComponent(">"), button -> modIndex(1)));

            Button copyButton = new Button(this.width / 2 - 52, this.height / 2 + 75, 50, 20, new TranslationTextComponent("nicephore.gui.screenshots.copy"), button -> {
                final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
                try {
                    final File screenshot = screenshots.get(index);
                    imageToClipBoard.copyImage(ImageIO.read(screenshot));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            copyButton.active = !Minecraft.ON_OSX && Objects.equals(System.getProperty("java.awt.headless"), "false");
            if(!copyButton.active && (mouseX >= (double)copyButton.x && mouseY >= (double)copyButton.y && mouseX < (double)(copyButton.x + copyButton.getWidth()) && mouseY < (double)(copyButton.y + copyButton.getHeight()))) {
                renderComponentTooltip(matrixStack, Arrays.asList(new TranslationTextComponent("nicephore.gui.screenshots.copy.unable").withStyle(TextFormatting.RED)), mouseX, mouseY);
            }
            this.addButton(copyButton);

            this.addButton(new Button(this.width / 2 + 3, this.height / 2 + 75, 50, 20, new TranslationTextComponent("nicephore.gui.screenshots.delete"), button -> deleteScreenshot(screenshots.get(index))));
        }

        if (screenshots.isEmpty()){
            drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslationTextComponent("nicephore.screenshots.empty"), centerX, 20, Color.RED.getRGB());
        }
        else {
            final File currentScreenshot = screenshots.get(index);
            if (currentScreenshot.exists()){

                RenderSystem.bindTexture(SCREENSHOT_TEXTURE.getId());
                RenderSystem.enableBlend();
                blit(matrixStack, centerX - width / 2, 50, 0, 0, width, height, width, height);
                RenderSystem.disableBlend();

                drawCenteredString(matrixStack, Minecraft.getInstance().font, new TranslationTextComponent("nicephore.gui.screenshots.pages", index + 1, screenshots.size()), centerX, 20, Color.WHITE.getRGB());
                drawCenteredString(matrixStack, Minecraft.getInstance().font, new StringTextComponent(MessageFormat.format("{0} ({1})", currentScreenshot.getName(), getFileSizeMegaBytes(currentScreenshot))).getContents(), centerX, 35, Color.WHITE.getRGB());
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
        this.onClose();
        PlayerHelper.sendHotbarMessage(new TranslationTextComponent(textComponentId));
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

    @Override
    public void onClose() {
        if (SCREENSHOT_TEXTURE != null){
            SCREENSHOT_TEXTURE.close();
        }

        super.onClose();
    }
}
