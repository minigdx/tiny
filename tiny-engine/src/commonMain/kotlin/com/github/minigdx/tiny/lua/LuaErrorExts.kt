package com.github.minigdx.tiny.lua

import org.luaj.vm2.LuaError

fun LuaError.errorLine(): Pair<Int, String>? {
    return fileline?.let { fileline ->
        val separator = fileline.lastIndexOf(':')
        val lineNumber = fileline.substring(separator + 1).toInt()

        val line = if (fileline.startsWith("@")) {
            "into the file $fileline"
        } else {
            fileline.lines()[lineNumber - 1]
        }
        lineNumber to line
    }
}
