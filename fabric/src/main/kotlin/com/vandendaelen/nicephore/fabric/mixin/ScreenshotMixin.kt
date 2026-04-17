package com.vandendaelen.nicephore.fabric.mixin

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.fabric.platform.FabricScreenshotHook
import net.minecraft.client.Screenshot
import net.minecraft.network.chat.Component
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.io.File
import java.util.function.Consumer

/**
 * Intercepts screenshot capture on Fabric (no screenshot event in fabric-api 0.145.4+26.1.2).
 *
 * Vanilla flow inside Screenshot.grab:
 *  grab(File, String, RenderTarget, int, Consumer<Component>)
 *   -> takeScreenshot(target, width, Consumer<NativeImage>)
 *      NativeImage consumer = lambda$grab$0(workDir, fileName, msgConsumer, image)
 *   -> lambda$grab$0 builds the output File, then submits Runnable to TracingExecutor:
 *        lambda$grab$1(NativeImage image, File file, Consumer<Component> msgConsumer)
 *   -> lambda$grab$1: image.writeToFile(file); builds success Component; msgConsumer.accept(comp)
 *
 * Strategy: inject at HEAD of lambda$grab$1 (cancellable).
 * If the hook is registered we take over completely:
 *   1. image.writeToFile(file) — save the screenshot
 *   2. callback(image, file) — notify the hook (launches ScreenshotThread, etc.)
 *   3. If callback returns a non-null, non-empty Component → deliver it via msgConsumer
 *      If callback returns Component.literal("") → suppress the chat message
 *   4. cancel() — prevents vanilla from re-saving and re-messaging
 *
 * If the hook is NOT registered, we do nothing and vanilla proceeds normally.
 */
@Mixin(Screenshot::class)
abstract class ScreenshotMixin {

    @Inject(
        method = ["lambda\$grab\$1(Lcom/mojang/blaze3d/platform/NativeImage;Ljava/io/File;Ljava/util/function/Consumer;)V"],
        at = [At("HEAD")],
        cancellable = true,
        remap = false,
    )
    private fun onGrabLambda1(
        image: NativeImage,
        file: File,
        @Suppress("UNCHECKED_CAST") msgConsumer: Consumer<Component>,
        ci: CallbackInfo,
    ) {
        val callback = FabricScreenshotHook.registered ?: return
        // Save the image ourselves
        runCatching { image.writeToFile(file) }.onFailure { return }
        // Notify the hook; it may return a replacement Component (or null to keep vanilla msg)
        val replacement = callback(image, file)
        if (replacement != null && replacement.string.isNotEmpty()) {
            msgConsumer.accept(replacement)
        }
        // Cancel vanilla body whether or not there is a replacement component, because
        // we already wrote the file and the hook has been notified. Vanilla would
        // re-write the file (double-write) and deliver a redundant chat message.
        ci.cancel()
    }
}
