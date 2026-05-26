package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.BufferedChunkGenerator
import com.github.minigdx.tiny.sound.ChunkGenerator
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

    // True once the AudioWorklet module is loaded. Only gates noteOn/noteOff,
    // which actually need the worklet node. SFX playback via playChunkGenerator
    // uses regular AudioBufferSource nodes and does not depend on this flag.
    var workletReady: Boolean = false

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
            }
        }
        // Start loading the AudioWorklet immediately. addModule() works on a suspended
        // AudioContext, so by the time the user interacts (required by browser autoplay
        // policy before any sound can play) the worklet is most likely already loaded.
        // Without this, Firefox and Safari hit a race where the first sound.sfx call
        // right after the first input lands before addModule resolves.
        initializeAudioWorklet()

        // Resume the AudioContext on the first user interaction (browser autoplay policy).
        if (audioContext.state != AudioContextState.running) {
            inputHandler.onFirstUserInteraction {
                audioContext.resumeAsync()
            }
        }
    }

    private val soundContext = CoroutineScope(Dispatchers.Main)

    private fun initializeAudioWorklet() {
        nextStartTime = audioContext.currentTime

        soundContext.launch {
            // Load the bundled worklet from Vite assets
            audioContext.audioWorklet.addModule(SynthesizerAudioWorklet)
            audioWorkletNode = AudioWorkletNode(audioContext, AudioWorkletProcessorName("SynthesizerProcessor"))
            val destinationNode = audioContext.destination
            audioWorkletNode.connect(destinationNode)
            workletReady = true
        }
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        if (!workletReady) return

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
        if (!workletReady) return

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

    /**
     * Play audio data from a ChunkGenerator using standard Web Audio API nodes.
     * This method extracts all audio data from the generator, creates an AudioBuffer,
     * and plays it using an AudioBufferSourceNode (bypassing the AudioWorklet).
     *
     * @param chunkGenerator The generator containing the audio sample data
     * @param loop Whether to loop the audio playback
     * @return AudioBufferSourceNode that can be used to control playback (e.g., stop())
     * @throws IllegalStateException if the AudioContext has not been initialized yet
     * @throws IllegalArgumentException if the ChunkGenerator produces no audio data
     */
    fun playChunkGenerator(
        chunkGenerator: ChunkGenerator,
        loop: Boolean = false,
        loopStartSample: Int = 0,
    ): web.audio.AudioBufferSourceNode {
        if (!::audioContext.isInitialized) {
            throw IllegalStateException("AudioContext is not initialized")
        }
        // If the AudioContext is still suspended (e.g. autoplay policy not yet satisfied),
        // try to resume it. A buffer source started on a suspended context will start
        // playing as soon as the context becomes running.
        if (audioContext.state != AudioContextState.running) {
            audioContext.resumeAsync()
        }

        // Extract all audio data from the chunk generator
        val audioData = mutableListOf<Float>()
        val chunkSizeToRequest = 2205 // ~50ms at 44.1kHz (SAMPLE_RATE * 0.05)

        // Generate chunks until we've extracted all data
        var continueReading = true
        while (continueReading) {
            val chunk = chunkGenerator.generateChunk(chunkSizeToRequest)

            if (chunk.size == 0) {
                // No more data available
                continueReading = false
            } else {
                // Copy chunk data to our accumulator
                for (i in 0 until chunk.size) {
                    audioData.add(chunk[i])
                }

                // If we got less than requested, we're likely at the end
                if (chunk.size < chunkSizeToRequest) {
                    continueReading = false
                }
            }
        }

        if (audioData.isEmpty()) {
            throw IllegalArgumentException("ChunkGenerator produced no audio data")
        }

        // Create AudioBuffer
        val sampleCount = audioData.size
        val audioBuffer = audioContext.createBuffer(
            // Mono
            numberOfChannels = 1,
            length = sampleCount,
            // 44.1 kHz
            sampleRate = SAMPLE_RATE.toFloat(),
        )

        // Get the channel data and copy our float samples
        val channelData = audioBuffer.getChannelData(0)
        for (i in 0 until sampleCount) {
            channelData[i] = audioData[i]
        }

        // Create buffer source node
        val sourceNode = audioContext.createBufferSource()
        sourceNode.buffer = audioBuffer
        sourceNode.loop = loop
        if (loop && loopStartSample > 0) {
            sourceNode.loopStart = loopStartSample.toDouble() / SAMPLE_RATE.toDouble()
        }

        // Connect directly to destination (bypass AudioWorklet)
        sourceNode.connect(audioContext.destination)

        // Start playback immediately
        sourceNode.start()

        return sourceNode
    }

    override fun createSoundHandler(
        buffer: FloatArray,
        loopStartSample: Int,
    ): SoundHandler {
        val handler = WebSoundHandler(BufferedChunkGenerator(buffer, loopStartSample), this, loopStartSample)
        addSoundHandler(handler)
        return handler
    }

    companion object {
        private const val MAX_INSTRUMENTS = 8
    }
}
