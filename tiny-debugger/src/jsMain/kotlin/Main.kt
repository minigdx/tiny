package com.github.minigdx.tiny.debugger

import kotlinx.browser.window

fun main() {
    val app = DebuggerApp()
    window.onload = {
        app.init()
    }
}
