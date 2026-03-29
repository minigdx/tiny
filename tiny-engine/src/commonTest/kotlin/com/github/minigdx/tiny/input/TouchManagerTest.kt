package com.github.minigdx.tiny.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TouchManagerTest {
    @Test
    fun touch_down_then_is_just_touched_then_reset() {
        val touchManager = TouchManager(10)
        touchManager.onTouchDown(TouchSignal.TOUCH1, 0f, 0f)
        assertNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
        touchManager.processReceivedEvent()
        assertNotNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
        touchManager.processReceivedEvent()
        assertNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
    }

    @Test
    fun touch_down_then_reset_then_is_just_touched() {
        val touchManager = TouchManager(10)
        touchManager.onTouchDown(TouchSignal.TOUCH1, 0f, 0f)
        touchManager.processReceivedEvent()
        assertNotNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
        touchManager.processReceivedEvent()
        assertNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
    }

    @Test
    fun key_just_pressed() {
        // push key
        val touchManager = TouchManager(10)
        touchManager.onKeyPressed(1)
        touchManager.processReceivedEvent()
        assertTrue(touchManager.isKeyJustPressed(1))
        assertTrue(touchManager.isAnyKeyJustPressed)
        assertTrue(touchManager.isAnyKeyPressed)

        // keep it pressed
        touchManager.processReceivedEvent()
        assertFalse(touchManager.isKeyJustPressed(1))
        assertTrue(touchManager.isKeyPressed(1))
        assertFalse(touchManager.isAnyKeyJustPressed)
        assertTrue(touchManager.isAnyKeyPressed)

        // release the key
        touchManager.onKeyReleased(1)
        touchManager.processReceivedEvent()
        assertFalse(touchManager.isAnyKeyPressed)
        assertFalse(touchManager.isKeyPressed(1))
    }

    @Test
    fun key_pressed_without_released() {
        // push key
        val touchManager = TouchManager(10)
        touchManager.onKeyPressed(1)
        touchManager.processReceivedEvent()
        assertTrue(touchManager.isKeyJustPressed(1))
        assertTrue(touchManager.isAnyKeyJustPressed)
        assertTrue(touchManager.isAnyKeyPressed)

        // keep it pressed
        touchManager.onKeyPressed(1)
        touchManager.processReceivedEvent()
        assertFalse(touchManager.isKeyJustPressed(1))
        assertTrue(touchManager.isKeyPressed(1))
        assertFalse(touchManager.isAnyKeyJustPressed)
        assertTrue(touchManager.isAnyKeyPressed)

        // release the key
        touchManager.onKeyReleased(1)
        touchManager.processReceivedEvent()
        assertFalse(touchManager.isAnyKeyPressed)
        assertFalse(touchManager.isKeyPressed(1))
    }

    @Test
    fun touching() {
        // push key
        val touchManager = TouchManager(10)
        touchManager.onTouchDown(TouchSignal.TOUCH1, 0f, 0f)
        touchManager.processReceivedEvent()
        touchManager.onTouchMove(TouchSignal.TOUCH1, 10f, 0f)
        touchManager.processReceivedEvent()

        val pos = touchManager.isTouched(TouchSignal.TOUCH1)
        assertEquals(0f, pos?.x)
    }

    @Test
    fun resetAllState_clears_keys_and_touches() {
        val touchManager = TouchManager(10)

        // Press keys and touch
        touchManager.onKeyPressed(1)
        touchManager.onKeyPressed(2)
        touchManager.onTouchDown(TouchSignal.TOUCH1, 5f, 10f)
        touchManager.processReceivedEvent()

        // Verify state is set
        assertTrue(touchManager.isKeyPressed(1))
        assertTrue(touchManager.isKeyPressed(2))
        assertTrue(touchManager.isAnyKeyPressed)
        assertTrue(touchManager.isAnyKeyJustPressed)
        assertNotNull(touchManager.isTouched(TouchSignal.TOUCH1))
        assertNotNull(touchManager.isJustTouched(TouchSignal.TOUCH1))

        // Reset all state
        touchManager.resetAllState()

        // Verify all state is cleared
        assertFalse(touchManager.isKeyPressed(1))
        assertFalse(touchManager.isKeyPressed(2))
        assertFalse(touchManager.isAnyKeyPressed)
        assertFalse(touchManager.isAnyKeyJustPressed)
        assertNull(touchManager.isTouched(TouchSignal.TOUCH1))
        assertNull(touchManager.isJustTouched(TouchSignal.TOUCH1))
    }

    @Test
    fun resetAllState_clears_pending_events() {
        val touchManager = TouchManager(10)

        // Queue events but don't process them
        touchManager.onKeyPressed(3)
        touchManager.onTouchDown(TouchSignal.TOUCH2, 1f, 2f)

        // Reset all state (including pending events)
        touchManager.resetAllState()

        // Process should have nothing to apply
        touchManager.processReceivedEvent()

        assertFalse(touchManager.isKeyPressed(3))
        assertNull(touchManager.isTouched(TouchSignal.TOUCH2))
    }
}
