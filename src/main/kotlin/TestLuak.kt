import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform

fun main(args: Array<String>) {

    // https://docs.oracle.com/javase/tutorial/2d/images/saveimage.html
    // https://mkyong.com/java/how-to-convert-bufferedimage-to-byte-in-java/
    // https://github.com/square/gifencoder
    val logger = StdOutLogger()
    try {
        val vfs = CommonVirtualFileSystem()
        val gameOption = GameOption(
            256,
            256,
            2,
            spriteSize = 16 to 16
        )

        GameEngine(
            gameOption = gameOption,
            platform = GlfwPlatform(gameOption, logger, vfs),
            vfs = vfs,
            logger = logger,
        ).main()
    } catch (ex: Exception) {
        logger.error("TINY", ex) { "An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it."}
    }
}


