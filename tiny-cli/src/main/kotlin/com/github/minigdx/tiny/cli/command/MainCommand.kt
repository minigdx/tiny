package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.mingdx.tiny.doc.CliAnnotation

@CliAnnotation(hidden = true)
class MainCommand : CliktCommand() {
    init {
        subcommands(
            CreateCommand(),
            RunCommand(),
            DebugCommand(),
            AddCommand(),
            ExportCommand(),
            ServeCommand(),
            PaletteCommand(),
            SfxCommand(),
            UpdateCommand(),
            ResourcesCommand(),
            DocsCommand(),
        )
    }

    override fun run() {
        echo("\uD83E\uDDF8 ʕ　·ᴥ·ʔ TINY - The Tiny Virtual Gaming Console")
    }
}
