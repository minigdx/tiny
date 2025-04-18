package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.toTinyException
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
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
import com.github.minigdx.tiny.sound.Song2
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.WaveGenerator
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

    private var debugEnabled: Boolean = true
    private val debugActions = mutableListOf<DebugAction>()

    private val notes = mutableListOf<WaveGenerator>()

    private var song: Song2? = null
    private var longuestDuration: Seconds = 0f

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
    private var current: Int = 0

    override val frameBuffer = FrameBuffer(gameOptions.width, gameOptions.height, gameOptions.colors())

    private var accumulator: Seconds = 0f

    lateinit var renderContext: RenderContext
    lateinit var inputHandler: InputHandler
    lateinit var inputManager: InputManager
    lateinit var soundManager: SoundManager

    private lateinit var resourceFactory: ResourceFactory

    fun main() {
        val windowManager = platform.initWindowManager()

        inputHandler = platform.initInputHandler()
        inputManager = platform.initInputManager()
        soundManager = platform.initSoundManager(inputHandler)

        resourceFactory = ResourceFactory(vfs, platform, logger, gameOptions.colors())

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

        val sounds = gameOptions.sounds.mapIndexed { index, soundName ->
            resourceFactory.soundEffect(index, soundName)
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
                        val isValid = try {
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
                            scripts[current]?.reload = true
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
                        scripts[current]?.reload = true
                    }

                    GAME_SOUND -> {
                        sounds[resource.index] = resource as Sound
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        with(scripts[current]) {
            if (this == null) return

            if (exited >= 0) {
                val previous = current
                // next script
                current = min(exited + 1, scripts.size - 1)
                try {
                    val state = getState()

                    logger.debug("GAME_ENGINE") {
                        "Stop $name to switch the next game script ${scripts[current]?.name}"
                    }
                    // Reevaluate the game to flush the previous state.
                    scripts[current]?.evaluate(customizeLuaGlobal)
                    scripts[current]?.setState(state)

                    listener?.switchScript(scripts[previous], scripts[current])
                } catch (ex: LuaError) {
                    popupError(ex.toTinyException(content.decodeToString()))
                }
            } else if (reload) {
                clear()
                // Stop all sounds to avoid annoying sound loop
                sounds.forEach { s -> s?.stop() }
                try {
                    val state = getState()
                    evaluate(customizeLuaGlobal)
                    setState(state)

                    listener?.reload(scripts[current])

                    inError = false
                } catch (ex: LuaError) {
                    popupError(ex.toTinyException(content.decodeToString()))
                }
            }

            soundManager.playNotes(notes, longuestDuration)
            notes.clear()
            longuestDuration = 0f

            song?.run { soundManager.playSong(this) }
            song = null

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inputManager.record()
                inError = try {
                    scripts[current]?.advance()
                    false
                } catch (ex: TinyException) {
                    if (!inError) { // display the log only once.
                        popupError(ex)
                    }
                    true
                }
                engineGameScript?.advance()
                accumulator -= REFRESH_LIMIT

                // The user hit Ctrl + R(ecord)
                if (inputHandler.isCombinationPressed(Key.CTRL, Key.R)) {
                    popup("recording GIF", "#00FF00")
                    platform.record()
                    // The user hit Ctrl + S(creenshot)
                } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.S)) {
                    popup("screenshot PNG", "#00FF00")
                    platform.screenshot()
                }

                var msgIndex = 0
                if (!debugEnabled) {
                    debugActions.clear()
                }
                debugActions.forEach { debugAction ->
                    when (debugAction) {
                        is DebugMessage -> {
                            val (msg, color) = debugAction
                            engineGameScript?.invoke(
                                "printDebug",
                                valueOf(msgIndex++),
                                valueOf(msg),
                                valueOf(color),
                            )
                        }

                        is DebugRect -> {
                            val (x, y, width, height, color) = debugAction
                            engineGameScript?.invoke(
                                "shape.rect",
                                valueOf(x),
                                valueOf(y),
                                valueOf(width),
                                valueOf(height),
                                valueOf(color),
                            )
                        }

                        is DebugEnabled -> Unit // NOP
                        is DebugLine -> {
                            val (x1, y1, x2, y2, color) = debugAction
                            engineGameScript?.invoke(
                                "shape.line",
                                valueOf(x1),
                                valueOf(y1),
                                valueOf(x2),
                                valueOf(y2),
                                valueOf(color),
                            )
                        }

                        is DebugPoint -> {
                            val (x, y, color) = debugAction
                            engineGameScript?.invoke(
                                "shape.circlef",
                                valueOf(x),
                                valueOf(y),
                                valueOf(2),
                                valueOf(color),
                            )
                        }
                    }
                }
                debugActions.clear()
                inputManager.reset()
            }
        }
    }

    override fun debug(action: DebugAction) {
        when (action) {
            is DebugEnabled -> {
                debugEnabled = action.enabled
            }

            else -> debugActions.add(action)
        }
    }

    private suspend fun GameEngine.popupError(ex: TinyException) {
        logger.warn(
            "TINY",
        ) {
            val error = "line ${ex.lineNumber}:${ex.line} <-- the \uD83D\uDC1E is around here (${ex.message})"
            "The line ${ex.lineNumber} trigger an execution error (${ex.message}). Please fix your script!\n" + error
        }
        val msg = "error line ${ex.lineNumber}:${ex.line} (${ex.message})"
        popup(msg, "#FF0000", true)
    }

    private suspend fun popup(message: String, color: String, forever: Boolean = false) {
        engineGameScript?.invoke("popup", valueOf(0), valueOf(message), valueOf(color), valueOf(forever))
    }

    private suspend fun clear() {
        engineGameScript?.invoke("clear")
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
        val protected = max(0, min(index, sounds.size - 1))
        if (protected >= sounds.size) return null
        return sounds[protected]
    }

    override fun note(wave: WaveGenerator) {
        longuestDuration = max(longuestDuration, wave.duration)
        notes.add(wave)
    }

    override fun sfx(song: Song2) {
        this.song = song
    }

    override fun script(name: String): GameScript? {
        return scripts
            .drop(1) // drop the _boot.lua
            .firstOrNull { script -> script?.name == name }
    }

    override fun draw() {
        platform.draw(renderContext, frameBuffer)
    }

    override fun end() {
        sounds.forEach { it?.stop() }
        soundManager.destroy()
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }
}
