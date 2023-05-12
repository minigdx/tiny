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
}
