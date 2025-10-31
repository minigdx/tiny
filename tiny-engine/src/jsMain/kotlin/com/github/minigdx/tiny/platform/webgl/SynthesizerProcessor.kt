package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.InstrumentPlayer
import js.array.ReadonlyArray
import js.objects.ReadonlyRecord
import js.typedarrays.Float32Array
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import web.audio.AudioParamName
import web.audio.AudioWorkletProcessor
import web.audio.AudioWorkletProcessorReference
import web.events.EventHandler

class SynthesizerProcessor : AudioWorkletProcessor() {
    private var currentInstrumentPlayer: InstrumentPlayer? = null

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
                    val instrumentJson = data.instrument as? String
                    println("Note ON: note=$note, frequency=$frequency Hz")

                    if (instrumentJson != null && note != null) {
                        try {
                            val instrument = Json.decodeFromString<Instrument>(instrumentJson)
                            // Stop current note if any
                            currentInstrumentPlayer?.close()
                            // Create new player and start note
                            currentInstrumentPlayer = InstrumentPlayer(instrument)
                            currentInstrumentPlayer?.noteOn(Note.entries[note])
                            println("Created InstrumentPlayer and started note")
                        } catch (e: Exception) {
                            println("Error deserializing instrument: ${e.message}")
                        }
                    }
                }
                "noteOff" -> {
                    val note = data.note as? Int
                    println("Note OFF: note=$note")
                    if (note != null) {
                        currentInstrumentPlayer?.noteOff(Note.entries[note])
                    }
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
        // Get the first output channel (mono output)
        val output = outputs[0]?.get(0) ?: return true

        // Generate audio samples
        val player = currentInstrumentPlayer
        if (player != null) {
            // Fill the output buffer with generated samples
            for (i in 0 until output.length) {
                val sample = player.generate()
                output[i] = sample
            }
        } else {
            // No active player, output silence
            for (i in 0 until output.length) {
                output[i] = 0f
            }
        }

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
