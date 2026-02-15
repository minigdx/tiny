package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.BreakpointHit
import com.github.minigdx.tiny.cli.debug.CurrentBreakpoints
import com.github.minigdx.tiny.cli.debug.DebugRemoteCommand
import com.github.minigdx.tiny.cli.debug.Disconnect
import com.github.minigdx.tiny.cli.debug.EngineRemoteCommand
import com.github.minigdx.tiny.cli.debug.Reload
import com.github.minigdx.tiny.cli.debug.RequestBreakpoints
import com.github.minigdx.tiny.cli.debug.ResumeExecution
import com.github.minigdx.tiny.cli.debug.ToggleBreakpoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.WebSocket

class EngineDebugSocket(
    private val debugPort: Int,
    private val onBreakpointHit: (BreakpointHit) -> Unit,
    private val onCurrentBreakpoints: (CurrentBreakpoints) -> Unit,
    private val onReload: (Reload) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private var ws: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun connect() {
        val host = kotlinx.browser.window.location.hostname
        ws = WebSocket("ws://$host:$debugPort/debug")

        ws?.onopen = {
            console.log("Engine debug connected on port $debugPort")
            onConnected()
            send(RequestBreakpoints)
        }

        ws?.onmessage = { event ->
            val data = event.data as String
            try {
                val command = json.decodeFromString<EngineRemoteCommand>(data)
                when (command) {
                    is BreakpointHit -> onBreakpointHit(command)
                    is CurrentBreakpoints -> onCurrentBreakpoints(command)
                    is Reload -> onReload(command)
                }
            } catch (e: Exception) {
                console.error("Engine debug parse error", e)
            }
        }

        ws?.onclose = {
            console.log("Engine debug disconnected, reconnecting...")
            onDisconnected()
            kotlinx.browser.window.setTimeout({ connect() }, 2000)
        }

        ws?.onerror = {
            console.error("Engine debug error")
        }
    }

    fun send(command: DebugRemoteCommand) {
        val msg = json.encodeToString(command)
        ws?.send(msg)
    }

    fun toggleBreakpoint(
        script: String,
        line: Int,
        enabled: Boolean,
        condition: String? = null,
    ) {
        send(ToggleBreakpoint(script, line, enabled, condition))
    }

    fun resume() {
        send(ResumeExecution(advanceByStep = false))
    }

    fun step() {
        send(ResumeExecution(advanceByStep = true))
    }

    fun disconnect() {
        send(Disconnect)
    }
}
