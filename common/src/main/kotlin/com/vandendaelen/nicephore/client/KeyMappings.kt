package com.vandendaelen.nicephore.client

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.KeybindingDescriptor
import com.vandendaelen.nicephore.platform.Modifier
import org.lwjgl.glfw.GLFW

object KeyMappings {
    const val COPY = "copy"
    const val SCREENSHOT_GUI = "screenshot_gui"
    const val GALLERY_GUI = "gallery_gui"

    val all: List<KeybindingDescriptor> = listOf(
        KeybindingDescriptor(
            id = COPY,
            translationKey = "${Nicephore.MODID}.keybinds.copy",
            defaultKey = GLFW.GLFW_KEY_C,
            modifier = Modifier.CTRL,
        ),
        KeybindingDescriptor(
            id = SCREENSHOT_GUI,
            translationKey = "${Nicephore.MODID}.keybinds.screenshots.gui",
            defaultKey = GLFW.GLFW_KEY_S,
            modifier = Modifier.CTRL,
        ),
        KeybindingDescriptor(
            id = GALLERY_GUI,
            translationKey = "${Nicephore.MODID}.keybinds.gallery.gui",
            defaultKey = GLFW.GLFW_KEY_G,
            modifier = Modifier.CTRL,
        ),
    )
}
