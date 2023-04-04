package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.PixelFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Blender(private val gamePalette: ColorPalette) {

    private val switch: MutableMap<ColorIndex, ColorIndex> = mutableMapOf()

    private var dithering: Boolean = false

    fun dither(patternIndex: Int) {
        dithering = patternIndex > 0
    }

    fun pal() {
        switch.clear()
    }

    fun pal(source: ColorIndex, target: ColorIndex) {
        switch[source] = target
    }

    fun mix(colors: Array<ColorIndex>, x: Pixel, y: Pixel): Array<ColorIndex>? {
        val color = gamePalette.check(colors[0])
        colors[0] = switch[gamePalette.check(color)] ?: color
        // Return null if transparent
        if(colors[0] == 0x00) return null
        // Return null if dithering enable every 2 pixels
        return if(dithering && (x + y) and 0x01 == 0x00) {
            null
        } else {
            colors
        }
    }

}
class FrameBuffer(val width: Pixel, val height: Pixel) {

    private val colorIndexBuffer: PixelArray = PixelArray(width, height, PixelFormat.INDEX)

    internal var buffer: ByteArray = ByteArray(height * width * PixelFormat.RGBA)

    internal var gifBuffer: IntArray = IntArray(0)

    internal val clipper: Clipper = Clipper(width, height)

    internal val blender = Blender(gamePalette)

    fun pixel(x: Pixel, y: Pixel): ColorIndex {
        return colorIndexBuffer.getOne(x, y)
    }

    fun pixel(x: Pixel, y: Pixel, colorIndex: ColorIndex) {
        val index = gamePalette.check(colorIndex)
        if (gamePalette.isTransparent(index)) return
        if(!clipper.isIn(x, y)) return
        colorIndexBuffer.set(x, y, index)
    }

    fun clear(clearIndx: Int) {
        val clearIndex = gamePalette.check(clearIndx)
        for (x in 0 until width) {
            for (y in 0 until height) {
                colorIndexBuffer.set(x, y, clearIndex)
            }
        }
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
        blender: (Array<Int>, Pixel, Pixel) -> Array<Int>? = this.blender::mix
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
            reverseX, reverseY,
            blender
        )
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

    companion object {

        val gamePalette: ColorPalette = ColorPalette(
            listOf(
                "#000000",
                "#1D2B53",
                "#7E2553",
                "#008751",
                "#AB5236",
                "#5F574F",
                "#C2C3C7",
                "#FFF1E8",
                "#FF004D",
                "#FFA300",
                "#FFEC27",
                "#00E436",
                "#29ADFF",
                "#83769C",
                "#FF77A8",
                "#FFCCAA"
            )
        )
    }
}
