package com.vandendaelen.nicephore.platform

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.network.chat.Component
import java.io.File
import java.util.ServiceLoader

interface ScreenshotHook {
    fun register(callback: (image: NativeImage, file: File) -> Component?)

    companion object {
        val current: ScreenshotHook by lazy {
            ServiceLoader.load(ScreenshotHook::class.java).firstOrNull()
                ?: error("No ScreenshotHook implementation found on classpath.")
        }
    }
}
