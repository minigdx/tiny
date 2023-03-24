package com.github.minigdx.tiny.file

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameLevel
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.engine.GameScript
import com.github.minigdx.tiny.engine.ResourceType
import com.github.minigdx.tiny.engine.ResourceType.BOOT_GAMESCRIPT
import com.github.minigdx.tiny.engine.ResourceType.BOOT_SPRITESHEET
import com.github.minigdx.tiny.engine.ResourceType.GAME_GAMESCRIPT
import com.github.minigdx.tiny.engine.ResourceType.GAME_SPRITESHEET
import com.github.minigdx.tiny.engine.SpriteSheet
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.platform.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.io.File

class ResourceFactory(
    private val vfs: VirtualFileSystem,
    private val platform: Platform,
    private val logger: Logger,
) {

    fun gameLevel(name: String): Flow<GameLevel> {
        return flowOf(GameLevel(ResourceType.GAME_LEVEL))
    }

    fun gamescript(name: String, gameOption: GameOption) = script(name, gameOption, GAME_GAMESCRIPT)
    fun bootscript(name: String, gameOption: GameOption) = script(name, gameOption, BOOT_GAMESCRIPT)

    private fun script(name: String, gameOption: GameOption, resourceType: ResourceType): Flow<GameScript> {
        // Emit empty game script to install each script ASAP.
        return flowOf(GameScript(name, gameOption, resourceType).apply {
            loading = true
        }).onCompletion {
            // Lazy loading of the script.
            val lazyScript = vfs.watch(FileStream(File(name))).map { content ->
                GameScript(name, gameOption, resourceType).apply {
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
        return vfs.watch(FileStream(File(name))).map { data ->
            val imageData = platform.extractRGBA(data)
            val pixels = convertTexture(imageData.data)
            val sheet = convertToColorIndex(pixels, imageData.width, imageData.height)
            SpriteSheet(sheet, imageData.width, imageData.height, resourceType)
        }.onEach {
            logger.debug("RESOURCE_FACTORY") {
                "Loading spritesheet '$name' ($resourceType)"
            }
        }
    }

    private fun convertTexture(imageData: ByteArray): ByteArray {
        fun dst(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Int {
            val r = (r1 - r2) * (r1 - r2)
            val g = (g1 - g2) * (g1 - g2)
            val b = (b1 - b2) * (b1 - b2)
            return r + g + b
        }

        (0 until imageData.size step GameEngine.RGBA).forEach { pixel ->
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
                val coord = (x + y * width) * GameEngine.RGBA
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

}
