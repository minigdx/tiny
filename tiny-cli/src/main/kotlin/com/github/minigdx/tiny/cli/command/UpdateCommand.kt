package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.minigdx.tiny.cli.command.utils.ColorUtils
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import java.io.File

class UpdateCommand : CliktCommand(name = "update") {
    private val gameDirectory by option("-d", "--directory", help = "The directory containing your game to be updated.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val newZoom by option("--zoom", help = "Set the game zoom level (1-8).")
        .int()

    private val hideMouseCursor by option("--hide-cursor", help = "Hide the system mouse cursor.")
        .flag()

    private val showMouseCursor by option("--show-cursor", help = "Show the system mouse cursor.")
        .flag()

    override fun help(context: Context) = "View and update game parameters."

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("❌ No _tiny.json found in ${gameDirectory.absolutePath}")
            throw Abort()
        }

        val gameParameters = try {
            GameParameters.read(configFile)
        } catch (e: Exception) {
            echo("❌ Error reading _tiny.json: ${e.message}")
            throw Abort()
        }

        if (gameParameters !is GameParametersV1) {
            echo("❌ Only V1 game configuration is supported.")
            throw Abort()
        }

        val hasUpdates = newZoom != null || hideMouseCursor || showMouseCursor
        if (hasUpdates) {
            applyUpdates(gameParameters, configFile)
        } else {
            displayParameters(gameParameters)
        }
    }

    private fun applyUpdates(
        params: GameParametersV1,
        configFile: File,
    ) {
        var updated = params

        newZoom?.let { zoom ->
            if (zoom !in 1..8) {
                echo("❌ Zoom must be between 1 and 8.")
                throw Abort()
            }
            updated = updated.copy(zoom = zoom)
            echo("✅ Zoom updated to $zoom")
        }

        if (hideMouseCursor) {
            updated = updated.copy(hideMouseCursor = true)
            echo("✅ Mouse cursor hidden")
        } else if (showMouseCursor) {
            updated = updated.copy(hideMouseCursor = false)
            echo("✅ Mouse cursor visible")
        }

        try {
            updated.write(configFile)
        } catch (e: Exception) {
            echo("❌ Error saving parameters: ${e.message}")
            throw Abort()
        }

        echo()
        displayParameters(updated)
    }

    private fun displayParameters(params: GameParametersV1) {
        echo("🎮 ${params.name}")
        echo()
        echo("🖥  Resolution: ${params.resolution.width}x${params.resolution.height}")
        echo("📐 Sprites: ${params.sprites.width}x${params.sprites.height}")
        echo("🔍 Zoom: ${params.zoom}")
        echo("🎨 Palette: ${ColorUtils.formatCurrentPaletteDisplay(params.colors, maxColors = 16)}")
        echo("📝 Scripts: ${params.scripts.joinToString(", ").ifEmpty { "none" }}")
        echo("🖼️  Spritesheets: ${params.spritesheets.joinToString(", ").ifEmpty { "none" }}")
        echo("🗺️  Levels: ${params.levels.joinToString(", ").ifEmpty { "none" }}")
        echo("🔊 Sounds: ${listOfNotNull(params.sound).joinToString(", ").ifEmpty { "none" }}")
        echo("🖱️  Hide mouse cursor: ${if (params.hideMouseCursor) "yes" else "no"}")
    }
}
