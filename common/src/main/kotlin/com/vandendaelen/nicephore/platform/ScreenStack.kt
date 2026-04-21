package com.vandendaelen.nicephore.platform

import net.minecraft.client.gui.screens.Screen
import java.util.ServiceLoader

/**
 * Navigates between nested screens. NeoForge pushes a layer stack (returns to parent on close);
 * Fabric has no equivalent public API so it maintains its own stack of parent Screen references
 * and restores them on pop.
 *
 * Use this when going from one Nicephore screen to another. To open the first screen from the
 * game world, call Minecraft.getInstance().setScreen(...) directly — push semantics only make
 * sense when a parent screen already exists.
 */
interface ScreenStack {
    fun push(child: Screen)

    /**
     * Pops the most recently-pushed child, restoring its parent. Call from the child screen's
     * onClose() handler BEFORE super.onClose().
     *
     * @return true if a parent was restored (caller should NOT fall through to its own close
     *   behaviour — the parent is now the active screen). Returns false if the stack is empty
     *   (caller should invoke super.onClose() or equivalent to exit to the game world).
     */
    fun pop(): Boolean

    companion object {
        val current: ScreenStack by lazy {
            ServiceLoader.load(ScreenStack::class.java).firstOrNull()
                ?: error("No ScreenStack implementation found on classpath.")
        }
    }
}
