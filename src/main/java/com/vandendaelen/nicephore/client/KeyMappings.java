package com.vandendaelen.nicephore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.vandendaelen.nicephore.Nicephore;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {
    public static final KeyMapping COPY_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.copy", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_C), Nicephore.MOD_NAME);
    public static final KeyMapping GUI_SCREENSHOT_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.screenshots.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_S), Nicephore.MOD_NAME);
    public static final KeyMapping GUI_GALLERY_KEY = new KeyMapping(Nicephore.MODID + ".keybinds.gallery.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, getKey(GLFW.GLFW_KEY_G), Nicephore.MOD_NAME);

    private static InputConstants.Key getKey(final int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key);
    }
}
