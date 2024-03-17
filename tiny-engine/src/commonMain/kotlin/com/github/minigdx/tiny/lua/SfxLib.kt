package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.Envelope
import com.github.minigdx.tiny.sound.Modulation
import com.github.minigdx.tiny.sound.Noise2
import com.github.minigdx.tiny.sound.NoiseWave
import com.github.minigdx.tiny.sound.Pattern2
import com.github.minigdx.tiny.sound.Pulse2
import com.github.minigdx.tiny.sound.PulseWave
import com.github.minigdx.tiny.sound.SawTooth2
import com.github.minigdx.tiny.sound.SawToothWave
import com.github.minigdx.tiny.sound.Silence2
import com.github.minigdx.tiny.sound.Sine2
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.Song2
import com.github.minigdx.tiny.sound.SoundGenerator
import com.github.minigdx.tiny.sound.Square2
import com.github.minigdx.tiny.sound.SquareWave
import com.github.minigdx.tiny.sound.Sweep
import com.github.minigdx.tiny.sound.Track
import com.github.minigdx.tiny.sound.Triangle2
import com.github.minigdx.tiny.sound.TriangleWave
import com.github.minigdx.tiny.sound.Vibrato
import com.github.minigdx.tiny.sound.WaveGenerator
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import kotlin.math.floor
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
        ctrl.set("to_score", toScore())
        ctrl.set("empty_score", emptyScore())
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

    inner class toScore : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val table = arg.checktable() ?: return NIL
            val bpm = table["bpm"].toint()
            val beatDuration = (60f / bpm / 8f)

            val tracksTable = table["tracks"].checktable()!!
            val tracks = tracksTable.keys().map { key ->
                val track = tracksTable[key].checktable()!!

                val envelope = track["env"].checktable()!!.toEnv()
                val mod = track["mod"].checktable()!!.toMod()

                val patternsTable = track["patterns"].checktable()!!
                val patterns = patternsTable.keys().map { pkey ->
                    patternsTable[pkey].checktable()!!.toPattern(pkey.toint(), mod, envelope)
                }.associateBy { it.index }

                val musicTable = track["music"].checktable()!!
                val music = musicTable.keys().map {
                    val index = musicTable[it].toint()
                    patterns[index]!!
                }

                Track(patterns, music, beatDuration, envelope, mod)
            }
            val score = Song2(
                bpm = bpm,
                volume = table["volume"].toint() / 255f,
                tracks = tracks.toTypedArray(),
            ).toString()

            return valueOf(score)
        }

        fun LuaTable.toWave(mod: Modulation?, env: Envelope): SoundGenerator {
            val noteIndex = this["note"].toint()
            val volume = this["volume"].toint() / 255f
            return when (this["type"].tojstring()) {
                "Sine" -> Sine2(Note.fromIndex(noteIndex), mod, env, volume)
                "Square" -> Square2(Note.fromIndex(noteIndex), mod, env, volume)
                "Triangle" -> Triangle2(Note.fromIndex(noteIndex), mod, env, volume)
                "Noise" -> Noise2(Note.fromIndex(noteIndex), mod, env, volume)
                "Pulse" -> Pulse2(Note.fromIndex(noteIndex), mod, env, volume)
                "Sawtooth" -> SawTooth2(Note.fromIndex(noteIndex), mod, env, volume)
                else -> Silence2(Note.C0, null, null, 0f)
            }
        }

        fun LuaTable.toEnv(): Envelope {
            val envelope = Envelope(
                (this["attack"].todouble().toFloat() / 255f),
                (this["decay"].todouble().toFloat() / 255f),
                this["sustain"].todouble().toFloat() / 255f,
                (this["release"].todouble().toFloat() / 255f),
            )

            return envelope
        }

        fun LuaTable.toPattern(index: Int, mod: Modulation?, env: Envelope): Pattern2 {
            val notes = this.keys().map { key ->
                this[key].checktable()!!.toWave(mod, env)
            }
            return Pattern2(index, notes)
        }

        fun LuaTable.toMod(): Modulation? {
            val mod = when (this["type"].toint()) {
                1 -> Sweep(
                    Note.fromIndex(this["a"].toint()).frequency.toInt(),
                    this["b"].toint() == 1,
                )

                2 -> Vibrato(
                    Note.fromIndex(this["a"].toint()).frequency,
                    this["b"].toint() / 255f,
                )

                else -> null
            }
            return mod
        }
    }

    inner class toTable : OneArgFunction() {

        private fun SoundGenerator.toLuaTable(): LuaTable {
            val note = LuaTable()
            note.set("type", this.name)
            note.set("index", this.index)
            note.set("note", this.note.index)
            note.set("volume", (this.volume * 255).toInt())
            return note
        }

        override fun call(arg: LuaValue): LuaValue {
            val score = arg.optjstring(null) ?: return NIL
            val song = convertScoreToSong2(score)

            val tracks = LuaTable()
            song.tracks.forEach { t ->
                val track = LuaTable()
                val inv = 1 / t.beatDuration

                val env = LuaTable()
                env["attack"] = valueOf(((t.envelope?.attack ?: 0f) * 255 * inv).toInt())
                env["decay"] = valueOf(((t.envelope?.decay ?: 0f) * 255 * inv).toInt())
                env["sustain"] = valueOf(((t.envelope?.sustain ?: 0f) * 255).toInt())
                env["release"] = valueOf(((t.envelope?.release ?: 0f) * 255 * inv).toInt())

                track["env"] = env

                val modulation = t.modulation

                val mod = LuaTable()
                when (modulation) {
                    is Sweep -> {
                        mod["type"] = 1
                        mod["a"] = Note.fromFrequency(modulation.sweep).index
                        mod["b"] = if (modulation.acceleration) 1 else 0
                        mod["c"] = 0
                        mod["d"] = 0
                    }

                    is Vibrato -> {
                        mod["type"] = 2
                        mod["a"] = Note.fromFrequency(modulation.vibratoFrequency).index
                        mod["b"] = valueOf((modulation.depth.toDouble() * 255).toInt())
                        mod["c"] = 0
                        mod["d"] = 0
                    }

                    null -> {
                        mod["type"] = 0
                        mod["a"] = 0
                        mod["b"] = 0
                        mod["c"] = 0
                        mod["d"] = 0
                    }
                }
                track["mod"] = mod

                val patterns = LuaTable()
                t.patterns.forEach { (index, pattern) ->
                    val notes = LuaTable()
                    pattern.notes.forEach { note ->
                        notes.insert(0, note.toLuaTable())
                    }
                    patterns.insert(index, notes)
                }
                track["patterns"] = patterns

                val music = LuaTable()
                t.music.map { it.index }.forEach {
                    music.insert(0, valueOf(it))
                }
                track["music"] = music

                tracks.insert(0, track)
            }

            val result = LuaTable()
            result["bpm"] = valueOf(song.bpm)
            result["volume"] = valueOf(floor(song.volume.toDouble() * 255))
            result["tracks"] = tracks

            return result
        }
    }

    inner class emptyScore : ZeroArgFunction() {
        override fun call(): LuaValue {
            val pattern = Pattern2(1, emptyList())
            val track = Track(
                mapOf(1 to pattern),
                listOf(pattern),
                (60f / 120 / 8f),
                Envelope(0.1f, 0f, 1f, 0.1f),
                null,
            )
            val emptyTrack = Track(
                emptyMap(),
                emptyList(),
                (60f / 120 / 8f),
                Envelope(0.1f, 0f, 1f, 0.1f),
                null,
            )
            val song = Song2(120, 0.5f, arrayOf(track, emptyTrack, emptyTrack, emptyTrack))
            return valueOf(song.toString())
        }
    }

    inner class sfx : TwoArgFunction() {

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val score = arg1.optjstring("")!!
            val waves = convertScoreToSong2(score)
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

        fun convertToSound(note: String, mod: Modulation?, env: Envelope?): SoundGenerator {
            val wave = note.substring(0, 2).toInt(16)
            val noteIndex = note.substring(2, 4).toInt(16)
            val volume = note.substring(4, 6).toInt(16) / 255f

            return when (wave) {
                1 -> Sine2(Note.fromIndex(noteIndex), mod, env, volume)
                2 -> Square2(Note.fromIndex(noteIndex), mod, env, volume)
                3 -> Triangle2(Note.fromIndex(noteIndex), mod, env, volume)
                4 -> Noise2(Note.fromIndex(noteIndex), mod, env, volume)
                5 -> Pulse2(Note.fromIndex(noteIndex), mod, env, volume)
                6 -> SawTooth2(Note.fromIndex(noteIndex), mod, env, volume)
                else -> Silence2(Note.C0, null, null, 0f)
            }
        }

        /**
         * tiny-sfx <bpm> <volume>
         * <nb pattern track> <mod active> <mod param1> <mod param2> <mod param3> <mod param4> <env active> <env param1> <env param2> <env param3> <env param4>
         * <wave type><note><volume> ...
         * ...nb pattern...
         * <pattern index order>
         */
        fun convertScoreToSong2(score: String): Song2 {
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

            val (_, bpm, volume) = header.split(" ")

            val duration = 60f / bpm.toFloat() / 8f

            var tail = lines.drop(1)

            var remainingTrack = 4

            val tracks = mutableListOf<Track>()
            do {
                val track = tail.first()
                val configuration = track.split(" ").map { it.toInt(16) }
                val nbPattern = configuration.first()
                val (env, envA, envB, envC, envD) = configuration.drop(1)
                val (mod, modA, modB, _, _) = configuration.drop(6)

                val modulation = when (mod) {
                    1 -> {
                        Sweep(
                            sweep = Note.fromIndex(modA).frequency.toInt(),
                            acceleration = modB / 255f > 0.5f,
                        )
                    }

                    2 -> {
                        Vibrato(
                            vibratoFrequency = Note.fromIndex(modA).frequency,
                            depth = modB / 255f,
                        )
                    }

                    else -> null
                }

                val envelope = if (env > 0) {
                    Envelope(
                        attack = (envA / 255f) * duration,
                        decay = (envB / 255f) * duration,
                        sustain = envC / 255f,
                        release = (envD / 255f) * duration,
                    )
                } else {
                    null
                }
                val (patterns, patternsOrdered) = if (nbPattern > 0) {
                    val patternsStr = tail.drop(1)

                    // Map<Index, Pattern>
                    val patterns = patternsStr.take(nbPattern).mapIndexed { indexPattern, pattern ->
                        val beatsStr = pattern.trim().split(" ")
                        val beats = convertToSound(beatsStr, modulation, envelope)
                        Pattern2(indexPattern + 1, beats)
                    }.associateBy { it.index }

                    val patternOrder = patternsStr.drop(nbPattern).first()
                    val orders = patternOrder.trim().split(" ").map { it.toInt() }

                    val patternsOrdered = orders.map { patterns[it]!! }
                    tail = tail.drop(nbPattern + 2) // drop patterns + configuration + patterns order
                    patterns to patternsOrdered
                } else {
                    tail = tail.drop(1) // drop  configuration
                    emptyMap<Int, Pattern2>() to emptyList()
                }

                tracks.add(Track(patterns, patternsOrdered, duration, envelope = envelope, modulation = modulation))

                remainingTrack--
            } while (remainingTrack > 0)

            return Song2(bpm.toInt(), volume.toInt() / 255f, tracks.toTypedArray())
        }

        private fun convertToSound(beatsStr: List<String>, mod: Modulation?, env: Envelope?): List<SoundGenerator> {
            val beats = beatsStr
                .asSequence()
                .filter { it.isNotBlank() }
                .map { beat -> convertToSound(beat, mod, env) }
            return beats.toList()
        }

        private const val TINY_SFX_HEADER = "tiny-sfx"
    }
}
