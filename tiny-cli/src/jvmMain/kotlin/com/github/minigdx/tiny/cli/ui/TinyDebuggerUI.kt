package com.github.minigdx.tiny.cli.ui

import com.github.minigdx.tiny.cli.command.BreakpointHit
import com.github.minigdx.tiny.cli.command.DebugRemoteCommand
import com.github.minigdx.tiny.cli.command.EngineRemoteCommand
import com.github.minigdx.tiny.cli.command.ResumeExecution
import com.github.minigdx.tiny.cli.command.ToggleBreakpoint
import com.github.minigdx.tiny.cli.config.GameParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.Gutter
import org.fife.ui.rtextarea.IconRowEvent
import org.fife.ui.rtextarea.IconRowListener
import org.fife.ui.rtextarea.LineNumberList
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.table.DefaultTableModel
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter

/**
 * [TinyDebuggerUI] is the main class of the debugger.
 *
 * It's a Swing application that will display:
 * - the source code of the game
 * - the local variables of the game when a breakpoint is hit
 *
 * The debugger can:
 * - add/remove breakpoints
 * - display the local variables
 *
 * The debugger is connected to the game engine via two channels:
 * - [debugCommandSender] to send commands to the engine
 * - [engineCommandReceiver] to receive commands from the engine
 */

class TinyDebuggerUI(
    private val debugCommandSender: SendChannel<DebugRemoteCommand>,
    private val engineCommandReceiver: ReceiveChannel<EngineRemoteCommand>,
    private var gameParameters: GameParameters,
) : JFrame("\uD83E\uDDF8 Tiny Debugger") {

    private val tabbedPane = JTabbedPane()

    private val tableModel = DefaultTableModel(arrayOf("Name", "Value"), 0)
    private val table = JTable(tableModel)

    private val textAreas: MutableMap<String, RSyntaxTextArea> = mutableMapOf()

    private val io = CoroutineScope(Dispatchers.IO)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(800, 600)
        contentPane.layout = BoxLayout(contentPane, BoxLayout.X_AXIS)

        add(
            tabbedPane.apply {
                preferredSize = Dimension(600, 600)
            },
        )
        add(
            JPanel(BorderLayout()).apply {
                add(Toolbar(), BorderLayout.PAGE_START)
                add(JScrollPane(table), BorderLayout.CENTER)
            }.apply {
                preferredSize = Dimension(200, 600)
            },
        )

        io.launch {
            val scriptsContent = gameParameters.getAllScripts()
                .map { it to File(it).readText() }

            SwingUtilities.invokeLater {
                scriptsContent.forEach { (scriptName, scriptContent) ->
                    addScriptTab(scriptName, scriptContent)
                }
            }

            for (command in engineCommandReceiver) {
                when (command) {
                    is BreakpointHit -> {
                        SwingUtilities.invokeLater {
                            val textArea = textAreas[command.script]
                            textArea?.setActiveLineRange(command.line - 1, command.line - 1)
                            textArea?.highlightLine(command.line, LIGHT_RED)

                            tableModel.rowCount = 0
                            command.locals.forEach { (name, value) ->
                                tableModel.addRow(arrayOf(name, value))
                            }
                            command.upValues.forEach { (name, value) ->
                                tableModel.addRow(arrayOf(name, value))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Toolbar(): Component {
        return JPanel().apply {
            contentPane.layout = BoxLayout(contentPane, BoxLayout.X_AXIS)
            add(
                JButton("⏯").apply {
                    addActionListener {
                        io.launch {
                            debugCommandSender.send(ResumeExecution())
                            textAreas.values.forEach {
                                it.highlighter.removeAllHighlights()
                            }
                        }
                    }
                },
            )
            add(
                JButton("⤵").apply {
                    addActionListener {
                        io.launch {
                            // Goes to the next line
                            debugCommandSender.send(ResumeExecution(advanceByStep = true))
                            textAreas.values.forEach {
                                it.highlighter.removeAllHighlights()
                            }
                        }
                    }
                },
            )
        }
    }

    private fun RSyntaxTextArea.highlightLine(lineNumber: Int, color: Color) {
        try {
            // Convert line number to offset
            val start = this.getLineStartOffset(lineNumber - 1)
            val end = this.getLineEndOffset(lineNumber - 1)

            // Highlight the line
            this.highlighter.addHighlight(start, end, DefaultHighlighter.DefaultHighlightPainter(color))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addScriptTab(scriptName: String, scriptContent: String) {
        val textArea = RSyntaxTextArea(20, 60).apply {
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_LUA
            isCodeFoldingEnabled = true
            isEditable = false
            text = scriptContent
            highlightCurrentLine = false
        }
        textAreas[scriptName] = textArea

        val scrollPane = RTextScrollPane(textArea)
        scrollPane.gutter.bookmarkIcon = UIManager.getIcon("FileView.directoryIcon")
        scrollPane.gutter.isBookmarkingEnabled = true
        scrollPane.gutter.addIconRowListener(GutterListener(scriptName))
        scrollPane.gutter.addLineNumberListener(LineNumberListener(scrollPane))

        val panel = JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
        }

        tabbedPane.addTab(scriptName, panel)
    }

    private fun Gutter.addLineNumberListener(mouseListener: MouseListener) {
        val lineNumberList = Gutter::class.java.getDeclaredField("lineNumberList")
        lineNumberList.isAccessible = true
        val lineNumber = lineNumberList.get(this) as LineNumberList
        lineNumber.addMouseListener(mouseListener)
    }

    inner class LineNumberListener(private val scrollPane: RTextScrollPane) : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val l = viewToModelLine(scrollPane.textArea, e.point)
            scrollPane.gutter.toggleBookmark(l)
        }

        @Throws(BadLocationException::class)
        private fun viewToModelLine(textArea: RTextArea, p: Point): Int {
            val offs: Int = textArea.viewToModel2D(p)
            return if (offs > -1) textArea.getLineOfOffset(offs) else -1
        }
    }

    inner class GutterListener(private val scriptName: String) : IconRowListener {
        override fun bookmarkAdded(e: IconRowEvent) {
            io.launch {
                debugCommandSender.send(ToggleBreakpoint(scriptName, e.line + 1, true))
            }
        }

        override fun bookmarkRemoved(e: IconRowEvent) {
            io.launch {
                debugCommandSender.send(ToggleBreakpoint(scriptName, e.line + 1, false))
            }
        }
    }

    data class CurrentBreakpoint(var scriptName: String, var line: Int)

    companion object {
        private val LIGHT_RED = Color(255, 102, 102, 100)
    }
}
