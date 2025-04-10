package com.github.minigdx.tiny.resources

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

data class GameLevel(
    override val version: Int,
    override val index: Int,
    override val type: ResourceType,
    override val name: String,
    val numberOfLayers: Int,
    val ldktLevel: LdtkLevel,
) : GameResource {
    override var reload: Boolean = false
    val imageLayers: Array<LdKtImageLayer?> = Array(numberOfLayers) { null }
    val intLayers: Array<LdKtIntLayer?> = Array(numberOfLayers) { null }
    val entities = ldktLevel.entities

    fun copy(): GameLevel {
        val gameLevel =
            GameLevel(
                version,
                index,
                type,
                name,
                numberOfLayers,
                ldktLevel,
            )
        imageLayers.copyInto(gameLevel.imageLayers)
        intLayers.copyInto(gameLevel.intLayers)
        return gameLevel
    }
}

@Serializable
data class LdtkLevel(
    val identifier: String,
    val uniqueIdentifer: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val bgColor: String,
    val customFields: JsonElement,
    val layers: List<String>,
    val entities: Map<String, List<LdtkEntity>>,
)

@Serializable
data class LdtkEntity(
    val id: String,
    val iid: String,
    val layer: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: Int,
    val customFields: JsonElement,
)
