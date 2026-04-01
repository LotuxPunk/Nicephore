# Screenshot Renaming

## Problem

Screenshots use Minecraft's default timestamp naming (`2026-03-31_18.03.26.png`). Users cannot rename files from within the mod — they must use their OS file manager, which breaks the game flow.

## Solution

### Rename button in ScreenshotScreen

Add a "Rename" button in the detail view's bottom bar (next to Copy and Delete). Clicking it opens a rename dialog.

### Rename dialog

A new `RenameScreen` pushed as a GUI layer:
- Shows current filename in an editable text field (without extension)
- "Save" button applies the rename
- "Cancel" button returns to the detail view
- Extension is preserved automatically (`.png` or `.jpg`)

### Rename logic

1. User enters new name in text field
2. On "Save": rename the file on disk using `File.renameTo()`
3. If the target filename already exists, show an error message
4. If rename succeeds, invalidate the thumbnail cache for the old filename
5. Return to ScreenshotScreen which reinitializes with the updated file list

### Text field

Use Minecraft's `EditBox` widget for the text input. Pre-fill with the current filename (without extension). Limit to valid filename characters (no `/\:*?"<>|`).

## Files to modify

| File | Changes |
|------|---------|
| `gui/RenameScreen.kt` | **New** — text field + save/cancel dialog |
| `gui/ScreenshotScreen.kt` | Add "Rename" button in buildWidgets() |
| `utils/ThumbnailCache.kt` | Add method to delete cached thumbnail by filename |

## Verification

1. Open screenshot detail view — "Rename" button visible next to Copy and Delete
2. Click Rename — dialog opens with current filename pre-filled
3. Type new name, click Save — file renamed on disk, detail view refreshes
4. Open gallery — thumbnail shows new filename
5. Try renaming to existing filename — error message shown
6. Click Cancel — returns to detail view unchanged
