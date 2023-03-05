package com.github.minigdx.tiny.file

interface WatchedFile {
    val name: String
    val hasToBeReload: Boolean

    fun whenLoadedOrReloaded(block: (ByteArray) -> Unit): WatchedFile
}
