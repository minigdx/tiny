package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.ChunkGenerator
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.InstrumentPlayer
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set
import web.audio.AudioContext
import web.audio.AudioContextState
import web.audio.AudioWorkletNode
import web.events.EventHandler

class WebSoundManager : SoundManager() {
    lateinit var audioContext: AudioContext
    private var audioWorkletNode: AudioWorkletNode? = null

    var ready: Boolean = false

    // Active instrument players by note
    private val activeInstrumentPlayers = mutableMapOf<Note, InstrumentPlayer>()

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
        audioContext.onstatechange = EventHandler {
            // See: https://developer.mozilla.org/en-US/docs/Web/API/BaseAudioContext/state#resuming_interrupted_play_states_in_ios_safari
            // Resume the audio context if interrupted, only on iOS
            if (audioContext.state != AudioContextState.running) {
                audioContext.resumeAsync()
            } else {
                ready = false
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

    @Suppress("UNUSED_VARIABLE")
    private fun initializeAudioWorklet() {
    }

    override fun noteOn(
        note: Note,
        instrument: Instrument,
    ) {
        println("NOTE_ON $note $ready")
        if (!ready) return

        // Get or create instrument player for this note
        val instrumentPlayer = activeInstrumentPlayers.getOrPut(note) {
            InstrumentPlayer(instrument)
        }

        // Trigger note on
        instrumentPlayer.noteOn(note)
    }

    override fun noteOff(note: Note) {
        println("NOTE_OFF $note $ready")
        if (!ready) return

        // Find the instrument player for this note
        val instrumentPlayer = activeInstrumentPlayers[note] ?: return

        // Trigger note off (will handle release phase)
        instrumentPlayer.noteOff(note)
        // Note: We don't remove from map here - let the audio process callback
        // remove it after the release phase is complete
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

    private fun convertBuffer(buffer: FloatArray): Float32Array {
        val length = buffer.size
        val result = Float32Array(length)
        (0 until length).forEach { index ->
            val byte = buffer[index]
            result[index] = byte
        }
        return result
    }
}
