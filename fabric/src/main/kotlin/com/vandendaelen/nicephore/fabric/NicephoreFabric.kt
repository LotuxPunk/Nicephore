package com.vandendaelen.nicephore.fabric

import com.vandendaelen.nicephore.Nicephore
import net.fabricmc.api.ClientModInitializer

object NicephoreFabric : ClientModInitializer {
    override fun onInitializeClient() {
        Nicephore.LOGGER.info("NicephoreFabric initialising — platform impls wired in next task")
    }
}
