package com.github.minigdx.tiny.platform

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.sound.Sound
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.CoroutineDispatcher

class ImageData(
    // Array of byte of RGBA color
    val data: ByteArray,
    // Width of the Image
    val width: Pixel,
    // Height of the Image
    val height: Pixel,
)

class SoundData(val name: String, val sound: Sound)

interface Platform {
    /**
     * Game Option from the game.
     */
    val gameOptions: GameOptions

    /**
     * Create the window where the game will render
     */
    fun initWindowManager(): WindowManager

    /**
     * Prepare the platform for the game loop
     */
    fun initRenderManager(windowManager: WindowManager): RenderContext

    /**
     * Let's run the game loop
     */
    fun gameLoop(gameLoop: GameLoop)

    /**
     * Save the last 30 seconds of the game.
     */
    fun record() = Unit

    /**
     * Generate a screenshoot of the actual frame.
     */
    fun screenshot() = Unit

    /**
     * The game loop stopped.
     * Game is existing.
     */
    fun endGameLoop()

    /**
     * Initialise the input manager.
     */
    fun initInputHandler(): InputHandler

    fun initInputManager(): InputManager

    /**
     * Initialise the sound manager.
     */
    fun initSoundManager(inputHandler: InputHandler): SoundManager

    /**
     * Create Coroutine Dispatcher dedicated for the IO.
     */
    fun io(): CoroutineDispatcher

    /**
     * Create a SourceStream from the name of the resource.
     * Regarding the platform, the name can be adjusted.
     */
    fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean = true,
    ): SourceStream<ByteArray>

    /**
     * Create a SourceStream from an image from uncompressed data.
     */
    fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean = true,
    ): SourceStream<ImageData>

    /**
     * Create a SourceStream from a midi file.
     */
    fun createSoundStream(name: String): SourceStream<SoundData>

    /**
     * Create a file using the name.
     *
     * @param: name of the file, with the extension, if any.
     */
    fun createLocalFile(name: String): LocalFile

    /**
     * Render the operations into the frame buffer.
     */
    fun render(
        renderContext: RenderContext,
        ops: List<RenderOperation>,
    )

    /**
     * Draw the frame buffer on the screen.
     */
    fun draw(renderContext: RenderContext)
}
