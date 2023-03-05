package com.github.minigdx.tiny.file

import kotlinx.coroutines.flow.Flow
import java.io.File

interface SourceStream<T> {

    fun wasModified(): Boolean = false

    fun read(): T
}

class FileStream(val origin: File) : SourceStream<ByteArray> {
    private var lastModified: Long = 0

    override fun wasModified(): Boolean {
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
}

interface VirtualFileSystem {

    fun <T> watch(source: SourceStream<T>): Flow<T>

}
