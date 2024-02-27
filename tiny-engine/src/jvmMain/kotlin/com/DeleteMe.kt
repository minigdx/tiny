import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.TouchSignal
import com.github.minigdx.tiny.input.Vector2
import com.github.minigdx.tiny.lua.SfxLib
import com.github.minigdx.tiny.platform.glfw.JavaMidiSoundManager

fun main() {
    val score = """tiny-sfx 80 255
            |02 00 00 00 00 00 01 01 00 FF 01
    |011FFF 011FFF 011FFF 011FFF 011FFF 011FFF
    |021FFF 0112FF 021FFF 0112FF 0212FF
    |1 1 1
    |00 00 00 00 00 00 00 00 00 00 00
    |00 00 00 00 00 00 00 00 00 00 00
    |00 00 00 00 00 00 00 00 00 00 00
    """.trimMargin()

    val song = SfxLib.convertScoreToSong2(score)
    val javaMidiSoundManager = JavaMidiSoundManager()
    javaMidiSoundManager.initSoundManager(
        object : InputHandler {
            override fun isKeyJustPressed(key: Key): Boolean {
                TODO("Not yet implemented")
            }

            override fun isKeyPressed(key: Key): Boolean {
                TODO("Not yet implemented")
            }

            override fun isTouched(signal: TouchSignal): Vector2? {
                TODO("Not yet implemented")
            }

            override fun isJustTouched(signal: TouchSignal): Vector2? {
                TODO("Not yet implemented")
            }

            override val currentTouch: Vector2
                get() = TODO("Not yet implemented")
        },
    )

    println("beats: " + song.numberOfBeats)
    println("samples: " + song.numberOfTotalSample)

    javaMidiSoundManager.playSong(song)

    Thread.sleep(5000)
    javaMidiSoundManager.destroy()
}
