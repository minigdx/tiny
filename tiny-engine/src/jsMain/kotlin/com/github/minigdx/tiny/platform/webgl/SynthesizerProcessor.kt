package com.github.minigdx.tiny.platform.webgl

import js.array.ReadonlyArray
import js.objects.ReadonlyRecord
import js.typedarrays.Float32Array
import web.audio.AudioParamName
import web.audio.AudioWorkletProcessor
import web.audio.AudioWorkletProcessorReference
import web.events.EventHandler

class SynthesizerProcessor : AudioWorkletProcessor {

    init {

        port.onmessage = EventHandler { event ->
            println("EVENT RECEIVED: $event")
        }
    }

    override fun process(
        inputs: ReadonlyArray<ReadonlyArray<Float32Array<*>>>,
        outputs: ReadonlyArray<ReadonlyArray<Float32Array<*>>>,
        parameters: ReadonlyRecord<AudioParamName, Float32Array<*>>,
    ): Boolean {
        println("TODO AUDIO WORKLET !!!!!")
        return true
    }

    companion object : AudioWorkletProcessorReference(
        value = AudioWorkletProcessor::class.js,
        parameterDescriptors = arrayOf(),
    ) {
        init {
            // WA to force Kotlin/JS don't remove class members (like `process`)
            requireNotNull(::SynthesizerProcessor)
        }
    }
}
