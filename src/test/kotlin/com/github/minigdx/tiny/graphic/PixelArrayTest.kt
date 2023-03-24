package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.util.PixelFormat
import com.github.minigdx.tiny.util.PixelFormat.INDEX
import com.github.minigdx.tiny.util.PixelFormat.RGBA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class PixelArrayTest {

    @Test
    fun `copyFrom - copy a fragment from a pixel array`() {
        val source = PixelArray(3, 3, INDEX)
        source.set(0, 0, 1)
        source.set(1, 0, 1)
        source.set(2, 0, 1)

        val target = PixelArray(3, 3, INDEX)

        target.copyFrom(source, width = 2, height = 2)

        val exp = PixelArray(3, 3, INDEX)
        exp.set(0, 0, 1)
        exp.set(1, 0, 1)

        assertEquals(exp.toString(), target.toString())
    }

    @Test
    fun `copyFrom - copy from a pixel array`() {
        val source = PixelArray(10, 10, INDEX)
        source.set(9, 9, 1)
        val target = PixelArray(10, 10, INDEX)

        target.copyFrom(source)

        assertEquals(source.toString(), target.toString())
    }

    @Test
    fun `copyFrom - copy from a pixel array with RGBA`() {
        val source = PixelArray(2, 2, RGBA)
        source.set(1, 1, 1, 2, 3, 4)
        val target = PixelArray(2, 2, RGBA)

        target.copyFrom(source)

        assertEquals(source.toString(), target.toString())
    }

    @Test
    fun `copyFrom - copy from a pixel array with too long width`() {
        val source = PixelArray(2, 2, INDEX)
        source.set(1, 1, 1)
        val target = PixelArray(2, 2, INDEX)

        target.copyFrom(source, 0, 0, 0, 0, 5, 2)

        assertEquals(source.toString(), target.toString())
    }

    @Test
    fun `get set - get and set a pixel`() {
        val source = PixelArray(1, 1, INDEX)
        source.set(0, 0, 9)
        val result = source.get(0, 0)
        assertTrue(result.size == 1)
        assertTrue(result[0] == 9)
    }


    @Test
    fun `get set - get and set a RGBA pixel`() {
        val source = PixelArray(1, 1, PixelFormat.RGBA)
        source.set(0, 0, 1, 2, 3, 4)
        val result = source.get(0, 0)
        assertTrue(result.size == 4)
        assertTrue(result[0] == 1)
        assertTrue(result[1] == 2)
        assertTrue(result[2] == 3)
        assertTrue(result[3] == 4)
    }

    @Test
    fun `copyFrom - copy from a pixel array with reverseX`() {
        val source = PixelArray(3, 1, RGBA)
        source.set(0, 0, 1, 1, 1, 1)
        source.set(1, 0, 2, 2, 2, 2)
        source.set(2, 0, 3, 3, 3, 3)
        val target = PixelArray(3, 1, RGBA)

        target.copyFrom(source, reverseX = true)

        assertEquals(source.toString().reversed().replace("\n", ""), target.toString().replace("\n", ""))
    }

    @Test
    fun `copyFrom - copy from a pixel array with reverseY`() {
        val source = PixelArray(1, 3, RGBA)
        source.set(0, 0, 1, 1, 1, 1)
        source.set(0, 1, 2, 2, 2, 2)
        source.set(0, 2, 3, 3, 3, 3)
        val target = PixelArray(1, 3, RGBA)

        target.copyFrom(source, reverseY = true)

        assertEquals(source.toString().reversed().replace("\n", ""), target.toString().replace("\n", ""))
    }

    @Test
    fun `copyFrom - copy from a pixel array with reverseX and reverseY`() {
        val source = PixelArray(3, 3, RGBA)
        source.set(0, 0, 1, 1, 1, 1)
        source.set(1, 1, 2, 2, 2, 2)
        source.set(2, 2, 3, 3, 3, 3)
        val target = PixelArray(3, 3, RGBA)

        val inv = PixelArray(3, 3, RGBA)
        inv.set(0, 0, 3, 3, 3, 3)
        inv.set(1, 1, 2, 2, 2, 2)
        inv.set(2, 2, 1, 1, 1, 1)

        target.copyFrom(source, reverseX = true, reverseY = true)

        assertEquals(inv.toString(), target.toString())
    }
}
