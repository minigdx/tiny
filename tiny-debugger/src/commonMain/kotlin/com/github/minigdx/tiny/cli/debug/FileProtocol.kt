package com.github.minigdx.tiny.cli.debug

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(val name: String, val content: String)

@Serializable
data class FilesMessage(val type: String, val files: List<FileInfo>)

@Serializable
data class FileChangedMessage(val type: String, val file: FileInfo)
