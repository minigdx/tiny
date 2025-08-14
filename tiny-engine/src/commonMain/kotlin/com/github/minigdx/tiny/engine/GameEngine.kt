package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.internal.ObjectPool
import com.github.minigdx.tiny.input.internal.PoolObject
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.toTinyException
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.performance.PerformanceMetrics
import com.github.minigdx.tiny.render.RenderContext
import com.github.minigdx.tiny.render.RenderFrame
import com.github.minigdx.tiny.render.RenderUnit
import com.github.minigdx.tiny.render.operations.DrawSprite
import com.github.minigdx.tiny.render.operations.RenderOperation
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.ResourceFactory
import com.github.minigdx.tiny.resources.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourceType.ENGINE_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_LEVEL
import com.github.minigdx.tiny.resources.ResourceType.GAME_SOUND
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class GameEngine(
    val gameOptions: GameOptions,
    val platform: Platform,
    val vfs: VirtualFileSystem,
    val logger: Logger,
    val customizeLuaGlobal: GameResourceAccess.(Globals) -> Unit = {},
    val listener: GameEngineListener? = null,
) : GameLoop, GameResourceAccess {
    private val events: MutableList<GameResource> = mutableListOf()
    private val workEvents: MutableList<GameResource> = mutableListOf()

    private var numberOfResources: Int = 0

    private val ops = mutableListOf<RenderOperation>()

    private lateinit var scripts: Array<GameScript?>
    private lateinit var spriteSheets: Array<SpriteSheet?>
    private lateinit var levels: Array<GameLevel?>
    private lateinit var sounds: Array<Sound?>

    override var bootSpritesheet: SpriteSheet? = null
        private set

    private var engineGameScript: GameScript? = null

    private var inError = false

    /**
     * Current script by index.
     */
    private var currentScriptIndex: Int = 0

    override val frameBuffer = FrameBuffer(gameOptions.width, gameOptions.height, gameOptions.colors())

    private var accumulator: Seconds = 0f
    private var currentFrame: Long = 0L
    private var currentMetrics: PerformanceMetrics? = null

    lateinit var renderContext: RenderContext
    lateinit var inputHandler: InputHandler
    lateinit var inputManager: InputManager
    lateinit var soundManager: SoundManager

    private lateinit var resourceFactory: ResourceFactory

    private val operationFactory = OperationsObjectPool()

    fun main() {
        val windowManager = platform.initWindowManager()

        inputHandler = platform.initInputHandler()
        inputManager = platform.initInputManager()
        soundManager = platform.initSoundManager(inputHandler)

        resourceFactory = ResourceFactory(
            vfs = vfs,
            platform = platform,
            logger = logger,
            colorPalette = gameOptions.colors(),
        )

        val resourcesScope = CoroutineScope(platform.io())

        val gameScripts = gameOptions.gameScripts.mapIndexed { index, script ->
            resourceFactory.gamescript(index + 1, script, inputHandler, gameOptions)
        }
        this.scripts = Array(gameScripts.size + 1) { null }

        val spriteSheets = gameOptions.spriteSheets.mapIndexed { index, sheet ->
            resourceFactory.gameSpritesheet(index, sheet)
        }
        this.spriteSheets = Array(spriteSheets.size) { null }

        val gameLevels = gameOptions.gameLevels.mapIndexed { index, level ->
            resourceFactory.gameLevel(index, level)
        }
        this.levels = Array(gameLevels.size) { null }

        val sounds = gameOptions.sounds.mapIndexed { index, sound ->
            resourceFactory.soundEffect(index, sound)
        }
        this.sounds = Array(sounds.size) { null }

        val resources = listOf(
            resourceFactory.bootscript("_boot.lua", inputHandler, gameOptions),
            resourceFactory.enginescript("_engine.lua", inputHandler, gameOptions),
            resourceFactory.bootSpritesheet("_boot.png"),
        ) + gameScripts + spriteSheets + gameLevels + sounds

        numberOfResources = resources.size

        logger.debug("GAME_ENGINE") { "Number of resources to load: $numberOfResources" }

        resourcesScope.launch {
            resources.asFlow()
                .flatMapMerge(concurrency = 128) { resource -> resource }
                .collect(ScriptsCollector(events))
        }

        renderContext = platform.initRenderManager(windowManager)

        platform.gameLoop(this)
    }

    override suspend fun advance(delta: Seconds) {
        platform.performanceMonitor.frameStart()
        platform.performanceMonitor.operationStart("game_update")

        workEvents.addAll(events)

        workEvents.forEach { resource ->
            // The resource is loading
            if (!resource.reload) {
                logger.info("GAME_ENGINE") { "Loaded ${resource.name} ${resource.type} (version: ${resource.version})" }
                when (resource.type) {
                    BOOT_GAMESCRIPT -> {
                        // Always put the boot script at the top of the stack
                        val bootScript = resource as GameScript
                        bootScript.resourceAccess = this
                        bootScript.evaluate(customizeLuaGlobal)
                        scripts[0] = bootScript
                    }

                    GAME_GAMESCRIPT -> {
                        resource as GameScript
                        resource.resourceAccess = this
                        // Game script will be evaluated when the boot script will exit
                        scripts[resource.index] = resource
                    }

                    ENGINE_GAMESCRIPT -> {
                        // Don't put the engine script in the stack
                        engineGameScript = resource as GameScript
                        engineGameScript?.resourceAccess = this
                        engineGameScript?.evaluate(customizeLuaGlobal)
                    }

                    BOOT_SPRITESHEET -> {
                        bootSpritesheet = resource as SpriteSheet
                    }

                    GAME_SPRITESHEET -> {
                        spriteSheets[resource.index] = resource as SpriteSheet
                    }

                    GAME_LEVEL -> {
                        levels[resource.index] = resource as GameLevel
                    }

                    GAME_SOUND -> {
                        sounds[resource.index] = resource as Sound
                    }
                }
                numberOfResources--
                logger.debug("GAME_ENGINE") { "Remaining resources to load: $numberOfResources." }
                if (numberOfResources == 0) {
                    logger.debug("GAME_ENGINE") { "All resources are loaded. Notify the boot script." }
                    // Force to notify the boot script
                    scripts[0]!!.resourcesLoaded()
                }
            } else {
                logger.info("GAME_ENGINE") { "Reload ${resource.name} ${resource.type} (version: ${resource.version})" }
                // The resource already has been loaded.
                when (resource.type) {
                    BOOT_GAMESCRIPT -> {
                        // Always put the boot script at the top of the stack
                        val bootScript = resource as GameScript
                        bootScript.resourceAccess = this
                        bootScript.evaluate(customizeLuaGlobal)
                        scripts[0] = bootScript
                    }

                    GAME_GAMESCRIPT -> {
                        resource as GameScript
                        resource.resourceAccess = this
                        val isValid =
                            try {
                                resource.isValid(customizeLuaGlobal)
                                true
                            } catch (ex: LuaError) {
                                popupError(ex.toTinyException(resource.content.decodeToString()))
                                false
                            }
                        if (isValid) {
                            scripts[resource.index] = resource
                            // Force the reloading of the script, as the script update might be used as resource of
                            // the current game script.
                            scripts[currentScriptIndex]?.reload = true
                            clear()
                        }
                    }

                    ENGINE_GAMESCRIPT -> {
                        // Don't put the engine script in the stack
                        engineGameScript = resource as GameScript
                        engineGameScript?.resourceAccess = this
                        engineGameScript?.evaluate(customizeLuaGlobal)
                    }

                    BOOT_SPRITESHEET -> {
                        bootSpritesheet = resource as SpriteSheet
                    }

                    GAME_SPRITESHEET -> {
                        spriteSheets[resource.index] = resource as SpriteSheet
                    }

                    GAME_LEVEL -> {
                        levels[resource.index] = resource as GameLevel
                        // Force the reloading of the script as level init might occur in the _init block.
                        scripts[currentScriptIndex]?.reload = true
                    }

                    GAME_SOUND -> {
                        sounds[resource.index] = resource as Sound
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        with(scripts[currentScriptIndex]) {
            if (this == null) return

            if (exited >= 0) {
                val previous = currentScriptIndex
                // next script
                currentScriptIndex = min(exited + 1, scripts.size - 1)
                try {
                    val state = getState()

                    logger.debug("GAME_ENGINE") {
                        "Stop $name to switch the next game script ${scripts[currentScriptIndex]?.name}"
                    }
                    // Reevaluate the game to flush the previous state.
                    scripts[currentScriptIndex]?.evaluate(customizeLuaGlobal)
                    scripts[currentScriptIndex]?.setState(state)

                    listener?.switchScript(scripts[previous], scripts[currentScriptIndex])
                } catch (ex: LuaError) {
                    popupError(ex.toTinyException(content.decodeToString()))
                }
            } else if (reload) {
                clear()
                try {
                    val state = getState()
                    evaluate(customizeLuaGlobal)
                    setState(state)

                    listener?.reload(scripts[currentScriptIndex])

                    inError = false
                } catch (ex: LuaError) {
                    popupError(ex.toTinyException(content.decodeToString()))
                }
            }

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inputManager.record()
                inError =
                    try {
                        ops.clear() // Remove all drawing operation to prepare the new frame.
                        scripts[currentScriptIndex]?.advance()
                        false
                    } catch (ex: TinyException) {
                        if (!inError) { // display the log only once.
                            popupError(ex)
                        }
                        true
                    }
                engineGameScript?.advance()
                accumulator -= REFRESH_LIMIT
                currentFrame++

                // The user hit Ctrl + R(ecord)
                if (inputHandler.isCombinationPressed(Key.CTRL, Key.R)) {
                    popup("recording GIF", "#00FF00")
                    platform.record()
                    // The user hit Ctrl + S(creenshot)
                } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.S)) {
                    popup("screenshot PNG", "#00FF00")
                    platform.screenshot()
                } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.P)) {
                    platform.performanceMonitor.isEnabled = !platform.performanceMonitor.isEnabled
                    val message = if (platform.performanceMonitor.isEnabled) {
                        "enable the profiler (Ctrl+P to disabled)"
                    } else {
                        "disabled the profiler"
                    }
                    popup(message, "#00FF00")
                }
                currentMetrics?.run { storeFrameMetrics(this) }

                inputManager.reset()
            }
        }

        // End performance monitoring for game update
        val updateTime = platform.performanceMonitor.operationEnd("game_update")

        logPerformanceMetrics(updateTime)
    }

    private suspend fun GameEngine.popupError(ex: TinyException) {
        logger.warn(
            "TINY",
        ) {
            val error = "line ${ex.lineNumber}:${ex.line} <-- the \uD83D\uDC1E is around here (${ex.message})"
            "The line ${ex.lineNumber} trigger an execution error (${ex.message}). " +
                "Please fix the script ${ex.name}!\n" + error
        }
        val msg = "error line ${ex.lineNumber}:${ex.line} (${ex.message})"
        popup(msg, "#FF0000", true)
    }

    private suspend fun popup(
        message: String,
        color: String,
        forever: Boolean = false,
    ) {
        engineGameScript?.invoke("popup", valueOf(0), valueOf(message), valueOf(color), valueOf(forever))
    }

    private suspend fun clear() {
        engineGameScript?.invoke("clear")
    }

    /**
     * Log performance metrics during game update
     */
    private fun logPerformanceMetrics(updateTime: Double) {
        // Only log every 60 frames to avoid spam
        if (platform.performanceMonitor.isEnabled && (currentFrame % 60 == 0L)) {
            val averageMetrics = platform.performanceMonitor.getAverageMetrics(60)
            if (averageMetrics != null) {
                logger.debug("PERFORMANCE") {
                    val fps = ((averageMetrics.fps * 10).toInt() / 10.0).toString().padStart(6)
                    val frameTime = ((averageMetrics.frameTime * 100).toInt() / 100.0).toString().padStart(6)
                    val updateTimeFormatted = ((updateTime * 100).toInt() / 100.0).toString().padStart(6)
                    val memory = (((averageMetrics.memoryUsed / 1024.0 / 1024.0) * 10).toInt() / 10.0).toString().padStart(6)
                    val drawCalls = averageMetrics.drawCalls.toString().padStart(6)
                    val readPixels = averageMetrics.readPixels.toString().padStart(6)
                    val drawOnScreen = averageMetrics.drawOnScreen.toString().padStart(6)

                    "\n┌─────────────────┬────────┐\n" +
                        "│ FPS             │ $fps │\n" +
                        "│ Frame Time      │ ${frameTime}ms │\n" +
                        "│ Update Time     │ ${updateTimeFormatted}ms │\n" +
                        "│ Memory          │ ${memory}MB │\n" +
                        "│ Draw Calls      │ $drawCalls │\n" +
                        "│ Read Pixels     │ $readPixels │\n" +
                        "│ Draw On Screen  │ $drawOnScreen │\n" +
                        "└─────────────────┴────────┘"
                }
            }
        }
    }

    /**
     * Store frame metrics for debugging visualization
     */
    private suspend fun storeFrameMetrics(metrics: PerformanceMetrics) {
        if (!platform.performanceMonitor.isEnabled) {
            return
        }
        // Add performance debug messages
        if (metrics.fps < 30) {
            popup("LOW FPS: ${metrics.fps}", "#FF0000")
        }

        if (metrics.memoryAllocated > 1024 * 1024) { // More than 1MB allocated
            popup("HIGH ALLOC: ${(metrics.memoryAllocated / 1024 / 1024)}MB", "#FFAA00")
        }

        // Show frame time if it's high
        if (metrics.frameTime > 16.67) { // Slower than 60 FPS
            popup("Frame: ${metrics.frameTime}ms", "#AAAA00")
        }
    }

    override fun spritesheet(index: Int): SpriteSheet? {
        val protected = max(0, min(index, spriteSheets.size - 1))
        if (protected >= spriteSheets.size) return null
        return spriteSheets[protected]
    }

    override fun spritesheet(name: String): Int? {
        return spriteSheets
            .indexOfFirst { it?.name == name }
            .takeIf { it >= 0 }
    }

    override fun newSpritesheetIndex(): Int {
        return spriteSheets.size
    }

    override fun spritesheet(sheet: SpriteSheet) {
        if (sheet.index < 0) {
            // The index is negative. Let's copy it at the last place.
            spriteSheets = spriteSheets.copyOf(spriteSheets.size + 1)
            spriteSheets[spriteSheets.size - 1] = sheet
        } else if (sheet.index >= spriteSheets.size) {
            require(sheet.index <= 256) { "Tiny support only 256 spritesheets" }
            spriteSheets = spriteSheets.copyOf(sheet.index + 1)
            spriteSheets[sheet.index] = sheet
        } else {
            spriteSheets[sheet.index] = sheet
        }
    }

    override fun level(index: Int): GameLevel? {
        val protected = max(0, min(index, levels.size - 1))
        if (protected >= levels.size) return null
        return levels[protected]
    }

    override fun sound(index: Int): Sound? {
        return sounds.getOrNull(index.coerceIn(0, sounds.size - 1))
    }

    override fun sound(name: String): Sound? {
        val index = sounds.indexOfFirst { it?.data?.name == name }
            .takeIf { it >= 0 }
            ?: return null

        return sound(index)
    }

    override fun play(musicalBar: MusicalBar): SoundHandler {
        return soundManager.createSoundHandler(musicalBar).also { it.play() }
    }

    override fun play(musicalSequence: MusicalSequence): SoundHandler {
        return soundManager.createSoundHandler(musicalSequence).also { it.play() }
    }

    override fun play(track: MusicalSequence.Track): SoundHandler {
        return soundManager.createSoundHandler(track).also { it.play() }
    }

    override fun exportAsSound(sequence: MusicalSequence) {
        soundManager.exportAsSound(sequence)
    }

    override fun script(name: String): GameScript? {
        return scripts
            .drop(1) // drop the _boot.lua
            .firstOrNull { script -> script?.name == name }
    }

    override fun addOp(op: RenderOperation) {
        renderFrame = null // invalid the previous render frame
        val last = ops.lastOrNull()
        if (op.mergeWith(last)) {
            return
        }

        if (last == null || op.target.compatibleWith(last.target)) {
            ops.add(op)
        } else {
            println("last -> $last ; $op")
            // Render only the framebuffer OR GPU operations
            render()
            ops.add(op)
        }
    }

    override fun renderAsBuffer(block: () -> Unit): FrameBuffer {
        // Render on the screen to clean up render states.
        render()

        val renderFrame = platform.executeOffScreen(renderContext) {
            block.invoke()
            render()
        }

        val buffer = FrameBuffer(gameOptions.width, gameOptions.height, gameOptions.colors())
        renderFrame.copyInto(buffer.colorIndexBuffer)
        return buffer
    }

    /**
     * Will render the remaining operations on the screen.
     */
    override fun draw() {
        platform.performanceMonitor.operationStart("render")
        render() // Render the last operation into the frame buffer
        platform.performanceMonitor.operationEnd("render")

        platform.performanceMonitor.operationStart("draw")
        platform.draw(renderContext)
        platform.performanceMonitor.operationEnd("draw")

        // Complete frame monitoring and get metrics
        currentMetrics = platform.performanceMonitor.frameEnd()
    }

    /**
     * Will render the actual operations in the frame buffer
     */
    fun render() {
        val last = ops.lastOrNull() ?: return

        val frameBufferRender = last.target.compatibleWith(RenderUnit.CPU)
        if (frameBufferRender) {
            // The remaining operations are only for the CPU.
            // Let's execute it now.
            ops.forEach {
                check(it.target.compatibleWith(RenderUnit.CPU)) { "Expected only ops than can be executed on CPU!" }
            }
            ops.clear()
            // The framebuffer will be the next render operation
            val op = DrawSprite.from(
                this,
                frameBuffer.asSpriteSheet,
                0,
                0,
                frameBuffer.width,
                frameBuffer.height,
            )
            ops.add(op)
        }

        // Render operations in the GPU frame buffer
        platform.render(renderContext, ops)

        // The framebuffer has been rendered.
        // It can be reset.
        if (frameBufferRender) {
            frameBuffer.clear()
        }
        ops.clear()
    }

    private var renderFrame: RenderFrame? = null

    override fun readPixel(
        x: Int,
        y: Int,
    ): ColorIndex {
        if (renderFrame == null) {
            render()
            renderFrame = platform.readRender(renderContext)
        }
        return renderFrame?.getPixel(x, y) ?: ColorPalette.TRANSPARENT_INDEX
    }

    override fun readFrame(): FrameBuffer {
        if (renderFrame == null) {
            render()
            renderFrame = platform.readRender(renderContext)
        }
        val copy = FrameBuffer(frameBuffer.width, frameBuffer.height, frameBuffer.gamePalette)
        renderFrame?.copyInto(copy.colorIndexBuffer)
        return copy
    }

    override fun end() {
        soundManager.destroy()
    }

    override fun save(
        filename: String,
        content: String,
    ) {
        platform.createLocalFile(filename, null).save(content.encodeToByteArray())
    }

    override fun <T : PoolObject<T>> obtain(type: KClass<T>): T {
        return operationFactory.obtain(type)
    }

    override fun <T : PoolObject<T>> releaseOperation(
        operation: T,
        type: KClass<T>,
    ) {
        operationFactory.release(operation, type)
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }
}

class OperationsObjectPool {
    private abstract class PoolObjectPool<T : PoolObject<T>>(size: Int) : ObjectPool<T>(size) {
        abstract fun new(): T

        abstract fun destroy(obj: T)

        override fun newInstance(): T {
            return new().also {
                it.pool = this
            }
        }

        override fun destroyInstance(obj: T) {
            destroy(obj)
            obj.pool = null
        }
    }

    private val drawSprite =
        object : PoolObjectPool<DrawSprite>(256) {
            override fun new(): DrawSprite {
                return DrawSprite()
            }

            override fun destroy(obj: DrawSprite) {
                obj.source = null
                obj.dither = 0xFFFF
                obj.pal = emptyArray()
                obj.camera = null
                obj.clipper = null
                obj._attributes.forEach { attribute -> attribute.release() }
                obj._attributes.clear()
            }
        }

    private val drawSpriteAttribute =
        object : PoolObjectPool<DrawSprite.DrawSpriteAttribute>(2048) {
            override fun new(): DrawSprite.DrawSpriteAttribute {
                return DrawSprite.DrawSpriteAttribute()
            }

            override fun destroy(obj: DrawSprite.DrawSpriteAttribute) {
                obj.sourceX = 0
                obj.sourceY = 0
                obj.sourceWidth = 0
                obj.sourceHeight = 0
                obj.destinationX = 0
                obj.destinationY = 0
                obj.flipX = false
                obj.flipY = false
            }
        }

    private fun <T : PoolObject<T>> getPool(type: KClass<T>): ObjectPool<T> {
        @Suppress("UNCHECKED_CAST")
        val pool: ObjectPool<T> =
            when (type) {
                DrawSprite::class -> drawSprite as ObjectPool<T>
                DrawSprite.DrawSpriteAttribute::class -> drawSpriteAttribute as ObjectPool<T>
                else -> throw IllegalArgumentException("No pool found for type: $type")
            }
        return pool
    }

    fun <T : PoolObject<T>> obtain(type: KClass<T>): T {
        return getPool(type).obtain()
    }

    fun <T : PoolObject<T>> release(
        operation: T,
        type: KClass<T>,
    ) {
        getPool(type).destroyInstance(operation)
    }
}
