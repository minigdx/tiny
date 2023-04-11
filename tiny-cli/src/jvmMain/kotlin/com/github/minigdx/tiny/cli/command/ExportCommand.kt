package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.util.zip.ZipInputStream

class ExportCommand : CliktCommand("export") {

    val gameDirectory by argument(help = "The directory containing all game information")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    override fun run() {
        echo("Export ${gameDirectory.absolutePath}")
        // Get the game configuration
        // Get the JS template
        // Get file by files instead of reading a full zip]]]]]]]
        //
        // ?
        val archive = ExportCommand::class.java.getResourceAsStream("tiny-engine.zip") ?: TODO("Tiny-engine.zip not found")
        val zip = ZipInputStream(archive)
        var nextEntry = zip.nextEntry
        while (nextEntry != null) {
            println(" -> " + nextEntry.name)
            nextEntry = zip.nextEntry
        }
        // Generate the JS Template using game parameter.
        // Output this template in a new directory
        // Copy all files from the game configuration into this directory
        // Copy engine files into this directory (engine.js, _boot.lua, ...)
        // Zip this new directory and enjoy?
        // -> The zip can be created in memory
    }
}
