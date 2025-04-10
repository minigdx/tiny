package com.github.minigdx.tiny.engine

/**
 * Exception occurred in the game engine.
 */
class TinyException(
    // Script name
    val name: String,
    // line number of the script throwing the error
    val lineNumber: Int,
    // content of the script
    val content: String,
    message: String?,
    cause: Throwable?,
) : RuntimeException(message, cause) {
    val line: String = content.split("\n").getOrNull(lineNumber - 1)?.trim() ?: ""
}
