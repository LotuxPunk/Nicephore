package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.enums.ScreenshotFilter
import com.vandendaelen.nicephore.enums.SortOrder
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class NicephoreConfigTest {
    @TempDir lateinit var tempDir: Path
    private lateinit var configPath: Path

    @BeforeEach
    fun setUp() {
        configPath = tempDir.resolve("nicephore.json")
        NicephoreConfigHolder.pathOverride = configPath
        NicephoreConfigHolder.update { NicephoreConfig() }
    }

    @AfterEach
    fun tearDown() {
        NicephoreConfigHolder.pathOverride = null
    }

    @Test
    fun `default config round-trips through JSON`() {
        val default = NicephoreConfig()
        val jsonString = Json { encodeDefaults = true }.encodeToString(NicephoreConfig.serializer(), default)
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(NicephoreConfig.serializer(), jsonString)
        assertEquals(default, decoded)
    }

    @Test
    fun `update persists the change to disk and is readable again`() {
        NicephoreConfigHolder.update { it.copy(galleryColumns = 4) }
        assertEquals(4, NicephoreConfigHolder.current.galleryColumns)
        assertTrue(configPath.exists())
        val raw = configPath.readText()
        assertTrue(raw.contains("\"galleryColumns\""))
        val reloaded = NicephoreConfigHolder.load()
        assertEquals(4, reloaded.galleryColumns)
    }

    @Test
    fun `unknown keys in on-disk JSON are ignored`() {
        configPath.parent.toFile().mkdirs()
        configPath.toFile().writeText("""{"galleryColumns": 3, "sortOrder": "OLDEST", "unknownField": true}""")
        val loaded = NicephoreConfigHolder.load()
        assertEquals(3, loaded.galleryColumns)
        assertEquals(SortOrder.OLDEST, loaded.sortOrder)
    }

    @Test
    fun `missing config file produces defaults and writes them back`() {
        val loaded = NicephoreConfigHolder.load()
        assertEquals(NicephoreConfig(), loaded)
        assertTrue(configPath.exists())
    }

    @Test
    fun `corrupted config file falls back to defaults`() {
        configPath.parent.toFile().mkdirs()
        configPath.toFile().writeText("not valid json")
        val loaded = NicephoreConfigHolder.load()
        assertEquals(NicephoreConfig(), loaded)
    }
}
