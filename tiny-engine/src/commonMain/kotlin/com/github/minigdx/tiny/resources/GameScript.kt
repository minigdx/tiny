package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.lua.CtrlLib
import com.github.minigdx.tiny.lua.GfxLib
import com.github.minigdx.tiny.lua.JuiceLib
import com.github.minigdx.tiny.lua.MapLib
import com.github.minigdx.tiny.lua.MathLib
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.lua.ShapeLib
import com.github.minigdx.tiny.lua.SprLib
import com.github.minigdx.tiny.lua.StdLib
import com.github.minigdx.tiny.lua.StdLibListener
import com.github.minigdx.tiny.lua.errorLine
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib

class GameScript(
    /**
     * Index of game script
     */
    override val index: Int,
    /**
     * Name of the game script
     */
    override val name: String,
    val gameOptions: GameOptions,
    val inputHandler: InputHandler,
    override val type: ResourceType
) : GameResource, StdLibListener {

    var exited: Boolean = false
    var evaluated: Boolean = false

    lateinit var resourceAccess: GameResourceAccess

    override var reload: Boolean = false

    var content: ByteArray = ByteArray(0)

    private var initFunction: LuaValue? = null
    private var updateFunction: LuaValue? = null
    private var drawFunction: LuaValue? = null
    private var setStateFunction: LuaValue? = null
    private var getStateFunction: LuaValue? = null

    private var globals: Globals? = null

    class State(val args: LuaValue)

    private fun createLuaGlobals(): Globals = Globals().apply {
        val sprLib = SprLib(this@GameScript.gameOptions, this@GameScript.resourceAccess)

        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib())
        load(CoroutineLib())
        load(StdLib(gameOptions, resourceAccess, this@GameScript))
        load(MapLib(this@GameScript.resourceAccess))
        load(GfxLib(this@GameScript.resourceAccess))
        load(CtrlLib(inputHandler, sprLib))
        load(SfxLib(this@GameScript.resourceAccess))
        load(ShapeLib(this@GameScript.resourceAccess))
        load(MathLib())
        load(sprLib)
        load(JuiceLib())
        LoadState.install(this)
        LuaC.install(this)
    }

    override fun exit(nextScriptIndex: Int) {
        // FIXME: use the script index
        exited = true
    }

    fun isValid(): Boolean {
        return try {
            createLuaGlobals().load(content.decodeToString()).call()
            true
        } catch (exception: LuaError) {
            println("Can't parse '$name' as the file is not valid.")
            exception.errorLine()?.let { (lineNumber, line) ->
                println("Error on line $lineNumber:$line")
            }
            exception.printStackTrace()
            false
        }
    }

    fun evaluate() {
        globals = createLuaGlobals()

        evaluated = true
        exited = false
        reload = false

        globals?.load(content.decodeToString())?.call()

        initFunction = globals?.get("_init")?.nullIfNil()
        updateFunction = globals?.get("_update")?.nullIfNil()
        drawFunction = globals?.get("_draw")?.nullIfNil()
        getStateFunction = globals?.get("_getState")?.nullIfNil()
        setStateFunction = globals?.get("_setState")?.nullIfNil()

        initFunction?.call(valueOf(gameOptions.width), valueOf(gameOptions.height))
    }

    internal fun invoke(name: String, vararg args: LuaValue) {
        @Suppress("UNCHECKED_CAST")
        globals?.get(name)?.nullIfNil()?.invoke(args as Array<LuaValue>)
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

    fun resourcesLoaded() {
        globals?.get("_resources")?.nullIfNil()?.call()
    }

    private fun LuaValue.nullIfNil(): LuaValue? {
        return if (this.isnil()) {
            null
        } else {
            this
        }
    }

    override fun toString(): String {
        return """--- LUA SCRIPT ($index - $name) ---
existed: $exited | evaluated: $evaluated | reload: $reload |
init: ${initFunction != null} | update: ${updateFunction != null} | draw: ${drawFunction != null} |
            ----
            ${content.decodeToString()}
"""
    }
}
