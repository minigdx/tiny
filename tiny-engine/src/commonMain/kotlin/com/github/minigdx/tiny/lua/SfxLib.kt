package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.Beat
import com.github.minigdx.tiny.sound.NoiseWave
import com.github.minigdx.tiny.sound.Pattern
import com.github.minigdx.tiny.sound.PulseWave
import com.github.minigdx.tiny.sound.SawToothWave
import com.github.minigdx.tiny.sound.SilenceWave
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.Song
import com.github.minigdx.tiny.sound.SquareWave
import com.github.minigdx.tiny.sound.TriangleWave
import com.github.minigdx.tiny.sound.WaveGenerator
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.max
import kotlin.math.min

@TinyLib(
    "sfx",
    """Sound API to play/loop/stop a sound.
A sound can be an SFX sound, generated using the tiny-cli sfx command or a MIDI file.
Please note that a SFX sound will produce the same sound whatever platform and whatever computer
as the sound is generated. 

A MIDI sound will depend of the MIDI synthesizer available on the machine.
  
WARNING: Because of browser behaviour, a sound can *only* be played only after the first 
user interaction. 

Avoid to start a music or a sound at the beginning of the game.
Before it, force the player to hit a key or click by adding an interactive menu 
or by starting the sound as soon as the player is moving.
""",
)
class SfxLib(
    private val resourceAccess: GameResourceAccess,
    // When validating the script, don't play sound
    private val playSound: Boolean = true,
) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("play", play())
        ctrl.set("loop", loop())
        ctrl.set("stop", stop())
        ctrl.set("sine", sine())
        ctrl.set("square", square())
        ctrl.set("triangle", triangle())
        ctrl.set("noise", noise())
        ctrl.set("pulse", pulse())
        ctrl.set("sawtooth", sawtooth())
        ctrl.set("to_table", toTable())
        ctrl.set("sfx", sfx())
        arg2.set("sfx", ctrl)
        arg2.get("package").get("loaded").set("sfx", ctrl)
        return ctrl
    }

    abstract inner class WaveFunction : ThreeArgFunction() {
        private val notes = Note.values().associateBy { it.index }

        override fun call(
            @TinyArg(
                "note",
                description = "Note from c0 to b8. Please check `note` for more information.",
            ) arg1: LuaValue,
            @TinyArg("duration", description = "Duration in the note in seconds (default 0.1 second)") arg2: LuaValue,
            @TinyArg("volume", description = "Volume express in percentage (between 0.0 and 1.0)") arg3: LuaValue,
        ): LuaValue {
            val note = if (arg1.isint()) {
                notes[arg1.checkint()] ?: return NIL
            } else {
                return NIL
            }

            val duration = arg2.optdouble(0.1)
            val volume = max(min(arg3.optdouble(1.0), 1.0), 0.0)
            resourceAccess.note(wave(note, duration.toFloat(), volume.toFloat()))
            return NIL
        }

        abstract fun wave(note: Note, duration: Seconds, volume: Percent): WaveGenerator
    }

    @TinyFunction("Generate and play a sine wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class sine : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SineWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a sawtooth wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class sawtooth : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SawToothWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a square wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class square : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SquareWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a triangle wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class triangle : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = TriangleWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a noise wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class noise : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = NoiseWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a pulse wave sound.", example = SFX_WAVE_EXAMPLE)
    inner class pulse : WaveFunction() {
        @TinyCall("Generate and play a sound using one note.")
        override fun wave(note: Note, duration: Seconds, volume: Percent) = PulseWave(note, duration, volume)
    }

    @TinyFunction(
        "Play a sound by it's index. " +
            "The index of a sound is given by it's position in the sounds field from the `_tiny.json` file." +
            "The first sound is at the index 0.",
    )
    inner class play : OneArgFunction() {

        @TinyCall("Play the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            canPlay(resourceAccess.sound(index))?.play()
            return NIL
        }
    }

    @TinyFunction("Play a sound and loop over it.")
    inner class loop : OneArgFunction() {

        @TinyCall("Play the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Play the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            canPlay(resourceAccess.sound(index))?.loop()
            return NIL
        }
    }

    @TinyFunction("Stop a sound.")
    inner class stop : OneArgFunction() {

        @TinyCall("Stop the sound at the index 0.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Stop the sound by it's index.")
        override fun call(@TinyArg("sound") arg: LuaValue): LuaValue {
            val index = if (arg.isnumber()) {
                arg.checkint()
            } else {
                0
            }
            canPlay(resourceAccess.sound(index))?.stop()
            return NIL
        }
    }

    inner class toTable : OneArgFunction() {

        private fun Beat.toLuaTable(): LuaTable {
            val beat = LuaTable()
            notes.forEach { wave ->
                val note = LuaTable()
                note.set("type", wave.name)
                note.set("index", wave.index)
                note.set("note", wave.note.index)
                beat.insert(0, note)
            }
            return beat
        }

        override fun call(arg: LuaValue): LuaValue {
            val score = arg.optjstring(null) ?: return NIL
            val song = convertScoreToSong(score)

            val patterns = LuaTable()
            song.patterns.forEach { (index, pattern) ->
                val beats = LuaTable()
                pattern.beats.forEach { beat ->
                    beats.insert(beat.index, beat.toLuaTable())
                }
                patterns.insert(index, beats)
            }
            val result = LuaTable()
            result["bpm"] = valueOf(song.bpm)
            result["volume"] = valueOf(song.volume.toDouble())
            result["patterns"] = patterns
            return result
        }
    }

    inner class sfx : TwoArgFunction() {

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val score = arg1.optjstring("")!!
            val waves = convertScoreToSong(score)
            resourceAccess.sfx(waves)
            return NIL
        }
    }

    private fun canPlay(sound: Sound?): Sound? {
        return if (playSound) {
            sound
        } else {
            null
        }
    }

    companion object {

        fun convertToWave(note: String, duration: Seconds): WaveGenerator {
            val wave = note.substring(0, 2).toInt(16)
            val noteIndex = note.substring(2, 4).toInt(16)
            val volume = note.substring(4, 6).toInt(16) / 255f

            return when (wave) {
                1 -> SineWave(Note.fromIndex(noteIndex), duration, volume)
                2 -> SquareWave(Note.fromIndex(noteIndex), duration, volume)
                3 -> TriangleWave(Note.fromIndex(noteIndex), duration, volume)
                4 -> NoiseWave(Note.fromIndex(noteIndex), duration, volume)
                5 -> PulseWave(Note.fromIndex(noteIndex), duration, volume)
                6 -> SawToothWave(Note.fromIndex(noteIndex), duration, volume)
                else -> SilenceWave(duration)
            }
        }

        fun convertScoreToSong(score: String): Song {
            val lines = score.lines()
            if (lines.isEmpty()) {
                throw IllegalArgumentException(
                    "The content of the score is empty. Can't convert it into a song. " +
                        "Check if the score is not empty or correctly loaded!",
                )
            }

            val header = lines.first()
            if (!header.startsWith(TINY_SFX_HEADER)) {
                throw IllegalArgumentException(
                    "The '$TINY_SFX_HEADER' is missing from the fist line of the score. " +
                        "Is the score a valid score?",
                )
            }

            val (_, nbPattern, bpm, volume) = header.split(" ")

            val duration = 60f / bpm.toFloat() / 8f

            // Map<Index, Pattern>
            val patterns = lines.drop(1).take(nbPattern.toInt()).mapIndexed { indexPattern, pattern ->
                val beatsStr = pattern.trim().split(" ")
                val beats = convertToBeats(beatsStr, duration)
                Pattern(indexPattern + 1, beats)
            }.associateBy { it.index }

            val patternOrder = lines.drop(nbPattern.toInt() + 1).firstOrNull()
            val orders = if (patternOrder.isNullOrBlank()) {
                listOf(1)
            } else {
                patternOrder.trim().split(" ").map { it.toInt() }
            }

            val patternsOrdered = orders.map { patterns[it]!! }

            return Song(bpm.toInt(), volume.toInt() / 255f, patterns, patternsOrdered)
        }

        private fun convertToBeats(beatsStr: List<String>, duration: Seconds): List<Beat> {
            val beats = beatsStr
                .asSequence()
                .mapIndexed { index, beat ->
                    val notes = beat.split(":")
                        .asSequence()
                        .filter { it.isNotBlank() }
                        .map { note -> convertToWave(note, duration) }
                    Beat(index + 1, notes.toList())
                }
            return beats.toList()
        }

        private const val TINY_SFX_HEADER = "tiny-sfx"
    }
}
