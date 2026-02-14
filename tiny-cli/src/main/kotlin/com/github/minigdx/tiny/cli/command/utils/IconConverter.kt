package com.github.minigdx.tiny.cli.command.utils

import java.io.ByteArrayOutputStream
import java.io.File

object IconConverter {

    /**
     * Convert a PNG icon file to Windows ICO format.
     *
     * Uses the ICO format with an embedded PNG image, which is supported
     * by Windows Vista and later (ICO with PNG payload).
     */
    fun convertToIco(pngFile: File, outputFile: File): File? {
        return try {
            val pngBytes = pngFile.readBytes()
            val image = javax.imageio.ImageIO.read(pngFile)
            val width = image.width
            val height = image.height

            val ico = ByteArrayOutputStream()

            // ICONDIR header (6 bytes)
            ico.write(shortToLE(0))  // Reserved
            ico.write(shortToLE(1))  // Type: 1 = ICO
            ico.write(shortToLE(1))  // Image count: 1

            // ICONDIRENTRY (16 bytes)
            ico.write(if (width >= 256) 0 else width)   // Width (0 means 256)
            ico.write(if (height >= 256) 0 else height)  // Height (0 means 256)
            ico.write(0)              // Color palette count
            ico.write(0)              // Reserved
            ico.write(shortToLE(1))   // Color planes
            ico.write(shortToLE(32))  // Bits per pixel
            ico.write(intToLE(pngBytes.size))  // Size of PNG data
            ico.write(intToLE(22))    // Offset to PNG data (6 + 16)

            // PNG image data
            ico.write(pngBytes)

            outputFile.writeBytes(ico.toByteArray())
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert a PNG icon file to macOS ICNS format using the `sips` tool.
     *
     * Only available on macOS where `sips` is a built-in system tool.
     */
    fun convertToIcns(pngFile: File, outputFile: File): File? {
        return try {
            val process = ProcessBuilder(
                "sips", "-s", "format", "icns",
                pngFile.absolutePath,
                "--out", outputFile.absolutePath,
            ).start()
            val exitCode = process.waitFor()
            if (exitCode == 0) outputFile else null
        } catch (e: Exception) {
            null
        }
    }

    private fun shortToLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        )
    }

    private fun intToLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        )
    }
}
