package com.github.minigdx.tiny.sound

import java.io.ByteArrayInputStream
import javax.sound.midi.Sequencer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

class SfxSound(byteArray: ByteArray) : Sound {
    private val clip: Clip

    init {
        val audioFormat = AudioFormat(SoundManager.SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val audioStream = AudioInputStream(ByteArrayInputStream(byteArray), audioFormat, byteArray.size.toLong())
        clip = AudioSystem.getClip()
        clip.open(audioStream)
    }

    override fun play() {
        stop()
        clip.start()
    }

    override fun loop() {
        clip.loop(Sequencer.LOOP_CONTINUOUSLY)
    }

    override fun stop() {
        clip.stop()
        clip.framePosition = 0
    }
}
