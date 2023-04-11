package com.github.minigdx.tiny

import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.webgl.WebGlPlatform
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLCollection
import org.w3c.dom.get

fun main() {
    // Look for the first canvas in current page.
    val tinyGameTag = document.getElementsByTagName("tiny-game")

    if (tinyGameTag.length == 0) {
        throw IllegalArgumentException(
            "No <tiny-game> has been found in the current page. Check that the page including your javascript game" +
                "has a least one <tiny-game> tag to render the game in."
        )
    }

    // Get the actual root path and compute the root path to let the game load the resources from
    // the correct URL.
    // This portion may need to be customized regarding the service where the game is deployed (itch.io, ...)
    var rootPath = window.location.protocol + "//" + window.location.host + window.location.pathname
    rootPath = rootPath.replace("index.html", "")

    tinyGameTag.forEach { game ->

        val gameWidth = game.getAttribute("width")?.toInt() ?: 128
        val gameHeight = game.getAttribute("height")?.toInt() ?: 128
        val gameZoom = game.getAttribute("zoom")?.toInt() ?: 1

        val scripts = game.getElementsByTagName("tiny-script").map { script ->
            script.getAttribute("name")
        }.filterNotNull()

        val canvas = document.createElement("canvas")
        canvas.setAttribute("width", (gameWidth * gameZoom).toString())
        canvas.setAttribute("height", (gameHeight * gameZoom).toString())
        game.appendChild(canvas)

        val gameOptions = GameOptions(
            width = gameWidth,
            height = gameHeight,
            palette = emptyList(),
            gameScripts = scripts,
            spriteSheets = emptyList(),
            gameLevels = emptyList(),
            zoom = gameZoom,
            gutter = 0 to 0,
            spriteSize = 16 to 16,
        )

        val logger = StdOutLogger()

        GameEngine(
            gameOptions = gameOptions,
            platform = WebGlPlatform(canvas as HTMLCanvasElement, logger, gameOptions, rootPath),
            vfs = CommonVirtualFileSystem(),
            logger = logger
        ).main()
    }
}

fun <T> HTMLCollection.map(block: (Element) -> T): List<T> {
    val result = mutableListOf<T>()
    this.forEach {  elt ->
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
