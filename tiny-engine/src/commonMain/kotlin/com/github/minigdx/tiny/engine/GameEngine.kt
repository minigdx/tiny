package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.toTinyException
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import com.github.minigdx.tiny.render.DefaultVirtualFrameBuffer
import com.github.minigdx.tiny.render.VirtualFrameBuffer
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.ResourceFactory
import com.github.minigdx.tiny.sound.DefaultSoundBoard
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue.Companion.valueOf

@OptIn(ExperimentalCoroutinesApi::class)
class GameEngine(
    val gameOptions: GameOptions,
    val platform: Platform,
    val vfs: VirtualFileSystem,
    val logger: Logger,
    val listener: GameEngineListener? = null,
) : GameLoop {
    private var currentScriptHasError = false

    private var accumulator: Seconds = 0f
    private var currentFrame: Long = 0L
    private var previousFrame: Long = 0L
    private var currentMetrics: PerformanceMetrics? = null

    lateinit var inputHandler: InputHandler
    lateinit var inputManager: InputManager
    lateinit var soundManager: SoundManager

    private lateinit var resourceFactory: ResourceFactory

    private lateinit var virtualFrameBuffer: VirtualFrameBuffer

    private val performanceMonitor = platform.performanceMonitor

    private lateinit var gameResourceProcessor: GameResourceProcessor

    fun main() {
        val windowManager = platform.initWindowManager()

        inputHandler = platform.initInputHandler()
        inputManager = platform.initInputManager()
        soundManager = platform.initSoundManager(inputHandler)

        val kgl = platform.initRenderManager(windowManager)

        virtualFrameBuffer = DefaultVirtualFrameBuffer(
            kgl,
            gameOptions,
            performanceMonitor,
        )

        virtualFrameBuffer.init(windowManager)

        val virtualSoundBoard = DefaultSoundBoard(soundManager)

        resourceFactory = ResourceFactory(
            vfs = vfs,
            platform = platform,
            inputHandler = inputHandler,
            logger = logger,
            gameOptions = gameOptions,
            virtualFrameBuffer = virtualFrameBuffer,
            virtualSoundBoard = virtualSoundBoard,
            soundManager = soundManager,
        )

        gameResourceProcessor = GameResourceProcessor(
            resourceFactory,
            gameOptions,
            platform,
            logger,
        )

        platform.gameLoop(this)
    }

    override suspend fun advance(delta: Seconds) {
        performanceMonitor.frameStart()

        try {
            gameResourceProcessor.processAvailableEvents()
        } catch (ex: TinyException) {
            popupError(ex)
        }

        val currentGameScript = gameResourceProcessor.currentScript ?: return

        if (currentGameScript.exited >= 0) {
            exitCurrentGameScript(currentGameScript)
        } else if (currentGameScript.reload) {
            soundManager.stopAll()
            reloadCurrentGameScript(currentGameScript)
        }

        // Fixed step simulation
        accumulator += delta
        if (accumulator >= REFRESH_LIMIT) {
            performanceMonitor.operationStart("game_update")
            inputManager.record()
            advanceGameScript(gameResourceProcessor.currentScript)
            advanceEngineScript()

            accumulator -= REFRESH_LIMIT
            currentFrame++

            interceptUserShortcup()
            currentMetrics?.run { storeFrameMetrics(this) }

            inputManager.reset()

            val updateTime = performanceMonitor.operationEnd("game_update")
            // End performance monitoring for game update
            logPerformanceMetrics(updateTime)
        }
    }

    /**
     * Will render the remaining operations on the screen.
     */
    override fun draw() {
        performanceMonitor.operationStart("draw")

        virtualFrameBuffer.bindTextures(gameResourceProcessor.spritesheetToBind)
        gameResourceProcessor.spritesheetToBind.clear()

        virtualFrameBuffer.draw()
        performanceMonitor.operationEnd("draw")

        // Complete frame monitoring and get metrics
        currentMetrics = performanceMonitor.frameEnd()

        if (currentFrame != previousFrame) {
            platform.newFrameRendered(virtualFrameBuffer)
            previousFrame = currentFrame
        }
    }

    private suspend fun advanceEngineScript() {
        gameResourceProcessor.engineGameScript?.advance()
    }

    private suspend fun advanceGameScript(currentScript: GameScript?) {
        currentScriptHasError = try {
            currentScript?.advance()
            false
        } catch (ex: TinyException) {
            if (!currentScriptHasError) { // display the log only once.
                popupError(ex)
            }
            true
        }
    }

    private suspend fun interceptUserShortcup() {
        // The user hit Ctrl + R(ecord)
        if (inputHandler.isCombinationPressed(Key.CTRL, Key.R)) {
            popup("recording GIF", "#00FF00")
            platform.record()
            // The user hit Ctrl + S(creenshot)
        } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.S)) {
            popup("screenshot PNG", "#00FF00")
            platform.screenshot()
            // The user hit Ctrl + P(rofile)
        } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.P)) {
            performanceMonitor.isEnabled = !performanceMonitor.isEnabled
            val message = if (performanceMonitor.isEnabled) {
                "enable the profiler (Ctrl+P to disabled)"
            } else {
                "disabled the profiler"
            }

            popup(message, "#00FF00")
        }
    }

    private suspend fun reloadCurrentGameScript(currentGameScript: GameScript) {
        clear()
        try {
            val state = currentGameScript.getState()
            currentGameScript.evaluate()
            currentGameScript.setState(state)

            listener?.reload(currentGameScript)

            currentScriptHasError = false
        } catch (ex: LuaError) {
            popupError(ex.toTinyException(currentGameScript.content.decodeToString()))
        }
    }

    private suspend fun exitCurrentGameScript(currentGameScript: GameScript) {
        val (previous, current) = gameResourceProcessor.setCurrentScript(currentGameScript.exited)

        try {
            val state = currentGameScript.getState()

            logger.debug("GAME_ENGINE") {
                "Stop ${currentGameScript.name} to switch the next game script ${current.name}"
            }
            // Reevaluate the game to flush the previous state.
            current.evaluate()
            current.setState(state)

            listener?.switchScript(previous, current)
        } catch (ex: LuaError) {
            popupError(ex.toTinyException(currentGameScript.content.decodeToString()))
        }
    }

    private suspend fun GameEngine.popupError(ex: TinyException) {
        logger.warn(
            "TINY",
        ) {
            val error = "line ${ex.lineNumber}:${ex.line} <-- the \uD83D\uDC1E is around here (${ex.message})"
            "The line ${ex.lineNumber} trigger an execution error (${ex.message}). " +
                "Please fix the script ${ex.name}!\n" + error
        }
        val msg = "error line ${ex.lineNumber}:${ex.line} (${ex.message})"
        popup(msg, "#FF0000", true)
    }

    private suspend fun popup(
        message: String,
        color: String,
        forever: Boolean = false,
    ) {
        gameResourceProcessor.engineGameScript?.invoke(
            "popup",
            valueOf(0),
            valueOf(message),
            valueOf(color),
            valueOf(forever),
        )
    }

    private suspend fun clear() {
        gameResourceProcessor.engineGameScript?.invoke("clear")
    }

    /**
     * Log performance metrics during game update
     */
    private fun logPerformanceMetrics(updateTime: Double) {
        // Only log every 60 frames to avoid spam
        if (performanceMonitor.isEnabled && (currentFrame % 60 == 0L)) {
            val averageMetrics = performanceMonitor.getAverageMetrics(60)
            if (averageMetrics != null) {
                logger.debug("PERFORMANCE") {
                    val fps = ((averageMetrics.fps * 10).toInt() / 10.0).toString().padStart(6)
                    val frameTime = ((averageMetrics.frameTime * 100).toInt() / 100.0).toString().padStart(6)
                    val updateTimeFormatted = ((updateTime * 100).toInt() / 100.0).toString().padStart(6)
                    val memory =
                        (((averageMetrics.memoryUsed / 1024.0 / 1024.0) * 10).toInt() / 10.0).toString().padStart(6)
                    val drawCalls = averageMetrics.drawCalls.toString().padStart(6)
                    val readPixels = averageMetrics.readPixels.toString().padStart(6)
                    val drawOnScreen = averageMetrics.drawOnScreen.toString().padStart(6)

                    "\n┌─────────────────┬────────┐\n" +
                        "│ FPS             │ $fps │\n" +
                        "│ Frame Time      │ ${frameTime}ms │\n" +
                        "│ Update Time     │ ${updateTimeFormatted}ms │\n" +
                        "│ Memory          │ ${memory}MB │\n" +
                        "│ Draw Calls      │ $drawCalls │\n" +
                        "│ Read Pixels     │ $readPixels │\n" +
                        "│ Draw On Screen  │ $drawOnScreen │\n" +
                        "└─────────────────┴────────┘"
                }
            }
        }
    }

    /**
     * Store frame metrics for debugging visualization
     */
    private suspend fun storeFrameMetrics(metrics: PerformanceMetrics) {
        if (!performanceMonitor.isEnabled) {
            return
        }
        // Add performance debug messages
        if (metrics.fps < 30) {
            popup("LOW FPS: ${metrics.fps}", "#FF0000")
        }

        if (metrics.memoryAllocated > 1024 * 1024) { // More than 1MB allocated
            popup("HIGH ALLOC: ${(metrics.memoryAllocated / 1024 / 1024)}MB", "#FFAA00")
        }

        // Show frame time if it's high
        if (metrics.frameTime > 16.67) { // Slower than 60 FPS
            popup("Frame: ${metrics.frameTime}ms", "#AAAA00")
        }
    }

    override fun end() {
        soundManager.destroy()
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }
}
