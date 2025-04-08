package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.Frame
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.Song2
import com.github.minigdx.tiny.sound.WaveGenerator
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GfxLibTest {

    private val mockResources = object : GameResourceAccess {
        override val bootSpritesheet: SpriteSheet? = null
        override val frameBuffer: FrameBuffer = FrameBuffer(10, 10, ColorPalette(listOf("#FFFFFF")))
        override fun spritesheet(index: Int): SpriteSheet? = null
        override fun spritesheet(name: String): Int? = null
        override fun spritesheet(sheet: SpriteSheet) = Unit
        override fun newSpritesheetIndex(): Int = 0

        override fun level(index: Int): GameLevel? = null
        override fun sound(index: Int): Sound? = null
        override fun script(name: String): GameScript? = null
        override fun drawOffscreen(): Frame {
            TODO("Not yet implemented")
        }

        override fun note(wave: WaveGenerator) = Unit

        override fun sfx(song: Song2) = Unit
    }

    @Test
    fun it_sets_the_clip() {
        val clip = GfxLib(mockResources).clip()
        clip.call(valueOf(1), valueOf(2), valueOf(3), valueOf(4))

        assertEquals(1, mockResources.frameBuffer.clipper.left)
        assertEquals(2, mockResources.frameBuffer.clipper.top)
        assertEquals(1 + 3, mockResources.frameBuffer.clipper.right)
        assertEquals(2 + 4, mockResources.frameBuffer.clipper.bottom)
    }

    @Test
    fun it_sets_the_dither() {
        val dither = GfxLib(mockResources).dither()

        dither.call(valueOf(0xA5A5))

        val a = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 0, 0, null)?.get(0) ?: 0
        val b = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 1, 0, null)?.get(0) ?: 0
        val c = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 0, 1, null)?.get(0) ?: 0
        val d = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 1, 1, null)?.get(0) ?: 0

        assertEquals(1, a)
        assertEquals(0, b)
        assertEquals(0, c)
        assertEquals(1, d)
    }

    @Test
    fun it_sets_the_dither_pattern_no_effect() {
        val dither = GfxLib(mockResources).dither()

        dither.call(valueOf(0xFFFF))

        val a = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 0, 0, null)?.get(0) ?: 0
        val b = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 1, 0, null)?.get(0) ?: 0
        val c = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 0, 1, null)?.get(0) ?: 0
        val d = mockResources.frameBuffer.blender.mix(byteArrayOf(1), 1, 1, null)?.get(0) ?: 0

        assertEquals(1, a)
        assertEquals(1, b)
        assertEquals(1, c)
        assertEquals(1, d)
    }

    @Test
    fun it_reset_the_clip() {
        val clip = GfxLib(mockResources).clip()
        clip.call()

        assertEquals(0, mockResources.frameBuffer.clipper.left)
        assertEquals(0, mockResources.frameBuffer.clipper.top)
        assertEquals(10, mockResources.frameBuffer.clipper.right)
        assertEquals(10, mockResources.frameBuffer.clipper.bottom)
    }
}
