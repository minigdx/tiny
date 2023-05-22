package integration

import com.github.minigdx.tiny.platform.test.TestHelper
import com.github.minigdx.tiny.platform.test.TestHelper.test
import kotlin.test.Test

class ShapeTest {

    //language=Lua
    private val circlefScript = """
        function _draw()
           gfx.cls(1)
           shape.circlef(5, 5, 3, 2)
         end
    """.trimIndent()

    @Test
    fun circlef() = test("circlef", circlefScript) { platform ->
        platform.advance()

        TestHelper.assertEquals(
            """
1111111111
1111111111
1111222111
1112222211
1122222221
1122222221
1122222221
1112222211
1111222111
1111111111
""",
            platform.frames.last()
        )
    }

    //language=Lua
    private val trianglefScript = """
       function _draw()
           gfx.cls(1)
           shape.trianglef(0, 0, 0, 10, 10, 0, 2)
       end 
    """.trimIndent()

    @Test
    fun trianglef() = test("trianglef", trianglefScript) { platform ->
        platform.advance()

        TestHelper.assertEquals(
            """
2222222222
2222222221
2222222211
2222222111
2222221111
2222211111
2222111111
2221111111
2211111111
2111111111
""",
            platform.frames.last()
        )
    }

    //language=Lua
    private val rectfScript = """
       function _draw()
           gfx.cls(1)
           shape.rectf(0, 0, 5, 10, 2)
       end 
    """.trimIndent()

    @Test
    fun rectf() = test("rectf", rectfScript) { platform ->
        platform.advance()

        TestHelper.assertEquals(
            """
2222211111
2222211111
2222211111
2222211111
2222211111
2222211111
2222211111
2222211111
2222211111
2222211111
""",
            platform.frames.last()
        )
    }
}
