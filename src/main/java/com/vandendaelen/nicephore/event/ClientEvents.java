package com.vandendaelen.nicephore.event;

import com.vandendaelen.nicephore.JPEGThread;
import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
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
public class ClientEvents {
    public static KeyBinding COPY_KEY;

    static InputMappings.Input getKey(int key) {
        return InputMappings.Type.KEYSYM.getOrMakeInput(key);
    }

    public static void init() {
        COPY_KEY = new KeyBinding(Nicephore.MODID + ".keybinds.copy", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_V), Nicephore.MOD_NAME);
        ClientRegistry.registerKeyBinding(COPY_KEY);
    }

    @SubscribeEvent
    public static void onKey(InputUpdateEvent event) {
        if(COPY_KEY.isPressed()){
            CopyImageToClipBoard imageToClipBoard = new CopyImageToClipBoard();
            try {
                imageToClipBoard.copyLastScreenshot();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        JPEGThread thread = new JPEGThread(event.getImage(), event.getScreenshotFile());
        thread.start();
    }

}
