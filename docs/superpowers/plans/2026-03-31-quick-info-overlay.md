# Quick Info Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show date/time and file size on hover over gallery thumbnails, using kotlinx-datetime and shared utility functions.

**Architecture:** Add kotlinx-datetime dependency. Move file size formatting to `Util`, add date formatting. Render a semi-transparent overlay in `GalleryScreen.extractRenderState()` when mouse is over a thumbnail.

**Tech Stack:** Kotlin, kotlinx-datetime 0.7.1, Minecraft GuiGraphicsExtractor

---

## File Structure

| File | Responsibility |
|------|---------------|
| `build.gradle` | Add kotlinx-datetime dependency |
| `utils/Util.kt` | Add `formatFileSize()` and `formatFileDate()` utility functions |
| `gui/ScreenshotScreen.kt` | Replace private `getFileSizeMegaBytes` with `Util.formatFileSize()` |
| `gui/GalleryScreen.kt` | Add hover overlay rendering in `extractRenderState()` |

All Kotlin paths relative to `src/main/kotlin/com/vandendaelen/nicephore/`.

---

### Task 1: Add kotlinx-datetime dependency

**Files:**
- Modify: `build.gradle`

- [ ] **Step 1: Add kotlinx-datetime to dependencies block**

In `build.gradle`, after the kotlinx-coroutines-core jarJar entry, add:

```groovy
    implementation 'org.jetbrains.kotlinx:kotlinx-datetime:0.7.1'
    jarJar('org.jetbrains.kotlinx:kotlinx-datetime:[0.7.1,0.8.0)') {
        version { prefer '0.6.2' }
    }
```

- [ ] **Step 2: Verify dependency resolves**

Run: `./gradlew dependencies --configuration compileClasspath 2>&1 | grep datetime`

Expected: Line showing `org.jetbrains.kotlinx:kotlinx-datetime:0.7.1`

- [ ] **Step 3: Commit**

```bash
git add build.gradle
git commit -m "feat: add kotlinx-datetime dependency"
```

---

### Task 2: Add shared utility functions to Util.kt

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt`

- [ ] **Step 1: Add formatFileSize and formatFileDate functions**

Add these two functions to the `Util` object, and add the required imports at the top of the file:

New imports to add:

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.DecimalFormat
```

New functions to add at the end of the `Util` object:

```kotlin
    fun formatFileSize(file: File): String {
        val size = file.length().toDouble()
        val formatter = DecimalFormat("#0.00")
        val mbSize = 1024 * 1024
        val kbSize = 1024

        return if (size > mbSize) {
            "${formatter.format(size / mbSize)} MB"
        } else {
            "${formatter.format(size / kbSize)} KB"
        }
    }

    fun formatFileDate(file: File): String {
        val instant = Instant.fromEpochMilliseconds(file.lastModified())
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "$month ${local.dayOfMonth}, ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt
git commit -m "feat: add formatFileSize and formatFileDate utilities using kotlinx-datetime"
```

---

### Task 3: Replace ScreenshotScreen.getFileSizeMegaBytes with Util.formatFileSize

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/ScreenshotScreen.kt`

- [ ] **Step 1: Replace the call site**

In `ScreenshotScreen.kt`, find the line (around line 137):

```kotlin
                Component.literal(MessageFormat.format("{0} ({1})", currentScreenshot.name, getFileSizeMegaBytes(currentScreenshot))),
```

Replace with:

```kotlin
                Component.literal("${currentScreenshot.name} (${Util.formatFileSize(currentScreenshot)})"),
```

Add import at top:

```kotlin
import com.vandendaelen.nicephore.utils.Util
```

- [ ] **Step 2: Remove the private getFileSizeMegaBytes function**

Delete the entire `getFileSizeMegaBytes` function from the `companion object` (lines 187-197):

```kotlin
        private fun getFileSizeMegaBytes(file: File): String {
            val size = FileUtils.sizeOf(file).toDouble()
            val formatter: NumberFormat = DecimalFormat("#0.00")
            val mbSize = 1024 * 1024
            val kbSize = 1024

            return if (size > mbSize) {
                MessageFormat.format("{0} MB", formatter.format(FileUtils.sizeOf(file).toDouble() / mbSize))
            } else {
                MessageFormat.format("{0} KB", formatter.format(FileUtils.sizeOf(file).toDouble() / kbSize))
            }
        }
```

- [ ] **Step 3: Remove unused imports**

Remove these imports that are no longer needed:

```kotlin
import org.apache.commons.io.FileUtils
import java.text.DecimalFormat
import java.text.MessageFormat
import java.text.NumberFormat
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/ScreenshotScreen.kt
git commit -m "refactor: replace ScreenshotScreen.getFileSizeMegaBytes with Util.formatFileSize"
```

---

### Task 4: Add hover info overlay to GalleryScreen

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt`

- [ ] **Step 1: Add the overlay rendering in extractRenderState**

In `GalleryScreen.kt`, inside the `extractRenderState` method, find the block inside the `ScreenshotLoader.LoadState.LOADED` branch (after the `drawExtensionBadge` call). Add the hover overlay rendering after the extension badge:

Replace:

```kotlin
                        drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x + 2, y + imageHeight - 12)
```

with:

```kotlin
                        drawExtensionBadge(guiGraphics, FilenameUtils.getExtension(file.name), x + 2, y + imageHeight - 12)

                        // Hover info overlay
                        if (mouseX >= x && mouseX < x + imageWidth && mouseY >= y && mouseY < y + imageHeight) {
                            val overlayY = y + imageHeight - OVERLAY_HEIGHT
                            guiGraphics.fill(x, overlayY, x + imageWidth, y + imageHeight, OVERLAY_COLOR)
                            val font = Minecraft.getInstance().font
                            val dateText = Util.formatFileDate(file)
                            val sizeText = Util.formatFileSize(file)
                            guiGraphics.text(font, dateText, x + 2, overlayY + 2, Color.WHITE.rgb)
                            guiGraphics.text(font, sizeText, x + imageWidth - font.width(sizeText) - 2, overlayY + 2, Color.WHITE.rgb)
                        }
```

- [ ] **Step 2: Add the overlay constants to the companion object**

Add to the `GalleryScreen` companion object:

```kotlin
        private const val OVERLAY_HEIGHT = 12
        private const val OVERLAY_COLOR = 0xAA000000.toInt()
```

- [ ] **Step 3: Add Util import**

Add import at the top of GalleryScreen.kt:

```kotlin
import com.vandendaelen.nicephore.utils.Util
```

(It may already be imported — check first.)

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/client/gui/GalleryScreen.kt
git commit -m "feat: add hover info overlay with date and file size on gallery thumbnails"
```

---

### Task 5: Final verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify Util is used by both screens**

Run: `grep -rn "Util.formatFile" src/main/kotlin/`

Expected output includes references in both GalleryScreen.kt and ScreenshotScreen.kt.
