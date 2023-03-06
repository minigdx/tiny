package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.GameEngine
import com.github.minigdx.tiny.GameOption
import com.github.minigdx.tiny.engine.GameLoop

interface Platform {
    /**
     * Create the window where the game will render
     */
    fun initWindow(gameOption: GameOption)

    /**
     * Prepare the platform for the game loop
     */
    fun initGameLoop()

    /**
     * Let's run the game loop
     */
    fun gameLoop(gameLoop: GameLoop)

    /**
     * The game loop stopped.
     * Game is existing.
     */
    fun endGameLoop()
}
