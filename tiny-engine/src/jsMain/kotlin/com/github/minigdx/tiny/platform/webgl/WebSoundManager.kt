package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.InstrumentPlayer
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.util.MutableFixedSizeList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import web.audio.AudioContext
import web.audio.AudioContextState
import web.audio.AudioWorkletNode
import web.audio.AudioWorkletProcessorName
import web.audio.running
import web.events.EventHandler
import web.worklets.addModule
import kotlin.js.json
import kotlin.math.pow

class WebSoundManager : SoundManager() {
    lateinit var audioContext: AudioContext

    var ready: Boolean = false

    private var nextStartTime: Double = 0.0

    private val instrumentPlayers = MutableFixedSizeList<InstrumentPlayer>(MAX_INSTRUMENTS)

    private lateinit var audioWorkletNode: AudioWorkletNode

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
        audioContext.onstatechange = EventHandler {
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
                audioContext.resumeAsync()
                initializeAudioWorklet()
            }
        } else {
            initializeAudioWorklet()
        }
    }

    private val soundContext = CoroutineScope(Dispatchers.Main)

    private fun initializeAudioWorklet() {
        nextStartTime = audioContext.currentTime

        soundContext.launch {
            // Load the bundled worklet from Vite assets
            val result = audioContext.audioWorklet.addModule(SynthesizerAudioWorklet)
            audioWorkletNode = AudioWorkletNode(audioContext, AudioWorkletProcessorName("SynthesizerProcessor"))
            val destinationNode = audioContext.destination
            audioWorkletNode.connect(destinationNode)
            ready = true
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
        val instrumentJson = Json.encodeToString(instrument)

        audioWorkletNode.port.postMessage(
            json(
                "type" to "noteOn",
                "note" to note.ordinal,
                "frequency" to frequency,
                "instrument" to instrumentJson,
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

    companion object {
        private const val MAX_INSTRUMENTS = 8
    }
}
