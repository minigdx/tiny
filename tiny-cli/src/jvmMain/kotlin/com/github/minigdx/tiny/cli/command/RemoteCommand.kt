package com.github.minigdx.tiny.cli.command

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
data class HandshakeRequest(val name: String, val gamescripts: List<String>) : RemoteCommand

/**
 * Toggle a breakpoint in the game engine.
 *
 * @param script the name of the script where the breakpoint is.
 * @param line the line number of the breakpoint.
 * @param enabled true if the breakpoint is enabled, false otherwise.
 */
@Serializable
data class ToggleBreakpoint(val script: String, val line: Int, val enabled: Boolean) : DebugRemoteCommand

/**
 * Resume game execution.
 *
 * @param script the name of the script where the execution should resume.
 * @param line the line number of the execution point. If null, the program should resume normally.
 */
@Serializable
data class ResumeExecution(val script: String?, val line: Int?) : DebugRemoteCommand

/**
 * A breakpoint has been hit in the game engine.
 *
 * @param script the name of the script where the breakpoint is.
 * @param line the line number of the breakpoint.
 * @param locals the local variables at the breakpoint.
 */
@Serializable
data class BreakpointHit(
    val script: String,
    val line: Int,
    val locals: Map<String, String>,
    val upValues: Map<String, String>,
) : EngineRemoteCommand
