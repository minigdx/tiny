package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.InputManager
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.log.Logger
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
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourcesState
import com.github.minigdx.tiny.resources.ResourcesState.BOOT
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue.Companion.valueOf

class ScriptsCollector(private val events: MutableList<GameScript>) : FlowCollector<GameScript> {

    private val scriptsByName = mutableMapOf<ResourceType, GameScript>()

    private var bootscriptLoaded = false

    override suspend fun emit(value: GameScript) {
        val script = scriptsByName[value.type]
        // New script. The content will have to be loaded by the GameEngine.
        // It's added in the script stack
        if (script == null) {
            scriptsByName[value.type] = value
            // The boot script is ready. Let's add everyone!
            if (value.type == BOOT_GAMESCRIPT && !bootscriptLoaded) {
                events.add(value) // first the boot script
                // then the game script.
                scriptsByName[GAME_GAMESCRIPT]?.let { gamescript ->
                    events.add(gamescript)
                }
                scriptsByName[ENGINE_GAMESCRIPT]?.let { gameScript ->
                    events.add(gameScript)
                }
                bootscriptLoaded = true
            } else if (bootscriptLoaded) {
                events.add(value)
            }
        } else {
            // Ignore the script until the bootscript is loaded
            if (bootscriptLoaded) {
                events.add(value.apply { reloaded = true })
            }
        }
    }

}

@OptIn(FlowPreview::class)
class GameEngine(
    val gameOption: GameOption,
    val platform: Platform,
    val vfs: VirtualFileSystem,
    val logger: Logger,
) : GameLoop {

    private val scripts: MutableList<GameScript> = mutableListOf()
    private val scriptsByName: MutableMap<String, GameScript> = mutableMapOf()

    private val events: MutableList<GameScript> = mutableListOf()
    private val workEvents: MutableList<GameScript> = mutableListOf()

    private var resources = emptyMap<ResourceType, GameResource>()
    private var spriteSheets = emptyMap<ResourceType, SpriteSheet>()
    private var levels = emptyMap<ResourceType, GameLevel>()

    private var resourcesState: ResourcesState = BOOT

    private var current: GameScript? = null

    private val frameBuffer = FrameBuffer(gameOption.width, gameOption.height, gameOption.colors())

    private var accumulator: Seconds = 0f

    private lateinit var engineGameScript: GameScript

    private lateinit var renderContext: RenderContext
    private lateinit var inputHandler: InputHandler
    private lateinit var inputManager: InputManager

    private lateinit var resourceFactory: ResourceFactory

    fun main() {
        val windowManager = platform.initWindowManager()

        inputHandler = platform.initInputHandler()
        inputManager = platform.initInputManager()

        resourceFactory = ResourceFactory(vfs, platform, logger, gameOption.colors())

        val resourcesScope = CoroutineScope(platform.io())

        val scripts = listOf(
            resourceFactory.bootscript("_boot.lua", inputHandler, gameOption),
            resourceFactory.gamescript("game.lua", inputHandler, gameOption),
            resourceFactory.enginescript("_engine.lua", inputHandler, gameOption),
        )

        resourcesScope.launch {
            scripts.asFlow()
                .flatMapMerge { script -> script }
                .collect(ScriptsCollector(events))
        }

        val resourcesName = listOf(
            resourceFactory.bootSpritesheet("_boot.png"),
            resourceFactory.gameSpritesheet("game.png"),
            resourceFactory.gameLevel("platform/simplified/Level_0"),
        )

        resourcesScope.launch {
            resourcesName.asFlow()
                .flatMapMerge { flow -> flow }
                .collect { resource ->
                    resources += resource.type to resource
                    if (resource.type in setOf(BOOT_SPRITESHEET, GAME_SPRITESHEET)) {
                        spriteSheets += resource.type to (resource as SpriteSheet)
                    }
                    if (resource.type == GAME_LEVEL) {
                        levels += resource.type to (resource as GameLevel)
                    }
                    if (resources.size == resourcesName.size && resourcesState == BOOT) {
                        resourcesState = ResourcesState.BOOTED
                    }
                }

        }

        renderContext = platform.initRenderManager(windowManager)

        platform.gameLoop(this)
    }

    private var inError = false

    override fun advance(delta: Seconds) {
        workEvents.addAll(events)

        workEvents.forEach { gameScript ->
            // If the script is the engine script, don't add it to the stack.
            if(gameScript.type == ENGINE_GAMESCRIPT) {
                engineGameScript = gameScript
                // Prepare the custom game script used only by the engine
                engineGameScript.evaluate()
                engineGameScript.frameBuffer = frameBuffer
                engineGameScript.spriteSheets = this@GameEngine.spriteSheets
            } else if (!gameScript.reloaded) {
                // First time script loading. Adding it to the stack of script
                logger.debug("GAME_ENGINE") { "Add ${gameScript.name} to the game script stack" }
                scripts.add(gameScript)
                scriptsByName[gameScript.name] = gameScript
                if (current == null) {
                    current = scripts.firstOrNull()
                    current?.frameBuffer = frameBuffer
                }
            } else {
                // The script is already in the stack. Time to update it.
                scriptsByName[gameScript.name]?.run {
                    if (isValid(gameScript.content)) {
                        reloaded = true
                        content = gameScript.content
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        inputManager.record()

        with(current) {
            if (this == null) return

            if (exited) {

                scripts.removeFirst()
                val state = getState()
                current = scripts.firstOrNull()

                logger.debug("GAME_ENGINE") {
                    "Stop $name to switch the next game script ${current?.name}"
                }
                current?.frameBuffer = frameBuffer
                current?.spriteSheets = this@GameEngine.spriteSheets
                current?.level = this@GameEngine.levels[GAME_LEVEL]
                evaluate()
                current?.setState(state)
            } else if (loading) {
                evaluate()
            } else if (reloaded) {
                val state = getState()
                evaluate()
                setState(state)
                inError = false
            }

            if (spriteSheets.values.any { it.reload }) {
                spriteSheets.forEach { (_, s) -> s.reload = false }
                current?.spriteSheets = this@GameEngine.spriteSheets
            }
            if (levels.values.any { it.reload }) {
                levels.forEach { (_, s) -> s.reload = false }
                current?.level = this@GameEngine.levels[GAME_LEVEL]
            }

            if (resourcesState == ResourcesState.BOOTED) {
                resourcesState = ResourcesState.LOADED
                logger.debug("GAME_ENGINE") { "Resources loaded" }

                // Prepare the custom game script used only by the engine
                engineGameScript.evaluate()
                engineGameScript.frameBuffer = frameBuffer
                engineGameScript.spriteSheets = this@GameEngine.spriteSheets

                current?.spriteSheets = this@GameEngine.spriteSheets
                current?.level = this@GameEngine.levels[GAME_LEVEL]
                current?.resourcesLoaded()
            }

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inError = try {
                    current?.advance()
                    engineGameScript.advance()
                    false
                } catch (ex: LuaError) {
                    if (!inError) { // display the log only once.
                        logger.warn(
                            "TINY",
                            ex
                        ) { "The line ${ex.level} trigger an execution error (${ex.getLuaMessage()}). Please fix your script!" }
                    }
                    true
                }
                accumulator -= REFRESH_LIMIT
            }
        }

        // The user hit Ctrl + R(ecord)
        if (inputHandler.isKeyJustPressed(Key.CTRL) && inputHandler.isKeyPressed(Key.R)) {
            engineGameScript.invoke("popup", valueOf(0), valueOf("recording GIF"), valueOf(4))
            platform.record()
        } else if (inputHandler.isKeyJustPressed(Key.R) && inputHandler.isKeyPressed(Key.CTRL)) {
            engineGameScript.invoke("popup", valueOf(0), valueOf("recording GIF"), valueOf(4))
            platform.record()
        }
        inputManager.reset()
    }

    override fun draw() {
        with(current) {
            if (this == null) return
            platform.draw(renderContext, frameBuffer)
        }
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }

}
