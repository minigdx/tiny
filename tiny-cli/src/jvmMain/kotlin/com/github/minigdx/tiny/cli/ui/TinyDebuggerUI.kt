package com.github.minigdx.tiny.cli.ui

import com.github.minigdx.tiny.cli.command.BreakpointHit
import com.github.minigdx.tiny.cli.command.DebugRemoteCommand
import com.github.minigdx.tiny.cli.command.EngineRemoteCommand
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
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.table.DefaultTableModel
import javax.swing.text.BadLocationException

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
            JScrollPane(table).apply {
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

            // Find row index for a given key (if it exists)
            fun findRowIndex(key: String): Int {
                for (i in 0 until tableModel.rowCount) {
                    if (tableModel.getValueAt(i, 0) == key) {
                        return i
                    }
                }
                return -1
            }

            for (command in engineCommandReceiver) {
                when (command) {
                    is BreakpointHit -> {
                        SwingUtilities.invokeLater {
                            (0 until tableModel.rowCount).forEach {
                                val name = tableModel.getValueAt(it, 0).toString()
                                val rowIndex = command.locals.containsKey(name)
                                if (!rowIndex) {
                                    tableModel.removeRow(it)
                                }
                            }
                            command.locals.forEach { (name, value) ->
                                val rowIndex = findRowIndex(name)
                                if (rowIndex != -1) {
                                    // Update existing row
                                    tableModel.setValueAt(value, rowIndex, 1)
                                } else {
                                    // Add new row
                                    tableModel.addRow(arrayOf(name, value))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addScriptTab(scriptName: String, scriptContent: String) {
        val textArea = RSyntaxTextArea(20, 60).apply {
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_LUA
            isCodeFoldingEnabled = true
            isEditable = false
            text = scriptContent
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
}
