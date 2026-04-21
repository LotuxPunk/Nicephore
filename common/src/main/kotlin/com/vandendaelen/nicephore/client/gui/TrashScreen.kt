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

class TrashScreen(private val onTrashClosed: () -> Unit = {}) : AbstractNicephoreScreen(TITLE) {
    private var trashedFiles: List<File> = emptyList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    private lateinit var grid: GridLayout
    private var pageIndex: Int = 0

    private fun getNumberOfPages(): Int {
        return kotlin.math.ceil(trashedFiles.size / grid.imagesToDisplay.toDouble()).toInt().coerceAtLeast(1)
    }

    override fun init() {
        super.init()

        grid = computeGrid(this.width, this.height - PAGE_TEXT_HEIGHT, aspectRatio)
        trashedFiles = TrashManager.listTrash()
        pageIndex = clampIndex(pageIndex, getNumberOfPages())

        val pageFiles = trashedFiles.drop(grid.imagesToDisplay * pageIndex).take(grid.imagesToDisplay)
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

        val pageFiles = trashedFiles.drop(grid.imagesToDisplay * pageIndex).take(grid.imagesToDisplay)

        if (pageFiles.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT

            if (getNumberOfPages() > 1) {
                addNavigationButtons(centerX, bottomLine, { modPage(-1) }, { modPage(1) })
            }

            pageFiles.forEachIndexed { slotIndex, file ->
                val x = grid.slotX(slotIndex)
                val y = grid.slotY(slotIndex)

                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.restore")) { restoreFile(file) }
                        .bounds(x, y + grid.imageHeight + 2, grid.imageWidth / 2 - 1, BUTTON_HEIGHT).build()
                )
                this.addRenderableWidget(
                    Button.builder(Component.translatable("nicephore.gui.trash.delete")) { deletePermanently(file) }
                        .bounds(x + grid.imageWidth / 2 + 1, y + grid.imageHeight + 2, grid.imageWidth / 2 - 1, BUTTON_HEIGHT).build()
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

        val pageFiles = trashedFiles.drop(grid.imagesToDisplay * pageIndex).take(grid.imagesToDisplay)

        if (pageFiles.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + 20, Color.GRAY.rgb
            )
        } else {
            pageFiles.forEachIndexed { slotIndex, file ->
                val x = grid.slotX(slotIndex)
                val y = grid.slotY(slotIndex)

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

                        if (mouseX >= x && mouseX < x + grid.imageWidth && mouseY >= y && mouseY < y + grid.imageHeight) {
                            val overlayY = y + grid.imageHeight - 12
                            guiGraphics.fill(x, overlayY, x + grid.imageWidth, y + grid.imageHeight, 0xAA000000.toInt())
                            val font = Minecraft.getInstance().font
                            guiGraphics.text(font, Util.formatFileDate(file), x + 2, overlayY + 2, Color.WHITE.rgb)
                            val sizeText = Util.formatFileSize(file)
                            guiGraphics.text(font, sizeText, x + grid.imageWidth - font.width(sizeText) - 2, overlayY + 2, Color.WHITE.rgb)
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
            }

            if (getNumberOfPages() > 1) {
                val bottomLine = this.height - BOTTOM_BAR_HEIGHT
                guiGraphics.centeredText(
                    Minecraft.getInstance().font,
                    Component.literal("${pageIndex + 1} / ${getNumberOfPages()}"),
                    centerX, bottomLine - 12, Color.WHITE.rgb
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
        com.vandendaelen.nicephore.platform.ScreenStack.current.push(
            EmptyTrashConfirmScreen(this)
        )
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
        onTrashClosed()
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
