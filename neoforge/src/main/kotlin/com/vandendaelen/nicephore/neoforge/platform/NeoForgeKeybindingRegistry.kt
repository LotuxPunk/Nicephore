package com.vandendaelen.nicephore.neoforge.platform

import com.mojang.blaze3d.platform.InputConstants
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.KeybindingDescriptor
import com.vandendaelen.nicephore.platform.KeybindingRegistry
import com.vandendaelen.nicephore.platform.Modifier
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier

class NeoForgeKeybindingRegistry : KeybindingRegistry {
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

        internal fun buildMapping(descriptor: KeybindingDescriptor): KeyMapping {
            val key = InputConstants.Type.KEYSYM.getOrCreate(descriptor.defaultKey)
            return KeyMapping(
                descriptor.translationKey,
                KeyConflictContext.IN_GAME,
                descriptor.modifier.toKeyModifier(),
                key,
                category,
            )
        }
    }

    @EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
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
