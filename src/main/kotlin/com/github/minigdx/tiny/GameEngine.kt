package com.github.minigdx.tiny

import com.github.minigdx.tiny.engine.GameLoop
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.platform.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.nio.file.SecureDirectoryStream

class ScriptsCollector(private val events: MutableList<GameScript>) : FlowCollector<GameScript> {

    private val scriptsByName = mutableMapOf<String, Boolean>()

    override suspend fun emit(value: GameScript) {
        val script = scriptsByName[value.name]
        // New script. The content will have to be loaded by the GameEngine.
        // It's added in the script stack
        if (script == null) {
            scriptsByName.put(value.name, true)
            events.add(value)
        } else {
            events.add(value.apply { reloaded = true })
        }
    }

}

class GameOption(
    val width: Pixel = 256,
    val height: Pixel = 256
)
class GameEngine(
    val gameOption: GameOption,
    val platform: Platform,
    val vfs: VirtualFileSystem
) : GameLoop {

    private val scripts: MutableList<GameScript> = mutableListOf()
    private val scriptsByName: MutableMap<String, GameScript> = mutableMapOf()

    private val events: MutableList<GameScript> = mutableListOf()
    private val workEvents: MutableList<GameScript> = mutableListOf()

    private var current: GameScript? = null

    private var accumulator: Seconds = 0f

    fun main() {
        platform.initWindow(gameOption)

        val scope = CoroutineScope(Dispatchers.Default)

        // plutot essayer de faire la partie drawing avant d'attaquer la partie texture ?
        // faire des primitives pour lignes / cercles / rectangles / pixel
        // 3 scripts ??
        // 1er script : boucle uniquement de code ? -> avec resourceLoaded, uniquement 1 script est nécessaire.
        // 2nd script : boucle avec texture bundle
        // 3nd script : texture du jeu
        // loading texture async. tant qu'il n'y a pas la texture, utiliser placeholder : uniquement red ??
        // créer methode function resourceLoaded end
        val scriptsName = listOf("src/main/resources/boot.lua", "src/main/resources/test.lua")

        scope.launch {
            scriptsName.asFlow()
                .map { name -> GameScript(name).apply { loading = true } }
                .onCompletion {
                    val scriptsLoading = scriptsName.map { file ->
                        vfs.watch(FileStream(File(file))).map { content ->
                            GameScript(file).apply {
                                this.content = content
                            }
                        }
                    }.merge()

                    emitAll(scriptsLoading)
                }.collect(ScriptsCollector(events))
        }

        platform.initGameLoop()

        platform.gameLoop(this)
    }


    override fun advance(delta: Seconds) {
        workEvents.addAll(events)

        workEvents.forEach { gameScript ->
            if (!gameScript.reloaded) {
                // First time script loading. Adding it to the stack of script
                scripts.add(gameScript)
                scriptsByName[gameScript.name] = gameScript
                if (current == null) {
                    current = scripts.firstOrNull()
                }
            } else {
                // The script is already in the stack. Time to update it.
                scriptsByName[gameScript.name]?.run {
                    if (isValid()) {
                        reloaded = true
                        content = gameScript.content
                    }
                }
            }
        }
        events.removeAll(workEvents)
        workEvents.clear()

        with(current) {
            if (this == null) return

            if (exited) {
                scripts.removeFirst()
                current = scripts.firstOrNull()
                val state = getState()
                current?.setState(state)
            } else if (loading) {
                evaluate()
            } else if (reloaded) {
                val state = getState()
                evaluate()
                setState(state)
            }

            // Fixed step simulation
            accumulator += delta
            if(accumulator >= REFRESH_LIMIT) {
                current?.advance()
                accumulator -= REFRESH_LIMIT
            }
        }

    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1/60f
    }

}

class TinyLib(val parent: GameScript) : TwoArgFunction() {
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val tiny = LuaTable()
        env["exit"] = exit()
        return tiny
    }

    internal inner class exit : ZeroArgFunction() {
        override fun call(): LuaValue {
            parent.exited = true
            return NONE
        }
    }

}

class State(val args: LuaValue)

class GameScript(val name: String) {

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
