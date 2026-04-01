# Date Grouping & Sort Order

## Problem

The gallery shows screenshots as a flat list sorted by newest-first with no visual organization. Users with many screenshots can't quickly find images from a specific time period, and have no way to change sort order.

## Solution

### 1. Date grouping

**Dependency:** Add `kotlinx-datetime` to `build.gradle` (+ jarJar bundle) for date calculations.

Group screenshots into date sections based on `File.lastModified()` (converted via `kotlinx.datetime.Instant.fromEpochMilliseconds()`) with full-width header rows:
- **Today** — `lastModified` is today
- **Yesterday** — `lastModified` is yesterday
- **This Week** — `lastModified` within past 7 days (excluding today/yesterday)
- **This Month** — `lastModified` within past 30 days (excluding this week)
- **Older** — everything else

Empty groups are skipped. Each group header renders as a centered text label spanning the full grid width, taking the vertical space of one row. This means a page with 2 groups shows fewer thumbnails than a page with 0 groups.

### 2. Sort order

A cycle button in the toolbar allows switching between:
- **Newest first** (default) — `lastModified` descending
- **Oldest first** — `lastModified` ascending
- **Name A-Z** — alphabetical by filename
- **Name Z-A** — reverse alphabetical

The sort order is persisted in `NicephoreConfig.Client` as a new enum `SortOrder`.

Date grouping is only shown for date-based sorts (newest/oldest). When sorting by name, the flat list is shown without groups.

### 3. Data model

New `ScreenshotGroup` data class:
```kotlin
data class ScreenshotGroup(
    val label: String,        // e.g. "Today", "Yesterday"
    val files: List<File>
)
```

`GalleryScreen` computes groups from the sorted file list. The grid layout iterates over groups, rendering a header row then thumbnails for each group.

### 4. Pagination with groups

The page size (`imagesToDisplay`) stays based on total thumbnail slots. Group headers consume one row of vertical space each, reducing the visible thumbnails on pages that contain group boundaries. The pagination counts total files (not slots), so page navigation stays consistent.

## Files to modify

| File | Changes |
|------|---------|
| `enums/SortOrder.kt` | **New** — enum with NEWEST, OLDEST, NAME_ASC, NAME_DESC |
| `config/NicephoreConfig.kt` | Add `sortOrder` enum config property |
| `gui/GalleryScreen.kt` | Compute groups, render headers, apply sort, add sort button |
| `gui/AbstractNicephoreScreen.kt` | Add sort button to toolbar (or GalleryScreen adds it locally) |

## Verification

1. `./gradlew compileKotlin` passes
2. Open gallery — screenshots grouped under "Today", "Yesterday", etc.
3. Click sort button — cycles through sort orders, gallery updates
4. Sort by name — no date groups shown, flat alphabetical list
5. Navigate pages — pagination works correctly across group boundaries
6. Take new screenshot, reopen gallery — appears under "Today"
