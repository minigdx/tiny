package com.github.minigdx.tiny.platform.android

import android.content.Context
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.RenderContext

class AndroidWindowManager(
    private val context: Context,
    private val gameOptions: GameOptions,
) : WindowManager {
    private var tinyView: TinyView? = null
    private val logger = Logger("AndroidWindowManager")

    fun setGameLoop(gameLoop: GameLoop) {
        // This will be set when the view is created
    }

    fun getTinyView(
        renderContext: RenderContext,
        gameLoop: GameLoop,
        inputHandler: AndroidInputHandler,
    ): TinyView {
        if (tinyView == null) {
            tinyView = TinyView(context, gameOptions, logger).apply {
                setGameLoop(gameLoop, renderContext)
                setInputHandler(inputHandler)
            }
        }
        return tinyView!!
    }

    override fun init() {
        // Initialization is done when the view is requested
    }

    override fun setFullScreen(fullscreen: Boolean) {
        // Fullscreen is typically handled by the Activity
        logger.info { "Fullscreen mode: $fullscreen (handled by Activity)" }
    }

    override fun setScreenSize(
        width: Pixel,
        height: Pixel,
    ) {
        // Screen size is determined by the Android device
        logger.info { "Screen size request: ${width}x$height (handled by Android)" }
    }

    override fun setBorderless(borderless: Boolean) {
        // Borderless is typically handled by the Activity theme
        logger.info { "Borderless mode: $borderless (handled by Activity theme)" }
    }

    override fun toggleFrameRate() {
        // Frame rate display would need to be implemented in the renderer
        logger.info { "Toggle frame rate display not yet implemented" }
    }

    override fun updateFrameRate(delta: Float) {
        // Update frame rate if displaying
    }

    override fun isFrameRateVisible(): Boolean = false

    override fun width(): Int {
        return tinyView?.width ?: gameOptions.width
    }

    override fun height(): Int {
        return tinyView?.height ?: gameOptions.height
    }

    override fun gameWidth(): Int = gameOptions.width

    override fun gameHeight(): Int = gameOptions.height

    override fun frameBufferWidth(): Int = gameOptions.width * gameOptions.zoom

    override fun frameBufferHeight(): Int = gameOptions.height * gameOptions.zoom

    override fun aspectRatio(): Percent {
        val screenWidth = width().toFloat()
        val screenHeight = height().toFloat()
        val gameWidth = gameWidth().toFloat()
        val gameHeight = gameHeight().toFloat()

        val screenRatio = screenWidth / screenHeight
        val gameRatio = gameWidth / gameHeight

        return if (screenRatio > gameRatio) {
            // Screen is wider than game
            screenHeight / gameHeight
        } else {
            // Screen is taller than game
            screenWidth / gameWidth
        }
    }

    override fun giveBackFocus() {
        // Not applicable on Android
    }

    override fun pollEvents() {
        // Event polling is handled by the Android system
    }

    override fun isClosing(): Boolean = false

    override fun swapBuffers() {
        // Buffer swapping is handled by GLSurfaceView
    }

    fun destroy() {
        tinyView?.destroy()
        tinyView = null
    }
}
