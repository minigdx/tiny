package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator

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

    private fun toAudioBuffer(wave: WaveGenerator): AudioBuffer {
        val audioBuffer = audioContext.createBuffer(
            1,
            (wave.duration * SAMPLE_RATE).toInt(),
            SAMPLE_RATE,
        )
        val channel = audioBuffer.getChannelData(0)

        val numSamples: Int = (SAMPLE_RATE * wave.duration).toInt()

        val values = (0 until numSamples).map { index ->
            wave.generate(index)
        }
        channel.set(values.toTypedArray())
        return audioBuffer
    }
    override fun playNotes(notes: List<WaveGenerator>, longuestDuration: Seconds) {
        // FIXME: how to merge waves??
        val firstWave = notes.firstOrNull() ?: return
        val source = audioContext.createBufferSource()
        source.buffer = toAudioBuffer(firstWave)
        source.connect(audioContext.destination)
        source.start()
    }
}
