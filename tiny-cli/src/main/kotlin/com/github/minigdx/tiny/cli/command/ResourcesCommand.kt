package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.table
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class ResourcesCommand : CliktCommand(name = "resources") {
    private val gameDirectory by argument(help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val categoryFilter by option("--category", help = "Filter categories by name or pattern")

    private var selectedIndex = 0

    override fun help(context: Context) = "Inspect and manage categorized resources in the game."

    override fun run() {
        val tiny = gameDirectory.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }

        try {
            var gameParameters = GameParameters.read(tiny) as GameParametersV1

            showResourceManager(gameParameters)
            currentContext.terminal.receiveKeyEvents { event ->
                val next = when (event.key) {
                    "ArrowUp" -> {
                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        true
                    }

                    "ArrowDown" -> {
                        val maximumValue = getResourceCategories(gameParameters as GameParametersV1).map { it.value.size }.sum() - 1
                        selectedIndex = (selectedIndex + 1).coerceAtMost(maximumValue)
                        true
                    }

                    "d" -> {
                        gameParameters = deleteSelected(selectedIndex, gameParameters as GameParametersV1)
                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        true
                    }

                    "q" -> {
                        save(gameParameters)
                        false
                    }

                    else -> true
                }
                if (next) {
                    showResourceManager(gameParameters)
                    InputReceiver.Status.Continue
                } else {
                    InputReceiver.Status.Finished
                }
            }
        } catch (e: Exception) {
            echo("❌ Error reading _tiny.json: ${e.message}")
        }
    }

    private fun save(configuration: GameParameters) {
        val configurationFile = gameDirectory.resolve("_tiny.json")
        configuration.write(configurationFile)
    }

    private fun deleteSelected(
        selectedIndex: Int,
        parameters: GameParametersV1,
    ): GameParametersV1 {
        val categories = getResourceCategories(parameters)

        // Flatten all resources while keeping track of their category and index within that category
        val flattenedResources = mutableListOf<Triple<String, String, Int>>() // (categoryName, resourcePath, indexInCategory)
        categories.forEach { (categoryName, resources) ->
            resources.forEachIndexed { indexInCategory, resourcePath ->
                flattenedResources.add(Triple(categoryName, resourcePath, indexInCategory))
            }
        }

        // Validate selectedIndex
        if (selectedIndex < 0 || selectedIndex >= flattenedResources.size) {
            throw IndexOutOfBoundsException("Invalid selectedIndex: $selectedIndex. Valid range: 0-${flattenedResources.size - 1}")
        }

        // Get the resource to delete
        val (categoryName, resourceToDelete, indexInCategory) = flattenedResources[selectedIndex]

        // Create a copy of parameters with the resource removed from the appropriate category
        return when (categoryName) {
            "\uD83D\uDCDD scripts" -> {
                val updatedScripts = parameters.scripts.toMutableList()
                updatedScripts.removeAt(indexInCategory)
                parameters.copy(scripts = updatedScripts)
            }
            "\uD83D\uDDBC\uFE0F spritesheets" -> {
                val updatedSpritesheets = parameters.spritesheets.toMutableList()
                updatedSpritesheets.removeAt(indexInCategory)
                parameters.copy(spritesheets = updatedSpritesheets)
            }
            "\uD83D\uDDFA\uFE0F levels" -> {
                val updatedLevels = parameters.levels.toMutableList()
                updatedLevels.removeAt(indexInCategory)
                parameters.copy(levels = updatedLevels)
            }
            "\uD83D\uDD08 sounds" -> {
                val updatedSounds = parameters.sounds.toMutableList()
                updatedSounds.removeAt(indexInCategory)
                parameters.copy(sounds = updatedSounds)
            }
            else -> throw IllegalStateException("Unknown category: $categoryName")
        }
    }

    private fun showResourceManager(gameParameters: GameParametersV1) {
        val categories = getResourceCategories(gameParameters)

        if (categories.isEmpty()) {
            echo("No categories found${if (categoryFilter != null) " matching filter '$categoryFilter'" else ""}")
            return
        }

        val table = table {
            header {
                row("Category", "Resource")
            }
            body {
                var index = 0
                categories.forEach { (categorie, resources) ->
                    resources.forEachIndexed { rindex, resource ->
                        row {
                            val isSelected = selectedIndex == index
                            cell(if (isSelected) TextStyles.bold(categorie) else categorie)
                            val lineContent = "$rindex $resource"
                            val content = if (isSelected) {
                                TextStyles.bold(lineContent)
                            } else {
                                lineContent
                            }
                            cell(content) {
                                columnSpan = 4
                            }

                            index++
                        }
                    }
                }
            }
        }

        currentContext.terminal.cursor.move {
            clearScreen()
        }
        echo(table)
        echo("\nCommands:")
        echo("• Enter category number to manage resources")
        echo("• 'q' to quit")
        echo("• 'd' to delete from the game")
    }

    private fun getResourceCategories(gameParameters: GameParametersV1): Map<String, List<String>> {
        return listOf(
            "\uD83D\uDCDD scripts" to gameParameters.scripts,
            "\uD83D\uDDBC\uFE0F spritesheets" to gameParameters.spritesheets,
            "\uD83D\uDDFA\uFE0F levels" to gameParameters.levels,
            "\uD83D\uDD08 sounds" to gameParameters.sounds,
        ).filter { it.second.isNotEmpty() }
            .toMap()
    }
}
