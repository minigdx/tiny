package com.github.minigdx.tiny.platform.webgl

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.sound.MidiSound
import com.github.minigdx.tiny.sound.SoundManager

class PicoAudioSound(val audio: dynamic, val smf: dynamic) : MidiSound {
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

class PicoAudioSoundMananger : SoundManager {

    override fun initSoundManager(inputHandler: InputHandler) = Unit

    override suspend fun createSound(data: ByteArray): MidiSound {
        val audio = js("var PicoAudio = require('picoaudio'); new PicoAudio.default()")
        val smf = audio.parseSMF(data)
        return PicoAudioSound(audio, smf)
    }

    override fun playNotes(notes: List<Pair<Note, Seconds>>) {
        TODO("Not yet implemented")
    }
}
