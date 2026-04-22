package com.vandendaelen.nicephore.forge.platform

import com.vandendaelen.nicephore.platform.PlatformContext
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLPaths
import java.nio.file.Path

class ForgePlatformContext : PlatformContext {
    override val loaderName: String = "forge"

    // Use block-form getters with backticks on `get` — Kotlin 2.3.x's property-accessor parser
    // gets confused by the adjacent `.get()` method call on an enum constant when written as an
    // expression-body getter (`val foo: T get() = X.get()`), reporting "Unresolved reference 'get'".
    // Escaping the method name with backticks and moving to a block body disambiguates it.
    override val configDir: Path
        get() = FMLPaths.CONFIGDIR.`get`()
    override val minecraftDir: Path
        get() = FMLPaths.GAMEDIR.`get`()
    override val screenshotDir: Path
        get() = minecraftDir.resolve("screenshots")

    // Forge 64.x refactored ModList to expose static methods directly (no .get() singleton).
    override fun isModLoaded(modId: String): Boolean = ModList.isLoaded(modId)
}
