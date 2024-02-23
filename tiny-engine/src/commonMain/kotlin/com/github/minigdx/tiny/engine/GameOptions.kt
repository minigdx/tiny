package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.input.MouseProject
import com.github.minigdx.tiny.input.Vector2

data class GameOptions(
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
    val hideMouseCursor: Boolean = false,
    val runTests: Boolean = false,
) : MouseProject {

    init {
        require(width > 0) { "The width needs to be a positive number." }
        require(height > 0) { "The height needs to be a positive number." }
        require(palette.size < 256) { "The number of colors should be less than 256." }
        require(spriteSheets.size <= 256) { "The number of spritesheets is limited to 256." }
        require(zoom > 0) { "The zoom needs to be a positive non null value." }
    }

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
