package com.vandendaelen.nicephore

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.Logger

/**
 * Loader-agnostic constants and shared infrastructure for Nicephore.
 * The loader-specific entry point (NicephoreNeoForge, NicephoreFabric) references these.
 */
object Nicephore {
    const val MODID: String = "nicephore"
    const val MOD_NAME: String = "Nicephore"

    @JvmField
    val LOGGER: Logger = LogUtils.getLogger()

    internal val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Starts the TrashManager cleanup coroutine. Idempotent — safe to call from any loader's init.
     * Implementation populated in Task 5 when TrashManager moves to :common.
     */
    fun startBackgroundTasks() {
        // Populated in Task 5.
    }
}
