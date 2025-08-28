package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.log.Logger
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
import com.github.minigdx.tiny.resources.SpriteSheet.SpriteSheetKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GameResourceProcessor(
    resourceFactory: ResourceFactory,
    gameOptions: GameOptions,
    platform: Platform,
    private val logger: Logger,
) : GameResourceAccess {
    private val scripts: Array<GameScript?>
    private val spriteSheets: Array<SpriteSheet?>
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

    // next available texture unit
    // The unit 0 is reserved for the primitive texture.
    private var nextAvailableTextureUnit = 1

    private val textureUnitPerSpriteSheet = mutableMapOf<SpriteSheetKey, Int>()

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
        spritesheetToBind.clear()

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
            currentScript = scripts[0]
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
        levels[resource.index] = gameLevel
        // Force the reloading of the script as level init might occur in the _init block.
        scripts[currentScriptIndex]?.reload = true

        gameLevel.tilesset.values.forEach { spriteSheet ->
            spriteSheet.textureUnit = textureUnitPerSpriteSheet.getOrPut(spriteSheet.key) { getNextAvailableTextureUnit() }
        }
        spritesheetToBind.addAll(gameLevel.tilesset.values)
    }

    private fun loadGameSpriteSheet(resource: GameResource) {
        val spriteSheet = resource as SpriteSheet
        spriteSheets[resource.index] = spriteSheet

        spriteSheet.textureUnit = textureUnitPerSpriteSheet.getOrPut(spriteSheet.key) { getNextAvailableTextureUnit() }
        spritesheetToBind.add(spriteSheet)
    }

    private fun loadBootSpriteSheet(resource: GameResource) {
        val spriteSheet = resource as SpriteSheet
        bootSpritesheet = spriteSheet

        spriteSheet.textureUnit = textureUnitPerSpriteSheet.getOrPut(spriteSheet.key) { getNextAvailableTextureUnit() }
        spritesheetToBind.add(spriteSheet)
    }

    private fun getNextAvailableTextureUnit(): Int {
        check(nextAvailableTextureUnit < MAX_TEXTURE_UNIT) {
            "There is too many textures managed by the engine. " +
                "The maximum texture allowed is $MAX_TEXTURE_UNIT"
        }
        return nextAvailableTextureUnit++
    }

    private suspend fun loadEngineScript(resource: GameResource) {
        // Don't put the engine script in the stack
        engineGameScript = resource as GameScript
        engineGameScript?.resourceAccess = this
        engineGameScript?.evaluate()
    }

    private fun loadGameScript(resource: GameResource) {
        resource as GameScript
        resource.resourceAccess = this
        // Game script will be evaluated when the boot script will exit
        scripts[resource.index] = resource
    }

    private suspend fun loadBootScript(resource: GameResource) {
        // Always put the boot script at the top of the stack
        val bootScript = resource as GameScript
        bootScript.resourceAccess = this
        bootScript.evaluate()
        scripts[0] = bootScript
    }

    override fun setCurrentScript(index: Int): Pair<GameScript?, GameScript> {
        val previousGameScript = scripts[currentScriptIndex]
        currentScriptIndex = index % scripts.size
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
        TODO("Not yet implemented")
    }

    override fun saveSpritesheet(sheet: SpriteSheet) {
        // TODO: set the spritesheet texture unit?
        // TODO: add it into the texture to bind
        TODO("Not yet implemented")
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

    companion object {
        // Number of total texture managed by the game engine
        // (game engine + spritesheets + primitives + levels spritesheets)
        // It needs to be sync with the number of texture unit in [com.github.minigdx.tiny.render.gl.SpriteBatchStage]
        private const val MAX_TEXTURE_UNIT = 17
    }
}
