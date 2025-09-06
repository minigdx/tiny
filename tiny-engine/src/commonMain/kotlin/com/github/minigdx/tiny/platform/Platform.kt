package com.github.minigdx.tiny.platform

import com.danielgergely.kgl.Kgl
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.sound.Music
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

class SoundData(
    // Name of the file.
    val name: String,
    // Deserialized data of the file.
    val music: Music,
    // Ready to play musical bars. (sfx)
    val musicalBars: List<FloatArray>,
    // Ready to play musical sequences (music)
    val musicalSequences: List<FloatArray> = emptyList(),
)

interface Platform {
    /**
     * Game Option from the game.
     */
    val gameOptions: GameOptions

    /**
     * Performance monitor for this platform
     */
    val performanceMonitor: PerformanceMonitor

    /**
     * Create the window where the game will render
     */
    fun initWindowManager(): WindowManager

    /**
     * Prepare the platform for the game loop
     */
    fun initRenderManager(windowManager: WindowManager): Kgl

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

    fun writeImage(buffer: ByteArray)

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
     * Create a SourceStream from a sfx file.
     */
    fun createSoundStream(
        name: String,
        soundManager: SoundManager,
    ): SourceStream<SoundData>

    /**
     * Create a file using the name.
     *
     * @param: name of the file, with the extension, if any.
     */
    fun createLocalFile(
        name: String,
        parentDirectory: String? = "data",
    ): LocalFile
}
