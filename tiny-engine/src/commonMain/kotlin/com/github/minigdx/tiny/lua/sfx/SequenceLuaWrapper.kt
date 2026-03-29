package com.github.minigdx.tiny.lua.sfx

import com.github.minigdx.tiny.lua.WrapperLuaTable
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicConfiguration
import com.github.minigdx.tiny.sound.MusicGenerator
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

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

        wrap(
            "config",
            {
                val config = sequence.configuration ?: return@wrap NIL
                configToLuaTable(config)
            },
        )

        function1("generate") { arg ->
            val config = luaTableToConfig(arg.checktable()!!)
            MusicGenerator.generate(sequence, config)
            sequence.configuration = config
            // Link instruments after generation
            sequence.tracks.forEach { track ->
                track.instrument = music.instruments.getOrNull(track.instrumentIndex)
            }
            cachedBuffer = null
            NONE
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

    companion object {
        fun configToLuaTable(config: MusicConfiguration): LuaValue {
            val table = LuaTable()
            table.set("root", LuaValue.valueOf(config.root))
            table.set("scale_name", LuaValue.valueOf(config.scaleName))
            table.set("progression_name", LuaValue.valueOf(config.progressionName))
            table.set("lead_style", LuaValue.valueOf(config.leadStyle))
            table.set("drum_pattern", LuaValue.valueOf(config.drumPattern))
            table.set("chord_instrument", LuaValue.valueOf(config.chordInstrument))
            table.set("bass_instrument", LuaValue.valueOf(config.bassInstrument))
            table.set("lead_instrument", LuaValue.valueOf(config.leadInstrument))
            table.set("drum_instrument", LuaValue.valueOf(config.drumInstrument))
            table.set("chord_volume", LuaValue.valueOf(config.chordVolume.toDouble()))
            table.set("bass_volume", LuaValue.valueOf(config.bassVolume.toDouble()))
            table.set("lead_volume", LuaValue.valueOf(config.leadVolume.toDouble()))
            table.set("drum_volume", LuaValue.valueOf(config.drumVolume.toDouble()))
            table.set("bpm", LuaValue.valueOf(config.bpm))
            table.set("seed", LuaValue.valueOf(config.seed.toDouble()))
            return table
        }

        fun luaTableToConfig(table: LuaTable): MusicConfiguration {
            return MusicConfiguration(
                root = table.get("root").optjstring("C") ?: "C",
                scaleName = table.get("scale_name").optjstring("Major") ?: "Major",
                progressionName = table.get("progression_name").optjstring("Classic") ?: "Classic",
                leadStyle = table.get("lead_style").optjstring("Stepwise") ?: "Stepwise",
                drumPattern = table.get("drum_pattern").optjstring("Rock") ?: "Rock",
                chordInstrument = table.get("chord_instrument").optint(0),
                bassInstrument = table.get("bass_instrument").optint(1),
                leadInstrument = table.get("lead_instrument").optint(2),
                drumInstrument = table.get("drum_instrument").optint(3),
                chordVolume = table.get("chord_volume").optdouble(0.3).toFloat(),
                bassVolume = table.get("bass_volume").optdouble(0.4).toFloat(),
                leadVolume = table.get("lead_volume").optdouble(0.25).toFloat(),
                drumVolume = table.get("drum_volume").optdouble(0.35).toFloat(),
                bpm = table.get("bpm").optint(120),
                seed = table.get("seed").optlong(42L),
            )
        }
    }
}
