package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.sound.SoundHandler
import com.github.minigdx.tiny.sound.VirtualSoundBoard
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * MusicLib provides playback capabilities for pre-computed music sequences.
 * Music sequences are pre-rendered at startup for optimal playback performance.
 */
@TinyLib(
    "music",
    """Music API to play/loop/stop pre-computed music sequences.
Music sequences are pre-rendered at startup for optimal performance.

WARNING: Because of browser behaviour, a sound can *only* be played after the first
user interaction.

Avoid to start music at the beginning of the game.
Before it, force the player to hit a key or click by adding an interactive menu
or by starting the music as soon as the player is moving.
""",
)
class MusicLib(
    private val resourceAccess: GameResourceAccess,
    private val soundBoard: VirtualSoundBoard,
    // When validating the script, don't play sound
    private val playSound: Boolean = true,
) : TwoArgFunction() {

    // Track all active handlers so they can be stopped with music.stop()
    private val activeHandlers = mutableListOf<SoundHandler>()

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()

        ctrl.set("play", play())
        ctrl.set("stop", stop())

        arg2.set("music", ctrl)
        arg2.get("package").get("loaded").set("music", ctrl)
        return ctrl
    }

    @TinyFunction("Play a pre-computed music sequence.")
    inner class play : TwoArgFunction() {
        @TinyCall("Play a pre-computed music at music_index. Returns a handler with stop() method. The music can be looped.")
        override fun call(
            @TinyArg("music_index", type = LuaType.NUMBER)
            arg1: LuaValue,
            @TinyArg("loop", type = LuaType.BOOLEAN)
            arg2: LuaValue,
        ): LuaValue {
            val loop = arg2.optboolean(false)
            val index = arg1.checkint()
            val buffer = getPrecomputedMusic(index)

            val result = WrapperLuaTable()
            if (playSound && buffer != null && buffer.isNotEmpty()) {
                val handler = soundBoard.prepareFromBuffer(buffer)
                activeHandlers.add(handler)

                result.function0("stop") {
                    handler.stop()
                    activeHandlers.remove(handler)
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

    @TinyFunction("Stop all currently playing music.")
    inner class stop : ZeroArgFunction() {
        @TinyCall("Stop all currently playing music.")
        override fun call(): LuaValue {
            val toBeRemoved = activeHandlers.toList()
            toBeRemoved.forEach { it.stop() }
            activeHandlers.clear()
            return NONE
        }
    }

    /**
     * Get the pre-computed music buffer by index.
     * The buffer is pre-rendered at startup.
     */
    private fun getPrecomputedMusic(index: Int): FloatArray? {
        val sound = resourceAccess.findSound(0) ?: return null
        val musicBuffers = sound.data.musicalSequences

        return if (index in 0 until musicBuffers.size) {
            musicBuffers[index]
        } else {
            null
        }
    }
}
