package com.vandendaelen.nicephore.platform

import net.minecraft.client.gui.screens.Screen
import java.util.ServiceLoader

/**
 * Navigates between nested screens. NeoForge pushes a layer stack (returns to parent on close);
 * Fabric has no equivalent public API so it falls back to plain setScreen (ESC exits to the game).
 *
 * Use this when going from one Nicephore screen to another. To open the first screen from the
 * game world, call Minecraft.getInstance().setScreen(...) directly — push semantics only make
 * sense when a parent screen already exists.
 */
interface ScreenStack {
    fun push(child: Screen)

    companion object {
        val current: ScreenStack by lazy {
            ServiceLoader.load(ScreenStack::class.java).firstOrNull()
                ?: error("No ScreenStack implementation found on classpath.")
        }
    }
}
