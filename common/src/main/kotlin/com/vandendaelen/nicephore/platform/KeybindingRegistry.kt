package com.vandendaelen.nicephore.platform

import java.util.ServiceLoader

enum class Modifier { NONE, CTRL, SHIFT, ALT }

data class KeybindingDescriptor(
    val id: String,
    val translationKey: String,
    val defaultKey: Int,
    val modifier: Modifier,
    val category: String = "nicephore.key_mapping",
)

interface KeybindingRegistry {
    fun register(descriptors: List<KeybindingDescriptor>)
    fun consumeClick(id: String): Boolean

    companion object {
        val current: KeybindingRegistry by lazy {
            ServiceLoader.load(KeybindingRegistry::class.java).firstOrNull()
                ?: error("No KeybindingRegistry implementation found on classpath.")
        }
    }
}
