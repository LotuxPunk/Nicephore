package com.vandendaelen.nicephore

import com.mojang.logging.LogUtils
import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.utils.TrashManager
import kotlinx.coroutines.*
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import org.slf4j.Logger

@Mod(Nicephore.MODID)
class Nicephore(modEventBus: IEventBus, modContainer: ModContainer) {
    companion object {
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

    init {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC)
        startBackgroundTasks()
    }
}
