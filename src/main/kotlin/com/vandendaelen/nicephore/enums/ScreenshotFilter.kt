package com.vandendaelen.nicephore.enums

import java.io.FilenameFilter

enum class ScreenshotFilter(val predicate: FilenameFilter) {
    JPEG(FilenameFilter { _, name -> name.endsWith(".jpg") }),
    PNG(FilenameFilter { _, name -> name.endsWith(".png") }),
    BOTH(FilenameFilter { _, name -> name.endsWith(".jpg") || name.endsWith(".png") });

    operator fun next(): ScreenshotFilter {
        val values = entries.toTypedArray()
        return values[(this.ordinal + 1) % values.size]
    }
}
