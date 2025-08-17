package com.github.minigdx.tiny.lua

class StdLibTest {
    // FIXME: fix

    /*
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

    private val gameResourceAccess =
        mock<GameResourceAccess2> {
            every { frameBuffer } returns this@StdLibTest.frameBuffer
            every { bootSpritesheet } returns spritesheet
            every { addOp(any()) } returns Unit
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

        val print = StdLib(gameOptions, gameResourceAccess).print()
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

        val print = StdLib(gameOptions, gameResourceAccess).print()
        print.invoke(varargsOf(arrayOf(valueOf("a"), valueOf(0), valueOf(0))))

        val grouped = frameBuffer.colorIndexBuffer.pixels.toSet()

        // The buffer should contain two colors
        assertEquals(2, grouped.size)
    }

     */
}
