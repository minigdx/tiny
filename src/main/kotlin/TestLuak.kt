import com.github.minigdx.tiny.GameEngine
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.platform.GlfwPlatform

class Texture(val data: ByteArray)

fun main(args: Array<String>) {

    GameEngine(
        platform = GlfwPlatform(),
        vfs = CommonVirtualFileSystem()
    ).main()

}


