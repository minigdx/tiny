package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.resources.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourceType.ENGINE_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_LEVEL
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.util.PixelFormat
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LdKtIntLayer(
    val name: String,
    val index: Int,
    val x: Pixel,
    val y: Pixel,
    val width: Pixel,
    val height: Pixel,
    var ints: PixelArray = PixelArray(width, height),
)

class LdKtImageLayer(
    val name: String,
    val index: Int,
    val x: Pixel,
    val y: Pixel,
    val width: Pixel,
    val height: Pixel,
    var pixels: PixelArray = PixelArray(width, height),
)

@FlowPreview
class ResourceFactory(
    private val vfs: VirtualFileSystem,
    private val platform: Platform,
    private val logger: Logger,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun gameLevel(name: String): Flow<GameLevel> {
        return flowOf("$name/data.json")
            .map { platform.createByteArrayStream(it) }
            .flatMapMerge { filestream -> vfs.watch(filestream) }
            .map { data ->
                val levelData: LdtkLevel = json.decodeFromString(data.decodeToString())
                levelData
            }.onEach { level ->
                logger.debug("RESOURCE_FACTORY") {
                    "Loading level " + level.uniqueIdentifer + " with layers " + level.layers.joinToString(", ")
                }
            }.flatMapMerge { level ->

                val layers = listOf("$name/_composite.png") + level.layers.map { layer -> "$name/$layer" }
                val pngLayers = layers
                    .mapIndexed { index, layer ->
                        LdKtImageLayer(
                            name = layer,
                            index = index,
                            x = level.x,
                            y = level.y,
                            width = level.width,
                            height = level.height
                        )
                    }.asFlow()
                    .flatMapMerge { layer ->
                        vfs.watch(platform.createImageStream(layer.name)).map { imageData ->
                            // val imageData = platform.extractRGBA(data)
                            convertToColorIndex(imageData.data, level.width, level.height)
                        }.map { texture ->
                            layer.apply {
                                pixels = texture
                            }
                        }
                    }

                val intLayers = layers
                    .map { layer -> layer.replace(".png", ".csv") }
                    .mapIndexed { index, layer ->
                    layer to index
                }.asFlow()
                    .flatMapMerge { (layer, index) ->
                        vfs.watch(platform.createByteArrayStream(layer)).map { data ->
                            data.decodeToString()
                                .lines()
                                .map { l -> l.split(",").filter { it.isNotBlank() } }
                                .filterNot { it.isEmpty() }
                        }.map { lines ->
                            val l = LdKtIntLayer(
                                name = layer,
                                index = index,
                                x = level.x,
                                y = level.y,
                                width = lines.first().size,
                                height = lines.size
                            )

                            lines.forEachIndexed { y, columns ->
                                columns.forEachIndexed { x, i ->
                                    l.ints.set(x, y, i.toInt())
                                }
                            }
                            l
                        }
                    }

                flowOf(GameLevel(GAME_LEVEL, level.layers.size + 1, level))
                    .combine(pngLayers) { l, layer ->
                        l.apply {
                            imageLayers[layer.index] = layer
                        }
                    }.combine(intLayers) { l, layer ->
                        l.apply {
                           this.intLayers[layer.index] = layer
                        }
                    }
            }
    }

    fun gamescript(name: String, inputHandler: InputHandler, gameOption: GameOption) =
        script(name, inputHandler, gameOption, GAME_GAMESCRIPT)

    fun enginescript(name: String, inputHandler: InputHandler, gameOption: GameOption) =
        script(name, inputHandler, gameOption, ENGINE_GAMESCRIPT)

    fun bootscript(name: String, inputHandler: InputHandler, gameOption: GameOption) =
        script(name, inputHandler, gameOption, BOOT_GAMESCRIPT)

    private fun script(
        name: String,
        inputHandler: InputHandler,
        gameOption: GameOption,
        resourceType: ResourceType
    ): Flow<GameScript> {
        // Emit empty game script to install each script ASAP.
        return flowOf(GameScript(name, gameOption, inputHandler, resourceType).apply {
            loading = true
        }).onCompletion {
            // Lazy loading of the script.
            val lazyScript = vfs.watch(platform.createByteArrayStream(name)).map { content ->
                GameScript(name, gameOption, inputHandler, resourceType).apply {
                    this.content = content
                }
            }
            emitAll(lazyScript)
        }.onEach {
            logger.debug("RESOURCE_FACTORY") {
                "Loading script '$name'"
            }
        }
    }

    fun gameSpritesheet(name: String): Flow<SpriteSheet> {
        return spritesheet(name, GAME_SPRITESHEET)
    }

    fun bootSpritesheet(name: String): Flow<SpriteSheet> {
        return spritesheet(name, BOOT_SPRITESHEET)
    }

    private fun spritesheet(name: String, resourceType: ResourceType): Flow<SpriteSheet> {
        return vfs.watch(platform.createImageStream(name)).map { imageData ->
            val sheet = convertToColorIndex(imageData.data, imageData.width, imageData.height)
            SpriteSheet(sheet, imageData.width, imageData.height, resourceType)
        }.onEach {
            logger.debug("RESOURCE_FACTORY") {
                "Loading spritesheet '$name' ($resourceType)"
            }
        }
    }

    private fun convertToColorIndex(data: ByteArray, width: Pixel, height: Pixel): PixelArray {
        val result = PixelArray(width, height)

        (0 until width).forEach { x ->
            (0 until height).forEach { y ->
                val coord = (x + y * width) * PixelFormat.RGBA
                val index = FrameBuffer.gamePalette.fromRGBA(
                    byteArrayOf(
                        data[coord + 0],
                        data[coord + 1],
                        data[coord + 2],
                        data[coord + 3],
                    )
                )

                result.set(x, y, index)
            }
        }
        return result
    }

}
