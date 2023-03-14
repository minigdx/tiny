package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.SpriteSheetType.GAME
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.platform.RenderContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.luaj.vm2.LuaError
import java.io.File

class ScriptsCollector(private val events: MutableList<GameScript>) : FlowCollector<GameScript> {

    private val scriptsByName = mutableMapOf<String, Boolean>()

    override suspend fun emit(value: GameScript) {
        val script = scriptsByName[value.name]
        // New script. The content will have to be loaded by the GameEngine.
        // It's added in the script stack
        if (script == null) {
            scriptsByName.put(value.name, true)
            events.add(value)
        } else {
            events.add(value.apply { reloaded = true })
        }
    }

}

enum class SpriteSheetType {
    // BOOT,
    GAME
}

class SpriteSheet(
    var pixels: Array<Array<ColorIndex>>,
    var width: Pixel,
    var height: Pixel,
    val type: SpriteSheetType,
    var reload: Boolean = true,
    var isLoaded: Boolean = false
) {
    fun copy(dstX: Pixel, dstY: Pixel, dst: FrameBuffer, x: Pixel, y: Pixel, width: Pixel, height: Pixel) {
        (0 until width).forEach { offsetX ->
            (0 until height).forEach { offsetY ->
                val colorIndex = pixels[y + offsetY][x + offsetX]
                dst.pixel(dstX + offsetX, dstY + offsetY, colorIndex)
            }
        }
    }
}

enum class ResourcesState {
    BOOT,
    BOOTED,
    LOADED,
}

class GameEngine(
    val gameOption: GameOption,
    val platform: Platform,
    val vfs: VirtualFileSystem,
    val logger: Logger,
) : GameLoop {

    private val scripts: MutableList<GameScript> = mutableListOf()
    private val scriptsByName: MutableMap<String, GameScript> = mutableMapOf()

    private val events: MutableList<GameScript> = mutableListOf()
    private val workEvents: MutableList<GameScript> = mutableListOf()

    private var spriteSheets = emptyMap<SpriteSheetType, SpriteSheet>()
    private var resourcesState: ResourcesState = ResourcesState.BOOT

    private var current: GameScript? = null

    private var accumulator: Seconds = 0f

    private lateinit var renderContext: RenderContext

    fun main() {
        platform.initWindowManager()

        val scope = CoroutineScope(Dispatchers.Default)

        val scriptsName = listOf("src/main/resources/boot.lua", "src/main/resources/test.lua")
        val resourcesName = listOf("src/main/resources/boot.png", "src/main/resources/test.png")

        scope.launch {
            scriptsName.asFlow()
                .map { name -> GameScript(name, gameOption).apply { loading = true } }
                .onCompletion {
                    val scriptsLoading = scriptsName.map { file ->
                        vfs.watch(FileStream(File(file))).map { content ->
                            GameScript(file, gameOption).apply {
                                this.content = content
                            }
                        }
                    }.merge()

                    emitAll(scriptsLoading)
                }.collect(ScriptsCollector(events))
        }
        scope.launch {
            resourcesName.asFlow()
                .zip(listOf(/*BOOT,*/ GAME, GAME).asFlow()) { file, type ->
                    file to type
                }
                .flatMapMerge { (file, type) ->
                    vfs.watch(FileStream(File(file)))
                        .map { data -> platform.extractRGBA(data) }
                        .map { data ->
                            val pixels = convertTexture(data.data)
                            val sheet = convertToColorIndex(pixels, data.width, data.height)
                            SpriteSheet(sheet, data.width, data.height, type)
                        }
                }
                .collect { spriteSheet ->
                    spriteSheets += spriteSheet.type to spriteSheet
                    if (spriteSheets.keys.containsAll(SpriteSheetType.values().toSet())) {
                        resourcesState = ResourcesState.BOOTED
                    }
                }

        }

        renderContext = platform.initRenderManager()

        platform.gameLoop(this)
    }

    private fun convertTexture(imageData: ByteArray): ByteArray {
        fun dst(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Int {
            val r = (r1 - r2) * (r1 - r2)
            val g = (g1 - g2) * (g1 - g2)
            val b = (b1 - b2) * (b1 - b2)
            return r + g + b
        }

        (0 until imageData.size step RGBA).forEach { pixel ->
            val r = imageData[pixel + 0]
            val g = imageData[pixel + 1]
            val b = imageData[pixel + 2]

            val paletteColor = FrameBuffer.defaultPalette.minBy { color ->
                dst(color[0].toInt(), color[1].toInt(), color[2].toInt(), r.toInt(), g.toInt(), b.toInt())
            }

            imageData[pixel + 0] = paletteColor[0]
            imageData[pixel + 1] = paletteColor[1]
            imageData[pixel + 2] = paletteColor[2]
            imageData[pixel + 3] = paletteColor[3]
        }
        return imageData
    }

    private fun convertToColorIndex(data: ByteArray, width: Pixel, height: Pixel): Array<Array<ColorIndex>> {
        val map = FrameBuffer.defaultPalette.mapIndexed { index, color -> color.toList() to index }
            .toMap()

        val result: Array<Array<ColorIndex>> = Array(height) {
            Array(width) { 0 }
        }

        (0 until width).forEach { x ->
            (0 until height).forEach { y ->
                val coord = (x + y * width) * RGBA
                val key = listOf(
                    data[coord + 0],
                    data[coord + 1],
                    data[coord + 2],
                    data[coord + 3]
                )
                result[y][x] = map[key] ?: 0
            }
        }
        return result
    }

    private var inError = false

    override fun advance(delta: Seconds) {
        workEvents.addAll(events)

        workEvents.forEach { gameScript ->
            if (!gameScript.reloaded) {
                // First time script loading. Adding it to the stack of script
                scripts.add(gameScript)
                scriptsByName[gameScript.name] = gameScript
                if (current == null) {
                    current = scripts.firstOrNull()
                }
            } else {
                // The script is already in the stack. Time to update it.
                scriptsByName[gameScript.name]?.run {
                    if (isValid()) {
                        reloaded = true
                        content = gameScript.content
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        with(current) {
            if (this == null) return

            if (exited) {
                scripts.removeFirst()
                current = scripts.firstOrNull()
                val state = getState()
                current?.setState(state)
            } else if (loading) {
                evaluate()
            } else if (reloaded) {
                val state = getState()
                evaluate()
                setState(state)
                // FIXME: call resourcesLoaded
            }

            if (spriteSheets.values.any { it.reload }) {
                spriteSheets.forEach { _, s -> s.reload = false }
                current?.spriteSheets = this@GameEngine.spriteSheets
            }

            if (resourcesState == ResourcesState.BOOTED) {
                resourcesState = ResourcesState.LOADED
                logger.debug("GAME_ENGINE") { "Resources loaded" }
                current?.spriteSheets = this@GameEngine.spriteSheets
                current?.resourcesLoaded()
            }

            // Fixed step simulation
            accumulator += delta
            if (accumulator >= REFRESH_LIMIT) {
                inError = try {
                    current?.advance()
                    false
                } catch (ex: LuaError) {
                    if (!inError) { // display the log only once.
                        logger.warn("TINY") { "The line ${ex.level} trigger an execution error (${ex.getLuaMessage()}). Please fix your script!" }
                    }
                    true
                }
                accumulator -= REFRESH_LIMIT
            }
        }

    }

    override fun draw() {
        with(current) {
            if (this == null) return
            platform.draw(renderContext, frameBuffer)
        }
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
        const val RGBA = 4
    }

}
