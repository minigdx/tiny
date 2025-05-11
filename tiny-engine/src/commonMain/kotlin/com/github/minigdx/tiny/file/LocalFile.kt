package com.github.minigdx.tiny.file

interface LocalFile {
    /**
     * Name of the file, without the extension
     */
    val name: String

    /**
     * Extension. Can be blank
     */
    val extension: String

    /**
     * Read the content of the file
     */
    fun readAll(): ByteArray?

    /**
     * Save the content of the file
     */
    fun save(content: ByteArray)
}
