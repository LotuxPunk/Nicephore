package com.vandendaelen.nicephore.client.event

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.NicephoreClient
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.eventbus.api.listener.SubscribeEvent
import net.minecraftforge.fml.common.Mod

// EventBus 7 auto-routes listeners based on the event type's own BUS field, so the old
// `bus = Bus.FORGE` parameter is no longer required on @EventBusSubscriber.
@Mod.EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientGameBusEvents {
    @SubscribeEvent
    @JvmStatic
    fun onKey(event: InputEvent.Key) {
        NicephoreClient.pollKeybindings()
    }
}
