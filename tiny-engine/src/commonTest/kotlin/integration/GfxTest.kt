package integration

import com.github.minigdx.tiny.platform.test.TestHelper.assertEquals
import com.github.minigdx.tiny.platform.test.TestHelper.test
import kotlin.test.Test
import kotlin.test.assertEquals

class GfxTest {
    val clsScript =
        """
        function _draw()
            gfx.cls(2)
        end
        """.trimIndent()

    @Test
    fun cls() =
        test("cls", clsScript) { platform ->
            platform.advance()

            assertEquals(
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
                platform.frames.last(),
            )
        }
}
