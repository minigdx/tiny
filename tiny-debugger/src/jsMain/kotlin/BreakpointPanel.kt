package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement

class BreakpointPanel(
    private val container: HTMLDivElement,
    private val onToggleBreakpoint: (String, Int, Boolean) -> Unit,
    private val onRemoveBreakpoint: (String, Int) -> Unit,
    private val onNavigateBreakpoint: (String, Int) -> Unit,
    private val onRemoveAll: () -> Unit,
) {
    fun update(
        breakpoints: Map<String, Set<Int>>,
        conditions: Map<String, Map<Int, String>>,
    ) {
        container.innerHTML = ""

        val allBps = mutableListOf<Triple<String, Int, String?>>()
        breakpoints.forEach { (script, lines) ->
            lines.sorted().forEach { line ->
                val condition = conditions[script]?.get(line)
                allBps.add(Triple(script, line, condition))
            }
        }

        if (allBps.isEmpty()) {
            val empty = document.createElement("div") as HTMLDivElement
            empty.className = "bp-empty"
            empty.textContent = "No breakpoints set"
            container.appendChild(empty)
            return
        }

        // Master row with count and remove-all button
        val masterRow = document.createElement("div") as HTMLDivElement
        masterRow.className = "bp-master-row"

        val masterLabel = document.createElement("span") as HTMLSpanElement
        masterLabel.className = "bp-info"
        masterLabel.textContent = "${allBps.size} breakpoint${if (allBps.size != 1) "s" else ""}"
        masterRow.appendChild(masterLabel)

        val removeAllBtn = document.createElement("span") as HTMLElement
        removeAllBtn.className = "bp-remove-all"
        removeAllBtn.innerHTML = LucideIcons.trash2
        removeAllBtn.title = "Remove all breakpoints"
        removeAllBtn.onclick = {
            onRemoveAll()
        }
        masterRow.appendChild(removeAllBtn)

        container.appendChild(masterRow)

        // Individual breakpoint rows
        allBps.forEach { (script, line, condition) ->
            val row = document.createElement("div") as HTMLDivElement
            row.className = "bp-row"

            val checkbox = document.createElement("input") as HTMLInputElement
            checkbox.type = "checkbox"
            checkbox.className = "bp-toggle"
            checkbox.checked = true
            checkbox.onchange = {
                onToggleBreakpoint(script, line, checkbox.checked)
            }
            row.appendChild(checkbox)

            val info = document.createElement("span") as HTMLSpanElement
            info.className = "bp-info"
            info.textContent = "$script:$line"
            info.title = "Click to navigate"
            info.onclick = {
                onNavigateBreakpoint(script, line)
            }
            row.appendChild(info)

            if (condition != null) {
                val badge = document.createElement("span") as HTMLSpanElement
                badge.className = "bp-condition-badge"
                badge.innerHTML = LucideIcons.bug
                badge.title = "Condition: $condition"
                row.appendChild(badge)
            }

            val removeBtn = document.createElement("span") as HTMLElement
            removeBtn.className = "bp-remove"
            removeBtn.innerHTML = LucideIcons.trash2
            removeBtn.title = "Remove breakpoint"
            removeBtn.onclick = { e ->
                e.stopPropagation()
                onRemoveBreakpoint(script, line)
            }
            row.appendChild(removeBtn)

            container.appendChild(row)
        }
    }
}
