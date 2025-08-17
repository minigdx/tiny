package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.render.batch.BatchManager
import com.github.minigdx.tiny.resources.SpriteSheet

class DefaultVirtualFrameBuffer(
    private val platform: Platform,
    private val gameOptions: GameOptions,
) : VirtualFrameBuffer {
    private val batchManager = BatchManager()

    private val primitiveBuffer = FrameBuffer(
        gameOptions.width,
        gameOptions.height,
        gameOptions.colors(),
    )

    override fun draw(
        source: SpriteSheet,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        val immediateDraw = batchManager.submitSprite(
            source,
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX,
            destinationY,
            flipX,
            flipY,
            primitiveBuffer.blender.dithering,
            primitiveBuffer.blender.switch,
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )

        if (immediateDraw) {
            batchManager.consumeAllBatches { batch ->
                platform.drawIntoFrameBuffer(batch)
                primitiveBuffer.clear(0)
            }
        }
    }

    override fun drawPrimitive(block: (FrameBuffer) -> Unit) {
        val immediateDraw = batchManager.submitSprite(
            primitiveBuffer.asSpriteSheet,
            0,
            0,
            gameOptions.width,
            gameOptions.height,
            0,
            0,
            false,
            false,
            primitiveBuffer.blender.dithering,
            primitiveBuffer.blender.switch,
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
        if (immediateDraw) {
            batchManager.consumeAllBatches { batch ->
                platform.drawIntoFrameBuffer(batch)
                primitiveBuffer.clear(0)
            }
        }
        block(primitiveBuffer)
    }

    override fun draw() {
        batchManager.consumeAllBatches { batch ->
            platform.drawIntoFrameBuffer(batch)
        }
        platform.drawFrameBuffer()
    }
}