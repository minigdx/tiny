package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.TinyException
import org.luaj.vm2.LuaError

fun LuaError.errorLine(): Pair<Int, String>? {
    return fileline?.let { fileline ->
        val separator = fileline.lastIndexOf(':')
        val lineNumber = fileline.substring(separator + 1).toInt()

        val line =
            if (fileline.startsWith("@")) {
                "into the file $fileline"
            } else {
                fileline.lines()[lineNumber - 1]
            }
        lineNumber to line
    }
}

fun LuaError.fromErrorLine(): Int? {
    return errorLine()?.first
}

fun LuaError.fromMessage(): Int? {
    val msg = message ?: return null

    val pattern = """\[[\s\S]*]:(\d+):.*""".toRegex()
    val match = pattern.matchEntire(msg)
    return match?.groupValues?.get(1)?.toIntOrNull()
}

fun LuaError.toTinyException(content: String): TinyException {
    val line = line.takeIf { line != -1 }
        // There is no line information, le's check the fileline
        ?: fromErrorLine()
        // The error line might be in the error message
        ?: fromMessage()
        // Let's give it up...
        ?: -1
    return TinyException(
        name = this.script,
        content = content,
        lineNumber = line,
        message = this.message,
        cause = this,
    )
}
