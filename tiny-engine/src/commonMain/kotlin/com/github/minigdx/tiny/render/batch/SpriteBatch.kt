package com.github.minigdx.tiny.render.batch

/**
 * A batch of sprite instances that share the same rendering state.
 * Sprites are grouped by their BatchKey to minimize GPU state changes during rendering.
 * Each batch can hold up to MAX_SPRITE_PER_BATCH sprites before requiring a new batch.
 * Implements efficient merging logic to combine compatible batches when possible.
 */
class SpriteBatch : Batch<SpriteBatchInstance> {
    var vertexIndex = 0
    var uvsIndex = 0

    var numberOfVertex = 0

    val vertex = FloatArray(VERTEX_ARRAY_SIZE)
    val uvs = FloatArray(UVS_ARRAY_SIZE)

    override fun canAddInto(): Boolean {
        // Is there still room to add another sprite in the batch?
        return ((numberOfVertex + VERTEX_PER_SPRITE) * 3 < VERTEX_ARRAY_SIZE)
    }

    override fun add(instance: SpriteBatchInstance) {
        if (!canAddInto()) return

        vertexIndex = instance.addVertexInto(vertexIndex, vertex)
        uvsIndex = instance.addUvsInto(uvsIndex, uvs)
        numberOfVertex += VERTEX_PER_SPRITE
    }

    override fun reset() {
        vertexIndex = 0
        uvsIndex = 0
        numberOfVertex = 0
    }

    companion object {
        const val MAX_SPRITE_PER_BATCH = 1000
        private const val VERTEX_PER_SPRITE = 6

        // 2 floats per vertex. So the array is nb sprite * nb vertex * 2
        private const val VERTEX_ARRAY_SIZE = MAX_SPRITE_PER_BATCH * VERTEX_PER_SPRITE * 3
        private const val UVS_ARRAY_SIZE = MAX_SPRITE_PER_BATCH * VERTEX_PER_SPRITE * 2
    }
}
