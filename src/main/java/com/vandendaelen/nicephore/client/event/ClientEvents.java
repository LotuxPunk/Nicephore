package com.vandendaelen.nicephore.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.client.gui.GalleryScreen;
import com.vandendaelen.nicephore.client.gui.ScreenshotScreen;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.thread.JPEGThread;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

@EventBusSubscriber(value = Dist.CLIENT, modid = Nicephore.MODID)
public final class ClientEvents {
    public static KeyMapping COPY_KEY;
    public static KeyMapping GUI_SCREENSHOT_KEY;
    public static KeyMapping GUI_GALLERY_KEY;

    static InputConstants.Key getKey(final int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key);
    }

    public static void init() {
        COPY_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.copy", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_C), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(COPY_KEY);
        GUI_SCREENSHOT_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.screenshots.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_S), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(GUI_SCREENSHOT_KEY);
        GUI_GALLERY_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.gallery.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_G), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(GUI_GALLERY_KEY);
    }

    @SubscribeEvent
    public static void onKey(final InputEvent.KeyInputEvent event) {
        if (COPY_KEY.consumeClick()) {
            final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
            try {
                imageToClipBoard.copyLastScreenshot();
                PlayerHelper.sendMessage(new TranslatableComponent("nicephore.clipboard.success"));
            } catch (IOException e) {
                Nicephore.LOGGER.error(e.getMessage());
                PlayerHelper.sendMessage(new TranslatableComponent("nicephore.clipboard.error"));
            }
        }

        if (GUI_SCREENSHOT_KEY.consumeClick()){
            if (ScreenshotScreen.canBeShow()){
                Minecraft.getInstance().setScreen(new ScreenshotScreen());
            }
            else {
                PlayerHelper.sendHotbarMessage(new TranslatableComponent("nicephore.screenshots.empty"));
            }
        }

        if (GUI_GALLERY_KEY.consumeClick()){
            if (GalleryScreen.canBeShow()){
                Minecraft.getInstance().setScreen(new GalleryScreen());
            }
            else {
                PlayerHelper.sendHotbarMessage(new TranslatableComponent("nicephore.screenshots.empty"));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        final JPEGThread thread = new JPEGThread(event.getImage(), event.getScreenshotFile());
        thread.start();

        if (NicephoreConfig.Client.getScreenshotCustomMessage()) {
            event.setResultMessage(new TextComponent(""));
        }
    }

}
