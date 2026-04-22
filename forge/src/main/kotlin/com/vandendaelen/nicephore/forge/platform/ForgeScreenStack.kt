package com.vandendaelen.nicephore.forge.platform

import com.vandendaelen.nicephore.platform.ScreenStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * Forge has no public equivalent of NeoForge's pushGuiLayer / GuiLayerManager. We emulate
 * the parent-stack behaviour by maintaining our own LIFO of parent screens: push() captures
 * the current screen before replacing it, pop() restores the most-recently-captured parent.
 * Same approach as the Fabric impl — the net UX matches NeoForge's layer stack.
 */
class ForgeScreenStack : ScreenStack {

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
