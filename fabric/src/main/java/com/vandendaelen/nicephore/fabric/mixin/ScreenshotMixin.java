package com.vandendaelen.nicephore.fabric.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.vandendaelen.nicephore.fabric.platform.FabricScreenshotHook;
import kotlin.jvm.functions.Function2;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

/**
 * Intercepts screenshot capture on Fabric. fabric-api 0.145.4+26.1.2 does not expose a
 * ScreenshotEvent API in the version we ship against, so we inject at the head of
 * Screenshot.lambda$grab$1 — the Runnable submitted to the IO pool that writes the file
 * and posts the result Component. Target is a static synthetic method, so the handler
 * must also be static (written in Java for predictable Mixin bytecode behaviour).
 */
@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {

    @Inject(
            method = "lambda$grab$1(Lcom/mojang/blaze3d/platform/NativeImage;Ljava/io/File;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void nicephore$onGrabLambda1(
            NativeImage image,
            File file,
            Consumer<Component> msgConsumer,
            CallbackInfo ci
    ) {
        Function2<NativeImage, File, Component> callback = FabricScreenshotHook.Companion.getRegistered();
        if (callback == null) {
            return;
        }
        try {
            image.writeToFile(file);
        } catch (Throwable t) {
            return;
        }
        Component replacement = callback.invoke(image, file);
        if (replacement != null && !replacement.getString().isEmpty()) {
            msgConsumer.accept(replacement);
        }
        ci.cancel();
    }
}
