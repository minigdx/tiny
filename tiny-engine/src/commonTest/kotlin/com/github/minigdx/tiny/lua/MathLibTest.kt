package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals

class MathLibTest {
    val lib = MathLib()

    @Test
    fun sign() {
        val sign = lib.sign()

        assertEquals(-1, sign.call(valueOf(-10)).toint())
        assertEquals(1, sign.call(valueOf(10)).toint())
        assertEquals(-1, sign.call(valueOf(-0.1)).toint())
        assertEquals(1, sign.call(valueOf(0.1)).toint())
        assertEquals(1, sign.call(valueOf("whatever")).toint())
    }

    @Test
    fun clamp() {
        val clamp = lib.clamp()

        assertEquals(0, clamp.call(valueOf(-1), valueOf(0), valueOf(1)).toint(), "Values within the given bounds should be returned as-is.")
        assertEquals(
            -1,
            clamp.call(valueOf(-1), valueOf(-2), valueOf(1)).toint(),
            "The lower bound should be returned when greater than the given value.",
        )
        assertEquals(
            1,
            clamp.call(valueOf(-1), valueOf(2), valueOf(1)).toint(),
            "The upper bound should be returned when less than the given value.",
        )
        assertEquals(
            0,
            clamp.call(valueOf(2), valueOf(1), valueOf(0)).toint(),
            "The upper bound should be returned when less than the lower bound.",
        )
        assertEquals(
            1,
            clamp.call(valueOf(1), valueOf(0), valueOf(1)).toint(),
            "The upper bound should be returned when equal to the lower bound.",
        )
        assertEquals(0, clamp.call(valueOf("junk"), valueOf("value"), valueOf("test")).toint())
    }
}
