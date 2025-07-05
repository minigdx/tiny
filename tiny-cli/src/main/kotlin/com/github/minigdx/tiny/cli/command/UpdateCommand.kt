package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.table
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import java.io.File

class UpdateCommand : CliktCommand(name = "update") {
    val gameDirectory by argument(help = "The directory containing your game to be updated.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    override fun help(context: Context) = "Interactively view and update game parameters."

    private var currentParameters: GameParametersV1? = null
    private var selectedIndex = 0
    private val editableParameters = mutableListOf<EditableParameter>()

    override fun run() {
        val configFile = gameDirectory.resolve("_tiny.json")
        if (!configFile.exists()) {
            echo("❌ No _tiny.json found in ${gameDirectory.absolutePath}! Can't update parameters without it.")
            throw Abort()
        }

        try {
            val gameParameters = GameParameters.read(configFile)
            if (gameParameters !is GameParametersV1) {
                echo("❌ Only GameParametersV1 is supported for updates.")
                throw Abort()
            }

            currentParameters = gameParameters
            setupEditableParameters()
            runInteractiveLoop(configFile)
        } catch (e: Exception) {
            echo("❌ Error reading _tiny.json: ${e.message}")
            throw Abort()
        }
    }

    private fun setupEditableParameters() {
        val params = currentParameters ?: return
        editableParameters.clear()

        // Add basic parameters
        editableParameters.add(EditableParameter("name", params.name, false))
        editableParameters.add(
            EditableParameter(
                "resolution",
                "${params.resolution.width}x${params.resolution.height}",
                false,
            ),
        )
        editableParameters.add(EditableParameter("sprites", "${params.sprites.width}x${params.sprites.height}", false))
        editableParameters.add(EditableParameter("zoom", params.zoom.toString(), false))
        editableParameters.add(EditableParameter("palette", formatPaletteDisplay(params.colors), false))
        editableParameters.add(EditableParameter("scripts", params.scripts.joinToString(", "), false))
        editableParameters.add(EditableParameter("spritesheets", params.spritesheets.joinToString(", "), false))
        editableParameters.add(EditableParameter("levels", params.levels.joinToString(", "), false))
        editableParameters.add(EditableParameter("sounds", params.sounds.joinToString(", "), false))
        editableParameters.add(EditableParameter("libraries", params.libraries.joinToString(", "), false))
        editableParameters.add(EditableParameter("hideMouseCursor", if (params.hideMouseCursor) "Yes" else "No", true))
    }

    private fun formatPaletteDisplay(colors: List<String>): String {
        if (colors.isEmpty()) return "No colors"

        val colorSquares = colors.take(16).joinToString("") { hexColor ->
            val colorWithoutHash = hexColor.removePrefix("#")
            val r = colorWithoutHash.substring(0, 2).toInt(16)
            val g = colorWithoutHash.substring(2, 4).toInt(16)
            val b = colorWithoutHash.substring(4, 6).toInt(16)

            TextColors.rgb(r / 255.0, g / 255.0, b / 255.0)("◼")
        }

        val extraCount = colors.size - 16
        return if (extraCount > 0) {
            "$colorSquares +$extraCount"
        } else {
            colorSquares
        }
    }

    private fun runInteractiveLoop(configFile: File) {
        echo("🎮 Interactive Game Parameter Editor")
        echo("Use ↑/↓ arrow keys to navigate, Enter to toggle values, 'q' to quit and save")
        echo()

        displayParameters()
        currentContext.terminal.receiveKeyEvents { event ->
            val next = when (event.key) {
                "ArrowUp" -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    true
                }

                "ArrowDown" -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(editableParameters.lastIndex)
                    true
                }

                "Enter" -> {
                    toggleParameter()
                    true
                }

                "q" -> {
                    saveAndExit(configFile)
                    false
                }
                else -> true
            }
            if (next) {
                displayParameters()
                InputReceiver.Status.Continue
            } else {
                InputReceiver.Status.Finished
            }
        }
    }

    private fun displayParameters() {
        currentContext.terminal.cursor.move {
            clearScreen()
        }

        echo("🎮 Game Parameters for: ${currentParameters?.name}")
        echo()

        val table = table {
            header {
                row("Parameter", "Value", "Editable")
            }
            body {
                editableParameters.forEachIndexed { index, param ->
                    val isSelected = index == selectedIndex
                    val paramName = if (isSelected) TextStyles.bold(param.name) else param.name
                    val paramValue = if (isSelected) TextStyles.bold(param.value) else param.value
                    val editable = if (param.isEditable) "✓" else "✗"

                    row(paramName, paramValue, editable)
                }
            }
        }

        echo(table)
        echo()
        echo("Selected: ${editableParameters[selectedIndex].name}")
        if (editableParameters[selectedIndex].isEditable) {
            echo("Press Enter to toggle this value")
        }
        echo("Press 'q' to quit and save changes")
    }

    private fun toggleParameter() {
        val param = editableParameters[selectedIndex]
        if (!param.isEditable) {
            echo("⚠️  This parameter is not editable")
            return
        }

        when (param.name) {
            "hideMouseCursor" -> {
                val currentParams = currentParameters ?: return
                val newValue = !currentParams.hideMouseCursor
                currentParameters = currentParams.copy(hideMouseCursor = newValue)
                param.value = if (newValue) "Yes" else "No"
                echo("✅ ${param.name} toggled to: ${param.value}")
            }
        }
    }

    private fun saveAndExit(configFile: File) {
        try {
            currentParameters?.write(configFile)
            echo("✅ Parameters saved successfully!")
        } catch (e: Exception) {
            echo("❌ Error saving parameters: ${e.message}")
        }
    }

    private data class EditableParameter(
        val name: String,
        var value: String,
        val isEditable: Boolean,
    )
}
