package com.vandendaelen.nicephore.forge.platform

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.ClientLifecycleHook
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.listener.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

class ForgeClientLifecycleHook : ClientLifecycleHook {
    override fun onClientSetup(action: () -> Unit) {
        pending = action
    }

    companion object {
        @Volatile internal var pending: (() -> Unit)? = null
    }

    @Mod.EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
    object SetupSubscriber {
        @SubscribeEvent
        @JvmStatic
        fun onClientSetup(event: FMLClientSetupEvent) {
            pending?.invoke()
        }
    }
}
