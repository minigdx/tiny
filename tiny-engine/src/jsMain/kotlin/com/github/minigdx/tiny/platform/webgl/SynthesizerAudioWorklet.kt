package com.github.minigdx.tiny.platform.webgl

import web.audio.AudioWorkletModule
import web.audio.AudioWorkletProcessorName
import web.audio.registerProcessor

val SynthesizerAudioWorklet = AudioWorkletModule { self ->
    self.registerProcessor(AudioWorkletProcessorName("SynthesizerProcessor"), SynthesizerProcessor)
}
