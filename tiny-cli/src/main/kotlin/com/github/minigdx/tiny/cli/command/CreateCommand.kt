package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors
import com.github.minigdx.tiny.cli.GamePalette
import com.github.minigdx.tiny.cli.command.utils.ColorUtils.brightness
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size
import kotlinx.serialization.json.encodeToStream
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.FileOutputStream

@Language("Lua")
private const val DEFAULT_GAME_SCRIPT = """
function _init()

end


function _update()

end


function _draw()
    gfx.cls()
    print("Congratulation! Your game is running!")
end
"""

class CreateCommand : CliktCommand(name = "create") {
    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val gameName by option(help = "🏷 The name of the game")
        .prompt(text = "🏷  The name of the game", default = generateRandomGameName())

    private val gameResolution by option(help = "🖥 The game resolution (e.g., 800x600)")
        .prompt(text = "\uD83D\uDDA5  Game resolution (e.g., 800x600)", default = "256x256")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val gameScript by option(help = "📝 Name of the default game script")
        .prompt(text = "\uD83D\uDCDD Name of the first game script", default = "game.lua")
        .validate { require(it.endsWith(".lua")) { "Invalid game script extension: $it" } }

    private val spriteSize by option(help = "📐 The sprite size (e.g., 16x16)")
        .prompt(text = "\uD83D\uDCD0  Sprite size (e.g., 16x16)", default = "16x16")
        .validate { require(it.matches(Regex("\\d+x\\d+"))) { "Invalid resolution format: $it" } }

    private val zoom by option(help = "🔍 Game zoom")
        .int()
        .prompt(text = "\uD83D\uDD0D  Game zoom", default = 2)

    private val spritesheets by option(help = "🖼️ The filenames of the sprite sheets, separated by a comma (e.g., file1.png, file2.png)")
        .prompt(text = "\uD83D\uDCC4  Sprite sheet name to include", default = "")
        .validate {
            require(
                it.isEmpty() ||
                    it.split(",")
                        .all { f -> f.trim().endsWith(".png") },
            ) { "Invalid image file $it. Only *.png are supported" }
        }

    private val palette by option(help = "🎨 The Color palette to use")
        .int()
        .prompt(
            """🎨  Please choose a game color palette:
${
                GamePalette.ALL.mapIndexed { index, gamePalette ->
                    formatPaletteDisplay(gamePalette, index)
                }.joinToString("\n")
            }
""",
        )

    private val hideMouseCursor by option(help = "🖱️ Hide system cursor mouse")
        .prompt("\uD83D\uDDB1\uFE0F  Hide system cursor mouse? (yes or no)", default = "No")
        .validate { it.lowercase() == "yes" || it.lowercase() == "no" }

    override fun help(context: Context) = "Create a new game with the help of a wizard 🧙."

    override fun run() {
        echo("➡\uFE0F  Game Name: $gameName")
        echo("➡\uFE0F  Game Resolution: $gameResolution")
        echo("➡\uFE0F  Game Resolution: $spriteSize")
        echo("➡\uFE0F  Sprite Sheet Filenames: ${spritesheets.ifBlank { "No spritesheet added!" }}")
        echo("➡\uFE0F  Color palette: ${GamePalette.ALL[palette - 1].name}")

        val configuration =
            GameParametersV1(
                name = gameName,
                resolution = gameResolution.toSize(),
                sprites = spriteSize.toSize(),
                zoom = zoom,
                colors = GamePalette.ALL[palette - 1].colors.sortedBy { brightness(it) },
                scripts = listOf(gameScript),
                hideMouseCursor = hideMouseCursor == "yes".lowercase(),
            ) as GameParameters

        if (!gameDirectory.exists()) gameDirectory.mkdirs()

        val configurationFile = gameDirectory.resolve("_tiny.json")
        FileOutputStream(configurationFile).use {
            JSON.encodeToStream(configuration, it)
        }

        gameDirectory.resolve(gameScript).writeText(DEFAULT_GAME_SCRIPT)

        CreateCommand::class.java.getResourceAsStream("/_tiny.stub.lua")?.let { content ->
            gameDirectory.resolve("_tiny.stub.lua").writeBytes(content.readAllBytes())
        }

        echo("\uD83C\uDFD7\uFE0F  Game created into: ${gameDirectory.absolutePath}")
        echo("\uD83C\uDFC3\u200D♂\uFE0F To run the game: tiny-cli run ${computePath(gameDirectory)}")
    }

    private fun String.toSize(): Size {
        val (w, h) = this.split("x")
        return Size(w.toInt(), h.toInt())
    }

    private fun generateRandomGameName(): String {
        val adjectives = listOf("Funny", "Awesome", "Crazy", "Epic", "Mystical", "Magical")
        val nouns = listOf("Unicorns", "Pandas", "Robots", "Dragons", "Ninjas", "Pirates")
        return "${adjectives.random()} ${nouns.random()} Game"
    }

    private fun computePath(gamePath: File): String {
        val currentPath = File(".")
        return gamePath.relativeTo(currentPath).path
    }

    companion object {
        private const val MAX_COLOR_PALETTE_DISPLAYED = 28

        private fun formatPaletteDisplay(
            palette: GamePalette,
            index: Int,
        ): String {
            val colorSquares = palette.colors.take(MAX_COLOR_PALETTE_DISPLAYED).joinToString("") { hexColor ->
                // Remove the '#' prefix and parse RGB components
                val colorWithoutHash = hexColor.removePrefix("#")
                val r = colorWithoutHash.substring(0, 2).toInt(16)
                val g = colorWithoutHash.substring(2, 4).toInt(16)
                val b = colorWithoutHash.substring(4, 6).toInt(16)

                // Create colored square using Mordant's background color
                TextColors.rgb(r / 255.0, g / 255.0, b / 255.0)("◼")
            }

            val overflowText = if (palette.colors.size > MAX_COLOR_PALETTE_DISPLAYED) {
                " + ${palette.colors.size - MAX_COLOR_PALETTE_DISPLAYED} colors"
            } else {
                ""
            }

            return "[${index + 1}] ${palette.name}  $colorSquares$overflowText"
        }
    }
}
