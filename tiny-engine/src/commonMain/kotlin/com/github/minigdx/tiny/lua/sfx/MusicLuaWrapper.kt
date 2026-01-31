package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable

/**
 * Lua wrapper for MusicalSequence (music editing API).
 *
 * Provides access to:
 * - Global settings (bpm, volume, beats_per_section, section_count)
 * - Current section navigation
 * - Track access
 * - Section access
 * - Playback controls
 */
class MusicLuaWrapper(
    private val origin: Sound,
    private val sequence: MusicalSequence,
    private val soundBoard: VirtualSoundBoard,
) : WrapperLuaTable() {

    // Cache for track wrappers
    private val trackWrapperCache = mutableMapOf<Int, TrackLuaWrapper>()

    // Cache for section wrappers
    private val sectionWrapperCache = mutableMapOf<Int, SectionLuaWrapper>()

    init {
        // Read-only index
        wrap("index") { valueOf(sequence.index) }

        // BPM (tempo)
        wrap(
            "bpm",
            { valueOf(sequence.tempo) },
            { sequence.tempo = it.checkint() },
        )

        // Global volume
        wrap(
            "volume",
            { valueOf(sequence.volume.toDouble()) },
            { sequence.volume = it.checkdouble().toFloat().coerceIn(0f, 1f) },
        )

        // Beats per section
        wrap(
            "beats_per_section",
            { valueOf(sequence.beatsPerSection) },
            { sequence.beatsPerSection = it.checkint().coerceIn(1, 64) },
        )

        // Section count
        wrap(
            "section_count",
            { valueOf(sequence.sectionCount) },
            { sequence.sectionCount = it.checkint().coerceIn(1, 64) },
        )

        // Current section index
        wrap(
            "current_section",
            { valueOf(sequence.currentSectionIndex) },
            {
                sequence.currentSectionIndex = it.checkint()
                    .coerceIn(0, sequence.sectionCount - 1)
            },
        )

        // Track count (read-only)
        wrap("track_count") { valueOf(sequence.tracks.size) }

        // Access to a track by index
        function1("track") { arg ->
            val index = arg.checkint()
            getOrCreateTrackWrapper(index)
        }

        // Access to a section by index
        function1("section") { arg ->
            val index = arg.checkint()
            getOrCreateSectionWrapper(index)
        }

        // Play the current section
        function0("play") {
            val handler = soundBoard.prepare(sequence).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        // Play all sections (full song)
        function0("play_all") {
            val handler = soundBoard.prepare(sequence).also { it.play() }
            val result = WrapperLuaTable()
            result.function0("stop") {
                handler.stop()
                NONE
            }
            result
        }

        // Stop playback
        function0("stop") {
            // Sound board will handle stopping
            NONE
        }

        // List all music indices
        wrap("all") {
            val luaTable = LuaTable()
            origin.data.music.sequences.mapIndexed { index, _ -> index }.forEach {
                luaTable.insert(0, valueOf(it))
            }
            luaTable
        }
    }

    private fun getOrCreateTrackWrapper(index: Int): TrackLuaWrapper {
        return trackWrapperCache.getOrPut(index) {
            val track = sequence.tracks.getOrNull(index)
                ?: throw IllegalArgumentException("Track index $index out of range (0-${sequence.tracks.size - 1})")
            TrackLuaWrapper(origin, sequence, track, soundBoard)
        }
    }

    private fun getOrCreateSectionWrapper(index: Int): SectionLuaWrapper {
        return sectionWrapperCache.getOrPut(index) {
            SectionLuaWrapper(origin, sequence, index, soundBoard)
        }
    }
}
