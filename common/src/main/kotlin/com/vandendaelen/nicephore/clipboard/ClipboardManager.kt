package com.vandendaelen.nicephore.clipboard

import java.io.File

fun interface ClipboardManager {
    fun clipboardImage(screenshot: File): Boolean
}
