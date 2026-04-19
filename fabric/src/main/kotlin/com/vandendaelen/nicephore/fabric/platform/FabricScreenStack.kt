package com.vandendaelen.nicephore.fabric.platform

import com.vandendaelen.nicephore.platform.ScreenStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Fabric has no public equivalent of NeoForge's pushGuiLayer. We emulate the parent-stack
 * behaviour by maintaining our own LIFO of parent screens: push() captures the current
 * screen before replacing it, pop() restores the most-recently-captured parent. The net
 * effect: ESC from a nested screen returns to its parent, matching NeoForge's UX.
 */
class FabricScreenStack : ScreenStack {

    private val parents: ArrayDeque<Screen> = ArrayDeque()

    override fun push(child: Screen) {
        val mc = Minecraft.getInstance()
        mc.screen?.let { parents.addLast(it) }
        mc.setScreen(child)
    }

    override fun pop(): Boolean {
        val parent = parents.removeLastOrNull() ?: return false
        Minecraft.getInstance().setScreen(parent)
        return true
    }
}
