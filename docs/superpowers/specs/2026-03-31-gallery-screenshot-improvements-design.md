# Gallery & Screenshot Screen Improvements

## Problem

The gallery and screenshot screens have three categories of issues:

1. **Performance** -- Blocking I/O on the render thread freezes the game when loading screenshots. Textures are re-registered and widgets are recreated every frame.
2. **Rendering architecture** -- Widget setup and texture registration happen in `extractRenderState()` (called every frame) instead of `init()` (called once). This violates MC 26.1's extract/render split.
3. **Hardcoded layout** -- Magic pixel offsets, fixed 4-column grid, GUI-scale threshold for row count. Layout doesn't adapt to different screen sizes.

## Solution

### 1. Async Image Loading with Coroutines

**Dependency:** Add `kotlinx-coroutines-core` to `build.gradle` (+ jarJar bundle).

**New class: `ScreenshotLoader`** (in `utils/`)
- Loads `NativeImage` from disk using `Dispatchers.IO`
- Returns results to the main thread using a Minecraft-aware dispatcher (`Dispatchers.Main` backed by `Minecraft.getInstance()::execute`)
- Supports cancellation via `CoroutineScope` tied to the screen lifecycle
- `onClose()` cancels all pending loads

**Loading flow:**
1. `init()` starts coroutine(s) to load images
2. Screen renders a loading placeholder (spinning dots or "Loading..." text) for each pending image slot
3. When a coroutine completes, it stores the loaded texture and the next `extractRenderState` picks it up
4. Texture registration happens once when the async load completes, not every frame

**Loading placeholder UI:**
- Each image slot shows a centered "Loading..." text while its image is being loaded
- If loading fails, show "Error" text in red in that slot
- Gallery page navigation cancels pending loads for the old page before starting new ones

### 2. Rendering Correctness

**Widget lifecycle:**
- Move all `addRenderableWidget()` calls from `extractRenderState()` to `init()`
- Remove `clearWidgets()` from `extractRenderState()` entirely
- When state changes (page navigation, filter change), call `rebuildWidgets()` which clears and re-adds
- `extractRenderState()` becomes read-only: blit textures, draw text overlays

**Texture lifecycle:**
- Register textures once when loaded (in `init()` or async callback), store the `Identifier`
- `extractRenderState()` uses stored `Identifier` for `blit()` -- no `tm.register()` per frame
- `onClose()` releases textures from the texture manager

**Fix `Util.fileToTexture()`:**
- Use `FileInputStream(file).use { }` to ensure stream is closed
- Return `null` instead of crashing with `!!` when `NativeImage.read()` fails
- Callers handle null with a placeholder texture or skip

### 3. Dynamic Grid Layout

**Computed layout:**
- Define named constants: `TOOLBAR_HEIGHT = 30`, `PADDING = 10`, `THUMBNAIL_BUTTON_HEIGHT = 20`, `BOTTOM_BAR_HEIGHT = 30`
- Available content area: `height - TOOLBAR_HEIGHT - BOTTOM_BAR_HEIGHT`
- Column count: configurable via settings slider (2-6), default "Auto"
- "Auto" mode: `columns = max(2, (width - 2 * PADDING) / (TARGET_THUMBNAIL_WIDTH + PADDING))`
- Row count: `rows = max(1, (contentHeight) / (thumbnailHeight + THUMBNAIL_BUTTON_HEIGHT + PADDING))`
- `imagesToDisplay = rows * columns`
- Grid positions computed from column/row indices with uniform padding

**Config slider in SettingsScreen:**
- New `NicephoreConfig.Client` property: `galleryColumns` (Int, 0 = auto, 2-6 = fixed)
- Slider widget in SettingsScreen showing "Auto" or the number

**ScreenshotScreen:**
- Replace `this.width * 0.5 * 1.2` with `width - 2 * SIDE_PADDING` capped to a max width
- Center image horizontally within available space

### 4. Resource Cleanup

- `Util.fileToTexture()`: close `FileInputStream` with `.use {}`, handle null gracefully
- Cancel coroutine scope in `onClose()` for both screens
- Unregister texture identifiers from `TextureManager` on close
- Remove redundant `screenshots.isNotEmpty()` double-check in `GalleryScreen.init()`

## Files to Modify

| File | Changes |
|------|---------|
| `build.gradle` | Add kotlinx-coroutines-core dependency + jarJar |
| `utils/ScreenshotLoader.kt` | **New** -- async image loading utility |
| `utils/Util.kt` | Fix fileToTexture() resource leak and null safety |
| `gui/AbstractNicephoreScreen.kt` | Extract layout constants, add rebuildWidgets() pattern |
| `gui/GalleryScreen.kt` | Async loading, dynamic grid, move widgets to init() |
| `gui/ScreenshotScreen.kt` | Async loading, computed sizing, move widgets to init() |
| `gui/SettingsScreen.kt` | Add column count slider |
| `config/NicephoreConfig.kt` | Add galleryColumns property |

## Verification

1. `./gradlew compileKotlin` passes
2. Open gallery with many screenshots (50+) -- no game freeze, loading placeholders visible
3. Navigate pages -- old page loads cancel, new page loads start
4. Verify FPS stays stable while gallery is open (no per-frame allocations)
5. Adjust column slider in settings -- gallery grid updates
6. Test on different GUI scales (1, 2, 3, auto)
7. Close gallery -- no texture leaks (check with F3 debug)
8. Open single screenshot view -- image loads async, no freeze
