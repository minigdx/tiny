package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.graphic.FrameBuffer
import com.squareup.gifencoder.FastGifEncoder
import com.squareup.gifencoder.ImageOptions
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

actual fun toGif(name: String, animation: List<FrameBuffer>) {
    val options = ImageOptions().apply {
        this.setDelay(20, TimeUnit.MILLISECONDS)
    }
    FileOutputStream("build/test-results/jvmTest/$name.gif").use { out ->
        val reference = animation.first()
        val encoder = FastGifEncoder(
            out,
            reference.width,
            reference.height,
            0,
            reference.gamePalette
        )

        animation.forEach { img ->
            encoder.addImage(img.gifBuffer, reference.width, options)
        }
        encoder.finishEncoding()
    }
}
