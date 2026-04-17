package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.platform.PlatformContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

object NicephoreConfigHolder {
    internal var pathOverride: Path? = null

    private val path: Path
        get() = pathOverride ?: PlatformContext.current.configDir.resolve("nicephore.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    var current: NicephoreConfig = NicephoreConfig()
        private set

    fun load(): NicephoreConfig {
        current = runCatching {
            json.decodeFromString<NicephoreConfig>(path.readText())
        }.getOrElse {
            val defaults = NicephoreConfig()
            runCatching { save(defaults) }.onFailure { e ->
                Nicephore.LOGGER.warn("Failed to write default nicephore.json: {}", e.message)
            }
            defaults
        }
        return current
    }

    fun update(mutate: (NicephoreConfig) -> NicephoreConfig) {
        val next = mutate(current)
        current = next
        save(next)
    }

    private fun save(cfg: NicephoreConfig) {
        path.parent?.createDirectories()
        path.writeText(json.encodeToString(cfg))
    }
}
