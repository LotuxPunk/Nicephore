package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.TrashManager
import com.vandendaelen.nicephore.utils.Util
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File

class GalleryScreen(private var index: Int = 0) : AbstractNicephoreScreen(TITLE), FilterListener {
    private var allScreenshots: List<File> = emptyList()
    private var pageScreenshots: List<File> = emptyList()
    private var groups: List<ScreenshotGroup> = emptyList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    private var selectionMode: Boolean = false
    private val selectedIndices: MutableSet<Int> = mutableSetOf()
    private lateinit var grid: GridLayout
    private var contentTopY: Int = TOOLBAR_HEIGHT
    private var itemsPerPage: Int = 1

    private fun getNumberOfPages(): Int {
        return kotlin.math.ceil(allScreenshots.size / itemsPerPage.toDouble()).toInt().coerceAtLeast(1)
    }

    override fun init() {
        super.init()
        selectedIndices.clear()

        // Gallery always uses two toolbar rows
        contentTopY = TOOLBAR_HEIGHT + BUTTON_HEIGHT + PADDING
        val extraToolbarOffset = contentTopY - TOOLBAR_HEIGHT

        grid = computeGrid(this.width, this.height - extraToolbarOffset - PAGE_TEXT_HEIGHT, aspectRatio, NicephoreConfig.Client.getGalleryColumns())
        itemsPerPage = grid.imagesToDisplay

        val sortOrder = NicephoreConfig.Client.getSortOrder()
        allScreenshots = loadAllScreenshots(sortOrder)
        index = clampIndex(index, getNumberOfPages())

        val skip = itemsPerPage * index
        pageScreenshots = allScreenshots.drop(skip).take(itemsPerPage)

        aspectRatio = if (pageScreenshots.isNotEmpty()) readAspectRatio(pageScreenshots[0]) else DEFAULT_ASPECT_RATIO

        groups = if (sortOrder.useDateGroups) {
            computeDateGroups(pageScreenshots)
        } else {
            listOf(ScreenshotGroup("", pageScreenshots))
        }

        // Trim items whose slots would overflow into the bottom bar.
        // computeSlotY accounts for group headers and partial rows, so we
        // check each slot against the actual available space.
        val maxContentBottom = this.height - BOTTOM_BAR_HEIGHT - PAGE_TEXT_HEIGHT - PADDING
        for (i in pageScreenshots.indices) {
            if (computeSlotY(i) + grid.slotHeight > maxContentBottom) {
                itemsPerPage = i.coerceAtLeast(1)
                // Re-page with the reduced item count
                pageScreenshots = allScreenshots.drop(itemsPerPage * index).take(itemsPerPage)
                groups = if (sortOrder.useDateGroups) computeDateGroups(pageScreenshots)
                         else listOf(ScreenshotGroup("", pageScreenshots))
                break
            }
        }

        if (pageScreenshots.isNotEmpty()) {
            loader.setOnLoadComplete { refreshWidgets() }
            loader.loadBatch(pageScreenshots, "gallery", useThumbnails = true)
        }

        refreshWidgets()
    }

    private fun loadAllScreenshots(sortOrder: SortOrder): List<File> {
        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        return SCREENSHOTS_DIR.listFiles(filter)
            ?.sortedWith(sortOrder.comparator)
            ?: emptyList()
    }

    private fun computeDateGroups(files: List<File>): List<ScreenshotGroup> {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val weekAgo = today.minus(7, DateTimeUnit.DAY)
        val monthAgo = today.minus(30, DateTimeUnit.DAY)

        val todayFiles = mutableListOf<File>()
        val yesterdayFiles = mutableListOf<File>()
        val weekFiles = mutableListOf<File>()
        val monthFiles = mutableListOf<File>()
        val olderFiles = mutableListOf<File>()

        for (file in files) {
            val fileDate = Instant.fromEpochMilliseconds(file.lastModified()).toLocalDateTime(tz).date
            when {
                fileDate == today -> todayFiles.add(file)
                fileDate == yesterday -> yesterdayFiles.add(file)
                fileDate > weekAgo -> weekFiles.add(file)
                fileDate > monthAgo -> monthFiles.add(file)
                else -> olderFiles.add(file)
            }
        }

        return listOfNotNull(
            todayFiles.takeIf { it.isNotEmpty() }?.let { ScreenshotGroup(Component.translatable("nicephore.group.today").string, it) },
            yesterdayFiles.takeIf { it.isNotEmpty() }?.let { ScreenshotGroup(Component.translatable("nicephore.group.yesterday").string, it) },
            weekFiles.takeIf { it.isNotEmpty() }?.let { ScreenshotGroup(Component.translatable("nicephore.group.thisWeek").string, it) },
            monthFiles.takeIf { it.isNotEmpty() }?.let { ScreenshotGroup(Component.translatable("nicephore.group.thisMonth").string, it) },
            olderFiles.takeIf { it.isNotEmpty() }?.let { ScreenshotGroup(Component.translatable("nicephore.group.older").string, it) },
        )
    }

    override fun buildWidgets() {
        // Row 1: Filter | Settings | Exit
        addToolbarButtons { cycleFilter() }

        // Row 2: Sort | Select | [All | None] | Trash
        val row2Y = PADDING + BUTTON_HEIGHT + PADDING
        val gap = 5
        var nextX = PADDING

        val sortOrder = NicephoreConfig.Client.getSortOrder()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.sort.label", Component.translatable(sortOrder.displayKey).string)) { cycleSortOrder() }
                .bounds(nextX, row2Y, 80, BUTTON_HEIGHT).build()
        )
        nextX += 80 + gap

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.select")) { toggleSelectionMode() }
                .bounds(nextX, row2Y, 50, BUTTON_HEIGHT).build()
        )
        nextX += 50 + gap

        if (selectionMode) {
            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.select.all")) { selectAll() }
                    .bounds(nextX, row2Y, 30, BUTTON_HEIGHT).build()
            )
            nextX += 30 + gap

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.select.none")) { selectedIndices.clear(); refreshWidgets() }
                    .bounds(nextX, row2Y, 40, BUTTON_HEIGHT).build()
            )
            nextX += 40 + gap

            if (selectedIndices.isNotEmpty()) {
                val centerX = this.width / 2
                val bottomLine = this.height - BOTTOM_BAR_HEIGHT
                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.move", selectedIndices.size)) { bulkMoveToTrash() }
                        .bounds(centerX - 60, bottomLine, 120, BUTTON_HEIGHT).build()
                )
            }
        }

        val trashCount = TrashManager.trashCount()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.trash", trashCount)) { openTrashScreen() }
                .bounds(this.width - PADDING - 50, row2Y, 50, BUTTON_HEIGHT).build()
        )

        if (pageScreenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            var slotIndex = 0
            for (group in groups) {
                for (file in group.files) {
                    val x = computeSlotX(slotIndex)
                    val y = computeSlotY(slotIndex)
                    val name = file.name
                    val text = Component.literal(StringUtils.abbreviate(name, grid.imageWidth / 6))

                    val capturedSlotIndex = slotIndex
                    this.addRenderableWidget(
                        Button.builder(text) {
                            if (selectionMode) {
                                toggleSelection(capturedSlotIndex)
                            } else {
                                openScreenshotScreen(allScreenshots.indexOf(file))
                            }
                        }.bounds(x, y + grid.imageHeight + 2, grid.imageWidth, BUTTON_HEIGHT).build()
                    )
                    slotIndex++
                }
            }
        }
    }

    private fun computeSlotX(slotIndex: Int): Int {
        var slotsConsumed = 0
        for (group in groups) {
            val slotsInThisGroup = slotIndex - slotsConsumed
            if (slotsInThisGroup < group.files.size) {
                val colInGroup = slotsInThisGroup % grid.columns
                return PADDING + colInGroup * (grid.imageWidth + PADDING)
            }
            slotsConsumed += group.files.size
        }
        return grid.slotX(slotIndex)
    }

    private fun computeSlotY(slotIndex: Int): Int {
        var y = contentTopY + grid.verticalOffset
        var slotsConsumed = 0

        for (group in groups) {
            if (group.label.isNotEmpty()) {
                y += GROUP_HEADER_HEIGHT
            }
            val groupSlotCount = group.files.size
            val slotsInThisGroup = slotIndex - slotsConsumed
            if (slotsInThisGroup < groupSlotCount) {
                val rowInGroup = slotsInThisGroup / grid.columns
                return y + rowInGroup * grid.slotHeight
            }
            val rowsInGroup = kotlin.math.ceil(groupSlotCount / grid.columns.toDouble()).toInt()
            y += rowsInGroup * grid.slotHeight
            slotsConsumed += groupSlotCount
        }
        return y
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val bottomLine = this.height - BOTTOM_BAR_HEIGHT

        if (pageScreenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + 20, Color.RED.rgb
            )
        } else {
            var slotIndex = 0

            for (group in groups) {
                if (group.label.isNotEmpty()) {
                    val headerY = computeSlotY(slotIndex) - GROUP_HEADER_HEIGHT
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.literal(group.label),
                        centerX, headerY, Color.LIGHT_GRAY.rgb
                    )
                }

                for (file in group.files) {
                    val x = computeSlotX(slotIndex)
                    val y = computeSlotY(slotIndex)

                    val slot = loader.getSlotState(slotIndex)
                    when (slot.state) {
                        ScreenshotLoader.LoadState.LOADED -> {
                            slot.loaded?.let {
                                guiGraphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    it.textureId,
                                    x, y, 0f, 0f, grid.imageWidth, grid.imageHeight, grid.imageWidth, grid.imageHeight
                                )
                            }
                            drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x + 2, y + grid.imageHeight - 12)

                            if (mouseX >= x && mouseX < x + grid.imageWidth && mouseY >= y && mouseY < y + grid.imageHeight) {
                                val overlayY = y + grid.imageHeight - OVERLAY_HEIGHT
                                guiGraphics.fill(x, overlayY, x + grid.imageWidth, y + grid.imageHeight, OVERLAY_COLOR)
                                val font = Minecraft.getInstance().font
                                val dateText = Util.formatFileDate(file)
                                val sizeText = Util.formatFileSize(file)
                                guiGraphics.text(font, dateText, x + 2, overlayY + 2, Color.WHITE.rgb)
                                guiGraphics.text(font, sizeText, x + grid.imageWidth - font.width(sizeText) - 2, overlayY + 2, Color.WHITE.rgb)
                            }

                            if (selectionMode) {
                                val checkX = x + 2
                                val checkY = y + 2
                                val isSelected = selectedIndices.contains(slotIndex)
                                guiGraphics.fill(checkX, checkY, checkX + 10, checkY + 10, if (isSelected) 0xFF00FF00.toInt() else 0xAAFFFFFF.toInt())
                                if (isSelected) {
                                    guiGraphics.text(Minecraft.getInstance().font, "✓", checkX + 1, checkY, Color.BLACK.rgb)
                                }
                            }
                        }
                        ScreenshotLoader.LoadState.LOADING -> {
                            guiGraphics.centeredText(
                                Minecraft.getInstance().font,
                                Component.translatable("nicephore.screenshots.loading"),
                                x + grid.imageWidth / 2, y + grid.imageHeight / 2, Color.GRAY.rgb
                            )
                        }
                        ScreenshotLoader.LoadState.ERROR -> {
                            guiGraphics.centeredText(
                                Minecraft.getInstance().font,
                                Component.translatable("nicephore.screenshots.error"),
                                x + grid.imageWidth / 2, y + grid.imageHeight / 2, Color.RED.rgb
                            )
                        }
                    }
                    slotIndex++
                }
            }

            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.gui.gallery.pages", index + 1, getNumberOfPages()),
                centerX, bottomLine - 12, Color.WHITE.rgb
            )
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun drawExtensionBadge(guiGraphics: GuiGraphicsExtractor, extension: String, x: Int, y: Int) {
        if (NicephoreConfig.Client.getScreenshotFilter() == ScreenshotFilter.BOTH) {
            guiGraphics.text(Minecraft.getInstance().font, extension.uppercase(), x, y, Color.WHITE.rgb)
        }
    }

    private fun modIndex(value: Int) {
        index = wrapIndex(index, value, getNumberOfPages())
        init()
    }

    private fun cycleSortOrder() {
        val next = NicephoreConfig.Client.getSortOrder().next()
        NicephoreConfig.Client.setSortOrder(next)
        index = 0
        init()
    }

    private fun openScreenshotScreen(value: Int) {
        Minecraft.getInstance().pushGuiLayer(ScreenshotScreen(value, index, this))
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    override fun onFilterChange(filter: ScreenshotFilter) {
        NicephoreConfig.Client.setScreenshotFilter(filter)
        init()
    }

    private fun toggleSelectionMode() {
        selectionMode = !selectionMode
        selectedIndices.clear()
        init()
    }

    private fun toggleSelection(slotIndex: Int) {
        if (selectedIndices.contains(slotIndex)) {
            selectedIndices.remove(slotIndex)
        } else {
            selectedIndices.add(slotIndex)
        }
        refreshWidgets()
    }

    private fun selectAll() {
        for (i in pageScreenshots.indices) {
            selectedIndices.add(i)
        }
        refreshWidgets()
    }

    private fun bulkMoveToTrash() {
        val filesToTrash = selectedIndices.mapNotNull { pageScreenshots.getOrNull(it) }
        Minecraft.getInstance().pushGuiLayer(
            DeleteConfirmScreen(filesToTrash, GalleryScreen(index))
        )
    }

    private fun openTrashScreen() {
        Minecraft.getInstance().pushGuiLayer(TrashScreen { init() })
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private const val OVERLAY_HEIGHT = 12
        private const val OVERLAY_COLOR = 0xAA000000.toInt()
        private const val GROUP_HEADER_HEIGHT = 16

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
