package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.ColorIndex

class PrimitiveBatch() : Batch<PrimitiveInstance> {
    var parametersIndex = 0
    var parametersType: FloatArray = FloatArray(MAX_INSTANCE) { 0f }
    var parameters12: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var parameters34: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var parameters56: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var parametersColor: Array<ColorIndex> = Array<ColorIndex>(MAX_INSTANCE) { 0 }

    override fun canAddInto(): Boolean {
        return (parametersIndex < MAX_INSTANCE)
    }

    override fun add(instance: PrimitiveInstance) {
        if (!canAddInto()) return

        parametersType[parametersIndex] = instance.parameters[0].toFloat()
        parameters12[parametersIndex + 0] = instance.parameters[1].toFloat()
        parameters12[parametersIndex + 1] = instance.parameters[2].toFloat()
        parameters34[parametersIndex + 0] = instance.parameters[3].toFloat()
        parameters34[parametersIndex + 1] = instance.parameters[4].toFloat()
        parameters56[parametersIndex + 0] = instance.parameters[5].toFloat()
        parameters56[parametersIndex + 1] = instance.parameters[6].toFloat()
        parametersColor[parametersIndex] = 4 // FIXME: to change. it should be from the instance.

        parametersIndex++
    }

    override fun reset() {
        parametersIndex = 0
    }

    companion object {
        private const val MAX_INSTANCE = 1000
    }
}
