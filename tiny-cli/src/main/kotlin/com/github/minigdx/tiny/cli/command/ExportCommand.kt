package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.cli.config.GameParametersV1
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportCommand : CliktCommand(name = "export") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val archive by option(help = "The name of the exported archive.")
        .default("tiny-export.zip")

    private val gameExporter = GameExporter()

    override fun help(context: Context) = "Export your game as a web game."

    override fun run() {
        echo("\uD83D\uDC77 Export ${gameDirectory.absolutePath}")

        gameExporter.export(gameDirectory, archive)

        echo("\uD83C\uDF89 Congratulation! Your game has been exported in the $archive file.")
    }
}

class GameExporter {
    fun export(
        gameDirectory: File,
        archive: String,
    ) {
        val configFile = gameDirectory.resolve("_tiny.json")
        val gameParameters = JSON.decodeFromStream<GameParameters>(FileInputStream(configFile))

        val exportedGame = ZipOutputStream(FileOutputStream(gameDirectory.resolve(archive)))

        // Add all engine files into the zip
        ENGINE_FILES.forEach { name ->
            val content = ExportCommand::class.java.getResourceAsStream("/tiny-engine-js/$name")
            exportedGame.putNextEntry(ZipEntry(name))
            exportedGame.write(content!!.readAllBytes())
            exportedGame.closeEntry()
        }

        // Add all game specific file into the zip
        exportedGame.putNextEntry(ZipEntry("_tiny.json"))
        exportedGame.write(configFile.readBytes())
        exportedGame.closeEntry()

        when (gameParameters) {
            is GameParametersV1 -> {
                (gameParameters.scripts + gameParameters.libraries.map { "$it.lua" }).forEach { name ->
                    exportedGame.putNextEntry(ZipEntry(name))
                    exportedGame.write(gameDirectory.resolve(name).readBytes())
                    exportedGame.closeEntry()
                }
                gameParameters.spritesheets.forEach { name ->
                    exportedGame.putNextEntry(ZipEntry(name))
                    exportedGame.write(gameDirectory.resolve(name).readBytes())
                    exportedGame.closeEntry()
                }
                gameParameters.sounds.forEach { name ->
                    exportedGame.putNextEntry(ZipEntry(name))
                    exportedGame.write(gameDirectory.resolve(name).readBytes())
                    exportedGame.closeEntry()
                }
                gameParameters.levels.forEach { name ->
                    val baseDirectory = gameDirectory.resolve(name)
                    val files = baseDirectory.listFiles() ?: emptyArray()
                    files.forEach { file ->
                        if (file.isFile) {
                            exportedGame.putNextEntry(ZipEntry(name + "/" + file.name))
                            exportedGame.write(file.readBytes())
                            exportedGame.closeEntry()
                        }
                    }
                }

                // Add index.html
                val content = ExportCommand::class.java.getResourceAsStream("/templates/index.html")!!.readAllBytes()
                var template = content.decodeToString()
                template = template.replace("{GAME_NAME}", gameParameters.name)
                template = template.replace("{GAME_WIDTH}", gameParameters.resolution.width.toString())
                template = template.replace("{GAME_HEIGHT}", gameParameters.resolution.height.toString())
                template = template.replace("{GAME_ZOOM}", gameParameters.zoom.toString())
                template = template.replace("{GAME_SPRW}", gameParameters.sprites.width.toString())
                template = template.replace("{GAME_SPRH}", gameParameters.sprites.height.toString())
                template = template.replace("{GAME_HIDE_MOUSE}", gameParameters.hideMouseCursor.toString())

                template =
                    replaceList(
                        template,
                        (gameParameters.scripts + gameParameters.libraries.map { "$it.lua" }),
                        "{GAME_SCRIPT}",
                        "GAME_SCRIPT",
                    )
                template =
                    replaceList(
                        template,
                        gameParameters.spritesheets,
                        "{GAME_SPRITESHEET}",
                        "GAME_SPRITESHEET",
                    )
                template = replaceList(template, gameParameters.levels, "{GAME_LEVEL}", "GAME_LEVEL")
                template = replaceList(template, gameParameters.sounds, "{GAME_SOUND}", "GAME_SOUND")

                template = template.replace("{GAME_COLORS}", gameParameters.colors.joinToString(","))

                exportedGame.putNextEntry(ZipEntry("index.html"))
                exportedGame.write(template.toByteArray())
                exportedGame.closeEntry()
            }
        }

        exportedGame.close()
    }

    private fun replaceList(
        template: String,
        values: List<String>,
        tag: String,
        delimiter: String,
    ): String {
        val pattern = ("<!-- $delimiter -->(.*?)<!-- ${delimiter}_END -->").toRegex(RegexOption.DOT_MATCHES_ALL)
        val delimiterTag = pattern.find(template)!!.groupValues[1]

        var result = ""
        values.forEach { script ->
            result += delimiterTag.replace(tag, script)
        }
        return template.replace(delimiterTag, result)
    }

    companion object {
        val ENGINE_FILES =
            listOf(
                "_boot.lua",
                "_boot.png",
                "_engine.lua",
                "tiny-engine.js",
            )
    }
}
