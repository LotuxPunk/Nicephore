package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder

/**
 * TEMPORARY STUB — replaced by NicephoreConfigHolder in Task 10.
 * Provides the same static API as the old ModConfigSpec-based config so that
 * files moved to :common in Task 5 continue to compile.
 * At runtime, the NeoForge entry point registers the real config before these are called.
 */
object NicephoreConfig {
    object Client {
        @Volatile private var _compressionLevel: Float = 0.9f
        @Volatile private var _jpegToggle: Boolean = false
        @Volatile private var _optimisedOutputToggle: Boolean = false
        @Volatile private var _shouldShowOptStatus: Boolean = true
        @Volatile private var _screenshotToClipboard: Boolean = true
        @Volatile private var _screenshotCustomMessage: Boolean = true
        @Volatile private var _pngOptimisationLevel: Byte = 2
        @Volatile private var _screenshotFilter: ScreenshotFilter = ScreenshotFilter.BOTH
        @Volatile private var _galleryColumns: Int = 0
        @Volatile private var _sortOrder: SortOrder = SortOrder.NEWEST

        fun getCompressionLevel(): Float = _compressionLevel
        fun getJPEGToggle(): Boolean = _jpegToggle
        fun setJPEGToggle(v: Boolean) { _jpegToggle = v }
        fun getOptimisedOutputToggle(): Boolean = _optimisedOutputToggle
        fun getShouldShowOptStatus(): Boolean = _shouldShowOptStatus
        fun setShouldShowOptStatus(v: Boolean) { _shouldShowOptStatus = v }
        fun getScreenshotToClipboard(): Boolean = _screenshotToClipboard
        fun setScreenshotToClipboard(v: Boolean) { _screenshotToClipboard = v }
        fun getScreenshotCustomMessage(): Boolean = _screenshotCustomMessage
        fun setScreenshotCustomMessage(v: Boolean) { _screenshotCustomMessage = v }
        fun getPNGOptimisationLevel(): Byte = _pngOptimisationLevel
        fun getScreenshotFilter(): ScreenshotFilter = _screenshotFilter
        fun setScreenshotFilter(v: ScreenshotFilter) { _screenshotFilter = v }
        fun getGalleryColumns(): Int = _galleryColumns
        fun setGalleryColumns(v: Int) { _galleryColumns = v }
        fun getSortOrder(): SortOrder = _sortOrder
        fun setSortOrder(v: SortOrder) { _sortOrder = v }
    }
}
