package com.github.minigdx.tiny.resources

import kotlinx.serialization.Serializable

class GameLevel(
    override val type: ResourceType,
    numberOfLayers: Int,
) : GameResource {

    override var reload: Boolean = true
    override var isLoaded: Boolean = false
    val imageLayers: Array<LdKtImageLayer?> = Array(numberOfLayers) { null }
    val intLayers: Array<LdKtIntLayer?> = Array(numberOfLayers) { null }
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
    val customFields: Map<String, String>,
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
    val customFields: Map<String, String>,
)
