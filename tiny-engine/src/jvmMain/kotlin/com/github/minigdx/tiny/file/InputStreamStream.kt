package com.github.minigdx.tiny.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class InputStreamStream(private val source: InputStream) : SourceStream<ByteArray> {
    override suspend fun read(): ByteArray {
        return withContext(Dispatchers.IO) {
            source.readAllBytes()
        }
    }
}
