package integration

import com.github.minigdx.tiny.platform.test.TestHelper
import kotlin.test.Test

class CoroutineTest {
    //language=Lua
    val clsScript = """
        local co = coroutine.create(function()
            gfx.cls(2)
            coroutine.yield()
            gfx.cls(1)
            coroutine.yield()
         
        end)
        
        function _draw()
            coroutine.resume(co)
        end
    """.trimIndent()

    @Test
    fun cls() = TestHelper.test("cls", clsScript) { platform ->
        platform.advance()

        TestHelper.assertEquals(
            """
2222222222
2222222222
2222222222
2222222222
2222222222
2222222222
2222222222
2222222222
2222222222
2222222222
""",
            platform.frames.last()
        )

        platform.advance()

        TestHelper.assertEquals(
            """
1111111111
1111111111
1111111111
1111111111
1111111111
1111111111
1111111111
1111111111
1111111111
1111111111
""",
            platform.frames.last()
        )
    }
}
