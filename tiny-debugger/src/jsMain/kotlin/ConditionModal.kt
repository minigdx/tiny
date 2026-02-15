package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTextAreaElement

class ConditionModal(
    private val overlay: HTMLDivElement,
) {
    private var onSave: ((String?) -> Unit)? = null

    fun show(
        currentCondition: String?,
        callback: (String?) -> Unit,
    ) {
        onSave = callback
        overlay.innerHTML = ""
        overlay.style.display = "flex"

        val modal = document.createElement("div") as HTMLDivElement
        modal.className = "modal"

        val title = document.createElement("div") as HTMLDivElement
        title.className = "modal-title"
        title.textContent = "Breakpoint Condition"
        modal.appendChild(title)

        val desc = document.createElement("div") as HTMLDivElement
        desc.className = "modal-desc"
        desc.textContent = "Enter a Lua expression. The breakpoint will only trigger when it evaluates to true."
        modal.appendChild(desc)

        val textarea = document.createElement("textarea") as HTMLTextAreaElement
        textarea.className = "modal-textarea"
        textarea.value = currentCondition ?: ""
        textarea.placeholder = "e.g. x > 10"
        modal.appendChild(textarea)

        val buttons = document.createElement("div") as HTMLDivElement
        buttons.className = "modal-buttons"

        val clearBtn = document.createElement("button") as HTMLButtonElement
        clearBtn.className = "modal-btn modal-btn-clear"
        clearBtn.textContent = "Clear"
        clearBtn.onclick = {
            overlay.style.display = "none"
            onSave?.invoke(null)
        }
        buttons.appendChild(clearBtn)

        val cancelBtn = document.createElement("button") as HTMLButtonElement
        cancelBtn.className = "modal-btn modal-btn-cancel"
        cancelBtn.textContent = "Cancel"
        cancelBtn.onclick = {
            overlay.style.display = "none"
        }
        buttons.appendChild(cancelBtn)

        val saveBtn = document.createElement("button") as HTMLButtonElement
        saveBtn.className = "modal-btn modal-btn-save"
        saveBtn.textContent = "Save"
        saveBtn.onclick = {
            overlay.style.display = "none"
            val value = textarea.value.trim()
            onSave?.invoke(value.ifEmpty { null })
        }
        buttons.appendChild(saveBtn)

        modal.appendChild(buttons)
        overlay.appendChild(modal)

        textarea.focus()
    }
}
