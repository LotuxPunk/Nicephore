package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.enums.OperatingSystems;
import com.vandendaelen.nicephore.enums.ScreenshotFilter;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.FilterListener;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

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
import java.util.List;
import java.util.stream.Collectors;

public class ScreenshotScreen extends Screen {
    private static final Component TITLE = Component.translatable("nicephore.gui.screenshots");
    private static final File SCREENSHOTS_DIR = new File(Minecraft.getInstance().gameDirectory, "screenshots");
    private static DynamicTexture SCREENSHOT_TEXTURE;
    private final int galleryScreenPage;
    private ArrayList<File> screenshots;
    private int index;
    private float aspectRatio;
    private final FilterListener listener;

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

    public static boolean canBeShow() {
        return SCREENSHOTS_DIR.exists() && SCREENSHOTS_DIR.list().length > 0;
    }

    private static String getFileSizeMegaBytes(File file) {
        final double size = FileUtils.sizeOf(file);
        final NumberFormat formatter = new DecimalFormat("#0.00");
        final int MB_SIZE = 1024 * 1024;
        final int KB_SIZE = 1024;

        if (size > MB_SIZE) {
            return MessageFormat.format("{0} MB", formatter.format((double) FileUtils.sizeOf(file) / MB_SIZE));
        }
        return MessageFormat.format("{0} KB", formatter.format((double) FileUtils.sizeOf(file) / KB_SIZE));
    }

    @Override
    protected void init() {
        super.init();

        FilenameFilter filter = NicephoreConfig.Client.getScreenshotFilter().getPredicate();
        screenshots = (ArrayList<File>) Arrays.stream(SCREENSHOTS_DIR.listFiles(filter)).sorted(Comparator.comparingLong(File::lastModified).reversed()).collect(Collectors.toList());

        index = getIndex();
        aspectRatio = 1.7777F;

        if (!screenshots.isEmpty()) {
            try (ImageInputStream in = ImageIO.createImageInputStream(screenshots.get(index))) {
                final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in);
                        aspectRatio = (float) (reader.getWidth(0) / (float) reader.getHeight(0));
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (SCREENSHOT_TEXTURE != null) {
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

    private void changeFilter() {
        ScreenshotFilter nextFilter = NicephoreConfig.Client.getScreenshotFilter().next();
        NicephoreConfig.Client.setScreenshotFilter(nextFilter);
        init();

        if (listener != null) {
            listener.onFilterChange(nextFilter);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        final int centerX = this.minecraft.getWindow().getGuiScaledWidth() / 2;
        final int pictureMidWith = (int) (this.minecraft.getWindow().getGuiScaledWidth() * 0.5 * 1.2);
        final int pictureHeight = (int) (pictureMidWith / aspectRatio);
        final int bottomLine = this.minecraft.getWindow().getGuiScaledHeight() - 30;

        this.renderBackground(guiGraphics);

        this.clearWidgets();
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name()), button -> changeFilter()).bounds(10, 10, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.exit"), button -> onClose()).bounds(this.minecraft.getWindow().getGuiScaledWidth() - 60, 10, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.gui.settings"), button -> openSettingsScreen()).bounds(this.width - 120, 10, 50, 20).build());

        if (!screenshots.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> modIndex(-1)).bounds(this.minecraft.getWindow().getGuiScaledWidth() / 2 - 80, bottomLine, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> modIndex(1)).bounds(this.minecraft.getWindow().getGuiScaledWidth() / 2 + 60, bottomLine, 20, 20).build());

            Button copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy"), button -> {
                final File screenshot = screenshots.get(index);
                if (CopyImageToClipBoard.getInstance().copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"));
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"));
                }
            }).bounds(this.minecraft.getWindow().getGuiScaledWidth() / 2 - 52, bottomLine, 50, 20).build();

            copyButton.active = OperatingSystems.getOS().getManager() != null;
            if (!copyButton.isActive() && (mouseX >= (double) copyButton.getX() && mouseY >= (double) copyButton.getY() && mouseX < (double) (copyButton.getX() + copyButton.getWidth()) && mouseY < (double) (copyButton.getY() + copyButton.getHeight()))) {
                guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(Component.translatable("nicephore.gui.screenshots.copy.unable").withStyle(ChatFormatting.RED)), mouseX, mouseY);
            }
            this.addRenderableWidget(copyButton);

            this.addRenderableWidget(Button.builder(Component.translatable("nicephore.gui.screenshots.delete"), button -> deleteScreenshot(screenshots.get(index))).bounds(this.minecraft.getWindow().getGuiScaledWidth() / 2 + 3, bottomLine, 50, 20).build());
        }

        if (screenshots.isEmpty()) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("nicephore.screenshots.empty"), centerX, 20, Color.RED.getRGB());
        } else {
            final File currentScreenshot = screenshots.get(index);
            if (currentScreenshot.exists()) {
                TextureManager tm = this.minecraft.getTextureManager();
                guiGraphics.blit(tm.register("screenshot", SCREENSHOT_TEXTURE), centerX - (int) (pictureMidWith) / 2, 50, 0, 0, pictureMidWith, pictureHeight, pictureMidWith, pictureHeight);

                guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("nicephore.gui.screenshots.pages", index + 1, screenshots.size()), centerX, 20, Color.WHITE.getRGB());
                guiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.literal(MessageFormat.format("{0} ({1})", currentScreenshot.getName(), getFileSizeMegaBytes(currentScreenshot))), centerX, 35, Color.WHITE.getRGB());
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void modIndex(int value) {
        final int max = screenshots.size();
        if (index + value >= 0 && index + value < max) {
            index += value;
        } else {
            if (index + value < 0) {
                index = max - 1;
            } else {
                index = 0;
            }
        }
        init();
    }

    private void deleteScreenshot(File file) {
        Minecraft.getInstance().pushGuiLayer(new DeleteConfirmScreen(file, galleryScreenPage > -1 ? new GalleryScreen(this.galleryScreenPage) : new ScreenshotScreen(index)));
    }

    private void openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(new SettingsScreen());
    }

    private int getIndex() {
        if (index >= screenshots.size() || index < 0) {
            index = screenshots.size() - 1;
        }
        return index;
    }

    private void closeScreen(String textComponentId) {
        this.onClose();
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId));
    }

    @Override
    public void onClose() {
        if (SCREENSHOT_TEXTURE != null) {
            SCREENSHOT_TEXTURE.close();
        }

        super.onClose();
    }
}
