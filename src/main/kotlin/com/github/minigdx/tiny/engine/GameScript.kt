package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.graphic.FrameBuffer
import com.github.minigdx.tiny.lua.TinyLib
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib

class GameScript(val name: String, gameOption: GameOption) {

    var exited: Boolean = false
    var evaluated: Boolean = false
    var loading: Boolean = false
    var reloaded: Boolean = false

    var content: ByteArray = ByteArray(0)

    private var initFunction: LuaValue? = null
    private var updateFunction: LuaValue? = null
    private var drawFunction: LuaValue? = null
    private var setStateFunction: LuaValue? = null
    private var getStateFunction: LuaValue? = null

    private var globals: Globals? = null

    internal val frameBuffer = FrameBuffer(gameOption.width, gameOption.width)

    class State(val args: LuaValue)

    private fun createLuaGlobals(): Globals = Globals().apply {
        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib())
        load(CoroutineLib())
        load(TinyLib(this@GameScript))
        LoadState.install(this)
        LuaC.install(this)
    }

    fun isValid(): Boolean {
        return try {
            createLuaGlobals().load(String(content))
            true
        } catch (exception: LuaError) {
            println("Can't parse '$name' as the file is not valid.")
            exception.printStackTrace()
            false
        }
    }


    fun evaluate() {
        globals = createLuaGlobals()

        evaluated = true
        loading = false
        reloaded = false
        exited = false

        globals?.load(String(content))?.call()

        initFunction = globals?.get("init")?.nullIfNil()
        updateFunction = globals?.get("update")?.nullIfNil()
        drawFunction = globals?.get("draw")?.nullIfNil()
        getStateFunction = globals?.get("getState")?.nullIfNil()
        setStateFunction = globals?.get("setState")?.nullIfNil()

        initFunction?.call()
    }

    fun getState(): State? {
        val data = getStateFunction?.call()
        return if (data != null && !data.isnil()) {
            return State(data)
        } else {
            null
        }
    }

    fun setState(state: State? = null) {
        if (state != null) {
            setStateFunction?.call(state.args)
        }

    }

    fun advance() {
        updateFunction?.call()
        drawFunction?.call()
    }

    private fun LuaValue.nullIfNil(): LuaValue? {
        return if (this.isnil()) {
            null
        } else {
            this
        }
    }

    override fun toString(): String {
        return """--- LUA SCRIPT ($name) ---
existed: $exited | evaluated: $evaluated | loading: $loading | reloaded: $reloaded |
init: ${initFunction != null} | update: ${updateFunction != null} | draw: ${drawFunction != null} |
            ----
            ${String(content)}
"""
    }
}
