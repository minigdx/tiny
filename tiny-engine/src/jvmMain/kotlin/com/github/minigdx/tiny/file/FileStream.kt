package com.github.minigdx.tiny.file

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.io.File

class FileStream(private val origin: File) : SourceStream<ByteArray>, TargetStream<ByteArray> {

    private var lastModified: Long = 0

    override fun wasModified(): Boolean {
        if (!origin.exists()) {
            return false
        }
        val wasModified = if (origin.lastModified() != lastModified) {
            lastModified = origin.lastModified()
            true
        } else {
            false
        }
        return wasModified
    }

    override suspend fun exists(): Boolean {
        return origin.exists()
    }

    override suspend fun read(): ByteArray {
        return origin.readBytes()
    }

    override fun write(data: ByteArray) {
        origin.writeBytes(data)
    }
}
