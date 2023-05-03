package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParameters.Companion.JSON
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream

class MainCommand : CliktCommand(invokeWithoutSubcommand = true) {

    init {
        subcommands(
            CreateCommand(),
            RunCommand(),
            AddCommand(),
            ExportCommand(),
            ServeCommand()
        )
    }

    override fun run() {
        echo("\uD83E\uDDF8 ʕ　·ᴥ·ʔ TINY - The Tiny Virtual Gaming Console")
    }
}
