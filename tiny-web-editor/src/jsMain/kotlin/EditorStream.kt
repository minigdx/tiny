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
        val textarea = document.getElementById(field.replace("#", "")) as? HTMLTextAreaElement
        if (textarea == null) {
            exist = false
        } else {
            exist = true
            this.textarea = textarea
            textarea.addEventListener("input", {
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
