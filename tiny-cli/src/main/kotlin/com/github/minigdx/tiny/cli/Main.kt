package com.github.minigdx.tiny.cli

import picocli.CommandLine
import kotlin.system.exitProcess


fun main(vararg args: String) {
    val exitCode: Int = CommandLine(TinyCommand()).execute(*args)
    exitProcess(exitCode)
}
