package com.github.minigdx.tiny.cli.config

import com.github.minigdx.tiny.engine.GameOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

enum class Version {
    V1
}

@Serializable
@JsonClassDiscriminator("version")
sealed class GameParameters() {
    abstract fun toGameOptions(): GameOption

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
    val colors: List<String>
) : GameParameters() {
    override fun toGameOptions(): GameOption {
        return GameOption(
            width = resolution.width,
            height = resolution.height,
            spriteSize = sprites.width to sprites.height,
            zoom = zoom
        )
    }
}
