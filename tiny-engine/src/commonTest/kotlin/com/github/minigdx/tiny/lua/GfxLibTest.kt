package com.github.minigdx.tiny.lua

class GfxLibTest {
    // FIXME:

    /*
    private val frameBuffer = FrameBuffer(10, 10, ColorPalette(listOf("#FFFFFF")))

    private val gameResourceAccess =
        mock<GameResourceAccess> {
            every { frameBuffer } returns this@GfxLibTest.frameBuffer
            every { addOp(any()) } returns Unit
        }

    private val gameOptions = GameOptions(10, 10, listOf("#FFFFFF"), listOf("game.lua"), emptyList())

    @Test
    fun it_sets_the_clip() {
        val clip = GfxLib(gameResourceAccess, gameOptions).clip()
        clip.call(valueOf(1), valueOf(2), valueOf(3), valueOf(4))

        assertEquals(1, frameBuffer.clipper.left)
        assertEquals(2, frameBuffer.clipper.top)
        assertEquals(1 + 3, frameBuffer.clipper.right)
        assertEquals(2 + 4, frameBuffer.clipper.bottom)
    }

    @Test
    fun it_sets_the_dither() {
        val dither = GfxLib(gameResourceAccess, gameOptions).dither()

        dither.call(valueOf(0xA5A5))

        val a = frameBuffer.blender.mix(byteArrayOf(1), 0, 0, null)?.get(0) ?: 0
        val b = frameBuffer.blender.mix(byteArrayOf(1), 1, 0, null)?.get(0) ?: 0
        val c = frameBuffer.blender.mix(byteArrayOf(1), 0, 1, null)?.get(0) ?: 0
        val d = frameBuffer.blender.mix(byteArrayOf(1), 1, 1, null)?.get(0) ?: 0

        assertEquals(1, a)
        assertEquals(0, b)
        assertEquals(0, c)
        assertEquals(1, d)
    }

    @Test
    fun it_sets_the_dither_full_pattern() {
        val dither = GfxLib(gameResourceAccess, gameOptions).dither()

        /**
     * 1000 -> 8
     * 1100 -> 8 + 4 = 12 = C
     * 0010 -> 2
     * 0001 -> 1
     *
     * --> 8C21
     */
        dither.call(valueOf(0x8C21))

        val result = Array<Byte>(4 * 4) { 0x01 }
        for (x in 0 until 4) {
            for (y in 0 until 4) {
                val index = x + y * 4
                val r = frameBuffer.blender.mix(byteArrayOf(result[index]), x, y, null)?.get(0) ?: 0
                result[index] = r
            }
        }

        val expected = Array<Byte>(4 * 4) { i ->
            if (i == 0 || i == 4 || i == 5 || i == 10 || i == 15) {
                0x01
            } else {
                0x00
            }
        }
        result.forEachIndexed { index, value ->
            assertEquals(value, expected[index])
        }
    }

    @Test
    fun it_sets_the_dither_pattern_no_effect() {
        val dither = GfxLib(gameResourceAccess, gameOptions).dither()

        dither.call(valueOf(0xFFFF))

        val a = frameBuffer.blender.mix(byteArrayOf(1), 0, 0, null)?.get(0) ?: 0
        val b = frameBuffer.blender.mix(byteArrayOf(1), 1, 0, null)?.get(0) ?: 0
        val c = frameBuffer.blender.mix(byteArrayOf(1), 0, 1, null)?.get(0) ?: 0
        val d = frameBuffer.blender.mix(byteArrayOf(1), 1, 1, null)?.get(0) ?: 0

        assertEquals(1, a)
        assertEquals(1, b)
        assertEquals(1, c)
        assertEquals(1, d)
    }

    @Test
    fun it_reset_the_clip() {
        val clip = GfxLib(gameResourceAccess, gameOptions).clip()
        clip.call()

        assertEquals(0, frameBuffer.clipper.left)
        assertEquals(0, frameBuffer.clipper.top)
        assertEquals(10, frameBuffer.clipper.right)
        assertEquals(10, frameBuffer.clipper.bottom)
    }

     */
}
