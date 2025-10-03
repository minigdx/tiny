package com.github.minigdx.tiny.platform.webgl

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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import web.audio.AudioContext
import web.audio.AudioContextState
import web.events.EventHandler

class WebSoundManager : SoundManager() {
    lateinit var audioContext: AudioContext

    var ready: Boolean = false

    private var nextStartTime: Double = 0.0

    private val instrumentPlayers = MutableFixedSizeList<InstrumentPlayer>(MAX_INSTRUMENTS)

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

        // Pre-generate first buffers to avoid initial gap
        generateSound()
        generateSound()

        soundContext.async {
            while (true) {
                // Generate next buffer when we're getting close to needing it
                val timeUntilNextBuffer = (nextStartTime - audioContext.currentTime) * 1000.0

                // Keep 2 buffers ahead to avoid underrun
                if (timeUntilNextBuffer < (BUFFER * 2000.0 / SAMPLE_RATE)) {
                    generateSound()
                }

                // Check more frequently than buffer duration
                delay(5)
            }
        }
    }

    private fun generateSound() {
        val buffer = audioContext.createBuffer(1, BUFFER, SAMPLE_RATE.toFloat())
        val floatData = buffer.getChannelData(0)

        (0 until BUFFER).forEach { sample ->
            floatData[sample] = 0f
            instrumentPlayers.forEach { instrumentPlayer ->
                floatData[sample] += instrumentPlayer.generate()
            }
            floatData[sample] = (floatData[sample] * MASTER_VOLUME).coerceIn(-1f, 1f)
        }

        // Remove finished instrument players
        instrumentPlayers.removeAll { it.isFinished() }

        val source = audioContext.createBufferSource()
        source.buffer = buffer
        source.connect(audioContext.destination)

        // Schedule buffer to play at precise time to avoid gaps
        if (nextStartTime < audioContext.currentTime) {
            nextStartTime = audioContext.currentTime
        }
        source.start(nextStartTime)

        // Calculate next buffer start time
        val bufferDuration = BUFFER.toDouble() / SAMPLE_RATE.toDouble()
        nextStartTime += bufferDuration
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        println("NOTE_ON $note $ready")
        if (!ready) return

        // Get or create instrument player for this note
        val instrumentPlayer = InstrumentPlayer(instrument)
        instrumentPlayers.add(instrumentPlayer)

        // Trigger note on
        instrumentPlayer.noteOn(note)
    }

    override fun noteOff(note: Note) {
        println("NOTE_OFF $note $ready")
        if (!ready) return

        // Find the instrument player for this note
        instrumentPlayers.forEach { it.noteOff(note) }
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
