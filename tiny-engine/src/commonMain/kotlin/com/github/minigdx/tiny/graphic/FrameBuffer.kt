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

class Camera() {
    var x = 0
        internal set
    var y = 0
        internal set

    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun cx(x: Int): Int {
        return x - this.x
    }

    fun cy(y: Int): Int {
        return y - this.y
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

    internal val camera = Camera()

    private var tmp = Array(1) { 0 }

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        val cx = camera.cx(x)
        val cy = camera.cy(y)
        return colorIndexBuffer.getOne(cx, cy)
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        val cx = camera.cx(x)
        val cy = camera.cy(y)
        if (!clipper.isIn(cx, cy)) return

        tmp[0] = gamePalette.check(colorIndex)
        val index = blender.mix(tmp, cx, cy) ?: return
        colorIndexBuffer.set(cx, cy, index[0])
    }

    fun clear(clearIndx: Int) {
        val clearIndex = gamePalette.check(clearIndx)
        colorIndexBuffer.reset(clearIndex, camera.x, camera.y, camera.x + width, camera.y + height)
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
        val cx = camera.cx(dstX)
        val cy = camera.cy(dstY)

        val clippedX = max(cx, clipper.left)
        val clippedWidthLeft = width - (clippedX - cx)
        val clippedWidth = min(cx + clippedWidthLeft, clipper.right) - cx

        val clippedY = max(cy, clipper.top)
        val clippedHeightTop = height - (clippedY - cy)
        val clippedHeight = min(cy + clippedHeightTop, clipper.bottom) - cy

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
