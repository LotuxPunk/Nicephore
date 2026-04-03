package com.vandendaelen.nicephore.client

import com.mojang.blaze3d.platform.InputConstants
import com.vandendaelen.nicephore.Nicephore
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

val NICEPHORE_CATEGORY = KeyMapping.Category(Identifier.fromNamespaceAndPath(Nicephore.MODID, "key_mapping"))

enum class KeyMappings(val key: KeyMapping) {
    COPY_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.copy",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C),
            NICEPHORE_CATEGORY
        )
    ),
    GUI_SCREENSHOT_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.screenshots.gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_S),
            NICEPHORE_CATEGORY
        )
    ),
    GUI_GALLERY_KEY(
        KeyMapping(
            "${Nicephore.MODID}.keybinds.gallery.gui",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G),
            NICEPHORE_CATEGORY
        )
    );

    fun consumeClick(): Boolean = key.consumeClick()
}
