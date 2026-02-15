package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.BreakpointHit
import com.github.minigdx.tiny.cli.debug.CurrentBreakpoints
import com.github.minigdx.tiny.cli.debug.FileInfo
import com.github.minigdx.tiny.cli.debug.GameMetadata
import com.github.minigdx.tiny.cli.debug.Reload
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement

class DebuggerApp {
    private val scripts = mutableMapOf<String, String>()
    private val breakpoints = mutableMapOf<String, MutableSet<Int>>()
    private val disabledBreakpoints = mutableMapOf<String, MutableSet<Int>>()
    private val conditions = mutableMapOf<String, MutableMap<Int, String>>()
    private var currentScript: String? = null
    private var gameId: String? = null
    private var isPaused = false

    private lateinit var scriptList: ScriptList
    private lateinit var codeEditor: CodeEditor
    private lateinit var variableInspector: VariableInspector
    private lateinit var toolbar: Toolbar
    private lateinit var conditionModal: ConditionModal
    private lateinit var engineSocket: EngineDebugSocket
    private lateinit var breakpointPanel: BreakpointPanel
    private lateinit var toggleAllCheckbox: HTMLInputElement

    fun init() {
        val scriptListContainer = document.getElementById("script-list") as HTMLDivElement
        val gutterContainer = document.getElementById("gutter") as HTMLDivElement
        val codeContainer = document.getElementById("code") as HTMLDivElement
        val variablesContainer = document.getElementById("variables") as HTMLDivElement
        val toolbarContainer = document.getElementById("toolbar") as HTMLDivElement
        val modalOverlay = document.getElementById("modal-overlay") as HTMLDivElement
        val breakpointPanelContainer = document.getElementById("breakpoint-panel") as HTMLDivElement
        toggleAllCheckbox = document.getElementById("toggle-all-breakpoints") as HTMLInputElement

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
                if (enabled) {
                    disabledBreakpoints[script]?.remove(line)
                    val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
                    scriptBps.add(line)
                } else {
                    breakpoints[script]?.remove(line)
                    val scriptDisabled = disabledBreakpoints.getOrPut(script) { mutableSetOf() }
                    scriptDisabled.add(line)
                }
                val condition = conditions[script]?.get(line)
                engineSocket.toggleBreakpoint(script, line, enabled, condition)
                refreshEditorBreakpoints()
                updateToggleAllCheckbox()
                saveBreakpointsToStorage()
            },
            onRemoveBreakpoint = { script, line ->
                breakpoints[script]?.remove(line)
                disabledBreakpoints[script]?.remove(line)
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
                disabledBreakpoints.forEach { (script, lines) ->
                    lines.forEach { line ->
                        engineSocket.toggleBreakpoint(script, line, false, null)
                    }
                }
                breakpoints.clear()
                disabledBreakpoints.clear()
                conditions.clear()
                refreshEditorBreakpoints()
                refreshBreakpointPanel()
                saveBreakpointsToStorage()
            },
        )

        toolbar = Toolbar(
            toolbarContainer,
            onResume = {
                isPaused = false
                toolbar.setPaused(false)
                engineSocket.resume()
            },
            onStep = { engineSocket.step() },
            onDisconnect = { engineSocket.disconnect() },
            onConnect = { engineSocket.connect() },
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

        toggleAllCheckbox.onchange = {
            handleToggleAllBreakpoints(toggleAllCheckbox.checked)
        }

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
        isPaused = true
        toolbar.setPaused(true)
        selectScript(hit.script)
        codeEditor.highlightLine(hit.line)
        variableInspector.update(hit.locals, hit.upValues)
    }

    private fun handleCurrentBreakpoints(bp: CurrentBreakpoints) {
        breakpoints.clear()
        disabledBreakpoints.clear()
        conditions.clear()
        bp.breakpoints.forEach { info ->
            if (info.enabled) {
                val scriptBps = breakpoints.getOrPut(info.script) { mutableSetOf() }
                scriptBps.add(info.line)
            } else {
                val scriptDisabled = disabledBreakpoints.getOrPut(info.script) { mutableSetOf() }
                scriptDisabled.add(info.line)
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
        isPaused = false
        toolbar.setPaused(false)
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
        val disabled = disabledBreakpoints[script] ?: emptySet()
        codeEditor.setBreakpoints(bps, conds, disabled)
    }

    private fun refreshBreakpointPanel() {
        breakpointPanel.update(breakpoints, conditions, disabledBreakpoints)
        updateToggleAllCheckbox()
    }

    private fun handleToggleBreakpoint(line: Int) {
        val script = currentScript ?: return
        val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
        val scriptDisabled = disabledBreakpoints.getOrPut(script) { mutableSetOf() }
        val enabled: Boolean
        if (scriptBps.contains(line)) {
            scriptBps.remove(line)
            conditions[script]?.remove(line)
            enabled = false
        } else if (scriptDisabled.contains(line)) {
            scriptDisabled.remove(line)
            scriptBps.add(line)
            enabled = true
        } else {
            scriptBps.add(line)
            enabled = true
        }
        refreshEditorBreakpoints()
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
                    disabledBreakpoints[script]?.remove(line)
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

    private fun handleToggleAllBreakpoints(enabled: Boolean) {
        if (enabled) {
            disabledBreakpoints.forEach { (script, lines) ->
                val scriptBps = breakpoints.getOrPut(script) { mutableSetOf() }
                lines.forEach { line ->
                    scriptBps.add(line)
                    val condition = conditions[script]?.get(line)
                    engineSocket.toggleBreakpoint(script, line, true, condition)
                }
            }
            disabledBreakpoints.clear()
        } else {
            breakpoints.forEach { (script, lines) ->
                val scriptDisabled = disabledBreakpoints.getOrPut(script) { mutableSetOf() }
                lines.forEach { line ->
                    scriptDisabled.add(line)
                    val condition = conditions[script]?.get(line)
                    engineSocket.toggleBreakpoint(script, line, false, condition)
                }
            }
            breakpoints.clear()
        }
        refreshEditorBreakpoints()
        refreshBreakpointPanel()
        saveBreakpointsToStorage()
    }

    private fun updateToggleAllCheckbox() {
        val totalEnabled = breakpoints.values.sumOf { it.size }
        val totalDisabled = disabledBreakpoints.values.sumOf { it.size }
        val total = totalEnabled + totalDisabled
        if (total == 0) {
            toggleAllCheckbox.checked = true
            toggleAllCheckbox.indeterminate = false
        } else if (totalDisabled == 0) {
            toggleAllCheckbox.checked = true
            toggleAllCheckbox.indeterminate = false
        } else if (totalEnabled == 0) {
            toggleAllCheckbox.checked = false
            toggleAllCheckbox.indeterminate = false
        } else {
            toggleAllCheckbox.checked = false
            toggleAllCheckbox.indeterminate = true
        }
    }

    private fun restoreBreakpointsFromStorage() {
        val id = gameId ?: return
        val stored = BreakpointStorage.load(id)
        stored.forEach { sb ->
            if (sb.enabled) {
                val scriptBps = breakpoints.getOrPut(sb.script) { mutableSetOf() }
                scriptBps.add(sb.line)
            } else {
                val scriptDisabled = disabledBreakpoints.getOrPut(sb.script) { mutableSetOf() }
                scriptDisabled.add(sb.line)
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
        BreakpointStorage.save(id, breakpoints, conditions, disabledBreakpoints)
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
