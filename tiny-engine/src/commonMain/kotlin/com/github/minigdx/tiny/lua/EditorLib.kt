package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.lua.sfx.InstrumentLuaWrapper
import com.github.minigdx.tiny.lua.sfx.MusicLuaWrapper
import com.github.minigdx.tiny.lua.sfx.SfxLuaWrapper
import com.github.minigdx.tiny.platform.Platform
import com.github.minigdx.tiny.sound.Instrument
import com.github.minigdx.tiny.sound.Music
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * EditorLib provides editing capabilities for music and SFX.
 * This library is intended for use in the SFX editor and allows creating,
 * modifying, and playing sounds in real-time with computation done just before playback.
 */
@TinyLib(
    "editor",
    """Editor API to create and edit music and SFX.
This library is intended for use in the SFX editor.
It allows creating, modifying, and playing sounds.
Sounds are computed just before playback.""",
)
class EditorLib(
    private val resourceAccess: GameResourceAccess,
    private val soundBoard: VirtualSoundBoard,
    private val platform: Platform,
    // When validating the script, don't play sound
    private val playSound: Boolean = true,
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()

        ctrl.set("instrument", instrument())
        ctrl.set("sfx", sfx())
        ctrl.set("music", music())
        ctrl.set("save", save())

        arg2.set("editor", ctrl)
        arg2.get("package").get("loaded").set("editor", ctrl)
        return ctrl
    }

    // Cache for instrument Lua wrappers
    private val instrumentWrapperCache = mutableMapOf<Int, LuaValue>()

    @TinyFunction("Save the actual music/sfx data in the current sfx file.")
    inner class save : ZeroArgFunction() {
        @TinyCall("Save the actual music/sfx data in the current sfx file.")
        override fun call(): LuaValue {
            val soundFile = resourceAccess.findSound(0) ?: return NIL

            val content = soundFile.data.music.serialize()
            platform.saveIntoGameDirectory(soundFile.name, content)

            return NONE
        }
    }

    @TinyFunction("Access instrument using its index or its name.")
    inner class instrument : LibFunction() {
        @TinyCall("Access instrument using its index or its name.")
        override fun call(a: LuaValue): LuaValue {
            val music = resourceAccess.findSound(0)?.data?.music ?: return NIL

            val index = a.asInstrumentIndex(music) ?: return NIL

            // Check cache first
            return instrumentWrapperCache[index]
                // Check music
                ?: music.instruments.getOrNull(index)?.let { instrument ->
                    createInstrumentWrapper(music, instrument).also {
                        // Cache the wrapper
                        instrumentWrapperCache[index] = it
                    }
                }
                // Give up
                ?: NIL
        }

        @TinyCall(
            "Access instrument using its index or its name. " +
                "Create it if the instrument is missing and the flag is true.",
        )
        override fun call(
            a: LuaValue,
            b: LuaValue,
        ): LuaValue {
            val instrument = call(a)
            val music = resourceAccess.findSound(0)?.data?.music ?: return NIL
            return if (instrument == NIL && b.optboolean(false)) {
                val index = a.toint()
                val newInstrument = Instrument(index)
                music.instruments[index] = newInstrument
                // Clear cache for this index since we're creating new instrument
                instrumentWrapperCache.remove(index)
                createInstrumentWrapper(music, newInstrument)
            } else {
                instrument
            }
        }

        private fun createInstrumentWrapper(
            music: Music,
            instrument: Instrument,
        ): InstrumentLuaWrapper {
            return InstrumentLuaWrapper(music, instrument, soundBoard)
        }
    }

    @TinyFunction("Access sfx using its index for editing.")
    inner class sfx : OneArgFunction() {
        @TinyCall("Access sfx using its index for editing. Returns a wrapper that can be used to edit and play the SFX.")
        override fun call(arg: LuaValue): LuaValue {
            val sound = resourceAccess.findSound(0) ?: return NIL

            val index = arg.checkint()
            return sound.data.music.musicalBars
                .getOrNull(index)
                ?.let { SfxLuaWrapper(sound, it, soundBoard, platform) } ?: NIL
        }
    }

    @TinyFunction("Access music sequence using its index for editing.")
    inner class music : OneArgFunction() {
        @TinyCall("Access music sequence using its index for editing. Returns a wrapper that can be used to edit and play the music.")
        override fun call(arg: LuaValue): LuaValue {
            val sound = resourceAccess.findSound(0) ?: return NIL

            val index = arg.checkint()
            return sound.data.music.sequences
                .getOrNull(index)
                ?.let { MusicLuaWrapper(sound, it, soundBoard, platform) } ?: NIL
        }
    }

    private fun LuaValue.asInstrumentIndex(music: Music): Int? {
        return if (this.isint()) {
            val index = this.checkint()
            music.instruments
                .firstOrNull { it?.index == index }
                ?.index
        } else {
            music.instruments
                .firstOrNull { inst -> inst?.name == this.checkjstring() }
                ?.index
        }
    }
}
