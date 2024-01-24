package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.engine.GameEngine
import com.github.minigdx.tiny.engine.GameResourceAccess
import com.github.minigdx.tiny.file.CommonVirtualFileSystem
import com.github.minigdx.tiny.log.StdOutLogger
import com.github.minigdx.tiny.lua.Note
import com.github.minigdx.tiny.lua.errorLine
import com.github.minigdx.tiny.platform.glfw.GlfwPlatform
import com.github.minigdx.tiny.render.LwjglGLRender
import com.github.minigdx.tiny.sound.NoiseWave
import com.github.minigdx.tiny.sound.PulseWave
import com.github.minigdx.tiny.sound.SilenceWave
import com.github.minigdx.tiny.sound.SineWave
import com.github.minigdx.tiny.sound.SoundManager
import com.github.minigdx.tiny.sound.TriangleWave
import kotlinx.serialization.json.decodeFromStream
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import java.io.File

class MusicLib(private val soundManager: SoundManager) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ctrl = LuaTable()
        ctrl.set("play", play())
        arg2.set("music", ctrl)
        arg2.get("package").get("loaded").set("music", ctrl)
        return ctrl
    }

    private fun extractNote(str: String): Note {
        val note = str.substringAfter("(").substringBefore(")")
        return Note.valueOf(note)
    }

    private val acceptedTypes = setOf("sine", "noise", "pulse", "triangle")

    private fun extractWaveType(str: String): String? {
        if (str == "*") return str

        val type = str.substringBefore("(")
        return if (acceptedTypes.contains(type)) {
            type
        } else {
            null
        }
    }

    inner class play : TwoArgFunction() {

        fun trim(str: String): String {
            val lastIndex = str.lastIndexOf(')')
            if (lastIndex < 0) return str
            return str.substring(0, lastIndex + 2)
        }

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val bpm = arg2.optint(120)
            val duration = 60 / bpm.toFloat() / 4
            val score = arg1.optjstring("")!!
            val parts = trim(score).split("-")
            val waves = parts.mapNotNull {
                val wave = extractWaveType(it)
                when (wave) {
                    "*" -> SilenceWave(duration)
                    "sine" -> SineWave(extractNote(it), duration)
                    "triangle" -> TriangleWave(extractNote(it), duration)
                    "noise" -> NoiseWave(extractNote(it), duration)
                    "pulse" -> PulseWave(extractNote(it), duration)
                    else -> null
                }
            }
            soundManager.playSfx(waves)
            return NIL
        }
    }
}

private val customizeGlobal: GameResourceAccess.(Globals) -> Unit = { global ->
    val engine = (this as? GameEngine)
    if (engine != null) {
        global.load(MusicLib(engine.soundManager))
    }
}

class SfxCommand : CliktCommand(name = "sfx", help = "Start the SFX Editor") {
    fun isOracleOrOpenJDK(): Boolean {
        val vendor = System.getProperty("java.vendor")?.lowercase()
        return vendor?.contains("oracle") == true || vendor?.contains("eclipse") == true || vendor?.contains("openjdk") == true
    }

    fun isMacOS(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }

    override fun run() {
        if (isMacOS() && isOracleOrOpenJDK()) {
            echo("\uD83D\uDEA7 === The Tiny CLI on Mac with require a special option.")
            echo("\uD83D\uDEA7 === If the application crash ➡ use the command 'tiny-cli-mac' instead.")
        }

        try {
            val configFile = SfxCommand::class.java.getResourceAsStream("/sfx/_tiny.json")
            if (configFile == null) {
                echo(
                    "\uD83D\uDE2D No _tiny.json found! Can't run the game without. " +
                        "The tiny-cli command doesn't seems to be bundled correctly. You might want to report an issue.",
                )
                throw Abort()
            }
            val gameParameters = GameParameters.JSON.decodeFromStream<GameParameters>(configFile)

            val logger = StdOutLogger("tiny-cli")
            val vfs = CommonVirtualFileSystem()
            val gameOption = gameParameters.toGameOptions()
            GameEngine(
                gameOptions = gameOption,
                platform = GlfwPlatform(
                    gameOption,
                    logger,
                    vfs,
                    File("."),
                    LwjglGLRender(logger, gameOption),
                    jarResourcePrefix = "/sfx",
                ),
                vfs = vfs,
                logger = logger,
                customizeGlobal,
            ).main()
        } catch (ex: Exception) {
            echo(
                "\uD83E\uDDE8 An unexpected exception occurred. " +
                    "The application will stop. " +
                    "It might be a bug in Tiny. " +
                    "If so, please report it.",
            )
            when (ex) {
                is LuaError -> {
                    val (nb, line) = ex.errorLine() ?: (null to null)
                    echo("Error found line $nb:$line")
                }
            }
            echo()
            ex.printStackTrace()
        }
    }
}
