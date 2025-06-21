package com.github.minigdx.tiny.cli.debug

import com.github.minigdx.tiny.cli.debug.LuaValue.Dictionary
import com.github.minigdx.tiny.cli.debug.LuaValue.Primitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaClosure
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.ExecutionListener
import kotlin.math.max

/**
 * [DebuggerExecutionListener] is the class that will listen to the Lua execution.
 *
 * It will be notified when:
 * - a function is called
 * - an instruction is executed
 * - a function returns
 *
 * It will be used to:
 * - manage the breakpoints
 * - send a notification when a breakpoint is hit
 * - collect the local variables when a breakpoint is hit
 * - collect the call stack when a breakpoint is hit
 * - collect the global variables when a breakpoint is hit
 */

class DebuggerExecutionListener(
    private val debugCommandReceiver: ReceiveChannel<DebugRemoteCommand>,
    private val engineCommandSender: SendChannel<EngineRemoteCommand>,
) : ExecutionListener {
    lateinit var globals: Globals

    private var breakpoints: Map<ExecutionPoint, Breakpoint> = emptyMap()

    private val currentExecutionPoint = Breakpoint()

    private var lineinfo: IntArray? = null

    private val blocker = CoroutineBlocker()

    private var advanceByStep: Boolean = false

    // Current line when the execution resume.
    // So when the step advance of one step, it's to another line.
    private var advanceByStepCurrentLine: Int = -1

    init {
        CoroutineScope(Dispatchers.IO).launch {
            for (debugRemoteCommand in debugCommandReceiver) {
                when (debugRemoteCommand) {
                    is ToggleBreakpoint -> toggleBreakpoint(debugRemoteCommand)
                    is ResumeExecution -> resumeExecution(debugRemoteCommand)
                    Disconnect -> disconnect()
                    RequestBreakpoints -> sendCurrentBreakpoints()
                }
            }
        }
    }

    private fun resumeExecution(debugRemoteCommand: ResumeExecution) {
        advanceByStep = debugRemoteCommand.advanceByStep
        advanceByStepCurrentLine = currentExecutionPoint.line
        blocker.unblock()
    }

    private fun disconnect() {
        breakpoints = emptyMap()
        blocker.unblock()
    }

    private suspend fun sendCurrentBreakpoints() {
        val breakpointInfoList = breakpoints.map { (executionPoint, breakpoint) ->
            BreakpointInfo(
                script = executionPoint.scriptName,
                line = executionPoint.line,
                enabled = breakpoint.enabled
            )
        }

        engineCommandSender.send(CurrentBreakpoints(breakpointInfoList))

        // If a breakpoint is currently hit, send a BreakpointHit to restore the state
        // Check if we're at a valid line in a script (indicating a breakpoint hit)
        val scriptName = currentExecutionPoint.script
        val line = currentExecutionPoint.line
        if (line > 0 && scriptName.isNotEmpty()) {
            sendBreakpointHit(scriptName, line)
        }
    }

    private suspend fun sendBreakpointHit(scriptName: String, line: Int) {
        val frames = callstack(globals.running).getCallFrames()

        val upValues =
            frames.flatMap { frame ->
                val upValues = (frame.f as? LuaClosure)?.upValues ?: emptyArray()
                val upValuesDesc = (frame.f as? LuaClosure)?.p?.upvalues ?: emptyArray()

                upValues.zip(upValuesDesc) { value, name ->
                    val upValueName = name.name?.tojstring() ?: ""
                    val upValueValue = value?.value ?: LuaValue.NIL
                    upValueName to upValueValue
                }
                    // Skip the _ENV upvalue
                    .filterNot { (name, _) -> name == "_ENV" }
            }.toMap()
                .mapValues { (_, value) -> formatValue(value) }

        val locals =
            frames.flatMap {
                it.getLocals()
            }.associate {
                // name to value
                it.arg(1).tojstring() to formatValue(it.arg(2))
            }

        engineCommandSender.send(
            BreakpointHit(
                script = scriptName,
                line = line,
                locals = locals,
                upValues = upValues,
            ),
        )
    }

    private fun toggleBreakpoint(debugRemoteCommand: ToggleBreakpoint) {
        val executionPoint = ExecutionPoint(debugRemoteCommand.script, debugRemoteCommand.line)
        val storedBreakpoint = breakpoints[executionPoint]

        if (storedBreakpoint == null) {
            val entry =
                executionPoint to
                    Breakpoint(
                        script = debugRemoteCommand.script,
                        line = debugRemoteCommand.line,
                        enabled = debugRemoteCommand.enabled,
                    )
            breakpoints = breakpoints + entry
        } else {
            storedBreakpoint.enabled = debugRemoteCommand.enabled
        }
    }

    /**
     * Format a LuaValue to a structured representation.
     */
    private fun formatValue(
        arg: LuaValue,
        recursiveSecurity: MutableSet<Int> = mutableSetOf(),
    ): com.github.minigdx.tiny.cli.debug.LuaValue =
        if (arg.istable()) {
            val table = arg as LuaTable
            if (recursiveSecurity.contains(table.hashCode())) {
                Primitive("table[<${table.hashCode()}>]")
            } else {
                recursiveSecurity.add(table.hashCode())
                val keys = table.keys()
                val entries = mutableMapOf<String, com.github.minigdx.tiny.cli.debug.LuaValue>()

                keys.forEach { key ->
                    val keyStr = key.optjstring("nil") ?: "nil"
                    entries[keyStr] = formatValue(table[key], recursiveSecurity)
                }

                Dictionary(entries)
            }
        } else if (arg.isfunction()) {
            val funcStr = "function(" + (0 until arg.narg()).joinToString(", ") { "arg" } + ")"
            Primitive(funcStr)
        } else {
            Primitive(arg.toString())
        }

    override suspend fun onCall(
        c: LuaClosure,
        varargs: Varargs,
        stack: Array<LuaValue>,
    ) {
        callstack(globals.running).onCall(c, varargs, stack)

        onCall(c)
    }

    override fun onCall(f: LuaFunction) {
        callstack(globals.running).onCall(f)
        (f as? LuaClosure)?.run { onCall(this) }
    }

    private fun onCall(c: LuaClosure) {
        val name = c.p.source.tojstring()
        currentExecutionPoint.script =
            if (name.startsWith("@")) {
                name.drop(1)
            } else {
                name
            }
        currentExecutionPoint.enabled = true
        currentExecutionPoint.function = c

        lineinfo = c.p.lineinfo
        // Computes the line of the pc.
        breakpoints.filter { (executionPoint, breakpoint) -> !breakpoint.init && executionPoint.scriptName == currentExecutionPoint.script }
            .values
            .filter { breakpoint -> (lineinfo ?: intArrayOf()).contains(breakpoint.line) }
            .forEach { breakpoint ->
                breakpoint.pc = lineinfo?.indexOfFirst { breakpoint.line == it } ?: -1
                breakpoint.init = true
                breakpoint.function = c
            }
    }

    override suspend fun onInstruction(
        pc: Int,
        v: Varargs,
        top: Int,
    ) {
        callstack(globals.running).onInstruction(pc, v, top)

        val line = lineinfo?.getOrNull(pc) ?: -1

        currentExecutionPoint.pc = pc
        currentExecutionPoint.line = line

        if (advanceByStep && line != advanceByStepCurrentLine) {
            pauseExecution(currentExecutionPoint.script, line)
        }

        breakpoints.values.forEach { breakpoint ->
            if (currentExecutionPoint.hit(breakpoint)) {
                pauseExecution(breakpoint.script, breakpoint.line)
            }
        }
    }

    private suspend fun pauseExecution(
        scriptName: String,
        line: Int,
    ) {
        sendBreakpointHit(scriptName, line)
        blocker.block()
    }

    override fun onReturn() {
        callstack(globals.running).onReturn()

        val frame = callstack(globals.running).getCurrentFrame()

        val p = (frame?.f as? LuaClosure)?.p
        val name = p?.source?.tojstring()
        currentExecutionPoint.script =
            if (name?.startsWith("@") == true) {
                name.drop(1)
            } else {
                name ?: "anonymous"
            }
        currentExecutionPoint.function = frame?.f as LuaClosure?
        lineinfo = p?.lineinfo
    }

    override fun traceback(level: Int): String = ""

    private fun callstack(t: LuaThread): CallStack {
        val callstack = t.callstack as? CallStack ?: CallStack()
        t.callstack = callstack
        return callstack
    }

    private class CallStack {
        var calls = 0
        var frame = emptyArray<CallFrame>()

        fun onCall(luaFunction: LuaFunction) {
            pushCall().set(luaFunction)
        }

        fun pushCall(): CallFrame {
            if (calls >= frame.size) {
                frame =
                    Array(max(4, frame.size * 3 / 2)) { i ->
                        frame.getOrNull(i) ?: CallFrame().apply {
                            previous = frame.getOrNull(i - 1)
                        }
                    }
            }
            return frame[calls++]
        }

        fun onCall(
            luaFunction: LuaClosure,
            varargs: Varargs,
            stack: Array<LuaValue>,
        ) {
            pushCall().set(luaFunction, varargs, stack)
        }

        fun onReturn() {
            if (calls > 0) {
                frame[--calls].reset()
            }
        }

        fun onInstruction(
            pc: Int,
            v: Varargs,
            top: Int,
        ) {
            if (calls > 0) {
                frame[calls - 1].instr(pc, v, top)
            }
        }

        fun getCallFrames(): List<CallFrame> {
            return frame.asList().take(calls + 1)
        }

        fun getCurrentFrame(): CallFrame? {
            return frame.getOrNull(calls - 1)
        }
    }

    private class CallFrame {
        var f: LuaFunction? = null
        var pc: Int = 0
        var top: Int = 0
        var v: Varargs? = null
        var stack: Array<LuaValue>? = null
        var previous: CallFrame? = null

        fun set(luaFunction: LuaFunction) {
            this.f = luaFunction
        }

        fun set(
            function: LuaClosure,
            varargs: Varargs,
            stack: Array<LuaValue>,
        ) {
            this.f = function
            this.v = varargs
            this.stack = stack
        }

        fun instr(
            pc: Int,
            v: Varargs,
            top: Int,
        ) {
            this.pc = pc
            this.v = v
            this.top = top
        }

        fun reset() {
            this.f = null
            this.v = null
            this.stack = null
        }

        fun getLocals(): List<Varargs> {
            with(f) {
                if (this == null || !this.isclosure()) return emptyList()

                val locvars =
                    this.checkclosure()!!.p.locvars
                        .map { it.varname }

                return locvars.zip(stack!!) { name, value -> LuaValue.varargsOf(name, value) }
            }
        }
    }
}
