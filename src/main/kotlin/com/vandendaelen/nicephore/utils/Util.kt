package com.vandendaelen.nicephore.utils

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.config.NicephoreConfig
import net.minecraft.client.renderer.texture.DynamicTexture
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.stream.IntStream
import java.util.stream.Stream

object Util {
    fun fileToTexture(file: File): DynamicTexture {
        var nativeImage: NativeImage? = null
        try {
            val inputStream = FileInputStream(file)
            nativeImage = NativeImage.read(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return DynamicTexture({ "nicephore_screenshot" }, nativeImage!!)
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
                val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
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
            e.printStackTrace()
            emptyList()
        }
    }

    fun getNumberOfFiles(directory: File): Long {
        return try {
            Files.list(directory.toPath()).use { stream ->
                stream.filter { path -> !Files.isDirectory(path) }.count()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            0L
        }
    }
}
