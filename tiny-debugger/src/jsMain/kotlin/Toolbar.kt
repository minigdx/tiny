package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement

class Toolbar(
    private val container: HTMLDivElement,
    private val onResume: () -> Unit,
    private val onStep: () -> Unit,
    private val onStepOver: () -> Unit,
    private val onDisconnect: () -> Unit,
    private val onConnect: () -> Unit,
) {
    private val resumeBtn: HTMLButtonElement
    private val disconnectBtn: HTMLButtonElement

    init {
        container.innerHTML = ""

        resumeBtn = document.createElement("button") as HTMLButtonElement
        resumeBtn.className = "toolbar-btn"
        resumeBtn.innerHTML = LucideIcons.pause
        resumeBtn.title = "Running"
        container.appendChild(resumeBtn)

        val stepBtn = document.createElement("button") as HTMLButtonElement
        stepBtn.className = "toolbar-btn"
        stepBtn.innerHTML = "${LucideIcons.stepInto} Step In"
        stepBtn.title = "Step to next instruction"
        stepBtn.onclick = { onStep() }
        container.appendChild(stepBtn)

        val stepOverBtn = document.createElement("button") as HTMLButtonElement
        stepOverBtn.className = "toolbar-btn"
        stepOverBtn.innerHTML = "${LucideIcons.stepOver} Step Over"
        stepOverBtn.title = "Step to next line in current script"
        stepOverBtn.onclick = { onStepOver() }
        container.appendChild(stepOverBtn)

        disconnectBtn = document.createElement("button") as HTMLButtonElement
        disconnectBtn.className = "toolbar-btn toolbar-btn-danger"
        disconnectBtn.innerHTML = "${LucideIcons.unplug} Disconnect"
        disconnectBtn.title = "Disconnect debugger"
        disconnectBtn.onclick = { onDisconnect() }
        container.appendChild(disconnectBtn)
    }

    fun setPaused(paused: Boolean) {
        if (paused) {
            resumeBtn.className = "toolbar-btn toolbar-btn-paused"
            resumeBtn.innerHTML = LucideIcons.play
            resumeBtn.title = "Resume execution"
            resumeBtn.onclick = { onResume() }
        } else {
            resumeBtn.className = "toolbar-btn"
            resumeBtn.innerHTML = LucideIcons.pause
            resumeBtn.title = "Running"
            resumeBtn.onclick = null
        }
    }

    fun setConnected(connected: Boolean) {
        if (connected) {
            disconnectBtn.className = "toolbar-btn toolbar-btn-danger"
            disconnectBtn.innerHTML = "${LucideIcons.unplug} Disconnect"
            disconnectBtn.title = "Disconnect debugger"
            disconnectBtn.onclick = { onDisconnect() }
        } else {
            disconnectBtn.className = "toolbar-btn toolbar-btn-success"
            disconnectBtn.innerHTML = "${LucideIcons.plug} Connect"
            disconnectBtn.title = "Connect to debugger"
            disconnectBtn.onclick = { onConnect() }
        }
    }
}
