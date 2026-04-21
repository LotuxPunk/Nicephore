package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder
import kotlinx.serialization.Serializable

@Serializable
data class NicephoreConfig(
    val optimiseScreenshots: Boolean = OperatingSystems.getOS() == OperatingSystems.WINDOWS,
    val showOptimisationStatus: Boolean = true,
    val screenshotToClipboard: Boolean = true,
    val screenshotCustomMessage: Boolean = true,
    val screenshotFilter: ScreenshotFilter = ScreenshotFilter.BOTH,
    val galleryColumns: Int = 0,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val compression: Double = 0.9,
    val makeJPEGs: Boolean = OperatingSystems.getOS() == OperatingSystems.WINDOWS,
    val pngOptimisationLevel: Int = 2,
)
