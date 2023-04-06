package com.github.minigdx.tiny.graphic

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorPaletteTest {

    private val palette = ColorPalette(listOf("#FF0000", "#00FF00", "#0000FF"))

    @Test
    fun getRGBA_it_get_a_color_from_its_index() {
        val transparent = palette.getRGBA(0)
        assertEquals(0x00.toByte(), transparent[0])
        assertEquals(0x00.toByte(), transparent[1])
        assertEquals(0x00.toByte(), transparent[2])
        assertEquals(0x00.toByte(), transparent[3])

        val red = palette.getRGBA(1)
        assertEquals(0xFF.toByte(), red[0])
        assertEquals(0x00.toByte(), red[1])
        assertEquals(0x00.toByte(), red[2])
        assertEquals(0xFF.toByte(), red[3])

        val green = palette.getRGBA(2)
        assertEquals(0x00.toByte(), green[0])
        assertEquals(0xFF.toByte(), green[1])
        assertEquals(0x00.toByte(), green[2])
        assertEquals(0xFF.toByte(), green[3])

        val blue = palette.getRGBA(3)
        assertEquals(0x00.toByte(), blue[0])
        assertEquals(0x00.toByte(), blue[1])
        assertEquals(0xFF.toByte(), blue[2])
        assertEquals(0xFF.toByte(), blue[3])


        val invalid = palette.getRGBA(4)
        assertEquals(0x00.toByte(), invalid[0])
        assertEquals(0x00.toByte(), invalid[1])
        assertEquals(0x00.toByte(), invalid[2])
        assertEquals(0x00.toByte(), invalid[3])
    }

    @Test
    fun getRGB_it_get_a_color_from_its_index() {
        val transparent = palette.getRGBA(0)
        assertEquals(0, transparent[0])
        assertEquals(0, transparent[1])
        assertEquals(0, transparent[2])

        val red = palette.getRGB(1)
        assertEquals(0xFF.toByte(), red[0])
        assertEquals(0x00.toByte(), red[1])
        assertEquals(0x00.toByte(), red[2])

        val green = palette.getRGB(2)
        assertEquals(0x00.toByte(), green[0])
        assertEquals(0xFF.toByte(), green[1])
        assertEquals(0x00.toByte(), green[2])

        val blue = palette.getRGB(3)
        assertEquals(0x00.toByte(), blue[0])
        assertEquals(0x00.toByte(), blue[1])
        assertEquals(0xFF.toByte(), blue[2])


        val invalid = palette.getRGB(4)
        assertEquals(0x00.toByte(), invalid[0])
        assertEquals(0x00.toByte(), invalid[1])
        assertEquals(0x00.toByte(), invalid[2])
    }

    @Test
    fun fromRGBA_it_get_the_index_of_a_RGBA_color() {
        val index = palette.fromRGBA(byteArrayOf(0xF0.toByte(), 0x00, 0x00, 0xFF.toByte()))
        assertEquals(1, index)
        val transparentIndex = palette.fromRGBA(byteArrayOf(0xF0.toByte(), 0x00, 0x00, 0x00))
        assertEquals(0, transparentIndex)
    }
}
