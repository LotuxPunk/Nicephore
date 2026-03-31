package com.vandendaelen.nicephore.client.event

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.client.KeyMappings
import com.vandendaelen.nicephore.client.gui.GalleryScreen
import com.vandendaelen.nicephore.client.gui.ScreenshotScreen
import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.thread.InitThread
import com.vandendaelen.nicephore.thread.ScreenshotThread
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.ScreenshotEvent

@EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientGameBusEvents {
    @SubscribeEvent
    @JvmStatic
    fun onKey(event: InputEvent.Key) {
        when {
            KeyMappings.COPY_KEY.consumeClick() -> {
                if (CopyImageToClipBoard.copyLastScreenshot()) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }
            KeyMappings.GUI_SCREENSHOT_KEY.consumeClick() -> {
                if (ScreenshotScreen.canBeShow()) {
                    Minecraft.getInstance().setScreen(ScreenshotScreen())
                } else {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"))
                }
            }
            KeyMappings.GUI_GALLERY_KEY.consumeClick() -> {
                if (GalleryScreen.canBeShow()) {
                    Minecraft.getInstance().setScreen(GalleryScreen())
                } else {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshots.empty"))
                }
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onScreenshot(event: ScreenshotEvent) {
        val thread = ScreenshotThread(event.image, event.screenshotFile)
        thread.start()

        if (NicephoreConfig.Client.getScreenshotCustomMessage()) {
            event.resultMessage = Component.literal("")
        }
    }
}

@EventBusSubscriber(value = [Dist.CLIENT], modid = Nicephore.MODID)
object ClientSetupEvents {
    @SubscribeEvent
    @JvmStatic
    fun onClientSetup(event: FMLClientSetupEvent) {
        val initThread = InitThread(NicephoreConfig.Client.getOptimisedOutputToggle())
        initThread.start()
    }
}
