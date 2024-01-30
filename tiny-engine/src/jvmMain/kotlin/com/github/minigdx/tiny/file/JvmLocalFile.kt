package com.github.minigdx.tiny.file

import java.io.File

class JvmLocalFile(
    name: String,
    val workingDirectory: File,
) : LocalFile {

    private val file = File(name)

    override val name: String = file.nameWithoutExtension

    override val extension: String = file.extension
    override fun readAll(): ByteArray {
        return workingDirectory.resolve(file).readBytes()
    }

    override fun save(content: ByteArray) {
        workingDirectory.resolve(file).writeBytes(content)
    }
}
