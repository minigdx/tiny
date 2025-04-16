package com.github.minigdx.tiny.platform.webgl

import com.danielgergely.kgl.KglJs
import com.danielgergely.kgl.WebGL2RenderingContext
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.RenderOperation
import com.github.minigdx.tiny.file.AjaxStream
import com.github.minigdx.tiny.file.ImageDataStream
import com.github.minigdx.tiny.file.JsLocalFile
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SoundDataSourceStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.gl.OpenGLRender
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import kotlin.math.min

class WebGlPlatform(
    private val canvas: HTMLCanvasElement,
    private val logger: Logger,
    override val gameOptions: GameOptions,
    val rootUrl: String,
) : Platform {
    private lateinit var render: OpenGLRender

    private val jsInputHandler = JsInputHandler(canvas, gameOptions)

    private var then: Double = 0.0

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun initWindowManager(): WindowManager {
        return WindowManager(
            canvas.clientWidth,
            canvas.clientHeight,
            canvas.clientWidth,
            canvas.clientHeight,
        )
    }

    override fun initRenderManager(windowManager: WindowManager): RenderContext {
        val context =
            canvas.getContext("webgl2") as? WebGL2RenderingContext
                ?: throw IllegalStateException(
                    "The canvas context is expected to be a webgl2 context. " +
                        "WebGL2 doesn't seems to be supported by your browser. " +
                        "Please update to a compatible browser to run the game in WebGL2.",
                )
        render = OpenGLRender(KglJs(context), logger, gameOptions)
        return render.init(windowManager)
    }

    override fun gameLoop(gameLoop: GameLoop) {
        window.requestAnimationFrame { now ->
            val nowInSeconds = now * 0.001
            val delta = nowInSeconds - then
            then = nowInSeconds

            uiScope.launch {
                gameLoop.advance(min(delta.toFloat(), 1 / 60f))
            }
            gameLoop.draw()

            gameLoop(gameLoop)
        }
    }

    override fun endGameLoop() = Unit

    override fun initInputHandler(): InputHandler = jsInputHandler

    override fun initInputManager(): InputManager = jsInputHandler

    override fun io(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        return AjaxStream("$rootUrl/$name")
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> {
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

    override fun createLocalFile(name: String): LocalFile {
        return JsLocalFile(name)
    }

    override fun draw(renderContext: RenderContext) = render.drawOnScreen(renderContext)

    override fun render(
        renderContext: RenderContext,
        ops: List<RenderOperation>,
    ) {
        return render.render(renderContext, ops)
    }
}
