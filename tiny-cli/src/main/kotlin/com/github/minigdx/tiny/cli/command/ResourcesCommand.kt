package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class ResourcesCommand : CliktCommand(name = "resources") {
    private val gameDirectory by option("-d", "--directory", help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val delete by option("--delete", help = "Remove a resource from the game by filename.")
        .multiple()

    override fun help(context: Context) = "Inspect and manage game resources."

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            throw MissingTinyConfigurationException(configFile)
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

        if (delete.isNotEmpty()) {
            deleteResources(gameParameters, configFile)
        } else {
            displayResources(gameParameters)
        }
    }

    private fun deleteResources(
        params: GameParametersV1,
        configFile: File,
    ) {
        var updated = params

        for (resource in delete) {
            val found = resource in updated.scripts ||
                resource in updated.spritesheets ||
                resource in updated.levels ||
                resource == updated.sound

            if (!found) {
                echo("❌ Resource not found: $resource")
                continue
            }

            updated = updated.copy(
                scripts = updated.scripts.filter { it != resource },
                spritesheets = updated.spritesheets.filter { it != resource },
                levels = updated.levels.filter { it != resource },
                sound = if (updated.sound == resource) null else updated.sound,
            )
            echo("✅ Removed: $resource")
        }

        try {
            updated.write(configFile)
        } catch (e: Exception) {
            echo("❌ Error saving _tiny.json: ${e.message}")
            throw Abort()
        }

        echo()
        displayResources(updated)
    }

    private fun displayResources(params: GameParametersV1) {
        echo("📦 Game resources")
        echo()

        displayCategory("📝 Scripts", params.scripts)
        displayCategory("🖼️  Spritesheets", params.spritesheets)
        displayCategory("🗺️  Levels", params.levels)
        displayCategory("🔊 Sounds", listOfNotNull(params.sound))
    }

    private fun displayCategory(
        label: String,
        resources: List<String>,
    ) {
        if (resources.isEmpty()) return
        echo("$label:")
        resources.forEachIndexed { index, resource ->
            echo("   ${index + 1}. $resource")
        }
        echo()
    }
}
