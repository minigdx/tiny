package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
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
        help = "The directory containing all game information",
    )
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val resources by argument(
        help = "The resource to add to the game. The kind of resource will be deducted from the file extension.",
    ).multiple(required = true)

    override fun help(context: Context) = "Add a resource to your game."

    override fun run() {
        val tiny = game.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }
        // Open the _tiny.json
        var gameParameters = GameParameters.read(tiny)

        // regarding the input, add it into the right resource
        resources.forEach { r ->
            val type =
                if (r.endsWith("png")) {
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
}
