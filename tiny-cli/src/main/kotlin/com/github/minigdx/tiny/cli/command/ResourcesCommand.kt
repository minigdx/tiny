package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.minigdx.tiny.cli.config.GameParameters
import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.exception.MissingTinyConfigurationException
import java.io.File

class ResourcesCommand : CliktCommand(name = "resources") {

    private val gameDirectory by argument(help = "The directory containing all game information.")
        .file(mustExist = true, canBeDir = true, canBeFile = false)
        .default(File("."))

    private val dryRun by option("--dry-run", help = "Simulate deletions without modifying the file")
        .flag(default = false)

    private val categoryFilter by option("--category", help = "Filter categories by name or pattern")

    override fun help(context: Context) = "Inspect and manage categorized resources in _tiny.json"

    override fun run() {
        val tiny = gameDirectory.resolve("_tiny.json")
        if (!tiny.exists()) {
            throw MissingTinyConfigurationException(tiny)
        }

        try {
            val gameParameters = GameParameters.read(tiny)

            if (gameParameters !is GameParametersV1) {
                echo("❌ This command only supports GameParametersV1 format")
                return
            }

            showResourceManager(gameParameters, tiny)
        } catch (e: Exception) {
            echo("❌ Error reading _tiny.json: ${e.message}")
        }
    }

    private fun showResourceManager(gameParameters: GameParametersV1, configFile: File) {
        var currentParameters = gameParameters

        while (true) {
            val categories = getResourceCategories(currentParameters)

            // Apply category filter if specified
            val filteredCategories = if (categoryFilter != null) {
                categories.filter { it.first.contains(categoryFilter!!, ignoreCase = true) }
            } else {
                categories
            }

            if (filteredCategories.isEmpty()) {
                echo("No categories found${if (categoryFilter != null) " matching filter '$categoryFilter'" else ""}")
                return
            }

            echo("\n🎮 Resource Manager - Categories:")
            echo("═".repeat(40))

            filteredCategories.forEachIndexed { index, (categoryName, resources) ->
                echo("${index + 1}. $categoryName (${resources.size} items)")
            }

            echo("\nCommands:")
            echo("• Enter category number to manage resources")
            echo("• 'q' to quit")
            echo("• 'r' to refresh")

            print("\n> ")
            val input = readLine()?.trim()

            when {
                input == null -> {
                    echo("👋 Goodbye!")
                    break
                }
                input.equals("q", ignoreCase = true) -> {
                    echo("👋 Goodbye!")
                    break
                }
                input.equals("r", ignoreCase = true) -> {
                    // Refresh by re-reading the file
                    try {
                        currentParameters = GameParameters.read(configFile) as GameParametersV1
                        echo("🔄 Refreshed!")
                    } catch (e: Exception) {
                        echo("❌ Error refreshing: ${e.message}")
                    }
                    continue
                }
                input.toIntOrNull() != null -> {
                    val categoryIndex = input.toInt() - 1
                    if (categoryIndex in filteredCategories.indices) {
                        val (categoryName, resources) = filteredCategories[categoryIndex]
                        val updatedParameters = manageCategoryResources(
                            currentParameters, 
                            categoryName, 
                            resources, 
                            configFile
                        )
                        if (updatedParameters != null) {
                            currentParameters = updatedParameters
                        }
                    } else {
                        echo("❌ Invalid category number")
                    }
                }
                else -> {
                    echo("❌ Invalid input")
                }
            }
        }
    }

    private fun manageCategoryResources(
        gameParameters: GameParametersV1,
        categoryName: String,
        resources: List<String>,
        configFile: File
    ): GameParametersV1? {
        var currentParameters = gameParameters

        while (true) {
            val currentResources = getCurrentCategoryResources(currentParameters, categoryName)

            if (currentResources.isEmpty()) {
                echo("\n📁 Category '$categoryName' is empty")
                echo("Press Enter to go back...")
                readLine()
                return currentParameters
            }

            echo("\n📁 Category: $categoryName")
            echo("═".repeat(40))

            currentResources.forEachIndexed { index, resource ->
                echo("${index + 1}. $resource")
            }

            echo("\nCommands:")
            echo("• Enter resource number to delete")
            echo("• 'b' to go back")
            echo("• 'r' to refresh")

            print("\n> ")
            val input = readLine()?.trim()

            when {
                input == null -> {
                    return currentParameters
                }
                input.equals("b", ignoreCase = true) -> {
                    return currentParameters
                }
                input.equals("r", ignoreCase = true) -> {
                    // Refresh current parameters
                    try {
                        currentParameters = GameParameters.read(configFile) as GameParametersV1
                        echo("🔄 Refreshed!")
                    } catch (e: Exception) {
                        echo("❌ Error refreshing: ${e.message}")
                    }
                    continue
                }
                input.toIntOrNull() != null -> {
                    val resourceIndex = input.toInt() - 1
                    if (resourceIndex in currentResources.indices) {
                        val resourceToDelete = currentResources[resourceIndex]

                        echo("\n⚠️  Are you sure you want to remove '$resourceToDelete' from $categoryName?")
                        echo("This will only remove the entry from _tiny.json, not delete the file.")
                        print("Type 'yes' to confirm: ")

                        val confirmation = readLine()?.trim()
                        if (confirmation != null && confirmation.equals("yes", ignoreCase = true)) {
                            val updatedParameters = removeResourceFromCategory(
                                currentParameters, 
                                categoryName, 
                                resourceToDelete
                            )

                            if (dryRun) {
                                echo("🔍 [DRY RUN] Would remove '$resourceToDelete' from $categoryName")
                            } else {
                                try {
                                    updatedParameters.write(configFile)
                                    echo("✅ Removed '$resourceToDelete' from $categoryName")
                                    currentParameters = updatedParameters
                                } catch (e: Exception) {
                                    echo("❌ Error saving file: ${e.message}")
                                }
                            }
                        } else {
                            echo("❌ Cancelled")
                        }
                    } else {
                        echo("❌ Invalid resource number")
                    }
                }
                else -> {
                    echo("❌ Invalid input")
                }
            }
        }
    }

    private fun getResourceCategories(gameParameters: GameParametersV1): List<Pair<String, List<String>>> {
        return listOf(
            "scripts" to gameParameters.scripts,
            "spritesheets" to gameParameters.spritesheets,
            "levels" to gameParameters.levels,
            "sounds" to gameParameters.sounds,
            "libraries" to gameParameters.libraries
        ).filter { it.second.isNotEmpty() }
    }

    private fun getCurrentCategoryResources(gameParameters: GameParametersV1, categoryName: String): List<String> {
        return when (categoryName) {
            "scripts" -> gameParameters.scripts
            "spritesheets" -> gameParameters.spritesheets
            "levels" -> gameParameters.levels
            "sounds" -> gameParameters.sounds
            "libraries" -> gameParameters.libraries
            else -> emptyList()
        }
    }

    private fun removeResourceFromCategory(
        gameParameters: GameParametersV1,
        categoryName: String,
        resource: String
    ): GameParametersV1 {
        return when (categoryName) {
            "scripts" -> gameParameters.copy(scripts = gameParameters.scripts - resource)
            "spritesheets" -> gameParameters.copy(spritesheets = gameParameters.spritesheets - resource)
            "levels" -> gameParameters.copy(levels = gameParameters.levels - resource)
            "sounds" -> gameParameters.copy(sounds = gameParameters.sounds - resource)
            "libraries" -> gameParameters.copy(libraries = gameParameters.libraries - resource)
            else -> gameParameters
        }
    }
}
