package com.vandendaelen.nicephore.thread

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.Services
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Reference
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.text.MessageFormat
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream

class ScreenshotThread(
    private val image: NativeImage,
    private val screenshot: File
) : Thread() {

    override fun run() {
        try {
            val tempFile = Files.createTempFile("nicephore_", ".png")
            try {
                image.writeToFile(tempFile)
            } finally {
                image.close()
            }
            val png = ImageIO.read(tempFile.toFile())
            Files.deleteIfExists(tempFile)
            val jpegFile = File(screenshot.parentFile, screenshot.name.replace("png", "jpg"))
            val result = BufferedImage(png.width, png.height, BufferedImage.TYPE_INT_RGB)
            result.createGraphics().drawImage(png, 0, 0, Color.WHITE, null)

            if (Services.config.getJPEGToggle()) {
                val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                val params = writer.defaultWriteParam
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.progressiveMode = ImageWriteParam.MODE_DEFAULT
                params.compressionQuality = Services.config.getCompressionLevel()
                FileImageOutputStream(jpegFile).use { outputStream ->
                    writer.output = outputStream
                    writer.write(null, IIOImage(result, null, null), params)
                    writer.dispose()
                }
            }

            if (Services.config.getOptimisedOutputToggle()) {
                val shouldShowOptStatus = Services.config.getShouldShowOptStatus()
                if (shouldShowOptStatus) {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshot.optimize"))
                }

                if (Services.config.getJPEGToggle()) {
                    try {
                        val ect = File("mods${File.separator}nicephore${File.separator}${Reference.File.ECT}")
                        val cmd = MessageFormat.format(Reference.Command.ECT, ect, jpegFile)
                        ProcessBuilder(cmd.split(" ")).start().waitFor()
                    } catch (e: Exception) {
                        Nicephore.LOGGER.warn("ECT not found or failed, skipping JPEG optimization", e)
                    }
                }

                try {
                    val oxipng = File("mods${File.separator}nicephore${File.separator}${Reference.File.OXIPNG}")
                    val pngFile = File(screenshot.parentFile, screenshot.name)
                    val cmd = MessageFormat.format(Reference.Command.OXIPNG, oxipng, Services.config.getPNGOptimisationLevel(), pngFile)
                    ProcessBuilder(cmd.split(" ")).start().waitFor()
                } catch (e: Exception) {
                    Nicephore.LOGGER.warn("oxipng not found or failed, skipping PNG optimization", e)
                }

                if (shouldShowOptStatus) {
                    PlayerHelper.sendHotbarMessage(Component.translatable("nicephore.screenshot.optimizeFinished"))
                }
            }

            CopyImageToClipBoard.setLastScreenshot(screenshot)

            if (Services.config.getScreenshotCustomMessage()) {
                if (Services.config.getScreenshotToClipboard()) {
                    if (CopyImageToClipBoard.copyLastScreenshot()) {
                        PlayerHelper.sendMessage(
                            Component.translatable("nicephore.clipboard.success").withStyle(ChatFormatting.GREEN)
                        )
                    } else {
                        PlayerHelper.sendMessage(
                            Component.translatable("nicephore.clipboard.error").withStyle(ChatFormatting.RED)
                        )
                    }
                }

                val pngComponent = Component.translatable("nicephore.screenshot.png")
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle { style ->
                        style.withClickEvent(ClickEvent.OpenFile(screenshot.absolutePath))
                    }

                val jpgComponent = Component.translatable("nicephore.screenshot.jpg")
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle { style ->
                        style.withClickEvent(ClickEvent.OpenFile(jpegFile.absolutePath))
                    }

                val folderComponent = Component.translatable("nicephore.screenshot.folder")
                    .withStyle(ChatFormatting.UNDERLINE)
                    .withStyle { style ->
                        style.withClickEvent(ClickEvent.OpenFile(screenshot.parent))
                    }

                PlayerHelper.sendMessage(
                    Component.translatable("nicephore.screenshot.success", screenshot.name.replace(".png", ""))
                )

                if (Services.config.getJPEGToggle()) {
                    PlayerHelper.sendMessage(
                        Component.translatable("nicephore.screenshot.options", pngComponent, jpgComponent, folderComponent)
                    )
                } else {
                    PlayerHelper.sendMessage(
                        Component.translatable("nicephore.screenshot.reducedOptions", pngComponent, folderComponent)
                    )
                }
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.error(e.message)
            PlayerHelper.sendMessage(
                Component.translatable("nicephore.screenshot.error").withStyle(ChatFormatting.RED)
            )
        }
    }
}
