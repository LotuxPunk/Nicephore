package com.vandendaelen.nicephore.fabric

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.NicephoreClient
import com.vandendaelen.nicephore.config.NicephoreConfigHolder
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object NicephoreFabric : ClientModInitializer {
    override fun onInitializeClient() {
        Nicephore.LOGGER.info("NicephoreFabric initialising")
        // Eagerly initialise the config so file I/O happens before first tick.
        NicephoreConfigHolder.load()
        NicephoreClient.onInit()

        // Poll keybinds each client tick.
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            NicephoreClient.pollKeybindings()
        }
    }
}
