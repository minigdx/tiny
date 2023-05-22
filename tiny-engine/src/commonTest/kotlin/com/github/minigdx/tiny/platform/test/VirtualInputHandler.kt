package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.TouchSignal
import com.github.minigdx.tiny.input.Vector2

class VirtualInputHandler : InputHandler, InputManager {

    private val previousFrame: MutableSet<Key> = mutableSetOf()
    private var pressed: MutableSet<Key> = mutableSetOf()
    fun press(key: Key) {
        pressed.add(key)
        previousFrame.add(key)
    }

    fun release(key: Key) {
        pressed.remove(key)
        previousFrame.remove(key)
    }

    override fun isKeyJustPressed(key: Key): Boolean {
        return previousFrame.contains(key)
    }

    override fun isKeyPressed(key: Key): Boolean {
        return pressed.contains(key)
    }

    override fun isTouched(signal: TouchSignal): Vector2? {
        TODO("Not yet implemented")
    }

    override fun isJustTouched(signal: TouchSignal): Vector2? {
        TODO("Not yet implemented")
    }

    override val currentTouch: Vector2
        get() = TODO("Not yet implemented")

    override fun record() {
        previousFrame.clear()
    }

    override fun reset() = Unit
}
