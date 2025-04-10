package com.github.minigdx.tiny.file

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

class CommonVirtualFileSystem : VirtualFileSystem {
    private val delay = 1.seconds

    override fun <T> watch(source: SourceStream<T>): Flow<T> {
        return flow {
            if (source.exists()) {
                emit(source.read())
                delay(delay)
            }
            while (true) {
                if (source.wasModified()) {
                    emit(source.read())
                }
                delay(delay)
            }
        }
    }

    override suspend fun save(
        targetStream: TargetStream<ByteArray>,
        data: ByteArray,
    ) {
        targetStream.write(data)
    }
}
