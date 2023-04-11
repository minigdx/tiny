package com.github.minigdx.tiny.file

import java.io.InputStream

class InputStreamStream(private val source: InputStream) : SourceStream<ByteArray> {
    override suspend fun read(): ByteArray {
        return source.readAllBytes()
    }
}
