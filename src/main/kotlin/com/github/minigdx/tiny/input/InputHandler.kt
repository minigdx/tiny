package com.github.minigdx.tiny.input


interface InputHandler {

    /**
     * Is the [key] was pressed?
     *
     * It returns true once and will return true only if
     * the key is released then pressed again.
     *
     * This method should be used to count action trigger only
     * once (ie: starting an action like opening a door)
     */
    fun isKeyJustPressed(key: Key): Boolean

    /**
     * Is the [key] is actually pressed?
     *
     * This method should be used to know when the key is pressed
     * and running an action until the key is not released.
     * (ie: running while the key is pressed, stop when it's not)
     */
    fun isKeyPressed(key: Key): Boolean

    /**
     * Is any of [keys] passed in parameter are actually pressed?
     */
    fun isAnyKeysPressed(vararg keys: Key): Boolean = keys.any { isKeyPressed(it) }

    /**
     * Is all of [keys] passed in parameter are been just pressed?
     */
    fun isAllKeysPressed(vararg keys: Key): Boolean = keys.all { isKeyPressed(it) }

    /**
     * Is none of [keys] passed in parameter has been pressed?
     */
    fun isNoneKeysPressed(vararg keys: Key): Boolean = keys.none { isKeyPressed(it) }

    /**
     * Is [signal] touched on the screen?
     *
     * @return null if not touched, coordinates otherwise.
     */
    fun isTouched(signal: TouchSignal): Vector2?

    /**
     * Is [signal] just touched on the screen?
     *
     * @return null if not touched, coordinates of the touch in game screen coordinate.
     */
    fun isJustTouched(signal: TouchSignal): Vector2?

    /**
     * Keys pressed by the user but rendered as text.
     * Useful to capture text from the user.
     */
    fun textJustTyped(): String? = null

    /**
     * Position of the touch when there is no signal.
     *
     * It will be the mouse position on the web platform or the mouse
     * position on the desktop platform.
     *
     * The position can be keep by the consumer as the position vector will be updated.
     * If the cursor is outside of the game area, the position vector will not be updated,
     * it will keep the last position value of the touch position.
     *
     * On mobile devise, as this information is not available, it returns null.
     *
     * The position of the upper left corner is (0, 0) while the
     * bottom right corner will depends of the size of your game screen.
     *
     * Please note that the position will be regarding the game screen and not the device screen.
     * Which mean that even if the window of your game is resized, the coordinate of the bottom right
     * corner will NOT change.
     *
     * @return: position of the touch when idle.
     *          or null if not available on the current platform
     *          or null if outside of the game area.
     */
    fun touchIdlePosition(): Vector2? = null

    /**
     * Position of the current touch.
     *
     * It will be the mouse position on the web platform and desktop platform.
     * It will be the last touch position on the mobile platform.
     *
     * If the mouse is outside the game screen, the position will be the last one before
     * the mouse leave the game screen.
     *
     * To detect if the touch is still in the game screen, please use [touchIdlePosition] instead.
     *
     * @return: position of the current touch.
     */
    val currentTouch: Vector2
}
