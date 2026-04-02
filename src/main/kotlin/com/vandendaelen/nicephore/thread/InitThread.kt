package com.vandendaelen.nicephore.thread

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Reference
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.zip.ZipInputStream

class InitThread(private val optimiseConfig: Boolean) : Thread() {

    override fun run() {
        if (!optimiseConfig) return

        try {
            if (Files.exists(DESTINATION.toPath())) {
                updateExistingInstall()
            } else {
                freshInstall()
            }
        } catch (e: Exception) {
            Nicephore.LOGGER.error("Failed to initialize optimization tools", e)
            notifyToolsUnavailable()
        }
    }

    private fun updateExistingInstall() {
        try {
            val response = getResponse(getJsonReader(REFERENCES_JSON))
            if (response != null) {
                applyResponse(response)
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to read local references.json, attempting remote update", e)
        }

        try {
            val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))
            if (response != null) {
                if (Reference.Version.OXIPNG != response.oxipng_version) {
                    Reference.Version.OXIPNG = response.oxipng_version
                    downloadAndExtract(response.oxipng, OXIPNG_ZIP, "oxipng")
                }
                if (Reference.Version.ECT != response.ect_version) {
                    Reference.Version.ECT = response.ect_version
                    downloadAndExtract(response.ect, ECT_ZIP, "ECT")
                }
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to check for tool updates", e)
            notifyToolsUnavailable()
        }
    }

    private fun freshInstall() {
        try {
            Files.createDirectory(DESTINATION.toPath())
            val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))

            if (response != null) {
                applyResponse(response)
                downloadAndExtract(response.oxipng, OXIPNG_ZIP, "oxipng")
                downloadAndExtract(response.ect, ECT_ZIP, "ECT")
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed fresh install of optimization tools", e)
            notifyToolsUnavailable()
        }
    }

    private fun applyResponse(response: Response) {
        Reference.Command.OXIPNG = response.oxipng_command
        Reference.Command.ECT = response.ect_command
        Reference.File.OXIPNG = response.oxipng_file
        Reference.File.ECT = response.ect_file
        Reference.Version.OXIPNG = response.oxipng_version
        Reference.Version.ECT = response.ect_version
    }

    private fun notifyToolsUnavailable() {
        Minecraft.getInstance().execute {
            PlayerHelper.sendMessage(
                Component.translatable("nicephore.tools.unavailable")
            )
        }
    }

    private fun getResponse(reader: JsonReader): Response? {
        val gson = Gson()
        val collectionType = object : TypeToken<Collection<Response>>() {}.type
        val responses: Collection<Response> = gson.fromJson(reader, collectionType)
        return responses.firstOrNull { it.platform == OperatingSystems.getOS().name }
    }

    internal class Response {
        var platform: String = ""
        var oxipng: String = ""
        var oxipng_file: String = ""
        var oxipng_command: String = ""
        var oxipng_version: String = ""
        var ect: String = ""
        var ect_file: String = ""
        var ect_command: String = ""
        var ect_version: String = ""
    }

    @Throws(IOException::class)
    private fun getJsonReader(url: String, file: File): JsonReader {
        FileUtils.copyURLToFile(URI(url).toURL(), file, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
        return getJsonReader(file)
    }

    @Throws(FileNotFoundException::class)
    private fun getJsonReader(file: File): JsonReader {
        return JsonReader(FileReader(file))
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000

        private val DESTINATION = File(
            Minecraft.getInstance().gameDirectory.absolutePath,
            "mods${File.separator}nicephore"
        )
        private val REFERENCES_JSON = File(DESTINATION, "${File.separator}references.json")
        private val OXIPNG_ZIP = File(DESTINATION, "${File.separator}oxipng.zip")
        private val ECT_ZIP = File(DESTINATION, "${File.separator}ect.zip")

        private fun downloadAndExtract(url: String, zip: File, toolName: String) {
            var lastException: Exception? = null

            for (attempt in 1..MAX_RETRIES) {
                try {
                    downloadFile(url, zip)
                    unzip(zip.absolutePath, DESTINATION.absolutePath)
                    Nicephore.LOGGER.info("Successfully downloaded {}", toolName)
                    return
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < MAX_RETRIES) {
                        Nicephore.LOGGER.warn("Retrying download of {} (attempt {}/{})", toolName, attempt + 1, MAX_RETRIES)
                        sleep(RETRY_DELAY_MS)
                    }
                }
            }

            Nicephore.LOGGER.error("Failed to download {} after {} attempts", toolName, MAX_RETRIES, lastException)
        }

        private fun downloadFile(url: String, destination: File) {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS.toLong()))
                .build()

            val request = HttpRequest.newBuilder()
                .uri(URI(url))
                .timeout(Duration.ofMillis(READ_TIMEOUT_MS.toLong()))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() != 200) {
                throw IOException("HTTP ${response.statusCode()} downloading $url")
            }

            response.body().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }

        private fun unzip(zipFilePath: String, destDir: String) {
            val dir = File(destDir)
            if (!dir.exists()) dir.mkdirs()
            val destDirPath = dir.canonicalPath
            FileInputStream(zipFilePath).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var ze = zis.nextEntry
                    while (ze != null) {
                        val fileName = ze.name
                        val newFile = File(destDir + File.separator + fileName)
                        if (!newFile.canonicalPath.startsWith(destDirPath + File.separator)) {
                            throw IOException("Entry is outside of the target dir: $fileName")
                        }
                        Nicephore.LOGGER.debug("Extracting {}", newFile.absolutePath)
                        File(newFile.parent).mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                        ze = zis.nextEntry
                    }
                    zis.closeEntry()
                }
            }
        }
    }
}
