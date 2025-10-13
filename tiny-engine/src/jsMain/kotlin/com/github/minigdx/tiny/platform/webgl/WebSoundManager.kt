package com.github.minigdx.tiny.platform.webgl

import HissGeneratorWorkletModule
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.ChunkGenerator
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.InstrumentPlayer
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.MutableFixedSizeList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import web.audio.AudioContext
import web.audio.AudioContextState
import web.audio.AudioWorkletNode
import web.audio.AudioWorkletProcessorName
import web.audio.running
import web.events.EventHandler
import web.worklets.addModule
import kotlin.js.json
import kotlin.math.pow

// Import the worklet URL using Vite's worker syntax
// This will be bundled properly by Vite with all imports resolved
@JsModule("./worklet-loader.js")
@JsNonModule
external val workletLoaderModule: dynamic

private val workletUrl: String
    get() = workletLoaderModule.default as String

class WebSoundManager : SoundManager() {
    lateinit var audioContext: AudioContext

    var ready: Boolean = false

    private var nextStartTime: Double = 0.0

    private val instrumentPlayers = MutableFixedSizeList<InstrumentPlayer>(MAX_INSTRUMENTS)

    private lateinit var audioWorkletNode: AudioWorkletNode

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
        println("INIT audio context " + audioContext.state)
        audioContext.onstatechange = EventHandler {
            println("audio context " + audioContext.state)
            // See: https://developer.mozilla.org/en-US/docs/Web/API/BaseAudioContext/state#resuming_interrupted_play_states_in_ios_safari
            // Resume the audio context if interrupted, only on iOS
            if (audioContext.state != AudioContextState.running) {
                audioContext.resumeAsync()
            } else {
                ready = true
            }
        }
        if (audioContext.state != AudioContextState.running) {
            inputHandler.onFirstUserInteraction {
                println("FIRST USER audio context " + audioContext.state)
                audioContext.resumeAsync()
                initializeAudioWorklet()
            }
        } else {
            println("FIRST USER audio context " + audioContext.state)
            initializeAudioWorklet()
        }
    }

    private val soundContext = CoroutineScope(Dispatchers.Main)

    private fun initializeAudioWorklet() {
        println("initializeAudioWorklet()")
        ready = true
        nextStartTime = audioContext.currentTime

        soundContext.launch {
            println("add module()")
            // Load the bundled worklet from Vite assets
            println(HissGeneratorWorkletModule.toString())
            audioContext.audioWorklet.addModule(HissGeneratorWorkletModule)

            audioWorkletNode = AudioWorkletNode(audioContext, AudioWorkletProcessorName("TODO"))
            audioWorkletNode.connect(audioContext.destination)
            println("connected !!!!")
            ready = true
            println("READY")
        }
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        if (!ready) return

        // Get or create instrument player for this note
        val instrumentPlayer = InstrumentPlayer(instrument)
        instrumentPlayers.add(instrumentPlayer)

        // Trigger note on
        instrumentPlayer.noteOn(note)

        // Send noteOn event to audio worklet
        val frequency = noteToFrequency(note)
        audioWorkletNode.port.postMessage(
            json(
                "type" to "noteOn",
                "note" to note.ordinal,
                "frequency" to frequency,
            ),
        )
    }

    private fun noteToFrequency(note: Note): Double {
        // A4 = 440 Hz, using MIDI note numbers
        // C0 = MIDI 12, A4 = MIDI 69
        val midiNote = note.ordinal + 12 // Assuming Note enum starts at C0
        return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
    }

    override fun noteOff(note: Note) {
        if (!ready) return

        // Find the instrument player for this note
        instrumentPlayers.forEach { it.noteOff(note) }

        // Send noteOff event to audio worklet
        audioWorkletNode.port.postMessage(
            json(
                "type" to "noteOff",
                "note" to note.ordinal,
            ),
        )
    }

    override fun createSoundHandler(buffer: FloatArray): SoundHandler {
        TODO()
    }

    override fun createSoundHandler(buffer: Sequence<FloatArray>): SoundHandler {
        TODO("Not yet implemented")
    }

    override fun createSoundHandler(chunkGenerator: ChunkGenerator): SoundHandler {
        TODO("Not yet implemented")
    }

    companion object {
        private const val BUFFER = 4096 // Larger buffer for better stability
        private const val MAX_INSTRUMENTS = 8
    }
}
