package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.MouseProject
import com.github.minigdx.tiny.input.TouchManager
import com.github.minigdx.tiny.input.TouchSignal
import com.github.minigdx.tiny.input.TouchSignal.TOUCH1
import com.github.minigdx.tiny.input.TouchSignal.TOUCH2
import com.github.minigdx.tiny.input.TouchSignal.TOUCH3
import com.github.minigdx.tiny.input.Vector2
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.TouchEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.experimental.and

class JsInputHandler(
    private val canvas: HTMLCanvasElement,
    private val projector: MouseProject,
) : InputHandler, InputManager {
    init {
        canvas.addEventListener("keydown", ::keyDown, false)
        canvas.addEventListener("keyup", ::keyUp, false)
        canvas.addEventListener("touchstart", ::touchStart, false)
        canvas.addEventListener("touchend", ::touchEnd, false)
        canvas.addEventListener("touchmove", ::touchMove, false)
        canvas.addEventListener("mousedown", ::mouseDown, false)
        canvas.addEventListener("mouseup", ::mouseUp, false)
        canvas.addEventListener("mousemove", ::mouseMove, false)
        canvas.addEventListener("mouseleave", ::mouseLeave, false)
        canvas.addEventListener("mouseenter", ::mouseEnter, false)
    }

    private val flagMouse1: Short = 0x1
    private val flagMouse2: Short = 0x10
    private val flagMouse3: Short = 0x100
    private val flags = arrayOf(flagMouse1, flagMouse2, flagMouse3)
    private val touchSignals =
        arrayListOf(
            TOUCH1,
            TOUCH2,
            TOUCH3,
        )
    private val touchManager = TouchManager(UNKNOWN_KEY)

    private var isMouseInsideCanvas: Boolean = false
    private var mousePosition: Vector2 = Vector2(0f, 0f)

    private fun mouseDown(event: Event) {
        event as MouseEvent
        val jsTouch = event.buttons
        flags.forEachIndexed { index, flag ->
            if (jsTouch.and(flag) == flag) {
                val touch = touchSignals[index]

                val rect = canvas.getBoundingClientRect()
                val x = event.clientX.toFloat() - rect.left.toFloat()
                val y = event.clientY.toFloat() - rect.top.toFloat()

                val gamePosition = projector.project(x, y)
                gamePosition?.let { (gameX, gameY) ->
                    touchManager.onTouchDown(touch, gameX, gameY)
                }
            }
        }
    }

    private fun mouseUp(event: Event) {
        event as MouseEvent
        touchSignals.forEach { touch ->
            touchManager.onTouchUp(touch)
        }
    }

    private fun mouseMove(event: Event) {
        event as MouseEvent
        val jsTouch = event.buttons
        flags.forEachIndexed { index, flag ->
            if (jsTouch.and(flag) == flag) {
                val touch = touchSignals[index]

                val rect = canvas.getBoundingClientRect()
                val x = event.clientX.toFloat() - rect.left.toFloat()
                val y = event.clientY.toFloat() - rect.top.toFloat()

                val gamePosition = projector.project(x, y)
                gamePosition?.let { (gameX, gameY) ->
                    touchManager.onTouchMove(touch, gameX, gameY)
                }
            }
        }

        if (isMouseInsideCanvas) {
            val rect = canvas.getBoundingClientRect()
            val x = event.clientX.toFloat() - rect.left.toFloat()
            val y = event.clientY.toFloat() - rect.top.toFloat()

            val gamePosition = projector.project(x, y)
            if (gamePosition == null) {
                // the mouse is in the canvas but NOT in the game screen.
                isMouseInsideCanvas = false
            } else {
                val (gameX, gameY) = gamePosition
                mousePosition.x = gameX
                mousePosition.y = gameY
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mouseLeave(event: Event) {
        isMouseInsideCanvas = false
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mouseEnter(event: Event) {
        isMouseInsideCanvas = true
    }

    private fun touchStart(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val defaultTouch = TouchSignal.signal(it)
            if (defaultTouch != null) {
                val jsTouch = event.targetTouches[it]!!

                val touch = touchManager.getTouchSignal(jsTouch.identifier, defaultTouch)

                val rect = canvas.getBoundingClientRect()
                val x = jsTouch.clientX.toFloat() - rect.left.toFloat()
                val y = jsTouch.clientY.toFloat() - rect.top.toFloat()

                val gamePosition = projector.project(x, y)
                gamePosition?.let { (gameX, gameY) ->
                    touchManager.onTouchDown(touch, gameX, gameY)
                    mousePosition.x = gameX
                    mousePosition.y = gameY
                }
            }
        }
        if (event.cancelable && event.target == canvas) event.preventDefault()
    }

    private fun touchEnd(event: Event) {
        event as TouchEvent
        (0 until event.changedTouches.length).forEach {
            val defaultTouch = TouchSignal.signal(it)
            if (defaultTouch != null) {
                val jsTouch = event.changedTouches[it]!!
                val touch = touchManager.getTouchSignal(jsTouch.identifier, defaultTouch)
                touchManager.onTouchUp(touch)
            }
        }
        if (event.cancelable && event.target == canvas) event.preventDefault()
    }

    private fun touchMove(event: Event) {
        event as TouchEvent
        (0 until event.targetTouches.length).forEach {
            val defaultTouch = TouchSignal.signal(it)
            if (defaultTouch != null) {
                val jsTouch = event.targetTouches[it]!!
                val touch = touchManager.getTouchSignal(jsTouch.identifier, defaultTouch)

                val rect = canvas.getBoundingClientRect()
                val x = jsTouch.clientX.toFloat() - rect.left.toFloat()
                val y = jsTouch.clientY.toFloat() - rect.top.toFloat()
                val gamePosition = projector.project(x, y)
                gamePosition?.let { (gameX, gameY) ->
                    touchManager.onTouchMove(touch, gameX, gameY)
                    mousePosition.x = gameX
                    mousePosition.y = gameY
                }
            }
        }

        if (event.cancelable && event.target == canvas) event.preventDefault()
    }

    private fun keyDown(event: Event) {
        event as KeyboardEvent
        if (event.keyCode in (0..256)) {
            touchManager.onKeyPressed(event.keyCode)
        }

        if (event.target == canvas) {
            event.preventDefault()
        }
    }

    private fun keyUp(event: Event) {
        event as KeyboardEvent
        if (event.keyCode in (0..256)) {
            touchManager.onKeyReleased(event.keyCode)
        }
        if (event.target == canvas) {
            event.preventDefault()
        }
    }

    override fun isKeyJustPressed(key: Key): Boolean =
        if (key == Key.ANY_KEY) {
            touchManager.isAnyKeyJustPressed
        } else {
            touchManager.isKeyJustPressed(key.keyCode)
        }

    override fun isKeyPressed(key: Key): Boolean =
        if (key == Key.ANY_KEY) {
            touchManager.isAnyKeyPressed
        } else {
            touchManager.isKeyPressed(key.keyCode)
        }

    override fun isTouched(signal: TouchSignal): Vector2? = touchManager.isTouched(signal)

    override fun isJustTouched(signal: TouchSignal): Vector2? = touchManager.isJustTouched(signal)

    override fun touchIdlePosition(): Vector2? {
        return if (isMouseInsideCanvas) {
            mousePosition
        } else {
            null
        }
    }

    override val currentTouch: Vector2
        get() = mousePosition

    override fun record() = Unit

    override fun reset() {
        touchManager.processReceivedEvent()
    }
}
