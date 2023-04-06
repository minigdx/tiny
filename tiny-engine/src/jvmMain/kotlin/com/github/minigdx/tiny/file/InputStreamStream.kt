package com.github.minigdx.tiny.file

import java.io.InputStream

class InputStreamStream(private val source: InputStream) : SourceStream<ByteArray> {
    private var modified = true

    override fun wasModified(): Boolean {
        return modified
    }

    override suspend fun read(): ByteArray {
        modified = false
        return source.readAllBytes()
    }
}
