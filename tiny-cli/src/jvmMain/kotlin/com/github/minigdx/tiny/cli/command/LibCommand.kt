package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File

class LibCommand : CliktCommand(name = "lib", help = "Download a library for your game.") {

    val game by option(
        help = "The directory containing all game information",
    )
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    val libs by argument(
        help = "The library to be added to the game. All available libraries are available here: https://github.com/minigdx/tiny/tiny-repository-libs.",
    ).multiple(required = true)

    val update by option(help = "Update existing libraries").flag(default = false)

    // get by commit hash?
    override fun run() {
        val configFile = game.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("\uD83D\uDE2D No _tiny.json found! Can't run the game without.")
            throw Abort()
        }
        val gameParameters = GameParameters.read(configFile)

        val libsToCheck = if (update) {
            libs + when (gameParameters) {
                is GameParametersV1 -> gameParameters.libraries
            }
        } else {
            libs
        }

        val addedLibs = runBlocking {
            libsToCheck.map { lib ->
                lib to createUrlFor(lib)
            }.map { (lib, url) ->
                lib to async(Dispatchers.IO) {
                    HttpClient().use { client ->
                        val httpResponse = client.get(url)
                        if (httpResponse.status == HttpStatusCode.OK) {
                            httpResponse.readBytes()
                        } else {
                            null
                        }
                    }
                }
            }.mapNotNull { (lib, asyncContent) ->
                val content = asyncContent.await()
                if (content == null) {
                    echo("⛔ library $lib ($lib.lua) failed to be downloaded... check the name!")
                    null
                } else {
                    game.resolve("$lib.lua").writeBytes(content)
                    echo("\uD83D\uDCBD library $lib ($lib.lua) downloaded!")
                    lib
                }
            }
        }

        val updatedGameParameters = addedLibs.fold(gameParameters) { parameter, lib ->
            parameter.addLibrary(lib)
        }

        // Save the updated _tiny.json
        updatedGameParameters.write(configFile)
        echo("\uD83D\uDCBD All libraries have been downloaded and added to the game!")
    }

    private fun createUrlFor(lib: String): String {
        val (name, commit) = if (lib.contains("@")) {
            lib.split("@")
        } else {
            listOf(lib, "main")
        }
        return "https://raw.githubusercontent.com/minigdx/tiny/$commit/tiny-repository-libs/$name.lua"
    }
}
