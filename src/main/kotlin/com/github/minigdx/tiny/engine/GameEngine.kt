package com.github.minigdx.tiny.engine

import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.file.FileStream
import com.github.minigdx.tiny.file.VirtualFileSystem
import com.github.minigdx.tiny.platform.RenderContext
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
import java.io.File

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

    private lateinit var renderContext: RenderContext

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
                .map { name -> GameScript(name, gameOption).apply { loading = true } }
                .onCompletion {
                    val scriptsLoading = scriptsName.map { file ->
                        vfs.watch(FileStream(File(file))).map { content ->
                            GameScript(file, gameOption).apply {
                                this.content = content
                            }
                        }
                    }.merge()

                    emitAll(scriptsLoading)
                }.collect(ScriptsCollector(events))
        }

        renderContext = platform.createDrawContext()

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
            if (accumulator >= REFRESH_LIMIT) {
                current?.advance()
                accumulator -= REFRESH_LIMIT
            }
        }

    }

    override fun draw() {
        with(current) {
            if (this == null) return
            platform.draw(renderContext, frameBuffer.generateBuffer(), frameBuffer.width, frameBuffer.height)
        }
    }

    companion object {
        private const val REFRESH_LIMIT: Seconds = 1 / 60f
    }

}
