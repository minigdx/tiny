import com.github.minigdx.tiny.cli.config.GameParametersV1
import com.github.minigdx.tiny.cli.config.Size

fun main() {
    // Create a test GameParametersV1 instance with some resources
    val testParams = GameParametersV1(
        name = "Test Game",
        resolution = Size(320, 240),
        sprites = Size(16, 16),
        zoom = 2,
        colors = listOf("#000000", "#FFFFFF"),
        scripts = listOf("script1.lua", "script2.lua"),
        spritesheets = listOf("sprites1.png", "sprites2.png"),
        levels = listOf("level1.json"),
        sounds = listOf("sound1.mid", "sound2.mid")
    )
    
    println("Original GameParametersV1:")
    println("Scripts: ${testParams.scripts}")
    println("Spritesheets: ${testParams.spritesheets}")
    println("Levels: ${testParams.levels}")
    println("Sounds: ${testParams.sounds}")
    
    // Test the getResourceCategories function to see the flattened structure
    val categories = getResourceCategories(testParams)
    println("\nResource categories:")
    var globalIndex = 0
    categories.forEach { (category, resources) ->
        println("$category:")
        resources.forEachIndexed { index, resource ->
            println("  [$globalIndex] $index: $resource")
            globalIndex++
        }
    }
    
    // Test deleting different resources
    println("\n--- Testing deletion ---")
    
    // Delete first script (index 0)
    val afterDeleteScript = deleteSelected(0, testParams)
    println("After deleting index 0 (first script):")
    println("Scripts: ${afterDeleteScript.scripts}")
    
    // Delete first spritesheet (index 2 in original, index 1 after first deletion)
    val afterDeleteSpritesheet = deleteSelected(1, testParams)
    println("After deleting index 1 (first spritesheet):")
    println("Spritesheets: ${afterDeleteSpritesheet.spritesheets}")
    
    // Delete level (index 4 in original)
    val afterDeleteLevel = deleteSelected(4, testParams)
    println("After deleting index 4 (level):")
    println("Levels: ${afterDeleteLevel.levels}")
    
    // Delete last sound (index 6 in original)
    val afterDeleteSound = deleteSelected(6, testParams)
    println("After deleting index 6 (last sound):")
    println("Sounds: ${afterDeleteSound.sounds}")
}

// Copy the functions from ResourcesCommand for testing
private fun getResourceCategories(gameParameters: GameParametersV1): Map<String, List<String>> {
    return listOf(
        "\uD83D\uDCDD scripts" to gameParameters.scripts,
        "\uD83D\uDDBC\uFE0F spritesheets" to gameParameters.spritesheets,
        "\uD83D\uDDFA\uFE0F levels" to gameParameters.levels,
        "\uD83D\uDD08 sounds" to gameParameters.sounds,
    ).filter { it.second.isNotEmpty() }
        .toMap()
}

private fun deleteSelected(selectedIndex: Int, parameters: GameParametersV1): GameParametersV1 {
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