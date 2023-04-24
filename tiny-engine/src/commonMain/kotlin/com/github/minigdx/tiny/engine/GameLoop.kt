package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds

interface GameLoop {
    /**
     * The code that is executed to advance the game.
     *
     * The delta is passed so the game engine can make the game run at a fixed speed.
     */
    fun advance(delta: Seconds)

    fun draw()

    /**
     * End of the game loop. Time to stop the game
     */
    fun end() = Unit
}
