package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.Pixel

/**
 * Represents a single sprite instance within a batch.
 * Contains the source region coordinates from the spritesheet and destination
 * coordinates on screen, along with flip flags for horizontal/vertical mirroring.
 * Instances with the same rendering state are grouped together in SpriteBatch objects.
 */
class SpriteInstance(
    var sourceX: Pixel = 0,
    var sourceY: Pixel = 0,
    var sourceWidth: Pixel = 0,
    var sourceHeight: Pixel = 0,
    var destinationX: Pixel = 0,
    var destinationY: Pixel = 0,
    var flipX: Boolean = false,
    var flipY: Boolean = false,
) {
    fun set(
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
    ): SpriteInstance {
        this.sourceX = sourceX
        this.sourceY = sourceY
        this.sourceWidth = sourceWidth
        this.sourceHeight = sourceHeight
        this.destinationX = destinationX
        this.destinationY = destinationY
        this.flipX = flipX
        this.flipY = flipY

        return this
    }

    fun addVertexInto(
        vertexIndex: Int,
        vertexData: FloatArray,
    ): Int {
        var indexVertex = vertexIndex
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()
        // A - Right/Up
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()
        // A - Right/Down
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        // B - Right/Down
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        // B - Left/Down
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        // B - Left/Up
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()

        return indexVertex
    }

    fun addUvsInto(
        uvsIndex: Int,
        uvs: FloatArray,
    ): Int {
        var indexVertex = uvsIndex
        val a = this
        // A - Left/Up
        val v1 = a.uvLeft to a.uvUp
        // A - Right/Up
        val v2 = a.uvRight to a.uvUp
        // A - Right/Down
        val v3 = a.uvRight to a.uvDown
        // B - Right/Down
        val va = a.uvRight to a.uvDown
        // B - Left/Down
        val vb = a.uvLeft to a.uvDown
        // B - Left/Up
        val vc = a.uvLeft to a.uvUp

        val uvsPoint = if (!a.flipX && !a.flipY) {
            listOf(v1, v2, v3, va, vb, vc)
        } else if (a.flipX && !a.flipY) {
            listOf(v2, v1, vb, vb, va, v2)
        } else if (!a.flipX && a.flipY) {
            listOf(vb, va, v2, v2, vc, vb)
        } else {
            listOf(va, vb, vc, v1, v2, v3)
        }

        uvsPoint.forEach { (x, y) ->
            uvs[indexVertex++] = x.toFloat()
            uvs[indexVertex++] = y.toFloat()
        }
        return indexVertex
    }

    fun addTextureIndicesInto(
        textureIndicesIndex: Int,
        textureIndices: FloatArray,
        textureIndex: Int,
    ): Int {
        var index = textureIndicesIndex
        val textureIndexFloat = textureIndex.toFloat()

        // Set same texture index for all 6 vertices of this sprite
        repeat(6) {
            textureIndices[index++] = textureIndexFloat
        }

        return index
    }

    val positionLeft: Pixel
        get() = destinationX
    val positionRight: Int
        get() = destinationX + sourceWidth
    val positionUp: Pixel
        get() = destinationY
    val positionDown: Int
        get() = destinationY + sourceHeight
    val uvLeft: Pixel
        get() = sourceX
    val uvRight: Int
        get() = sourceX + sourceWidth
    val uvUp: Pixel
        get() = sourceY
    val uvDown: Int
        get() = sourceY + sourceHeight
}
