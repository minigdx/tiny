package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.BreakpointHit
import com.github.minigdx.tiny.cli.debug.CurrentBreakpoints
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.Reload
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

class DebuggerApp(private val debugPort: Int) {
    private val scripts = mutableMapOf<String, String>()
    private val breakpoints = mutableMapOf<String, MutableSet<Int>>()
    private val conditions = mutableMapOf<String, MutableMap<Int, String>>()
    private var currentScript: String? = null

    private lateinit var scriptList: ScriptList
    private lateinit var codeEditor: CodeEditor
    private lateinit var variableInspector: VariableInspector
    private lateinit var toolbar: Toolbar
    private lateinit var conditionModal: ConditionModal
    private lateinit var engineSocket: EngineDebugSocket
    private lateinit var fileWatcherSocket: FileWatcherSocket

    fun init() {
        val scriptListContainer = document.getElementById("script-list") as HTMLDivElement
        val gutterContainer = document.getElementById("gutter") as HTMLDivElement
        val codeContainer = document.getElementById("code") as HTMLDivElement
        val variablesContainer = document.getElementById("variables") as HTMLDivElement
        val toolbarContainer = document.getElementById("toolbar") as HTMLDivElement
        val modalOverlay = document.getElementById("modal-overlay") as HTMLDivElement

        conditionModal = ConditionModal(modalOverlay)

        codeEditor = CodeEditor(
            gutterContainer,
            codeContainer,
            onToggleBreakpoint = { line -> handleToggleBreakpoint(line) },
            onConditionRequest = { line -> handleConditionRequest(line) },
        )

        variableInspector = VariableInspector(variablesContainer)
        variableInspector.clear()

        scriptList = ScriptList(scriptListContainer) { name ->
            selectScript(name)
        }

        toolbar = Toolbar(
            toolbarContainer,
            onResume = { engineSocket.resume() },
            onStep = { engineSocket.step() },
            onDisconnect = { engineSocket.disconnect() },
        )

        engineSocket = EngineDebugSocket(
            debugPort = debugPort,
            onBreakpointHit = { hit -> handleBreakpointHit(hit) },
            onCurrentBreakpoints = { bp -> handleCurrentBreakpoints(bp) },
            onReload = { reload -> handleReload(reload) },
            onConnected = { toolbar.setConnected(true) },
            onDisconnected = { toolbar.setConnected(false) },
        )

        fileWatcherSocket = FileWatcherSocket(
            onFiles = { files -> handleFiles(files) },
            onFileChanged = { file -> handleFileChanged(file) },
        )

        fileWatcherSocket.connect()
        engineSocket.connect()
    }

    private fun handleFiles(files: List<FileInfo>) {
        scripts.clear()
        files.forEach { file ->
            scripts[file.name] = file.content
        }
        scriptList.update(scripts.keys.toList())
        if (currentScript == null && scripts.isNotEmpty()) {
            selectScript(scripts.keys.first())
        }
    }

    private fun handleFileChanged(file: FileInfo) {
        scripts[file.name] = file.content
        scriptList.update(scripts.keys.toList())
        if (file.name == currentScript) {
            codeEditor.setContent(file.content)
            refreshEditorBreakpoints()
        }
    }

    private fun handleBreakpointHit(hit: BreakpointHit) {
        selectScript(hit.script)
        codeEditor.highlightLine(hit.line)
        variableInspector.update(hit.locals, hit.upValues)
    }

    private fun handleCurrentBreakpoints(bp: CurrentBreakpoints) {
        breakpoints.clear()
        conditions.clear()
        bp.breakpoints.forEach { info ->
            val scriptBps = breakpoints.getOrPut(info.script) { mutableSetOf() }
            if (info.enabled) {
                scriptBps.add(info.line)
            }
            if (info.condition != null) {
                val scriptConds = conditions.getOrPut(info.script) { mutableMapOf() }
                scriptConds[info.line] = info.condition
            }
        }
        refreshEditorBreakpoints()
    }

    private fun handleReload(reload: Reload) {
        codeEditor.highlightLine(null)
        variableInspector.clear()
    }

    private fun selectScript(name: String) {
        currentScript = name
        val content = scripts[name] ?: return
        codeEditor.setContent(content)
        scriptList.setActive(name, scripts.keys.toList())
        refreshEditorBreakpoints()
        codeEditor.highlightLine(null)
    }

    private fun refreshEditorBreakpoints() {
        val script = currentScript ?: return
        val bps = breakpoints[script] ?: emptySet()
        val conds = conditions[script] ?: emptyMap()
        codeEditor.setBreakpoints(bps, conds)
    }

    private fun handleToggleBreakpoint(line: Int) {
        val script = currentScript ?: return
        val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
        val enabled: Boolean
        if (scriptBps.contains(line)) {
            scriptBps.remove(line)
            conditions[script]?.remove(line)
            enabled = false
        } else {
            scriptBps.add(line)
            enabled = true
        }
        codeEditor.toggleBreakpoint(line)
        val condition = conditions[script]?.get(line)
        engineSocket.toggleBreakpoint(script, line, enabled, condition)
    }

    private fun handleConditionRequest(line: Int) {
        val script = currentScript ?: return
        val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
        val scriptConds = conditions.getOrPut(script) { mutableMapOf() }
        val currentCondition = scriptConds[line]

        conditionModal.show(currentCondition) { newCondition ->
            if (newCondition != null) {
                scriptConds[line] = newCondition
                if (!scriptBps.contains(line)) {
                    scriptBps.add(line)
                    codeEditor.toggleBreakpoint(line)
                }
            } else {
                scriptConds.remove(line)
            }
            codeEditor.setCondition(line, newCondition)
            val enabled = scriptBps.contains(line)
            engineSocket.toggleBreakpoint(script, line, enabled, newCondition)
        }
    }
}
