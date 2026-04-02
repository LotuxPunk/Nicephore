package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.Util
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FilenameUtils
import java.awt.Color
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

class ScrollableGalleryWidget(
    x: Int, y: Int, width: Int, height: Int,
    private val files: List<File>,
    private val loader: ScreenshotLoader,
    private val selectionMode: Boolean,
    private val selectedIndices: MutableSet<Int>,
    private val onThumbnailClick: (Int) -> Unit,
    private val onSelectionChange: () -> Unit
) : AbstractWidget(x, y, width, height, Component.empty()) {

    var scrollOffset: Float = 0f
        private set

    private val columns: Int
    private val imageWidth: Int
    private val imageHeight: Int
    private val slotHeight: Int
    private val totalRows: Int
    private val separatorRows: Set<Int>
    private val separatorLabels: Map<Int, String>

    init {
        val configColumns = NicephoreConfig.Client.getGalleryColumns()
        val availableWidth = width - SCROLLBAR_WIDTH - GRID_PADDING
        columns = if (configColumns in 2..6) configColumns
                  else (availableWidth / (TARGET_THUMB_WIDTH + GRID_PADDING)).coerceIn(2, 6)

        imageWidth = (availableWidth - (columns - 1) * GRID_PADDING) / columns
        imageHeight = (imageWidth / ASPECT_RATIO).toInt()
        slotHeight = imageHeight + LABEL_HEIGHT + GRID_PADDING
        totalRows = if (files.isEmpty()) 0 else ceil(files.size / columns.toDouble()).toInt()

        val sepRows = mutableSetOf<Int>()
        val sepLabels = mutableMapOf<Int, String>()
        if (files.isNotEmpty()) {
            val tz = TimeZone.currentSystemDefault()
            var prevDate = fileDate(files[0], tz)
            for (i in 1 until files.size) {
                val curDate = fileDate(files[i], tz)
                if (curDate != prevDate && i % columns == 0) {
                    val row = i / columns
                    sepRows.add(row)
                    sepLabels[row] = formatSeparatorDate(curDate)
                }
                prevDate = curDate
            }
        }
        separatorRows = sepRows
        separatorLabels = sepLabels
    }

    private fun fileDate(file: File, tz: TimeZone): kotlinx.datetime.LocalDate {
        return Instant.fromEpochMilliseconds(file.lastModified()).toLocalDateTime(tz).date
    }

    private fun formatSeparatorDate(date: kotlinx.datetime.LocalDate): String {
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "$month ${date.dayOfMonth}, ${date.year}"
    }

    private fun totalContentHeight(): Int {
        return GRID_PADDING + totalRows * slotHeight + separatorRows.size * SEPARATOR_HEIGHT
    }

    private fun maxScroll(): Float {
        return (totalContentHeight() - height).coerceAtLeast(0).toFloat()
    }

    fun setScrollOffset(offset: Float) {
        scrollOffset = offset.coerceIn(0f, maxScroll())
    }

    private fun rowContentY(row: Int): Int {
        val separatorsAbove = separatorRows.count { it <= row }
        return GRID_PADDING + row * slotHeight + separatorsAbove * SEPARATOR_HEIGHT
    }

    private fun visibleRowRange(): IntRange {
        if (totalRows == 0) return IntRange.EMPTY
        val topY = scrollOffset.toInt()
        val bottomY = topY + height

        var firstRow = 0
        for (r in 0 until totalRows) {
            if (rowContentY(r) + slotHeight > topY) {
                firstRow = r
                break
            }
        }
        var lastRow = totalRows - 1
        for (r in firstRow until totalRows) {
            if (rowContentY(r) > bottomY) {
                lastRow = r - 1
                break
            }
        }

        val bufferedFirst = (firstRow - 1).coerceAtLeast(0)
        val bufferedLast = (lastRow + 1).coerceAtMost(totalRows - 1)
        return bufferedFirst..bufferedLast
    }

    fun updateVisibleSlots() {
        val range = visibleRowRange()
        val activeIndices = mutableSetOf<Int>()
        for (row in range) {
            for (col in 0 until columns) {
                val fileIndex = row * columns + col
                if (fileIndex < files.size) {
                    activeIndices.add(fileIndex)
                    loader.loadSlot(fileIndex, files[fileIndex], "gallery", useThumbnails = true)
                }
            }
        }
        loader.releaseAllExcept(activeIndices)
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.enableScissor(x, y, x + width - SCROLLBAR_WIDTH, y + height)

        val range = visibleRowRange()
        val font = Minecraft.getInstance().font

        for (row in range) {
            if (separatorRows.contains(row)) {
                val sepY = y + rowContentY(row) - SEPARATOR_HEIGHT - scrollOffset.toInt()
                val label = separatorLabels[row] ?: ""
                val labelWidth = font.width(label)
                val gridMidX = x + (width - SCROLLBAR_WIDTH) / 2
                val lineY = sepY + SEPARATOR_HEIGHT / 2

                guiGraphics.fill(x + GRID_PADDING, lineY, gridMidX - labelWidth / 2 - 4, lineY + 1, SEPARATOR_COLOR)
                guiGraphics.text(font, label, gridMidX - labelWidth / 2, sepY + 2, SEPARATOR_COLOR)
                guiGraphics.fill(gridMidX + labelWidth / 2 + 4, lineY, x + width - SCROLLBAR_WIDTH - GRID_PADDING, lineY + 1, SEPARATOR_COLOR)
            }

            for (col in 0 until columns) {
                val fileIndex = row * columns + col
                if (fileIndex >= files.size) continue

                val file = files[fileIndex]
                val slotX = x + GRID_PADDING + col * (imageWidth + GRID_PADDING)
                val slotY = y + rowContentY(row) - scrollOffset.toInt()

                val slot = loader.getSlotState(fileIndex)
                when (slot.state) {
                    ScreenshotLoader.LoadState.LOADED -> {
                        slot.loaded?.let {
                            guiGraphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                it.textureId,
                                slotX, slotY, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight
                            )
                        }

                        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
                            guiGraphics.text(font, FilenameUtils.getExtension(file.name).uppercase(), slotX + 2, slotY + imageHeight - 12, Color.WHITE.rgb)
                        }

                        if (mouseX >= slotX && mouseX < slotX + imageWidth && mouseY >= slotY && mouseY < slotY + imageHeight) {
                            val overlayY = slotY + imageHeight - OVERLAY_HEIGHT
                            guiGraphics.fill(slotX, overlayY, slotX + imageWidth, slotY + imageHeight, OVERLAY_COLOR)
                            val dateText = Util.formatFileDate(file)
                            val sizeText = Util.formatFileSize(file)
                            guiGraphics.text(font, dateText, slotX + 2, overlayY + 2, Color.WHITE.rgb)
                            guiGraphics.text(font, sizeText, slotX + imageWidth - font.width(sizeText) - 2, overlayY + 2, Color.WHITE.rgb)
                        }

                        if (selectionMode) {
                            val checkX = slotX + 2
                            val checkY = slotY + 2
                            val isSelected = selectedIndices.contains(fileIndex)
                            guiGraphics.fill(checkX, checkY, checkX + 10, checkY + 10, if (isSelected) 0xFF00FF00.toInt() else 0xAAFFFFFF.toInt())
                            if (isSelected) {
                                guiGraphics.text(font, "\u2713", checkX + 1, checkY, Color.BLACK.rgb)
                            }
                        }

                        val name = file.nameWithoutExtension
                        val maxChars = imageWidth / 6
                        val displayName = if (name.length > maxChars) name.take(maxChars - 1) + "\u2026" else name
                        guiGraphics.text(font, displayName, slotX, slotY + imageHeight + 2, Color.WHITE.rgb)
                    }
                    ScreenshotLoader.LoadState.LOADING -> {
                        guiGraphics.centeredText(font, Component.translatable("nicephore.screenshots.loading"),
                            slotX + imageWidth / 2, slotY + imageHeight / 2, Color.GRAY.rgb)
                    }
                    ScreenshotLoader.LoadState.ERROR -> {
                        guiGraphics.centeredText(font, Component.translatable("nicephore.screenshots.error"),
                            slotX + imageWidth / 2, slotY + imageHeight / 2, Color.RED.rgb)
                    }
                }
            }
        }

        guiGraphics.disableScissor()

        if (totalContentHeight() > height) {
            val scrollbarX = x + width - SCROLLBAR_WIDTH
            val trackHeight = height
            val thumbHeight = (height.toFloat() / totalContentHeight() * trackHeight).toInt().coerceIn(20, trackHeight)
            val thumbY = y + ((scrollOffset / maxScroll()) * (trackHeight - thumbHeight)).roundToInt()

            guiGraphics.fill(scrollbarX, y, scrollbarX + SCROLLBAR_WIDTH, y + height, SCROLLBAR_TRACK_COLOR)
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR)
        }
    }

    override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
        val mouseX = event.x
        val mouseY = event.y
        if (mouseX >= x + width - SCROLLBAR_WIDTH) return

        val contentMouseY = mouseY.toInt() - y + scrollOffset.toInt()
        for (row in visibleRowRange()) {
            val rowY = rowContentY(row)
            for (col in 0 until columns) {
                val fileIndex = row * columns + col
                if (fileIndex >= files.size) continue

                val slotX = x + GRID_PADDING + col * (imageWidth + GRID_PADDING)
                if (mouseX.toInt() >= slotX && mouseX.toInt() < slotX + imageWidth &&
                    contentMouseY >= rowY && contentMouseY < rowY + imageHeight) {
                    if (selectionMode) {
                        if (selectedIndices.contains(fileIndex)) selectedIndices.remove(fileIndex)
                        else selectedIndices.add(fileIndex)
                        onSelectionChange()
                    } else {
                        onThumbnailClick(fileIndex)
                    }
                    return
                }
            }
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scrollOffset = (scrollOffset - verticalAmount.toFloat() * SCROLL_SPEED).coerceIn(0f, maxScroll())
        updateVisibleSlots()
        return true
    }

    override fun updateWidgetNarration(narration: NarrationElementOutput) {
        defaultButtonNarrationText(narration)
    }

    companion object {
        private const val ASPECT_RATIO = 16f / 9f
        private const val TARGET_THUMB_WIDTH = 150
        private const val GRID_PADDING = 8
        private const val LABEL_HEIGHT = 12
        private const val SEPARATOR_HEIGHT = 16
        private const val OVERLAY_HEIGHT = 12
        private const val SCROLLBAR_WIDTH = 6
        private const val SCROLL_SPEED = 30f

        private const val SEPARATOR_COLOR = 0xFF999999.toInt()
        private const val OVERLAY_COLOR = 0xAA000000.toInt()
        private const val SCROLLBAR_TRACK_COLOR = 0x44FFFFFF.toInt()
        private const val SCROLLBAR_THUMB_COLOR = 0xAAFFFFFF.toInt()
    }
}
