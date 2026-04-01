package com.vandendaelen.nicephore.enums

import java.io.File

enum class SortOrder(val displayKey: String, val comparator: Comparator<File>, val useDateGroups: Boolean) {
    NEWEST("nicephore.sort.newest", compareByDescending(File::lastModified), true),
    OLDEST("nicephore.sort.oldest", compareBy(File::lastModified), true),
    NAME_ASC("nicephore.sort.name_asc", compareBy(File::getName), false),
    NAME_DESC("nicephore.sort.name_desc", compareByDescending(File::getName), false);

    fun next(): SortOrder = entries[(ordinal + 1) % entries.size]
}
