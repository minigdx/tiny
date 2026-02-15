package com.github.minigdx.tiny.debugger

import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StoredBreakpoint(
    val script: String,
    val line: Int,
    val enabled: Boolean,
    val condition: String? = null,
)

object BreakpointStorage {
    private val json = Json { ignoreUnknownKeys = true }

    fun save(
        gameId: String,
        breakpoints: Map<String, Set<Int>>,
        conditions: Map<String, Map<Int, String>>,
    ) {
        val stored = mutableListOf<StoredBreakpoint>()
        breakpoints.forEach { (script, lines) ->
            lines.forEach { line ->
                val condition = conditions[script]?.get(line)
                stored.add(StoredBreakpoint(script, line, true, condition))
            }
        }
        val data = json.encodeToString(stored)
        window.localStorage.setItem("tiny-debugger-bp-$gameId", data)
    }

    fun load(gameId: String): List<StoredBreakpoint> {
        val data = window.localStorage.getItem("tiny-debugger-bp-$gameId") ?: return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
