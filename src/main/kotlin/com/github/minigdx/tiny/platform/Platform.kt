package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.graphic.FrameBuffer

interface Platform {
    /**
     * Game Option from the game.
     */
    val gameOption: GameOption
    /**
     * Create the window where the game will render
     */
    fun initWindowManager()

    /**
     * Prepare the platform for the game loop
     */
    fun initRenderManager(): RenderContext

    /**
     * Let's run the game loop
     */
    fun gameLoop(gameLoop: GameLoop)

    /**
     * Draw the image on the screen
     */
    fun draw(context: RenderContext, frameBuffer: FrameBuffer)

    /**
     * Save the last 30 seconds of the game.
     */
    fun record()

    /**
     * The game loop stopped.
     * Game is existing.
     */
    fun endGameLoop()
}
