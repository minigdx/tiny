package com.github.minigdx.tiny.platform.webgl

import com.danielgergely.kgl.KglJs
import com.danielgergely.kgl.WebGL2RenderingContext
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.AjaxStream
import com.github.minigdx.tiny.file.ImageDataStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.render.GLRender
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonNull.content
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

class WebGlPlatform(
    private val canvas: HTMLCanvasElement,
    private val logger: Logger,
    override val gameOption: GameOption,
    val rootUrl: String,
) : Platform {

    private lateinit var render: GLRender

    private val jsInputHandler = JsInputHandler(canvas)

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
            ?: throw IllegalStateException("The canvas context is expected to be a webgl2 context. " +
                "WebGL2 doesn't seems to be supported by your browser. " +
                "Please update to a compatible browser to run the game in WebGL2.")
        render = GLRender(KglJs(context), logger, gameOption)
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

    override fun extractRGBA(imageData: ByteArray): ImageData {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        val context = canvas.getContext("2d") as CanvasRenderingContext2D

        val img = Image()
        img.addEventListener("load", object : EventListener {
            override fun handleEvent(event: Event) {
                println("width2 = " + img.width)

            }

        })

        img.addEventListener("onload", object : EventListener {
            override fun handleEvent(event: Event) {
                println("width2 = " + img.width)

            }
        })
        img.src = URL.createObjectURL(Blob(imageData.toTypedArray(), BlobPropertyBag(type = "image/png")))


        canvas.width = 320;
        canvas.height = 320;
        context.drawImage(img, 0.0, 0.0 );
        val rawImageData = context.getImageData(0.0, 0.0, 256.0, 256.0);
        println("raw = " + rawImageData.data.get(0))
        println("raw length = " + rawImageData.data.length)
        val data = Int8Array(rawImageData.data.buffer).unsafeCast<ByteArray>()
        println("data length = " + data.size)


        println("width = " + img.width)
        println(data[0])
        return ImageData(data, 256, 256)
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
}
