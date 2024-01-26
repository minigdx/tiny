package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.sound.Sound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class PicoAudioSound(val audio: dynamic, val smf: dynamic) : Sound {
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

class SfxSound(
    private val buffer: Float32Array,
    private val picoAudioSoundMananger: PicoAudioSoundMananger,
) : Sound {

    private var currentSource: AudioBufferSourceNode? = null

    override fun play() {
        stop()
        currentSource = picoAudioSoundMananger.playSfxBuffer(buffer)
    }

    override fun loop() {
        currentSource = picoAudioSoundMananger.playSfxBuffer(buffer, loop = true)
    }

    override fun stop() {
        currentSource?.stop()
    }
}

class PicoAudioSoundMananger : SoundManager {

    lateinit var audioContext: AudioContext

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
    }

    override suspend fun createSfxSound(bytes: ByteArray): Sound {
        val score = bytes.decodeToString()
        val duration = 60f / 120f / 4.0f
        val waves = SfxLib.convertScoreToWaves(score, duration)
        val buffer = generateSfxBuffer((waves.size * duration * SAMPLE_RATE).toInt(), waves)
        return SfxSound(buffer, this)
    }

    override suspend fun createMidiSound(data: ByteArray): Sound {
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

    override fun playSfx(notes: List<WaveGenerator>) {
        if (notes.isEmpty()) return

        val numSamples: Int = (SAMPLE_RATE * notes.first().duration * notes.size).toInt()

        val result = generateSfxBuffer(numSamples, notes)

        playSfxBuffer(result)
    }

    internal fun playSfxBuffer(result: Float32Array, loop: Boolean = false): AudioBufferSourceNode {
        val sfxBuffer = audioContext.createBuffer(
            1,
            result.length,
            SAMPLE_RATE,
        )

        val channel = sfxBuffer.getChannelData(0)
        channel.set(result)

        val source = audioContext.createBufferSource()
        source.buffer = sfxBuffer
        source.connect(audioContext.destination)
        source.loop = loop
        source.start()
        return source
    }

    internal fun generateSfxBuffer(
        numSamples: Int,
        notes: List<WaveGenerator>,
    ): Float32Array {
        var currentIndex = 0
        val result = Float32Array(numSamples)

        notes.forEach {
            val buffer = toAudioBuffer(listOf(it), it.duration)
            result.set(buffer.getChannelData(0), currentIndex)
            currentIndex += buffer.getChannelData(0).length
        }
        return result
    }

    override fun playNotes(notes: List<WaveGenerator>, longestDuration: Seconds) {
        if (notes.isEmpty()) return

        val source = audioContext.createBufferSource()
        source.buffer = toAudioBuffer(notes, longestDuration)
        source.connect(audioContext.destination)
        source.start()
    }
}
