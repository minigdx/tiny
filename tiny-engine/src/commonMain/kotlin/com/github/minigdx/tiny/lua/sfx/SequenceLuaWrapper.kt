package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalPattern
import com.github.minigdx.tiny.sound.MusicalPhrase
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaValue
import kotlin.time.TimeSource.Monotonic.markNow

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

        wrap(
            "loopFrom",
            { valueOf(sequence.loopFrom) },
            { sequence.loopFrom = it.checkint() },
        )

        wrap(
            "pattern_count",
            { valueOf(sequence.patterns.size) },
        )

        // arrangement: read/write as a Lua table of ints (0-based pattern indices)
        wrap(
            "arrangement",
            {
                val t = WrapperLuaTable()
                sequence.arrangement.forEachIndexed { i, v ->
                    t.rawset(i + 1, valueOf(v))
                }
                t
            },
            { luaVal ->
                val t = luaVal.checktable() ?: return@wrap
                val list = mutableListOf<Int>()
                for (i in 1..t.length()) {
                    list.add(t.get(i).checkint())
                }
                sequence.arrangement = list
            },
        )

        // track(trackIndex, patternIndex) — patternIndex defaults to 0
        function2("track") { trackArg, patternArg ->
            val trackIndex = trackArg.checkint()
            val patternIndex = if (patternArg.isnil()) 0 else patternArg.checkint()
            val pattern = sequence.patterns.getOrNull(patternIndex) ?: return@function2 NIL
            val phrase = pattern.tracks.getOrNull(trackIndex) ?: return@function2 NIL
            TrackLuaWrapper(music, phrase)
        }

        // ensure_pattern(index) — create patterns up to the given index if they don't exist
        function1("ensure_pattern") { arg ->
            val patternIndex = arg.checkint()
            if (patternIndex >= 0 && patternIndex >= sequence.patterns.size) {
                sequence.patterns = Array(patternIndex + 1) { i ->
                    if (i < sequence.patterns.size) {
                        sequence.patterns[i]
                    } else {
                        MusicalPattern(
                            index = i,
                            tracks = Array(4) { channelIndex ->
                                MusicalPhrase(
                                    index = channelIndex,
                                    instrumentIndex = sequence.patterns.firstOrNull()
                                        ?.tracks?.getOrNull(channelIndex)?.instrumentIndex ?: 0,
                                ).also { phrase ->
                                    phrase.instrument = sequence.patterns.firstOrNull()
                                        ?.tracks?.getOrNull(channelIndex)?.instrument
                                    (0..32).forEach { beat ->
                                        phrase.addBeat(
                                            com.github.minigdx.tiny.sound.MusicalNote(null, beat.toFloat(), 1f, 1f),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            LuaValue.valueOf(sequence.patterns.size)
        }

        function0("invalidate") {
            cachedBuffer = null
            NONE
        }

        function0("play") {
            val startMark = markNow()
            val totalBeats = sequence.patterns.flatMap { it.tracks.toList() }.maxOf { it.beats.size }
            val buffer = cachedBuffer ?: soundBoard.convert(sequence).also { cachedBuffer = it }
            val bufferDurationSeconds = buffer.size.toDouble() / 44100.0
            val handler = soundBoard.createHandler(buffer, sequence.loopStartSample).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result.wrap("playing") { valueOf(handler.isPlaying()) }
            result.wrap("beat") {
                val elapsed = startMark.elapsedNow().toDouble(kotlin.time.DurationUnit.SECONDS)
                val elapsedInLoop = elapsed % bufferDurationSeconds
                valueOf(elapsedInLoop * totalBeats / bufferDurationSeconds)
            }
            result
        }

        function0("loop") {
            val startMark = markNow()
            val totalBeats = sequence.patterns.flatMap { it.tracks.toList() }.maxOf { it.beats.size }
            val buffer = cachedBuffer ?: soundBoard.convert(sequence).also { cachedBuffer = it }
            val bufferDurationSeconds = buffer.size.toDouble() / 44100.0
            val handler = soundBoard.createHandler(buffer, sequence.loopStartSample).also { it.loop() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result.wrap("playing") { valueOf(handler.isPlaying()) }
            result.wrap("beat") {
                val elapsed = startMark.elapsedNow().toDouble(kotlin.time.DurationUnit.SECONDS)
                val elapsedInLoop = elapsed % bufferDurationSeconds
                valueOf(elapsedInLoop * totalBeats / bufferDurationSeconds)
            }
            result
        }

        function0("export") {
            val buffer = cachedBuffer ?: soundBoard.convert(sequence).also { cachedBuffer = it }
            platform.saveWave(buffer)
            NONE
        }
    }
}
