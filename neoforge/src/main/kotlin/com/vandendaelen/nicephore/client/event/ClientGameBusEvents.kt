package com.vandendaelen.nicephore.client.event

import com.vandendaelen.nicephore.Nicephore
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent

@EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientGameBusEvents {
    @SubscribeEvent
    @JvmStatic
    fun onKey(event: InputEvent.Key) {
        // Keybinding polling rewired via NicephoreClient in Task 9
    }
}
