package com.vandendaelen.nicephore.utils

import com.vandendaelen.nicephore.enums.OperatingSystems
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.Image
import java.awt.datatransfer.Clipboard
import java.io.File

object CopyImageToClipBoard : ClipboardOwner {
    private var lastScreenshot: File? = null

    fun setLastScreenshot(screenshot: File) {
        lastScreenshot = screenshot
    }

    fun copyImage(screenshot: File): Boolean {
        val manager = OperatingSystems.getOS().manager
        if (manager != null) {
            manager.clipboardImage(screenshot)
            return true
        }
        return false
    }

    fun copyLastScreenshot(): Boolean {
        return lastScreenshot?.let { copyImage(it) } ?: false
    }

    override fun lostOwnership(clip: Clipboard, trans: Transferable) {
        println("Lost Clipboard Ownership")
    }

    private class TransferableImage(private val image: Image) : Transferable {
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor == DataFlavor.imageFlavor) {
                return image
            }
            throw UnsupportedFlavorException(flavor)
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            transferDataFlavors.any { it == flavor }
    }
}
