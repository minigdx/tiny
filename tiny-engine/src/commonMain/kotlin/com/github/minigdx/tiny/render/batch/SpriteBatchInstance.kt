package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.Pixel

/**
 * Represents a single sprite instance within a batch.
 * Contains the source region coordinates from the spritesheet and destination
 * coordinates on screen, along with flip flags for horizontal/vertical mirroring.
 * Instances with the same rendering state are grouped together in SpriteBatch objects.
 */
class SpriteBatchInstance(
    var sourceX: Pixel = 0,
    var sourceY: Pixel = 0,
    var sourceWidth: Pixel = 0,
    var sourceHeight: Pixel = 0,
    var destinationX: Pixel = 0,
    var destinationY: Pixel = 0,
    var flipX: Boolean = false,
    var flipY: Boolean = false,
    var depth: Float = 0f,
) : Instance {
    fun set(
        sourceX: Pixel,
        sourceY: Pixel,
        sourceWidth: Pixel,
        sourceHeight: Pixel,
        destinationX: Pixel,
        destinationY: Pixel,
        flipX: Boolean,
        flipY: Boolean,
        depth: Float,
    ): SpriteBatchInstance {
        this.sourceX = sourceX
        this.sourceY = sourceY
        this.sourceWidth = sourceWidth
        this.sourceHeight = sourceHeight
        this.destinationX = destinationX
        this.destinationY = destinationY
        this.flipX = flipX
        this.flipY = flipY
        this.depth = depth
        return this
    }

    override fun reset() {
        this.sourceX = 0
        this.sourceY = 0
        this.sourceWidth = 0
        this.sourceHeight = 0
        this.destinationX = 0
        this.destinationY = 0
        this.flipX = false
        this.flipY = false
        this.depth = 0f
    }

    fun addVertexInto(
        vertexIndex: Int,
        vertexData: FloatArray,
    ): Int {
        var indexVertex = vertexIndex
        // A - Left/Up
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()
        vertexData[indexVertex++] = depth
        // A - Right/Up
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()
        vertexData[indexVertex++] = depth
        // A - Right/Down
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        vertexData[indexVertex++] = depth
        // B - Right/Down
        vertexData[indexVertex++] = positionRight.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        vertexData[indexVertex++] = depth
        // B - Left/Down
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionDown.toFloat()
        vertexData[indexVertex++] = depth
        // B - Left/Up
        vertexData[indexVertex++] = positionLeft.toFloat()
        vertexData[indexVertex++] = positionUp.toFloat()
        vertexData[indexVertex++] = depth

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
