package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard

class SequenceLuaWrapper(
    private val music: Music,
    private val sequence: MusicalSequence,
    private val soundBoard: VirtualSoundBoard,
) : WrapperLuaTable() {
    init {
        wrap(
            "index",
            { valueOf(sequence.index) },
        )

        wrap(
            "tempo",
            { valueOf(sequence.tempo) },
            { sequence.tempo = it.checkint() },
        )

        function1("track") { arg ->
            val index = arg.checkint()
            val track = sequence.tracks.getOrNull(index) ?: return@function1 NIL
            TrackLuaWrapper(music, track)
        }

        function0("play") {
            val handler = soundBoard.prepare(sequence).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result.wrap("playing") { valueOf(handler.isPlaying()) }
            result
        }
    }
}
