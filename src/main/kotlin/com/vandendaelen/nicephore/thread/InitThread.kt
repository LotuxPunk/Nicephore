package com.vandendaelen.nicephore.thread

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.Reference
import net.minecraft.client.Minecraft
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipInputStream

class InitThread(private val optimiseConfig: Boolean) : Thread() {

    override fun run() {
        if (optimiseConfig) {
            if (Files.exists(DESTINATION.toPath())) {
                try {
                    val response = getResponse(getJsonReader(REFERENCES_JSON))
                    if (response != null) {
                        Reference.Command.OXIPNG = response.oxipng_command
                        Reference.Command.ECT = response.ect_command
                        Reference.File.OXIPNG = response.oxipng_file
                        Reference.File.ECT = response.ect_file
                        Reference.Version.OXIPNG = response.oxipng_version
                        Reference.Version.ECT = response.ect_version
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                try {
                    val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))
                    if (response != null) {
                        if (Reference.Version.OXIPNG != response.oxipng_version) {
                            Reference.Version.OXIPNG = response.oxipng_version
                            downloadAndExtract(response.oxipng, OXIPNG_ZIP)
                        }
                        if (Reference.Version.ECT != response.ect_version) {
                            Reference.Version.ECT = response.ect_version
                            downloadAndExtract(response.ect, ECT_ZIP)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                freshInstall()
            }
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
        FileUtils.copyURLToFile(URI(url).toURL(), file)
        return getJsonReader(file)
    }

    @Throws(FileNotFoundException::class)
    private fun getJsonReader(file: File): JsonReader {
        return JsonReader(FileReader(file))
    }

    private fun freshInstall() {
        try {
            Files.createDirectory(DESTINATION.toPath())
            val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))

            if (response != null) {
                Reference.Command.OXIPNG = response.oxipng_command
                Reference.Command.ECT = response.ect_command
                Reference.File.OXIPNG = response.oxipng_file
                Reference.File.ECT = response.ect_file
                Reference.Version.OXIPNG = response.oxipng_version
                Reference.Version.ECT = response.ect_version

                downloadAndExtract(response.oxipng, OXIPNG_ZIP)
                downloadAndExtract(response.ect, ECT_ZIP)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val DESTINATION = File(
            Minecraft.getInstance().gameDirectory.absolutePath,
            "mods${File.separator}nicephore"
        )
        private val REFERENCES_JSON = File(DESTINATION, "${File.separator}references.json")
        private val OXIPNG_ZIP = File(DESTINATION, "${File.separator}oxipng.zip")
        private val ECT_ZIP = File(DESTINATION, "${File.separator}ect.zip")

        private fun downloadAndExtract(url: String, zip: File) {
            try {
                BufferedInputStream(URI(url).toURL().openStream()).use { input ->
                    FileOutputStream(zip).use { output ->
                        val dataBuffer = ByteArray(1024)
                        var bytesRead: Int
                        while (input.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                            output.write(dataBuffer, 0, bytesRead)
                        }
                    }
                }
                unzip(zip.absolutePath, DESTINATION.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun unzip(zipFilePath: String, destDir: String) {
            val dir = File(destDir)
            if (!dir.exists()) dir.mkdirs()
            val buffer = ByteArray(1024)
            try {
                FileInputStream(zipFilePath).use { fis ->
                    ZipInputStream(fis).use { zis ->
                        var ze = zis.nextEntry
                        while (ze != null) {
                            val fileName = ze.name
                            val newFile = File(destDir + File.separator + fileName)
                            println("Unzipping to ${newFile.absolutePath}")
                            File(newFile.parent).mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            zis.closeEntry()
                            ze = zis.nextEntry
                        }
                        zis.closeEntry()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
