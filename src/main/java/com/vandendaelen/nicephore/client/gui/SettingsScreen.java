package com.vandendaelen.nicephore.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class SettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("nicephore.gui.settings");

    protected SettingsScreen() {
        super(TITLE);
    }

    @Override
    public void render(@NotNull PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        final int centerX = this.width / 2;
        final int startingLine = this.width / 2 - 150;

        drawCenteredString(pPoseStack, Minecraft.getInstance().font, Component.translatable("nicephore.gui.settings"), centerX, 35, Color.WHITE.getRGB());

        this.clearWidgets();
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.exit"), button -> onClose()).bounds(this.width - 60, 10, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.showOptimisationStatus", NicephoreConfig.Client.getShouldShowOptStatus() ? "ON" : "OFF"), button -> changeShowOptimisationStatus(!NicephoreConfig.Client.getShouldShowOptStatus())).bounds(startingLine, 60, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.makeJPEGs", NicephoreConfig.Client.getJPEGToggle() ? "ON" : "OFF"), button -> changeMakeJPEGs(!NicephoreConfig.Client.getJPEGToggle())).bounds(startingLine, 90, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.screenshotCustomMessage", NicephoreConfig.Client.getScreenshotCustomMessage() ? "ON" : "OFF"), button -> changeScreenshotCustomMessage(!NicephoreConfig.Client.getScreenshotCustomMessage())).bounds(startingLine, 120, 300, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nicephore.screenshot.setScreenshotToClipboard", NicephoreConfig.Client.getScreenshotToClipboard() ? "ON" : "OFF"), button -> changeScreenshotToClipboard(!NicephoreConfig.Client.getScreenshotToClipboard())).bounds(startingLine, 150, 300, 20).build());

        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    private void changeShowOptimisationStatus(boolean value) {
        NicephoreConfig.Client.setShouldShowOptStatus(value);
    }

    private void changeMakeJPEGs(boolean value) {
        NicephoreConfig.Client.setJPEGToggle(value);
    }

    private void changeScreenshotCustomMessage(boolean value) {
        NicephoreConfig.Client.setScreenshotCustomMessage(value);
    }

    private void changeScreenshotToClipboard(boolean value) {
        NicephoreConfig.Client.setScreenshotToClipboard(value);
    }

//    private void changeFilter() {
//        ScreenshotFilter nextFilter = NicephoreConfig.Client.getScreenshotFilter().next();
//        NicephoreConfig.Client.setScreenshotFilter(nextFilter);
//        init();
//
//        if (listener != null) {
//            listener.onFilterChange(nextFilter);
//        }
//    }
}
