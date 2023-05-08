import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.forEachIndexed
import com.github.minigdx.tiny.getRootPath
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.dom.createElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLTextAreaElement

fun main() {
    val rootPath = getRootPath()

    val elts = document.getElementsByTagName("tiny-editor")
    elts.forEachIndexed { index, game ->
        val code = game.textContent ?: ""

        val link = document.createElement("a") {
            setAttribute("id", "link-editor-$index")
            setAttribute("class", "tiny-play")
            setAttribute("href", "#link-editor-$index")
        } as HTMLAnchorElement
        game.before(link)

        val playLink = document.createElement("div").apply {
            setAttribute("class", "tiny-container")
        }
        link.after(playLink)

        var clicked = false
        link.textContent = "\uD83D\uDC7E ▶ Try an example"
        link.onclick = { _ ->
            if (!clicked) {
                createGame(playLink, index, code, rootPath)
                clicked = true
            }
            true
        }
    }
}

private fun createGame(
    container: Element,
    index: Int,
    code: String,
    rootPath: String
) {
    val canvas = document.createElement("canvas").apply {
        setAttribute("width", "512")
        setAttribute("height", "512")
        setAttribute("class", "tiny-canvas")
    }
    container.appendChild(canvas)

    val textarea = (document.createElement("textarea") as HTMLTextAreaElement).apply {
        setAttribute("id", "editor-$index")
        setAttribute("spellcheck", "false")
        setAttribute("class", "tiny-textarea")
        innerHTML = "-- Update the code to update the game!\n" + code
    }
    container.appendChild(textarea)

    val logger = StdOutLogger("tiny-editor-$index")

    val gameOptions = GameOptions(
        width = 256,
        height = 256,
        // https://lospec.com/palette-list/rgr-proto16
        palette = listOf(
            "#FFF9B3",
            "#B9C5CC",
            "#4774B3",
            "#144B66",
            "#8FB347",
            "#2E994E",
            "#F29066",
            "#E65050",
            "#707D7C",
            "#293C40",
            "#170B1A",
            "#0A010D",
            "#570932",
            "#871E2E",
            "#FFBF40",
            "#CC1424"
        ),
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
}

class EditorWebGlPlatform(val delegate: Platform) : Platform {

    override val gameOptions: GameOptions = delegate.gameOptions
    override fun initWindowManager(): WindowManager = delegate.initWindowManager()

    override fun initRenderManager(windowManager: WindowManager): RenderContext =
        delegate.initRenderManager(windowManager)

    override fun gameLoop(gameLoop: GameLoop) = delegate.gameLoop(gameLoop)

    override fun draw(context: RenderContext, frameBuffer: FrameBuffer) = delegate.draw(context, frameBuffer)

    override fun endGameLoop() = delegate.endGameLoop()

    override fun initInputHandler(): InputHandler = delegate.initInputHandler()

    override fun initInputManager(): InputManager = delegate.initInputManager()
    override fun initSoundManager(inputHandler: InputHandler): SoundManager = delegate.initSoundManager(inputHandler)

    override fun io(): CoroutineDispatcher = delegate.io()

    override fun createByteArrayStream(name: String): SourceStream<ByteArray> {
        return if (name.startsWith("#")) {
            EditorStream(name)
        } else {
            delegate.createByteArrayStream(name)
        }
    }

    override fun createImageStream(name: String): SourceStream<ImageData> = delegate.createImageStream(name)
    override fun createSoundStream(name: String): SourceStream<SoundData> {
        TODO("Not yet implemented")
    }
}
