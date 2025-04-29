package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.MusicalNote
import kotlinx.serialization.json.Json
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

/**
 *
 * envelope(attack, delay, substain, release)
 * instrument(List<Harmonie>, envelope)
 *
 * note (pitch, duration)
 * bars (List<note>, instrument)
 * track (List<bars>)
 * sequence(List<track>)
 * music(List<sequence)
 *
 *
 * sfx.instrument("Piano").name = "Guitar" // rename the instrument Piano to Guitar.
 * local instru = sfx.instrument(3) // set the current instrument
 * sfx.instrument(3).attack = 0.2 // set the attack of the current instrument.
 * instru.wave = 2
 * sfx.note(C1, 3, 0.5).play() // play C1 during 3 seconds using the current instrument and half the volume;
 *
 * sfx.bars(2) // Load the bars number 2
 *
 * sfx.beat(5, C1, 3, 0.5) // Set the beat 5 with the note C1.
 * sfx.bars(2).play()
 * local seq = sfx.sequence(2)
 * local track = seq.track(1)
 * track.solo()
 * track.play()
 *
 * sequence.play()
 *
 *
 * sfx.music(1) // load another music
 * sfx.save("toto") // save actual music
 *
 *
 */

@TinyLib(
    "sfx",
    """Sound API to play/loop/stop a sound.
A sound can be created using the sound editor, using the command line `tiny-cli sfx <filename>`.
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
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("play", play())
        ctrl.set("loop", loop())
        ctrl.set("stop", stop())

        ctrl.set("instrument", instrument())
        ctrl.set("bar", bar())

        ctrl.set("save", save())

        arg2.set("sfx", ctrl)
        arg2.get("package").get("loaded").set("sfx", ctrl)
        return ctrl
    }

    private var currentMusic: Music? = null

    fun getCurrentMusic(): Music {
        return currentMusic ?: Music().also { currentMusic = it }
    }

    @TinyFunction("Save the actual music using the filename.")
    inner class save : OneArgFunction() {
        @TinyCall("Save the actual music using the filename")
        override fun call(
            @TinyArg("filename") arg: LuaValue,
        ): LuaValue {
            val music = getCurrentMusic()
            val content = Json.encodeToString(music)
            resourceAccess.save(arg.checkjstring()!!, content)
            return NONE
        }
    }

    @TinyFunction("Access instrument using its index or its name.")
    inner class instrument : OneArgFunction() {
        @TinyCall("Access instrument using its index or its name.")
        override fun call(arg: LuaValue): LuaValue {
            val music = getCurrentMusic()
            val index = arg.asInstrumentIndex(music) ?: return NIL
            return music.instruments
                .getOrNull(index)
                ?.toLua() ?: NIL
        }

        fun Instrument.toLua(): LuaValue {
            val obj = WrapperLuaTable()

            obj.wrap("index") { valueOf(this.index) }
            obj.wrap(
                "name",
                { this.name?.let { valueOf(it) } ?: NIL },
                { this.name = it.optjstring(null) },
            )
            obj.wrap(
                "wave",
                { valueOf(this.wave.name) },
                {
                    this.wave = it.checkjstring()
                        ?.let { Instrument.WaveType.valueOf(it) }
                        ?: Instrument.WaveType.SINE
                },
            )
            obj.wrap(
                "attack",
                { valueOf(this.attack.toDouble()) },
                { this.attack = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "decay",
                { valueOf(this.decay.toDouble()) },
                { this.decay = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "sustain",
                { valueOf(this.sustain.toDouble()) },
                { this.sustain = it.optdouble(0.0).toFloat() },
            )
            obj.wrap(
                "release",
                { valueOf(this.release.toDouble()) },
                { this.release = it.optdouble(0.0).toFloat() },
            )

            obj.function1("play") { noteAsString ->
                val hardVolume = 0.8f

                val oneNote =
                    MusicalBar(
                        1,
                        this,
                        tempo = 120,
                    ).apply {
                        setNotes(
                            listOf(
                                MusicalNote(
                                    note = Note.fromName(noteAsString.tojstring()),
                                    beat = 0f,
                                    duration = 1f,
                                    volume = hardVolume,
                                ),
                            ),
                        )
                    }

                resourceAccess.play(oneNote)
                NONE
            }

            obj.wrap("harmonics") {
                WrapperLuaTable().apply {
                    (0 until this@toLua.harmonics.size).forEach { index ->
                        wrap(
                            "${index + 1}",
                            { valueOf(this@toLua.harmonics[index].toDouble()) },
                            { this@toLua.harmonics[index] = it.tofloat() },
                        )
                    }
                }
            }

            return obj
        }
    }

    @TinyFunction("Access instrument using its index or its name.")
    inner class bar : OneArgFunction() {
        @TinyCall("Access instrument using its index or its name.")
        override fun call(arg: LuaValue): LuaValue {
            val music = getCurrentMusic()
            val index = arg.checkint()
            return music.musicalBars
                .getOrNull(index)
                ?.toLua(music) ?: NIL
        }

        fun MusicalBar.toLua(music: Music): LuaValue {
            val obj = WrapperLuaTable()

            obj.wrap(
                "index",
                { valueOf(this.index) },
            )

            obj.wrap(
                "bpm",
                { valueOf(this.tempo) },
                { this.tempo = it.checkint() },
            )

            obj.function1("set_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val note = Note.fromIndex(49 - arg["note"].toint())
                val duration = arg["duration"].todouble().toFloat()

                this.setNote(note, beat, duration)

                NONE
            }

            obj.function1("remove_note") { arg ->
                val beat = arg["beat"].todouble().toFloat()
                val note = Note.fromIndex(49 - arg["note"].toint())

                this.removeNote(note, beat)

                NONE
            }

            obj.function0("play") {
                resourceAccess.play(this)
                NONE
            }

            obj.function1("instrument") { arg ->
                if (arg.isnil()) {
                    this.instrument?.let { valueOf(it.index) } ?: NIL
                } else {
                    // Set the instrument for the bar.
                    val index = arg.asInstrumentIndex(music) ?: return@function1 NIL
                    val instrument = music.instruments.getOrNull(index) ?: return@function1 NIL
                    this.instrument = instrument
                    NONE
                }
            }

            return obj
        }
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
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val index =
                if (arg.isnumber()) {
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
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val index =
                if (arg.isnumber()) {
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
        override fun call(
            @TinyArg("sound") arg: LuaValue,
        ): LuaValue {
            val index =
                if (arg.isnumber()) {
                    arg.checkint()
                } else {
                    0
                }
            canPlay(resourceAccess.sound(index))?.stop()
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

    private fun LuaValue.asInstrumentIndex(music: Music): Int? {
        return if (this.isint()) {
            this.checkint() % music.instruments.size
        } else {
            music.instruments
                .firstOrNull { inst -> inst.name == this.checkjstring() }
                ?.index
        }
    }
}
