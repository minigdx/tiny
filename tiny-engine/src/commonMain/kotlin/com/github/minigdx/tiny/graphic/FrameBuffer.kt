package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.PixelFormat
import kotlin.math.max
import kotlin.math.min

class Blender(private val gamePalette: ColorPalette) {

    private var switch: Array<ColorIndex> = Array(gamePalette.size) { index -> index }

    private var dithering: Int = 0xFFFF

    fun dither(pattern: Int) {
        dithering = pattern and 0xFFFF
    }

    fun pal() {
        switch = Array(gamePalette.size) { index -> index }
    }

    fun pal(source: ColorIndex, target: ColorIndex) {
        switch[gamePalette.check(source)] = gamePalette.check(target)
    }

    fun mix(colors: Array<ColorIndex>, x: Pixel, y: Pixel): Array<ColorIndex>? {
        fun dither(pattern: Int): Boolean {
            val a = x % 4
            val b = (y % 4) * 4

            return (pattern shr (15 - (a + b))) and 0x01 == 0x01
        }

        val color = gamePalette.check(colors[0])
        colors[0] = switch[gamePalette.check(color)]
        // Return null if transparent
        if (gamePalette.isTransparent(colors[0])) return null
        return if (!dither(dithering)) {
            null
        } else {
            colors
        }
    }
}
class FrameBuffer(
    val width: Pixel,
    val height: Pixel,
    val gamePalette: ColorPalette
) {

    internal val colorIndexBuffer: PixelArray = PixelArray(width, height, PixelFormat.INDEX)

    internal var buffer: ByteArray = ByteArray(height * width * PixelFormat.RGBA)

    internal var gifBuffer: IntArray = IntArray(0)

    internal val clipper: Clipper = Clipper(width, height)

    internal val blender = Blender(gamePalette)

    private var tmp = Array<Int>(1) { 0 }

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        return colorIndexBuffer.getOne(x, y)
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        if (!clipper.isIn(x, y)) return

        tmp[0] = gamePalette.check(colorIndex)
        val index = blender.mix(tmp, x, y) ?: return
        colorIndexBuffer.set(x, y, index[0])
    }

    fun clear(clearIndx: Int) {
        val clearIndex = gamePalette.check(clearIndx)
        colorIndexBuffer.reset(clearIndex)
    }

    fun copyFrom(
        source: PixelArray,
        dstX: Pixel = 0,
        dstY: Pixel = 0,
        sourceX: Pixel = 0,
        sourceY: Pixel = 0,
        width: Pixel = this.width,
        height: Pixel = this.height,
        reverseX: Boolean = false,
        reverseY: Boolean = false,
        blender: (Array<Int>, Pixel, Pixel) -> Array<Int> = { colors, _, _ -> colors }
    ) {

        val clippedX = max(dstX, clipper.left)
        val clippedWidth = min(dstX + width, clipper.right) - dstX

        val clippedY = max(dstY, clipper.top)
        val clippedHeight = min(dstY + height, clipper.bottom) - dstY

        colorIndexBuffer.copyFrom(
            source,
            clippedX,
            clippedY,
            sourceX + clippedX - dstX,
            sourceY + clippedY - dstY,
            clippedWidth,
            clippedHeight,
            reverseX, reverseY
        ) { c, x, y -> this.blender.mix(blender(c, x, y), x, y) }
    }

    /**
     * Create a buffer using the colorIndexBuffer as reference.
     */
    fun generateBuffer(): ByteArray {
        // Reset the old buffer
        gifBuffer = IntArray(height * width)

        var pos = 0
        for (x in 0 until width) {
            for (y in 0 until height) {
                val index = colorIndexBuffer.getOne(x, y)
                val color = gamePalette.getRGBA(index)

                buffer[pos++] = color[0]
                buffer[pos++] = color[1]
                buffer[pos++] = color[2]
                buffer[pos++] = color[3]

                gifBuffer[x + y * width] = gamePalette.getRGAasInt(index)
            }
        }
        return buffer
    }
}
