package com.vandendaelen.nicephore.utils

import com.vandendaelen.nicephore.enums.ScreenshotFilter

fun interface FilterListener {
    fun onFilterChange(filter: ScreenshotFilter)
}
