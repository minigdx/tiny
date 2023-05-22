package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.file.SourceStream

class ObjectStream<T : Any>(val data: T) : SourceStream<T> {
    override suspend fun read(): T {
        return data
    }
}
