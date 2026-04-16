package com.vandendaelen.nicephore

import com.mojang.logging.LogUtils
import com.vandendaelen.nicephore.utils.TrashManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger

object Nicephore {
    const val MODID: String = "nicephore"
    const val MOD_NAME: String = "Nicephore"

    @JvmField
    val LOGGER: Logger = LogUtils.getLogger()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var started = false

    fun startBackgroundTasks() {
        if (started) return
        started = true
        backgroundScope.launch {
            while (isActive) {
                TrashManager.cleanupOldFiles()
                delay(60 * 60 * 1000L) // 1 hour
            }
        }
    }
}
