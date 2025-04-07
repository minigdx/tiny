package com.github.minigdx.tiny.input

import com.github.minigdx.tiny.Pixel

interface MouseProject {

    /**
     * Project the mouse coordinate from the drawing area into game screen coordinates.
     *
     * The (0, 0) from the drawing area is located in the top left
     * The (0, 0) from the game screen area is also located in the top left.
     *
     * The method return null when the mouse is out-of the drawing area.
     */
    fun project(x: Float, y: Float): Vector2?

    fun unproject(x: Pixel, y: Pixel): Vector2? = TODO()
}
