package com.vandendaelen.nicephore.platform

import java.util.ServiceLoader

interface ClientLifecycleHook {
    fun onClientSetup(action: () -> Unit)

    companion object {
        val current: ClientLifecycleHook by lazy {
            ServiceLoader.load(ClientLifecycleHook::class.java).firstOrNull()
                ?: error("No ClientLifecycleHook implementation found on classpath.")
        }
    }
}
