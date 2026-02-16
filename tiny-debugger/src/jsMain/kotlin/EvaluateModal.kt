package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTextAreaElement

class EvaluateModal(
    private val overlay: HTMLDivElement,
) {
    private var onEvaluate: ((String) -> Unit)? = null
    private var resultDiv: HTMLDivElement? = null

    fun show(callback: (String) -> Unit) {
        onEvaluate = callback
        overlay.innerHTML = ""
        overlay.style.display = "flex"

        val modal = document.createElement("div") as HTMLDivElement
        modal.className = "modal"

        val title = document.createElement("div") as HTMLDivElement
        title.className = "modal-title"
        title.textContent = "Evaluate Expression"
        modal.appendChild(title)

        val desc = document.createElement("div") as HTMLDivElement
        desc.className = "modal-desc"
        desc.textContent = "Enter a Lua expression to evaluate in the current execution context."
        modal.appendChild(desc)

        val textarea = document.createElement("textarea") as HTMLTextAreaElement
        textarea.className = "modal-textarea"
        textarea.value = ""
        textarea.placeholder = "e.g. 1 + 1"
        modal.appendChild(textarea)

        val result = document.createElement("div") as HTMLDivElement
        result.className = "modal-result"
        result.style.display = "none"
        modal.appendChild(result)
        resultDiv = result

        val buttons = document.createElement("div") as HTMLDivElement
        buttons.className = "modal-buttons"

        val closeBtn = document.createElement("button") as HTMLButtonElement
        closeBtn.className = "modal-btn modal-btn-cancel"
        closeBtn.textContent = "Close"
        closeBtn.onclick = {
            hide()
        }
        buttons.appendChild(closeBtn)

        val evalBtn = document.createElement("button") as HTMLButtonElement
        evalBtn.className = "modal-btn modal-btn-save"
        evalBtn.textContent = "Evaluate"
        evalBtn.onclick = {
            val expression = textarea.value.trim()
            if (expression.isNotEmpty()) {
                onEvaluate?.invoke(expression)
            }
        }
        buttons.appendChild(evalBtn)

        modal.appendChild(buttons)
        overlay.appendChild(modal)

        textarea.focus()
    }

    fun showResult(
        value: String,
        error: String?,
    ) {
        val div = resultDiv ?: return
        div.style.display = "block"
        if (error != null) {
            div.className = "modal-result modal-result-error"
            div.textContent = error
        } else {
            div.className = "modal-result modal-result-success"
            div.textContent = value
        }
    }

    fun hide() {
        overlay.style.display = "none"
        resultDiv = null
        onEvaluate = null
    }
}
