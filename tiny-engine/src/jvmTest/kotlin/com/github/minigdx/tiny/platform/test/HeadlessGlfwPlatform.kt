package com.github.minigdx.tiny.platform.test

import com.danielgergely.kgl.KglLwjgl
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.InputStreamStream
import com.github.minigdx.tiny.file.JvmLocalFile
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.file.SoundDataSourceStream
import com.github.minigdx.tiny.file.SourceStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.PixelFormat.RGBA
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.log.LogLevel
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.SoundData
import com.github.minigdx.tiny.platform.WindowManager
import com.github.minigdx.tiny.platform.performance.PerformanceMonitor
import com.github.minigdx.tiny.sound.JavaSoundManager
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.math.min

interface GameController {
    fun step()

    fun captureScreen(): ByteArray

    fun compareWith(reference: ByteArray): Boolean

    fun saveScreenshot(name: String)

    fun compareWithReference(name: String): Boolean
}

class HeadlessGlfwPlatform(
    override val gameOptions: GameOptions,
    private val logger: Logger,
    private val vfs: VirtualFileSystem,
    private val workdirectory: File,
    private val jarResourcePrefix: String = "",
    private val testName: String = "test",
    private val generateReferences: Boolean = false,
    private val projectDirectory: File = File(System.getProperty("user.dir")),
) : Platform, GameController {
    override val performanceMonitor: PerformanceMonitor = HeadlessPerformanceMonitor()

    private var window: Long = 0
    private var lastFrame: Long = getTime()
    private var gameLoop: GameLoop? = null

    private val lwjglInputHandler = VirtualInputHandler()

    private fun getTime(): Long {
        return System.nanoTime() / 1000000
    }

    private fun getDelta(): Seconds {
        val time = getTime()
        val delta = (time - lastFrame)
        lastFrame = time
        return min(delta / 1000f, 1 / 60f)
    }

    override fun initWindowManager(): WindowManager {
        if (!GLFW.glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        GLFW.glfwDefaultWindowHints()

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

        // Make window invisible for headless mode
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)

        val windowWidth = (gameOptions.width + gameOptions.gutter.first * 2) * gameOptions.zoom
        val windowHeight = (gameOptions.height + gameOptions.gutter.first * 2) * gameOptions.zoom
        window = GLFW.glfwCreateWindow(
            windowWidth,
            windowHeight,
            "Tiny Headless",
            MemoryUtil.NULL,
            MemoryUtil.NULL,
        )
        if (window == MemoryUtil.NULL) {
            throw IllegalStateException("Failed to create the GLFW window")
        }

        GLFW.glfwMakeContextCurrent(window)

        val tmpWidth = MemoryUtil.memAllocInt(1)
        val tmpHeight = MemoryUtil.memAllocInt(1)
        GLFW.glfwGetWindowSize(window, tmpWidth, tmpHeight)

        val tmpFrameBufferWidth = MemoryUtil.memAllocInt(1)
        val tmpFrameBufferHeight = MemoryUtil.memAllocInt(1)
        GLFW.glfwGetFramebufferSize(window, tmpFrameBufferWidth, tmpFrameBufferHeight)

        GL.createCapabilities(true)

        return WindowManager(
            windowWidth = tmpWidth.get(),
            windowHeight = tmpHeight.get(),
            screenWidth = tmpFrameBufferWidth.get(),
            screenHeight = tmpFrameBufferHeight.get(),
        )
    }

    override fun initRenderManager(windowManager: WindowManager) = KglLwjgl

    override fun initInputManager(): InputManager {
        return lwjglInputHandler
    }

    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun initInputHandler(): InputHandler {
        return lwjglInputHandler
    }

    override fun gameLoop(gameLoop: GameLoop) {
        this.gameLoop = gameLoop
    }

    suspend fun advance(): Boolean {
        gameLoop?.advance(getDelta())
        gameLoop?.draw()
        return !GLFW.glfwWindowShouldClose(window)
    }

    override fun step() {
        runBlocking {
            gameLoop?.advance(1f / 60f)
            gameLoop?.draw()
        }
    }

    override fun endGameLoop() {
        GLFW.glfwTerminate()
    }

    override fun record() = Unit

    override fun screenshot() = Unit

    override fun captureScreen(): ByteArray {
        val width = gameOptions.width
        val height = gameOptions.height
        val buffer = ByteBuffer.allocateDirect(width * height * 4)

        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer)

        val result = ByteArray(width * height * 4)
        buffer.get(result)
        return result
    }

    override fun compareWith(reference: ByteArray): Boolean {
        val current = captureScreen()
        return current.contentEquals(reference)
    }

    override fun saveScreenshot(name: String) {
        val screenshot = captureScreen()
        val targetDir = if (generateReferences) {
            File(projectDirectory, ".references")
        } else {
            File(projectDirectory, ".test")
        }
        targetDir.mkdirs()

        val file = File(targetDir, "$name.raw")
        file.writeBytes(screenshot)
    }

    override fun compareWithReference(name: String): Boolean {
        val current = captureScreen()
        val referencesDir = File(projectDirectory, ".references")
        val referenceFile = File(referencesDir, "$name.raw")

        if (!referenceFile.exists()) {
            // If generating references, save the current screenshot as reference
            if (generateReferences) {
                saveScreenshot(name)
                return true
            }
            throw IllegalStateException("Reference file not found: ${referenceFile.absolutePath}")
        }

        val reference = referenceFile.readBytes()
        val matches = current.contentEquals(reference)

        // Always save current screenshot to .test directory for comparison
        val testDir = File(projectDirectory, ".test")
        testDir.mkdirs()
        val testFile = File(testDir, "$name.raw")
        testFile.writeBytes(current)

        return matches
    }

    override fun writeImage(buffer: ByteArray) = Unit

    private fun extractRGBA(imageData: ByteArray): ImageData {
        val image = ImageIO.read(ByteArrayInputStream(imageData))
        val width = image.width
        val height = image.height

        val rgb = image.getRGB(0, 0, width, height, null, 0, width)

        val result = ByteArray(width * height * RGBA)

        (0 until rgb.size).forEach { pixel ->
            val p = rgb[pixel]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val a = (p shr 24) and 0xFF

            result[pixel * RGBA + 0] = r.toByte()
            result[pixel * RGBA + 1] = g.toByte()
            result[pixel * RGBA + 2] = b.toByte()
            result[pixel * RGBA + 3] = a.toByte()
        }
        return ImageData(result, width, height)
    }

    override fun createByteArrayStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ByteArray> {
        val resourceName =
            if (canUseJarPrefix) {
                "$jarResourcePrefix/$name"
            } else {
                "/$name"
            }

        val fromJar = HeadlessGlfwPlatform::class.java.getResourceAsStream(resourceName)
        return if (fromJar != null) {
            InputStreamStream(fromJar)
        } else {
            FileStream(workdirectory.resolve(name))
        }
    }

    override fun createImageStream(
        name: String,
        canUseJarPrefix: Boolean,
    ): SourceStream<ImageData> {
        return object : SourceStream<ImageData> {
            private val delegate = createByteArrayStream(name, canUseJarPrefix)

            override suspend fun exists(): Boolean = delegate.exists()

            override suspend fun read(): ImageData {
                return extractRGBA(delegate.read())
            }

            override fun wasModified(): Boolean = delegate.wasModified()
        }
    }

    override fun createSoundStream(
        name: String,
        soundManager: SoundManager,
    ): SourceStream<SoundData> {
        return SoundDataSourceStream(name, soundManager, createByteArrayStream(name))
    }

    override fun initSoundManager(inputHandler: InputHandler): SoundManager {
        return JavaSoundManager().also {
            it.initSoundManager(inputHandler)
        }
    }

    override fun createLocalFile(
        name: String,
        parentDirectory: String?,
    ): LocalFile =
        JvmLocalFile(
            name,
            parentDirectory?.let { workdirectory.resolve(it) } ?: workdirectory,
        )
}

fun headlessGlfwTest(
    name: String,
    script: String,
    block: (GameController) -> Unit,
) {
    headlessGlfwTest(name, script, 10 to 10, false, block)
}

fun headlessGlfwTest(
    name: String,
    script: String,
    size: Pair<Int, Int>,
    block: (GameController) -> Unit,
) {
    headlessGlfwTest(name, script, size, false, block)
}

fun headlessGlfwTest(
    name: String,
    script: String,
    size: Pair<Int, Int>,
    generateReferences: Boolean,
    block: (GameController) -> Unit,
) {
    val colors = listOf(
        "#000000",
        "#FFFFFF",
        "#FF0000",
    )

    val (w, h) = size
    val gameOptions = GameOptions(w, h, colors, listOf("game.lua"), emptyList())

    val tempDir = createTempDirectory("headless-glfw-test").toFile()
    tempDir.deleteOnExit()

    val gameFile = File(tempDir, "game.lua")
    gameFile.writeText(script)

    val bootFile = File(tempDir, "_boot.lua")
    bootFile.writeText("tiny.exit(0)")

    val engineFile = File(tempDir, "_engine.lua")
    engineFile.writeText("")

    val logger = StdOutLogger("headless-test", LogLevel.NONE)

    val platform = HeadlessGlfwPlatform(
        gameOptions = gameOptions,
        logger = logger,
        vfs = com.github.minigdx.tiny.file.CommonVirtualFileSystem(),
        workdirectory = tempDir,
        testName = name,
        generateReferences = generateReferences,
    )

    try {
        val engine = com.github.minigdx.tiny.engine.GameEngine(
            gameOptions = gameOptions,
            platform = platform,
            vfs = com.github.minigdx.tiny.file.CommonVirtualFileSystem(),
            logger = logger,
        )

        engine.main()

        block(platform)
    } finally {
        platform.endGameLoop()
        tempDir.deleteRecursively()
    }
}
