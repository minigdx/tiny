package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GameOption(
    val width: Pixel = 256,
    val height: Pixel = 256,
    val zoom: Int = 2,
    val record: Seconds = 8f,
)
