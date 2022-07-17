package com.vandendaelen.nicephore.client.event;

import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.client.KeyMappings;
import com.vandendaelen.nicephore.client.gui.GalleryScreen;
import com.vandendaelen.nicephore.client.gui.ScreenshotScreen;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.thread.InitThread;
import com.vandendaelen.nicephore.thread.ScreenshotThread;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Nicephore.MODID)
public final class ClientForgeBusEvents {

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        final InitThread initThread = new InitThread(NicephoreConfig.Client.getOptimisedOutputToggle());
        initThread.start();
    }

    @SubscribeEvent
    public static void onKey(final InputEvent.Key event) {
        if (KeyMappings.COPY_KEY.consumeClick()) {
            if (CopyImageToClipBoard.getInstance().copyLastScreenshot()) {
                PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"));
            } else {
                PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"));
            }
        } else if (KeyMappings.GUI_SCREENSHOT_KEY.consumeClick()) {
            if (ScreenshotScreen.canBeShow()) {
                Minecraft.getInstance().setScreen(new ScreenshotScreen());
            } else {
                PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"));
            }
        } else if (KeyMappings.GUI_GALLERY_KEY.consumeClick()) {
            if (GalleryScreen.canBeShow()) {
                Minecraft.getInstance().setScreen(new GalleryScreen());
            } else {
                PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        final ScreenshotThread thread = new ScreenshotThread(event.getImage(), event.getScreenshotFile());
        thread.start();

        if (NicephoreConfig.Client.getScreenshotCustomMessage()) {
            event.setResultMessage(Component.literal(""));
        }
    }

}
