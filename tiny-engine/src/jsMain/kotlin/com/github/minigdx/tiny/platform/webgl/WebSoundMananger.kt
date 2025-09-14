package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.SoundManager
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

class WebSoundMananger : SoundManager() {
    lateinit var audioContext: AudioContext

    var ready: Boolean = false

    override fun initSoundManager(inputHandler: InputHandler) {
        audioContext = AudioContext()
        audioContext.onstatechange = {
            // See: https://developer.mozilla.org/en-US/docs/Web/API/BaseAudioContext/state#resuming_interrupted_play_states_in_ios_safari
            // Resume the audio context if interrupted, only on iOS
            if (audioContext.state != "running") {
                audioContext.resume()
            } else {
                ready = false
            }
        }
        if (audioContext.state != "running") {
            inputHandler.onFirstUserInteraction {
                audioContext.resume()
            }
        } else {
            ready = true
        }
    }

    override fun createSoundHandler(buffer: FloatArray): SoundHandler {
        val result = convertBuffer(buffer)
        // return WebSoundHandler(result, this)
        TODO()
    }

    override fun createSoundHandler(buffer: Sequence<FloatArray>): SoundHandler {
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

    internal fun playSfxBuffer(
        result: Float32Array,
        loop: Boolean = false,
    ): AudioBufferSourceNode {
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
