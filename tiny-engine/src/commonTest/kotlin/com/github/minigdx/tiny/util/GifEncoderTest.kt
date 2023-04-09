package com.github.minigdx.tiny.util

import com.github.minigdx.tiny.file.TargetStream
import com.github.minigdx.tiny.graphic.ColorPalette
import kotlin.test.Test
import kotlin.test.assertEquals

class GifEncoderTest {

    private val encoder = GifEncoder(3, 5, ColorPalette((0 until 255).map { "#0000" + it.toString(16).uppercase().padStart(2, '0') }))

    class ByteArrayStream : TargetStream<ByteArray> {
        var output: ByteArray = byteArrayOf()
            private set
        override fun write(data: ByteArray) {
            output += data
        }
    }

    @Test
    fun writeHeader_it_write_the_GIF_header() {
        val output = ByteArrayStream()

        encoder.writeHeader(output)

        assertEquals("GIF89a".encodeToByteArray(), output.output)
    }

    @Test
    fun writeHeader_it_write_the_Logical_screen_header() {
        val output = ByteArrayStream()

        encoder.writeLogicalScreen(output)

        assertEquals(byteArrayOf(0x03, 0x00, 0x05, 0x00), output.output)
    }

    @Test
    fun writeGlobalColorTableHeader_it_write_the_GlobalColorTable_header() {
        val output = ByteArrayStream()

        encoder.writeGlobalColorTableHeader(output)

        assertEquals(byteArrayOf(0x98.toByte()), output.output)
    }

    private fun assertEquals(expected: ByteArray, actual: ByteArray) {
        expected.indices.forEach { index ->
            assertEquals(
                expected[index],
                actual[index],
                "Byte (${expected[index].toHex()}) " +
                    "at the index $index is different (${actual[index].toHex()})"
            )
        }
    }

    private fun Byte.toHex() = "0x" + this.toString(16).uppercase().padStart(2, '0')
}
