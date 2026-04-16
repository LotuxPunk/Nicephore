package com.vandendaelen.nicephore.neoforge

import com.vandendaelen.nicephore.Nicephore
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

@Mod(Nicephore.MODID)
class NicephoreNeoForge(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        Nicephore.startBackgroundTasks()
    }
}
