package com.vandendaelen.nicephore.neoforge.platform

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.ClientLifecycleHook
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

class NeoForgeClientLifecycleHook : ClientLifecycleHook {
    override fun onClientSetup(action: () -> Unit) {
        pending = action
    }

    companion object {
        @Volatile internal var pending: (() -> Unit)? = null
    }

    @EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
    object SetupSubscriber {
        @SubscribeEvent
        @JvmStatic
        fun onClientSetup(event: FMLClientSetupEvent) {
            pending?.invoke()
        }
    }
}
