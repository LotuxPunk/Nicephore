# Code Cleanup & Robustness

## Problem

The codebase has 11 `printStackTrace()`/`println()` calls instead of proper logger usage, a potential race condition in `InitThread` writing to `Reference` fields, and no retry/timeout logic for downloading optimization tools (oxipng, ECT). These make debugging user reports difficult and cause silent failures.

## Solution

### 1. Replace all bare logging with Nicephore.LOGGER

Replace every `e.printStackTrace()` and `println()` with `Nicephore.LOGGER.error/warn/info()`.

**Call sites to fix:**
- `utils/Util.kt` — 2 `printStackTrace()` calls in `getBatchOfFiles()` and `getNumberOfFiles()`
- `utils/CopyImageToClipBoard.kt` — 1 `println()` debug output
- `thread/InitThread.kt` — 4 `printStackTrace()` calls + 1 `println()` debug output
- `client/gui/AbstractNicephoreScreen.kt` — 1 `printStackTrace()` in `readAspectRatio()`
- `clipboard/impl/MacOSClipboardManagerImpl.kt` — 1 `printStackTrace()`

Use `LOGGER.error(message, exception)` for failures, `LOGGER.warn()` for recoverable issues, `LOGGER.debug()` for diagnostic info.

### 2. Thread-safe Reference fields

`InitThread` runs on a background thread and writes tool paths/versions to `Reference.File` and `Reference.Command` fields. The game thread reads these later in `ScreenshotThread`.

Fix: Audit `Reference.kt` — if fields are compile-time constants (never written at runtime), make them `const val`. If they're written by `InitThread`, mark them `@Volatile` to ensure visibility across threads.

### 3. Download retry + timeout for tool downloads

In `InitThread`, HTTP downloads for oxipng and ECT have no retry or timeout:

- Add connection timeout: 10 seconds
- Add read timeout: 30 seconds
- Retry failed downloads up to 3 times with 2-second delay between attempts
- Log warning on each retry: `"Retrying download of {tool} (attempt {n}/3)"`
- Log error on final failure: `"Failed to download {tool} after 3 attempts"`
- After all retries exhausted, send player a chat message on next tick: `"Nicephore: optimization tools unavailable — screenshots will not be optimized"`

### 4. Graceful fallback when tools missing

`ScreenshotThread` already catches exceptions when tools are missing, but uses `LOGGER.warn` inconsistently. Standardize:
- If tool binary not found at screenshot time: `LOGGER.warn("oxipng not found, skipping PNG optimization")`
- If tool execution fails: `LOGGER.error("oxipng optimization failed", exception)`

## Files to Modify

| File | Changes |
|------|---------|
| `utils/Util.kt` | Replace 2 `printStackTrace()` with LOGGER |
| `utils/CopyImageToClipBoard.kt` | Replace `println()` with LOGGER.debug |
| `utils/Reference.kt` | Audit fields, add `@Volatile` or `const` as needed |
| `thread/InitThread.kt` | Replace 5 bare log calls with LOGGER, add retry+timeout |
| `thread/ScreenshotThread.kt` | Standardize tool-missing error messages |
| `client/gui/AbstractNicephoreScreen.kt` | Replace `printStackTrace()` with LOGGER |
| `clipboard/impl/MacOSClipboardManagerImpl.kt` | Replace `printStackTrace()` with LOGGER |

## Verification

1. `./gradlew compileKotlin` passes
2. Run game, take a screenshot — verify log output uses `[nicephore/]` prefix (not bare stack traces)
3. Disconnect network, restart game — verify retry messages appear in log, player gets chat notification
4. With tools present, take screenshot — verify optimization runs normally
5. Delete `mods/nicephore/oxipng` — take screenshot — verify warning logged, no crash
