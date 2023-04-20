package com.github.minigdx.tiny.platform.webgl

import com.danielgergely.kgl.KglJs
import com.danielgergely.kgl.WebGL2RenderingContext
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.AjaxStream
import com.github.minigdx.tiny.file.ImageDataStream
import com.github.minigdx.tiny.file.SoundDataSourceStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.GLRender
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.HTMLCanvasElement

class WebGlPlatform(
    private val canvas: HTMLCanvasElement,
    private val logger: Logger,
    override val gameOptions: GameOptions,
    val rootUrl: String,
) : Platform {

    private lateinit var render: GLRender

    private val jsInputHandler = JsInputHandler(canvas, gameOptions)

    private var then: Double = 0.0

    override fun initWindowManager(): WindowManager {
        return WindowManager(
            canvas.clientWidth,
            canvas.clientHeight,
            canvas.clientWidth,
            canvas.clientHeight
        )
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        val context = canvas.getContext("webgl2") as? WebGL2RenderingContext
            ?: throw IllegalStateException(
                "The canvas context is expected to be a webgl2 context. " +
                    "WebGL2 doesn't seems to be supported by your browser. " +
                    "Please update to a compatible browser to run the game in WebGL2."
            )
        render = GLRender(KglJs(context), logger, gameOptions)
        return render.init(windowManager)
    }

    override fun gameLoop(gameLoop: GameLoop) {
        window.requestAnimationFrame { now ->
            val nowInSeconds = now * 0.001
            val delta = nowInSeconds - then
            then = nowInSeconds

            gameLoop.advance(delta.toFloat())
            gameLoop.draw()

            gameLoop(gameLoop)
        }
    }

    override fun draw(context: RenderContext, frameBuffer: FrameBuffer) {
        val image = frameBuffer.generateBuffer()
        render.draw(context, image, frameBuffer.width, frameBuffer.height)
    }

    override fun record() = Unit

    override fun endGameLoop() = Unit

    override fun initInputHandler(): InputHandler = jsInputHandler

    override fun initInputManager(): InputManager = jsInputHandler

    override fun io(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    override fun createByteArrayStream(name: String): SourceStream<ByteArray> {
        return AjaxStream("$rootUrl/$name")
    }

    override fun createImageStream(name: String): SourceStream<ImageData> {
        return ImageDataStream("$rootUrl/$name")
    }

    private lateinit var soundManager: SoundManager

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        soundManager = PicoAudioSoundMananger()
        soundManager.initSoundManager(inputHandler)
        return soundManager
    }

    override fun createSoundStream(name: String): SourceStream<SoundData> {
        return SoundDataSourceStream(name, soundManager, createByteArrayStream(name))
    }
}
