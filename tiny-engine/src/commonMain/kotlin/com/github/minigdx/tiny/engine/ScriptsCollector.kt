package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.ResourceType
import kotlinx.coroutines.flow.FlowCollector

class ScriptsCollector(private val events: MutableList<GameResource>) : FlowCollector<GameResource> {

    private var bootscriptLoaded = false

    private val waitingList: MutableList<GameResource> = mutableListOf()

    private val loadedResources: MutableMap<ResourceType, MutableMap<Int, GameResource>> = mutableMapOf()

    override suspend fun emit(value: GameResource) {
        // The application has not yet booted.
        // But the boot script just got loaded
        if (value.type == ResourceType.BOOT_GAMESCRIPT && !bootscriptLoaded) {
            events.add(value)
            waitingList.forEach {
                val toReload = loadedResources[it.type]?.containsKey(it.index) == true
                if (!toReload) {
                    loadedResources.getOrPut(it.type) { mutableMapOf() }[it.index] = it
                }
            }
            events.addAll(waitingList)
            waitingList.clear()
            bootscriptLoaded = true
        } else if (!bootscriptLoaded) {
            waitingList.add(value)
        } else {
            // Check if the resources is loading or reloaded
            val toReload = loadedResources[value.type]?.containsKey(value.index) == true
            if (!toReload) {
                loadedResources.getOrPut(value.type) { mutableMapOf() }[value.index] = value
            }
            events.add(
                value.apply {
                    reload = toReload
                },
            )
        }
    }
}
