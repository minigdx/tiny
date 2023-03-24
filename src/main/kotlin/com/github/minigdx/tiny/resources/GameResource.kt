package com.github.minigdx.tiny.resources

interface GameResource {
    /**
     * Type of the resource.
     */
    val type: ResourceType

    /**
     * The resource needs to be reloaded ?
     */
    var reload: Boolean

    /**
     * The resource is loaded?
     */
    var isLoaded: Boolean
}
