# Thumbnail Caching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cache downscaled PNG thumbnails on disk so the gallery loads ~50 KB files instead of ~5 MB full-resolution screenshots.

**Architecture:** New `ThumbnailCache` utility handles cache directory, invalidation (mtime comparison), and thumbnail generation (ImageIO scale + write). `ScreenshotLoader.loadBatch()` gains a `useThumbnails` parameter that routes through the cache. `loadSingle()` remains unchanged (full-res for the viewer).

**Tech Stack:** Kotlin, `java.awt.image.BufferedImage` for scaling, `javax.imageio.ImageIO` for read/write, NativeImage for GPU upload

---

## File Structure

| File | Responsibility |
|------|---------------|
| `utils/ThumbnailCache.kt` | **New** — cache dir management, thumbnail generation, mtime invalidation |
| `utils/ScreenshotLoader.kt` | Add `useThumbnails` flag to `loadBatch()`, route through cache |
| `gui/GalleryScreen.kt` | Pass `useThumbnails = true` to loader |

All paths relative to `src/main/kotlin/com/vandendaelen/nicephore/`.

---

### Task 1: Create ThumbnailCache utility

**Files:**
- Create: `src/main/kotlin/com/vandendaelen/nicephore/utils/ThumbnailCache.kt`

- [ ] **Step 1: Create ThumbnailCache.kt**

```kotlin
package com.vandendaelen.nicephore.utils

import com.vandendaelen.nicephore.Nicephore
import net.minecraft.client.Minecraft
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

object ThumbnailCache {
    private const val THUMBNAIL_MAX_WIDTH = 200

    private val cacheDir: File by lazy {
        File(Minecraft.getInstance().gameDirectory, ".nicephore${File.separator}thumbnails").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getThumbnail(original: File): File {
        val thumbFile = File(cacheDir, original.name)

        if (thumbFile.exists() && thumbFile.lastModified() >= original.lastModified()) {
            return thumbFile
        }

        return generateThumbnail(original, thumbFile)
    }

    private fun generateThumbnail(original: File, thumbFile: File): File {
        try {
            val fullImage = ImageIO.read(original)
                ?: throw IOException("ImageIO.read returned null for ${original.name}")

            val scaledWidth = THUMBNAIL_MAX_WIDTH.coerceAtMost(fullImage.width)
            val scaledHeight = (scaledWidth.toFloat() / fullImage.width * fullImage.height).toInt()

            val scaled = fullImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
            val buffered = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
            buffered.createGraphics().apply {
                drawImage(scaled, 0, 0, null)
                dispose()
            }

            cacheDir.mkdirs()
            ImageIO.write(buffered, "png", thumbFile)
            Nicephore.LOGGER.debug("Generated thumbnail for {}", original.name)
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to generate thumbnail for {}", original.name, e)
        }

        return thumbFile
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/ThumbnailCache.kt
git commit -m "feat: add ThumbnailCache for downscaled thumbnail generation and caching"
```

---

### Task 2: Add thumbnail support to ScreenshotLoader

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/ScreenshotLoader.kt`

- [ ] **Step 1: Add useThumbnails parameter to loadBatch**

In `ScreenshotLoader.kt`, change the `loadBatch` method signature from:

```kotlin
    fun loadBatch(files: List<File>, idPrefix: String) {
```

to:

```kotlin
    fun loadBatch(files: List<File>, idPrefix: String, useThumbnails: Boolean = false) {
```

Then inside the `scope.launch` block, change:

```kotlin
                val nativeImage = readImageFromDisk(file)
```

to:

```kotlin
                val fileToLoad = if (useThumbnails) ThumbnailCache.getThumbnail(file) else file
                val nativeImage = readImageFromDisk(fileToLoad)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL (existing callers still work because of default parameter `false`)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/ScreenshotLoader.kt
git commit -m "feat: add useThumbnails parameter to ScreenshotLoader.loadBatch"
```

---

### Task 3: Use thumbnails in GalleryScreen

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt`

- [ ] **Step 1: Pass useThumbnails=true in GalleryScreen.init()**

In `GalleryScreen.kt`, change line 59:

```kotlin
            loader.loadBatch(screenshots, "gallery")
```

to:

```kotlin
            loader.loadBatch(screenshots, "gallery", useThumbnails = true)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt
git commit -m "feat: use cached thumbnails for gallery image loading"
```

---

### Task 4: Final verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify no regressions**

Run: `grep -rn "ThumbnailCache" src/main/kotlin/` to confirm usage is wired up.

Expected output includes:
- `ThumbnailCache.kt` (definition)
- `ScreenshotLoader.kt` (calls `ThumbnailCache.getThumbnail`)
- No other files (ScreenshotScreen should NOT reference ThumbnailCache)

- [ ] **Step 3: Commit if any fixes needed**

```bash
git add -A
git commit -m "fix: address issues found during thumbnail cache verification"
```
