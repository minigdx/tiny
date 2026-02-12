package com.github.minigdx.tiny

import com.github.minigdx.tiny.engine.GameConfig
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.AjaxStream
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.dom.appendText
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLCollection
import org.w3c.dom.get

fun getRootPath(): String {
    // Get the actual root path and compute the root path to let the game load the resources from
    // the correct URL.
    // This portion may need to be customized regarding the service where the game is deployed (itch.io, ...)
    var rootPath = window.location.protocol + "//" + window.location.host + window.location.pathname
    rootPath = rootPath.substring(0, rootPath.lastIndexOf('/'))

    // Remove the last "/" to avoid double slash when the engine getting resources.
    if (rootPath.endsWith("/")) {
        rootPath = rootPath.dropLast(1)
    }
    return rootPath
}

fun main() {
    val rootPath = getRootPath()
    val tinyGameTag = document.getElementsByTagName("tiny-game")

    CoroutineScope(Dispatchers.Main).launch {
        setupGames(rootPath, tinyGameTag)
    }
}

suspend fun setupGames(
    rootPath: String,
    tinyGameTag: HTMLCollection,
) {
    if (tinyGameTag.length == 0) {
        throw IllegalArgumentException(
            "No <tiny-game> has been found in the current page. " +
                "Check that the page including your javascript game" +
                "has a least one <tiny-game> tag to render the game in.",
        )
    }

    if (rootPath.startsWith("file://")) {
        tinyGameTag.forEach { game ->
            val h1 = document.createElement("h1")
            h1.appendText(
                "\uD83D\uDEA8 " +
                    "You're accessing the page without a webserver (ie: file:// as URL). " +
                    "Tiny can't run without a webserver. " +
                    "Please start a webserver to serve HTML pages and access it through " +
                    "a valid URL (ie: http://localhost) \uD83D\uDEA8",
            )
            game.appendChild(h1)
        }
        throw IllegalArgumentException(
            "Tiny can't run without a webserver." +
                "Please run a webserver to serve the files so you can acess it through " +
                "http://localhost instead of file://some/path.",
        )
    }

    for (index in 0 until tinyGameTag.length) {
        val game = tinyGameTag[index] ?: continue
        val gamePath = game.getAttribute("game") ?: "."
        val gameRootPath = "$rootPath/$gamePath".trimEnd('/')

        val logger = StdOutLogger("game-$index")
        logger.debug("TINY-JS") { "Boot the game using the URL '$gameRootPath'." }

        // Fetch and parse _tiny.json
        val configUrl = "$gameRootPath/_tiny.json"
        val configBytes = AjaxStream(configUrl).read()
        val configJson = configBytes.decodeToString()
        val config = GameConfig.parse(configJson)
        val gameOptions = config.toGameOptions().copy(gutter = 0 to 0)

        val canvas = document.createElement("canvas")
        canvas.setAttribute("width", (gameOptions.width * gameOptions.zoom).toString())
        canvas.setAttribute("height", (gameOptions.height * gameOptions.zoom).toString())
        canvas.setAttribute("tabindex", "1")
        if (gameOptions.hideMouseCursor) {
            canvas.setAttribute("style", "cursor: none;")
        }
        game.appendChild(canvas)

        GameEngine(
            gameOptions = gameOptions,
            platform = WebGlPlatform(canvas as HTMLCanvasElement, gameOptions, config.id, gameRootPath),
            vfs = CommonVirtualFileSystem(),
            logger = logger,
        ).main()
    }
}

fun <T> HTMLCollection.map(block: (Element) -> T): List<T> {
    val result = mutableListOf<T>()
    this.forEach { elt ->
        result.add(block(elt))
    }
    return result
}

fun HTMLCollection.forEach(block: (Element) -> Unit) {
    (0 until this.length).forEach { index ->
        val elt = this[index]
        if (elt != null) {
            block(elt)
        }
    }
}

fun HTMLCollection.forEachIndexed(block: (index: Int, Element) -> Unit) {
    (0 until this.length).forEach { index ->
        val elt = this[index]
        if (elt != null) {
            block(index, elt)
        }
    }
}
