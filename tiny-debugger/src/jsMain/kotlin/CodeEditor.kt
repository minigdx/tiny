package com.github.minigdx.tiny.debugger

import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

data class BreakpointMarker(
    val line: Int,
    val hasCondition: Boolean = false,
)

class CodeEditor(
    private val gutterContainer: HTMLDivElement,
    private val codeContainer: HTMLDivElement,
    private val onToggleBreakpoint: (Int) -> Unit,
    private val onConditionRequest: (Int) -> Unit,
) {
    private var content: String = ""
    private var lines: List<String> = emptyList()
    private var breakpoints = mutableSetOf<Int>()
    private var disabledBreakpoints = mutableSetOf<Int>()
    private var conditions = mutableMapOf<Int, String>()
    private var highlightedLine: Int? = null

    init {
        codeContainer.addEventListener("scroll", {
            gutterContainer.scrollTop = codeContainer.scrollTop
        })
    }

    fun setContent(code: String) {
        content = code
        lines = code.split("\n")
        render()
        codeContainer.scrollTop = 0.0
        gutterContainer.scrollTop = 0.0
    }

    fun setBreakpoints(
        bps: Set<Int>,
        conds: Map<Int, String>,
        disabled: Set<Int> = emptySet(),
    ) {
        breakpoints = bps.toMutableSet()
        conditions = conds.toMutableMap()
        disabledBreakpoints = disabled.toMutableSet()
        renderGutter()
    }

    fun highlightLine(line: Int?) {
        highlightedLine = line
        renderCode()
        renderGutter()
        if (line != null) {
            scrollToLine(line)
        }
    }

    fun toggleBreakpoint(line: Int) {
        if (breakpoints.contains(line)) {
            breakpoints.remove(line)
            conditions.remove(line)
        } else {
            breakpoints.add(line)
        }
        renderGutter()
    }

    fun setCondition(
        line: Int,
        condition: String?,
    ) {
        if (condition != null) {
            conditions[line] = condition
        } else {
            conditions.remove(line)
        }
        renderGutter()
    }

    fun hasBreakpoint(line: Int): Boolean = breakpoints.contains(line)

    fun getCondition(line: Int): String? = conditions[line]

    private fun render() {
        renderGutter()
        renderCode()
    }

    private fun renderGutter() {
        gutterContainer.innerHTML = ""
        for (i in lines.indices) {
            val lineNum = i + 1
            val gutterLine = document.createElement("div") as HTMLDivElement
            gutterLine.className = "gutter-line"

            if (highlightedLine == lineNum) {
                gutterLine.classList.add("gutter-hit")
            }

            if (breakpoints.contains(lineNum)) {
                val marker = document.createElement("span") as HTMLElement
                marker.className = "breakpoint-marker"
                marker.innerHTML =
                    if (conditions.containsKey(lineNum)) {
                        LucideIcons.circleEllipsis
                    } else {
                        LucideIcons.circleDot
                    }
                marker.title =
                    if (conditions.containsKey(lineNum)) {
                        "Conditional: ${conditions[lineNum]}"
                    } else {
                        "Breakpoint"
                    }
                gutterLine.appendChild(marker)
            } else if (disabledBreakpoints.contains(lineNum)) {
                val marker = document.createElement("span") as HTMLElement
                marker.className = "breakpoint-marker breakpoint-disabled"
                marker.innerHTML = LucideIcons.circleSlash2
                marker.title = "Breakpoint (disabled)"
                gutterLine.appendChild(marker)
            }

            val lineNumber = document.createElement("span") as HTMLElement
            lineNumber.className = "line-number"
            lineNumber.textContent = "$lineNum"
            gutterLine.appendChild(lineNumber)

            gutterLine.onclick = { e ->
                onToggleBreakpoint(lineNum)
            }

            gutterLine.oncontextmenu = { e ->
                e.preventDefault()
                onConditionRequest(lineNum)
            }

            gutterContainer.appendChild(gutterLine)
        }
    }

    private fun renderCode() {
        codeContainer.innerHTML = ""

        val highlighted = highlight(content)
        val codeLines = highlighted.split("\n")

        codeLines.forEachIndexed { index, line ->
            val lineDiv = document.createElement("div") as HTMLDivElement
            lineDiv.className = "code-line"
            lineDiv.innerHTML = line

            val lineNum = index + 1
            if (highlightedLine == lineNum) {
                lineDiv.classList.add("code-line-hit")
            } else {
                lineDiv.classList.remove("code-line-hit")
            }

            codeContainer.appendChild(lineDiv)
        }
    }

    fun scrollToLine(line: Int) {
        val lineElements = codeContainer.children
        val index = line - 1
        if (index >= 0 && index < lineElements.length) {
            val element = lineElements.item(index) as? HTMLElement
            element?.scrollIntoView(
                js("({behavior: 'smooth', block: 'center'})"),
            )
        }
    }
}
