package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.sound.Sound
import com.github.minigdx.tiny.sound.SoundManager
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

class PicoAudioSoundMananger : SoundManager() {

    lateinit var audioContext: AudioContext

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
    }

    override suspend fun createSfxSound(bytes: ByteArray): Sound {
        val score = bytes.decodeToString()
        val song = SfxLib.convertScoreToSong(score)
        val (buf, length) = createBufferFromSong(song)
        val buffer = convertBuffer(buf, length)
        return SfxSound(buffer, this)
    }

    override suspend fun createMidiSound(data: ByteArray): Sound {
        val audio = js("var PicoAudio = require('picoaudio'); new PicoAudio.default()")
        val smf = audio.parseSMF(data)
        return PicoAudioSound(audio, smf)
    }

    override fun playBuffer(buffer: FloatArray, numberOfSamples: Long) {
        val result = convertBuffer(buffer, numberOfSamples)
        playSfxBuffer(result)
    }

    private fun convertBuffer(buffer: FloatArray, length: Long): Float32Array {
        val result = Float32Array(length.toInt())
        (0 until length.toInt()).forEach { index ->
            val byte = buffer[index]
            result[index] = byte
        }
        return result
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
}
