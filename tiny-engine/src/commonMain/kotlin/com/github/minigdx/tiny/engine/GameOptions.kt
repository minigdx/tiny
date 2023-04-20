package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.input.MouseProject
import com.github.minigdx.tiny.input.Vector2

class GameOptions(
    val width: Pixel,
    val height: Pixel,
    val palette: List<String>,
    val gameScripts: List<String>,
    val spriteSheets: List<String>,
    val gameLevels: List<String> = emptyList(),
    val sounds: List<String> = emptyList(),
    val zoom: Int = 2,
    val record: Seconds = 8f,
    val gutter: Pair<Pixel, Pixel> = 10 to 10,
    val spriteSize: Pair<Pixel, Pixel> = 8 to 8,

) : MouseProject {

    fun colors(): ColorPalette {
        return ColorPalette(palette)
    }

    override fun project(x: Float, y: Float): Vector2? {
        val left = gutter.first * zoom
        val right = (gutter.first + width) * zoom

        val top = gutter.second * zoom
        val bottom = (gutter.second + height) * zoom

        // The mouse is out of the game screen.
        if (x.toInt() !in left..right || y.toInt() !in top..bottom) {
            return null
        }

        val xx = x / zoom - gutter.first
        val yy = y / zoom - gutter.second
        return Vector2(xx, yy)
    }
}
