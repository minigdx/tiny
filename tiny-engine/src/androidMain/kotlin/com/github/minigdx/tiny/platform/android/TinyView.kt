package com.github.minigdx.tiny.platform.android

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.render.RenderContext
import kotlinx.coroutines.runBlocking
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TinyView(
    context: Context,
    private val gameOptions: GameOptions,
    private val logger: Logger,
) : GLSurfaceView(context) {
    private var renderer: TinyRenderer? = null
    private var inputHandler: AndroidInputHandler? = null

    init {
        // Use OpenGL ES 3.0
        setEGLContextClientVersion(3)

        // Set RGBA_8888 format
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        // Set renderer
        renderer = TinyRenderer(gameOptions, logger)
        setRenderer(renderer)

        // Render only when requested or continuously based on game options
        renderMode = RENDERMODE_CONTINUOUSLY

        // Enable touch events
        isFocusableInTouchMode = true
        requestFocus()
    }

    fun setGameLoop(
        gameLoop: GameLoop,
        renderContext: RenderContext,
    ) {
        renderer?.setGameLoop(gameLoop, renderContext)
    }

    fun setInputHandler(handler: AndroidInputHandler) {
        inputHandler = handler
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return inputHandler?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return inputHandler?.onKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return inputHandler?.onKeyUp(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        renderer?.pause()
    }

    override fun onResume() {
        super.onResume()
        renderer?.resume()
    }

    fun destroy() {
        renderer?.destroy()
    }
}

class TinyRenderer(
    private val gameOptions: GameOptions,
    private val logger: Logger,
) : GLSurfaceView.Renderer {
    private var gameLoop: GameLoop? = null
    private var renderContext: RenderContext? = null
    private var width: Int = 0
    private var height: Int = 0
    private var isPaused = false
    private var lastTime = System.nanoTime()

    fun setGameLoop(
        loop: GameLoop,
        context: RenderContext,
    ) {
        gameLoop = loop
        renderContext = context
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?,
    ) {
        // OpenGL ES initialization is handled by the RenderContext
        logger.debug(TAG) { "Surface created" }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int,
    ) {
        this.width = width
        this.height = height

        // Update viewport
        // renderContext?.updateViewport(0, 0, width, height)

        logger.debug(TAG) { "Surface changed: ${width}x$height" }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (isPaused) return

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastTime) / 1_000_000_000f // Convert to seconds
        lastTime = currentTime

        // Clamp delta time to prevent large jumps
        val clampedDelta = deltaTime.coerceAtMost(1f / 30f)

        runBlocking {
            // Advance the game loop
            gameLoop?.advance(clampedDelta)
        }

        // The actual rendering is handled by the Platform.draw() method
        // which is called from within the game loop
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
        lastTime = System.nanoTime()
    }

    fun destroy() {
        // Clean up resources if needed
    }

    companion object {
        private const val TAG = "\uD83E\uDDF8 TINY"
    }
}
