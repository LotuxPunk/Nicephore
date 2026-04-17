package com.vandendaelen.nicephore.neoforge.platform

import com.vandendaelen.nicephore.platform.ScreenStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

class NeoForgeScreenStack : ScreenStack {
    override fun push(child: Screen) {
        Minecraft.getInstance().pushGuiLayer(child)
    }
}
