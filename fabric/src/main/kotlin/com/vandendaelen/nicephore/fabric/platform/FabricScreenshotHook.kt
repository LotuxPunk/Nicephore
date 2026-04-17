package com.vandendaelen.nicephore.fabric.platform

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.platform.ScreenshotHook
import net.minecraft.network.chat.Component
import java.io.File
import java.util.function.Consumer

/**
 * Fabric has no built-in screenshot event in fabric-api 0.145.4.
 * We use a Mixin (ScreenshotMixin) that injects into the private
 * Screenshot.lambda$grab$0(File, String, Consumer, NativeImage) helper,
 * which has access to both the NativeImage and the File.
 * The mixin calls [FabricScreenshotHook.fireHook] before the vanilla save logic.
 */
class FabricScreenshotHook : ScreenshotHook {

    override fun register(callback: (image: NativeImage, file: File) -> Component?) {
        registered = callback
    }

    companion object {
        @Volatile
        var registered: ((NativeImage, File) -> Component?)? = null

        /**
         * Called by [com.vandendaelen.nicephore.fabric.mixin.ScreenshotMixin].
         * Returns the wrapped consumer to use (wrapping the original if the callback
         * wants to substitute the result-message), or null to use the original unchanged.
         */
        fun fireHook(
            file: File,
            image: NativeImage,
            originalConsumer: Consumer<Component>,
        ): Consumer<Component>? {
            val callback = registered ?: return null
            val replacement = callback(image, file) ?: return null
            // If replacement is an empty literal, suppress the message.
            // Otherwise deliver the replacement instead of the vanilla message.
            return if (replacement.string.isEmpty()) {
                Consumer { /* suppress */ }
            } else {
                Consumer { originalConsumer.accept(replacement) }
            }
        }
    }
}
