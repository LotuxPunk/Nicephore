package com.vandendaelen.nicephore.forge.platform

import com.mojang.blaze3d.platform.InputConstants
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.KeybindingDescriptor
import com.vandendaelen.nicephore.platform.KeybindingRegistry
import com.vandendaelen.nicephore.platform.Modifier
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.eventbus.api.listener.SubscribeEvent
import net.minecraftforge.fml.common.Mod

class ForgeKeybindingRegistry : KeybindingRegistry {
    override fun register(descriptors: List<KeybindingDescriptor>) {
        pending = descriptors
    }

    override fun consumeClick(id: String): Boolean = mappings[id]?.consumeClick() ?: false

    companion object {
        internal val mappings: MutableMap<String, KeyMapping> = mutableMapOf()
        @Volatile internal var pending: List<KeybindingDescriptor> = emptyList()

        private fun Modifier.toKeyModifier(): KeyModifier = when (this) {
            Modifier.NONE -> KeyModifier.NONE
            Modifier.CTRL -> KeyModifier.CONTROL
            Modifier.SHIFT -> KeyModifier.SHIFT
            Modifier.ALT -> KeyModifier.ALT
        }

        private val category = KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Nicephore.MODID, "key_mapping")
        )

        // Forge 64.x added a sort `order` parameter to the 6-arg KeyMapping constructor.
        // 0 keeps our keybinds in declaration order within the category (same effective UX
        // as NeoForge's 5-arg constructor without an order).
        private const val KEY_MAPPING_ORDER = 0

        internal fun buildMapping(descriptor: KeybindingDescriptor): KeyMapping {
            val key = InputConstants.Type.KEYSYM.getOrCreate(descriptor.defaultKey)
            return KeyMapping(
                descriptor.translationKey,
                KeyConflictContext.IN_GAME,
                descriptor.modifier.toKeyModifier(),
                key,
                category,
                KEY_MAPPING_ORDER,
            )
        }
    }

    @Mod.EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
    object RegistrationSubscriber {
        @SubscribeEvent
        @JvmStatic
        fun onRegister(event: RegisterKeyMappingsEvent) {
            pending.forEach { descriptor ->
                val mapping = buildMapping(descriptor)
                mappings[descriptor.id] = mapping
                event.register(mapping)
            }
        }
    }
}
