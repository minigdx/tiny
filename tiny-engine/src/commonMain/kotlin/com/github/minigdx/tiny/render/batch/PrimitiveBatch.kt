package com.github.minigdx.tiny.render.batch

class PrimitiveBatch() : Batch<PrimitiveInstance> {
    var parametersIndex = 0
    var meshPosition: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var meshSize: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }

    var parametersType: FloatArray = FloatArray(MAX_INSTANCE) { 0f }
    var parametersColor: FloatArray = FloatArray(MAX_INSTANCE) { 0f }
    var parametersFilled: FloatArray = FloatArray(MAX_INSTANCE) { 0f }

    var parameters12: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var parameters34: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }
    var parameters56: FloatArray = FloatArray(MAX_INSTANCE * 2) { 0f }

    var dither: FloatArray = FloatArray(MAX_INSTANCE) { 0xFFFF.toFloat() }

    override fun canAddInto(): Boolean {
        return (parametersIndex < MAX_INSTANCE)
    }

    override fun add(instance: PrimitiveInstance) {
        if (!canAddInto()) return

        parametersType[parametersIndex] = instance.type.toFloat()
        parametersColor[parametersIndex] = instance.color.toFloat()
        parametersFilled[parametersIndex] = if (instance.filled) {
            1.0f
        } else {
            0.0f
        }

        dither[parametersIndex] = instance.dither.toFloat()

        val vec2ParametesIndex = parametersIndex * 2
        meshPosition[vec2ParametesIndex + 0] = instance.meshX.toFloat()
        meshPosition[vec2ParametesIndex + 1] = instance.meshY.toFloat()

        meshSize[vec2ParametesIndex + 0] = instance.meshWidth.toFloat()
        meshSize[vec2ParametesIndex + 1] = instance.meshHeight.toFloat()

        parameters12[vec2ParametesIndex + 0] = instance.parameters[0].toFloat()
        parameters12[vec2ParametesIndex + 1] = instance.parameters[1].toFloat()
        parameters34[vec2ParametesIndex + 0] = instance.parameters[2].toFloat()
        parameters34[vec2ParametesIndex + 1] = instance.parameters[3].toFloat()
        parameters56[vec2ParametesIndex + 0] = instance.parameters[4].toFloat()
        parameters56[vec2ParametesIndex + 1] = instance.parameters[5].toFloat()

        parametersIndex++
    }

    override fun reset() {
        parametersIndex = 0
    }

    companion object {
        private const val MAX_INSTANCE = 1000
    }
}
