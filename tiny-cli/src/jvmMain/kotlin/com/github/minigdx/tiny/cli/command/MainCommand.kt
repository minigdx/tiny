package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class MainCommand : CliktCommand(invokeWithoutSubcommand = true) {

    init {
        subcommands(
            CreateCommand(),
            RunCommand(),
            AddCommand(),
            ExportCommand(),
            ServeCommand(),
            LibCommand(),
        )
    }

    override fun run() {
        echo("\uD83E\uDDF8 ʕ　·ᴥ·ʔ TINY - The Tiny Virtual Gaming Console")
    }
}
