package com.github.minigdx.tiny

import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import kotlinx.browser.document
import kotlinx.browser.window
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

    // Remove the last "/" to a avoid double slash when the engine getting resources.
    if (rootPath.endsWith("/")) {
        rootPath.dropLast(1)
    }
    return rootPath
}

fun main() {
    val rootPath = getRootPath()
    val tinyGameTag = document.getElementsByTagName("tiny-game")
    setupGames(rootPath, tinyGameTag)
}

fun setupGames(
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

    tinyGameTag.forEachIndexed { index, game ->

        val gameId = game.getAttribute("id")!!
        val gameWidth = game.getAttribute("width")?.toInt() ?: 128
        val gameHeight = game.getAttribute("height")?.toInt() ?: 128
        val gameZoom = game.getAttribute("zoom")?.toInt() ?: 1
        val hideMouse = game.getAttribute("mouse")?.toBoolean() ?: false

        val sprWidth = game.getAttribute("spritew")?.toInt() ?: 16
        val sprHeight = game.getAttribute("spriteh")?.toInt() ?: 16

        val scripts = game.getElementsByTagName("tiny-script").map { script ->
            script.getAttribute("name")
        }.filterNotNull()

        val levels = game.getElementsByTagName("tiny-level").map { level ->
            level.getAttribute("name")
        }.filterNotNull()

        val sounds = game.getElementsByTagName("tiny-sound").map { level ->
            level.getAttribute("name")
        }.filterNotNull()

        val spritesheets = game.getElementsByTagName("tiny-spritesheet").map { spritesheet ->
            spritesheet.getAttribute("name")
        }.filterNotNull()

        val colors =
            game.getElementsByTagName("tiny-colors")[0]?.getAttribute("name")?.split(",")?.toList() ?: emptyList()
        val canvas = document.createElement("canvas")
        canvas.setAttribute("width", (gameWidth * gameZoom).toString())
        canvas.setAttribute("height", (gameHeight * gameZoom).toString())
        canvas.setAttribute("tabindex", "1")
        if (hideMouse) {
            canvas.setAttribute("style", "cursor: none;")
        }
        game.appendChild(canvas)

        val gameOptions =
            GameOptions(
                width = gameWidth,
                height = gameHeight,
                palette = colors.ifEmpty { listOf("#FFFFFF", "#000000") },
                gameScripts = scripts,
                spriteSheets = spritesheets,
                gameLevels = levels,
                sounds = sounds,
                zoom = gameZoom,
                gutter = 0 to 0,
                spriteSize = sprWidth to sprHeight,
                hideMouseCursor = hideMouse,
            )

        val logger = StdOutLogger("game-$index")
        logger.debug("TINY-JS") { "Boot the game using the URL '$rootPath'." }

        GameEngine(
            gameOptions = gameOptions,
            platform = WebGlPlatform(canvas as HTMLCanvasElement, gameOptions, gameId, rootPath),
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
