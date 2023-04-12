package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.SpriteSheet
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GfxLibTest {

    private val mockResources = object : GameResourceAccess {
        override val bootSpritesheet: SpriteSheet? = null
        override val frameBuffer: FrameBuffer = FrameBuffer(10, 10, ColorPalette(listOf("#FFFFFF")))
        override fun spritesheet(index: Int): SpriteSheet? = null
        override fun level(index: Int): GameLevel? = null
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
    fun it_reset_the_clip() {
        val clip = GfxLib(mockResources).clip()
        clip.call()

        assertEquals(0, mockResources.frameBuffer.clipper.left)
        assertEquals(0, mockResources.frameBuffer.clipper.top)
        assertEquals(10, mockResources.frameBuffer.clipper.right)
        assertEquals(10, mockResources.frameBuffer.clipper.bottom)
    }
}
