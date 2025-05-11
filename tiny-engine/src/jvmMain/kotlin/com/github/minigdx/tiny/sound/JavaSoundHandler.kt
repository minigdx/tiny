package com.github.minigdx.tiny.sound

class JavaSoundHandler(
    val data: FloatArray,
    private val mixerGateway: MixerGateway,
) : SoundHandler {
    var position = 0

    var loop: Boolean = false
    var stop: Boolean = false

    override fun play() {
        loop = false
        mixerGateway.add(this)
    }

    override fun loop() {
        loop = true
        mixerGateway.add(this)
    }

    override fun stop() {
        stop = true
    }
}
