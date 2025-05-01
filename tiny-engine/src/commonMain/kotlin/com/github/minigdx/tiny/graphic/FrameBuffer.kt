package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlin.math.max
import kotlin.math.min

class Blender(internal val gamePalette: ColorPalette) {
    internal var switch: Array<ColorIndex> = Array(gamePalette.size) { index -> index }

    internal var dithering: Int = 0xFFFF

    internal val hasDithering: Boolean
        get() {
            return dithering != 0xFFFF
        }

    fun dither(pattern: Int): Int {
        val prec = dithering
        dithering = pattern and 0xFFFF
        return prec
    }

    fun pal() {
        switch = Array(gamePalette.size) { index -> index }
    }

    fun pal(
        source: ColorIndex,
        target: ColorIndex,
    ) {
        switch[gamePalette.check(source)] = gamePalette.check(target)
    }

    fun mix(
        colors: ByteArray,
        x: Pixel,
        y: Pixel,
        transparency: Array<Int>?,
    ): ByteArray? {
        fun dither(pattern: Int): Boolean {
            val a = x % 4
            val b = (y % 4) * 4

            return (pattern shr (15 - (a + b))) and 0x01 == 0x01
        }

        val color = gamePalette.check(colors[0].toInt())
        colors[0] = switch[color].toByte()
        // Return null if transparent
        if (transparency == null && gamePalette.isTransparent(colors[0].toInt())) return null
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

    fun set(
        x: Int,
        y: Int,
    ) {
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
    val gamePalette: ColorPalette,
) {
    internal val colorIndexBuffer: PixelArray = PixelArray(width, height, PixelFormat.INDEX)

    internal val clipper: Clipper = Clipper(width, height)

    internal val blender = Blender(gamePalette)

    internal val camera = Camera()

    private var tmp = ByteArray(1) { 0 }

    private val transparency = arrayOf(0)

    fun pixel(
        x: Pixel,
        y: Pixel,
    ): ColorIndex {
        val cx = camera.cx(x)
        val cy = camera.cy(y)
        return colorIndexBuffer.getOne(cx, cy)
    }

    fun pixel(
        x: Pixel,
        y: Pixel,
        colorIndex: ColorIndex,
    ) {
        val cx = camera.cx(x)
        val cy = camera.cy(y)
        if (!clipper.isIn(cx, cy)) return

        tmp[0] = gamePalette.check(colorIndex).toByte()
        val index = blender.mix(tmp, cx, cy, transparency) ?: return
        colorIndexBuffer.set(cx, cy, index[0].toInt())
    }

    fun fill(
        startX: Pixel,
        endX: Pixel,
        y: Pixel,
        colorIndex: ColorIndex,
    ) {
        val cy = camera.cy(y)
        val leftX = min(startX, endX)
        val rightX = max(startX, endX)
        // fill outside the screen?
        if (cy !in clipper.top..clipper.bottom - 1) return
        val left = max(camera.cx(leftX), clipper.left)
        val right = min(camera.cx(rightX), clipper.right)

        // nothing to do because out of the screen or no pixel.
        if (left == right || left >= clipper.right || right < clipper.left) {
            return
        }
        val color = gamePalette.check(colorIndex)

        // can't optimise if there is some dithering
        if (blender.hasDithering) {
            (left..right).forEach { x ->
                pixel(x, y, colorIndex)
            }
        } else {
            tmp[0] = color.toByte()
            val targetColor = blender.mix(tmp, 0, 0, transparency) ?: return
            colorIndexBuffer.fill(left, right, cy, targetColor[0])
        }
    }

    /**
     * Clear the framebuffer and set the same color on each pixel.
     */
    fun clear(clearIndex: ColorIndex = ColorPalette.TRANSPARENT_INDEX) {
        val colorIndex = gamePalette.check(clearIndex)
        colorIndexBuffer.reset(colorIndex)
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
        blender: (ByteArray, Pixel, Pixel) -> ByteArray = { colors, _, _ -> colors },
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
            reverseX, reverseY,
        ) { c, x, y -> this.blender.mix(blender(c, x, y), x, y, null) }
    }

    /**
     * Create a buffer using the colorIndexBuffer as reference.
     */
    fun generateBuffer(): ByteArray {
        return this.colorIndexBuffer.pixels
    }

    /**
     * Fast copy another frame buffer into this frame buffer.
     * It's a raw copy of each element.
     */
    fun fastCopyFrom(frameBuffer: FrameBuffer) {
        // Copying into itself. So there is nothing to do.
        if (frameBuffer == this) {
            return
        }
        frameBuffer.colorIndexBuffer.pixels.copyInto(
            colorIndexBuffer.pixels,
            0,
            0,
            colorIndexBuffer.size,
        )
    }

    val asSpriteSheet =
        SpriteSheet(
            0,
            0,
            "framebuffer",
            type = ResourceType.GAME_SPRITESHEET,
            pixels = this.colorIndexBuffer,
            width = this.width,
            height = this.height,
            reload = false,
        )
}
