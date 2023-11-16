package com.github.minigdx.tiny.resources

interface GameResource {
    /**
     * Version of the resource (ie: ~ number of time it has been reloaded)
     * This version should be used only to know if the resources has been updated.
     */
    val version: Int

    /**
     * Index of this game resource, by type.
     */
    val index: Int

    /**
     * Name of the resource.
     */
    val name: String

    /**
     * Type of the resource.
     */
    val type: ResourceType

    /**
     * The resource needs to be reloaded ?
     */
    var reload: Boolean
}
