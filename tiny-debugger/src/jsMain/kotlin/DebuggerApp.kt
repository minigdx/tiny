package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.BreakpointHit
import com.github.minigdx.tiny.cli.debug.CurrentBreakpoints
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.GameMetadata
import com.github.minigdx.tiny.cli.debug.Reload
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement

class DebuggerApp {
    private val scripts = mutableMapOf<String, String>()
    private val breakpoints = mutableMapOf<String, MutableSet<Int>>()
    private val conditions = mutableMapOf<String, MutableMap<Int, String>>()
    private var currentScript: String? = null
    private var gameId: String? = null

    private lateinit var scriptList: ScriptList
    private lateinit var codeEditor: CodeEditor
    private lateinit var variableInspector: VariableInspector
    private lateinit var toolbar: Toolbar
    private lateinit var conditionModal: ConditionModal
    private lateinit var engineSocket: EngineDebugSocket
    private lateinit var breakpointPanel: BreakpointPanel

    fun init() {
        val scriptListContainer = document.getElementById("script-list") as HTMLDivElement
        val gutterContainer = document.getElementById("gutter") as HTMLDivElement
        val codeContainer = document.getElementById("code") as HTMLDivElement
        val variablesContainer = document.getElementById("variables") as HTMLDivElement
        val toolbarContainer = document.getElementById("toolbar") as HTMLDivElement
        val modalOverlay = document.getElementById("modal-overlay") as HTMLDivElement
        val breakpointPanelContainer = document.getElementById("breakpoint-panel") as HTMLDivElement

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

        breakpointPanel = BreakpointPanel(
            breakpointPanelContainer,
            onToggleBreakpoint = { script, line, enabled ->
                val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
                if (enabled) {
                    scriptBps.add(line)
                } else {
                    scriptBps.remove(line)
                }
                val condition = conditions[script]?.get(line)
                engineSocket.toggleBreakpoint(script, line, enabled, condition)
                refreshEditorBreakpoints()
                saveBreakpointsToStorage()
            },
            onRemoveBreakpoint = { script, line ->
                breakpoints[script]?.remove(line)
                conditions[script]?.remove(line)
                engineSocket.toggleBreakpoint(script, line, false, null)
                refreshEditorBreakpoints()
                refreshBreakpointPanel()
                saveBreakpointsToStorage()
            },
            onNavigateBreakpoint = { script, line ->
                selectScript(script)
                codeEditor.highlightLine(line)
            },
            onRemoveAll = {
                breakpoints.forEach { (script, lines) ->
                    lines.forEach { line ->
                        engineSocket.toggleBreakpoint(script, line, false, null)
                    }
                }
                breakpoints.clear()
                conditions.clear()
                refreshEditorBreakpoints()
                refreshBreakpointPanel()
                saveBreakpointsToStorage()
            },
        )

        toolbar = Toolbar(
            toolbarContainer,
            onResume = { engineSocket.resume() },
            onStep = { engineSocket.step() },
            onDisconnect = { engineSocket.disconnect() },
        )

        engineSocket = EngineDebugSocket(
            onBreakpointHit = { hit -> handleBreakpointHit(hit) },
            onCurrentBreakpoints = { bp -> handleCurrentBreakpoints(bp) },
            onReload = { reload -> handleReload(reload) },
            onAllFiles = { msg -> handleFiles(msg.files) },
            onFileChanged = { msg -> handleFileChanged(msg.file) },
            onGameMetadata = { meta -> handleGameMetadata(meta) },
            onConnected = { toolbar.setConnected(true) },
            onDisconnected = { toolbar.setConnected(false) },
        )

        initDragHandle()
        engineSocket.connect()
    }

    private fun handleGameMetadata(meta: GameMetadata) {
        gameId = meta.gameId
        restoreBreakpointsFromStorage()
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
        refreshBreakpointPanel()
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

    private fun refreshBreakpointPanel() {
        breakpointPanel.update(breakpoints, conditions)
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
        refreshBreakpointPanel()
        saveBreakpointsToStorage()
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
            refreshBreakpointPanel()
            saveBreakpointsToStorage()
        }
    }

    private fun restoreBreakpointsFromStorage() {
        val id = gameId ?: return
        val stored = BreakpointStorage.load(id)
        stored.forEach { sb ->
            val scriptBps = breakpoints.getOrPut(sb.script) { mutableSetOf() }
            if (sb.enabled) {
                scriptBps.add(sb.line)
            }
            if (sb.condition != null) {
                val scriptConds = conditions.getOrPut(sb.script) { mutableMapOf() }
                scriptConds[sb.line] = sb.condition
            }
            engineSocket.toggleBreakpoint(sb.script, sb.line, sb.enabled, sb.condition)
        }
        refreshEditorBreakpoints()
        refreshBreakpointPanel()
    }

    private fun saveBreakpointsToStorage() {
        val id = gameId ?: return
        BreakpointStorage.save(id, breakpoints, conditions)
    }

    private fun initDragHandle() {
        val dragHandle = document.getElementById("drag-handle") ?: return
        val variablesPanel = document.getElementById("variables-panel") ?: return
        var dragging = false

        dragHandle.addEventListener("mousedown", { e: org.w3c.dom.events.Event ->
            dragging = true
            e.preventDefault()
        })

        document.addEventListener("mousemove", { e: org.w3c.dom.events.Event ->
            if (dragging) {
                val mouseEvent = e.asDynamic()
                val parentRect = variablesPanel.parentElement?.getBoundingClientRect()
                if (parentRect != null) {
                    val newHeight = parentRect.bottom - (mouseEvent.clientY as Double)
                    val clamped = newHeight.coerceIn(60.0, 500.0)
                    variablesPanel.asDynamic().style.height = "${clamped}px"
                }
            }
        })

        document.addEventListener("mouseup", { _: org.w3c.dom.events.Event ->
            dragging = false
        })
    }
}
