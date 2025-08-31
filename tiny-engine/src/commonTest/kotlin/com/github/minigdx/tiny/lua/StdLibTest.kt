package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.LuaValue.Companion.varargsOf
import kotlin.test.Test
import kotlin.test.assertEquals

class StdLibTest {
    private val colors = listOf("#FFFFFF", "#000000")

    private val frameBuffer = FrameBuffer(10, 10, ColorPalette(colors))
    private val spritesheet =
        SpriteSheet(
            version = 0,
            index = 0,
            name = "boot",
            type = ResourceType.BOOT_SPRITESHEET,
            pixels = PixelArray(1, 1, PixelFormat.INDEX),
            width = 1,
            height = 1,
        )

    private val virtualFrameBuffer = mock<VirtualFrameBuffer> {
        every { drawPrimitive(any()) } calls { (block: (FrameBuffer) -> Unit) -> block(frameBuffer) }
    }

    private val gameResourceAccess = mock<GameResourceAccess> {
        every { bootSpritesheet } returns spritesheet
    }

    private val gameOptions =
        GameOptions(
            10,
            10,
            colors,
            gameScripts = emptyList(),
            spriteSheets = emptyList(),
            gameLevels = emptyList(),
        )

    @Test
    fun it_print_text() {
        frameBuffer.clear(0)
        spritesheet.pixels.set(0, 0, 1)

        val print = StdLib(gameOptions, gameResourceAccess, virtualFrameBuffer).print()
        // only "a" is an accepted letter as for the test, the bootspritesheet is too small
        print.invoke(varargsOf(arrayOf(valueOf("a"), valueOf(0), valueOf(0), valueOf(2))))

        val grouped = frameBuffer.colorIndexBuffer.pixels.toSet()

        // The buffer should contain two colors
        assertEquals(2, grouped.size)
    }

    @Test
    fun it_print_text_with_default_color() {
        frameBuffer.clear(0)
        spritesheet.pixels.set(0, 0, 1)

        val print = StdLib(gameOptions, gameResourceAccess, virtualFrameBuffer).print()
        print.invoke(varargsOf(arrayOf(valueOf("a"), valueOf(0), valueOf(0))))

        val grouped = frameBuffer.colorIndexBuffer.pixels.toSet()

        // The buffer should contain two colors
        assertEquals(2, grouped.size)
    }
}
