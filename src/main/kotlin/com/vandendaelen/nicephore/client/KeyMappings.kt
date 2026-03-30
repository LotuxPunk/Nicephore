package com.vandendaelen.nicephore.client

import com.mojang.blaze3d.platform.InputConstants
import com.vandendaelen.nicephore.Nicephore
import net.minecraft.client.KeyMapping
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

enum class KeyMappings(val key: KeyMapping) {
    COPY_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.copy",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C),
            Nicephore.MOD_NAME
        )
    ),
    GUI_SCREENSHOT_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.screenshots.gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_S),
            Nicephore.MOD_NAME
        )
    ),
    GUI_GALLERY_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.gallery.gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G),
            Nicephore.MOD_NAME
        )
    );

    fun consumeClick(): Boolean = key.consumeClick()
}
