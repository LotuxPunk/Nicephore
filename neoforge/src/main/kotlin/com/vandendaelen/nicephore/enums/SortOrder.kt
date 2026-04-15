package com.vandendaelen.nicephore.enums

import java.io.File

enum class SortOrder(val displayKey: String, val comparator: Comparator<File>) {
    NEWEST("nicephore.sort.newest", compareByDescending(File::lastModified)),
    OLDEST("nicephore.sort.oldest", compareBy(File::lastModified));

    fun next(): SortOrder = entries[(ordinal + 1) % entries.size]
}
