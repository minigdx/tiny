package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.engine.GameLoop

interface Platform {
    /**
     * Create the window where the game will render
     */
    fun initWindow(gameOption: GameOption)

    /**
     * Prepare the platform for the game loop
     */
    fun createDrawContext(): DrawContext

    /**
     * Let's run the game loop
     */
    fun gameLoop(gameLoop: GameLoop)

    /**
     * Draw the image on the screen
     */
    fun draw(context: DrawContext, image: ByteArray, width: Pixel, height: Pixel)

    /**
     * The game loop stopped.
     * Game is existing.
     */
    fun endGameLoop()
}
