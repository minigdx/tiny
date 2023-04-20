package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.LuaValue.Companion.varargsOf
import kotlin.test.Test
import kotlin.test.assertEquals

class StdLibTest {

    private val colors = listOf("#FFFFFF", "#000000")

    private val mockResources = object : GameResourceAccess {
        override val bootSpritesheet: SpriteSheet = SpriteSheet(
            0, "boot", ResourceType.BOOT_SPRITESHEET,
            PixelArray(1, 1, PixelFormat.INDEX), 1, 1,
        )
        override val frameBuffer: FrameBuffer = FrameBuffer(10, 10, ColorPalette(colors))
        override fun spritesheet(index: Int): SpriteSheet? = null
        override fun level(index: Int): GameLevel? = null
        override fun sound(index: Int): Sound? = null
    }

    private val gameOptions = GameOptions(
        10,
        10,
        colors,
        gameScripts = emptyList(),
        spriteSheets = emptyList(),
        gameLevels = emptyList()
    )

    private val listener = object : StdLibListener {
        override fun exit(nextScriptIndex: Int) = Unit
    }

    @Test
    fun it_print_text() {
        mockResources.frameBuffer.clear(0)
        mockResources.bootSpritesheet.pixels.set(0, 0, 1)

        val print = StdLib(gameOptions, mockResources, listener).print()
        // only a is an accepted letter as for the test, the bootspritesheet is too small
        print.invoke(varargsOf(arrayOf(valueOf("a"), valueOf(0), valueOf(0), valueOf(2))))

        val grouped = mockResources.frameBuffer.colorIndexBuffer.pixels.toSet()

        // The buffer should contains two colors
        assertEquals(2, grouped.size)
    }

    @Test
    fun it_print_text_with_default_color() {
        mockResources.frameBuffer.clear(0)
        mockResources.bootSpritesheet.pixels.set(0, 0, 1)

        val print = StdLib(gameOptions, mockResources, listener).print()
        print.invoke(varargsOf(arrayOf(valueOf("a"), valueOf(0), valueOf(0))))

        val grouped = mockResources.frameBuffer.colorIndexBuffer.pixels.toSet()

        // The buffer should contains two colors
        assertEquals(2, grouped.size)
    }
}
