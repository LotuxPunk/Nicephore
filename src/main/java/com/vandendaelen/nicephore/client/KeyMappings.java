package com.vandendaelen.nicephore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.vandendaelen.nicephore.Nicephore;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public enum KeyMappings {
    COPY_KEY("copy", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, GLFW.GLFW_KEY_C),
    GUI_SCREENSHOT_KEY("screenshots.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, GLFW.GLFW_KEY_S),
    GUI_GALLERY_KEY("gallery.gui", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, GLFW.GLFW_KEY_G);

    private final KeyMapping key;

    KeyMappings(String localizationKey, KeyConflictContext context, KeyModifier modifier, int key) {
        this.key = new KeyMapping(Nicephore.MODID + ".keybinds." + localizationKey, context, modifier, getKey(key), Nicephore.MOD_NAME);
    }

    public boolean consumeClick() {
        return getKey().consumeClick();
    }

    private static InputConstants.Key getKey(final int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key);
    }

    public KeyMapping getKey() {
        return key;
    }
}
