package com.github.minigdx.tiny.file

interface SourceStream<T> {
    suspend fun exists(): Boolean = true

    fun wasModified(): Boolean = false

    suspend fun read(): T
}
