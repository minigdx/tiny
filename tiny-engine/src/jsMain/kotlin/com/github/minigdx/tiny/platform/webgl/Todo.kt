package com.github.minigdx.tiny.platform.webgl

import js.array.ReadonlyArray
import js.objects.ReadonlyRecord
import js.typedarrays.Float32Array
import web.audio.AudioParamName
import web.audio.AudioWorkletProcessor
import web.audio.AudioWorkletProcessorReference

class Todo : AudioWorkletProcessor {
    override fun process(
        inputs: ReadonlyArray<ReadonlyArray<Float32Array<*>>>,
        outputs: ReadonlyArray<ReadonlyArray<Float32Array<*>>>,
        parameters: ReadonlyRecord<AudioParamName, Float32Array<*>>,
    ): Boolean {
        TODO("Not yet implemented")
    }

    companion object : AudioWorkletProcessorReference(
        value = AudioWorkletProcessor::class.js,
        parameterDescriptors = arrayOf(),
    )
}
