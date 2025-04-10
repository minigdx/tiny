package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals

class Vec2LibTest {
    @Test
    fun it_creates_empty_vec2() {
        val result = Vec2Lib.create().call()
        assertEquals(result.get("x"), valueOf(0))
        assertEquals(result.get("y"), valueOf(0))
    }

    @Test
    fun it_creates_an_vec2() {
        val result = Vec2Lib.create().call(valueOf(3), valueOf(4))
        assertEquals(result.get("x"), valueOf(3))
        assertEquals(result.get("y"), valueOf(4))
    }

    @Test
    fun it_normalize_a_vector() {
        val result = Vec2Lib.nor().call(Vec2Lib.create().call(valueOf(2), valueOf(0)))
        assertEquals(result.get("x"), valueOf(1))
        assertEquals(result.get("y"), valueOf(0))
    }

    @Test
    fun it_normalize_coordinates() {
        val result = Vec2Lib.nor().call(valueOf(5), valueOf(0))
        assertEquals(result.get("x"), valueOf(1))
        assertEquals(result.get("y"), valueOf(0))
    }

    @Test
    fun it_normalize_empty_vector() {
        val result = Vec2Lib.nor().call(valueOf(0), valueOf(0))
        assertEquals(result.get("x"), valueOf(0))
        assertEquals(result.get("y"), valueOf(0))
    }
}
