package com.vandendaelen.nicephore

import com.mojang.logging.LogUtils
import com.vandendaelen.nicephore.utils.TrashManager
import kotlinx.coroutines.*
import org.slf4j.Logger

object Nicephore {
    const val MODID: String = "nicephore"
    const val MOD_NAME: String = "Nicephore"
    @JvmField
    val LOGGER: Logger = LogUtils.getLogger()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startBackgroundTasks() {
        backgroundScope.launch {
            while (isActive) {
                TrashManager.cleanupOldFiles()
                delay(60 * 60 * 1000L) // 1 hour
            }
        }
    }
}
