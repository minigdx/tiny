package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.render.batch.BatchManager
import com.github.minigdx.tiny.render.gl.FrameBufferStage
import com.github.minigdx.tiny.render.gl.SpriteBatchStage
import com.github.minigdx.tiny.resources.SpriteSheet

class DefaultVirtualFrameBuffer(
    private val spriteBatchStage: SpriteBatchStage,
    private val frameBufferStage: FrameBufferStage,
    private val gameOptions: GameOptions,
) : VirtualFrameBuffer {
    private var isPrimitiveBufferUpdated = false

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
                spriteBatchStage.execute(batch)
                primitiveBuffer.clear(0)
            }
        }
    }

    override fun drawPrimitive(block: (FrameBuffer) -> Unit) {
        if (!isPrimitiveBufferUpdated) {
            isPrimitiveBufferUpdated = true
        }
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
                spriteBatchStage.execute(batch)
                primitiveBuffer.clear(0)
                isPrimitiveBufferUpdated = false
            }
        }
        block(primitiveBuffer)
    }

    override fun draw() {
        batchManager.consumeAllBatches { batch ->
            spriteBatchStage.execute(batch)
        }
        spriteBatchStage.endStage()
        frameBufferStage.execute(spriteBatchStage)
        spriteBatchStage.startStage()
    }

    override fun bindTextures(spritesheetToBind: List<SpriteSheet>) {
        spriteBatchStage.bindTextures(spritesheetToBind)
    }

    override fun readFrameBuffer(): RenderFrame {
        TODO()
    }
}
