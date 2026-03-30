package com.vandendaelen.nicephore.utils

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object PlayerHelper {
    fun sendMessage(message: Component) {
        Minecraft.getInstance().player?.displayClientMessage(message, false)
    }

    fun sendHotbarMessage(message: Component) {
        Minecraft.getInstance().player?.displayClientMessage(message, true)
    }
}
