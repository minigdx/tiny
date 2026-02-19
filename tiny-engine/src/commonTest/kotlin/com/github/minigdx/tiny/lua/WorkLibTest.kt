package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue.Companion.valueOf
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkLibTest {

    private fun createGlobals(): Pair<Globals, WorkLib> {
        val workLib = WorkLib()
        val resourceAccess = mock<GameResourceAccess> { }
        val globals = Globals().apply {
            load(TinyBaseLib(resourceAccess))
            load(PackageLib())
            load(Bit32Lib())
            load(TableLib())
            load(StringLib())
            load(CoroutineLib())
            load(workLib)
            LoadState.install(this)
            LuaC.install(this)
        }
        return globals to workLib
    }

    @Test
    fun `add creates coroutine and runs on first advance`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            result = 0
            work.add(function()
                result = 42
            end)
            """,
        ).call()

        // Coroutine not yet started
        assertEquals(0, globals["result"].toint())

        // First advance triggers the coroutine
        workLib.advance(0.0)
        assertEquals(42, globals["result"].toint())
    }

    @Test
    fun `wait pauses coroutine for given duration`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            step = 0
            work.add(function()
                step = 1
                work.wait(1.0)
                step = 2
            end)
            """,
        ).call()

        // First advance: coroutine starts, sets step=1, then yields with wait(1.0)
        workLib.advance(0.0)
        assertEquals(1, globals["step"].toint())

        // 0.5s later: not enough time has passed
        workLib.advance(0.5)
        assertEquals(1, globals["step"].toint())

        // 1.0s later: time threshold reached
        workLib.advance(1.0)
        assertEquals(2, globals["step"].toint())
    }

    @Test
    fun `wait_until resumes when condition becomes true`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            flag = false
            result = 0
            work.add(function()
                work.wait_until(function() return flag end)
                result = 99
            end)
            """,
        ).call()

        // First advance: coroutine starts, yields on wait_until
        workLib.advance(0.0)
        assertEquals(0, globals["result"].toint())

        // flag is still false
        workLib.advance(0.1)
        assertEquals(0, globals["result"].toint())

        // Set flag to true
        globals["flag"] = valueOf(true)
        workLib.advance(0.2)
        assertEquals(99, globals["result"].toint())
    }

    @Test
    fun `wait_frame pauses for exactly one frame`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            step = 0
            work.add(function()
                step = 1
                work.wait_frame()
                step = 2
                work.wait_frame()
                step = 3
            end)
            """,
        ).call()

        workLib.advance(0.0)
        assertEquals(1, globals["step"].toint())

        workLib.advance(0.0)
        assertEquals(2, globals["step"].toint())

        workLib.advance(0.0)
        assertEquals(3, globals["step"].toint())
    }

    @Test
    fun `dead coroutines are cleaned up`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            work.add(function()
                -- completes immediately
            end)
            """,
        ).call()

        // Advance to run and complete the coroutine
        workLib.advance(0.0)

        // Count should be 0 after the coroutine finishes
        assertEquals(0, globals.load("return work.count()").call().toint())
    }

    @Test
    fun `count returns number of active coroutines`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            work.add(function() work.wait(10) end)
            work.add(function() work.wait(10) end)
            """,
        ).call()

        assertEquals(2, globals.load("return work.count()").call().toint())

        // Start both coroutines
        workLib.advance(0.0)
        assertEquals(2, globals.load("return work.count()").call().toint())
    }

    @Test
    fun `multiple waits in sequence work correctly`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            step = 0
            work.add(function()
                step = 1
                work.wait(0.5)
                step = 2
                work.wait(0.5)
                step = 3
            end)
            """,
        ).call()

        workLib.advance(0.0) // start
        assertEquals(1, globals["step"].toint())

        workLib.advance(0.5) // first wait done
        assertEquals(2, globals["step"].toint())

        workLib.advance(0.8) // not enough for second wait (target = 0.5 + 0.5 = 1.0)
        assertEquals(2, globals["step"].toint())

        workLib.advance(1.0) // second wait done
        assertEquals(3, globals["step"].toint())
    }

    @Test
    fun `multiple independent coroutines run concurrently`() = runTest {
        val (globals, workLib) = createGlobals()
        globals.load(
            """
            a = 0
            b = 0
            work.add(function()
                work.wait(1.0)
                a = 10
            end)
            work.add(function()
                work.wait(2.0)
                b = 20
            end)
            """,
        ).call()

        workLib.advance(0.0) // start both
        assertEquals(0, globals["a"].toint())
        assertEquals(0, globals["b"].toint())

        workLib.advance(1.0) // first coroutine completes
        assertEquals(10, globals["a"].toint())
        assertEquals(0, globals["b"].toint())

        workLib.advance(2.0) // second coroutine completes
        assertEquals(10, globals["a"].toint())
        assertEquals(20, globals["b"].toint())
    }
}
