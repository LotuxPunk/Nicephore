package com.vandendaelen.nicephore.neoforge.platform

import com.vandendaelen.nicephore.platform.ScreenStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * NeoForge delegates nested-screen navigation to its native layer-stack API.
 * pushGuiLayer stacks a screen on top; popGuiLayer returns to the one beneath.
 *
 * We track our own push count as well so pop() can distinguish "we pushed a layer,
 * pop it" from "top-level screen opened via setScreen, no layer to pop — caller
 * should exit to game". Calling popGuiLayer when the layer stack is empty would
 * no-op at best and throw at worst, so the guard is strictly safer than delegating
 * directly to Minecraft.getInstance().popGuiLayer() unconditionally.
 */
class NeoForgeScreenStack : ScreenStack {

    private var pushedLayers: Int = 0

    override fun push(child: Screen) {
        Minecraft.getInstance().pushGuiLayer(child)
        pushedLayers++
    }

    override fun pop(): Boolean {
        if (pushedLayers <= 0) return false
        pushedLayers--
        Minecraft.getInstance().popGuiLayer()
        return true
    }
}
