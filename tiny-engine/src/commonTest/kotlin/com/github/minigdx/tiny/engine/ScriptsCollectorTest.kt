package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.resources.GameResource
import com.github.minigdx.tiny.resources.ResourceType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptsCollectorTest {
    private class TestGameResource(
        override val version: Int = 1,
        override val index: Int = 0,
        override val name: String = "test",
        override val type: ResourceType,
        override var reload: Boolean = false,
    ) : GameResource

    @Test
    fun emit_order_game_engine_boot_should_reorder_to_boot_engine_game() =
        runTest {
            val eventChannel = Channel<GameResource>(Channel.UNLIMITED)
            val collector = GameResourceCollector(eventChannel)
            val events = mutableListOf<GameResource>()

            // Launch a coroutine to collect events from the channel
            val job = launch {
                for (event in eventChannel) {
                    events.add(event)
                }
            }

            val gameScript = TestGameResource(index = 1, type = ResourceType.GAME_GAMESCRIPT)
            val engineScript = TestGameResource(index = 1, type = ResourceType.ENGINE_GAMESCRIPT)
            val bootScript = TestGameResource(index = 1, type = ResourceType.BOOT_GAMESCRIPT)

            collector.emit(gameScript)
            collector.emit(engineScript)
            collector.emit(bootScript)

            eventChannel.close()
            job.join()

            assertEquals(3, events.size)
            assertEquals(ResourceType.BOOT_GAMESCRIPT, events[0].type)
            assertEquals(ResourceType.ENGINE_GAMESCRIPT, events[1].type)
            assertEquals(ResourceType.GAME_GAMESCRIPT, events[2].type)
        }

    @Test
    fun emit_order_engine_game_boot_should_reorder_to_boot_engine_game() =
        runTest {
            val eventChannel = Channel<GameResource>(Channel.UNLIMITED)
            val collector = GameResourceCollector(eventChannel)
            val events = mutableListOf<GameResource>()

            // Launch a coroutine to collect events from the channel
            val job = launch {
                for (event in eventChannel) {
                    events.add(event)
                }
            }

            val engineScript = TestGameResource(index = 2, type = ResourceType.ENGINE_GAMESCRIPT)
            val gameScript = TestGameResource(index = 2, type = ResourceType.GAME_GAMESCRIPT)
            val bootScript = TestGameResource(index = 2, type = ResourceType.BOOT_GAMESCRIPT)

            collector.emit(engineScript)
            collector.emit(gameScript)
            collector.emit(bootScript)

            eventChannel.close()
            job.join()

            assertEquals(3, events.size)
            assertEquals(ResourceType.BOOT_GAMESCRIPT, events[0].type)
            assertEquals(ResourceType.ENGINE_GAMESCRIPT, events[1].type)
            assertEquals(ResourceType.GAME_GAMESCRIPT, events[2].type)
        }

    @Test
    fun emit_same_game_script_twice_should_set_reload_flag_on_second() =
        runTest {
            val eventChannel = Channel<GameResource>(Channel.UNLIMITED)
            val collector = GameResourceCollector(eventChannel)
            val events = mutableListOf<GameResource>()

            // Launch a coroutine to collect events from the channel
            val job = launch {
                for (event in eventChannel) {
                    events.add(event)
                }
            }

            val bootScript = TestGameResource(index = 3, type = ResourceType.BOOT_GAMESCRIPT)
            val engineScript = TestGameResource(index = 3, type = ResourceType.ENGINE_GAMESCRIPT)
            val gameScript1 = TestGameResource(index = 3, type = ResourceType.GAME_GAMESCRIPT)
            val gameScript2 = TestGameResource(
                version = 2,
                index = 3,
                name = "test",
                type = ResourceType.GAME_GAMESCRIPT,
            )

            collector.emit(bootScript)
            collector.emit(engineScript)
            collector.emit(gameScript1)
            collector.emit(gameScript2)

            eventChannel.close()
            job.join()

            assertEquals(4, events.size)
            assertEquals(ResourceType.BOOT_GAMESCRIPT, events[0].type)
            assertEquals(ResourceType.ENGINE_GAMESCRIPT, events[1].type)
            assertEquals(ResourceType.GAME_GAMESCRIPT, events[2].type)
            assertEquals(ResourceType.GAME_GAMESCRIPT, events[3].type)

            assertFalse(events[2].reload)
            assertTrue(events[3].reload)
        }
}
