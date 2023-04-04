package com.github.minigdx.tiny.input.internal

import com.github.minigdx.tiny.input.KeyCode
import com.github.minigdx.tiny.input.TouchSignal
import com.github.minigdx.tiny.input.Vector2

class InternalTouchEvent(
    /**
     * Key code is not null if the event is about a key change
     */
    var keycode: KeyCode? = null,
    /**
     * If the keycode is not null; it's a touch event
     */
    var touchSignal: TouchSignal = TouchSignal.TOUCH1,
    var position: Vector2 = Vector2(0f, 0f),
    var way: InternalTouchEventWay = InternalTouchEventWay.DOWN
) {
    val isTouchEvent: Boolean
        get() = keycode == null
}
