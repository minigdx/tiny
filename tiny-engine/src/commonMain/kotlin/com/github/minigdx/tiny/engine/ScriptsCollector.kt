package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.ResourceType
import kotlinx.coroutines.flow.FlowCollector

class ScriptsCollector(private val events: MutableList<GameResource>) : FlowCollector<GameResource> {

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
        if(firstToBeLoaded == null) {
            // Every mandatory element has been loaded
            val resourcesOfType = loadedResources.getOrPut(currentType) { mutableMapOf() }
            val exist = resourcesOfType[value.index]
            resourcesOfType[value.index] = value
            value.reload = exist != null
            events.add(value)
        } else if(loadingOrder.isEmpty()) {
            // Last mandatory element to be loaded. Sort it then emit it in order
            waitingList.add(value)
            val tmp = sortWaitingListByMandatoryLoadingOrder()
            waitingList.clear()
            tmp.forEach { emit(it) }
        } else if(loadingOrder.isNotEmpty()) {
            // Still waiting for mandatory elements
            waitingList.add(value)
        }
    }

    private fun sortWaitingListByMandatoryLoadingOrder(): MutableList<GameResource> {
        val tmp = mutableListOf<GameResource>()
        tmp.addAll(waitingList)
        // Sort by type according to loadingOrder
        tmp.sortBy { resource ->
            val orderIndex = mandatoryLoading.indexOf(resource.type)
            if (orderIndex == -1) mandatoryLoading.size else orderIndex
        }
        return tmp
    }
}
