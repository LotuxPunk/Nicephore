package com.vandendaelen.nicephore.neoforge.platform

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.platform.ScreenshotHook
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.client.event.ScreenshotEvent
import net.neoforged.neoforge.common.NeoForge
import java.io.File

class NeoForgeScreenshotHook : ScreenshotHook {
    private var registered: ((NativeImage, File) -> Component?)? = null

    init {
        NeoForge.EVENT_BUS.addListener<ScreenshotEvent> { event ->
            val callback = registered ?: return@addListener
            val replacement = callback(event.image, event.screenshotFile)
            if (replacement != null) {
                event.resultMessage = replacement
            }
        }
    }

    override fun register(callback: (image: NativeImage, file: File) -> Component?) {
        registered = callback
    }
}
