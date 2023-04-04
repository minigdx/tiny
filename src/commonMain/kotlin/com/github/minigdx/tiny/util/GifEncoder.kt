package com.github.minigdx.tiny.util

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.file.TargetStream
import com.github.minigdx.tiny.graphic.ColorPalette
import kotlin.experimental.and

class GifEncoder(
    private val width: Pixel,
    private val height: Pixel,
    private val palette: ColorPalette,
) {
    fun write(output: TargetStream<ByteArray>) {
        writeHeader(output)
        writeLogicalScreen(output)
        writeGlobalColorTableHeader(output)
        writeBackgroundColorIndex(output)
        writePixelAspectRatio(output)
    }
    internal fun writeHeader(output: TargetStream<ByteArray>) {
        output.write("GIF89a".encodeToByteArray())
    }

    internal fun writeLogicalScreen(output: TargetStream<ByteArray>) {
        output.write(toLittleEndian(width, height))
    }

    internal fun writeBackgroundColorIndex(output: TargetStream<ByteArray>) {
        output.write(byteArrayOf(0))
    }

    internal fun writePixelAspectRatio(output: TargetStream<ByteArray>) {
        output.write(byteArrayOf(0))
    }

    internal fun writeGlobalColorTableHeader(output: TargetStream<ByteArray>) {
        // As the animation will use only the colors from the palette,
        // We can define the palette as the global color table (GCT).
        val globalColorFlag = 1 shl 7
        val colorResolution = 1 shl 4
        val sort = 0 shl 3
        val gctSize = countBits(palette.size) - 1

        val flag = (globalColorFlag or colorResolution or sort or gctSize)
        output.write(byteArrayOf(flag.toByte()))
    }

    private fun toLittleEndian(vararg value: Int): ByteArray {
        val result = ByteArray(value.size * 2)
        var current = 0
        value.forEach {
            result[current++] = it.toByte()
            result[current++] = (it shl 8).toByte()
        }
        return result
    }

    private fun countBits(value: Int): Int {
        var count = 0
        var v = value
        while (v != 0) {
            count++
            v = v ushr 1
        }
        return count
    }
}
