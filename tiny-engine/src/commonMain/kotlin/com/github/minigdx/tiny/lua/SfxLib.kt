package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.Percent
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
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import kotlin.math.max
import kotlin.math.min

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

    abstract inner class WaveFunction : ThreeArgFunction() {
        private val notes = Note.values()

        override fun call(
            @TinyArg(
                "note",
                description = "Note from c0 to b8. Please check `note` for more information.",
            ) arg1: LuaValue,
            @TinyArg("duration", description = "Duration in the note in seconds (default 0.1 second)") arg2: LuaValue,
            @TinyArg("volume", description = "Volume express in percentage (between 0.0 and 1.0)") arg3: LuaValue,
        ): LuaValue {
            val note = if (arg1.isint()) {
                notes[arg1.checkint()]
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

    @TinyFunction("Generate and play a sine wave sound.")
    inner class sine : WaveFunction() {
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SineWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a sawtooth wave sound.")
    inner class sawtooth : WaveFunction() {
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SawTooth(note, duration, volume)
    }

    @TinyFunction("Generate and play a square wave sound.")
    inner class square : WaveFunction() {
        override fun wave(note: Note, duration: Seconds, volume: Percent) = SquareWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a triangle wave sound.")
    inner class triangle : WaveFunction() {
        override fun wave(note: Note, duration: Seconds, volume: Percent) = TriangleWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a noise wave sound.")
    inner class noise : WaveFunction() {
        override fun wave(note: Note, duration: Seconds, volume: Percent) = NoiseWave(note, duration, volume)
    }

    @TinyFunction("Generate and play a pulse wave sound.")
    inner class pulse : WaveFunction() {
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

    private fun canPlay(sound: Sound?): Sound? {
        return if (playSound) {
            sound
        } else {
            null
        }
    }
}
