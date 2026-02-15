package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.FileChangedMessage
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.FilesMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.WebSocket
import kotlin.js.json

class FileWatcherSocket(
    private val onFiles: (List<FileInfo>) -> Unit,
    private val onFileChanged: (FileInfo) -> Unit,
) {
    private var ws: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        val host = kotlinx.browser.window.location.host
        val protocol = if (kotlinx.browser.window.location.protocol == "https:") "wss" else "ws"
        ws = WebSocket("$protocol://$host/ws")

        ws?.onopen = {
            console.log("FileWatcher connected")
        }

        ws?.onmessage = { event ->
            val data = event.data as String
            try {
                val jsonObj = json.decodeFromString<JsonObject>(data)
                val type = jsonObj["type"]?.jsonPrimitive?.content
                when (type) {
                    "files" -> {
                        val msg = json.decodeFromString<FilesMessage>(data)
                        onFiles(msg.files)
                    }
                    "fileChanged" -> {
                        val msg = json.decodeFromString<FileChangedMessage>(data)
                        onFileChanged(msg.file)
                    }
                }
            } catch (e: Exception) {
                console.error("FileWatcher parse error", e)
            }
        }

        ws?.onclose = {
            console.log("FileWatcher disconnected, reconnecting...")
            kotlinx.browser.window.setTimeout({ connect() }, 2000)
        }

        ws?.onerror = {
            console.error("FileWatcher error")
        }
    }
}
