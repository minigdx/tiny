package com.github.minigdx.tiny.platform.glfw

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.SoundManager.Companion.SAMPLE_RATE
import com.github.minigdx.tiny.sound.WaveGenerator
import java.io.ByteArrayInputStream
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Sequencer.LOOP_CONTINUOUSLY
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.experimental.and
import kotlin.experimental.or

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
            if (isOpen) {
                close()
            }
        }
    }
}

class JavaMidiSoundManager : SoundManager {

    private lateinit var notesLine: SourceDataLine

    private var buffer = ByteArray(0)

    override fun initSoundManager(inputHandler: InputHandler) {
        notesLine = AudioSystem.getSourceDataLine(
            AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SoundManager.SAMPLE_RATE.toFloat(),
                16,
                1, // TODO: set 2 to get Stereo
                2,
                SoundManager.SAMPLE_RATE.toFloat(),
                false,
            ),
        )

        notesLine.open()
        notesLine.start()
    }

    override suspend fun createSound(data: ByteArray): MidiSound {
        return JavaMidiSound(data)
    }

    override fun playNotes(notes: List<WaveGenerator>, longuestDuration: Seconds) {
        if (notes.isEmpty()) return

        val sampleRate = SAMPLE_RATE

        // TODO: boucle sur les notes.
        //   regarder dans le buffer et faire un + entre note et buffer
        //   write le tout et let's go
        //   mettre a jour la List : LiveNote(note, duration, type)
        buffer = ByteArray((longuestDuration * sampleRate).toInt() * 2)
        notes.forEach { wave ->
            val numSamples: Int = (sampleRate * wave.duration).toInt()

            for (i in 0 until numSamples) {
                val byteA = buffer[2 * i] and 0xFF.toByte()
                val byteB = (buffer[2 * i + 1]).toInt().shl(8).toByte()

                // TODO: le mix de note n'est pas bon.
                // TODO: la fin du son n'est pas ouf.
                val actualBuffer = byteA.or(byteB).toShort()

                val sample = wave.generate(i)
                val sampleValue = (Short.MAX_VALUE * sample).toInt().toShort() + actualBuffer

                buffer[2 * i] = (sampleValue and 0xFF).toByte()
                buffer[2 * i + 1] = ((sampleValue shr 8) and 0xFF).toByte()
            }
        }

        // generate the byte array
        notesLine.write(buffer, 0, buffer.size)
    }
}
