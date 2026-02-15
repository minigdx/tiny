package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement

class Toolbar(
    private val container: HTMLDivElement,
    private val onResume: () -> Unit,
    private val onStep: () -> Unit,
    private val onDisconnect: () -> Unit,
) {
    private val statusIndicator: HTMLSpanElement

    init {
        container.innerHTML = ""

        val resumeBtn = document.createElement("button") as HTMLButtonElement
        resumeBtn.className = "toolbar-btn"
        resumeBtn.innerHTML = "&#9654; Resume"
        resumeBtn.title = "Resume execution"
        resumeBtn.onclick = { onResume() }
        container.appendChild(resumeBtn)

        val stepBtn = document.createElement("button") as HTMLButtonElement
        stepBtn.className = "toolbar-btn"
        stepBtn.innerHTML = "&#10140; Step"
        stepBtn.title = "Step to next line"
        stepBtn.onclick = { onStep() }
        container.appendChild(stepBtn)

        val disconnectBtn = document.createElement("button") as HTMLButtonElement
        disconnectBtn.className = "toolbar-btn toolbar-btn-danger"
        disconnectBtn.innerHTML = "&#10005; Disconnect"
        disconnectBtn.title = "Disconnect debugger"
        disconnectBtn.onclick = { onDisconnect() }
        container.appendChild(disconnectBtn)

        statusIndicator = document.createElement("span") as HTMLSpanElement
        statusIndicator.className = "status-indicator disconnected"
        statusIndicator.textContent = "Disconnected"
        container.appendChild(statusIndicator)
    }

    fun setConnected(connected: Boolean) {
        if (connected) {
            statusIndicator.className = "status-indicator connected"
            statusIndicator.textContent = "Connected"
        } else {
            statusIndicator.className = "status-indicator disconnected"
            statusIndicator.textContent = "Disconnected"
        }
    }
}
