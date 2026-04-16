package com.vandendaelen.nicephore.clipboard.impl

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.clipboard.ClipboardManager
import java.io.File
import java.io.IOException

class MacOSClipboardManagerImpl private constructor() : ClipboardManager {
    override fun clipboardImage(screenshot: File): Boolean {
        val escapedPath = screenshot.absolutePath
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        val cmd = arrayOf(
            "osascript", "-e",
            "tell app \"Finder\" to set the clipboard to ( POSIX file \"$escapedPath\" )"
        )
        return try {
            Runtime.getRuntime().exec(cmd)
            true
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to copy screenshot to macOS clipboard", e)
            false
        }
    }

    companion object {
        val instance: MacOSClipboardManagerImpl by lazy { MacOSClipboardManagerImpl() }
    }
}
