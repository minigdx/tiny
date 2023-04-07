package com.github.minigdx.tiny.cli.config

import com.github.minigdx.tiny.engine.GameOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("version")
sealed class GameParameters() {
    abstract fun toGameOptions(): GameOptions

}

@Serializable
data class Size(val width: Int, val height: Int)

@SerialName("V1")
@Serializable
class GameParametersV1(
    val name: String,
    val resolution: Size,
    val sprites: Size,
    val zoom: Int,
    val colors: List<String>,
    /**
     * Script used by the game.
     * The first script will be the first to be ran after the boot sequence.
     */
    val scripts: List<String> = listOf("game.lua"),
    /**
     * Sprite sheets to be loaded.
     * The first spritesheet will be the one used by default.
     */
    val spritesheets: List<String> = listOf("game.png"),
    /**
     * Level to be loaded.
     * The first level will be the one used by default.
     */
    val levels: List<String> = emptyList()
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
        )
    }
}
