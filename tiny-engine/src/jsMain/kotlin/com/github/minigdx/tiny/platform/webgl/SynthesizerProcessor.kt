package com.github.minigdx.tiny.platform.webgl

import js.array.ReadonlyArray
import js.objects.ReadonlyRecord
import js.typedarrays.Float32Array
import web.audio.AudioParamName
import web.audio.AudioWorkletProcessor
import web.audio.AudioWorkletProcessorReference
import web.events.EventHandler

class SynthesizerProcessor : AudioWorkletProcessor() {

    init {
        println("SynthesizerProcessor initialized!")
        port.onmessage = EventHandler { event ->
            val data = event.data.unsafeCast<dynamic>()
            val type = data.type as? String
            println("EVENT RECEIVED - Type: $type, Data: $data")

            when (type) {
                "noteOn" -> {
                    val note = data.note as? Int
                    val frequency = data.frequency as? Double
                    println("Note ON: note=$note, frequency=$frequency Hz")
                }
                "noteOff" -> {
                    val note = data.note as? Int
                    println("Note OFF: note=$note")
                }
                else -> {
                    println("Unknown message type: $type")
                }
            }
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
        value = SynthesizerProcessor::class.js,
        parameterDescriptors = arrayOf(),
    ) {
        init {
            // WA to force Kotlin/JS don't remove class members (like `process`)
            requireNotNull(::SynthesizerProcessor)
        }
    }
}
