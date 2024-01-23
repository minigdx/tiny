package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class PicoAudioSound(val audio: dynamic, val smf: dynamic) : MidiSound {
    override fun play() {
        audio.init()
        audio.setData(smf)
        audio.play()
    }

    override fun loop() {
        audio.init()
        audio.setData(smf)
        audio.play(true)
    }

    override fun stop() {
        audio.stop()
    }
}

class PicoAudioSoundMananger : SoundManager {

    lateinit var audioContext: AudioContext

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
    }

    override suspend fun createSound(data: ByteArray): MidiSound {
        val audio = js("var PicoAudio = require('picoaudio'); new PicoAudio.default()")
        val smf = audio.parseSMF(data)
        return PicoAudioSound(audio, smf)
    }

    private fun toAudioBuffer(notes: List<WaveGenerator>, longestDuration: Seconds): AudioBuffer {
        val numSamples = (longestDuration * SAMPLE_RATE).toInt()
        val fadeOutIndex = getFadeOutIndex(longestDuration)

        val audioBuffer = audioContext.createBuffer(
            1,
            numSamples,
            SAMPLE_RATE,
        )
        val channel = audioBuffer.getChannelData(0)

        val result = Float32Array(numSamples)
        (0 until numSamples).forEach { index ->
            val signal = fadeOut(mix(index, notes), index, fadeOutIndex, numSamples)
            result[index] = signal
        }
        channel.set(result)
        return audioBuffer
    }

    override fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds) {
        if (notes.isEmpty()) return

        val source = audioContext.createBufferSource()
        source.buffer = toAudioBuffer(notes, longestDuration)
        source.connect(audioContext.destination)
        source.start()
    }
}
