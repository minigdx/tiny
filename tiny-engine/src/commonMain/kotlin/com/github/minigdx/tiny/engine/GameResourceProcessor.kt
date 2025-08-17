package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.log.Logger
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
import kotlinx.coroutines.flow.Flow

class GameResourceProcessor(
    resourceFactory: ResourceFactory,
    inputHandler: InputHandler,
    gameOptions: GameOptions,
    private val logger: Logger,
) {

    val scripts: Array<GameScript?>
    val spriteSheets: Array<SpriteSheet?>
    val levels: Array<GameLevel?>
    val sounds: Array<Sound?>

    private val workEvents: MutableList<GameResource> = mutableListOf()

    private var numberOfResources: Int = 0

    val resources: List<Flow<GameResource>>

    init {
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

        resources = listOf(
            resourceFactory.bootscript("_boot.lua", inputHandler, gameOptions),
            resourceFactory.enginescript("_engine.lua", inputHandler, gameOptions),
            resourceFactory.bootSpritesheet("_boot.png"),
        ) + gameScripts + spriteSheets + gameLevels + sounds

        numberOfResources = resources.size
        logger.debug("GAME_ENGINE") { "Number of resources to load: $numberOfResources" }
    }

    suspend fun process(events: List<GameResource>) {
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
        numberOfResources--
        logger.debug("GAME_ENGINE") { "Remaining resources to load: $numberOfResources." }
        if (numberOfResources == 0) {
            logger.debug("GAME_ENGINE") { "All resources are loaded. Notify the boot script." }
            // Force to notify the boot script
            scripts[0]!!.resourcesLoaded()
        }
    }

    private fun loadGameSound(resource: GameResource) {
        sounds[resource.index] = resource as Sound
    }

    private fun loadGameLevel(resource: GameResource) {
/*
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
            scripts[currentScriptIndex]?.reload = true
            clear()
        }

 */

    }

    private fun loadGameSpriteSheet(resource: GameResource) {
        spriteSheets[resource.index] = resource as SpriteSheet
    }

    private fun loadBootSpriteSheet(resource: GameResource) {
      //  bootSpritesheet = resource as SpriteSheet
    }

    private fun loadEngineScript(resource: GameResource) {
        /*
        // Don't put the engine script in the stack
        engineGameScript = resource as GameScript
        engineGameScript?.resourceAccess = this
        engineGameScript?.evaluate(customizeLuaGlobal)

         */
    }

    private fun loadGameScript(resource: GameResource) {
        /*
        resource as GameScript
        resource.resourceAccess = this
        // Game script will be evaluated when the boot script will exit
        scripts[resource.index] = resource

         */
    }

    private suspend fun loadBootScript(resource: GameResource) {
        /*
        // Always put the boot script at the top of the stack
        val bootScript = resource as GameScript
        bootScript.resourceAccess = this
        bootScript.evaluate(customizeLuaGlobal)
        scripts[0] = bootScript

         */
    }

    suspend fun processReloadedResource(resource: GameResource) {
        logger.info("GAME_ENGINE") { "Reload ${resource.name} ${resource.type} (version: ${resource.version})" }
        // The resource already has been loaded.
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
}