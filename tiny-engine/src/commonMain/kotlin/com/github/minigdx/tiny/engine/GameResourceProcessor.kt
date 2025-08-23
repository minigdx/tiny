package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
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
import com.github.minigdx.tiny.resources.ResourceType.PRIMITIVE_SPRITESHEET
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.resources.SpriteSheet
import com.github.minigdx.tiny.resources.SpriteSheet.SpriteSheetKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

interface GameResourceAccess2 {
    /**
     * The boot script sprite sheet.
     * Only the engine / bootscript can control what to do with it.
     */
    val bootSpritesheet: SpriteSheet?

    val currentScript: GameScript?

    /**
     * Set the current script.
     * @return the previous game script, if any
     */
    fun setCurrentScript(index: Int): Pair<GameScript?, GameScript>

    /**
     * Access a sprite sheet by its index.
     */
    fun findSpritesheet(index: Int): SpriteSheet?

    /**
     * Find a sprite sheet by its name
     */
    fun findSpritesheet(name: String): Int?

    /**
     * Generate a new Spritesheet index, in case of a new spritesheet.
     */
    fun newSpritesheetIndex(): Int

    /**
     * Save the spritesheet as a new spritesheet in the list of spritesheet;
     */
    fun saveSpritesheet(sheet: SpriteSheet)

    /**
     * Access a level by its index.
     */
    fun findLevel(index: Int): GameLevel?

    /**
     * Access sound by its index
     */
    fun findSound(index: Int): Sound?

    /**
     * Access sound by its name
     */
    fun findSound(name: String): Sound?

    fun findGameScript(name: String): GameScript?
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameResourceProcessor(
    val events: MutableList<GameResource>,
    resourceFactory: ResourceFactory,
    gameOptions: GameOptions,
    platform: Platform,
    private val logger: Logger,
) : GameResourceAccess2 {
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

    private val workEvents: MutableList<GameResource> = mutableListOf()

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

        numberOfResources = resources.size
        logger.debug("GAME_ENGINE") { "Number of resources to load: $numberOfResources" }

        val resourcesScope = CoroutineScope(platform.io())
        resourcesScope.launch {
            resources.asFlow()
                .flatMapMerge(concurrency = 128) { resource -> resource }
                .collect(ScriptsCollector(events))
        }
    }

    suspend fun process(events: List<GameResource>) {
        spritesheetToBind.clear()

        workEvents.addAll(events)
        workEvents.forEach { event ->
            if (event.reload) {
                processReloadedResource(event)
            } else {
                processNewResource(event)
            }
        }
        workEvents.clear()
    }

    suspend fun processNewResource(resource: GameResource) {
        logger.info("GAME_ENGINE") { "Loaded ${resource.name} ${resource.type} (version: ${resource.version})" }
        processResourceByType(resource)
        numberOfResources--
        logger.debug("GAME_ENGINE") { "Remaining resources to load: $numberOfResources." }
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
        TODO("Not yet implemented")
    }

    override fun findSpritesheet(name: String): Int? {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun findSound(index: Int): Sound? {
        TODO("Not yet implemented")
    }

    override fun findSound(name: String): Sound? {
        TODO("Not yet implemented")
    }

    override fun findGameScript(name: String): GameScript? {
        TODO("Not yet implemented")
    }

    companion object {
        // Number of total texture managed by the game engine
        // (game engine + spritesheets + primitives + levels spritesheets)
        // It needs to be sync with the number of texture unit in [com.github.minigdx.tiny.render.gl.SpriteBatchStage]
        private const val MAX_TEXTURE_UNIT = 17
    }
}
