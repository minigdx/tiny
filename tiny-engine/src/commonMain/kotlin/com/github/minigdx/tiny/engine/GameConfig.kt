package com.github.minigdx.tiny.engine

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("version")
sealed class GameConfig {
    abstract val name: String
    abstract val id: String

    abstract fun toGameOptions(): GameOptions

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun parse(jsonString: String): GameConfig = JSON.decodeFromString(jsonString)
    }
}

@Serializable
data class GameConfigSize(val width: Int, val height: Int)

@Serializable
data class GameConfigFontBank(
    val name: String,
    val width: Int,
    val height: Int,
    val characters: List<String>,
    val x: Int = 0,
    val y: Int = 0,
)

@Serializable
data class GameConfigFont(
    val name: String,
    val spritesheet: String,
    val spaceWidth: Int? = null,
    val banks: List<GameConfigFontBank>,
)

@SerialName("V1")
@Serializable
data class GameConfigV1(
    override val name: String,
    override val id: String,
    val resolution: GameConfigSize,
    val sprites: GameConfigSize,
    val zoom: Int,
    val colors: List<String>,
    val scripts: List<String> = emptyList(),
    val spritesheets: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
    val sound: String? = null,
    val hideMouseCursor: Boolean = false,
    /**
     * Custom boot script to use instead of the default boot.lua.
     * This script should exist in the game directory.
     * When set, this script will be used as the first script to run.
     */
    val bootScript: String? = null,
    val fonts: List<GameConfigFont> = emptyList(),
) : GameConfig() {
    override fun toGameOptions(): GameOptions =
        GameOptions(
            width = resolution.width,
            height = resolution.height,
            palette = colors,
            spriteSize = sprites.width to sprites.height,
            gameScripts = scripts,
            spriteSheets = spritesheets,
            gameLevels = levels,
            zoom = zoom,
            sound = sound,
            hideMouseCursor = hideMouseCursor,
            bootScript = bootScript,
            fonts = fonts.map { font ->
                FontDescriptor.fromConfig(font)
            },
        )
}
