package com.github.minigdx.tiny.file

import java.io.File

class JvmWatchedFile(override val name: String) : WatchedFile {

    private val file: File = File(name)

    private var fileUpdated: Boolean = true

    private val listeners: MutableList<(ByteArray) -> Unit> = mutableListOf()

    private var lastModified = 0L

    override val hasToBeReload: Boolean
        get() {
            return fileUpdated
        }

    internal fun preLoop() {
        val current = file.lastModified()

        fileUpdated = lastModified != current

        if (fileUpdated) {
            lastModified = current
            fileUpdated = false
            listeners.forEach {
                it.invoke(File(name).readBytes())
            }
        }
    }

    internal fun postLoop() = Unit

    override fun whenLoadedOrReloaded(block: (ByteArray) -> Unit): WatchedFile {
        listeners.add(block)
        return this
    }
}
