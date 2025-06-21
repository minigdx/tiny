package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class MainCommand : CliktCommand() {
    init {
        subcommands(
            CreateCommand(),
            RunCommand(),
            DebugCommand(),
            AddCommand(),
            ExportCommand(),
            ExportDesktopCommand(),
            ServeCommand(),
            PaletteCommand(),
            SfxCommand(),
        )
    }

    override fun run() {
        echo("\uD83E\uDDF8 ʕ　·ᴥ·ʔ TINY - The Tiny Virtual Gaming Console")
    }
}
