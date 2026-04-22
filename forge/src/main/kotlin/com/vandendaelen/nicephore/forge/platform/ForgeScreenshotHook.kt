package com.vandendaelen.nicephore.forge.platform

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.platform.ScreenshotHook
import net.minecraft.network.chat.Component
import net.minecraftforge.client.event.ScreenshotEvent
import java.io.File
import java.util.function.Consumer

class ForgeScreenshotHook : ScreenshotHook {
    private var registered: ((NativeImage, File) -> Component?)? = null

    init {
        // EventBus 7: each event owns its own BUS rather than routing through
        // MinecraftForge.EVENT_BUS. addListener has overloads for Predicate and Consumer,
        // so pass an explicit Consumer SAM wrapper to disambiguate.
        ScreenshotEvent.BUS.addListener(Consumer<ScreenshotEvent> { event ->
            val callback = registered ?: return@Consumer
            val replacement = callback(event.image, event.screenshotFile)
            if (replacement != null) {
                event.resultMessage = replacement
            }
        })
    }

    override fun register(callback: (image: NativeImage, file: File) -> Component?) {
        registered = callback
    }
}
