package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.errorLine
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import com.github.minigdx.tiny.resources.GameLevel
import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.GameScript
import com.github.minigdx.tiny.resources.ResourceFactory
import com.github.minigdx.tiny.resources.ResourceType
import com.github.minigdx.tiny.resources.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourceType.ENGINE_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_LEVEL
import com.github.minigdx.tiny.resources.ResourceType.GAME_SOUND
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue.Companion.valueOf
import kotlin.math.max
import kotlin.math.min

class ScriptsCollector(private val events: MutableList<GameResource>) : FlowCollector<GameResource> {

    private var bootscriptLoaded = false

    private val waitingList: MutableList<GameResource> = mutableListOf()

    private val loadedResources: MutableMap<ResourceType, MutableMap<Int, GameResource>> = mutableMapOf()

    override suspend fun emit(value: GameResource) {
        // The application has not yet booted.
        // But the boot script just got loaded
        if (value.type == BOOT_GAMESCRIPT && !bootscriptLoaded) {
            events.add(value)
            events.addAll(waitingList)
            waitingList.clear()
            bootscriptLoaded = true
        } else if (!bootscriptLoaded) {
            waitingList.add(value)
        } else {
            // Check if the resources is loading or reloaded
            val toReload = loadedResources[value.type]?.containsKey(value.index) == true
            if (!toReload) {
                loadedResources.getOrPut(value.type) { mutableMapOf() }[value.index] = value
            }
            events.add(
                value.apply {
                    reload = toReload
                }
            )
        }
    }
}

@OptIn(FlowPreview::class)
class GameEngine(
    val gameOptions: GameOptions,
    val platform: Platform,
    val vfs: VirtualFileSystem,
    val logger: Logger,
) : GameLoop, GameResourceAccess {

    private val events: MutableList<GameResource> = mutableListOf()
    private val workEvents: MutableList<GameResource> = mutableListOf()

    private var numberOfResources: Int = 0

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

    private lateinit var renderContext: RenderContext
    private lateinit var inputHandler: InputHandler
    private lateinit var inputManager: InputManager

    private lateinit var resourceFactory: ResourceFactory

    fun main() {
        val windowManager = platform.initWindowManager()

        inputHandler = platform.initInputHandler()
        inputManager = platform.initInputManager()
        platform.initSoundManager(inputHandler)

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
            resourceFactory.bootSpritesheet("_boot.png")
        ) + gameScripts + spriteSheets + gameLevels + sounds

        numberOfResources = resources.size

        logger.debug("GAME_ENGINE") { "Number of resources to load: $numberOfResources" }

        resourcesScope.launch {
            resources.asFlow()
                .flatMapMerge { resource -> resource }
                .collect(ScriptsCollector(events))
        }

        renderContext = platform.initRenderManager(windowManager)

        platform.gameLoop(this)
    }

    override fun advance(delta: Seconds) {
        workEvents.addAll(events)

        workEvents.forEach { resource ->
            // The resource is loading
            if (!resource.reload) {
                logger.info("GAME_ENGINE") { "Loaded ${resource.name} ${resource.type}" }
                when (resource.type) {
                    BOOT_GAMESCRIPT -> {
                        // Always put the boot script at the top of the stack
                        val bootScript = resource as GameScript
                        bootScript.resourceAccess = this
                        bootScript.evaluate()
                        scripts[0] = bootScript
                    }

                    GAME_GAMESCRIPT -> {
                        resource as GameScript
                        resource.resourceAccess = this
                        resource.evaluate()
                        scripts[resource.index] = resource
                    }

                    ENGINE_GAMESCRIPT -> {
                        // Don't put the engine script in the stack
                        engineGameScript = resource as GameScript
                        engineGameScript?.resourceAccess = this
                        engineGameScript?.evaluate()
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
                logger.info("GAME_ENGINE") { "Reload ${resource.name} ${resource.type}" }
                // The resource already has been loaded.
                when (resource.type) {
                    BOOT_GAMESCRIPT -> {
                        // Always put the boot script at the top of the stack
                        val bootScript = resource as GameScript
                        bootScript.resourceAccess = this
                        bootScript.evaluate()
                        scripts[0] = bootScript
                    }

                    GAME_GAMESCRIPT -> {
                        resource as GameScript
                        resource.resourceAccess = this
                        val isValid = try {
                            resource.isValid()
                            true
                        } catch (ex: LuaError) {
                            val errorLine = ex.errorLine()
                            logger.warn(
                                "TINY"
                            ) {
                                val error =
                                    errorLine?.let { (l, line) -> "line $l:$line <-- the \uD83D\uDC1E is around here (${ex.getLuaMessage()})" }
                                "The line ${ex.level} trigger an execution error (${ex.getLuaMessage()}). Please fix your script!\n" + error
                            }

                            errorLine?.let { (l, line) ->
                                popup("error line $l:$line (${ex.getLuaMessage()})", "#FF0000", true)
                            }

                            false
                        }
                        if (isValid) {
                            scripts[resource.index] = resource
                            clear()
                        }
                    }

                    ENGINE_GAMESCRIPT -> {
                        // Don't put the engine script in the stack
                        engineGameScript = resource as GameScript
                        engineGameScript?.resourceAccess = this
                        engineGameScript?.evaluate()
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
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        inputManager.record()

        with(scripts[current]) {
            if (this == null) return

            if (exited) {
                // next script
                current = min(current + 1, scripts.size - 1)
                val state = getState()

                logger.debug("GAME_ENGINE") {
                    "Stop $name to switch the next game script ${scripts[current]?.name}"
                }
                scripts[current]?.setState(state)
            } else if (reload) {
                clear()
                // Stop all sounds to avoid annoying sound loop
                sounds.forEach { s -> s?.stop() }
                val state = getState()
                evaluate()
                setState(state)
                inError = false
            }

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inError = try {
                    scripts[current]?.advance()
                    false
                } catch (ex: LuaError) {
                    if (!inError) { // display the log only once.
                        val errorLine = ex.errorLine()
                        logger.warn(
                            "TINY"
                        ) {
                            val error =
                                errorLine?.let { (l, line) -> "line $l:$line <-- the \uD83D\uDC1E is around here (${ex.getLuaMessage()})" }
                            "The line ${ex.level} trigger an execution error (${ex.getLuaMessage()}). Please fix your script!\n" + error
                        }
                        errorLine?.let { (l, line) ->
                            popup("error line $l:$line (${ex.getLuaMessage()})", "#FF0000", forever = true)
                        }
                    }
                    true
                }
                engineGameScript?.advance()
                accumulator -= REFRESH_LIMIT
            }
        }

        // The user hit Ctrl + R(ecord)
        if (inputHandler.isCombinationPressed(Key.CTRL, Key.R)) {
            popup("recording GIF", "#00FF00")
            platform.record()
            // The user hit Ctrl + S(creenshot)
        } else if (inputHandler.isCombinationPressed(Key.CTRL, Key.S)) {
            popup("screenshot PNG", "#00FF00")
            platform.screenshot()
        }
        inputManager.reset()
    }

    private fun popup(message: String, color: String, forever: Boolean = false) {
        engineGameScript?.invoke("popup", valueOf(0), valueOf(message), valueOf(color), valueOf(forever))
    }

    private fun clear() {
        engineGameScript?.invoke("clear")
    }

    override fun spritesheet(index: Int): SpriteSheet? {
        val protected = max(0, min(index, spriteSheets.size - 1))
        if (protected >= spriteSheets.size) return null
        return spriteSheets[protected]
    }

    override fun spritesheet(sheet: SpriteSheet) {
        if (sheet.index >= spriteSheets.size || sheet.index < 0) {
            spriteSheets = spriteSheets.copyOf(spriteSheets.size + 1)
            spriteSheets[spriteSheets.size - 1] = sheet
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

    override fun draw() {
        platform.draw(renderContext, frameBuffer)
    }

    override fun end() {
        sounds.forEach { it?.stop() }
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }
}
