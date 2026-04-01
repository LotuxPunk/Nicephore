# Bulk Delete with Trash Folder

## Problem

1. Users can only delete screenshots one at a time — tedious with dozens of files.
2. Deleting is permanent — there's no undo if you accidentally delete a screenshot.

## Solution

### Trash folder instead of permanent delete

All deletions (single and bulk) move files to a trash folder instead of permanently deleting them:

- **Trash location:** `screenshots/.nicephore_trash/`
- Files are moved (not copied) to the trash folder, preserving the original filename
- If a file with the same name already exists in trash, append a counter (e.g., `screenshot_1.png`)
- Files in trash older than 30 days (based on when they were trashed, using `lastModified` after move) are permanently deleted
- Trash cleanup runs once when the gallery opens

### Trash viewer

A "Trash" button in the gallery toolbar opens a `TrashScreen`:
- Same grid layout as the gallery (reuses `AbstractNicephoreScreen` patterns)
- Shows thumbnails of trashed files
- Each thumbnail has a "Restore" button (moves file back to screenshots folder)
- A "Empty Trash" button permanently deletes all trashed files (with confirmation)
- A "Delete Permanently" button for individual files

### Selection mode in the gallery (bulk delete)

Add a "Select" toggle button in the gallery toolbar. When active:
- Each thumbnail shows a checkbox overlay in its top-left corner
- Clicking a thumbnail toggles its selection (instead of opening the detail view)
- A "Move to Trash (N)" button appears in the bottom bar
- A "Select All" / "Deselect All" button appears next to the select toggle

When selection mode is off (default), the gallery behaves as before.

### Selection state

- `selectedIndices: MutableSet<Int>` tracks which page-relative indices are selected
- Selection clears when changing pages or toggling selection mode off
- The trash button is only active when `selectedIndices.isNotEmpty()`

### Delete flow (both single and bulk)

1. User triggers delete (single from detail view, or bulk from gallery selection)
2. Confirmation dialog: "Move N screenshot(s) to trash?"
3. On confirm: move files to `.nicephore_trash/`, clear selection, reinitialize screen
4. On cancel: return with selection intact

### Updating existing single delete

The existing `DeleteConfirmScreen` and `ScreenshotScreen` delete button are updated to use the trash folder instead of `File.delete()`. The confirmation message changes from "Delete?" to "Move to trash?".

## Files to modify

| File | Changes |
|------|---------|
| `utils/TrashManager.kt` | **New** — trash folder management: move to trash, restore, cleanup 30-day old files, empty trash |
| `gui/GalleryScreen.kt` | Selection mode toggle, checkbox rendering, bulk trash button, trash viewer button |
| `gui/TrashScreen.kt` | **New** — grid view of trashed files with restore/delete/empty buttons |
| `gui/DeleteConfirmScreen.kt` | Update to use TrashManager instead of File.delete(), support file lists |
| `gui/ScreenshotScreen.kt` | Delete button uses TrashManager |

## Verification

1. Single delete from detail view — file moves to `.nicephore_trash/`, not permanently deleted
2. Open gallery, enter selection mode — checkboxes appear
3. Select 3 screenshots, click "Move to Trash (3)" — confirmation, files moved
4. Click "Trash" button — trash screen shows moved files
5. Click "Restore" on a trashed file — moves back to screenshots folder
6. Click "Empty Trash" — confirmation, all trashed files permanently deleted
7. Wait 30 days (or manually set old lastModified) — old trashed files auto-cleaned on gallery open
8. Gallery refreshes correctly after all operations
