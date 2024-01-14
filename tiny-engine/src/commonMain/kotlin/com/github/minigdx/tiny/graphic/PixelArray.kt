package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.Assert.assert
import kotlin.math.max
import kotlin.math.min

class PixelArray(val width: Pixel, val height: Pixel, val pixelFormat: Int = PixelFormat.INDEX) {

    internal var pixels = Array(width * height * pixelFormat) { 0 }

    val size = width * height * pixelFormat

    private val tmp = Array(pixelFormat) { 0 }

    fun copyFrom(array: PixelArray) {
        pixels = array.pixels.copyOf()
    }

    fun reset(pixel: Int, left: Int = 0, top: Int = 0, right: Int = width, bottom: Int = height) {
        val cleft = 0 - left
        val cright = right + 2 * cleft

        val ctop = 0 - top
        val cbottom = bottom + 2 * ctop

        pixels = Array(width * height * pixelFormat) { index ->
            val p = index / pixelFormat
            val x = p % width
            val y = p / width

            if (x in cleft..cright && y in ctop..cbottom) {
                pixel
            } else {
                0
            }
        }
    }

    fun set(x: Pixel, y: Pixel, vararg pixel: Int) {
        assert(x in 0 until width) { "x ($x) has to be between 0 and $width (excluded)" }
        assert(y in 0 until height) { "y ($y) has to be between 0 and $height (excluded)" }
        assert(pixel.size == pixelFormat) { "the assigned pixel needs to conform the pixel format ($pixelFormat)" }

        val correctedX = min(max(0, x), width - 1)
        val correctedY = min(max(0, y), height - 1)

        val position = (correctedX + correctedY * width) * pixelFormat
        pixel.forEachIndexed { index, value ->
            pixels[position + index] = value
        }
    }

    fun get(x: Pixel, y: Pixel): Array<Int> {
        assert(x in 0 until width) { "x ($x) has to be between 0 and $width (excluded)" }
        assert(y in 0 until height) { "y ($y) has to be between 0 and $height (excluded)" }
        val position = (x + y * width) * pixelFormat
        (0 until pixelFormat).forEach { index ->
            tmp[index] = pixels[position + index]
        }
        return tmp
    }

    /**
     * Return the value at the coordinate x/y.
     * The pixel format should be equals to 1 otherwise
     * it will returns only the first component of the color.
     */
    fun getOne(x: Pixel, y: Pixel): Int = get(x, y)[0]

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
        blender: (Array<Int>, Pixel, Pixel) -> Array<Int>?,
    ) {
        assert(source.pixelFormat == pixelFormat) {
            "Can't copy PixelArray because the pixel format is different between the two PixelArray"
        }

        val minWidth = min(width, min(width - (dstX + width - this.width), width - (sourceX + width - source.width)))
        val minHeight =
            min(height, min(height - (dstY + height - this.height), height - (sourceY + height - source.height)))

        (0 until minHeight).forEach { h ->
            val offsetY = if (reverseY) {
                minHeight - h - 1
            } else {
                h
            }

            (0 until minWidth).forEach { w ->
                val dstPosition = (w + dstX + (h + dstY) * this.width) * pixelFormat

                val offsetX = if (reverseX) {
                    minWidth - w - 1
                } else {
                    w
                }

                val sourcePosition = (offsetX + sourceX + (offsetY + sourceY) * source.width) * pixelFormat

                (0 until pixelFormat).forEach { index ->
                    tmp[index] = source.pixels[sourcePosition + index]
                }

                val blended = blender(tmp, dstX + w, dstY + h)

                if (blended != null) {
                    (0 until pixelFormat).forEach { index ->
                        this.pixels[dstPosition + index] = tmp[index]
                    }
                }
            }
        }
    }

    operator fun iterator(): Iterator<Int> = pixels.iterator()

    fun fill(startX: Int, endX: Int, y: Int, value: Int) {
        val yy = (y * width * pixelFormat)
        pixels.fill(value, yy + startX * pixelFormat, yy + endX * pixelFormat)
    }

    override fun toString(): String {
        val lineSize = width * pixelFormat
        var current = 0
        var currentLine = 0
        val result = StringBuilder(size + height)
        while (current < size) {
            result.append(pixels[current])
            current++
            currentLine++
            if (currentLine >= lineSize) {
                currentLine = 0
                result.append("\n")
            }
        }
        return result.toString()
    }
}
