package com.vandendaelen.nicephore.fabric.platform

import com.vandendaelen.nicephore.platform.PlatformContext
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

class FabricPlatformContext : PlatformContext {
    override val loaderName: String = "fabric"

    override val configDir: Path
        get() = FabricLoader.getInstance().configDir

    override val minecraftDir: Path
        get() = FabricLoader.getInstance().gameDir

    override val screenshotDir: Path
        get() = minecraftDir.resolve("screenshots")

    override fun isModLoaded(modId: String): Boolean =
        FabricLoader.getInstance().isModLoaded(modId)
}
