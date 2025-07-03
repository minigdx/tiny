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
import javax.swing.JOptionPane
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
        override fun isCellEditable(
            row: Int,
            column: Int,
        ): Boolean {
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

                if (row >= 0 && column == 1) { // Only for the value column
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

    // Store breakpoint conditions for each script and line
    private val breakpointConditions: MutableMap<Pair<String, Int>, String> = mutableMapOf()

    // Store active breakpoints for each script and line
    private val activeBreakpoints: MutableSet<Pair<String, Int>> = mutableSetOf()

    // Track condition error states to prevent redundant updates
    private val breakpointConditionErrors: MutableMap<Pair<String, Int>, String?> = mutableMapOf()

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

                            // Update visual indicator with condition error information
                            updateBreakpointVisualIndicator(
                                command.script,
                                command.line,
                                conditionError = command.conditionError,
                            )
                        }
                    }

                    is CurrentBreakpoints -> {
                        SwingUtilities.invokeLater {
                            // Clear existing breakpoints in the UI
                            textAreas.values.forEach { textArea ->
                                val gutter = (textArea.parent.parent as? RTextScrollPane)?.gutter
                                gutter?.removeAllTrackingIcons()
                            }

                            // Clear our tracking data structures
                            activeBreakpoints.clear()
                            breakpointConditions.clear()
                            breakpointConditionErrors.clear()

                            // Add received breakpoints to the UI
                            command.breakpoints.forEach { breakpointInfo ->
                                val textArea = textAreas[breakpointInfo.script]
                                if (textArea != null && breakpointInfo.enabled) {
                                    val gutter = (textArea.parent.parent as? RTextScrollPane)?.gutter
                                    gutter?.toggleBookmark(breakpointInfo.line - 1)

                                    // Restore our tracking data
                                    val breakpointKey = Pair(breakpointInfo.script, breakpointInfo.line)
                                    activeBreakpoints.add(breakpointKey)

                                    // Restore the condition if it exists
                                    breakpointInfo.condition?.let { condition ->
                                        breakpointConditions[breakpointKey] = condition
                                    }

                                    // Restore visual indicator for conditional breakpoints
                                    updateBreakpointVisualIndicator(breakpointInfo.script, breakpointInfo.line)
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
        scrollPane.gutter.addLineNumberListener(LineNumberListener(scriptName, scrollPane))

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

    inner class LineNumberListener(
        private val scriptName: String,
        private val scrollPane: RTextScrollPane,
    ) : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val l = viewToModelLine(scrollPane.textArea, e.point)
            scrollPane.gutter.toggleBookmark(l)

            if (e.button == MouseEvent.BUTTON3) { // Right click
                if (l >= 0) {
                    val breakpointKey = Pair(scriptName, l + 1)
                    if (activeBreakpoints.contains(breakpointKey)) {
                        // Right-clicked on an existing breakpoint
                        showConditionDialog(scriptName, l + 1)
                    }
                }
            }
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
            val line = e.line + 1
            val breakpointKey = Pair(scriptName, line)
            activeBreakpoints.add(breakpointKey)

            val condition = breakpointConditions[breakpointKey]
            io.launch {
                debugCommandSender.send(ToggleBreakpoint(scriptName, line, true, condition))
            }
        }

        override fun bookmarkRemoved(e: IconRowEvent) {
            val line = e.line + 1
            val breakpointKey = Pair(scriptName, line)
            activeBreakpoints.remove(breakpointKey)
            breakpointConditions.remove(breakpointKey)

            io.launch {
                debugCommandSender.send(ToggleBreakpoint(scriptName, line, false))
            }
        }
    }

    /**
     * Shows a dialog to input/edit a condition for a breakpoint.
     */
    private fun showConditionDialog(
        scriptName: String,
        line: Int,
    ) {
        val currentCondition = breakpointConditions[Pair(scriptName, line)] ?: ""

        val condition = JOptionPane.showInputDialog(
            this,
            "Enter Lua condition for breakpoint at line $line:\n(Leave empty to remove condition)",
            "Conditional Breakpoint",
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            currentCondition,
        ) as String?

        if (condition != null) {
            if (condition.trim().isEmpty()) {
                // Remove condition
                breakpointConditions.remove(Pair(scriptName, line))
                io.launch {
                    debugCommandSender.send(ToggleBreakpoint(scriptName, line, true, null))
                }
            } else {
                // Set/update condition
                breakpointConditions[Pair(scriptName, line)] = condition.trim()
                io.launch {
                    debugCommandSender.send(ToggleBreakpoint(scriptName, line, true, condition.trim()))
                }
            }

            // Update visual indicator
            updateBreakpointVisualIndicator(scriptName, line)
        }
    }

    /**
     * Updates the visual indicator for a breakpoint based on whether it has a condition.
     */
    private fun updateBreakpointVisualIndicator(
        scriptName: String,
        line: Int,
        condition: String? = null,
        conditionError: String? = null,
    ) {
        val textArea = textAreas[scriptName] ?: return
        val breakpointKey = Pair(scriptName, line)
        val storedCondition = condition ?: breakpointConditions[breakpointKey]

        // Check if the condition error has changed to avoid redundant updates
        val previousConditionError = breakpointConditionErrors[breakpointKey]
        if (conditionError == previousConditionError && condition == null) {
            // No change in condition error state, skip update
            return
        }

        // Update the stored condition error state
        breakpointConditionErrors[breakpointKey] = conditionError

        // Remove any existing condition comments from this line
        removeConditionComment(textArea, line)

        // Remove any existing background highlighting for condition errors
        removeConditionErrorHighlight(textArea, line)

        if (storedCondition != null) {
            if (conditionError != null) {
                // Condition evaluation failed - use boom emoji and highlight background
                addConditionComment(textArea, line, "💥 $storedCondition (error: $conditionError)")
                highlightConditionError(textArea, line)
            } else {
                // Condition is valid - use bug emoji
                addConditionComment(textArea, line, "🐛 $storedCondition")
            }
        }
    }

    /**
     * Adds a Lua comment with emoji and condition to the end of the specified line.
     */
    private fun addConditionComment(
        textArea: RSyntaxTextArea,
        line: Int,
        comment: String,
    ) {
        try {
            val lineIndex = line - 1 // Convert to 0-based index
            if (lineIndex < 0 || lineIndex >= textArea.lineCount) return

            val lineStart = textArea.getLineStartOffset(lineIndex)
            val lineEnd = textArea.getLineEndOffset(lineIndex)
            val lineText = textArea.getText(lineStart, lineEnd - lineStart)

            // Check if line already has our condition comment
            if (lineText.contains("-- 🐛") || lineText.contains("-- 💥")) {
                return // Comment already exists
            }

            // Remove trailing newline if present
            val cleanLineText = lineText.trimEnd('\n', '\r')
            val newLineText = "$cleanLineText -- $comment\n"

            textArea.replaceRange(newLineText, lineStart, lineEnd)
        } catch (e: BadLocationException) {
            // Ignore if line doesn't exist
        }
    }

    /**
     * Removes condition comments from the specified line.
     */
    private fun removeConditionComment(
        textArea: RSyntaxTextArea,
        line: Int,
    ) {
        try {
            val lineIndex = line - 1 // Convert to 0-based index
            if (lineIndex < 0 || lineIndex >= textArea.lineCount) return

            val lineStart = textArea.getLineStartOffset(lineIndex)
            val lineEnd = textArea.getLineEndOffset(lineIndex)
            val lineText = textArea.getText(lineStart, lineEnd - lineStart)

            // Remove condition comments (both bug and boom emojis)
            val cleanedText = lineText
                .replace(Regex("\\s*-- 🐛[^\\n\\r]*"), "")
                .replace(Regex("\\s*-- 💥[^\\n\\r]*"), "")

            if (cleanedText != lineText) {
                textArea.replaceRange(cleanedText, lineStart, lineEnd)
            }
        } catch (e: BadLocationException) {
            // Ignore if line doesn't exist
        }
    }

    /**
     * Highlights the line with light yellow background for condition errors.
     */
    private fun highlightConditionError(
        textArea: RSyntaxTextArea,
        line: Int,
    ) {
        try {
            val lineIndex = line - 1 // Convert to 0-based index
            if (lineIndex < 0 || lineIndex >= textArea.lineCount) return

            val lineStart = textArea.getLineStartOffset(lineIndex)
            val lineEnd = textArea.getLineEndOffset(lineIndex)

            val highlighter = textArea.highlighter
            val lightYellow = Color(255, 255, 224) // Light yellow background
            highlighter.addHighlight(lineStart, lineEnd - 1, DefaultHighlighter.DefaultHighlightPainter(lightYellow))
        } catch (e: BadLocationException) {
            // Ignore if line doesn't exist
        }
    }

    /**
     * Removes condition error highlighting from the specified line.
     */
    private fun removeConditionErrorHighlight(
        textArea: RSyntaxTextArea,
        line: Int,
    ) {
        try {
            val lineIndex = line - 1 // Convert to 0-based index
            if (lineIndex < 0 || lineIndex >= textArea.lineCount) return

            val lineStart = textArea.getLineStartOffset(lineIndex)
            val lineEnd = textArea.getLineEndOffset(lineIndex)

            val highlighter = textArea.highlighter
            val highlights = highlighter.highlights

            // Remove highlights that match our line range and are light yellow
            highlights.forEach { highlight ->
                if (highlight.startOffset >= lineStart && highlight.endOffset <= lineEnd) {
                    val painter = highlight.painter
                    if (painter is DefaultHighlighter.DefaultHighlightPainter) {
                        // Check if it's our light yellow highlight
                        val lightYellow = Color(255, 255, 224)
                        try {
                            highlighter.removeHighlight(highlight)
                        } catch (e: Exception) {
                            // Ignore removal errors
                        }
                    }
                }
            }
        } catch (e: BadLocationException) {
            // Ignore if line doesn't exist
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
                        isFocusable = false // Prevent focus which can interfere with events
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
    private fun showDictionaryDialog(
        name: String,
        dictionary: LuaValue.Dictionary,
    ) {
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
    private fun populateTreeNode(
        node: DefaultMutableTreeNode,
        dictionary: LuaValue.Dictionary,
    ) {
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
    private fun addValueToTable(
        name: String,
        value: LuaValue,
    ) {
        variableValues[name] = value
        tableModel.addRow(arrayOf(name, ""))
    }

    companion object {
        private val LIGHT_RED = Color(255, 102, 102, 100)
        private val LIGHT_GREY = Color(151, 151, 151, 100)
        private val LIGHT_BLUE = Color(102, 153, 255, 100) // For conditional breakpoints
        private val LIGHT_ORANGE = Color(255, 165, 0, 100) // For condition errors
    }
}
