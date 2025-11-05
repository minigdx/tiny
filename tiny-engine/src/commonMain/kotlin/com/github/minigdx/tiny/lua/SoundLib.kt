package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.MusicalBar
import com.github.minigdx.tiny.sound.MusicalSequence
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "sound",
    """Sound API to play/loop/stop a sound.
A sound can be created using the sound editor, using the command line `tiny-cli sfx <filename>`.

WARNING: Because of browser behaviour, a sound can *only* be played only after the first
user interaction.

Avoid to start a music or a sound at the beginning of the game.
Before it, force the player to hit a key or click by adding an interactive menu
or by starting the sound as soon as the player is moving.
""",
)
class SoundLib(
    private val resourceAccess: GameResourceAccess,
    private val soundBoard: VirtualSoundBoard,
    // When validating the script, don't play sound
    private val playSound: Boolean = true,
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()

        ctrl.set("sfx", sfx())
        // TODO later :P
        // ctrl.set("music", music())
        ctrl.set("note", note())

        arg2.set("sound", ctrl)
        arg2.get("package").get("loaded").set("sound", ctrl)
        return ctrl
    }

    @TinyFunction("Play a sfx.")
    inner class sfx() : TwoArgFunction() {
        @TinyCall("Play a sfx at sfx_index. The sfx can be looped.")
        override fun call(
            @TinyArg("sfx_index", type = LuaType.NUMBER)
            arg1: LuaValue,
            @TinyArg("loop", type = LuaType.BOOLEAN)
            arg2: LuaValue,
        ): LuaValue {
            val loop = arg2.optboolean(false)
            val sfx = getSfx(arg1.checkint())

            val result = WrapperLuaTable()
            if (playSound && sfx != null) {
                val handler = soundBoard.prepare(sfx)

                result.function0("stop") {
                    handler.stop()
                    NONE
                }

                if (loop) {
                    handler.loop()
                } else {
                    handler.play()
                }
            } else {
                result.function0("stop") {
                    NONE
                }
            }
            return result
        }
    }

    @TinyFunction("Play a music")
    inner class music() : TwoArgFunction() {
        @TinyCall("Play the music at the index music_index. The music can be looped.")
        override fun call(
            @TinyArg("music_index", type = LuaType.NUMBER)
            arg1: LuaValue,
            @TinyArg("loop", type = LuaType.BOOLEAN)
            arg2: LuaValue,
        ): LuaValue {
            val loop = arg2.optboolean(false)
            val music = getMusic(arg1.checkint())
            val result = WrapperLuaTable()
            if (playSound && music != null) {
                val handler = soundBoard.prepare(music)
                result.function0("stop") {
                    handler.stop()
                    NONE
                }

                if (loop) {
                    handler.loop()
                } else {
                    handler.play()
                }
            } else {
                result.function0("stop") {
                    NONE
                }
            }

            return result
        }
    }

    @TinyFunction("Play a note by an instrument until it's stopped", example = NOTE_EXAMPLE)
    inner class note() : TwoArgFunction() {
        @TinyCall("Play the note note_name using the instrument at instrument_index")
        override fun call(
            @TinyArg("note_name", type = LuaType.STRING)
            arg1: LuaValue,
            @TinyArg("instrument_index", type = LuaType.NUMBER)
            arg2: LuaValue,
        ): LuaValue {
            val note = Note.fromName(arg1.checkjstring()!!)
            val instrumentIndex = arg2.optint(0)
            val result = WrapperLuaTable()
            result.function0("stop") {
                soundBoard.noteOff(note)
                NONE
            }
            val instrument = getInstrument(instrumentIndex)
            if (playSound && instrument != null) {
                soundBoard.noteOn(note, instrument)
            }
            return result
        }
    }

    private fun getInstrument(index: Int): Instrument? {
        val instruments = resourceAccess.findSound(0)
            ?.data
            ?.music
            ?.instruments ?: return null
        return if (index in 0 until instruments.size) {
            instruments[index]
        } else {
            null
        }
    }

    private fun getSfx(index: Int): MusicalBar? {
        val sfx = resourceAccess.findSound(0)
            ?.data
            ?.music
            ?.musicalBars ?: return null

        return if (index in 0 until sfx.size) {
            sfx[index]
        } else {
            null
        }
    }

    private fun getMusic(index: Int): MusicalSequence? {
        val music = resourceAccess.findSound(0)
            ?.data
            ?.music
            ?.sequences ?: return null

        return if (index in 0 until music.size) {
            music[index]
        } else {
            null
        }
    }
}
