import com.github.minigdx.tiny.file.SourceStream
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import kotlin.js.Date

class EditorStream(val field: String) : SourceStream<ByteArray> {

    val exist: Boolean
    var updated: Boolean = false
    var timeout: Double = 0.0

    var textarea: HTMLTextAreaElement? = null

    init {
        val codeInput = document.getElementById(field.replace("#", ""))
        if (codeInput == null) {
            exist = false
        } else {
            exist = true
            val textareas = codeInput.getElementsByTagName("textarea")
            textarea = textareas[0] as HTMLTextAreaElement
            textarea?.addEventListener("input", {
                updated = true
                timeout = Date.now() + 2000 // add 2 seconds
            }, null)
        }
    }

    override suspend fun exists(): Boolean = exist

    override fun wasModified(): Boolean {
        val wasModified = updated && timeout <= Date.now()
        if (wasModified) {
            updated = false
        }
        return exist && wasModified
    }

    override suspend fun read(): ByteArray {
        val value = textarea?.value ?: ""
        return value.encodeToByteArray()
    }
}
