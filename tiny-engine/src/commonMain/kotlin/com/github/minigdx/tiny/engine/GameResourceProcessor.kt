package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.toTinyException
import com.github.minigdx.tiny.platform.Platform
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
import com.github.minigdx.tiny.resources.ResourceType.PRIMITIVE_SPRITESHEET
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch
import org.luaj.vm2.LuaError

@OptIn(ExperimentalCoroutinesApi::class)
class GameResourceProcessor(
    resourceFactory: ResourceFactory,
    gameOptions: GameOptions,
    platform: Platform,
    private val logger: Logger,
) : GameResourceAccess {
    private val scripts: Array<GameScript?>
    private var spriteSheets: Array<SpriteSheet?>
    private val levels: Array<GameLevel?>
    private val sounds: Array<Sound?>

    override var bootSpritesheet: SpriteSheet? = null
        private set
    override var currentScript: GameScript? = null
        private set

    var engineGameScript: GameScript? = null
        private set

    private var numberOfResources: Int = 0

    private val resources: List<Flow<GameResource>>

    /**
     * Current script by index.
     */
    private var currentScriptIndex: Int = 0

    val spritesheetToBind = mutableListOf<SpriteSheet>()

    private val eventChannel = Channel<GameResource>(Channel.UNLIMITED)
    private val gameResourceCollector = GameResourceCollector(eventChannel)

    val toBeLoaded = mutableSetOf<String>()

    init {

        val gameScripts = gameOptions.gameScripts.mapIndexed { index, script ->
            resourceFactory.gamescript(index + 1, script)
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

        resources = listOf(
            resourceFactory.bootscript("_boot.lua"),
            resourceFactory.enginescript("_engine.lua"),
            resourceFactory.bootSpritesheet("_boot.png"),
        ) + gameScripts + spriteSheets + gameLevels + sounds

        toBeLoaded.addAll(
            setOf("_boot.lua", "_engine.lua", "_boot.png"),
        )
        toBeLoaded.addAll(gameOptions.gameLevels)
        toBeLoaded.addAll(gameOptions.gameScripts)
        toBeLoaded.addAll(gameOptions.spriteSheets)
        toBeLoaded.addAll(gameOptions.sounds)

        numberOfResources = resources.size
        logger.debug("GAME_ENGINE") { "Number of resources to load: $numberOfResources" }

        val resourcesScope = CoroutineScope(platform.io())
        resourcesScope.launch {
            resources.asFlow()
                .flatMapMerge(concurrency = 128) { resource -> resource }
                .collect(gameResourceCollector)
        }
    }

    suspend fun processAvailableEvents() {
        // Process all available events from the channel without blocking
        while (true) {
            val result = eventChannel.tryReceive()
            if (result.isFailure) {
                // No more events available, break
                break
            }

            val event = result.getOrNull() ?: continue

            toBeLoaded.remove(event.name)

            if (event.reload) {
                processReloadedResource(event)
            } else {
                processNewResource(event)
            }
        }
    }

    suspend fun processNewResource(resource: GameResource) {
        logger.info("GAME_ENGINE") { "Loaded ${resource.name} ${resource.type} (version: ${resource.version})" }
        processResourceByType(resource)
        numberOfResources--
        logger.debug("GAME_ENGINE") { "Remaining resources to load: $numberOfResources. (${toBeLoaded.joinToString(", ")})" }
        if (numberOfResources == 0) {
            logger.debug("GAME_ENGINE") { "All resources are loaded. Notify the boot script." }
            // Force to notify the boot script
            scripts[0]!!.resourcesLoaded()
        }
    }

    suspend fun processReloadedResource(resource: GameResource) {
        logger.info("GAME_ENGINE") { "Reload ${resource.name} ${resource.type} (version: ${resource.version})" }
        processResourceByType(resource)
    }

    private suspend fun processResourceByType(resource: GameResource) {
        when (resource.type) {
            BOOT_GAMESCRIPT -> loadBootScript(resource)
            GAME_GAMESCRIPT -> loadGameScript(resource)
            ENGINE_GAMESCRIPT -> loadEngineScript(resource)
            BOOT_SPRITESHEET -> loadBootSpriteSheet(resource)
            GAME_SPRITESHEET -> loadGameSpriteSheet(resource)
            GAME_LEVEL -> loadGameLevel(resource)
            GAME_SOUND -> loadGameSound(resource)
            PRIMITIVE_SPRITESHEET -> Unit
        }
    }

    private fun loadGameSound(resource: GameResource) {
        sounds[resource.index] = resource as Sound
    }

    private fun loadGameLevel(resource: GameResource) {
        val gameLevel = resource as GameLevel
        gameLevel.tilesset.forEach { (name, spriteSheet) ->
            spriteSheet.textureUnit = levels[resource.index]?.tilesset[name]?.textureUnit
        }
        levels[resource.index] = gameLevel
        // If the current script is _boot, don't force reload as the game is still loading
        if (currentScriptIndex > 0) {
            // Force the reloading of the script as level init might occur in the _init block.
            scripts[currentScriptIndex]?.reload = true
        }

        spritesheetToBind.addAll(gameLevel.tilesset.values)
    }

    private fun loadGameSpriteSheet(resource: GameResource) {
        val spriteSheet = resource as SpriteSheet
        // Copy the texture unit used by the current spritesheet
        spriteSheet.textureUnit = spriteSheets[resource.index]?.textureUnit
        spriteSheets[resource.index] = spriteSheet

        spritesheetToBind.add(spriteSheet)
    }

    private fun loadBootSpriteSheet(resource: GameResource) {
        val spriteSheet = resource as SpriteSheet

        spriteSheet.textureUnit = bootSpritesheet?.textureUnit
        bootSpritesheet = spriteSheet

        spritesheetToBind.add(spriteSheet)
    }

    private suspend fun loadEngineScript(resource: GameResource) {
        // Don't put the engine script in the stack
        engineGameScript = resource as GameScript
        engineGameScript?.resourceAccess = this
        engineGameScript?.evaluate()
    }

    private suspend fun loadGameScript(resource: GameResource) {
        resource as GameScript
        resource.resourceAccess = this

        try {
            if (resource.reload && !resource.isValid()) {
                return
            }
        } catch (ex: LuaError) {
            throw ex.toTinyException(resource.content.decodeToString())
        }
        // Game script will be evaluated when the boot script will exit
        scripts[resource.index] = resource

        // Update the current script, if the loaded script is the current script.
        if (currentScriptIndex == resource.index) {
            currentScript = resource
        } else if (currentScriptIndex > 0) {
            // If the current script is not the boot scrpit,
            // Force the reload of the current script, as the script just updated might be used by the current script.
            currentScript?.reload = true
        }
    }

    private suspend fun loadBootScript(resource: GameResource) {
        // Always put the boot script at the top of the stack
        val bootScript = resource as GameScript
        bootScript.resourceAccess = this
        bootScript.evaluate()
        scripts[0] = bootScript
        currentScript = bootScript
        currentScriptIndex = 0
    }

    override fun setCurrentScript(index: Int): Pair<GameScript?, GameScript> {
        val previousGameScript = scripts[currentScriptIndex]
        currentScriptIndex = 1 + (index % scripts.size)
        currentScript = scripts[currentScriptIndex]
        return previousGameScript to currentScript!!
    }

    override fun findSpritesheet(index: Int): SpriteSheet? {
        return spriteSheets.atIndex(index)
    }

    override fun findSpritesheet(name: String): SpriteSheet? {
        return spriteSheets.find { it?.name == name }
    }

    override fun newSpritesheetIndex(): Int {
        return spriteSheets.size
    }

    override fun saveSpritesheet(sheet: SpriteSheet) {
        spritesheetToBind.add(sheet)
        if (sheet.index >= spriteSheets.size) {
            spriteSheets = spriteSheets.copyOf(sheet.index + 1)
        }
        spriteSheets[sheet.index] = sheet
    }

    override fun findLevel(index: Int): GameLevel? {
        return levels.atIndex(index)
    }

    override fun findSound(index: Int): Sound? {
        return sounds.atIndex(index)
    }

    override fun findSound(name: String): Sound? {
        return sounds.find { it?.name == name }
    }

    override fun findGameScript(name: String): GameScript? {
        return scripts.find { it?.name == name }
    }

    fun status(): Map<ResourceType, Collection<GameResource>> {
        return gameResourceCollector.status()
    }

    private fun <T> Array<T>.atIndex(index: Int): T? {
        if (this.isEmpty()) return null
        return this[index % this.size]
    }
}
