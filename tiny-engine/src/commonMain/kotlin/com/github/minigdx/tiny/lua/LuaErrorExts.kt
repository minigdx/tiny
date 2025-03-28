package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.TinyException
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

fun LuaError.toTinyException(content: String): TinyException {
    return TinyException(
        name = this.script,
        content = content,
        lineNumber = this.line,
        message = this.message,
        cause = this,
    )
}
