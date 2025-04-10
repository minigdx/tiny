package com.github.minigdx.tiny.cli

import com.github.ajalt.clikt.core.main
import com.github.minigdx.tiny.cli.command.MainCommand

fun main(vararg args: String) {
    MainCommand().main(args.toList())
}
