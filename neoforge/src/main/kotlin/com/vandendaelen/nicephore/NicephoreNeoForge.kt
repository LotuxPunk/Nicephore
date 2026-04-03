package com.vandendaelen.nicephore

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.platform.NeoForgeConfigProvider
import com.vandendaelen.nicephore.platform.Services
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig

@Mod(Nicephore.MODID)
class NicephoreNeoForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        Services.config = NeoForgeConfigProvider()
        modContainer.registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC)
        Nicephore.startBackgroundTasks()
    }
}
