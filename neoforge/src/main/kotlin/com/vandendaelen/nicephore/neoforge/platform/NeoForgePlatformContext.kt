package com.vandendaelen.nicephore.neoforge.platform

import com.vandendaelen.nicephore.platform.PlatformContext
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Path

class NeoForgePlatformContext : PlatformContext {
    override val loaderName: String = "neoforge"
    override val configDir: Path get() = FMLPaths.CONFIGDIR.get()
    override val minecraftDir: Path get() = FMLPaths.GAMEDIR.get()
    override val screenshotDir: Path get() = minecraftDir.resolve("screenshots")
    override fun isModLoaded(modId: String): Boolean = ModList.get().isLoaded(modId)
}
