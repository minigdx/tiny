import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameOption
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.platform.GlfwPlatform

fun main(args: Array<String>) {

    GameEngine(
        gameOption = GameOption(),
        platform = GlfwPlatform(),
        vfs = CommonVirtualFileSystem()
    ).main()

}


