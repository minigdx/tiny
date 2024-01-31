package com.github.minigdx.tiny.file

import kotlinx.browser.localStorage

class JsLocalFile(
    val filename: String,
) : LocalFile {

    override val name: String
    override val extension: String

    init {
        val (name, ext) = ("$filename.").split(".")
        this.name = name
        this.extension = ext
    }

    override fun readAll(): ByteArray {
        val item = localStorage.getItem(filename)
        return item?.encodeToByteArray() ?: ByteArray(0)
    }

    override fun save(content: ByteArray) {
        localStorage.setItem(filename, content.decodeToString())
    }
}
