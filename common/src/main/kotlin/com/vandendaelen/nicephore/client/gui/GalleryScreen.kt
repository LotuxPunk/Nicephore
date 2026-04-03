package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.platform.Services
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.TrashManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class GalleryScreen(
    private val initialScrollOffset: Float = 0f
) : AbstractNicephoreScreen(TITLE), FilterListener {

    private var allScreenshots: List<File> = emptyList()
    private val loader = ScreenshotLoader()
    private var selectionMode: Boolean = false
    private val selectedIndices: MutableSet<Int> = mutableSetOf()
    private var galleryWidget: ScrollableGalleryWidget? = null

    // Rainbow title state
    private var nextRainbowTime: Long = System.currentTimeMillis() + (10_000L..60_000L).random()
    private var rainbowStartTime: Long = 0L

    override fun init() {
        super.init()
        selectedIndices.clear()

        val sortOrder = Services.config.getSortOrder()
        val filter = Services.config.getScreenshotFilter().predicate
        allScreenshots = SCREENSHOTS_DIR.listFiles(filter)
            ?.sortedWith(sortOrder.comparator)
            ?: emptyList()

        loader.setOnLoadComplete { /* next frame picks up new textures */ }

        refreshWidgets()
    }

    override fun buildWidgets() {
        // --- Sidebar buttons ---
        var buttonY = SIDEBAR_PADDING + SIDEBAR_TITLE_HEIGHT

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", Services.config.getScreenshotFilter().name)) { cycleFilter() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        val sortOrder = Services.config.getSortOrder()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.sort.label", Component.translatable(sortOrder.displayKey).string)) { cycleSortOrder() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.select")) { toggleSelectionMode() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        val trashCount = TrashManager.trashCount()
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.trash", trashCount)) { openTrashScreen() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.settings")) { openSettingsScreen() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
        )
        buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

        // Selection mode buttons
        if (selectionMode) {
            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.select.all")) { selectAll() }
                    .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
            )
            buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.select.none")) { selectedIndices.clear(); refreshWidgets() }
                    .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
            )
            buttonY += BUTTON_HEIGHT + SIDEBAR_GAP

            if (selectedIndices.isNotEmpty()) {
                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.move", selectedIndices.size)) { bulkMoveToTrash() }
                        .bounds(SIDEBAR_PADDING, buttonY, SIDEBAR_BUTTON_WIDTH, BUTTON_HEIGHT).build()
                )
            }
        }

        // --- Scroll grid widget ---
        val gridX = SIDEBAR_WIDTH
        val gridWidth = this.width - SIDEBAR_WIDTH
        val widget = ScrollableGalleryWidget(
            x = gridX, y = 0, width = gridWidth, height = this.height,
            files = allScreenshots,
            loader = loader,
            selectionMode = selectionMode,
            selectedIndices = selectedIndices,
            onThumbnailClick = { fileIndex -> openScreenshotScreen(fileIndex) },
            onSelectionChange = { refreshWidgets() }
        )
        widget.setScrollOffset(initialScrollOffset)
        widget.updateVisibleSlots()
        galleryWidget = widget
        this.addRenderableWidget(widget)
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw sidebar background
        guiGraphics.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG_COLOR)

        // Draw branding title with occasional rainbow effect
        drawBrandingTitle(guiGraphics)

        if (allScreenshots.isEmpty()) {
            val gridCenterX = SIDEBAR_WIDTH + (this.width - SIDEBAR_WIDTH) / 2
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                gridCenterX, this.height / 2, Color.RED.rgb
            )
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun drawBrandingTitle(guiGraphics: GuiGraphicsExtractor) {
        val font = Minecraft.getInstance().font
        val title = "Nicephore"
        val now = System.currentTimeMillis()

        val isRainbow = now in rainbowStartTime until (rainbowStartTime + RAINBOW_DURATION_MS)

        // Schedule next rainbow
        if (now >= nextRainbowTime && !isRainbow) {
            rainbowStartTime = now
            nextRainbowTime = now + RAINBOW_DURATION_MS + (10_000L..60_000L).random()
        }

        val titleX = (SIDEBAR_WIDTH - font.width(title)) / 2
        val titleY = SIDEBAR_PADDING + 2

        if (isRainbow) {
            val elapsed = (now - rainbowStartTime).toFloat()
            var charX = titleX
            for ((i, char) in title.withIndex()) {
                val hue = ((elapsed / 500f + i * 0.1f) % 1f)
                val rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f)
                guiGraphics.text(font, char.toString(), charX, titleY, rgb)
                charX += font.width(char.toString())
            }
        } else {
            guiGraphics.text(font, title, titleX, titleY, Color.WHITE.rgb)
        }
    }

    private fun cycleSortOrder() {
        val next = Services.config.getSortOrder().next()
        Services.config.setSortOrder(next)
        init()
    }

    private fun openScreenshotScreen(fileIndex: Int) {
        val scrollOffset = galleryWidget?.scrollOffset ?: 0f
        Minecraft.getInstance().pushGuiLayer(ScreenshotScreen(fileIndex, scrollOffset, this))
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    override fun onFilterChange(filter: ScreenshotFilter) {
        Services.config.setScreenshotFilter(filter)
        init()
    }

    private fun toggleSelectionMode() {
        selectionMode = !selectionMode
        selectedIndices.clear()
        refreshWidgets()
    }

    private fun selectAll() {
        for (i in allScreenshots.indices) {
            selectedIndices.add(i)
        }
        refreshWidgets()
    }

    private fun bulkMoveToTrash() {
        val filesToTrash = selectedIndices.mapNotNull { allScreenshots.getOrNull(it) }
        Minecraft.getInstance().pushGuiLayer(
            DeleteConfirmScreen(filesToTrash, GalleryScreen())
        )
    }

    private fun openTrashScreen() {
        Minecraft.getInstance().pushGuiLayer(TrashScreen { init() })
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private const val SIDEBAR_WIDTH = 80
        private const val SIDEBAR_BUTTON_WIDTH = 60
        private const val SIDEBAR_PADDING = 10
        private const val SIDEBAR_GAP = 5
        private const val SIDEBAR_TITLE_HEIGHT = 20
        private const val RAINBOW_DURATION_MS = 2000L
        private const val SIDEBAR_BG_COLOR = 0x88000000.toInt()

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
