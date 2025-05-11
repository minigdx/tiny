package com.github.minigdx.tiny.platform.webgl

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.w3c.dom.events.Event
import kotlin.js.Promise

// https://github.com/seisuke/pikot8/blob/main/shared/src/jsMain/kotlin/io/github/seisuke/pikot8/AudioContext.kt
external class AudioContext {
    val sampleRate: Float
    val destination: AudioNode
    val baseLatency: Double // seconds, experimental
    val outputLatency: Double // seconds, experimental
    val state: String // should be running or suspended

    fun close()

    fun createOscillator(): OscillatorNode

    fun createBuffer(
        numOfChannels: Int,
        length: Int,
        sampleRate: Int,
    ): AudioBuffer

    fun createBufferSource(): AudioBufferSourceNode

    fun decodeAudioData(data: ArrayBuffer): Promise<AudioBuffer>

    fun createGain(): GainNode

    fun resume()

    var onstatechange: (() -> Unit)?
}

open external class AudioNode {
    var onended: ((Event) -> Unit)?

    fun connect(
        destination: AudioNode,
        output: Int = definedExternally,
        input: Int = definedExternally,
    ): AudioNode
}

external class OscillatorNode : AudioNode {
    fun start(time: Double = definedExternally)
}

external class AudioBuffer {
    fun getChannelData(channel: Int): Float32Array
}

external class AudioBufferSourceNode : AudioNode {
    fun start(time: Double = definedExternally)

    fun stop(time: Double = definedExternally)

    var buffer: AudioBuffer
    var loop: Boolean
}

external class AudioParam {
    val defaultValue: Float
    val maxValue: Float
    val minValue: Float
    var value: Float
}

external class GainNode : AudioNode {
    val gain: AudioParam
}
