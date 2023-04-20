package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import java.io.ByteArrayInputStream
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer

class JavaMidiSound(private val data: ByteArray) : MidiSound {

    private var sequencer: Sequencer? = null

    override fun play() {
        val seq: Sequencer = MidiSystem.getSequencer()

        seq.open()

        val sequence = MidiSystem.getSequence(ByteArrayInputStream(data))
        seq.sequence = sequence

        sequencer = seq
        seq.start()
    }

    override fun loop(loop: Int) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        sequencer?.stop()
    }
}

class JavaMidiSoundManager : SoundManager {
    override fun initSoundManager(inputHandler: InputHandler) = Unit

    override suspend fun createSound(data: ByteArray): MidiSound {
        return JavaMidiSound(data)
    }
}
