# Date Grouping & Sort Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Group gallery screenshots by date (Today, Yesterday, This Week, etc.) and add a sort order cycle button.

**Architecture:** New `SortOrder` enum + config property. GalleryScreen computes `ScreenshotGroup`s from the file list, renders group headers as full-width text rows, and thumbnails within each group. Date grouping only applies to date-based sorts.

**Tech Stack:** Kotlin, kotlinx-datetime (already added), NeoForge config

---

## File Structure

| File | Responsibility |
|------|---------------|
| `enums/SortOrder.kt` | **New** — NEWEST, OLDEST, NAME_ASC, NAME_DESC with comparator and display name |
| `config/NicephoreConfig.kt` | Add `sortOrder` enum config |
| `gui/GalleryScreen.kt` | Group computation, group header rendering, sort button, pagination with groups |

All paths relative to `src/main/kotlin/com/vandendaelen/nicephore/`.

---

### Task 1: Create SortOrder enum

**Files:**
- Create: `src/main/kotlin/com/vandendaelen/nicephore/enums/SortOrder.kt`

- [ ] **Step 1: Create SortOrder.kt**

```kotlin
package com.vandendaelen.nicephore.enums

import java.io.File

enum class SortOrder(val displayKey: String, val comparator: Comparator<File>, val useDateGroups: Boolean) {
    NEWEST("nicephore.sort.newest", compareByDescending(File::lastModified), true),
    OLDEST("nicephore.sort.oldest", compareBy(File::lastModified), true),
    NAME_ASC("nicephore.sort.name_asc", compareBy(File::getName), false),
    NAME_DESC("nicephore.sort.name_desc", compareByDescending(File::getName), false);

    fun next(): SortOrder = entries[(ordinal + 1) % entries.size]
}
```

- [ ] **Step 2: Add translation keys to en_us.json**

```json
"nicephore.sort.newest": "Newest",
"nicephore.sort.oldest": "Oldest",
"nicephore.sort.name_asc": "Name A-Z",
"nicephore.sort.name_desc": "Name Z-A",
"nicephore.sort.label": "Sort: %s",
"nicephore.group.today": "Today",
"nicephore.group.yesterday": "Yesterday",
"nicephore.group.thisWeek": "This Week",
"nicephore.group.thisMonth": "This Month",
"nicephore.group.older": "Older"
```

- [ ] **Step 3: Verify and commit**

Run: `./gradlew compileKotlin`
Commit: `feat: add SortOrder enum with comparators and date group flag`

---

### Task 2: Add sortOrder config property

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/config/NicephoreConfig.kt`

- [ ] **Step 1: Add sortOrder field and config**

Add field declaration in `Client` class (after `galleryColumns`):
```kotlin
val sortOrder: ModConfigSpec.EnumValue<SortOrder>
```

Add import: `import com.vandendaelen.nicephore.enums.SortOrder`

In init block, inside "GUI-specific settings" section (after galleryColumns, before `builder.pop()`):
```kotlin
sortOrder = builder
    .comment("Sort order for the gallery. Options: NEWEST, OLDEST, NAME_ASC, NAME_DESC")
    .defineEnum("sortOrder", SortOrder.NEWEST)
```

Add getter/setter in companion:
```kotlin
fun getSortOrder(): SortOrder = NicephoreConfig.CLIENT.sortOrder.get()

fun setSortOrder(value: SortOrder) {
    NicephoreConfig.CLIENT.sortOrder.set(value)
}
```

- [ ] **Step 2: Verify and commit**

Run: `./gradlew compileKotlin`
Commit: `feat: add sortOrder config property`

---

### Task 3: Rewrite GalleryScreen with date groups and sort order

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt`

- [ ] **Step 1: Rewrite GalleryScreen**

Replace the entire file with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.Util
import kotlinx.datetime.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Color
import java.io.File

data class ScreenshotGroup(
    val label: String,
    val files: List<File>
)

class GalleryScreen(private var index: Int = 0) : AbstractNicephoreScreen(TITLE), FilterListener {
    private var allScreenshots: List<File> = emptyList()
    private var pageScreenshots: List<File> = emptyList()
    private var groups: List<ScreenshotGroup> = emptyList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    private var columns: Int = 4
    private var rows: Int = 3
    private val imagesToDisplay: Int get() = rows * columns

    private fun computeGrid() {
        val configColumns = NicephoreConfig.Client.getGalleryColumns()
        val availableWidth = this.width - 2 * PADDING
        val availableHeight = this.height - TOOLBAR_HEIGHT - BOTTOM_BAR_HEIGHT - PADDING

        columns = if (configColumns in 2..6) {
            configColumns
        } else {
            (availableWidth / (TARGET_THUMBNAIL_WIDTH + PADDING)).coerceIn(2, 6)
        }

        val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
        val imageHeight = (imageWidth / aspectRatio).toInt()
        val slotHeight = imageHeight + BUTTON_HEIGHT + PADDING

        rows = ((availableHeight) / slotHeight).coerceIn(1, 4)
    }

    private fun getNumberOfPages(): Int {
        return kotlin.math.ceil(allScreenshots.size / imagesToDisplay.toDouble()).toInt().coerceAtLeast(1)
    }

    override fun init() {
        super.init()

        computeGrid()

        val sortOrder = NicephoreConfig.Client.getSortOrder()
        allScreenshots = loadAllScreenshots(sortOrder)
        index = clampIndex(index, getNumberOfPages())

        val skip = imagesToDisplay * index
        pageScreenshots = allScreenshots.drop(skip).take(imagesToDisplay)

        aspectRatio = if (pageScreenshots.isNotEmpty()) readAspectRatio(pageScreenshots[0]) else DEFAULT_ASPECT_RATIO

        groups = if (sortOrder.useDateGroups) {
            computeDateGroups(pageScreenshots)
        } else {
            listOf(ScreenshotGroup("", pageScreenshots))
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
        addToolbarButtons { cycleFilter() }

        // Sort button
        val sortOrder = NicephoreConfig.Client.getSortOrder()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.sort.label", Component.translatable(sortOrder.displayKey).string)) { cycleSortOrder() }
                .bounds(PADDING + 110, PADDING, 80, BUTTON_HEIGHT).build()
        )

        if (pageScreenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            var slotIndex = 0
            var currentY = TOOLBAR_HEIGHT

            for (group in groups) {
                if (group.label.isNotEmpty()) {
                    currentY += GROUP_HEADER_HEIGHT
                }

                for ((fileIndexInGroup, file) in group.files.withIndex()) {
                    val col = (slotIndex) % columns
                    if (col == 0 && slotIndex > 0) {
                        // Not handled here — y computed from slotIndex
                    }
                    val localRow = (slotIndex - (slotIndex / columns * columns)) // not needed, use col
                    val rowInPage = slotIndex / columns
                    val x = PADDING + col * (imageWidth + PADDING)

                    val name = file.name
                    val text = Component.literal(StringUtils.abbreviate(name, imageWidth / 6))

                    // We compute y per-slot in extractRenderState, but need button positions here too
                    // For buttons, use the same slot-to-position mapping
                    val buttonY = computeSlotY(slotIndex, imageHeight) + imageHeight + 2

                    this.addRenderableWidget(
                        Button.builder(text) { openScreenshotScreen(allScreenshots.indexOf(file)) }
                            .bounds(x, buttonY, imageWidth, BUTTON_HEIGHT).build()
                    )
                    slotIndex++
                }
            }
        }
    }

    private fun computeSlotY(slotIndex: Int, imageHeight: Int): Int {
        var y = TOOLBAR_HEIGHT
        var slotsConsumed = 0

        for (group in groups) {
            if (group.label.isNotEmpty()) {
                y += GROUP_HEADER_HEIGHT
            }
            val groupSlotCount = group.files.size
            val slotsInThisGroup = slotIndex - slotsConsumed
            if (slotsInThisGroup < groupSlotCount) {
                val rowInGroup = slotsInThisGroup / columns
                return y + rowInGroup * (imageHeight + BUTTON_HEIGHT + PADDING)
            }
            val rowsInGroup = kotlin.math.ceil(groupSlotCount / columns.toDouble()).toInt()
            y += rowsInGroup * (imageHeight + BUTTON_HEIGHT + PADDING)
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
            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            var slotIndex = 0

            for (group in groups) {
                if (group.label.isNotEmpty()) {
                    val headerY = computeSlotY(slotIndex, imageHeight) - GROUP_HEADER_HEIGHT + PADDING
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.literal(group.label),
                        centerX, headerY, Color.LIGHT_GRAY.rgb
                    )
                }

                for (file in group.files) {
                    val col = slotIndex % columns
                    val x = PADDING + col * (imageWidth + PADDING)
                    val y = computeSlotY(slotIndex, imageHeight)

                    val slot = loader.getSlotState(slotIndex)
                    when (slot.state) {
                        ScreenshotLoader.LoadState.LOADED -> {
                            slot.loaded?.let {
                                guiGraphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    it.textureId,
                                    x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight
                                )
                            }
                            drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x + 2, y + imageHeight - 12)

                            if (mouseX >= x && mouseX < x + imageWidth && mouseY >= y && mouseY < y + imageHeight) {
                                val overlayY = y + imageHeight - OVERLAY_HEIGHT
                                guiGraphics.fill(x, overlayY, x + imageWidth, y + imageHeight, OVERLAY_COLOR)
                                val font = Minecraft.getInstance().font
                                val dateText = Util.formatFileDate(file)
                                val sizeText = Util.formatFileSize(file)
                                guiGraphics.text(font, dateText, x + 2, overlayY + 2, Color.WHITE.rgb)
                                guiGraphics.text(font, sizeText, x + imageWidth - font.width(sizeText) - 2, overlayY + 2, Color.WHITE.rgb)
                            }
                        }
                        ScreenshotLoader.LoadState.LOADING -> {
                            guiGraphics.centeredText(
                                Minecraft.getInstance().font,
                                Component.translatable("nicephore.screenshots.loading"),
                                x + imageWidth / 2, y + imageHeight / 2, Color.GRAY.rgb
                            )
                        }
                        ScreenshotLoader.LoadState.ERROR -> {
                            guiGraphics.centeredText(
                                Minecraft.getInstance().font,
                                Component.translatable("nicephore.screenshots.error"),
                                x + imageWidth / 2, y + imageHeight / 2, Color.RED.rgb
                            )
                        }
                    }
                    slotIndex++
                }
            }

            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.gui.gallery.pages", index + 1, getNumberOfPages()),
                centerX, bottomLine + 5, Color.WHITE.rgb
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
```

- [ ] **Step 2: Verify and commit**

Run: `./gradlew compileKotlin`
Commit: `feat: add date grouping and sort order to gallery`

---

### Task 4: Final verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean compileKotlin`
Expected: BUILD SUCCESSFUL
