package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.ColorIndex
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
    private val batchManager = BatchManager()

    private val primitiveBuffer = FrameBuffer(
        gameOptions.width,
        gameOptions.height,
        gameOptions.colors(),
    )

    private val monocolors = createFontPalettes()

    private fun createFontPalettes(): Array<Array<ColorIndex>> {
        return (0 until gameOptions.colors().size).map { index ->
            val palette = Array(gameOptions.colors().size) { index }
            palette[0] = 0 // Set the transparent color
            palette
        }.toTypedArray()
    }

    override fun drawMonocolor(
        source: SpriteSheet,
        color: ColorIndex,
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        batchManager.submitSprite(
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
            monocolors[color],
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
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
        batchManager.submitSprite(
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
    }

    override fun drawPrimitive(block: (FrameBuffer) -> Unit) {
        // FIXME: TODO
    }

    private fun renderAllInFrameBuffer() {
        spriteBatchStage.startStage()
        // Render all remaining batch into the GPU Framebuffer.
        batchManager.consumeAllBatches { key, batch ->
            spriteBatchStage.execute(key, batch)
        }
        spriteBatchStage.endStage()
    }

    override fun draw() {
        renderAllInFrameBuffer()
        frameBufferStage.execute(spriteBatchStage)
    }

    override fun bindTextures(spritesheetToBind: List<SpriteSheet>) {
        spriteBatchStage.bindTextures(spritesheetToBind)
    }

    override fun readFrameBuffer(): RenderFrame {
        return spriteBatchStage.readFrameBuffer()
    }

    override fun clear(color: ColorIndex) {
        batchManager.clear()
        spriteBatchStage.clear(color)
    }
}
