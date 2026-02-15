package com.github.minigdx.tiny.cli.debug

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(val name: String, val content: String)
