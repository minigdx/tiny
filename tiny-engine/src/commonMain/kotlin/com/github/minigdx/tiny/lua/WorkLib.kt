package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.LuaType
import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

@TinyLib(
    "work",
    "Coroutine manager integrated with the game loop. " +
        "Use `work.add` to create background tasks that run across multiple frames, " +
        "`work.wait` to pause for a duration, " +
        "`work.wait_until` to pause until a condition is met, and " +
        "`work.wait_frame` to pause for exactly one frame.",
)
class WorkLib : TwoArgFunction() {

    private lateinit var globals: Globals

    private val coroutines = mutableListOf<TrackedCoroutine>()

    private lateinit var coroutineCreate: LuaValue
    private lateinit var coroutineResume: LuaValue
    private lateinit var coroutineStatus: LuaValue

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        globals = arg2 as Globals

        val coroutineTable = globals["coroutine"]
        coroutineCreate = coroutineTable["create"]
        coroutineResume = coroutineTable["resume"]
        coroutineStatus = coroutineTable["status"]

        val work = LuaTable()
        work["add"] = add()
        work["count"] = count()

        // Install Lua-level yield wrappers for wait/wait_until/wait_frame.
        // These are Lua functions that call coroutine.yield with typed markers
        // so the Kotlin side can interpret them when resuming.
        globals.load(LUA_YIELD_WRAPPERS, "work_internals").call()

        // Copy the Lua-defined functions into the work table
        work["wait"] = globals["_work_wait"]
        work["wait_until"] = globals["_work_wait_until"]
        work["wait_frame"] = globals["_work_wait_frame"]

        // Clean up temporary globals
        globals["_work_wait"] = NIL
        globals["_work_wait_until"] = NIL
        globals["_work_wait_frame"] = NIL

        arg2["work"] = work
        arg2["package"]["loaded"]["work"] = work
        return work
    }

    /**
     * Called each frame from [com.github.minigdx.tiny.resources.GameScript.advance].
     * Resumes eligible coroutines and cleans up dead ones.
     */
    suspend fun advance(time: Double) {
        val iterator = coroutines.iterator()
        while (iterator.hasNext()) {
            val tracked = iterator.next()

            // Check if the coroutine is dead
            if (isDead(tracked)) {
                iterator.remove()
                continue
            }

            val shouldResume = when (tracked.waitType) {
                WaitType.FRAME -> true
                WaitType.TIME -> time >= tracked.waitTarget
                WaitType.CONDITION -> {
                    tracked.waitFunction?.call()?.toboolean() == true
                }
            }

            if (shouldResume) {
                val result = coroutineResume.invokeSuspend(arrayOf(tracked.thread))
                parseYieldResult(tracked, result, time)

                // If the coroutine just finished, remove it
                if (isDead(tracked)) {
                    iterator.remove()
                }
            }
        }
    }

    private fun isDead(tracked: TrackedCoroutine): Boolean {
        return coroutineStatus.call(tracked.thread).tojstring() == "dead"
    }

    private fun parseYieldResult(
        tracked: TrackedCoroutine,
        result: Varargs,
        currentTime: Double,
    ) {
        val success = result.arg1().toboolean()
        if (!success) {
            // Coroutine errored — it will be detected as dead
            return
        }

        // Parse the yield marker: result is (true, marker, value...)
        val marker = result.arg(2)
        if (marker.isnil()) {
            // Simple yield with no marker — resume next frame
            tracked.waitType = WaitType.FRAME
            tracked.waitFunction = null
            return
        }

        when (marker.optjstring(null)) {
            "time" -> {
                val seconds = result.arg(3).todouble()
                tracked.waitType = WaitType.TIME
                tracked.waitTarget = currentTime + seconds
                tracked.waitFunction = null
            }
            "cond" -> {
                tracked.waitType = WaitType.CONDITION
                tracked.waitFunction = result.arg(3)
                tracked.waitTarget = 0.0
            }
            "frame" -> {
                tracked.waitType = WaitType.FRAME
                tracked.waitFunction = null
            }
            else -> {
                // Unknown marker — resume next frame
                tracked.waitType = WaitType.FRAME
                tracked.waitFunction = null
            }
        }
    }

    @TinyFunction(
        "Add a function as a background coroutine. " +
            "The function will be executed across multiple frames " +
            "and can use `work.wait()`, `work.wait_until()`, and `work.wait_frame()` to pause.",
        example = WORK_ADD_EXAMPLE,
    )
    inner class add : OneArgFunction() {
        @TinyCall("Add a function as a managed coroutine.")
        override fun call(
            @TinyArg("fn", type = LuaType.FUNCTION) arg: LuaValue,
        ): LuaValue {
            val thread = coroutineCreate.call(arg)
            val tracked = TrackedCoroutine(
                thread = thread,
                waitType = WaitType.FRAME,
                waitTarget = 0.0,
                waitFunction = null,
            )
            coroutines.add(tracked)
            return NIL
        }
    }

    @TinyFunction("Return the number of active coroutines.")
    inner class count : ZeroArgFunction() {
        @TinyCall("Get the count of active managed coroutines.")
        override fun call(): LuaValue {
            return valueOf(coroutines.size)
        }
    }

    private enum class WaitType {
        TIME,
        CONDITION,
        FRAME,
    }

    private class TrackedCoroutine(
        val thread: LuaValue,
        var waitType: WaitType,
        var waitTarget: Double,
        var waitFunction: LuaValue?,
    )

    companion object {
        private const val LUA_YIELD_WRAPPERS = """
            function _work_wait(seconds)
                coroutine.yield("time", seconds)
            end
            function _work_wait_until(fn)
                coroutine.yield("cond", fn)
            end
            function _work_wait_frame()
                coroutine.yield("frame")
            end
        """
    }
}
