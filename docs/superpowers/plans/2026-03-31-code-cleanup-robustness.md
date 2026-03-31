# Code Cleanup & Robustness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all bare `printStackTrace()`/`println()` calls with proper logging, make `Reference` fields thread-safe, and add retry/timeout logic for tool downloads.

**Architecture:** Pure cleanup pass — no new features, no API changes. Replace 11 bare log calls across 6 files, add `@Volatile` to mutable `Reference` fields, and wrap `InitThread` downloads in a retry loop with timeouts.

**Tech Stack:** Kotlin, NeoForge 26.1, SLF4J via `Nicephore.LOGGER`

---

## File Structure

| File | Changes |
|------|---------|
| `utils/Util.kt` | Replace 2 `printStackTrace()` |
| `utils/CopyImageToClipBoard.kt` | Replace 1 `println()` |
| `utils/Reference.kt` | Add `@Volatile` to mutable fields |
| `thread/InitThread.kt` | Replace 5 bare log calls, add retry+timeout |
| `thread/ScreenshotThread.kt` | Standardize tool-missing error logging |
| `client/gui/AbstractNicephoreScreen.kt` | Replace 1 `printStackTrace()` |
| `clipboard/impl/MacOSClipboardManagerImpl.kt` | Replace 1 `printStackTrace()` |

All paths relative to `src/main/kotlin/com/vandendaelen/nicephore/`.

---

### Task 1: Replace bare logging in utility and clipboard files

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt`
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/CopyImageToClipBoard.kt`
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/client/gui/AbstractNicephoreScreen.kt`
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/clipboard/impl/MacOSClipboardManagerImpl.kt`

- [ ] **Step 1: Fix Util.kt — replace 2 printStackTrace calls**

In `Util.kt`, replace the catch block in `getBatchOfFiles()` (line 50-52):

```kotlin
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
```

with:

```kotlin
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to list screenshot files in {}", directory.absolutePath, e)
            emptyList()
        }
```

And replace the catch block in `getNumberOfFiles()` (line 62-64):

```kotlin
        } catch (e: IOException) {
            e.printStackTrace()
            0L
        }
```

with:

```kotlin
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to count files in {}", directory.absolutePath, e)
            0L
        }
```

- [ ] **Step 2: Fix CopyImageToClipBoard.kt — replace println**

In `CopyImageToClipBoard.kt`, replace line 33:

```kotlin
        println("Lost Clipboard Ownership")
```

with:

```kotlin
        Nicephore.LOGGER.debug("Lost clipboard ownership")
```

Add import at the top:

```kotlin
import com.vandendaelen.nicephore.Nicephore
```

- [ ] **Step 3: Fix AbstractNicephoreScreen.kt — replace printStackTrace**

In `AbstractNicephoreScreen.kt`, replace the catch block in `readAspectRatio()` (line 53-55):

```kotlin
        } catch (e: IOException) {
            e.printStackTrace()
        }
```

with:

```kotlin
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to read aspect ratio from {}", file.name, e)
        }
```

Add import at the top:

```kotlin
import com.vandendaelen.nicephore.Nicephore
```

- [ ] **Step 4: Fix MacOSClipboardManagerImpl.kt — replace printStackTrace**

In `MacOSClipboardManagerImpl.kt`, replace the catch block (line 16-18):

```kotlin
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
```

with:

```kotlin
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to copy screenshot to macOS clipboard", e)
            false
        }
```

Add import at the top:

```kotlin
import com.vandendaelen.nicephore.Nicephore
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/Util.kt src/main/kotlin/com/vandendaelen/nicephore/utils/CopyImageToClipBoard.kt src/main/kotlin/com/vandendaelen/nicephore/client/gui/AbstractNicephoreScreen.kt src/main/kotlin/com/vandendaelen/nicephore/clipboard/impl/MacOSClipboardManagerImpl.kt
git commit -m "fix: replace bare printStackTrace/println with Nicephore.LOGGER calls"
```

---

### Task 2: Make Reference fields thread-safe

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/utils/Reference.kt`

- [ ] **Step 1: Add @Volatile to all mutable fields**

Replace the entire `Reference.kt` with:

```kotlin
package com.vandendaelen.nicephore.utils

object Reference {
    const val DOWNLOADS_URLS: String = "https://raw.githubusercontent.com/LotuxPunk/Nicephore/master/references/v1.1/REFERENCES.json"
    const val VERSION: String = "1"
    const val OXIPNG_EXE: String = "oxipng.exe"
    const val ECT_EXE: String = "ect-0.8.3.exe"

    object Command {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }

    object File {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }

    object Version {
        @Volatile
        @JvmField
        var OXIPNG: String = ""
        @Volatile
        @JvmField
        var ECT: String = ""
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/utils/Reference.kt
git commit -m "fix: add @Volatile to mutable Reference fields for thread safety"
```

---

### Task 3: Add retry, timeout, and proper logging to InitThread

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/thread/InitThread.kt`

- [ ] **Step 1: Rewrite InitThread with retry logic, timeouts, and proper logging**

Replace the entire `InitThread.kt` with:

```kotlin
package com.vandendaelen.nicephore.thread

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.utils.PlayerHelper
import com.vandendaelen.nicephore.utils.Reference
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.apache.commons.io.FileUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import java.util.zip.ZipInputStream

class InitThread(private val optimiseConfig: Boolean) : Thread() {

    override fun run() {
        if (!optimiseConfig) return

        try {
            if (Files.exists(DESTINATION.toPath())) {
                updateExistingInstall()
            } else {
                freshInstall()
            }
        } catch (e: Exception) {
            Nicephore.LOGGER.error("Failed to initialize optimization tools", e)
            notifyToolsUnavailable()
        }
    }

    private fun updateExistingInstall() {
        try {
            val response = getResponse(getJsonReader(REFERENCES_JSON))
            if (response != null) {
                applyResponse(response)
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.warn("Failed to read local references.json, attempting remote update", e)
        }

        try {
            val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))
            if (response != null) {
                if (Reference.Version.OXIPNG != response.oxipng_version) {
                    Reference.Version.OXIPNG = response.oxipng_version
                    downloadAndExtract(response.oxipng, OXIPNG_ZIP, "oxipng")
                }
                if (Reference.Version.ECT != response.ect_version) {
                    Reference.Version.ECT = response.ect_version
                    downloadAndExtract(response.ect, ECT_ZIP, "ECT")
                }
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed to check for tool updates", e)
            notifyToolsUnavailable()
        }
    }

    private fun freshInstall() {
        try {
            Files.createDirectory(DESTINATION.toPath())
            val response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON))

            if (response != null) {
                applyResponse(response)
                downloadAndExtract(response.oxipng, OXIPNG_ZIP, "oxipng")
                downloadAndExtract(response.ect, ECT_ZIP, "ECT")
            }
        } catch (e: IOException) {
            Nicephore.LOGGER.error("Failed fresh install of optimization tools", e)
            notifyToolsUnavailable()
        }
    }

    private fun applyResponse(response: Response) {
        Reference.Command.OXIPNG = response.oxipng_command
        Reference.Command.ECT = response.ect_command
        Reference.File.OXIPNG = response.oxipng_file
        Reference.File.ECT = response.ect_file
        Reference.Version.OXIPNG = response.oxipng_version
        Reference.Version.ECT = response.ect_version
    }

    private fun notifyToolsUnavailable() {
        Minecraft.getInstance().execute {
            PlayerHelper.sendMessage(
                Component.translatable("nicephore.tools.unavailable")
            )
        }
    }

    private fun getResponse(reader: JsonReader): Response? {
        val gson = Gson()
        val collectionType = object : TypeToken<Collection<Response>>() {}.type
        val responses: Collection<Response> = gson.fromJson(reader, collectionType)
        return responses.firstOrNull { it.platform == OperatingSystems.getOS().name }
    }

    internal class Response {
        var platform: String = ""
        var oxipng: String = ""
        var oxipng_file: String = ""
        var oxipng_command: String = ""
        var oxipng_version: String = ""
        var ect: String = ""
        var ect_file: String = ""
        var ect_command: String = ""
        var ect_version: String = ""
    }

    @Throws(IOException::class)
    private fun getJsonReader(url: String, file: File): JsonReader {
        FileUtils.copyURLToFile(URI(url).toURL(), file, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
        return getJsonReader(file)
    }

    @Throws(FileNotFoundException::class)
    private fun getJsonReader(file: File): JsonReader {
        return JsonReader(FileReader(file))
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000

        private val DESTINATION = File(
            Minecraft.getInstance().gameDirectory.absolutePath,
            "mods${File.separator}nicephore"
        )
        private val REFERENCES_JSON = File(DESTINATION, "${File.separator}references.json")
        private val OXIPNG_ZIP = File(DESTINATION, "${File.separator}oxipng.zip")
        private val ECT_ZIP = File(DESTINATION, "${File.separator}ect.zip")

        private fun downloadAndExtract(url: String, zip: File, toolName: String) {
            var lastException: Exception? = null

            for (attempt in 1..MAX_RETRIES) {
                try {
                    downloadFile(url, zip)
                    unzip(zip.absolutePath, DESTINATION.absolutePath)
                    Nicephore.LOGGER.info("Successfully downloaded {}", toolName)
                    return
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < MAX_RETRIES) {
                        Nicephore.LOGGER.warn("Retrying download of {} (attempt {}/{})", toolName, attempt + 1, MAX_RETRIES)
                        sleep(RETRY_DELAY_MS)
                    }
                }
            }

            Nicephore.LOGGER.error("Failed to download {} after {} attempts", toolName, MAX_RETRIES, lastException)
        }

        private fun downloadFile(url: String, destination: File) {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS.toLong()))
                .build()

            val request = HttpRequest.newBuilder()
                .uri(URI(url))
                .timeout(Duration.ofMillis(READ_TIMEOUT_MS.toLong()))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())

            if (response.statusCode() != 200) {
                throw IOException("HTTP ${response.statusCode()} downloading $url")
            }

            response.body().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }

        private fun unzip(zipFilePath: String, destDir: String) {
            val dir = File(destDir)
            if (!dir.exists()) dir.mkdirs()
            val destDirPath = dir.canonicalPath
            FileInputStream(zipFilePath).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var ze = zis.nextEntry
                    while (ze != null) {
                        val fileName = ze.name
                        val newFile = File(destDir + File.separator + fileName)
                        if (!newFile.canonicalPath.startsWith(destDirPath + File.separator)) {
                            throw IOException("Entry is outside of the target dir: $fileName")
                        }
                        Nicephore.LOGGER.debug("Extracting {}", newFile.absolutePath)
                        File(newFile.parent).mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                        ze = zis.nextEntry
                    }
                    zis.closeEntry()
                }
            }
        }
    }
}
```

- [ ] **Step 2: Add translation key for tools unavailable message**

In `src/main/resources/assets/nicephore/lang/en_us.json`, add:

```json
"nicephore.tools.unavailable": "Nicephore: optimization tools unavailable — screenshots will not be optimized"
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/thread/InitThread.kt src/main/resources/
git commit -m "feat: add retry/timeout for tool downloads, replace bare logging in InitThread"
```

---

### Task 4: Standardize tool-missing logging in ScreenshotThread

**Files:**
- Modify: `src/main/kotlin/com/vandendaelen/nicephore/thread/ScreenshotThread.kt`

- [ ] **Step 1: Improve ECT error logging**

In `ScreenshotThread.kt`, replace the ECT catch block (lines 63-66):

```kotlin
                    } catch (e: Exception) {
                        Nicephore.LOGGER.warn("Unable to optimise screenshot JPEG with ECT. Is it missing from the mods folder?")
                        Nicephore.LOGGER.warn(e.message)
                    }
```

with:

```kotlin
                    } catch (e: Exception) {
                        Nicephore.LOGGER.warn("ECT not found or failed, skipping JPEG optimization", e)
                    }
```

- [ ] **Step 2: Improve oxipng error logging**

Replace the oxipng catch block (lines 76-79):

```kotlin
                } catch (e: Exception) {
                    Nicephore.LOGGER.warn("Unable to optimise screenshot PNG with Oxipng. Is it missing from the mods folder?")
                    Nicephore.LOGGER.warn(e.message)
                }
```

with:

```kotlin
                } catch (e: Exception) {
                    Nicephore.LOGGER.warn("oxipng not found or failed, skipping PNG optimization", e)
                }
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/vandendaelen/nicephore/thread/ScreenshotThread.kt
git commit -m "fix: standardize tool-missing error logging in ScreenshotThread"
```

---

### Task 5: Final verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean compileKotlin`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Grep for any remaining bare log calls**

Run: `grep -rn "printStackTrace\|println(" src/main/kotlin/`

Expected: No matches (all replaced with LOGGER calls)

- [ ] **Step 3: Commit if any fixes needed**

If grep found remaining calls, fix them and commit:

```bash
git add -A
git commit -m "fix: remove remaining bare log calls"
```
