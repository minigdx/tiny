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
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.dom.createElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.url.URLSearchParams
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun main() {
    val rootPath = getRootPath()

    val elts = document.getElementsByTagName("tiny-editor")

    val url = URLSearchParams(window.location.search)
    val savedCode = url.get("game")
    val decodedCode = if (savedCode?.isNotBlank() == true) {
        Base64.decode(savedCode.encodeToByteArray()).decodeToString()
    } else {
        null
    }

    elts.forEachIndexed { index, game ->
        val code = game.textContent ?: ""
        val spritePath = game.getAttribute("sprite")
        val levelPath = game.getAttribute("level")

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

        val codeToUse = decodedCode ?: "-- Update the code to update the game!\n$code"

        var clicked = false
        link.textContent = "\uD83D\uDC7E ▶ Run and tweak an example"
        link.onclick = { _ ->
            if (!clicked) {
                createGame(playLink, index, codeToUse, spritePath, levelPath, rootPath)
                clicked = true
            }
            true
        }

        // There is a user code. Let's unfold the game.
        if (savedCode != null) {
            createGame(playLink, index, codeToUse, spritePath, levelPath, rootPath)
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun createGame(
    container: Element,
    index: Int,
    code: String,
    spritePath: String?,
    levelPath: String?,
    rootPath: String,
) {
    val canvas = document.createElement("canvas").apply {
        setAttribute("width", "512")
        setAttribute("height", "512")
        setAttribute("class", "tiny-canvas")
        setAttribute("tabindex", "1")
    }
    container.appendChild(canvas)

    val textarea = (document.createElement("textarea") as HTMLTextAreaElement).apply {
        setAttribute("id", "editor-$index")
        setAttribute("spellcheck", "false")
        setAttribute("class", "tiny-textarea")

        innerHTML = code
    }
    container.appendChild(textarea)

    val link = (document.createElement("a") as HTMLAnchorElement).apply {
        val b64 = Base64.encode(code.encodeToByteArray())
        id = "share-$index"
        href = "sandbox.html?game=$b64"
        textContent = "\uD83D\uDD17 Share this game!"
    }

    container.after(link)

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
            "#CC1424",
        ),
        gameScripts = listOf("#editor-$index"),
        spriteSheets = spritePath?.let { listOf(it) } ?: emptyList(),
        gameLevels = levelPath?.let { listOf(it) } ?: emptyList(),
        zoom = 2,
        gutter = 0 to 0,
        spriteSize = 16 to 16,
    )

    GameEngine(
        gameOptions = gameOptions,
        platform = EditorWebGlPlatform(WebGlPlatform(canvas as HTMLCanvasElement, logger, gameOptions, rootPath)),
        vfs = CommonVirtualFileSystem(),
        logger = logger,
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
    override fun createSoundStream(name: String): SourceStream<SoundData> = delegate.createSoundStream(name)
}
