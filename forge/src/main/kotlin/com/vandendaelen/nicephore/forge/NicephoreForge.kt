package com.vandendaelen.nicephore.forge

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.NicephoreClient
import com.vandendaelen.nicephore.config.NicephoreConfigHolder
import net.minecraftforge.fml.common.Mod

@Mod(Nicephore.MODID)
class NicephoreForge {
    init {
        NicephoreConfigHolder.load()
        NicephoreClient.onInit()
    }
}
