package com.github.minigdx.tiny.util

object Assert {
    fun assert(
        value: Boolean,
        message: () -> String = { "Assert failed!" },
    ) {
        if (!value) {
            throw IllegalStateException(message())
        }
    }
}
