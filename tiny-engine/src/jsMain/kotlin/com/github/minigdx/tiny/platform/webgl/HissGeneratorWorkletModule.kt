import com.github.minigdx.tiny.platform.webgl.Todo
import web.audio.AudioWorkletModule
import web.audio.AudioWorkletProcessorName
import web.audio.registerProcessor

val HissGeneratorWorkletModule = AudioWorkletModule { self ->
    self.registerProcessor(AudioWorkletProcessorName("TODO"), Todo)
}
