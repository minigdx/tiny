package com.github.minigdx.tiny.file

import kotlinx.browser.localStorage

class JsLocalFile(
    val filename: String,
    val localStoragePrefix: String = "tiny",
) : LocalFile {

    override val name: String
    override val extension: String

    init {
        val (name, ext) = ("$filename.").split(".")
        this.name = name
        this.extension = ext
    }

    override fun readAll(): ByteArray? {
        val item = localStorage.getItem("$localStoragePrefix-$filename")
        return item?.encodeToByteArray() ?: return null
    }

    override fun save(content: ByteArray) {
        localStorage.setItem("$localStoragePrefix-$filename", content.decodeToString())
    }
}
