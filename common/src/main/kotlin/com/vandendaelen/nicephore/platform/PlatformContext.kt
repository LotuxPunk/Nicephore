package com.vandendaelen.nicephore.platform

import java.nio.file.Path
import java.util.ServiceLoader

interface PlatformContext {
    val loaderName: String
    val configDir: Path
    val minecraftDir: Path
    val screenshotDir: Path
    fun isModLoaded(modId: String): Boolean

    companion object {
        val current: PlatformContext by lazy {
            ServiceLoader.load(PlatformContext::class.java).firstOrNull()
                ?: error("No PlatformContext implementation found on classpath. " +
                        "Expected a META-INF/services/${PlatformContext::class.java.name} file.")
        }
    }
}
