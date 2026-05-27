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
    screenWidth: Int,
    /**
     * Height of the window on the screen (resolution dependent)
     */
    screenHeight: Int,
) {
    var screenWidth: Int = screenWidth
        private set

    var screenHeight: Int = screenHeight
        private set

    /**
     * Update screen dimensions when the framebuffer size changes
     * (DPI changes, window resize, or entering/leaving fullscreen).
     */
    fun updateScreenDimensions(
        newWidth: Int,
        newHeight: Int,
    ) {
        screenWidth = newWidth
        screenHeight = newHeight
    }
}
