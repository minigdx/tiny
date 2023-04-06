package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.graphic.ColorPalette
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GameOption(
    val width: Pixel,
    val height: Pixel,
    val palette: List<String>,
    val zoom: Int = 2,
    val record: Seconds = 8f,
    val gutter: Pair<Pixel, Pixel> = 10 to 10,
    val spriteSize: Pair<Pixel, Pixel> = 8 to 8
) {

    fun colors(): ColorPalette {
        return ColorPalette(palette)
    }
}
