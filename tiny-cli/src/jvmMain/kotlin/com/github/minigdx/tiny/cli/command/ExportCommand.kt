package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull.content
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportCommand : CliktCommand("export") {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    internal val json = Json {
        ignoreUnknownKeys = true
    }

    override fun run() {
        echo("Export ${gameDirectory.absolutePath}")

        val configFile = gameDirectory.resolve("_tiny.json")
        val gameParameters = json.decodeFromStream<GameParameters>(FileInputStream(configFile))

        val exportedGame = ZipOutputStream(FileOutputStream("export.zip"))

        // Add all engine files into the zip
        TEMPLATES.forEach { name ->
            val content = ExportCommand::class.java.getResourceAsStream("/tiny-engine-js/$name")
            exportedGame.putNextEntry(ZipEntry(name))
            exportedGame.write(content!!.readAllBytes())
            exportedGame.closeEntry()
        }

        // Add all game specific file into the zip
        when (gameParameters) {
            is GameParametersV1 -> {
                gameParameters.scripts.forEach { name ->
                    exportedGame.putNextEntry(ZipEntry(name))
                    exportedGame.write(gameDirectory.resolve(name).readBytes())
                    exportedGame.closeEntry()
                }
                gameParameters.spritesheets.forEach { name ->
                    exportedGame.putNextEntry(ZipEntry(name))
                    exportedGame.write(gameDirectory.resolve(name).readBytes())
                    exportedGame.closeEntry()
                }
                // TODO: levels??
            }
        }

        exportedGame.close()
        // Generate the JS Template using game parameter.
        // Output this template in a new directory (Where??)
        // Copy all files from the game configuration into this directory
        // Copy engine files into this directory (engine.js, _boot.lua, ...)
        // Zip this new directory and enjoy?
        // -> The zip can be created in memory
    }

    companion object {
        val TEMPLATES = listOf(
            "_boot.lua",
            "_boot.png",
            "_engine.lua",
            "tiny-engine.js",
            "index.html", // TO BE REMOVED. The file should be in the CLI.
        )
    }
}
