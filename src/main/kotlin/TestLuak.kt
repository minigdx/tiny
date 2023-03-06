import com.github.minigdx.tiny.GameEngine
import com.github.minigdx.tiny.GameOption
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.platform.GlfwPlatform

class Texture(val data: ByteArray)

fun main(args: Array<String>) {

    GameEngine(
        gameOption = GameOption(),
        platform = GlfwPlatform(),
        vfs = CommonVirtualFileSystem()
    ).main()

}


