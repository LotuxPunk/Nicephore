package com.vandendaelen.nicephore.utils

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object PlayerHelper {
    fun sendMessage(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendSystemMessage(message)
        }
    }

    fun sendHotbarMessage(message: Component) {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().player?.sendOverlayMessage(message)
        }
    }
}
