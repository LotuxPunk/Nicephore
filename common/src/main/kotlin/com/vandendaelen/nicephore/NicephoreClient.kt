package com.vandendaelen.nicephore

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.client.KeyMappings
import com.vandendaelen.nicephore.client.gui.GalleryScreen
import com.vandendaelen.nicephore.client.gui.ScreenshotScreen
import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.platform.ClientLifecycleHook
import com.vandendaelen.nicephore.platform.KeybindingRegistry
import com.vandendaelen.nicephore.platform.ScreenshotHook
import com.vandendaelen.nicephore.thread.InitThread
import com.vandendaelen.nicephore.thread.ScreenshotThread
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.io.File

object NicephoreClient {

    fun onInit() {
        KeybindingRegistry.current.register(KeyMappings.all)
        ScreenshotHook.current.register(::onScreenshotTaken)
        ClientLifecycleHook.current.onClientSetup(::onClientReady)
        Nicephore.startBackgroundTasks()
    }

    fun pollKeybindings() {
        val kb = KeybindingRegistry.current
        when {
            kb.consumeClick(KeyMappings.COPY) -> {
                if (CopyImageToClipBoard.copyLastScreenshot()) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }
            kb.consumeClick(KeyMappings.SCREENSHOT_GUI) -> {
                if (ScreenshotScreen.canBeShow()) {
                    Minecraft.getInstance().setScreen(ScreenshotScreen())
                } else {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"))
                }
            }
            kb.consumeClick(KeyMappings.GALLERY_GUI) -> {
                if (GalleryScreen.canBeShow()) {
                    Minecraft.getInstance().setScreen(GalleryScreen())
                } else {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"))
                }
            }
        }
    }

    private fun onScreenshotTaken(image: NativeImage, file: File): Component? {
        ScreenshotThread(image, file).start()
        return if (NicephoreConfig.Client.getScreenshotCustomMessage()) {
            Component.literal("")
        } else {
            null
        }
    }

    private fun onClientReady() {
        InitThread(NicephoreConfig.Client.getOptimisedOutputToggle()).start()
    }
}
