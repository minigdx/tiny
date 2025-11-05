package com.github.minigdx.tiny.cli.config

import com.github.minigdx.tiny.engine.GameOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

@Serializable
@JsonClassDiscriminator("version")
sealed class GameParameters {
    abstract val name: String
    abstract val id: String

    abstract fun toGameOptions(): GameOptions

    @OptIn(ExperimentalSerializationApi::class)
    fun write(output: File) {
        FileOutputStream(output).use {
            JSON.encodeToStream(this, it)
        }
    }

    abstract fun addLevel(level: String): GameParameters

    abstract fun addSpritesheet(sprite: String): GameParameters

    abstract fun addScript(script: String): GameParameters

    abstract fun addSound(sound: String): GameParameters

    abstract fun setPalette(colors: List<String>): GameParameters

    /**
     * Return the list of the user Lua script to load.
     */
    abstract fun getAllScripts(): List<String>

    companion object {
        val JSON = Json {
            ignoreUnknownKeys = true
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun read(inputStream: InputStream): GameParameters {
            return JSON.decodeFromStream<GameParameters>(inputStream)
        }

        fun read(file: File): GameParameters {
            return read(FileInputStream(file))
        }
    }
}

@Serializable
data class Size(val width: Int, val height: Int)

@SerialName("V1")
@Serializable
data class GameParametersV1(
    override val name: String,
    /**
     * Unique identifier for this game.
     * Generated during at the creation time of the game.
     */
    override val id: String,
    val resolution: Size,
    val sprites: Size,
    val zoom: Int,
    val colors: List<String>,
    /**
     * Script used by the game.
     * The first script will be the first to be run after the boot sequence.
     */
    val scripts: List<String> = emptyList(),
    /**
     * Sprite sheets to be loaded.
     * The first spritesheet will be the one used by default.
     */
    val spritesheets: List<String> = emptyList(),
    /**
     * Level to be loaded.
     * The first level will be the one used by default.
     */
    val levels: List<String> = emptyList(),
    /**
     * List of sounds.
     */
    val sound: String? = null,
    /**
     * Is the default mouse cursor should be hidden?
     * If true, the mouse cursor will not be displayed.
     * The game have to display it by itself if the mouse is required.
     */
    val hideMouseCursor: Boolean = false,
) : GameParameters() {
    override fun toGameOptions(): GameOptions {
        return GameOptions(
            width = resolution.width,
            height = resolution.height,
            palette = colors,
            spriteSize = sprites.width to sprites.height,
            gameScripts = getAllScripts(),
            spriteSheets = spritesheets,
            gameLevels = levels,
            zoom = zoom,
            sound = sound,
            hideMouseCursor = hideMouseCursor,
        )
    }

    override fun getAllScripts(): List<String> {
        fun extractName(name: String): String {
            val atIndex = name.indexOf("@")
            return if (atIndex != -1) {
                name.substring(0, atIndex)
            } else {
                name
            }
        }

        return scripts
    }

    override fun addLevel(level: String): GameParameters {
        return copy(levels = levels + level)
    }

    override fun addSpritesheet(sprite: String): GameParameters {
        return copy(spritesheets = spritesheets + sprite)
    }

    override fun addScript(script: String): GameParameters {
        return copy(scripts = scripts + script)
    }

    override fun addSound(sound: String): GameParameters {
        return copy(sound = this@GameParametersV1.sound + sound)
    }

    override fun setPalette(colors: List<String>): GameParameters {
        return copy(colors = colors)
    }
}
