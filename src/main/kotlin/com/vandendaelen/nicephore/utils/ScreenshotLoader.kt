package com.vandendaelen.nicephore.utils

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.Nicephore
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ScreenshotLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class LoadState { LOADING, LOADED, ERROR }

    data class LoadedTexture(
        val texture: DynamicTexture,
        val textureId: Identifier
    )

    data class SlotState(
        val state: LoadState,
        val loaded: LoadedTexture? = null
    )

    private val slots = mutableMapOf<Int, SlotState>()
    private var onLoadComplete: (() -> Unit)? = null

    fun setOnLoadComplete(callback: () -> Unit) {
        onLoadComplete = callback
    }

    fun getSlotState(index: Int): SlotState {
        return slots[index] ?: SlotState(LoadState.LOADING)
    }

    fun loadBatch(files: List<File>, idPrefix: String) {
        cancelAll()
        slots.clear()

        files.forEachIndexed { index, file ->
            slots[index] = SlotState(LoadState.LOADING)
            scope.launch {
                val result = loadFile(file, "${idPrefix}_$index")
                Minecraft.getInstance().execute {
                    if (result != null) {
                        val tm = Minecraft.getInstance().textureManager
                        tm.register(result.textureId, result.texture)
                        slots[index] = SlotState(LoadState.LOADED, result)
                    } else {
                        slots[index] = SlotState(LoadState.ERROR)
                    }
                    onLoadComplete?.invoke()
                }
            }
        }
    }

    fun loadSingle(file: File, idPrefix: String) {
        cancelAll()
        slots.clear()
        slots[0] = SlotState(LoadState.LOADING)

        scope.launch {
            val result = loadFile(file, idPrefix)
            Minecraft.getInstance().execute {
                if (result != null) {
                    val tm = Minecraft.getInstance().textureManager
                    tm.register(result.textureId, result.texture)
                    slots[0] = SlotState(LoadState.LOADED, result)
                } else {
                    slots[0] = SlotState(LoadState.ERROR)
                }
                onLoadComplete?.invoke()
            }
        }
    }

    private fun loadFile(file: File, id: String): LoadedTexture? {
        return try {
            val nativeImage = FileInputStream(file).use { NativeImage.read(it) }
            val texture = DynamicTexture({ "nicephore_$id" }, nativeImage)
            val textureId = Identifier.withDefaultNamespace("nicephore_$id")
            LoadedTexture(texture, textureId)
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to load screenshot: ${file.name}", e)
            null
        }
    }

    fun cancelAll() {
        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        releaseTextures()
    }

    fun releaseTextures() {
        slots.values.forEach { slot ->
            slot.loaded?.texture?.close()
        }
        slots.clear()
    }

    fun destroy() {
        releaseTextures()
        scope.cancel()
    }
}
