package com.vandendaelen.nicephore.fabric.platform

import com.vandendaelen.nicephore.platform.ScreenStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Fabric has no public equivalent of NeoForge's pushGuiLayer (the layer-stack API).
 * Falls back to setScreen — ESC from the child will return to the game rather than
 * popping back to the parent.
 */
class FabricScreenStack : ScreenStack {
    override fun push(child: Screen) {
        Minecraft.getInstance().setScreen(child)
    }
}
