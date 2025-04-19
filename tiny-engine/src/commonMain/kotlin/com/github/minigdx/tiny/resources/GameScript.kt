package com.github.minigdx.tiny.resources

import com.github.minigdx.tiny.engine.Exit
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.log.Logger
import com.github.minigdx.tiny.lua.CtrlLib
import com.github.minigdx.tiny.lua.DebugLib
import com.github.minigdx.tiny.lua.GfxLib
import com.github.minigdx.tiny.lua.JuiceLib
import com.github.minigdx.tiny.lua.KeysLib
import com.github.minigdx.tiny.lua.MapLib
import com.github.minigdx.tiny.lua.MathLib
import com.github.minigdx.tiny.lua.NotesLib
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.lua.ShapeLib
import com.github.minigdx.tiny.lua.SprLib
import com.github.minigdx.tiny.lua.StdLib
import com.github.minigdx.tiny.lua.TestLib
import com.github.minigdx.tiny.lua.TestResult
import com.github.minigdx.tiny.lua.TinyBaseLib
import com.github.minigdx.tiny.lua.TinyLib
import com.github.minigdx.tiny.lua.Vec2Lib
import com.github.minigdx.tiny.lua.WorkspaceLib
import com.github.minigdx.tiny.lua.toTinyException
import com.github.minigdx.tiny.platform.Platform
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib

class GameScript(
    override val version: Int,
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
    val platform: Platform,
    val logger: Logger,
    override val type: ResourceType,
) : GameResource {
    var exited: Int = -1
    var evaluated: Boolean = false

    lateinit var resourceAccess: GameResourceAccess

    override var reload: Boolean = false

    var content: ByteArray = ByteArray(0)

    private var initFunction: LuaValue? = null
    private var updateFunction: LuaValue? = null
    private var drawFunction: LuaValue? = null
    private var setStateFunction: LuaValue? = null
    private var getStateFunction: LuaValue? = null

    var globals: Globals? = null

    private val tinyLib: TinyLib = TinyLib()

    internal val testResults = mutableListOf<TestResult>()

    class State(val args: LuaValue)

    private fun createLuaGlobals(
        customizeLuaGlobal: GameResourceAccess.(Globals) -> Unit,
        forValidation: Boolean = false,
    ): Globals =
        Globals().apply {
            val sprLib = SprLib(this@GameScript.gameOptions, this@GameScript.resourceAccess)

            load(TinyBaseLib(this@GameScript.resourceAccess))
            load(PackageLib())
            load(Bit32Lib())
            load(TableLib())
            load(StringLib())
            load(CoroutineLib())
            load(StdLib(gameOptions, resourceAccess))
            load(MapLib(this@GameScript.resourceAccess, gameOptions.spriteSize))
            load(GfxLib(this@GameScript.resourceAccess))
            load(CtrlLib(inputHandler, sprLib))
            load(SfxLib(this@GameScript.resourceAccess, playSound = !forValidation))
            load(ShapeLib(this@GameScript.resourceAccess))
            load(DebugLib(this@GameScript.resourceAccess, this@GameScript.logger))
            load(KeysLib())
            load(MathLib())
            load(Vec2Lib())
            load(tinyLib)
            load(sprLib)
            load(JuiceLib())
            load(NotesLib())
            load(WorkspaceLib(platform = platform))
            load(TestLib(this@GameScript))

            this@GameScript.resourceAccess.customizeLuaGlobal(this)

            LoadState.install(this)
            LuaC.install(this)
        }

    suspend fun isValid(customizeLuaGlobal: GameResourceAccess.(Globals) -> Unit): Boolean {
        with(createLuaGlobals(customizeLuaGlobal, forValidation = true)) {
            load(content.decodeToString()).call()
            get("_init").nullIfNil()?.callSuspend(valueOf(gameOptions.width), valueOf(gameOptions.height))
            if (gameOptions.runTests) {
                gameOptions.gameScripts.map { name ->
                    // use the new content for the game script evaluated
                    if (name == this@GameScript.name) {
                        content.decodeToString()
                    } else {
                        // use the cached content for the script not updated.
                        resourceAccess.script(name)?.content!!.decodeToString()
                    }
                }.forEach { scriptContent ->
                    val globalForTest = createLuaGlobals(customizeLuaGlobal, forValidation = true)
                    globalForTest.load(scriptContent).call()
                    globalForTest.get("_test").callSuspend()
                }
                logger.info("TEST") { "⚙\uFE0F === Ran ${testResults.size} tests ===" }
                testResults.forEach {
                    if (it.passed) {
                        logger.info("TEST") { "✅ ${it.script} - ${it.test}" }
                    } else {
                        logger.info("TEST") { "\uD83D\uDD34 ${it.script} - ${it.test}: ${it.reason}" }
                    }
                }
            }
        }
        return true
    }

    suspend fun evaluate(customizeLuaGlobal: GameResourceAccess.(Globals) -> Unit) {
        globals = createLuaGlobals(customizeLuaGlobal)

        evaluated = true
        exited = -1
        reload = false
        try {
            globals?.load(content.decodeToString(), name)?.callSuspend()

            initFunction = globals?.get("_init")?.nullIfNil()
            updateFunction = globals?.get("_update")?.nullIfNil()
            drawFunction = globals?.get("_draw")?.nullIfNil()
            getStateFunction = globals?.get("_getState")?.nullIfNil()
            setStateFunction = globals?.get("_setState")?.nullIfNil()

            initFunction?.callSuspend(valueOf(gameOptions.width), valueOf(gameOptions.height))
        } catch (ex: LuaError) {
            val luaCause = ex.luaCause
            // The user want to load another script.
            if (luaCause is Exit) {
                exited = luaCause.script
            } else {
                throw ex.toTinyException(content.decodeToString())
            }
        }
    }

    internal suspend fun invoke(
        name: String,
        vararg args: LuaValue,
    ) {
        val path = name.split(".")
        val head = path.first()
        val tail = path.drop(1)

        var function = globals?.get(head)
        tail.forEach {
            function = function?.get(it)
        }

        @Suppress("UNCHECKED_CAST")
        function?.nullIfNil()?.invokeSuspend(args as Array<LuaValue>)
    }

    suspend fun getState(): State? {
        val data = getStateFunction?.callSuspend()
        return if (data != null && !data.isnil()) {
            return State(data)
        } else {
            null
        }
    }

    suspend fun setState(state: State? = null) {
        if (state != null) {
            setStateFunction?.callSuspend(state.args)
        }
    }

    suspend fun advance() {
        tinyLib.advance()
        try {
            updateFunction?.callSuspend()
            drawFunction?.callSuspend()
        } catch (ex: LuaError) {
            val luaCause = ex.luaCause
            // The user want to load another script.
            if (luaCause is Exit) {
                exited = luaCause.script
            } else {
                throw ex.toTinyException(content.decodeToString())
            }
        }
    }

    suspend fun resourcesLoaded() {
        globals?.get("_resources")?.nullIfNil()?.callSuspend()
    }

    private fun LuaValue.nullIfNil(): LuaValue? {
        return if (this.isnil()) {
            null
        } else {
            this
        }
    }

    override fun toString(): String {
        return """--- Lua SCRIPT ($index - $name) ---
existed: $exited | evaluated: $evaluated | reload: $reload |
init: ${initFunction != null} | update: ${updateFunction != null} | draw: ${drawFunction != null} |
            ----
            ${content.decodeToString()}
"""
    }
}
