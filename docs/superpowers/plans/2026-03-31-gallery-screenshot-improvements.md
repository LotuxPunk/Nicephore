# Gallery & Screenshot Screen Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix performance, rendering correctness, and layout hardcoding in the gallery and screenshot screens.

**Architecture:** Add kotlinx-coroutines for async image loading off the render thread. Move widget creation from `extractRenderState()` (per-frame) to `init()` (once). Replace magic layout numbers with computed grid based on screen dimensions and a configurable column count.

**Tech Stack:** Kotlin, NeoForge 26.1, kotlinx-coroutines-core 1.10.2, Minecraft GUI API (Screen, GuiGraphicsExtractor)

---

## File Structure

| File | Responsibility |
|------|---------------|
| `build.gradle` | Add coroutines dependency |
| `config/NicephoreConfig.kt` | Add `galleryColumns` config property |
| `utils/Util.kt` | Fix `fileToTexture()` resource leak and null safety |
| `utils/ScreenshotLoader.kt` | **New** -- async image loading with coroutine scope |
| `gui/AbstractNicephoreScreen.kt` | Layout constants, `rebuildWidgets()` pattern |
| `gui/GalleryScreen.kt` | Async loading, dynamic grid, widget lifecycle fix |
| `gui/ScreenshotScreen.kt` | Async loading, computed sizing, widget lifecycle fix |
| `gui/SettingsScreen.kt` | Column count slider, widget lifecycle fix |

All paths relative to `src/main/kotlin/com/vandendaelen/nicephore/`.

---

### Task 1: Add kotlinx-coroutines dependency

**Files:**
- Modify: `build.gradle`

- [ ] **Step 1: Add coroutines to dependencies block**

In `build.gradle`, add coroutines implementation and jarJar inside the existing `dependencies` block, after the kotlin-stdlib jarJar entry:

```groovy
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'
    jarJar('org.jetbrains.kotlinx:kotlinx-coroutines-core:[1.10.2,1.11.0)') {
        version { prefer '1.10.2' }
    }
```

- [ ] **Step 2: Verify dependency resolves**

Run: `./gradlew dependencies --configuration compileClasspath 2>&1 | grep coroutines`

Expected: Line showing `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`

- [ ] **Step 3: Commit**

```bash
git add build.gradle
git commit -m "feat: add kotlinx-coroutines-core dependency for async image loading"
```

---

### Task 2: Fix Util.fileToTexture() resource leak and null safety

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt`

- [ ] **Step 1: Rewrite fileToTexture with proper resource handling**

Replace the `fileToTexture` function with:

```kotlin
fun fileToTexture(file: File): DynamicTexture? {
    return try {
        val nativeImage = FileInputStream(file).use { NativeImage.read(it) }
        DynamicTexture({ "nicephore_${file.nameWithoutExtension}" }, nativeImage)
    } catch (e: IOException) {
        Nicephore.LOGGER.error("Failed to load screenshot texture: ${file.name}", e)
        null
    }
}
```

Add missing import at the top of the file:

```kotlin
import com.vandendaelen.nicephore.Nicephore
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: Compilation errors in GalleryScreen and ScreenshotScreen because `fileToTexture` now returns nullable. This is expected -- callers will be updated in later tasks.

- [ ] **Step 3: Temporarily fix callers to compile**

In `GalleryScreen.kt` line 42, change:
```kotlin
screenshots.forEach { file -> screenshotTextures.add(Util.fileToTexture(file)) }
```
to:
```kotlin
screenshots.forEach { file -> Util.fileToTexture(file)?.let { screenshotTextures.add(it) } }
```

In `ScreenshotScreen.kt` line 52, change:
```kotlin
screenshotTexture = Util.fileToTexture(screenshots[index])
```
to:
```kotlin
screenshotTexture = Util.fileToTexture(screenshots[index])
```
(This already works since `screenshotTexture` is `DynamicTexture?` -- nullable.)

- [ ] **Step 4: Verify compilation passes**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt
git commit -m "fix: close FileInputStream in fileToTexture, return null on failure"
```

---

### Task 3: Add galleryColumns config property

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/config/NicephoreConfig.kt`

- [ ] **Step 1: Add galleryColumns field to Client class**

In `NicephoreConfig.kt`, add a new field declaration after `screenshotFilter`:

```kotlin
val galleryColumns: ModConfigSpec.IntValue
```

In the `init` block, inside the "GUI-specific settings" push/pop section (after the `screenshotFilter` definition, before `builder.pop()`), add:

```kotlin
galleryColumns = builder
    .comment(
        "Number of columns in the gallery grid.",
        "Set to 0 for automatic (based on screen width). Valid range: 0, 2-6."
    )
    .defineInRange("galleryColumns", 0, 0, 6)
```

- [ ] **Step 2: Add getter and setter in companion object**

Add to the `Client.Companion`:

```kotlin
fun getGalleryColumns(): Int = NicephoreConfig.CLIENT.galleryColumns.get()

fun setGalleryColumns(value: Int) {
    NicephoreConfig.CLIENT.galleryColumns.set(value)
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/config/NicephoreConfig.kt
git commit -m "feat: add galleryColumns config property (0=auto, 2-6=fixed)"
```

---

### Task 4: Create ScreenshotLoader async utility

**Files:**
- Create: `src/main/kotlin/com/vandendaelen/nicephore/utils/ScreenshotLoader.kt`

- [ ] **Step 1: Create the ScreenshotLoader class**

```kotlin
package com.vandendaelen.nicephore.utils

import com.mojang.blaze3d.platform.NativeImage
import com.vandendaelen.nicephore.Nicephore
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ScreenshotLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class LoadState { LOADING, LOADED, ERROR }

    data class LoadedTexture(
        val texture: DynamicTexture,
        val textureId: Identifier,
        val state: LoadState = LoadState.LOADED
    )

    data class SlotState(
        val state: LoadState,
        val loaded: LoadedTexture? = null
    )

    private val slots = mutableMapOf<Int, SlotState>()
    private var onLoadComplete: (() -> Unit)? = null

    fun setOnLoadComplete(callback: () -> Unit) {
        onLoadComplete = callback
    }

    fun getSlotState(index: Int): SlotState {
        return slots[index] ?: SlotState(LoadState.LOADING)
    }

    fun loadBatch(files: List<File>, idPrefix: String) {
        cancelAll()
        slots.clear()

        files.forEachIndexed { index, file ->
            slots[index] = SlotState(LoadState.LOADING)
            scope.launch {
                val result = loadFile(file, "${idPrefix}_$index")
                withContext(Dispatchers.Main.immediate) {
                    if (result != null) {
                        val tm = Minecraft.getInstance().textureManager
                        tm.register(result.textureId, result.texture)
                        slots[index] = SlotState(LoadState.LOADED, result)
                    } else {
                        slots[index] = SlotState(LoadState.ERROR)
                    }
                    onLoadComplete?.invoke()
                }
            }
        }
    }

    fun loadSingle(file: File, idPrefix: String) {
        cancelAll()
        slots.clear()
        slots[0] = SlotState(LoadState.LOADING)

        scope.launch {
            val result = loadFile(file, idPrefix)
            withContext(Dispatchers.Main.immediate) {
                if (result != null) {
                    val tm = Minecraft.getInstance().textureManager
                    tm.register(result.textureId, result.texture)
                    slots[0] = SlotState(LoadState.LOADED, result)
                } else {
                    slots[0] = SlotState(LoadState.ERROR)
                }
                onLoadComplete?.invoke()
            }
        }
    }

    private fun loadFile(file: File, id: String): LoadedTexture? {
        return try {
            val nativeImage = FileInputStream(file).use { NativeImage.read(it) }
            val texture = DynamicTexture({ "nicephore_$id" }, nativeImage)
            val textureId = Identifier.withDefaultNamespace("nicephore_$id")
            LoadedTexture(texture, textureId)
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to load screenshot: ${file.name}", e)
            null
        }
    }

    fun cancelAll() {
        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        releaseTextures()
    }

    fun releaseTextures() {
        val tm = Minecraft.getInstance().textureManager
        slots.values.forEach { slot ->
            slot.loaded?.let {
                it.texture.close()
            }
        }
        slots.clear()
    }

    fun destroy() {
        releaseTextures()
        scope.cancel()
    }
}
```

- [ ] **Step 2: Set up Dispatchers.Main for Minecraft**

The `Dispatchers.Main` needs to be backed by Minecraft's executor. Add the following to the `Nicephore` class init block in `Nicephore.kt`:

```kotlin
kotlinx.coroutines.Dispatchers.Main
```

Actually, `Dispatchers.Main` requires a Main dispatcher implementation on the classpath. For a Minecraft mod, we should use a custom dispatcher. Update `ScreenshotLoader` to use a Minecraft-aware approach instead:

Replace `withContext(Dispatchers.Main.immediate)` calls with:

```kotlin
Minecraft.getInstance().execute {
```

So the `loadBatch` launch block becomes:

```kotlin
scope.launch {
    val result = loadFile(file, "${idPrefix}_$index")
    Minecraft.getInstance().execute {
        if (result != null) {
            val tm = Minecraft.getInstance().textureManager
            tm.register(result.textureId, result.texture)
            slots[index] = SlotState(LoadState.LOADED, result)
        } else {
            slots[index] = SlotState(LoadState.ERROR)
        }
        onLoadComplete?.invoke()
    }
}
```

And similarly for `loadSingle`. Remove the `Dispatchers.Main` import. The full file should use `Minecraft.getInstance().execute { }` instead of `withContext(Dispatchers.Main.immediate)`.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/ScreenshotLoader.kt
git commit -m "feat: add ScreenshotLoader for async image loading with coroutines"
```

---

### Task 5: Update AbstractNicephoreScreen with layout constants

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/AbstractNicephoreScreen.kt`

- [ ] **Step 1: Add layout constants and rebuildWidgets pattern**

Replace the full `AbstractNicephoreScreen.kt` with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

abstract class AbstractNicephoreScreen(title: Component) : Screen(title) {

    override fun extractBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        this.extractTransparentBackground(guiGraphics)
    }

    protected fun rebuildWidgets() {
        this.clearWidgets()
        buildWidgets()
    }

    protected open fun buildWidgets() {
        // Subclasses override to add their widgets
    }

    protected fun closeScreen(textComponentId: String) {
        this.onClose()
        PlayerHelper.sendHotbarMessage(Component.translatable(textComponentId))
    }

    protected fun openSettingsScreen() {
        Minecraft.getInstance().pushGuiLayer(SettingsScreen())
    }

    protected fun readAspectRatio(file: File): Float {
        try {
            ImageIO.createImageInputStream(file).use { imageIn ->
                val readers = ImageIO.getImageReaders(imageIn)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    try {
                        reader.setInput(imageIn)
                        return reader.getWidth(0) / reader.getHeight(0).toFloat()
                    } finally {
                        reader.dispose()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return DEFAULT_ASPECT_RATIO
    }

    protected fun wrapIndex(currentIndex: Int, delta: Int, max: Int): Int {
        val newIndex = currentIndex + delta
        return when {
            newIndex in 0 until max -> newIndex
            newIndex < 0 -> max - 1
            else -> 0
        }
    }

    protected fun clampIndex(currentIndex: Int, max: Int): Int {
        return if (currentIndex >= max || currentIndex < 0) max - 1 else currentIndex
    }

    protected fun cycleFilter(listener: FilterListener? = null) {
        val nextFilter = NicephoreConfig.Client.getScreenshotFilter().next()
        NicephoreConfig.Client.setScreenshotFilter(nextFilter)
        init()
        listener?.onFilterChange(nextFilter)
    }

    protected fun addToolbarButtons(onFilterChange: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.filter", NicephoreConfig.Client.getScreenshotFilter().name)) { onFilterChange() }
                .bounds(PADDING, PADDING, 100, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.settings")) { openSettingsScreen() }
                .bounds(this.width - PADDING - 110, PADDING, 50, BUTTON_HEIGHT).build()
        )
    }

    protected fun addNavigationButtons(centerX: Int, bottomLine: Int, onPrev: () -> Unit, onNext: () -> Unit) {
        this.addRenderableWidget(
            Button.builder(Component.literal("<")) { onPrev() }
                .bounds(centerX - 80, bottomLine, 20, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(Component.literal(">")) { onNext() }
                .bounds(centerX + 60, bottomLine, 20, BUTTON_HEIGHT).build()
        )
    }

    companion object {
        val SCREENSHOTS_DIR: File = File(Minecraft.getInstance().gameDirectory, "screenshots")

        const val DEFAULT_ASPECT_RATIO = 16f / 9f
        const val PADDING = 10
        const val TOOLBAR_HEIGHT = 30
        const val BUTTON_HEIGHT = 20
        const val BOTTOM_BAR_HEIGHT = 30
        const val TARGET_THUMBNAIL_WIDTH = 150
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL (existing screens still compile since `buildWidgets` has a default no-op body)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/AbstractNicephoreScreen.kt
git commit -m "feat: add layout constants and rebuildWidgets pattern to AbstractNicephoreScreen"
```

---

### Task 6: Rewrite GalleryScreen with async loading and dynamic grid

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt`

- [ ] **Step 1: Rewrite GalleryScreen**

Replace the full `GalleryScreen.kt` with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import com.vandendaelen.nicephore.utils.Util
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
    private var screenshots: List<File> = emptyList()
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

    private fun getNumberOfPages(): Long {
        return kotlin.math.ceil(Util.getNumberOfFiles(SCREENSHOTS_DIR) / imagesToDisplay.toDouble()).toLong()
    }

    override fun init() {
        super.init()

        computeGrid()
        screenshots = Util.getBatchOfFiles((imagesToDisplay.toLong() * index), imagesToDisplay.toLong(), SCREENSHOTS_DIR)
        index = clampIndex(index, getNumberOfPages().toInt().coerceAtLeast(1))
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[0]) else DEFAULT_ASPECT_RATIO

        if (screenshots.isNotEmpty()) {
            loader.setOnLoadComplete { rebuildWidgets() }
            loader.loadBatch(screenshots, "gallery")
        }

        rebuildWidgets()
    }

    override fun buildWidgets() {
        addToolbarButtons { cycleFilter() }

        if (screenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT
            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            screenshots.forEachIndexed { imageIndex, file ->
                val col = imageIndex % columns
                val row = imageIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * (imageHeight + BUTTON_HEIGHT + PADDING)

                val name = file.name
                val text = Component.literal(StringUtils.abbreviate(name, imageWidth / 6))

                this.addRenderableWidget(
                    Button.builder(text) { openScreenshotScreen(imageIndex) }
                        .bounds(x, y + imageHeight + 2, imageWidth, BUTTON_HEIGHT).build()
                )
            }
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val bottomLine = this.height - BOTTOM_BAR_HEIGHT

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + 20, Color.RED.rgb
            )
        } else {
            val availableWidth = this.width - 2 * PADDING
            val imageWidth = (availableWidth - (columns - 1) * PADDING) / columns
            val imageHeight = (imageWidth / aspectRatio).toInt()

            screenshots.forEachIndexed { imageIndex, file ->
                val col = imageIndex % columns
                val row = imageIndex / columns
                val x = PADDING + col * (imageWidth + PADDING)
                val y = TOOLBAR_HEIGHT + row * (imageHeight + BUTTON_HEIGHT + PADDING)

                val slot = loader.getSlotState(imageIndex)
                when (slot.state) {
                    ScreenshotLoader.LoadState.LOADED -> {
                        slot.loaded?.let {
                            guiGraphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                it.textureId,
                                x, y, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight
                            )
                        }
                        drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x, y + PADDING)
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
            guiGraphics.text(Minecraft.getInstance().font, extension.uppercase(), x + 2, y - 12, Color.WHITE.rgb)
        }
    }

    private fun modIndex(value: Int) {
        index = wrapIndex(index, value, getNumberOfPages().toInt())
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

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists()
        }
    }
}
```

- [ ] **Step 2: Add loading/error translation keys**

In `src/main/resources/assets/nicephore/lang/en_us.json`, add:

```json
"nicephore.screenshots.loading": "Loading...",
"nicephore.screenshots.error": "Error"
```

If the lang file doesn't exist or has different structure, find and update the correct file.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt src/main/resources/
git commit -m "feat: rewrite GalleryScreen with async loading and dynamic grid"
```

---

### Task 7: Rewrite ScreenshotScreen with async loading

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/ScreenshotScreen.kt`

- [ ] **Step 1: Rewrite ScreenshotScreen**

Replace the full `ScreenshotScreen.kt` with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard
import com.vandendaelen.nicephore.utils.FilterListener
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.ScreenshotLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import org.apache.commons.io.FileUtils
import java.awt.Color
import java.io.File
import java.text.DecimalFormat
import java.text.MessageFormat
import java.text.NumberFormat
import java.util.Comparator

class ScreenshotScreen @JvmOverloads constructor(
    private var index: Int = 0,
    private val galleryScreenPage: Int = -1,
    private val listener: FilterListener? = null
) : AbstractNicephoreScreen(TITLE) {

    private var screenshots: ArrayList<File> = ArrayList()
    private var aspectRatio: Float = DEFAULT_ASPECT_RATIO
    private val loader = ScreenshotLoader()

    override fun init() {
        super.init()

        val filter = NicephoreConfig.Client.getScreenshotFilter().predicate
        screenshots = ArrayList(
            SCREENSHOTS_DIR.listFiles(filter)
                ?.sortedWith(Comparator.comparingLong(File::lastModified).reversed())
                ?: emptyList()
        )

        index = clampIndex(index, screenshots.size)
        aspectRatio = if (screenshots.isNotEmpty()) readAspectRatio(screenshots[index]) else DEFAULT_ASPECT_RATIO

        if (screenshots.isNotEmpty()) {
            loader.setOnLoadComplete { /* texture ready, next frame picks it up */ }
            loader.loadSingle(screenshots[index], "screenshot")
        }

        rebuildWidgets()
    }

    override fun buildWidgets() {
        addToolbarButtons { cycleFilter(listener) }

        if (screenshots.isNotEmpty()) {
            val centerX = this.width / 2
            val bottomLine = this.height - BOTTOM_BAR_HEIGHT

            addNavigationButtons(centerX, bottomLine, { modIndex(-1) }, { modIndex(1) })

            val copyButton = Button.builder(Component.translatable("nicephore.gui.screenshots.copy")) {
                val screenshot = screenshots[index]
                if (CopyImageToClipBoard.copyImage(screenshot)) {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.success"))
                } else {
                    PlayerHelper.sendMessage(Component.translatable("nicephore.clipboard.error"))
                }
            }.bounds(centerX - 52, bottomLine, 50, BUTTON_HEIGHT).build()

            copyButton.active = OperatingSystems.getOS().manager != null
            this.addRenderableWidget(copyButton)

            this.addRenderableWidget(
                Button.builder(Component.translatable("nicephore.gui.screenshots.delete")) { deleteScreenshot(screenshots[index]) }
                    .bounds(centerX + 3, bottomLine, 50, BUTTON_HEIGHT).build()
            )
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2
        val maxImageWidth = this.width - 2 * SIDE_PADDING
        val pictureMidWidth = maxImageWidth.coerceAtMost(MAX_IMAGE_WIDTH)
        val pictureHeight = (pictureMidWidth / aspectRatio).toInt()

        if (screenshots.isEmpty()) {
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.screenshots.empty"),
                centerX, TOOLBAR_HEIGHT + PADDING, Color.RED.rgb
            )
        } else {
            val slot = loader.getSlotState(0)
            when (slot.state) {
                ScreenshotLoader.LoadState.LOADED -> {
                    slot.loaded?.let {
                        guiGraphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            it.textureId,
                            centerX - pictureMidWidth / 2, IMAGE_TOP, 0f, 0f,
                            pictureMidWidth, pictureHeight, pictureMidWidth, pictureHeight
                        )
                    }
                }
                ScreenshotLoader.LoadState.LOADING -> {
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.translatable("nicephore.screenshots.loading"),
                        centerX, IMAGE_TOP + pictureHeight / 2, Color.GRAY.rgb
                    )
                }
                ScreenshotLoader.LoadState.ERROR -> {
                    guiGraphics.centeredText(
                        Minecraft.getInstance().font,
                        Component.translatable("nicephore.screenshots.error"),
                        centerX, IMAGE_TOP + pictureHeight / 2, Color.RED.rgb
                    )
                }
            }

            val currentScreenshot = screenshots[index]
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("nicephore.gui.screenshots.pages", index + 1, screenshots.size),
                centerX, TOOLBAR_HEIGHT, Color.WHITE.rgb
            )
            guiGraphics.centeredText(
                Minecraft.getInstance().font,
                Component.literal(MessageFormat.format("{0} ({1})", currentScreenshot.name, getFileSizeMegaBytes(currentScreenshot))),
                centerX, TOOLBAR_HEIGHT + 12, Color.WHITE.rgb
            )

            // Tooltip for disabled copy button
            val copyButtonX = centerX - 52
            val copyButtonY = this.height - BOTTOM_BAR_HEIGHT
            if (OperatingSystems.getOS().manager == null &&
                mouseX >= copyButtonX && mouseY >= copyButtonY &&
                mouseX < copyButtonX + 50 && mouseY < copyButtonY + BUTTON_HEIGHT
            ) {
                guiGraphics.setComponentTooltipForNextFrame(
                    Minecraft.getInstance().font,
                    listOf(Component.translatable("nicephore.gui.screenshots.copy.unable").withStyle(ChatFormatting.RED)),
                    mouseX, mouseY
                )
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun modIndex(value: Int) {
        index = wrapIndex(index, value, screenshots.size)
        init()
    }

    private fun deleteScreenshot(file: File) {
        Minecraft.getInstance().pushGuiLayer(
            DeleteConfirmScreen(
                file,
                if (galleryScreenPage > -1) GalleryScreen(galleryScreenPage) else ScreenshotScreen(index)
            )
        )
    }

    override fun onClose() {
        loader.destroy()
        super.onClose()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.screenshots")
        private const val SIDE_PADDING = 40
        private const val MAX_IMAGE_WIDTH = 800
        private const val IMAGE_TOP = 50

        fun canBeShow(): Boolean {
            return SCREENSHOTS_DIR.exists() && (SCREENSHOTS_DIR.list()?.isNotEmpty() == true)
        }

        private fun getFileSizeMegaBytes(file: File): String {
            val size = FileUtils.sizeOf(file).toDouble()
            val formatter: NumberFormat = DecimalFormat("#0.00")
            val mbSize = 1024 * 1024
            val kbSize = 1024

            return if (size > mbSize) {
                MessageFormat.format("{0} MB", formatter.format(size / mbSize))
            } else {
                MessageFormat.format("{0} KB", formatter.format(size / kbSize))
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/ScreenshotScreen.kt
git commit -m "feat: rewrite ScreenshotScreen with async loading and computed layout"
```

---

### Task 8: Update SettingsScreen with column slider and fix widget lifecycle

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/SettingsScreen.kt`

- [ ] **Step 1: Rewrite SettingsScreen with buildWidgets and column slider**

Replace the full `SettingsScreen.kt` with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.config.NicephoreConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import java.awt.Color

class SettingsScreen : AbstractNicephoreScreen(TITLE) {

    override fun init() {
        super.init()
        rebuildWidgets()
    }

    override fun buildWidgets() {
        val startingLine = this.width / 2 - 150

        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.screenshot.exit")) { onClose() }
                .bounds(this.width - PADDING - 50, PADDING, 50, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.showOptimisationStatus", if (NicephoreConfig.Client.getShouldShowOptStatus()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setShouldShowOptStatus(!NicephoreConfig.Client.getShouldShowOptStatus()) } }
                .bounds(startingLine, 60, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.makeJPEGs", if (NicephoreConfig.Client.getJPEGToggle()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setJPEGToggle(!NicephoreConfig.Client.getJPEGToggle()) } }
                .bounds(startingLine, 90, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.screenshotCustomMessage", if (NicephoreConfig.Client.getScreenshotCustomMessage()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setScreenshotCustomMessage(!NicephoreConfig.Client.getScreenshotCustomMessage()) } }
                .bounds(startingLine, 120, 300, BUTTON_HEIGHT).build()
        )
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.screenshot.setScreenshotToClipboard", if (NicephoreConfig.Client.getScreenshotToClipboard()) "ON" else "OFF")
            ) { toggleSetting { NicephoreConfig.Client.setScreenshotToClipboard(!NicephoreConfig.Client.getScreenshotToClipboard()) } }
                .bounds(startingLine, 150, 300, BUTTON_HEIGHT).build()
        )

        // Gallery columns slider
        val currentColumns = NicephoreConfig.Client.getGalleryColumns()
        val label = if (currentColumns == 0) "Auto" else "$currentColumns"
        this.addRenderableWidget(
            Button.builder(
                Component.translatable("nicephore.settings.galleryColumns", label)
            ) { cycleGalleryColumns() }
                .bounds(startingLine, 180, 300, BUTTON_HEIGHT).build()
        )
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val centerX = this.width / 2

        guiGraphics.centeredText(
            Minecraft.getInstance().font,
            Component.translatable("nicephore.gui.settings"),
            centerX, 35, Color.WHITE.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun toggleSetting(action: () -> Unit) {
        action()
        rebuildWidgets()
    }

    private fun cycleGalleryColumns() {
        val current = NicephoreConfig.Client.getGalleryColumns()
        // Cycle: 0 (auto) -> 2 -> 3 -> 4 -> 5 -> 6 -> 0 (auto)
        val next = when (current) {
            0 -> 2
            in 2..5 -> current + 1
            else -> 0
        }
        NicephoreConfig.Client.setGalleryColumns(next)
        rebuildWidgets()
    }

    companion object {
        private val TITLE = Component.translatable("nicephore.gui.settings")
    }
}
```

- [ ] **Step 2: Add translation key for gallery columns**

In the lang file, add:

```json
"nicephore.settings.galleryColumns": "Gallery Columns: %s"
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/SettingsScreen.kt src/main/resources/
git commit -m "feat: add gallery columns config slider to SettingsScreen, fix widget lifecycle"
```

---

### Task 9: Update DeleteConfirmScreen to use buildWidgets

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/DeleteConfirmScreen.kt`

- [ ] **Step 1: Migrate to buildWidgets pattern**

Replace the full `DeleteConfirmScreen.kt` with:

```kotlin
package com.vandendaelen.nicephore.client.gui

import com.vandendaelen.nicephore.utils.PlayerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.awt.Color
import java.io.File

class DeleteConfirmScreen(
    private val file: File,
    private val instanceToOpenIfDeleted: Screen?
) : AbstractNicephoreScreen(Component.translatable("nicephore.gui.delete")) {

    override fun init() {
        super.init()
        rebuildWidgets()
    }

    override fun buildWidgets() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("nicephore.gui.delete.yes")) {
                deleteScreenshot()
                if (instanceToOpenIfDeleted != null) {
                    Minecraft.getInstance().setScreen(instanceToOpenIfDeleted)
                } else {
                    onClose()
                }
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
            Component.translatable("nicephore.gui.delete.question", file.name),
            this.width / 2, this.height / 2 - 20, Color.RED.rgb
        )

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun deleteScreenshot() {
        if (file.exists() && file.delete()) {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.success", file.name))
        } else {
            PlayerHelper.sendMessage(Component.translatable("nicephore.screenshot.deleted.error", file.name))
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/DeleteConfirmScreen.kt
git commit -m "refactor: migrate DeleteConfirmScreen to buildWidgets pattern"
```

---

### Task 10: Final verification

- [ ] **Step 1: Full clean build**

Run: `./gradlew clean compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run the game and test**

Run the game client and verify:
- Open gallery (Ctrl+G) -- grid renders with loading placeholders, then images appear
- Navigate pages -- loads cancel and restart correctly
- Adjust column slider in Settings -- gallery adapts
- Open screenshot viewer from gallery -- single image loads async
- Delete a screenshot -- confirmation dialog works
- Close screens -- no crashes

- [ ] **Step 3: Final commit**

If any fixes were needed during testing, commit them:

```bash
git add -A
git commit -m "fix: address issues found during manual testing"
```
