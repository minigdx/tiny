package com.github.minigdx.tiny.render.batch

import com.github.minigdx.tiny.Pixel

class PrimitiveBatch() : Batch<PrimitiveInstance> {
    var parametersIndex = 0
    var parameters: Array<Pixel> = Array<Pixel>(1000) { 0 }

    fun canAddPrimitive(): Boolean {
        return (parametersIndex < 1000 - 7)
    }

    fun addPrimitive(instance: PrimitiveInstance) {
        instance.parameters.copyInto(parameters, parametersIndex)
    }

    override fun canAddInto(): Boolean {
        return (parametersIndex < 1000 - 7)
    }

    override fun add(instance: PrimitiveInstance) {
        if (!canAddInto()) return

        // TODO
    }

    override fun reset() {
        parametersIndex = 0
    }
}
