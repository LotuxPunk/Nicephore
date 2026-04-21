package com.vandendaelen.nicephore.utils

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.PlatformContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.time.Duration.Companion.days

object TrashManager {
    private const val TRASH_DIR_NAME = ".nicephore_trash"
    private val MAX_AGE = 30.days

    private val screenshotsDir: File
        get() = PlatformContext.current.screenshotDir.toFile()

    val trashDir: File by lazy {
        File(screenshotsDir, TRASH_DIR_NAME).also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun moveToTrash(file: File): Boolean {
        return try {
            trashDir.mkdirs()
            val targetFile = getUniqueTrashFile(file.name)
            Files.move(file.toPath(), targetFile.toPath())
            Nicephore.LOGGER.info("Moved {} to trash", file.name)
            true
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to move {} to trash", file.name, e)
            false
        }
    }

    fun moveToTrash(files: List<File>): Int {
        return files.count { moveToTrash(it) }
    }

    fun restore(trashedFile: File): Boolean {
        return try {
            val targetFile = File(screenshotsDir, trashedFile.name)
            if (targetFile.exists()) {
                Nicephore.LOGGER.warn("Cannot restore {}: file already exists in screenshots", trashedFile.name)
                return false
            }
            Files.move(trashedFile.toPath(), targetFile.toPath())
            Nicephore.LOGGER.info("Restored {} from trash", trashedFile.name)
            true
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to restore {} from trash", trashedFile.name, e)
            false
        }
    }

    fun deletePermanently(file: File): Boolean {
        return try {
            file.delete().also {
                if (it) Nicephore.LOGGER.info("Permanently deleted {}", file.name)
            }
        } catch (e: Exception) {
            Nicephore.LOGGER.error("Failed to permanently delete {}", file.name, e)
            false
        }
    }

    fun emptyTrash(): Int {
        val files = listTrash()
        return files.count { deletePermanently(it) }
    }

    fun listTrash(): List<File> {
        return trashDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun trashCount(): Int {
        return trashDir.listFiles()?.count { it.isFile } ?: 0
    }

    fun cleanupOldFiles() {
        val cutoff = System.currentTimeMillis() - MAX_AGE.inWholeMilliseconds
        val oldFiles = listTrash().filter { it.lastModified() < cutoff }
        if (oldFiles.isNotEmpty()) {
            val count = oldFiles.count { deletePermanently(it) }
            Nicephore.LOGGER.info("Cleaned up {} old files from trash (>{}d)", count, MAX_AGE.inWholeDays)
        }
    }

    private fun getUniqueTrashFile(name: String): File {
        var file = File(trashDir, name)
        if (!file.exists()) return file

        val baseName = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")
        var counter = 1
        while (file.exists()) {
            file = File(trashDir, "${baseName}_$counter.$extension")
            counter++
        }
        return file
    }
}
