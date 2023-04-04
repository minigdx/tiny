package com.github.minigdx.tiny.platform

actual class WindowManager(
    val windowWidth: Int,
    val windowHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val ratioWidth: Int = screenWidth / windowWidth,
    val ratioHeight: Int = screenHeight / windowHeight,
)
