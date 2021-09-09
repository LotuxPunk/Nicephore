package com.vandendaelen.nicephore.gui;

import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
    private static final TranslationTextComponent TITLE = new TranslationTextComponent("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDir, "screenshots");
    private static final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
    private static ResourceLocation SCREENSHOT_TEXTURE;
    private ArrayList<File> screenshots;
    private static int index;
    private float aspectRatio;

    public ScreenshotScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();

        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());
        index = getIndex();
        aspectRatio = 1.7777F;

        if (!screenshots.isEmpty()) {

            BufferedImage bimg = null;
            try {
                bimg = ImageIO.read(screenshots.get(index));
                final int width = bimg.getWidth();
                final int height = bimg.getHeight();
                bimg.getGraphics().dispose();
                aspectRatio = (float) (width / (double) height);
            } catch (IOException e) {
                e.printStackTrace();
            }

            textureManager.deleteTexture(SCREENSHOT_TEXTURE);

            File fileToLoad = screenshots.get(index);
            if (fileToLoad.exists()) {
                SCREENSHOT_TEXTURE = Util.fileTotexture(screenshots.get(index));
            } else {
                closeScreen("nicephore.screenshots.loading.error");
                return;
            }
        }

        this.buttons.clear();
        this.addButton(new Button(10, 10, 100, 20, new TranslationTextComponent("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()).getUnformattedComponentText(), button -> changeFilter()));

        if (!screenshots.isEmpty()) {
            this.addButton(new Button(this.width / 2 + 50, this.height / 2 + 75, 20, 20, new StringTextComponent(">").getUnformattedComponentText(), button -> modIndex(1)));
            this.addButton(new Button(this.width / 2 - 80, this.height / 2 + 75, 20, 20, new StringTextComponent("<").getUnformattedComponentText(), button -> modIndex(-1)));
            this.addButton(new Button(this.width / 2 - 55, this.height / 2 + 75, 50, 20, new TranslationTextComponent("nicephore.gui.screenshots.copy").getUnformattedComponentText(), button -> {
                final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
                try {
                    imageToClipBoard.copyImage(ImageIO.read(screenshots.get(index)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            this.addButton(new Button(this.width / 2 - 5, this.height / 2 + 75, 50, 20, new TranslationTextComponent("nicephore.gui.screenshots.delete").getUnformattedComponentText(), button -> deleteScreenshot(screenshots.get(index))));
        }
    }
    private void changeFilter(){
        ScreenshotFilter nextFilter = NicephoreConfig.Client.getScreenshotFilter().next();
        NicephoreConfig.Client.setScreenshotFilter(nextFilter);
        init();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        final int centerX = this.width / 2;
        final int width = (int) (this.width * 0.5);
        final int height = (int) (width / aspectRatio);

        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);

        if (screenshots.isEmpty()) {
            drawCenteredString(Minecraft.getInstance().fontRenderer, new TranslationTextComponent("nicephore.screenshots.empty").getUnformattedComponentText(), centerX, 20, Color.RED.getRGB());
            return;
        }

        textureManager.bindTexture(SCREENSHOT_TEXTURE);
        blit(centerX - width / 2, 50, 0, 0, width, height, width, height);

        drawCenteredString(Minecraft.getInstance().fontRenderer, new TranslationTextComponent("nicephore.gui.screenshots.pages", index + 1, screenshots.size()).getUnformattedComponentText(), centerX, 20, Color.WHITE.getRGB());
        drawCenteredString(Minecraft.getInstance().fontRenderer, new StringTextComponent(MessageFormat.format("{0} ({1})", screenshots.get(index).getName(), getFileSizeMegaBytes(screenshots.get(index)))).getUnformattedComponentText(), centerX, 35, Color.WHITE.getRGB());
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
        Minecraft.getInstance().displayGuiScreen(new DeleteConfirmScreen(file));
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
}
