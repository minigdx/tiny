import com.github.minigdx.tiny.file.SourceStream
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get
import kotlin.js.Date

class EditorStream(field: String) : SourceStream<ByteArray> {

    private val exist: Boolean
    private var updated: Boolean = false
    private var timeout: Double = 0.0

    private var textarea: HTMLDivElement? = null
    private var share: HTMLAnchorElement? = null

    init {
        val textarea = document.getElementById(field.replace("#", "")) as? HTMLDivElement
        if (textarea == null) {
            exist = false
        } else {
            exist = true
            this.textarea = textarea
            textarea.addEventListener("input", {
                updated = true
                timeout = Date.now() + 1500 // add 1.5 second
            }, null,)
        }
        share = document.getElementById(field.replace("#editor", "share")) as? HTMLAnchorElement
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
        val value = textarea?.innerText ?: ""

        share?.href = "sandbox.html?game=" + window.btoa(value)

        return value.encodeToByteArray()
    }
}
