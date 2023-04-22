package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
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
        /**
         * Source to copy.
         */
        source: PixelArray,
        /**
         * X coordinate where the data will be copied in this frame buffer
         */
        dstX: Pixel = 0,
        /**
         * Y coordinate where the data will be copied in this frame buffer
         */
        dstY: Pixel = 0,
        /**
         * X coordinate where the data will be consumed in the source.
         */
        sourceX: Pixel = 0,
        /**
         * Y coordinate where the data will be consumed in the source.
         */
        sourceY: Pixel = 0,
        /**
         * Width of the fragment to copy
         */
        width: Pixel = this.width,
        /**
         * Height of the fragment to copy
         */
        height: Pixel = this.height,
        /**
         * Flip horizontally
         */
        reverseX: Boolean = false,
        /**
         * Flip vertically
         */
        reverseY: Boolean = false,
        /**
         * Blend function
         */
        blender: (Array<Int>, Pixel, Pixel) -> Array<Int> = { colors, _, _ -> colors }
    ) {

        val clippedX = max(dstX, clipper.left)
        val clippedWidthLeft = width - (clippedX - dstX)
        val clippedWidth = min(dstX + clippedWidthLeft, clipper.right) - dstX

        val clippedY = max(dstY, clipper.top)
        val clippedHeightTop = height - (clippedY - dstY)
        val clippedHeight = min(dstY + clippedHeightTop, clipper.bottom) - dstY

        colorIndexBuffer.copyFrom(
            source,
            clippedX,
            clippedY,
            sourceX + width - clippedWidthLeft,
            sourceY + height - clippedHeightTop,
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
