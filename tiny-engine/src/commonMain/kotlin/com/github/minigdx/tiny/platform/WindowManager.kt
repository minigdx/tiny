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

    var ratioWidth: Int = screenWidth / windowWidth
        private set

    var ratioHeight: Int = screenHeight / windowHeight
        private set

    /**
     * Update screen dimensions when DPI changes (e.g., moving between Retina and standard displays).
     */
    fun updateScreenDimensions(
        newWidth: Int,
        newHeight: Int,
    ) {
        screenWidth = newWidth
        screenHeight = newHeight
        ratioWidth = newWidth / windowWidth
        ratioHeight = newHeight / windowHeight
    }
}
