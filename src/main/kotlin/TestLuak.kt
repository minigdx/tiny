import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.LogLevel
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.platform.GlfwPlatform

fun main(args: Array<String>) {

    val logger = StdOutLogger()
    try {
        GameEngine(
            gameOption = GameOption(),
            platform = GlfwPlatform(logger),
            vfs = CommonVirtualFileSystem()
        ).main()
    } catch (ex: Exception) {
        logger.error("TINY", ex) { "An unexpected exception occurred. The application will stop. It might be a bug in Tiny. If so, please report it."}
    }
}


