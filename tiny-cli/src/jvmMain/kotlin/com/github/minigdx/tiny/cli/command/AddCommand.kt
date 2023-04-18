package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class AddCommand : CliktCommand(name = "add") {

    val game by option(
        help = "The directory containing all game information"
    )
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val resources by argument(
        help = "The resource to add to the game. The kind of resource will be deducted from the file extension."
    ).multiple(required = true)

    override fun run() {
        val tiny = game.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }
        // Open the _tiny.json
        var gameParameters = GameParameters.read(tiny)

        // regarding the input, add it into the right resource
        resources.forEach { r ->
            if (r.endsWith("png")) {
                // Add spritesheet
                gameParameters = gameParameters.addSpritesheet(r)
            } else if (r.endsWith("lua")) {
                // Add script
                gameParameters = gameParameters.addScript(r)
            } else {
                val file = File(r)
                if (file.isDirectory && file.resolve("data.json").isFile) {
                    // Add level
                    gameParameters = gameParameters.addLevel(r)
                }
            }
        }

        // Save the updated _tiny.json
        gameParameters.write(tiny)
    }
}
