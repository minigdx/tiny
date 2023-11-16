package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.graphic.ColorPalette
import com.github.minigdx.tiny.graphic.PixelArray
import com.github.minigdx.tiny.graphic.PixelFormat
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.resources.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.resources.ResourceType.ENGINE_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.resources.ResourceType.GAME_LEVEL
import com.github.minigdx.tiny.resources.ResourceType.GAME_SPRITESHEET
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceFactory(
    private val vfs: VirtualFileSystem,
    private val platform: Platform,
    private val logger: Logger,
    private val colorPalette: ColorPalette,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun soundEffect(index: Int, name: String): Flow<Sound> {
        var version = 0
        return vfs.watch(platform.createSoundStream(name))
            .map { soundData -> Sound(version++, index, name, soundData) }
            .onEach {
                logger.debug("RESOURCE_FACTORY") {
                    "Loading sound '$name'"
                }
            }
    }

    fun gameLevel(index: Int, name: String): Flow<GameLevel> {
        var version = 0
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
            }.map { level ->
                val layers = listOf("$name/_composite.png") + level.layers.map { layer -> "$name/$layer" }
                val pngLayers = layers
                    .mapIndexed { index, layer ->
                        LdKtImageLayer(
                            name = layer,
                            index = index,
                            x = level.x,
                            y = level.y,
                            width = level.width,
                            height = level.height,
                        )
                    }.mapNotNull { layer ->
                        val stream = platform.createImageStream(layer.name)
                        if (stream.exists()) {
                            val imageData = stream.read()
                            val texture = convertToColorIndex(imageData.data, level.width, level.height)
                            layer.apply {
                                pixels = texture
                            }
                        } else {
                            null
                        }
                    }

                val intLayers = layers
                    .map { layer -> layer.replace(".png", ".csv") }
                    .mapIndexedNotNull { index, layer ->
                        val stream = platform.createByteArrayStream(layer)
                        if (stream.exists()) {
                            val data = stream.read()
                            val lines = data.decodeToString()
                                .lines()
                                .map { l -> l.split(",").filter { it.isNotBlank() } }
                                .filterNot { it.isEmpty() }
                            val l = LdKtIntLayer(
                                name = layer,
                                index = index,
                                x = level.x,
                                y = level.y,
                                width = lines.first().size,
                                height = lines.size,
                            )

                            lines.forEachIndexed { y, columns ->
                                columns.forEachIndexed { x, i ->
                                    l.ints.set(x, y, i.toInt())
                                }
                            }
                            l
                        } else {
                            null
                        }
                    }

                GameLevel(version++, index, GAME_LEVEL, name, level.layers.size + 1, level).apply {
                    pngLayers.forEach { layer ->
                        imageLayers[layer.index] = layer
                    }
                    intLayers.forEach { layer ->
                        this.intLayers[layer.index] = layer
                    }
                }.copy()
            }
    }

    fun gamescript(index: Int, name: String, inputHandler: InputHandler, gameOptions: GameOptions) =
        script(index, name, inputHandler, gameOptions, GAME_GAMESCRIPT)

    fun enginescript(name: String, inputHandler: InputHandler, gameOptions: GameOptions) =
        script(0, name, inputHandler, gameOptions, ENGINE_GAMESCRIPT)

    fun bootscript(name: String, inputHandler: InputHandler, gameOptions: GameOptions) =
        script(0, name, inputHandler, gameOptions, BOOT_GAMESCRIPT)

    private fun script(
        index: Int,
        name: String,
        inputHandler: InputHandler,
        gameOptions: GameOptions,
        resourceType: ResourceType,
    ): Flow<GameScript> {
        var version = 0
        return vfs.watch(platform.createByteArrayStream(name)).map { content ->
            GameScript(version++, index, name, gameOptions, inputHandler, resourceType).apply {
                this.content = content
            }
        }.onEach {
            logger.debug("RESOURCE_FACTORY") {
                "Loading script '$name'"
            }
        }
    }

    fun gameSpritesheet(index: Int, name: String): Flow<SpriteSheet> {
        return spritesheet(index, name, GAME_SPRITESHEET)
    }

    fun bootSpritesheet(name: String): Flow<SpriteSheet> {
        return spritesheet(0, name, BOOT_SPRITESHEET)
    }

    private fun spritesheet(index: Int, name: String, resourceType: ResourceType): Flow<SpriteSheet> {
        var version = 0
        return vfs.watch(platform.createImageStream(name)).map { imageData ->
            val sheet = convertToColorIndex(imageData.data, imageData.width, imageData.height)
            SpriteSheet(version++, index, name, resourceType, sheet, imageData.width, imageData.height)
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
                val index = colorPalette.fromRGBA(
                    byteArrayOf(
                        data[coord + 0],
                        data[coord + 1],
                        data[coord + 2],
                        data[coord + 3],
                    ),
                )

                result.set(x, y, index)
            }
        }
        return result
    }
}
