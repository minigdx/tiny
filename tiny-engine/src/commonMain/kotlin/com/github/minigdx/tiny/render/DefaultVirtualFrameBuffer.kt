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
    /**
     * Is the primitive frame buffer was updated?
     * If this framebuffer was updated,
     * it has to be updated in the GPU also.
     */
    private var isPrimitiveBufferUpdated = false

    /**
     * Is the [SpriteBatchStage.startStage] should be invoked,
     * as a group operation is started.
     */
    private var shouldStartStage = true

    private val batchManager = BatchManager()

    private val primitiveBuffer = FrameBuffer(
        gameOptions.width,
        gameOptions.height,
        gameOptions.colors(),
    )

    private fun renderAllInFrameBuffer() {
        if (shouldStartStage) {
            shouldStartStage = false
            spriteBatchStage.startStage()
        }

        if (isPrimitiveBufferUpdated) {
            bindTextures(listOf(primitiveBuffer.asSpriteSheet))
        }
        // Render all remaining batch into the GPU Framebuffer.
        batchManager.consumeAllBatches { batch ->
            spriteBatchStage.execute(batch)
        }
        isPrimitiveBufferUpdated = false
        primitiveBuffer.clear(0)
    }

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
            renderAllInFrameBuffer()
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
            renderAllInFrameBuffer()
        }
        isPrimitiveBufferUpdated = true
        block(primitiveBuffer)
    }

    override fun draw() {
        renderAllInFrameBuffer()
        spriteBatchStage.endStage()

        shouldStartStage = true

        frameBufferStage.execute(spriteBatchStage)
    }

    override fun bindTextures(spritesheetToBind: List<SpriteSheet>) {
        spriteBatchStage.bindTextures(spritesheetToBind)
    }

    override fun readFrameBuffer(): RenderFrame {
        return spriteBatchStage.readFrameBuffer()
    }
}
