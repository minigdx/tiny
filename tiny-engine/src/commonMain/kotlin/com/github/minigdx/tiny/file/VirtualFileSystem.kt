package com.github.minigdx.tiny.file

import kotlinx.coroutines.flow.Flow

interface VirtualFileSystem {
    fun <T> watch(source: SourceStream<T>): Flow<T>

    suspend fun save(
        targetStream: TargetStream<ByteArray>,
        data: ByteArray,
    )
}
