package com.github.minigdx.tiny.platform.android

import android.view.KeyEvent
import android.view.MotionEvent
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.TouchSignal
import com.github.minigdx.tiny.input.Vector2

class AndroidInputHandler(
    private val gameOptions: GameOptions,
) : InputHandler {
    private val keyPressed = mutableSetOf<Key>()
    private val keyJustPressed = mutableSetOf<Key>()
    private val touches = mutableMapOf<Int, TouchSignal>()
    private val justTouches = mutableMapOf<Int, TouchSignal>()
    private var isTextInputEnabled = false
    private var textInput = StringBuilder()

    override fun isKeyPressed(key: Key): Boolean = keyPressed.contains(key)

    override fun isKeyJustPressed(key: Key): Boolean = keyJustPressed.contains(key)

    override fun isTouched(signal: TouchSignal): TouchSignal? {
        return touches.values.firstOrNull { it.justTouched == signal.justTouched }
    }

    override fun isJustTouched(signal: TouchSignal): TouchSignal? {
        return justTouches.values.firstOrNull { it.justTouched == signal.justTouched }
    }

    override fun touchIdInUsed(): Set<Int> = touches.keys

    override fun justTouchedIdInUsed(): Set<Int> = justTouches.keys

    override fun getTouchSignal(idx: Int): TouchSignal? = touches[idx]

    override fun getJustTouchSignal(idx: Int): TouchSignal? = justTouches[idx]

    override fun clear() {
        keyJustPressed.clear()
        justTouches.clear()
    }

    override fun record() {
        // No-op for Android - input is event-driven
    }

    override fun processTextInput(text: String) {
        if (isTextInputEnabled) {
            textInput.append(text)
        }
    }

    override fun getTextInput(): String? {
        return if (isTextInputEnabled && textInput.isNotEmpty()) {
            val result = textInput.toString()
            textInput.clear()
            result
        } else {
            null
        }
    }

    override fun setTextInputEnabled(enabled: Boolean) {
        isTextInputEnabled = enabled
        if (!enabled) {
            textInput.clear()
        }
    }

    override fun isTextInputEnabled(): Boolean = isTextInputEnabled

    // Android-specific methods for handling input events
    fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val key = mapAndroidKeyToKey(keyCode) ?: return false
        if (!keyPressed.contains(key)) {
            keyJustPressed.add(key)
        }
        keyPressed.add(key)
        return true
    }

    fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val key = mapAndroidKeyToKey(keyCode) ?: return false
        keyPressed.remove(key)
        return true
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(event.actionIndex)
                val x = event.getX(event.actionIndex) / gameOptions.zoom
                val y = event.getY(event.actionIndex) / gameOptions.zoom
                val signal = TouchSignal(
                    position = Vector2(x, y),
                    justTouched = true,
                )
                touches[pointerId] = signal
                justTouches[pointerId] = signal
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val x = event.getX(i) / gameOptions.zoom
                    val y = event.getY(i) / gameOptions.zoom
                    touches[pointerId]?.let { signal ->
                        touches[pointerId] = signal.copy(
                            position = Vector2(x, y),
                            justTouched = false,
                        )
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointerId = event.getPointerId(event.actionIndex)
                touches.remove(pointerId)
                justTouches.remove(pointerId)
            }
        }
        return true
    }

    private fun mapAndroidKeyToKey(keyCode: Int): Key? {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> Key.A
            KeyEvent.KEYCODE_B -> Key.B
            KeyEvent.KEYCODE_C -> Key.C
            KeyEvent.KEYCODE_D -> Key.D
            KeyEvent.KEYCODE_E -> Key.E
            KeyEvent.KEYCODE_F -> Key.F
            KeyEvent.KEYCODE_G -> Key.G
            KeyEvent.KEYCODE_H -> Key.H
            KeyEvent.KEYCODE_I -> Key.I
            KeyEvent.KEYCODE_J -> Key.J
            KeyEvent.KEYCODE_K -> Key.K
            KeyEvent.KEYCODE_L -> Key.L
            KeyEvent.KEYCODE_M -> Key.M
            KeyEvent.KEYCODE_N -> Key.N
            KeyEvent.KEYCODE_O -> Key.O
            KeyEvent.KEYCODE_P -> Key.P
            KeyEvent.KEYCODE_Q -> Key.Q
            KeyEvent.KEYCODE_R -> Key.R
            KeyEvent.KEYCODE_S -> Key.S
            KeyEvent.KEYCODE_T -> Key.T
            KeyEvent.KEYCODE_U -> Key.U
            KeyEvent.KEYCODE_V -> Key.V
            KeyEvent.KEYCODE_W -> Key.W
            KeyEvent.KEYCODE_X -> Key.X
            KeyEvent.KEYCODE_Y -> Key.Y
            KeyEvent.KEYCODE_Z -> Key.Z
            KeyEvent.KEYCODE_0 -> Key.NUM0
            KeyEvent.KEYCODE_1 -> Key.NUM1
            KeyEvent.KEYCODE_2 -> Key.NUM2
            KeyEvent.KEYCODE_3 -> Key.NUM3
            KeyEvent.KEYCODE_4 -> Key.NUM4
            KeyEvent.KEYCODE_5 -> Key.NUM5
            KeyEvent.KEYCODE_6 -> Key.NUM6
            KeyEvent.KEYCODE_7 -> Key.NUM7
            KeyEvent.KEYCODE_8 -> Key.NUM8
            KeyEvent.KEYCODE_9 -> Key.NUM9
            KeyEvent.KEYCODE_SPACE -> Key.SPACE
            KeyEvent.KEYCODE_ENTER -> Key.ENTER
            KeyEvent.KEYCODE_ESCAPE -> Key.ESCAPE
            KeyEvent.KEYCODE_DEL -> Key.BACKSPACE
            KeyEvent.KEYCODE_TAB -> Key.TAB
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> Key.SHIFT_LEFT
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> Key.CONTROL_LEFT
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> Key.ALT_LEFT
            KeyEvent.KEYCODE_DPAD_UP -> Key.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Key.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Key.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Key.RIGHT
            KeyEvent.KEYCODE_F1 -> Key.F1
            KeyEvent.KEYCODE_F2 -> Key.F2
            KeyEvent.KEYCODE_F3 -> Key.F3
            KeyEvent.KEYCODE_F4 -> Key.F4
            KeyEvent.KEYCODE_F5 -> Key.F5
            KeyEvent.KEYCODE_F6 -> Key.F6
            KeyEvent.KEYCODE_F7 -> Key.F7
            KeyEvent.KEYCODE_F8 -> Key.F8
            KeyEvent.KEYCODE_F9 -> Key.F9
            KeyEvent.KEYCODE_F10 -> Key.F10
            KeyEvent.KEYCODE_F11 -> Key.F11
            KeyEvent.KEYCODE_F12 -> Key.F12
            else -> null
        }
    }
}
