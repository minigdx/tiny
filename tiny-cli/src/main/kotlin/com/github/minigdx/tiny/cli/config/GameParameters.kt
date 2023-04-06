package com.github.minigdx.tiny.cli.config

data class GameParameters(val version: String)

data class Size(val width: Int, val height: Int)

data class GameParametersV1(
    val version: String,
    val name: String,
    val resolution: Size,
    val sprites: Size,
)
