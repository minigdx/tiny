package com.github.minigdx.tiny.file

interface LocalFile {

    val name: String

    val extension: String
    fun readAll(): ByteArray

    fun save(content: ByteArray)
}
