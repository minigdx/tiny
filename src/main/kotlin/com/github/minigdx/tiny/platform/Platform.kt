package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.platform.glfw.WindowManager

class ImageData(val data: ByteArray, val width: Pixel, val height: Pixel)

interface Platform {
    /**
     * Game Option from the game.
     */
    val gameOption: GameOption
    /**
     * Create the window where the game will render
     */
    fun initWindowManager(): WindowManager

    /**
     * Prepare the platform for the game loop
     */
    fun initRenderManager(windowManager: WindowManager): RenderContext

    /**
     * Let's run the game loop
     */
    fun gameLoop(gameLoop: GameLoop)

    /**
     * Draw the image on the screen
     */
    fun draw(context: RenderContext, frameBuffer: FrameBuffer)

    /**
     * Take the compressed image data (ie: PNG) and return
     * an uncompressed image data with RGBA for each pixel.
     */
    fun extractRGBA(imageData: ByteArray): ImageData

    /**
     * Save the last 30 seconds of the game.
     */
    fun record()

    /**
     * The game loop stopped.
     * Game is existing.
     */
    fun endGameLoop()

    /**
     * Initialise the input manager.
     */
    fun initInputHandler(): InputHandler
    fun initInputManager(): InputManager
}
