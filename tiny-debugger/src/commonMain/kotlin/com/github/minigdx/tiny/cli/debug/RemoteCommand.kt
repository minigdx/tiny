package com.github.minigdx.tiny.cli.debug

import kotlinx.serialization.Serializable

/**
 * Command to be executed remotely.
 */
@Serializable
sealed interface RemoteCommand

/**
 * Command send by the debugger, to the game engine.
 */
@Serializable
sealed interface DebugRemoteCommand : RemoteCommand

/**
 * Command send by the game engine, to the debugger.
 */
@Serializable
sealed interface EngineRemoteCommand : RemoteCommand

@Serializable
data class Reload(val script: String) : EngineRemoteCommand

/**
 * Toggle a breakpoint in the game engine.
 *
 * @param script the name of the script where the breakpoint is.
 * @param line the line number of the breakpoint.
 * @param enabled true if the breakpoint is enabled, false otherwise.
 * @param condition optional Lua condition that must evaluate to true for the breakpoint to trigger.
 */
@Serializable
data class ToggleBreakpoint(val script: String, val line: Int, val enabled: Boolean, val condition: String? = null) : DebugRemoteCommand

/**
 * Resume game execution.
 */
@Serializable
data class ResumeExecution(val advanceByStep: Boolean = false) : DebugRemoteCommand

/**
 * Resume game execution.
 */
@Serializable
object Disconnect : DebugRemoteCommand

/**
 * Request current breakpoints from the game engine.
 */
@Serializable
object RequestBreakpoints : DebugRemoteCommand

/**
 * A breakpoint has been hit in the game engine.
 *
 * @param script the name of the script where the breakpoint is.
 * @param line the line number of the breakpoint.
 * @param locals the local variables at the breakpoint.
 * @param upValues the upvalues at the breakpoint.
 */
@Serializable
data class BreakpointHit(
    val script: String,
    val line: Int,
    val locals: Map<String, LuaValue>,
    val upValues: Map<String, LuaValue>,
    val conditionError: String? = null,
) : EngineRemoteCommand

/**
 * Current breakpoints in the game engine.
 *
 * @param breakpoints the list of current breakpoints.
 */
@Serializable
data class CurrentBreakpoints(
    val breakpoints: List<BreakpointInfo>,
) : EngineRemoteCommand

/**
 * Information about a breakpoint.
 *
 * @param script the name of the script where the breakpoint is.
 * @param line the line number of the breakpoint.
 * @param enabled true if the breakpoint is enabled, false otherwise.
 * @param condition optional Lua condition that must evaluate to true for the breakpoint to trigger.
 */
@Serializable
data class BreakpointInfo(
    val script: String,
    val line: Int,
    val enabled: Boolean,
    val condition: String? = null,
)
