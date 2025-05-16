package com.github.minigdx.tiny.graphic

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.util.Assert.assert
import kotlin.math.max
import kotlin.math.min

class PixelArray(val width: Pixel, val height: Pixel, val pixelFormat: Int = PixelFormat.INDEX) {
    internal var pixels = ByteArray(width * height * pixelFormat) { 0 }

    val size = width * height * pixelFormat

    private val tmp = ByteArray(pixelFormat) { 0 }

    fun copyFrom(array: PixelArray) {
        pixels = array.pixels.copyOf()
    }

    fun reset(pixel: Int) {
        pixels.fill(pixel.toByte())
    }

    fun set(
        x: Pixel,
        y: Pixel,
        vararg pixel: Int,
    ) {
        assert(x in 0 until width) { "x ($x) has to be between 0 and $width (excluded)" }
        assert(y in 0 until height) { "y ($y) has to be between 0 and $height (excluded)" }
        assert(pixel.size == pixelFormat) { "the assigned pixel needs to conform the pixel format ($pixelFormat)" }

        val correctedX = min(max(0, x), width - 1)
        val correctedY = min(max(0, y), height - 1)

        val position = (correctedX + correctedY * width) * pixelFormat
        pixel.forEachIndexed { index, value ->
            pixels[position + index] = value.toByte()
        }
    }

    fun get(
        x: Pixel,
        y: Pixel,
    ): ByteArray {
        assert(x >= 0 && x < width) { "x ($x) has to be between 0 and $width (excluded)" }
        assert(y >= 0 && y < height) { "y ($y) has to be between 0 and $height (excluded)" }
        val position = (x + y * width) * pixelFormat
        when (pixelFormat) {
            PixelFormat.RGBA -> {
                tmp[0] = pixels[position]
                tmp[1] = pixels[position + 1]
                tmp[2] = pixels[position + 2]
                tmp[3] = pixels[position + 3]
            }

            PixelFormat.RGB -> {
                tmp[0] = pixels[position]
                tmp[1] = pixels[position + 1]
                tmp[2] = pixels[position + 2]
            }

            PixelFormat.INDEX -> {
                tmp[0] = pixels[position]
            }
        }
        return tmp
    }

    /**
     * Return the value at the coordinate x/y.
     * The pixel format should be equals to 1 otherwise
     * it will returns only the first component of the color.
     */
    fun getOne(
        x: Pixel,
        y: Pixel,
    ): Int = get(x, y)[0].toInt()

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
        blender: (ByteArray, Pixel, Pixel) -> ByteArray?,
    ) {
        assert(source.pixelFormat == pixelFormat) {
            "Can't copy PixelArray because the pixel format is different between the two PixelArray"
        }

        val minWidth = min(width, min(width - (dstX + width - this.width), width - (sourceX + width - source.width)))
        val minHeight =
            min(height, min(height - (dstY + height - this.height), height - (sourceY + height - source.height)))

        for (h in 0 until minHeight) {
            val offsetY =
                if (reverseY) {
                    minHeight - h - 1
                } else {
                    h
                }

            for (w in 0 until minWidth) {
                val dstPosition = (w + dstX + (h + dstY) * this.width) * pixelFormat

                val offsetX =
                    if (reverseX) {
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

    fun fill(
        startX: Int,
        endX: Int,
        y: Int,
        value: Int,
    ) {
        fill(startX, endX, y, value.toByte())
    }

    fun fill(
        startX: Int,
        endX: Int,
        y: Int,
        value: Byte,
    ) {
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
