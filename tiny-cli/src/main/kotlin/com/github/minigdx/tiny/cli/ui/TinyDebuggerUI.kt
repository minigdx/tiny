package com.github.minigdx.tiny.cli.ui

import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.debug.BreakpointHit
import com.github.minigdx.tiny.cli.debug.CurrentBreakpoints
import com.github.minigdx.tiny.cli.debug.DebugRemoteCommand
import com.github.minigdx.tiny.cli.debug.Disconnect
import com.github.minigdx.tiny.cli.debug.EngineRemoteCommand
import com.github.minigdx.tiny.cli.debug.LuaValue
import com.github.minigdx.tiny.cli.debug.Reload
import com.github.minigdx.tiny.cli.debug.RequestBreakpoints
import com.github.minigdx.tiny.cli.debug.ResumeExecution
import com.github.minigdx.tiny.cli.debug.ToggleBreakpoint
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
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

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

    // Custom table model to store variable values
    private val tableModel = object : DefaultTableModel(arrayOf("Name", "Value"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            // Make all cells non-editable
            return false
        }
    }
    private val table = JTable(tableModel).apply {
        setDefaultRenderer(Object::class.java, VariableCellRenderer())

        // Add a mouse listener to handle clicks on dictionary cells
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = rowAtPoint(e.point)
                val column = columnAtPoint(e.point)

                if (row >= 0 && column == 1) {  // Only for the value column
                    val name = getValueAt(row, 0) as String
                    val luaValue = variableValues[name]

                    if (luaValue is LuaValue.Dictionary) {
                        showDictionaryDialog(name, luaValue)
                    }
                }
            }
        })
    }

    private val textAreas: MutableMap<String, RSyntaxTextArea> = mutableMapOf()

    // Store variable values for rendering
    private val variableValues: MutableMap<String, LuaValue> = mutableMapOf()

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
                add(toolbar(), BorderLayout.PAGE_START)
                add(JScrollPane(table), BorderLayout.CENTER)
            }.apply {
                preferredSize = Dimension(200, 600)
            },
        )

        io.launch {
            val scriptsContent =
                gameParameters.getAllScripts()
                    .map { it to File(it).readText() }

            SwingUtilities.invokeLater {
                val breakpointIcon =
                    ImageIO.read(TinyDebuggerUI::class.java.getResource("/icons/flag_square.png"))
                        .let { recolorImage(it, LIGHT_RED) }
                        .getScaledInstance(16, 16, 0)
                        .let { ImageIcon(it) }

                scriptsContent.forEach { (scriptName, scriptContent) ->
                    addScriptTab(scriptName, scriptContent, breakpointIcon)
                }

                // Request current breakpoints from the game engine
                io.launch {
                    debugCommandSender.send(RequestBreakpoints)
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
                            variableValues.clear()

                            command.locals.forEach { (name, value) ->
                                addValueToTable(name, value)
                            }
                            command.upValues.forEach { (name, value) ->
                                addValueToTable(name, value)
                            }
                        }
                    }

                    is CurrentBreakpoints -> {
                        SwingUtilities.invokeLater {
                            // Clear existing breakpoints in the UI
                            textAreas.values.forEach { textArea ->
                                val gutter = (textArea.parent.parent as? RTextScrollPane)?.gutter
                                gutter?.removeAllTrackingIcons()
                            }

                            // Add received breakpoints to the UI
                            command.breakpoints.forEach { breakpointInfo ->
                                val textArea = textAreas[breakpointInfo.script]
                                if (textArea != null && breakpointInfo.enabled) {
                                    val gutter = (textArea.parent.parent as? RTextScrollPane)?.gutter
                                    gutter?.toggleBookmark(breakpointInfo.line - 1)
                                }
                            }
                        }
                    }

                    is Reload ->
                        SwingUtilities.invokeLater {
                            val textArea = textAreas[command.script]!!
                            textArea.text = File(command.script).readText()
                        }
                }
            }
        }
    }

    private fun toolbar(): Component {
        val iconDisconnect =
            ImageIO.read(TinyDebuggerUI::class.java.getResource("/icons/character_remove.png"))
                .let { recolorImage(it, LIGHT_GREY) }
                .getScaledInstance(24, 24, 0)
                .let { ImageIcon(it) }

        val iconResume =
            ImageIO.read(TinyDebuggerUI::class.java.getResource("/icons/pawn_right.png"))
                .let { recolorImage(it, LIGHT_GREY) }
                .getScaledInstance(24, 24, 0)
                .let { ImageIcon(it) }

        val iconStep =
            ImageIO.read(TinyDebuggerUI::class.java.getResource("/icons/pawn_skip.png"))
                .let { recolorImage(it, LIGHT_GREY) }
                .getScaledInstance(24, 24, 0)
                .let { ImageIcon(it) }

        return JPanel().apply {
            contentPane.layout = BoxLayout(contentPane, BoxLayout.X_AXIS)
            add(
                JButton(iconDisconnect).apply {
                    toolTipText = "Disconnect from the game"
                    preferredSize = Dimension(32, 32)
                    addActionListener {
                        io.launch {
                            debugCommandSender.send(Disconnect)
                            textAreas.values.forEach {
                                it.highlighter.removeAllHighlights()
                            }
                        }
                    }
                },
            )
            add(
                JButton(iconResume).apply {
                    toolTipText = "Resume execution until the next breakpoint"
                    preferredSize = Dimension(32, 32)
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
                JButton(iconStep).apply {
                    toolTipText = "Step over the current line"
                    preferredSize = Dimension(32, 32)
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

    private fun RSyntaxTextArea.highlightLine(
        lineNumber: Int,
        color: Color,
    ) {
        try {
            // Convert line number to offset
            val start = this.getLineStartOffset(lineNumber - 1)
            val end = this.getLineEndOffset(lineNumber - 1)

            // Highlight the line
            this.highlighter.addHighlight(start, end, DefaultHighlighter.DefaultHighlightPainter(color))

            // Set the focus on that line
            this.caretPosition = start
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addScriptTab(
        scriptName: String,
        scriptContent: String,
        bookmarkIcon: Icon,
    ) {
        val textArea =
            RSyntaxTextArea(20, 60).apply {
                syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_LUA
                isCodeFoldingEnabled = true
                isEditable = false
                text = scriptContent
                highlightCurrentLine = false
            }
        textAreas[scriptName] = textArea

        val scrollPane = RTextScrollPane(textArea)
        scrollPane.gutter.bookmarkIcon = bookmarkIcon
        scrollPane.gutter.isBookmarkingEnabled = true
        scrollPane.gutter.addIconRowListener(GutterListener(scriptName))
        scrollPane.gutter.addLineNumberListener(LineNumberListener(scrollPane))

        val panel =
            JPanel(BorderLayout()).apply {
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
        private fun viewToModelLine(
            textArea: RTextArea,
            p: Point,
        ): Int {
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

    /**
     * Creates a new BufferedImage by replacing pixels of a specific color (like white)
     * in the source image with a target color, while preserving transparency.
     *
     * @param sourceImage The original BufferedImage.
     * @param targetColor The Color object representing the new color.
     * @return A new BufferedImage with the specified color replaced.
     */
    private fun recolorImage(
        sourceImage: BufferedImage,
        targetColor: Color,
    ): BufferedImage {
        if (sourceImage.width <= 0 || sourceImage.height <= 0) {
            throw IllegalArgumentException("Image size is invalid")
        }

        val width = sourceImage.width
        val height = sourceImage.height

        // Create a new image with the same dimensions and support for transparency (ARGB)
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // Iterate through each pixel
        for (y in 0 until height) {
            for (x in 0 until width) {
                val originalPixelARGB = sourceImage.getRGB(x, y) // Get pixel data including alpha
                val originalAlpha = (originalPixelARGB shr 24) and 0xFF

                // Check if the RGB portion matches the color to replace
                // We compare ignoring the original alpha channel using a mask
                if (originalAlpha == 0x00) {
                    newImage.setRGB(x, y, originalPixelARGB)
                } else {
                    // Create new pixel value: (alpha << 24) | (red << 16) | (green << 8) | blue
                    val newPixelARGB =
                        (originalAlpha shl 24) or
                                (targetColor.red shl 16) or
                                (targetColor.green shl 8) or
                                targetColor.blue
                    newImage.setRGB(x, y, newPixelARGB)
                }
            }
        }

        return newImage
    }

    /**
     * Custom cell renderer that can display either text or a tree structure for dictionaries.
     */
    private inner class VariableCellRenderer : javax.swing.table.DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            if (column == 0) {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            }

            val name = table.getValueAt(row, 0) as String
            val luaValue = variableValues[name]

            return when (luaValue) {
                is LuaValue.Dictionary -> {
                    // Create a panel with a button and label
                    val panel = JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.X_AXIS)
                        background = if (isSelected) table.selectionBackground else table.background
                    }

                    // Add the button with a mouse listener instead of action listener
                    val button = JButton("+").apply {
                        val dimension = Dimension(16, 16)
                        preferredSize = dimension
                        minimumSize = dimension
                        maximumSize = dimension
                        isFocusable = false  // Prevent focus which can interfere with events
                        isRequestFocusEnabled = false
                    }

                    // Add a mouse listener to handle clicks
                    button.addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            showDictionaryDialog(name, luaValue)
                        }
                    })

                    panel.add(button)
                    panel.add(JLabel("Dictionary (${luaValue.entries.size} entries)"))
                    panel.add(Box.createHorizontalGlue())

                    panel
                }

                is LuaValue.Primitive -> {
                    super.getTableCellRendererComponent(table, luaValue.value, isSelected, hasFocus, row, column)
                }

                null -> {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                }
            }
        }
    }

    /**
     * Shows a dialog with a tree view of a dictionary.
     */
    private fun showDictionaryDialog(name: String, dictionary: LuaValue.Dictionary) {
        // Use JDialog instead of JFrame to make it modal
        val dialog = javax.swing.JDialog(this, "Dictionary: $name", true)
        dialog.size = Dimension(400, 300)
        dialog.layout = BorderLayout()
        dialog.defaultCloseOperation = javax.swing.WindowConstants.DISPOSE_ON_CLOSE

        val root = DefaultMutableTreeNode(name)
        populateTreeNode(root, dictionary)

        val tree = JTree(DefaultTreeModel(root))
        tree.isRootVisible = true
        tree.showsRootHandles = true

        // Add a close button at the bottom
        val closeButton = JButton("Close")
        closeButton.addActionListener { dialog.dispose() }

        val buttonPanel = JPanel()
        buttonPanel.add(closeButton)

        dialog.add(JScrollPane(tree), BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)

        // Center the dialog on the parent window
        dialog.setLocationRelativeTo(this)

        // Make the dialog visible
        dialog.isVisible = true
    }

    /**
     * Recursively populates a tree node with the entries from a dictionary.
     */
    private fun populateTreeNode(node: DefaultMutableTreeNode, dictionary: LuaValue.Dictionary) {
        dictionary.entries.forEach { (key, value) ->
            when (value) {
                is LuaValue.Primitive -> {
                    val childNode = DefaultMutableTreeNode("$key: ${value.value}")
                    node.add(childNode)
                }

                is LuaValue.Dictionary -> {
                    val childNode = DefaultMutableTreeNode(key)
                    node.add(childNode)
                    populateTreeNode(childNode, value)
                }
            }
        }
    }

    /**
     * Adds a value to the table, handling both primitive values and dictionaries.
     */
    private fun addValueToTable(name: String, value: LuaValue) {
        variableValues[name] = value
        tableModel.addRow(arrayOf(name, ""))
    }

    companion object {
        private val LIGHT_RED = Color(255, 102, 102, 100)
        private val LIGHT_GREY = Color(151, 151, 151, 100)
    }
}
