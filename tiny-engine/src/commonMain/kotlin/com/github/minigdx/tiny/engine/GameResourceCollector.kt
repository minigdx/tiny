package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.ResourceType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector

class GameResourceCollector(private val eventChannel: Channel<GameResource>) : FlowCollector<GameResource> {
    private val waitingList: MutableList<GameResource> = mutableListOf()

    private val loadedResources: MutableMap<ResourceType, MutableMap<Int, GameResource>> = mutableMapOf()

    private val loadingOrder = mutableSetOf(
        ResourceType.BOOT_GAMESCRIPT,
        ResourceType.ENGINE_GAMESCRIPT,
    )

    private val mandatoryLoading = listOf(
        ResourceType.BOOT_GAMESCRIPT,
        ResourceType.ENGINE_GAMESCRIPT,
    )

    override suspend fun emit(value: GameResource) {
        val firstToBeLoaded = loadingOrder.firstOrNull()
        val currentType = value.type
        loadingOrder.remove(currentType)
        if (firstToBeLoaded == null) {
            // Every mandatory element has been loaded
            registerResource(value)
            eventChannel.send(value)
        } else if (loadingOrder.isEmpty()) {
            // Last mandatory element to be loaded. Sort it then emit it in order
            waitingList.add(value)
            val tmp = sortWaitingListByMandatoryLoadingOrder(waitingList)
            waitingList.clear()
            tmp.forEach { resource ->
                registerResource(resource)
                eventChannel.send(resource)
            }
        } else if (loadingOrder.isNotEmpty()) {
            // Still waiting for mandatory elements
            waitingList.add(value)
        }
    }

    private fun registerResource(value: GameResource) {
        val currentType = value.type
        val resourcesOfType = loadedResources.getOrPut(currentType) { mutableMapOf() }
        val exist = resourcesOfType[value.index]
        resourcesOfType[value.index] = value
        value.reload = exist != null
    }

    private fun sortWaitingListByMandatoryLoadingOrder(resources: MutableList<GameResource>): MutableList<GameResource> {
        val tmp = mutableListOf<GameResource>()
        tmp.addAll(resources)
        // Sort by type according to loadingOrder
        tmp.sortBy { resource ->
            val orderIndex = mandatoryLoading.indexOf(resource.type)
            if (orderIndex == -1) mandatoryLoading.size else orderIndex
        }
        return tmp
    }

    fun status(): Map<ResourceType, Collection<GameResource>> {
        return loadedResources.mapValues { (_, values) -> values.values }
    }
}
