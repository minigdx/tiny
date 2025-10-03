import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.forEachIndexed
import com.github.minigdx.tiny.getRootPath
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.dom.createElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Node
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

        val toolbar = document.createElement("div") {
            setAttribute("class", "tiny-toolbar")
        }
        game.before(toolbar)

        val link = document.createElement("a") {
            setAttribute("id", "link-editor-$index")
            setAttribute("class", "tiny-play tiny-button")
            setAttribute("href", "#link-editor-$index")
        } as HTMLAnchorElement
        toolbar.appendChild(link)

        val playLink = document.createElement("div").apply {
            setAttribute("class", "tiny-container")
        }
        toolbar.after(playLink)

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

        val playground = (
            document.createElement("a") {
                setAttribute("class", "tiny-button tiny-button-right")
                id = "share-$index"
                textContent = "↗\uFE0F Playground"
            } as HTMLAnchorElement
        ).apply {
            val b64 = Base64.encode(code.encodeToByteArray())
            href = "playground.html?game=$b64"
            target = "_blank"
        }

        toolbar.appendChild(playground)

        // There is a user code. Let's unfold the game.
        if (savedCode != null) {
            createGame(playLink, index, codeToUse, spritePath, levelPath, rootPath)
        }
    }
}

/**
 * Get the caret position in the editor.
 *
 * @param el the element where the caret is.
 *
 * @return the position of the caret.
 * -1 if the caret is not found.
 */
fun getCaretPosition(el: Element): Int {
    val selection = window.asDynamic().getSelection() ?: return -1
    val range = selection.getRangeAt(0)
    val prefix = range.cloneRange()
    prefix.selectNodeContents(el)
    prefix.setEnd(range.endContainer, range.endOffset)

    selection.removeAllRanges()
    selection.addRange(prefix)

    val length = selection.toString().length

    selection.removeAllRanges()
    selection.addRange(range)

    return length
}

/**
 * Set the caret position in the editor.
 *
 * @param pos the position of the caret.
 * @param parent the parent node where the caret should be set.
 *
 * @return -1 if the caret has been set. Otherwise, the remaining position to set.
 *
 * The function will iterate over the child nodes of the parent.
 * If the node is a text node, it will check if the position is within the text node.
 * If it is, it will set the caret at the position and return -1.
 * Otherwise, it will subtract the length of the text node from the position and continue.
 * If the node is not a text node, it will recursively call itself with the node as the parent.
 * If the recursive call returns a negative value, it means the caret has been set, so it will return the negative value.
 * Otherwise, it will continue.
 * If the loop finishes without setting the caret, it will return the remaining position.
 *
 * This function is used to set the caret position after updating the content of the editor.
 */
fun setCaret(
    pos: Int,
    parent: Node,
): Int {
    var position = pos
    for (i in 0 until parent.childNodes.length) {
        val node = parent.childNodes.item(i)!!
        if (node.nodeType == Node.TEXT_NODE) {
            if (node.textContent!!.length >= position) {
                val range = document.createRange()
                val sel = window.asDynamic().getSelection()
                range.setStart(node, position)
                range.collapse(true)
                sel?.removeAllRanges()
                sel?.addRange(range)
                return -1
            } else {
                position -= node.textContent!!.length
            }
        } else {
            position = setCaret(position, node)
            if (position < 0) {
                return position
            }
        }
    }
    return position
}

/**
 * Highlight the code with HTML tags.
 *
 * - String: <strong class="code_string">
 * - Comment: <em class="code_comment">
 * - Keyword: <strong class="code_keyword">
 * - Number: <em class="code_number">
 */
fun highlight(content: String): String {
    return content
        // Create lines
        .split("\n").map { "<div>$it</div>" }.joinToString("\n")
        // Replace \n with <br /> to be selected correctly in the range
        .replace("<div></div>", "<div><br /></div>")
        // String
        .replace(Regex("(\".*?\")"), "<strong class=\"code_string\">$1</strong>")
        // Comment
        .replace(Regex("--(.*)"), """<em class="code_comment">--$1</em>""")
        // Keyword
        .replace(
            Regex("\\b(if|else|elif|end|while|for|in|of|continue|break|return|function|local|do)\\b"),
            """<strong class="code_keyword">$1</strong>""",
        )
        // Numbers
        .replace(Regex("\\b(\\d+)"), "<em class=\"code_number\">$1</em>")
}

fun extractText(el: Element): String {
    el.innerHTML = el.innerHTML
        .replace("\n", "")
        .replace("</div>", "</div>\n")
        .trim()

    return (el.textContent ?: "")
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
    val textarea = (document.createElement("div") as HTMLDivElement).apply {
        setAttribute("id", "editor-$index")
        setAttribute("spellcheck", "false")
        setAttribute("class", "tiny-textarea")
        setAttribute("contenteditable", "true")

        innerHTML = highlight(code)

        oninput = { event ->
            val pos = getCaretPosition(this)
            console.log("html", this.innerHTML)
            this.innerHTML = highlight(extractText(this))
            console.log("html highlight", this.innerHTML)
            setCaret(pos, this)
        }
    }
    textarea.innerHTML = highlight(textarea.innerText)
    container.appendChild(textarea)

    val canvas = document.createElement("canvas").apply {
        setAttribute("width", "512")
        setAttribute("height", "512")
        setAttribute("class", "tiny-canvas")
        setAttribute("tabindex", "1")
    }
    container.appendChild(canvas)

    val logger = StdOutLogger("tiny-editor-$index")

    val gameOptions = GameOptions(
        width = 256,
        height = 256,
        // https://lospec.com/palette-list/rgr-proto16
        palette =
            listOf(
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
        platform = EditorWebGlPlatform(
            WebGlPlatform(
                canvas as HTMLCanvasElement,
                gameOptions,
                "tiny-editor-$index",
                rootPath,
            ),
        ),
        vfs = CommonVirtualFileSystem(),
        logger = logger,
    ).main()
}

class EditorWebGlPlatform(val delegate: Platform) : Platform {
    override val gameOptions: GameOptions = delegate.gameOptions

    override val performanceMonitor: PerformanceMonitor = delegate.performanceMonitor

    override fun initWindowManager(): WindowManager = delegate.initWindowManager()

    override fun initRenderManager(windowManager: WindowManager) = delegate.initRenderManager(windowManager)

    override fun gameLoop(gameLoop: GameLoop) = delegate.gameLoop(gameLoop)

    override fun writeImage(buffer: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun endGameLoop() = delegate.endGameLoop()

    override fun initInputHandler(): InputHandler = delegate.initInputHandler()

    override fun initInputManager(): InputManager = delegate.initInputManager()

    override fun initSoundManager(inputHandler: InputHandler): SoundManager = delegate.initSoundManager(inputHandler)

    override fun io(): CoroutineDispatcher = delegate.io()

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        return if (name.startsWith("#")) {
            EditorStream(name)
        } else {
            delegate.createByteArrayStream(name)
        }
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> =
        delegate.createImageStream(
            name,
        )

    override fun createSoundStream(
        name: String,
        soundManager: SoundManager,
    ): SourceStream<SoundData> =
        delegate.createSoundStream(
            name,
            soundManager,
        )

    override fun saveIntoHome(
        name: String,
        content: String,
    ) = delegate.saveIntoHome(name, content)

    override fun getFromHome(name: String): String? = delegate.getFromHome(name)
}
