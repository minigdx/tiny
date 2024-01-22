package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.resources.Sound
import com.github.minigdx.tiny.sound.NoiseWave
import com.github.minigdx.tiny.sound.PulseWave
import com.github.minigdx.tiny.sound.SawTooth
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.SquareWave
import com.github.minigdx.tiny.sound.TriangleWave
import com.github.minigdx.tiny.sound.WaveGenerator
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("sfx", "Sound API to play/loop/stop a sound.")
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
        ctrl.set("saw", sawtooth())
        arg2.set("sfx", ctrl)
        arg2.get("package").get("loaded").set("sfx", ctrl)
        return ctrl
    }

    abstract inner class WaveFunction : TwoArgFunction() {
        private val notes = Note.values()

        override fun call(
            @TinyArg("note", description = "Note from c0 to b8. Please check `note` for more information.") arg1: LuaValue,
            @TinyArg("duration", description = "Duration in the note in seconds (default 0.1 second)") arg2: LuaValue,
        ): LuaValue {
            val note = if (arg1.isint()) {
                notes[arg1.checkint()]
            } else {
                return NIL
            }

            val duration = arg2.optdouble(0.1)
            resourceAccess.note(wave(note, duration.toFloat()))
            return NIL
        }

        abstract fun wave(note: Note, duration: Seconds): WaveGenerator
    }

    @TinyFunction("Generate and play a sine wave sound.")
    inner class sine : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = SineWave(note, duration)
    }

    @TinyFunction("Generate and play a sawtooth wave sound.")
    inner class sawtooth : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = SawTooth(note, duration)
    }

    @TinyFunction("Generate and play a square wave sound.")
    inner class square : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = SquareWave(note, duration)
    }

    @TinyFunction("Generate and play a triangle wave sound.")
    inner class triangle : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = TriangleWave(note, duration)
    }

    @TinyFunction("Generate and play a noise wave sound.")
    inner class noise : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = NoiseWave(note, duration)
    }

    @TinyFunction("Generate and play a pulse wave sound.")
    inner class pulse : WaveFunction() {
        override fun wave(note: Note, duration: Seconds) = PulseWave(note, duration)
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

    private fun canPlay(sound: Sound?): Sound? {
        return if (playSound) {
            sound
        } else {
            null
        }
    }
}
