package com.github.minigdx.tiny.file

import kotlinx.coroutines.flow.Flow
import java.io.File

interface SourceStream<T> {

    fun wasModified(): Boolean = false

    fun read(): T
}

class FileStream(private val origin: File) : SourceStream<ByteArray>, TargetStream<ByteArray> {
    private var lastModified: Long = 0

    override fun wasModified(): Boolean {
        if(!origin.exists()) {
            return false
        }
        val wasModified = if(origin.lastModified() != lastModified) {
            lastModified = origin.lastModified()
            true
        } else {
            false
        }
        return wasModified
    }

    override fun read(): ByteArray {
        return origin.readBytes()
    }

    override fun write(data: ByteArray) {
        origin.writeBytes(data)
    }
}

interface TargetStream<T> {
    fun write(data: T)
}

interface VirtualFileSystem {

    fun <T> watch(source: SourceStream<T>): Flow<T>

    suspend fun save(targetStream: TargetStream<ByteArray>, data: ByteArray)
}
