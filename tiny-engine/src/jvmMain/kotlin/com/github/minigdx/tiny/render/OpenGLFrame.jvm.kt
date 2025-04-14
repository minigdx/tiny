package com.github.minigdx.tiny.render

import com.danielgergely.kgl.ByteBuffer
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelFormat
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

var count = 0

actual fun saveAsScreenshot(
    buffer: ByteBuffer,
    frame: FrameBuffer,
    width: Int,
    height: Int,
) {
    val tmp = ByteArray(PixelFormat.RGBA)

    buffer.position = 0

    if (count < 21) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until width) {
            for (y in (height - 1) downTo 0) {
                val i = x + y * width

                buffer.position = i * PixelFormat.RGBA
                buffer.get(tmp)

                val r = tmp[0].toInt() and 0xff
                val g = tmp[1].toInt() and 0xff
                val b = tmp[2].toInt() and 0xff
                val a = tmp[3].toInt() and 0xff
                val color = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, (height - 1) - y, color)
            }
        }
        val origin = File("fbo-$count.png")
        ImageIO.write(image, "png", origin)
        buffer.position = 0

        saveFramebuffer(count, frame)
    }

    count++
}

private fun saveFramebuffer(
    count: Int,
    buffer: FrameBuffer,
) {
    val width = buffer.width
    val height = buffer.height
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val colorData =
                buffer.gamePalette.getRGBA(
                    buffer.pixel(
                        x = buffer.camera.x + x,
                        y = buffer.camera.y + y,
                    ),
                )
            val r = colorData[0].toInt() and 0xff
            val g = colorData[1].toInt() and 0xff
            val b = colorData[2].toInt() and 0xff
            val a = colorData[3].toInt() and 0xff
            val color = (a shl 24) or (r shl 16) or (g shl 8) or b
            image.setRGB(x, y, color)
        }
    }

    val origin = File("framebuffer-$count.png")
    ImageIO.write(image, "png", origin)
}
