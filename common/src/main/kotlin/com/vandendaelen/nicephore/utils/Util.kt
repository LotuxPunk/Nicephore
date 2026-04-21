package com.vandendaelen.nicephore.utils

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.config.NicephoreConfigHolder
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.client.renderer.texture.DynamicTexture
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.util.Comparator
import java.util.stream.IntStream
import java.util.stream.Stream

object Util {
    fun fileToTexture(file: File): DynamicTexture? {
        return try {
            val nativeImage = FileInputStream(file).use { NativeImage.read(it) }
            DynamicTexture({ "nicephore_${file.nameWithoutExtension}" }, nativeImage)
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to load screenshot texture: ${file.name}", e)
            null
        }
    }

    fun <T> batches(source: List<T>, length: Int): Stream<List<T>> {
        require(length > 0) { "length = $length" }
        val size = source.size
        if (size <= 0) return Stream.empty()
        val fullChunks = (size - 1) / length
        return IntStream.range(0, fullChunks + 1).mapToObj { n ->
            source.subList(n * length, if (n == fullChunks) size else (n + 1) * length)
        }
    }

    fun getBatchOfFiles(toSkip: Long, toTake: Long, directory: File): List<File> {
        return try {
            Files.list(directory.toPath()).use { stream ->
                val filter = NicephoreConfigHolder.current.screenshotFilter.predicate
                stream
                    .filter { path -> !Files.isDirectory(path) && filter.accept(path.toFile(), path.fileName.toString()) }
                    .sequential()
                    .map(Path::toFile)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .skip(toSkip)
                    .limit(toTake)
                    .toList()
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to list screenshot files in {}", directory.absolutePath, e)
            emptyList()
        }
    }

    fun getNumberOfFiles(directory: File): Long {
        return try {
            Files.list(directory.toPath()).use { stream ->
                stream.filter { path -> !Files.isDirectory(path) }.count()
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to count files in {}", directory.absolutePath, e)
            0L
        }
    }

    fun formatFileSize(file: File): String {
        val size = file.length().toDouble()
        val formatter = DecimalFormat("#0.00")
        val mbSize = 1024 * 1024
        val kbSize = 1024

        return if (size > mbSize) {
            "${formatter.format(size / mbSize)} MB"
        } else {
            "${formatter.format(size / kbSize)} KB"
        }
    }

    fun formatFileDate(file: File): String {
        val instant = Instant.fromEpochMilliseconds(file.lastModified())
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "$month ${local.dayOfMonth}, ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
}
