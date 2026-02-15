package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

class ScriptList(
    private val container: HTMLDivElement,
    private val onSelect: (String) -> Unit,
) {
    private var activeScript: String? = null

    fun update(scripts: List<String>) {
        container.innerHTML = ""
        scripts.forEach { name ->
            val item = document.createElement("div") as HTMLElement
            item.className = "script-item"
            item.textContent = name
            if (name == activeScript) {
                item.classList.add("active")
            }
            item.onclick = {
                activeScript = name
                onSelect(name)
                update(scripts)
            }
            container.appendChild(item)
        }
    }

    fun setActive(
        name: String,
        scripts: List<String>,
    ) {
        activeScript = name
        update(scripts)
    }
}
