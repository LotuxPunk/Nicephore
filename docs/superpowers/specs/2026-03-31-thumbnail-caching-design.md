# Thumbnail Caching

## Problem

The gallery loads full-resolution screenshots (2-10 MB each, 1920x1080+) to display ~200px thumbnails. A 12-image gallery page reads ~60 MB from disk and uploads 60 MB to GPU VRAM. This causes noticeable load times even with async loading, and wastes memory.

## Solution

Generate downscaled PNG thumbnails on first gallery visit, cache them on disk, and load those for subsequent visits. The single screenshot viewer continues loading full-resolution images.

### Cache structure

```
game_directory/.nicephore/thumbnails/
  2026-03-31_18.03.26.png    (~50 KB, 200px wide)
  2026-03-31_18.04.12.jpg    (~30 KB, 200px wide)
  ...
```

- **Location:** `game_directory/.nicephore/thumbnails/`
- **Format:** PNG, max 200px wide, aspect ratio preserved
- **Naming:** Same filename as original screenshot
- **Size:** ~40-80 KB each vs 2-5 MB originals (100:1 reduction)

### Cache invalidation

- Compare `lastModified` timestamp of original file vs cached thumbnail
- If original is newer or thumbnail doesn't exist: regenerate
- No hash needed — screenshots are write-once files

### Thumbnail generation flow

1. `ScreenshotLoader.loadBatch()` is called with a list of screenshot files
2. For each file, check if a cached thumbnail exists and is up-to-date
3. **Cache hit:** Read the small cached PNG via `NativeImage.read()` (fast, ~50 KB)
4. **Cache miss:** Read full image via `ImageIO.read()`, scale down with `BufferedImage.getScaledInstance()`, write to cache dir as PNG, then load the cached file via `NativeImage.read()`
5. All I/O happens on `Dispatchers.IO`, texture creation on render thread (existing pattern)

### What changes

- `ScreenshotLoader` gets a new `loadBatchThumbnails()` method (or loadBatch gains a `useThumbnails` flag)
- `ScreenshotLoader.loadSingle()` remains unchanged — full resolution for the viewer
- New `ThumbnailCache` utility class handles cache dir creation, path resolution, invalidation checks, and thumbnail generation
- `GalleryScreen.init()` calls the thumbnail-aware load method

### What doesn't change

- `ScreenshotScreen` keeps loading full-resolution images
- No config changes needed (caching is always-on, transparent)
- No UI changes

## Files to modify

| File | Changes |
|------|---------|
| `utils/ThumbnailCache.kt` | **New** — cache directory management, thumbnail generation, invalidation |
| `utils/ScreenshotLoader.kt` | Add thumbnail-aware loading for batch mode |
| `gui/GalleryScreen.kt` | Call thumbnail-aware loader |

## Verification

1. `./gradlew compileKotlin` passes
2. Open gallery for first time — thumbnails generated in `.nicephore/thumbnails/`, visible loading then images appear
3. Close and reopen gallery — images appear near-instantly (cached)
4. Delete `.nicephore/thumbnails/` — gallery regenerates them on next open
5. Take a new screenshot, open gallery — new screenshot has thumbnail generated
6. Open single screenshot viewer — still shows full-resolution image (not thumbnail)
7. Check memory: gallery page should use ~500 KB GPU memory instead of ~60 MB
