package integration

import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOptions
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.ImageData
import com.github.minigdx.tiny.platform.test.HeadlessPlatform
import kotlin.test.Test

class GfxTest {

    private val colors = listOf(
        "#000000",
        "#FFFFFF",
        "#FF0000",
    )
    @Test
    fun todo() {
        val script = """
            x = 5
            
            function _draw()
                gfx.cls(2)
                shape.circlef(x, 5, 2, 1)
                x = x + 1
            end
        """.trimIndent()

        val resources = mapOf(
            "game.lua" to script.encodeToByteArray(),
            "_boot.lua" to """tiny.exit(0)""".encodeToByteArray(),
            "_engine.lua" to "".encodeToByteArray(),
            "_boot.png" to ImageData("".encodeToByteArray(), 0, 0)
        )

        val gameOptions = GameOptions(10, 10, colors, listOf("game.lua"), emptyList())
        val platform = HeadlessPlatform(gameOptions, resources)

        GameEngine(
            gameOptions = gameOptions,
            platform = platform,
            vfs = CommonVirtualFileSystem(),
            logger = StdOutLogger("test")

        ).main()

        platform.advance()
        platform.advance()
        platform.advance()
        platform.frames.forEach {
            println(it.colorIndexBuffer)
        }
    }
}
