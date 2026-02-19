package com.github.minigdx.tiny.debugger

import kotlin.js.Promise

@JsName("Notification")
external class BrowserNotification(title: String, options: dynamic = definedExternally) {
    companion object {
        val permission: String

        fun requestPermission(): Promise<String>
    }
}
