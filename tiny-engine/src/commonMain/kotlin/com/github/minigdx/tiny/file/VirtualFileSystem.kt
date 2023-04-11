package com.github.minigdx.tiny.file

import kotlinx.coroutines.flow.Flow

interface SourceStream<T> {

    suspend fun exists(): Boolean = true

    fun wasModified(): Boolean = false

    suspend fun read(): T
}

interface TargetStream<T> {
    fun write(data: T)
}

interface VirtualFileSystem {

    fun <T> watch(source: SourceStream<T>): Flow<T>

    suspend fun save(targetStream: TargetStream<ByteArray>, data: ByteArray)
}
