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

    fun loadBatch(files: List<File>, idPrefix: String, useThumbnails: Boolean = false) {
        cancelAll()
        slots.clear()

        files.forEachIndexed { index, file ->
            slots[index] = SlotState(LoadState.LOADING)
            scope.launch {
                val fileToLoad = if (useThumbnails) ThumbnailCache.getThumbnail(file) else file
                val nativeImage = readImageFromDisk(fileToLoad)
                Minecraft.getInstance().execute {
                    if (nativeImage != null) {
                        val id = "${idPrefix}_$index"
                        val texture = DynamicTexture({ "nicephore_$id" }, nativeImage)
                        val textureId = Identifier.withDefaultNamespace("nicephore_$id")
                        Minecraft.getInstance().textureManager.register(textureId, texture)
                        slots[index] = SlotState(LoadState.LOADED, LoadedTexture(texture, textureId))
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
            val nativeImage = readImageFromDisk(file)
            Minecraft.getInstance().execute {
                if (nativeImage != null) {
                    val texture = DynamicTexture({ "nicephore_$idPrefix" }, nativeImage)
                    val textureId = Identifier.withDefaultNamespace("nicephore_$idPrefix")
                    Minecraft.getInstance().textureManager.register(textureId, texture)
                    slots[0] = SlotState(LoadState.LOADED, LoadedTexture(texture, textureId))
                } else {
                    slots[0] = SlotState(LoadState.ERROR)
                }
                onLoadComplete?.invoke()
            }
        }
    }

    private fun readImageFromDisk(file: File): NativeImage? {
        return try {
            if (file.extension.lowercase() == "png") {
                FileInputStream(file).use { NativeImage.read(it) }
            } else {
                // NativeImage only reads PNG; convert JPEG/other formats via ImageIO
                val buffered = javax.imageio.ImageIO.read(file)
                    ?: throw IOException("ImageIO.read returned null for ${file.name}")
                val baos = java.io.ByteArrayOutputStream()
                javax.imageio.ImageIO.write(buffered, "png", baos)
                NativeImage.read(java.io.ByteArrayInputStream(baos.toByteArray()))
            }
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
