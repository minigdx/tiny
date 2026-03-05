package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard

class SequenceLuaWrapper(
    private val music: Music,
    private val sequence: MusicalSequence,
    private val soundBoard: VirtualSoundBoard,
    private val platform: Platform,
) : WrapperLuaTable() {

    private var cachedBuffer: FloatArray? = null

    init {
        wrap(
            "index",
            { valueOf(sequence.index) },
        )

        wrap(
            "name",
            { sequence.name?.let { valueOf(it) } ?: NIL },
            { sequence.name = it.optjstring(null) },
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

        function0("invalidate") {
            cachedBuffer = null
            NONE
        }

        function0("play") {
            val buffer = cachedBuffer ?: soundBoard.convert(sequence).also { cachedBuffer = it }
            val handler = soundBoard.createHandler(buffer).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result.wrap("playing") { valueOf(handler.isPlaying()) }
            result
        }

        function0("export") {
            val buffer = cachedBuffer ?: soundBoard.convert(sequence).also { cachedBuffer = it }
            platform.saveWave(buffer)
            NONE
        }
    }
}
