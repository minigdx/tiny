import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.forEach
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.get

@JsModule("prismjs")
@JsNonModule
external val Prism: PrismClass

external class PrismClass

fun main() {
    Prism

    // This portion may need to be customized regarding the service where the game is deployed (itch.io, ...)
    var rootPath = window.location.protocol + "//" + window.location.host + window.location.pathname
    rootPath = rootPath.replace("index.html", "")

    js("codeInput.registerTemplate(\"code-input\", codeInput.templates.prism(Prism, [new codeInput.plugins.Indent()]));")

    val elts = document.getElementsByTagName("tiny-editor")
    var index = 0
    elts.forEach { game ->
        val code = game.textContent ?: ""
        // Remove the text content
        game.textContent = ""

        val editor = document.createElement("code-input").apply {
            setAttribute("class", "line-numbers")
            setAttribute("style", "resize: both; overflow: hidden; width: 100%")
            setAttribute("lang", "LUA")
            setAttribute("value", code)
            setAttribute("template", "code-input")
            setAttribute("id", "editor-$index")
        }

        val canvas = document.createElement("canvas").apply {
            setAttribute("width", "512")
            setAttribute("height", "512")
        }

        game.appendChild(canvas)
        game.appendChild(editor)

        val logger = StdOutLogger("tiny-editor-$index")

        val gameOptions = GameOptions(
            width = 256,
            height = 256,
            palette = listOf("#E0F8D0", "#88C070", "#346856", "#081820"),
            gameScripts = listOf("#editor-$index"),
            spriteSheets = emptyList(),
            gameLevels = emptyList(),
            zoom = 2,
            gutter = 0 to 0,
            spriteSize = 16 to 16,
        )

        GameEngine(
            gameOptions = gameOptions,
            platform = EditorWebGlPlatform(WebGlPlatform(canvas as HTMLCanvasElement, logger, gameOptions, rootPath)),
            vfs = CommonVirtualFileSystem(),
            logger = logger
        ).main()

        index++
    }
}

class EditorWebGlPlatform(val delegate: Platform) : Platform {

    override val gameOptions: GameOptions = delegate.gameOptions
    override fun initWindowManager(): WindowManager = delegate.initWindowManager()

    override fun initRenderManager(windowManager: WindowManager): RenderContext = delegate.initRenderManager(windowManager)

    override fun gameLoop(gameLoop: GameLoop) = delegate.gameLoop(gameLoop)

    override fun draw(context: RenderContext, frameBuffer: FrameBuffer) = delegate.draw(context, frameBuffer)

    override fun extractRGBA(imageData: ByteArray): ImageData = delegate.extractRGBA(imageData)

    override fun record() = delegate.record()

    override fun endGameLoop() = delegate.endGameLoop()

    override fun initInputHandler(): InputHandler = delegate.initInputHandler()

    override fun initInputManager(): InputManager = delegate.initInputManager()

    override fun io(): CoroutineDispatcher = delegate.io()

    override fun createByteArrayStream(name: String): SourceStream<ByteArray> {
        return if (name.startsWith("#")) {
            EditorStream(name)
        } else {
            delegate.createByteArrayStream(name)
        }
    }

    override fun createImageStream(name: String): SourceStream<ImageData> = delegate.createImageStream(name)
}
