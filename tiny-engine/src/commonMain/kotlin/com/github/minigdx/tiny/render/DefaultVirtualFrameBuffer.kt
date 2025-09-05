package com.github.minigdx.tiny.render

import com.github.minigdx.tiny.ColorIndex
import com.github.minigdx.tiny.Pixel
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.render.batch.BatchManager
import com.github.minigdx.tiny.render.batch.PrimitiveBatch
import com.github.minigdx.tiny.render.batch.PrimitiveInstance
import com.github.minigdx.tiny.render.batch.PrimitiveKey
import com.github.minigdx.tiny.render.batch.SpriteBatch
import com.github.minigdx.tiny.render.batch.SpriteBatchInstance
import com.github.minigdx.tiny.render.batch.SpriteBatchKey
import com.github.minigdx.tiny.render.gl.FrameBufferStage
import com.github.minigdx.tiny.render.gl.SpriteBatchStage
import com.github.minigdx.tiny.resources.SpriteSheet

// TODO list:
// 1. bouger le framebuffer dans le default virtual frame buffer
// 2. modifier ShapeLib pour utiliser virtual framebuffer + rect
// 3. crééer nouveau stage. le stage va draw les shapers. -> utiliser instanciacing
// 4. bien tester et comprendre comment ça marche.
// 5. appliquer instanciacing sur SpriteBatchStage aussi.
// 6. Appliquer dithering et voilà !!!
class DefaultVirtualFrameBuffer(
    private val spriteBatchStage: SpriteBatchStage,
    private val frameBufferStage: FrameBufferStage,
    private val gameOptions: GameOptions,
) : VirtualFrameBuffer {
    private val spriteBatchManager = BatchManager(
        keyGenerator = { SpriteBatchKey() },
        instanceGenerator = { SpriteBatchInstance() },
        batchGenerator = { SpriteBatch() },
    )

    private val primitiveBatchManager = BatchManager(
        // TODO: for primitive, with instanciating -> one key for all?
        keyGenerator = { PrimitiveKey() },
        instanceGenerator = { PrimitiveInstance() },
        batchGenerator = { PrimitiveBatch() },
    )

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
        val key = spriteBatchManager.createKey()
        key.set(
            source,
            primitiveBuffer.blender.dithering,
            monocolors[color],
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX,
            destinationY,
            flipX,
            flipY,
        )
        spriteBatchManager.submit(key, instance)
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
        val key = spriteBatchManager.createKey()
        key.set(
            source,
            primitiveBuffer.blender.dithering,
            // TODO: changing pall might not be working as the array is the same.
            primitiveBuffer.blender.switch,
            primitiveBuffer.camera,
            primitiveBuffer.clipper,
        )
        val instance = spriteBatchManager.createInstance()
        instance.set(
            sourceX,
            sourceY,
            sourceWidth,
            sourceHeight,
            destinationX,
            destinationY,
            flipX,
            flipY,
        )
        spriteBatchManager.submit(key, instance)
    }

    override fun drawPrimitive(block: (FrameBuffer) -> Unit) {
        // FIXME: TODO
    }

    fun drawRecf(
        x: Pixel,
        y: Pixel,
        width: Pixel,
        height: Pixel,
        colorIndex: ColorIndex,
        filled: Boolean,
    ) {
        val key = primitiveBatchManager.createKey().set(colorIndex)
        val instance = primitiveBatchManager.createInstance().setRect(
            x,
            y,
            width,
            height,
            filled = filled,
        )
        primitiveBatchManager.submit(key, instance)
    }

    // FIXME: mettre le framebuffer dans le virtual frame buffer ?
    private fun renderAllInFrameBuffer() {
        spriteBatchStage.startStage()
        // Render all remaining batch into the GPU Framebuffer.
        spriteBatchManager.consumeAllBatches { key, batch ->
            spriteBatchStage.execute(key, batch)
        }
        spriteBatchStage.endStage()

        // otherStage.startStage()
        // otherBatchManager.consumeAllBatches { key, batch ->
        // OTHER STAGE.execute(key, batch)
        // }
        // otherStage.endStage()
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
        spriteBatchManager.clear()
        primitiveBatchManager.clear()
        // FIXME: clear framebuffer here
        spriteBatchStage.clear(color)
    }
}
