package com.vandendaelen.nicephore.fabric.platform

import com.mojang.blaze3d.platform.InputConstants
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.KeybindingDescriptor
import com.vandendaelen.nicephore.platform.KeybindingRegistry
import com.vandendaelen.nicephore.platform.Modifier
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import net.minecraft.client.Minecraft

class FabricKeybindingRegistry : KeybindingRegistry {

    private data class Entry(val descriptor: KeybindingDescriptor, val mapping: KeyMapping)
    private val mappings: MutableMap<String, Entry> = mutableMapOf()

    private val category = KeyMapping.Category(
        Identifier.fromNamespaceAndPath(Nicephore.MODID, "key_mapping")
    )

    override fun register(descriptors: List<KeybindingDescriptor>) {
        descriptors.forEach { d ->
            val mapping = KeyMapping(
                d.translationKey,
                InputConstants.Type.KEYSYM,
                d.defaultKey,
                category,
            )
            KeyMappingHelper.registerKeyMapping(mapping)
            mappings[d.id] = Entry(d, mapping)
        }
    }

    override fun consumeClick(id: String): Boolean {
        val entry = mappings[id] ?: return false
        if (!isModifierHeld(entry.descriptor.modifier)) return false
        return entry.mapping.consumeClick()
    }

    private fun isModifierHeld(modifier: Modifier): Boolean {
        if (modifier == Modifier.NONE) return true
        val window = Minecraft.getInstance().window.handle()
        return when (modifier) {
            Modifier.CTRL ->
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
            Modifier.SHIFT ->
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
            Modifier.ALT ->
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS
            Modifier.NONE -> true
        }
    }
}
