package com.vandendaelen.nicephore.neoforge

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.config.NicephoreConfig
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig

@Mod(Nicephore.MODID)
class NicephoreNeoForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC)
        Nicephore.startBackgroundTasks()
    }
}
