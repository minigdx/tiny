package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds

interface GameLoop {
    /**
     * The code that is executed to advance the game.
     *
     * The delta is passed so the game engine can make the game run at a fixed speed.
     */
    suspend fun advance(delta: Seconds)

    /**
     * Draw the frame on the screen
     */
    fun draw()

    /**
     * End of the game loop. Time to stop the game
     */
    fun end() = Unit
}
