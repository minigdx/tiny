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
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.minigdx.tiny.cli.GamePalette
import com.github.minigdx.tiny.cli.command.utils.ColorUtils
import com.github.minigdx.tiny.cli.command.utils.ColorUtils.brightness
import com.github.minigdx.tiny.cli.command.utils.PaletteImageGenerator
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size
import com.github.minigdx.tiny.platform.SoundData
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.UUID

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

@Language("Lua")
private const val DEFAULT_BOOT_SCRIPT = """
local ready = false

function _init(screen_width, screen_height)
    -- prepare your boot script
end

function _update()
    -- clear the screen and exist the boot script when all resources are loaded
    if (ready) then
        gfx.cls("#000000")
        tiny.exit(0) -- start the first script in the game script stack
    end
end

function _draw()
    gfx.cls("#000000")
    -- draw your boot animation
end

--[[
_resources is a magic method called when all the resources are loaded
Before this call, only primitives can be used. Sprites are not loaded yet, as sound, levels, ...
After, all resources are available
]]--
function _resources()
    -- all game resources are loaded
    ready = true
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

    private val bootScript by option(help = "🚀 Custom boot script to use instead of the default boot.lua")
        .prompt("\uD83D\uDE80  Custom boot script (leave empty for default boot.lua)", default = "")
        .validate {
            require(it.isEmpty() || it.endsWith(".lua")) { "Invalid boot script extension: $it. Must be a .lua file." }
        }

    override fun help(context: Context) = "Create a new game with the help of a wizard 🧙."

    override fun run() {
        echo("➡\uFE0F  Game Name: $gameName")
        echo("➡\uFE0F  Game Resolution: $gameResolution")
        echo("➡\uFE0F  Game Resolution: $spriteSize")
        echo("➡\uFE0F  Sprite Sheet Filenames: ${spritesheets.ifBlank { "No spritesheet added!" }}")
        echo("➡\uFE0F  Color palette: ${GamePalette.ALL[palette - 1].name}")
        if (bootScript.isNotBlank()) {
            echo("➡\uFE0F  Boot script: $bootScript")
        }

        val configuration = GameParametersV1(
            name = gameName,
            id = UUID.randomUUID().toString(),
            resolution = gameResolution.toSize(),
            sprites = spriteSize.toSize(),
            zoom = zoom,
            colors = GamePalette.ALL[palette - 1].colors.sortedBy { brightness(it) },
            scripts = listOf(gameScript),
            sound = "default-sound.sfx",
            hideMouseCursor = hideMouseCursor == "yes".lowercase(),
            bootScript = bootScript.ifBlank { null },
        ) as GameParameters

        if (!gameDirectory.exists()) gameDirectory.mkdirs()

        val configurationFile = gameDirectory.resolve("_tiny.json")
        configuration.write(configurationFile)

        val soundFile = gameDirectory.resolve("default-sound.sfx")
        soundFile.writeText(SoundData.DEFAULT_SFX.music.serialize())

        gameDirectory.resolve(gameScript).writeText(DEFAULT_GAME_SCRIPT)

        if (bootScript.isNotBlank()) {
            gameDirectory.resolve(bootScript).writeText(DEFAULT_BOOT_SCRIPT)
        }

        CreateCommand::class.java.getResourceAsStream("/_tiny.stub.lua")?.let { content ->
            gameDirectory.resolve("_tiny.stub.lua").writeBytes(content.readAllBytes())
        }

        // Generate palette image
        val paletteFile = PaletteImageGenerator.generatePaletteImage(gameDirectory, (configuration as GameParametersV1).colors)

        echo("\uD83C\uDFD7\uFE0F  Game created into: ${gameDirectory.absolutePath}")
        echo("\uD83C\uDFA8  Palette image created: ${paletteFile.name}")
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
            val colorsText = ColorUtils.formatCurrentPaletteDisplay(palette.colors, maxColors = MAX_COLOR_PALETTE_DISPLAYED)

            return if (palette.source != null) {
                val invoke = TextStyles.hyperlink(palette.source).invoke(palette.name)
                "[${index + 1}] $invoke $colorsText"
            } else {
                "[${index + 1}] ${palette.name} $colorsText"
            }
        }
    }
}
