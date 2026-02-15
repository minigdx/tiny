package com.github.minigdx.tiny.debugger

import com.github.minigdx.tiny.cli.debug.LuaValue
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableSectionElement

class VariableInspector(
    private val container: HTMLDivElement,
) {
    fun update(
        locals: Map<String, LuaValue>,
        upValues: Map<String, LuaValue>,
    ) {
        container.innerHTML = ""

        if (locals.isNotEmpty()) {
            addSection("Locals", locals)
        }

        if (upValues.isNotEmpty()) {
            addSection("UpValues", upValues)
        }

        if (locals.isEmpty() && upValues.isEmpty()) {
            val empty = document.createElement("div") as HTMLDivElement
            empty.className = "var-empty"
            empty.textContent = "No variables to display"
            container.appendChild(empty)
        }
    }

    fun clear() {
        container.innerHTML = ""
        val empty = document.createElement("div") as HTMLDivElement
        empty.className = "var-empty"
        empty.textContent = "Hit a breakpoint to inspect variables"
        container.appendChild(empty)
    }

    private fun addSection(
        title: String,
        variables: Map<String, LuaValue>,
    ) {
        val header = document.createElement("div") as HTMLDivElement
        header.className = "var-section-header"
        header.textContent = title
        container.appendChild(header)

        val table = document.createElement("table") as HTMLTableElement
        table.className = "var-table"

        val thead = document.createElement("thead") as HTMLElement
        thead.innerHTML = "<tr><th>Name</th><th>Value</th></tr>"
        table.appendChild(thead)

        val tbody = document.createElement("tbody") as HTMLTableSectionElement
        variables.forEach { (name, value) ->
            addVariableRow(tbody, name, value, 0)
        }
        table.appendChild(tbody)

        container.appendChild(table)
    }

    private fun addVariableRow(
        tbody: HTMLTableSectionElement,
        name: String,
        value: LuaValue,
        depth: Int,
        insertAfterRow: HTMLElement? = null,
    ): HTMLElement {
        val row = document.createElement("tr") as HTMLElement
        row.className = if (depth > 0) "var-row var-child" else "var-row"
        row.setAttribute("data-depth", depth.toString())

        val nameCell = document.createElement("td") as HTMLElement
        nameCell.className = "var-name"
        nameCell.style.paddingLeft = "${depth * 16 + 8}px"

        val valueCell = document.createElement("td") as HTMLElement
        valueCell.className = "var-value"

        when (value) {
            is LuaValue.Primitive -> {
                nameCell.textContent = name
                valueCell.textContent = value.value
            }
            is LuaValue.Dictionary -> {
                val toggle = document.createElement("span") as HTMLElement
                toggle.className = "var-toggle"
                toggle.textContent = "\u25B6 "
                nameCell.appendChild(toggle)

                val nameSpan = document.createElement("span") as HTMLElement
                nameSpan.textContent = name
                nameCell.appendChild(nameSpan)

                valueCell.textContent = "{${value.entries.size} entries}"

                var expanded = false

                row.onclick = {
                    expanded = !expanded
                    toggle.textContent = if (expanded) "\u25BC " else "\u25B6 "
                    if (expanded) {
                        var insertAfter = row
                        value.entries.forEach { (childName, childValue) ->
                            insertAfter = addVariableRow(tbody, childName, childValue, depth + 1, insertAfter)
                        }
                    } else {
                        // Remove all following rows with depth greater than current
                        while (true) {
                            val next = row.nextElementSibling as? HTMLElement ?: break
                            val nextDepth = next.getAttribute("data-depth")?.toIntOrNull() ?: 0
                            if (nextDepth <= depth) break
                            next.remove()
                        }
                    }
                }
            }
        }

        row.appendChild(nameCell)
        row.appendChild(valueCell)

        if (insertAfterRow != null) {
            insertAfterRow.after(row)
        } else {
            tbody.appendChild(row)
        }

        return row
    }
}
