package com.vandendaelen.nicephore.utils

import com.vandendaelen.nicephore.Nicephore
import net.minecraft.client.Minecraft
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

object ThumbnailCache {
    private const val THUMBNAIL_MAX_WIDTH = 200

    private val cacheDir: File by lazy {
        File(Minecraft.getInstance().gameDirectory, ".nicephore${File.separator}thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun removeThumbnail(originalName: String) {
        val thumbFile = File(cacheDir, originalName)
        if (thumbFile.exists()) thumbFile.delete()
    }

    fun getThumbnail(original: File): File {
        val thumbFile = File(cacheDir, original.name)

        if (thumbFile.exists() && thumbFile.lastModified() >= original.lastModified()) {
            return thumbFile
        }

        return generateThumbnail(original, thumbFile)
    }

    private fun generateThumbnail(original: File, thumbFile: File): File {
        return try {
            val fullImage = ImageIO.read(original)
                ?: throw IOException("ImageIO.read returned null for ${original.name}")

            val scaledWidth = THUMBNAIL_MAX_WIDTH.coerceAtMost(fullImage.width)
            val scaledHeight = (scaledWidth.toFloat() / fullImage.width * fullImage.height).toInt()

            val scaled = fullImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
            val buffered = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
            buffered.createGraphics().apply {
                drawImage(scaled, 0, 0, null)
                dispose()
            }

            cacheDir.mkdirs()
            ImageIO.write(buffered, "png", thumbFile)
            Nicephore.LOGGER.debug("Generated thumbnail for {}", original.name)
            thumbFile
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to generate thumbnail for {}, falling back to original", original.name, e)
            original
        }
    }
}
