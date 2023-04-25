package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import java.io.ByteArrayInputStream
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Sequencer.LOOP_CONTINUOUSLY

class JavaMidiSound(private val data: ByteArray) : MidiSound {

    private var sequencer: Sequencer? = null

    private fun _play(loop: Int) {
        val seq: Sequencer = MidiSystem.getSequencer()

        seq.open()

        val sequence = MidiSystem.getSequence(ByteArrayInputStream(data))
        seq.sequence = sequence

        sequencer = seq

        seq.loopCount = loop
        seq.start()
    }

    override fun play() {
        _play(0)
    }

    override fun loop() {
        _play(LOOP_CONTINUOUSLY)
    }

    override fun stop() {
        sequencer?.run {
            if (isRunning) {
                stop()
            }
            if (isOpen)
                close()
        }
    }
}

class JavaMidiSoundManager : SoundManager {
    override fun initSoundManager(inputHandler: InputHandler) = Unit

    override suspend fun createSound(data: ByteArray): MidiSound {
        return JavaMidiSound(data)
    }
}
