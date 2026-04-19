package com.vandendaelen.nicephore.fabric.platform

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.ScreenshotHook
import net.minecraft.client.Screenshot
import net.minecraft.network.chat.Component
import java.io.File
import java.util.function.Consumer

/**
 * Fabric has no built-in screenshot event in fabric-api 0.145.4+26.1.2.
 * We use a Mixin (com.vandendaelen.nicephore.fabric.mixin.ScreenshotMixin) that injects
 * at the head of Screenshot.lambda$grab$1(NativeImage, File, Consumer<Component>) —
 * the Runnable submitted to the IO pool that writes the screenshot file and posts the
 * success message. The mixin reads [registered] via [getRegistered] (JvmStatic) and
 * invokes the callback with the image + file before cancelling the vanilla body.
 */
class FabricScreenshotHook : ScreenshotHook {

    override fun register(callback: (image: NativeImage, file: File) -> Component?) {
        registered = callback
    }

    companion object {
        /**
         * The registered callback, set by [register] and consumed from Java by
         * [ScreenshotMixin]. Annotated [JvmStatic] so the Mixin's Java code can call
         * FabricScreenshotHook.getRegistered() directly instead of going through the
         * .Companion.getRegistered() accessor (which depends on Kotlin compiler internals).
         */
        @Volatile
        @JvmStatic
        var registered: ((NativeImage, File) -> Component?)? = null

        /**
         * Returns true if net.minecraft.client.Screenshot still has the synthetic
         * lambda$grab$1(NativeImage, File, Consumer) method that our Mixin targets.
         * If a future Minecraft refactor renames or removes that lambda, the Mixin can
         * no longer apply and this returns false — NicephoreFabric then logs a warning
         * at init time so the regression is visible instead of silently broken.
         */
        @JvmStatic
        fun probeTargetMethodPresent(): Boolean = runCatching {
            Screenshot::class.java.getDeclaredMethod(
                "lambda\$grab\$1",
                NativeImage::class.java,
                File::class.java,
                Consumer::class.java,
            )
        }.isSuccess

        /**
         * Called by [com.vandendaelen.nicephore.fabric.mixin.ScreenshotMixin].
         * Returns the wrapped consumer to use (wrapping the original if the callback
         * wants to substitute the result-message), or null to use the original unchanged.
         */
        @JvmStatic
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
