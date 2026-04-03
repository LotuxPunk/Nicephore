package com.vandendaelen.nicephore.platform

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.config.NicephoreConfigProvider
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder

class NeoForgeConfigProvider : NicephoreConfigProvider {
    override fun getCompressionLevel(): Float = NicephoreConfig.Client.getCompressionLevel()
    override fun getJPEGToggle(): Boolean = NicephoreConfig.Client.getJPEGToggle()
    override fun setJPEGToggle(value: Boolean) = NicephoreConfig.Client.setJPEGToggle(value)
    override fun getOptimisedOutputToggle(): Boolean = NicephoreConfig.Client.getOptimisedOutputToggle()
    override fun getShouldShowOptStatus(): Boolean = NicephoreConfig.Client.getShouldShowOptStatus()
    override fun setShouldShowOptStatus(value: Boolean) = NicephoreConfig.Client.setShouldShowOptStatus(value)
    override fun getScreenshotToClipboard(): Boolean = NicephoreConfig.Client.getScreenshotToClipboard()
    override fun setScreenshotToClipboard(value: Boolean) = NicephoreConfig.Client.setScreenshotToClipboard(value)
    override fun getScreenshotCustomMessage(): Boolean = NicephoreConfig.Client.getScreenshotCustomMessage()
    override fun setScreenshotCustomMessage(value: Boolean) = NicephoreConfig.Client.setScreenshotCustomMessage(value)
    override fun getPNGOptimisationLevel(): Byte = NicephoreConfig.Client.getPNGOptimisationLevel()
    override fun getScreenshotFilter(): ScreenshotFilter = NicephoreConfig.Client.getScreenshotFilter()
    override fun setScreenshotFilter(filter: ScreenshotFilter) = NicephoreConfig.Client.setScreenshotFilter(filter)
    override fun getGalleryColumns(): Int = NicephoreConfig.Client.getGalleryColumns()
    override fun setGalleryColumns(value: Int) = NicephoreConfig.Client.setGalleryColumns(value)
    override fun getSortOrder(): SortOrder = NicephoreConfig.Client.getSortOrder()
    override fun setSortOrder(value: SortOrder) = NicephoreConfig.Client.setSortOrder(value)
}
