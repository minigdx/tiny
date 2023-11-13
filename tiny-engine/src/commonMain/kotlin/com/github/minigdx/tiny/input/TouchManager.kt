package com.github.minigdx.tiny.input

import com.github.minigdx.tiny.input.internal.InternalTouchEvent
import com.github.minigdx.tiny.input.internal.InternalTouchEventWay
import com.github.minigdx.tiny.input.internal.ObjectPool

typealias KeyCode = Int

class TouchManager(lastKeyCode: KeyCode) {

    private val touchSignalCache = TouchSignal.values()

    internal val lastTouch: Vector2 = Vector2(0f, 0f)

    private val type = Array<Any?>(TouchSignal.values().size) { null }
    private val touch = Array<Vector2?>(TouchSignal.values().size) { null }
    private val justTouch = Array<Vector2?>(TouchSignal.values().size) { null }

    private val keyPressed = Array(lastKeyCode + 1) { false }
    private val justKeyPressed = Array(lastKeyCode + 1) { false }
    private val justPressedKeyCode = mutableSetOf<KeyCode>()

    /**
     * Any key pressed will be computed regarding the number of key pressed.
     * If more than 0, it means that a least one key is pressed.
     */
    private var numberOfKeyPressed = 0

    /**
     * Is any key is currently pressed?
     */
    val isAnyKeyPressed: Boolean
        get() = numberOfKeyPressed != 0

    /**
     * Is any key was just pressed?
     */
    var isAnyKeyJustPressed = false
        private set

    private val eventsPool = InternalTouchEventObjectPool()

    inner class InternalTouchEventObjectPool : ObjectPool<InternalTouchEvent>(10) {

        override fun newInstance(): InternalTouchEvent {
            return InternalTouchEvent()
        }

        override fun destroyInstance(obj: InternalTouchEvent) {
            obj.keycode = null
        }
    }

    private val queueEvents = mutableListOf<InternalTouchEvent>()

    /**
     * Converting the platform specific touch [id] into a [TouchSignal].
     */
    fun getTouchSignal(id: Any, default: TouchSignal): TouchSignal {
        type.forEachIndexed { index, savedId ->
            if (id == savedId) {
                return touchSignalCache[index]
            }
        }
        return assignTouchSignal(id, default)
    }

    private fun assignTouchSignal(id: Any, default: TouchSignal): TouchSignal {
        type[default.ordinal] = id
        return default
    }

    /**
     * To be called when a touch has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchDown(touchSignal: TouchSignal, x: Float, y: Float) {
        val event = eventsPool.newInstance()

        val ordinal = touchSignal.ordinal
        // Keep the position if the touch is currently touched
        val position = touch[ordinal] ?: event.position

        event.way = InternalTouchEventWay.DOWN
        event.touchSignal = touchSignal
        event.position = position.apply {
            position.x = x
            position.y = y
        }
        queueEvents.add(event)
    }

    /**
     * To be called when a touch move has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchMove(touchSignal: TouchSignal, x: Float, y: Float) {
        val event = eventsPool.obtain()
        event.way = InternalTouchEventWay.MOVE
        event.position.x = x
        event.position.y = y
        event.touchSignal = touchSignal
        queueEvents.add(event)
    }

    /**
     * To be called when a touch up has been detected on the platform.
     *
     * The platform touch needs to be converted using [getTouchSignal]
     * before being passed to [touchSignal].
     *
     * The coordinates [x] and [y] are screen coordinates.
     */
    fun onTouchUp(touchSignal: TouchSignal) {
        val event = eventsPool.obtain()
        event.way = InternalTouchEventWay.UP
        event.touchSignal = touchSignal
        queueEvents.add(event)
    }

    fun isTouched(touchSignal: TouchSignal): Vector2? {
        return touch[touchSignal.ordinal]
    }

    fun isJustTouched(touchSignal: TouchSignal): Vector2? {
        return justTouch[touchSignal.ordinal]
    }

    fun isKeyPressed(keyCode: KeyCode): Boolean = keyPressed[keyCode]

    fun isKeyJustPressed(keyCode: KeyCode): Boolean = justKeyPressed[keyCode]

    fun onKeyPressed(keyCode: KeyCode) {
        val event = eventsPool.obtain()
        event.way = InternalTouchEventWay.DOWN
        event.keycode = keyCode
        queueEvents.add(event)
    }

    fun onKeyReleased(keyCode: KeyCode) {
        val event = eventsPool.obtain()
        event.way = InternalTouchEventWay.UP
        event.keycode = keyCode
        queueEvents.add(event)
    }

    /**
     * Process received touch events
     *
     * Previous received events will be discarded.
     */
    fun processReceivedEvent() {
        // Reset previous justTouch
        (justTouch.indices).forEach { i ->
            justTouch[i] = null
        }

        // Reset previous key pressed
        justPressedKeyCode.forEach {
            justKeyPressed[it] = false
        }
        justPressedKeyCode.clear()
        isAnyKeyJustPressed = false

        queueEvents.forEach { event ->
            if (event.isTouchEvent) {
                processTouchEvent(event)
            } else {
                processKeyEvent(event)
            }
        }
        eventsPool.free(queueEvents)
        queueEvents.clear()
    }

    private fun processKeyEvent(event: InternalTouchEvent) {
        val keycode = event.keycode!!
        when (event.way) {
            InternalTouchEventWay.DOWN -> {
                // Set the key pressed only if the key is not ALREADY pressed
                // (Secure the state of the keys as in JS, the key signal is sent again when the key is not released)
                if (!keyPressed[keycode]) {
                    keyPressed[keycode] = true
                    justKeyPressed[keycode] = true
                    justPressedKeyCode.add(keycode)
                    isAnyKeyJustPressed = true
                    numberOfKeyPressed++
                }
            }

            InternalTouchEventWay.UP -> {
                keyPressed[keycode] = false
                numberOfKeyPressed--
            }

            InternalTouchEventWay.MOVE -> throw IllegalArgumentException("${event.keycode} is not supposed to move.")
        }
    }

    private fun processTouchEvent(event: InternalTouchEvent) {
        when (event.way) {
            InternalTouchEventWay.DOWN -> {
                justTouch[event.touchSignal.ordinal] = event.position
                touch[event.touchSignal.ordinal] = event.position
                lastTouch.set(event.position)
            }

            InternalTouchEventWay.MOVE -> {
                touch[event.touchSignal.ordinal]?.run {
                    x = event.position.x
                    y = event.position.y
                }
                lastTouch.set(event.position)
            }

            InternalTouchEventWay.UP -> {
                touch[event.touchSignal.ordinal] = null
            }
        }
    }
}
