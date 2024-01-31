package com.github.minigdx.tiny.file

import kotlinx.browser.localStorage

class JsLocalFile(
    override val name: String,
    override val extension: String,
) : LocalFile {

    private fun computeFilename(): String {
        return if (extension.isBlank()) {
            name
        } else {
            "$name.$extension"
        }
    }

    override fun readAll(): ByteArray {
        val item = localStorage.getItem(computeFilename())
        return item?.encodeToByteArray() ?: ByteArray(0)
    }

    override fun save(content: ByteArray) {
        localStorage.setItem(computeFilename(), content.decodeToString())
    }
}
