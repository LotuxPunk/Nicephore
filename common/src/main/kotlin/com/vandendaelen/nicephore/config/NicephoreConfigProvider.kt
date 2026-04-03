package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder

interface NicephoreConfigProvider {
    fun getCompressionLevel(): Float
    fun getJPEGToggle(): Boolean
    fun setJPEGToggle(value: Boolean)
    fun getOptimisedOutputToggle(): Boolean
    fun getShouldShowOptStatus(): Boolean
    fun setShouldShowOptStatus(value: Boolean)
    fun getScreenshotToClipboard(): Boolean
    fun setScreenshotToClipboard(value: Boolean)
    fun getScreenshotCustomMessage(): Boolean
    fun setScreenshotCustomMessage(value: Boolean)
    fun getPNGOptimisationLevel(): Byte
    fun getScreenshotFilter(): ScreenshotFilter
    fun setScreenshotFilter(filter: ScreenshotFilter)
    fun getGalleryColumns(): Int
    fun setGalleryColumns(value: Int)
    fun getSortOrder(): SortOrder
    fun setSortOrder(value: SortOrder)
}
