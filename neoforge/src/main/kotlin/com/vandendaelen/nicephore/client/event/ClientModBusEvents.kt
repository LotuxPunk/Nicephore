package com.vandendaelen.nicephore.client.event

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.client.KeyMappings
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent

@EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientModBusEvents {
    @SubscribeEvent
    @JvmStatic
    fun onKeyMappingsRegistration(event: RegisterKeyMappingsEvent) {
        KeyMappings.entries.forEach { event.register(it.key) }
    }
}
