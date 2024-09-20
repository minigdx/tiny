package com.github.minigdx.tiny.sound

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.platform.webgl.JsInputHandler

actual fun createInputHandler(): InputHandler {
    return JsInputHandler()
}
