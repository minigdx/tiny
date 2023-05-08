import com.github.minigdx.tiny.file.SourceStream
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.get
import kotlin.js.Date

class EditorStream(field: String) : SourceStream<ByteArray> {

    private val exist: Boolean
    private var updated: Boolean = false
    private var timeout: Double = 0.0

    private var textarea: HTMLTextAreaElement? = null

    init {
        val textarea = document.getElementById(field.replace("#", "")) as? HTMLTextAreaElement
        if (textarea == null) {
            exist = false
        } else {
            exist = true
            this.textarea = textarea
            textarea.addEventListener("input", {
                updated = true
                timeout = Date.now() + 1500 // add 1.5 second
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
