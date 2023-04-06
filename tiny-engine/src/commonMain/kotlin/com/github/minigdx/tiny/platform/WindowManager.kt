package com.github.minigdx.tiny.platform

/**
 * Configuration of the window in which the game is displayed.
 */
class WindowManager(
    /**
     * Width of the window
     */
    val windowWidth: Int,
    /**
     * Height of the window
     */
    val windowHeight: Int,
    /**
     * Width of the window on the screen (resolution dependent)
     */
    val screenWidth: Int,
    /**
     * Height of the window on the screen (resolution dependent)
     */
    val screenHeight: Int,
    val ratioWidth: Int = screenWidth / windowWidth,
    val ratioHeight: Int = screenHeight / windowHeight,
)
