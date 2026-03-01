package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.command.utils.FontAnalyzer
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class AddCommand : CliktCommand(name = "add") {
    val gameDirectory by option("-d", "--directory", help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val isFont by option("--font", help = "Mark .png resources as fonts instead of spritesheets.")
        .flag()

    val size by option("--size", help = "Character size in pixels (e.g., '8x12' or '8' for square). Requires --font.")

    val chars by option("--chars", help = "Characters in the font, in reading order (left-to-right, top-to-bottom). Requires --font.")

    val resources by argument(help = "The resource to add to the game. The kind of resource will be deducted from the file extension.")
        .multiple(required = true)

    override fun help(context: Context) = "Add a resource to your game."

    override fun run() {
        val tiny = gameDirectory.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }

        if (isFont && (size == null || chars == null)) {
            echo("❌ --font requires both --size and --chars options.")
            throw Abort()
        }

        if (!isFont && (size != null || chars != null)) {
            echo("⚠️  --size and --chars are only used with --font. They will be ignored.")
        }

        // Open the _tiny.json
        var gameParameters = GameParameters.read(tiny)

        // regarding the input, add it into the right resource
        resources.forEach { r ->
            val type =
                if (r.endsWith("png") && isFont) {
                    addFont(r, gameParameters).let { gameParameters = it }
                    "font"
                } else if (r.endsWith("png")) {
                    // Add spritesheet
                    gameParameters = gameParameters.addSpritesheet(r)
                    "spritesheet"
                } else if (r.endsWith("lua")) {
                    // Add script
                    gameParameters = gameParameters.addScript(r)
                    "script"
                } else if (r.endsWith("sfx")) {
                    // Add midi
                    gameParameters = gameParameters.addSound(r)
                    "sound"
                } else if (r.endsWith("ldtk")) {
                    // Add level
                    gameParameters = gameParameters.addLevel(r)
                    "level"
                } else {
                    null
                }
            if (type != null) {
                echo("➕ $r added into your game as $type!")
            } else {
                echo(
                    "❌ $r NOT added as the type of the resource is unknown. " +
                        "Please check the path of the resource and if it's supported by tiny.",
                )
            }
        }

        // Save the updated _tiny.json
        gameParameters.write(tiny)
    }

    private fun addFont(
        resource: String,
        params: GameParameters,
    ): GameParameters {
        val (charWidth, charHeight) = FontAnalyzer.parseSize(size!!)
        val (imageWidth, _) = FontAnalyzer.readImageDimensions(gameDirectory, resource)
        val fontName = FontAnalyzer.deriveFontName(resource)
        val rows = FontAnalyzer.splitCharsIntoRows(imageWidth, charWidth, chars!!)
        val charsPerRow = imageWidth / charWidth

        val font = FontAnalyzer.buildFontConfig(
            fontName = fontName,
            spritesheet = resource,
            charWidth = charWidth,
            charHeight = charHeight,
            characters = rows,
        )

        echo("   📐 Character size: ${charWidth}x$charHeight")
        echo("   📏 Grid: $charsPerRow chars/row, ${rows.size} rows, ${chars!!.length} total chars")

        return params.addFont(font)
    }
}
