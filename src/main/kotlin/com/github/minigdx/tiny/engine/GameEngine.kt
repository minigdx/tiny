package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.engine.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.engine.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.engine.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.engine.ResourcesState.BOOT
import com.github.minigdx.tiny.file.ResourceFactory
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import org.luaj.vm2.LuaError

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
                bootscriptLoaded = true
            }
        } else {
            // Ignore the script until the bootscript is loaded
            if(bootscriptLoaded) {
                events.add(value.apply { reloaded = true })
            }
        }
    }

}

enum class ResourceType {
    BOOT_GAMESCRIPT,
    GAME_GAMESCRIPT,
    BOOT_SPRITESHEET,
    GAME_SPRITESHEET,
    GAME_LEVEL,
}

interface GameResource {
    /**
     * Type of the resource.
     */
    val type: ResourceType

    /**
     * The resource needs to be reloaded ?
     */
    var reload: Boolean

    /**
     * The resource is loaded?
     */
    var isLoaded: Boolean
}

class SpriteSheet(
    var pixels: Array<Array<ColorIndex>>,
    var width: Pixel,
    var height: Pixel,
    override val type: ResourceType,
    override var reload: Boolean = true,
    override var isLoaded: Boolean = false,

    ) : GameResource {
    fun copy(dstX: Pixel, dstY: Pixel, dst: FrameBuffer, x: Pixel, y: Pixel, width: Pixel, height: Pixel) {
        (0 until width).forEach { offsetX ->
            (0 until height).forEach { offsetY ->
                val colorIndex = pixels[y + offsetY][x + offsetX]
                dst.pixel(dstX + offsetX, dstY + offsetY, colorIndex)
            }
        }
    }
}

class GameLevel(
    override val type: ResourceType,
    override var reload: Boolean = true,
    override var isLoaded: Boolean = false
) : GameResource

enum class ResourcesState {
    BOOT,
    BOOTED,
    LOADED,
}

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
    private var resourcesState: ResourcesState = BOOT

    private var current: GameScript? = null

    private var accumulator: Seconds = 0f

    private lateinit var renderContext: RenderContext

    private val resourceFactory = ResourceFactory(vfs, platform, logger)

    fun main() {
        val windowManager = platform.initWindowManager()

        val scope = CoroutineScope(Dispatchers.Default)

        val scripts = listOf(
            resourceFactory.bootscript("src/main/resources/boot.lua", gameOption),
            resourceFactory.gamescript("src/main/resources/test.lua", gameOption)
        )

        scope.launch {
            scripts.asFlow()
                .flatMapMerge { script -> script }
                .collect(ScriptsCollector(events))
        }

        val resourcesName = listOf(
            resourceFactory.bootSpritesheet("src/main/resources/boot.png"),
            resourceFactory.gameSpritesheet("src/main/resources/test.png"),
            resourceFactory.gameLevel("src/main/resources/hello/simplified/Level_0"),
        )

        scope.launch {
            resourcesName.asFlow()
                .flatMapMerge { flow -> flow }
                .collect { resource ->
                    resources += resource.type to resource
                    if (resource.type in setOf(BOOT_SPRITESHEET, GAME_SPRITESHEET)) {
                        spriteSheets += resource.type to (resource as SpriteSheet)
                    }
                    if (resources.size == resourcesName.size) {
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
            if (!gameScript.reloaded) {
                // First time script loading. Adding it to the stack of script
                scripts.add(gameScript)
                scriptsByName[gameScript.name] = gameScript
                if (current == null) {
                    current = scripts.firstOrNull()
                }
            } else {
                // The script is already in the stack. Time to update it.
                scriptsByName[gameScript.name]?.run {
                    if (isValid()) {
                        reloaded = true
                        content = gameScript.content
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        with(current) {
            if (this == null) return

            if (exited) {
                scripts.removeFirst()
                current = scripts.firstOrNull()
                val state = getState()
                current?.setState(state)
            } else if (loading) {
                evaluate()
            } else if (reloaded) {
                val state = getState()
                evaluate()
                setState(state)
                // FIXME: call resourcesLoaded
            }

            if (spriteSheets.values.any { it.reload }) {
                spriteSheets.forEach { _, s -> s.reload = false }
                current?.spriteSheets = this@GameEngine.spriteSheets
            }

            if (resourcesState == ResourcesState.BOOTED) {
                resourcesState = ResourcesState.LOADED
                logger.debug("GAME_ENGINE") { "Resources loaded" }
                current?.spriteSheets = this@GameEngine.spriteSheets
                current?.resourcesLoaded()
            }

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inError = try {
                    current?.advance()
                    false
                } catch (ex: LuaError) {
                    if (!inError) { // display the log only once.
                        logger.warn("TINY") { "The line ${ex.level} trigger an execution error (${ex.getLuaMessage()}). Please fix your script!" }
                    }
                    true
                }
                accumulator -= REFRESH_LIMIT
            }
        }

    }

    override fun draw() {
        with(current) {
            if (this == null) return
            platform.draw(renderContext, frameBuffer)
        }
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
        const val RGBA = 4
    }

}
