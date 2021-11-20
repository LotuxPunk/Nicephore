package com.vandendaelen.nicephore.event;

import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.gui.GalleryScreen;
import com.vandendaelen.nicephore.gui.ScreenshotScreen;
import com.vandendaelen.nicephore.thread.JPEGThread;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

@EventBusSubscriber(value = Dist.CLIENT, modid = Nicephore.MODID)
public final class ClientEvents {
    public static KeyBinding COPY_KEY;
    public static KeyBinding GUI_SCREENSHOT_KEY;
    public static KeyBinding GUI_GALLERY_KEY;

    static InputMappings.Input getKey(final int key) {
        return InputMappings.Type.KEYSYM.getOrCreate(key);
    }

    public static void init() {
        COPY_KEY = new KeyBinding(Nicephore.MODID + ".keybinds.copy", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_C), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(COPY_KEY);
        GUI_SCREENSHOT_KEY = new KeyBinding(Nicephore.MODID + ".keybinds.screenshots.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_S), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(GUI_SCREENSHOT_KEY);
        GUI_GALLERY_KEY = new KeyBinding(Nicephore.MODID + ".keybinds.gallery.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_G), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(GUI_GALLERY_KEY);
    }

    @SubscribeEvent
    public static void onKey(final InputUpdateEvent event) {
        if (COPY_KEY.consumeClick()) {
            final CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
            try {
                imageToClipBoard.copyLastScreenshot();
                PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.clipboard.success"));
            } catch (IOException e) {
                Nicephore.LOGGER.error(e.getMessage());
                PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.clipboard.error"));
            }
        }

        if (GUI_SCREENSHOT_KEY.consumeClick()){
            if (ScreenshotScreen.canBeShow()){
                Minecraft.getInstance().setScreen(new ScreenshotScreen());
            }
            else {
                PlayerHelper.sendHotbarMessage(new TranslationTextComponent("nicephore.screenshots.empty"));
            }
        }

        if (GUI_GALLERY_KEY.consumeClick()){
            if (GalleryScreen.canBeShow()){
                Minecraft.getInstance().setScreen(new GalleryScreen());
            }
            else {
                PlayerHelper.sendHotbarMessage(new TranslationTextComponent("nicephore.screenshots.empty"));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        final JPEGThread thread = new JPEGThread(event.getImage(), event.getScreenshotFile());
        thread.start();

        if (NicephoreConfig.Client.getScreenshotCustomMessage()) {
            event.setResultMessage(new StringTextComponent(""));
        }
    }

}
