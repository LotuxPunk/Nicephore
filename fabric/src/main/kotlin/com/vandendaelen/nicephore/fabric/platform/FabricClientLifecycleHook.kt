package com.vandendaelen.nicephore.fabric.platform

import com.vandendaelen.nicephore.platform.ClientLifecycleHook
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents

class FabricClientLifecycleHook : ClientLifecycleHook {
    override fun onClientSetup(action: () -> Unit) {
        ClientLifecycleEvents.CLIENT_STARTED.register { _ -> action() }
    }
}
