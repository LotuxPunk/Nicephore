package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.TrashManager
import com.vandendaelen.nicephore.utils.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class TrashScreen : AbstractNicephoreScreen(TITLE) {
    private var trashedFiles: List<File> = emptyList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    private var columns: Int = 4
    private var rows: Int = 3
    private val imagesToDisplay: Int get() = rows * columns
    private var pageIndex: Int = 0

    private fun computeGrid() {
        val availableWidth = this.width - 2 * PADDING
        val availableHeight = this.height - TOOLBAR_HEIGHT - BOTTOM_BAR_HEIGHT - PADDING

        columns = (availableWidth / (TARGET_THUMBNAIL_WIDTH + PADDING)).coerceIn(2, 6)

        val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
        val imageHeight = (imageWidth / aspectRatio).toInt()
        val slotHeight = imageHeight + BUTTON_HEIGHT + PADDING

        rows = ((availableHeight) / slotHeight).coerceIn(1, 4)
    }

    private fun getNumberOfPages(): Int {
        return kotlin.math.ceil(trashedFiles.size / imagesToDisplay.toDouble()).toInt().coerceAtLeast(1)
    }

    override fun init() {
        super.init()

        computeGrid()
        trashedFiles = TrashManager.listTrash()
        pageIndex = clampIndex(pageIndex, getNumberOfPages())

        val pageFiles = trashedFiles.drop(imagesToDisplay * pageIndex).take(imagesToDisplay)
        aspectRatio = if (pageFiles.isNotEmpty()) readAspectRatio(pageFiles[0]) else DEFAULT_ASPECT_RATIO

        if (pageFiles.isNotEmpty()) {
            loader.setOnLoadComplete { refreshWidgets() }
            loader.loadBatch(pageFiles, "trash", useThumbnails = true)
        }

        refreshWidgets()
    }

    override fun buildWidgets() {
        // Exit button
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )

        // Empty trash button
        if (trashedFiles.isNotEmpty()) {
            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.trash.empty")) { confirmEmptyTrash() }
                    .bounds(PADDING, PADDING, 80, BUTTON_HEIGHT).build()
            )
        }

        val pageFiles = trashedFiles.drop(imagesToDisplay * pageIndex).take(imagesToDisplay)

        if (pageFiles.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT

            if (getNumberOfPages() > 1) {
                this.addRenderableWidget(
                    Button.builder(Component.literal("<")) { modPage(-1) }
                        .bounds(centerX - 80, bottomLine, 20, BUTTON_HEIGHT).build()
                )
                this.addRenderableWidget(
                    Button.builder(Component.literal(">")) { modPage(1) }
                        .bounds(centerX + 60, bottomLine, 20, BUTTON_HEIGHT).build()
                )
            }

            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            pageFiles.forEachIndexed { slotIndex, file ->
                val col = slotIndex % columns
                val row = slotIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * (imageHeight + BUTTON_HEIGHT * 2 + PADDING + 4)

                // Restore button
                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.restore")) { restoreFile(file) }
                        .bounds(x, y + imageHeight + 2, imageWidth / 2 - 1, BUTTON_HEIGHT).build()
                )

                // Delete permanently button
                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.delete")) { deletePermanently(file) }
                        .bounds(x + imageWidth / 2 + 1, y + imageHeight + 2, imageWidth / 2 - 1, BUTTON_HEIGHT).build()
                )
            }
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2

        // Title
        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.trash.count", trashedFiles.size),
            centerX, TOOLBAR_HEIGHT - 10, Color.WHITE.rgb
        )

        val pageFiles = trashedFiles.drop(imagesToDisplay * pageIndex).take(imagesToDisplay)

        if (pageFiles.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + 20, Color.GRAY.rgb
            )
        } else {
            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            pageFiles.forEachIndexed { slotIndex, file ->
                val col = slotIndex % columns
                val row = slotIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * (imageHeight + BUTTON_HEIGHT * 2 + PADDING + 4)

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

                        if (mouseX >= x && mouseX < x + imageWidth && mouseY >= y && mouseY < y + imageHeight) {
                            val overlayY = y + imageHeight - 12
                            guiGraphics.fill(x, overlayY, x + imageWidth, y + imageHeight, 0xAA000000.toInt())
                            val font = Minecraft.getInstance().font
                            guiGraphics.text(font, Util.formatFileDate(file), x + 2, overlayY + 2, Color.WHITE.rgb)
                            val sizeText = Util.formatFileSize(file)
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
            }

            if (getNumberOfPages() > 1) {
                val bottomLine = this.height - BOTTOM_BAR_HEIGHT
                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.literal("${pageIndex + 1} / ${getNumberOfPages()}"),
                    centerX, bottomLine + 5, Color.WHITE.rgb
                )
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun modPage(delta: Int) {
        pageIndex = wrapIndex(pageIndex, delta, getNumberOfPages())
        init()
    }

    private fun restoreFile(file: File) {
        if (TrashManager.restore(file)) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.trash.restore.success", file.name))
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.gui.trash.restore.error"))
        }
        init()
    }

    private fun deletePermanently(file: File) {
        TrashManager.deletePermanently(file)
        init()
    }

    private fun confirmEmptyTrash() {
        Minecraft.getInstance().pushGuiLayer(
            EmptyTrashConfirmScreen(this)
        )
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.trash.title")
    }
}

class EmptyTrashConfirmScreen(
    private val trashScreen: TrashScreen
) : AbstractNicephoreScreen(Component.translatable("nicephore.gui.trash.empty")) {

    override fun init() {
        super.init()
        refreshWidgets()
    }

    override fun buildWidgets() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.yes")) {
                val count = TrashManager.emptyTrash()
                PlayerHelper.sendMessage(Component.translatable("nicephore.gui.trash.empty.success", count))
                Minecraft.getInstance().setScreen(trashScreen)
            }.bounds(this.width / 2 - 35, this.height / 2 + 30, 30, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.no")) {
                onClose()
            }.bounds(this.width / 2 + 5, this.height / 2 + 30, 30, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.trash.empty.confirm"),
            this.width / 2, this.height / 2 - 20, Color.RED.rgb
        )
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }
}
