package com.github.minigdx.tiny.cli.config

import com.github.minigdx.tiny.engine.GameOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Serializable
@JsonClassDiscriminator("version")
sealed class GameParameters() {
    abstract fun toGameOptions(): GameOptions

    fun write(output: File) {
        FileOutputStream(output).use {
            JSON.encodeToStream(this, it)
        }
    }

    abstract fun addLevel(level: String): GameParameters
    abstract fun addSpritesheet(sprite: String): GameParameters

    abstract fun addScript(script: String): GameParameters

    abstract fun addSound(sound: String): GameParameters

    companion object {
        val JSON = Json {
            ignoreUnknownKeys = true
        }

        fun read(file: File): GameParameters {
            return JSON.decodeFromStream<GameParameters>(FileInputStream(file))
        }
    }
}

@Serializable
data class Size(val width: Int, val height: Int)

@SerialName("V1")
@Serializable
data class GameParametersV1(
    val name: String,
    val resolution: Size,
    val sprites: Size,
    val zoom: Int,
    val colors: List<String>,
    /**
     * Script used by the game.
     * The first script will be the first to be ran after the boot sequence.
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
    val sounds: List<String> = emptyList(),
) : GameParameters() {
    override fun toGameOptions(): GameOptions {
        return GameOptions(
            width = resolution.width,
            height = resolution.height,
            palette = colors,
            spriteSize = sprites.width to sprites.height,
            gameScripts = scripts,
            spriteSheets = spritesheets,
            gameLevels = levels,
            zoom = zoom,
            sounds = sounds,
        )
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
        return copy(sounds = sounds + sound)
    }
}
