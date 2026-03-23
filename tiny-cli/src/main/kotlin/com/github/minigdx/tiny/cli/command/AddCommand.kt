package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.command.utils.FontAnalyzer
import com.github.minigdx.tiny.cli.command.utils.TtfConverter
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class AddCommand : CliktCommand(name = "add") {
    val gameDirectory by option("-d", "--directory", help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val fontName by option("--font", help = "Mark .png resources as fonts instead of spritesheets. Optionally specify the font name.")
        .optionalValue("")

    val size by option("--size", help = "Character size in pixels (e.g., '8x12' or '8' for square). Auto-detected if omitted.")

    val offset by option("--offset", help = "Pixel offset where the character grid begins (e.g., '8,12'). Auto-detected if omitted.")

    val boot by option("--boot", help = "Set a .lua script as the boot script instead of adding it to the scripts list.")
        .flag(default = false)

    val chars by option("--chars", help = "Characters in the font, in reading order (left-to-right, top-to-bottom). Requires --font.")

    val resources by argument(help = "The resource to add to the game. The kind of resource will be deducted from the file extension.")
        .multiple(required = true)

    override fun help(context: Context) = "Add a resource to your game."

    override fun run() {
        val tiny = gameDirectory.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }

        val hasTtf = resources.any { it.endsWith("ttf") || it.endsWith("otf") }
        if (fontName != null && chars == null && !hasTtf) {
            echo("❌ --font requires --chars option (or use a .ttf file for auto-detection).")
            throw Abort()
        }

        if (fontName == null && (size != null || chars != null || offset != null)) {
            echo("⚠️  --size, --chars, and --offset are only used with --font. They will be ignored.")
        }

        if (boot && resources.any { !it.endsWith("lua") }) {
            echo("❌ --boot can only be used with .lua scripts.")
            throw Abort()
        }

        if (boot && resources.size > 1) {
            echo("❌ --boot can only be used with a single script.")
            throw Abort()
        }

        // Open the _tiny.json
        var gameParameters = GameParameters.read(tiny)

        // regarding the input, add it into the right resource
        resources.forEach { r ->
            val type =
                if ((r.endsWith("ttf") || r.endsWith("otf")) && fontName != null) {
                    addTtfFont(r, gameParameters).let { gameParameters = it }
                    "font (from TTF)"
                } else if ((r.endsWith("ttf") || r.endsWith("otf")) && fontName == null) {
                    echo("⚠️  TTF files require --font option. Use: tiny-cli add --font $r")
                    null
                } else if (r.endsWith("png") && fontName != null) {
                    addFont(r, gameParameters).let { gameParameters = it }
                    "font"
                } else if (r.endsWith("png")) {
                    // Add spritesheet
                    gameParameters = gameParameters.addSpritesheet(r)
                    "spritesheet"
                } else if (r.endsWith("lua") && boot) {
                    // Set as boot script
                    gameParameters = gameParameters.setBootScript(r)
                    "boot script"
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

    private fun echoFontWarnings() {
        echo("   ⚠️  Tiny renders fonts as monospace: every character occupies the same cell width.")
        echo("      Proportional fonts will have uneven spacing. Prefer pixel/monospace fonts.")
        echo("   ⚠️  Semi-transparent pixels (anti-aliasing) are not supported.")
        echo("      They will be rendered as fully opaque, causing visual artifacts on glyph edges.")
    }

    private fun addTtfFont(
        resource: String,
        params: GameParameters,
    ): GameParameters {
        val ttfFile = gameDirectory.resolve(resource)
        val fontName = this.fontName!!.ifEmpty { FontAnalyzer.deriveFontName(resource) }
        val pngName = "$fontName.png"
        val pngFile = gameDirectory.resolve(pngName)

        val effectiveChars = chars ?: TtfConverter.DEFAULT_CHARS
        val targetHeight = size?.let { FontAnalyzer.parseSize(it).second }

        echo("   🔤 Converting TTF to PNG spritesheet...")

        val result = TtfConverter.convert(ttfFile, pngFile, effectiveChars, targetHeight)

        echo("   📐 Character size: ${result.cellWidth}x${result.cellHeight}")
        echo("   📏 Grid: ${result.cols} chars/row, ${result.rows} rows, ${effectiveChars.length} total chars")
        echoFontWarnings()

        val rows = FontAnalyzer.splitCharsIntoRows(
            result.cols * result.cellWidth,
            result.cellWidth,
            effectiveChars,
        )

        val font = FontAnalyzer.buildFontConfig(
            fontName = fontName,
            spritesheet = pngName,
            charWidth = result.cellWidth,
            charHeight = result.cellHeight,
            characters = rows,
            spaceWidth = result.spaceWidth,
        )

        return params.addFont(font)
    }

    private fun addFont(
        resource: String,
        params: GameParameters,
    ): GameParameters {
        val image = FontAnalyzer.readImage(gameDirectory, resource)
        val parsedSize = size?.let { FontAnalyzer.parseSize(it) }
        val parsedOffset = offset?.let { FontAnalyzer.parseOffset(it) }

        val result = FontAnalyzer.autoDetect(image, parsedSize, parsedOffset, chars!!.length)
        if (result == null) {
            echo("❌ Could not auto-detect character size. Please provide --size explicitly.")
            throw Abort()
        }

        if (result.sizeDetected) {
            echo("   🔍 Auto-detected character size: ${result.cellWidth}x${result.cellHeight}")
        }
        if (result.offsetDetected) {
            echo("   🔍 Auto-detected grid offset: ${result.offsetX},${result.offsetY}")
        }

        val fontName = this.fontName!!.ifEmpty { FontAnalyzer.deriveFontName(resource) }
        val effectiveWidth = image.width - result.offsetX
        val rows = FontAnalyzer.splitCharsIntoRows(effectiveWidth, result.cellWidth, chars!!)
        val charsPerRow = effectiveWidth / result.cellWidth

        val font = FontAnalyzer.buildFontConfig(
            fontName = fontName,
            spritesheet = resource,
            charWidth = result.cellWidth,
            charHeight = result.cellHeight,
            characters = rows,
            offsetX = result.offsetX,
            offsetY = result.offsetY,
        )

        echo("   📐 Character size: ${result.cellWidth}x${result.cellHeight}")
        echo("   📏 Grid: $charsPerRow chars/row, ${rows.size} rows, ${chars!!.length} total chars")
        if (result.offsetX != 0 || result.offsetY != 0) {
            echo("   📍 Offset: ${result.offsetX},${result.offsetY}")
        }
        echoFontWarnings()

        return params.addFont(font)
    }
}
