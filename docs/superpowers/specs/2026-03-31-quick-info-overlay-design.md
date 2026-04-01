# Quick Info Overlay on Gallery Thumbnails

## Problem

Gallery thumbnails only show a truncated filename on the button below each image. Users can't see when a screenshot was taken or how large it is without opening the detail view.

## Solution

### Info overlay on hover

When the mouse hovers over a gallery thumbnail, show a semi-transparent overlay at the bottom of the thumbnail with:
- **Date/time**: extracted from the filename (Minecraft format: `YYYY-MM-DD_HH.MM.SS`) or from `lastModified`
- **File size**: formatted as KB or MB (reuse the existing `getFileSizeMegaBytes` logic from ScreenshotScreen)

### Rendering

- The overlay is a dark semi-transparent rectangle at the bottom of the thumbnail (height ~20px)
- Text is rendered on top in white, small font
- Left-aligned: date/time, right-aligned: file size
- Only shown when `mouseX/mouseY` is within the thumbnail bounds
- Rendered in `extractRenderState()` — no widgets needed

### Date and size from file metadata

Use `File.lastModified()` for the date — reliable regardless of filename format. Format as `Mar 31, 18:03` using `kotlinx.datetime.Instant.fromEpochMilliseconds()` + `LocalDateTime` formatting.

**Dependency:** Add `kotlinx-datetime` to `build.gradle` (+ jarJar bundle). This library is also used by the date grouping feature.

Use `File.length()` for the file size — no need for `FileUtils.sizeOf()`.

### File size utility

Move `getFileSizeMegaBytes` from `ScreenshotScreen.Companion` to `Util` so both screens can use it. Rewrite to use `File.length()` directly.

## Files to modify

| File | Changes |
|------|---------|
| `gui/GalleryScreen.kt` | Add hover detection + overlay rendering in extractRenderState() |
| `gui/ScreenshotScreen.kt` | Remove getFileSizeMegaBytes, use Util version |
| `utils/Util.kt` | Add getFileSizeFormatted() and parseDateFromFilename() |

## Verification

1. Open gallery — no overlays visible by default
2. Hover over a thumbnail — dark overlay appears at bottom with date and file size
3. Move mouse away — overlay disappears
4. Hover over different thumbnails — overlay updates correctly
5. Screenshot with non-standard filename — falls back to lastModified date
6. Open detail view — still shows file size (now using shared Util method)
