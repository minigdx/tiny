package com.github.minigdx.tiny.platform.test

import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.assertEquals

expect fun toGif(name: String, animation: List<FrameBuffer>)

object TestHelper {

    fun assertEquals(expected: String, current: FrameBuffer) {
        assertEquals(expected.trim(), current.colorIndexBuffer.toString().trim())
    }

    fun test(name: String, script: String, block: suspend (platform: HeadlessPlatform) -> Unit) {
        test(name, script, 10 to 10, block)
    }

    val testScope = CoroutineScope(Dispatchers.Unconfined)

    fun test(name: String, script: String, size: Pair<Int, Int>, block: suspend (platform: HeadlessPlatform) -> Unit) {
        val colors = listOf(
            "#000000",
            "#FFFFFF",
            "#FF0000",
        )

        val resources = mapOf(
            "game.lua" to script,
            "_boot.lua" to "tiny.exit(0)",
            "_engine.lua" to "",
            "_boot.png" to ImageData(ByteArray(0), 0, 0),
        )

        val (w, h) = size
        val gameOptions = GameOptions(w, h, colors, listOf("game.lua"), emptyList())
        val platform = HeadlessPlatform(gameOptions, resources)

        GameEngine(
            gameOptions = gameOptions,
            platform = platform,
            vfs = CommonVirtualFileSystem(),
            logger = StdOutLogger("test"),

        ).main()

        testScope.launch { block(platform) }

        platform.saveAnimation(name)
    }
}
