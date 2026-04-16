package com.vandendaelen.nicephore.client.event

import com.vandendaelen.nicephore.Nicephore
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

@EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientModBusEvents {
    @SubscribeEvent
    @JvmStatic
    fun onClientSetup(event: FMLClientSetupEvent) {
        // Client setup logic rewired via NicephoreClient in Task 9
    }
}
