package com.github.minigdx.tiny.debugger

import kotlinx.browser.window

fun main() {
    val params = org.w3c.dom.url.URLSearchParams(window.location.search)
    val debugPort = params.get("debugPort")?.toIntOrNull() ?: 8081

    val app = DebuggerApp(debugPort)
    window.onload = {
        app.init()
    }
}
