package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.CliAnnotation
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import java.io.File

class CliToAsciidocKspProcessor(
    val env: SymbolProcessorEnvironment,
) : SymbolProcessor {
    val logger = env.logger

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip last KSP round. Everything should be done in one round
        resolver.getNewFiles().firstOrNull() ?: return emptyList()

        // Find all classes that extend CliktCommand
        val cliktCommandClasses = resolver
            .getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { classDecl ->
                classDecl.superTypes.any { superType ->
                    superType.resolve().declaration.qualifiedName?.asString() == "com.github.ajalt.clikt.core.CliktCommand"
                }
            }
            // Skip commands to be hidden
            .filterNot { classDecl -> classDecl.getAnnotationsByType(CliAnnotation::class).any { it.hidden } }
            .toList()

        if (cliktCommandClasses.isEmpty()) {
            return emptyList()
        }

        val sourceFiles = cliktCommandClasses
            .mapNotNull { it.containingFile }
            .toList()
            .toTypedArray()

        val file = env.codeGenerator.createNewFile(
            Dependencies(true, *sourceFiles),
            "/",
            "tiny-cli-commands",
            "adoc",
        )

        val commands = cliktCommandClasses.map { classDecl ->
            CliCommandDescriptor(
                name = extractCommandName(classDecl),
                className = classDecl.simpleName.asString(),
                description = extractHelpDescription(classDecl),
                arguments = extractArguments(classDecl),
                options = extractOptions(classDecl),
            )
        }.sortedBy { it.name }

        val result = asciidoc {
            title = "Tiny CLI Commands"
            section {
                commands.forEach { command ->
                    lib(command.name ?: command.className) {
                        paragraph(command.description ?: "No description available.")

                        if (command.arguments.isNotEmpty()) {
                            paragraph("*Arguments:*")
                            tableArgs(command.arguments.map { it.name to (it.description ?: "No description") })
                        }

                        if (command.options.isNotEmpty()) {
                            paragraph("*Options:*")
                            tableArgs(command.options.map { it.name to (it.description ?: "No description") })
                        }

                        // Generate usage example
                        val usage = buildString {
                            append("tiny-cli ${command.name ?: command.className.lowercase()}")
                            command.arguments.forEach { arg ->
                                append(" <${arg.name}>")
                            }
                            if (command.options.isNotEmpty()) {
                                append(" [options]")
                            }
                        }

                        code("# Usage\n$usage")
                    }
                }
            }
        }.generate()

        file.write(result.toByteArray())
        return emptyList()
    }

    private fun extractCommandName(classDecl: KSClassDeclaration): String? {
        // Use reflection to extract the command name from the CliktCommand constructor
        // Look for the primary constructor and its arguments
        val primaryConstructor = classDecl.primaryConstructor

        primaryConstructor?.let { constructor ->
            // Look for the supertype call to CliktCommand
            classDecl.superTypes.forEach { superType ->
                val resolved = superType.resolve()
                if (resolved.declaration.qualifiedName?.asString() == "com.github.ajalt.clikt.core.CliktCommand") {
                    // Try to find the name argument in the constructor call
                    superType.element?.typeArguments?.forEach { typeArg ->
                        // This approach might not work directly, let's try a different approach
                    }
                }
            }
        }

        // For now, let's use a simpler approach by examining the class source
        // and looking for constructor parameters in a more structured way
        val classString = classDecl.toString()

        // Look for CliktCommand(name = "commandname") pattern
        val namePattern = """CliktCommand\([^)]*name\s*=\s*"([^"]+)"""".toRegex()
        val nameMatch = namePattern.find(classString)
        if (nameMatch != null) {
            return nameMatch.groupValues[1]
        }

        // Fallback: derive from class name
        val className = classDecl.simpleName.asString()
        return if (className.endsWith("Command")) {
            className.removeSuffix("Command").lowercase()
        } else {
            className.lowercase()
        }
    }

    private fun extractHelpDescription(classDecl: KSClassDeclaration): String? {
        // Use reflection to find the help() method override in this class (not inherited)
        val helpFunction = classDecl.declarations
            .filterIsInstance<KSFunctionDeclaration>()
            .find { func ->
                func.simpleName.asString() == "help" &&
                    func.parentDeclaration == classDecl // Only methods declared in this class
            }

        helpFunction?.let { func ->
            // Get the source file location
            val location = func.location
            if (location is FileLocation) {
                try {
                    val sourceFile = File(location.filePath)
                    if (sourceFile.exists()) {
                        val lines = sourceFile.readLines()

                        // Get the line where the function is declared (KSP uses 1-based line numbers)
                        val functionLineIndex = location.lineNumber - 1

                        if (functionLineIndex >= 0 && functionLineIndex < lines.size) {
                            val functionLine = lines[functionLineIndex]

                            // Look for patterns like: override fun help(context: Context) = "Some description"
                            val singleExpressionPattern = """fun\s+help\s*\([^)]*\)\s*=\s*"([^"]+)"""".toRegex()
                            val match = singleExpressionPattern.find(functionLine)
                            if (match != null) {
                                return match.groupValues[1]
                            }

                            // Look for multi-line function declarations
                            // Check if the function continues on the next line
                            if (functionLine.contains("fun help") && !functionLine.contains("=")) {
                                // Look for the return statement in subsequent lines
                                for (i in (functionLineIndex + 1) until minOf(functionLineIndex + 10, lines.size)) {
                                    val line = lines[i].trim()

                                    // Look for return "..." pattern
                                    val returnPattern = """return\s+"([^"]+)"""".toRegex()
                                    val returnMatch = returnPattern.find(line)
                                    if (returnMatch != null) {
                                        return returnMatch.groupValues[1]
                                    }

                                    // Look for = "..." pattern
                                    val assignmentPattern = """=\s+"([^"]+)"""".toRegex()
                                    val assignmentMatch = assignmentPattern.find(line)
                                    if (assignmentMatch != null) {
                                        return assignmentMatch.groupValues[1]
                                    }

                                    // Stop if we hit a closing brace (end of function)
                                    if (line.contains("}")) {
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Error reading source file for function ${func.simpleName.asString()}: ${e.message}")
                }
            }
        }

        // Fallback: return null to indicate no description found
        return null
    }

    private fun extractArguments(classDecl: KSClassDeclaration): List<CliParameterDescriptor> {
        val arguments = mutableListOf<CliParameterDescriptor>()

        // Use reflection to get all properties declared in this class (not inherited from CliktCommand)
        val allProperties = classDecl.getAllProperties()

        allProperties
            .filter { property ->
                // Only include properties declared in the project classes, not from CliktCommand
                property.parentDeclaration == classDecl
            }
            .forEach { property ->
                val propertyName = property.simpleName.asString()

                // Read the property declaration from source code to capture chained method calls
                val location = property.location
                if (location is FileLocation) {
                    try {
                        val sourceFile = File(location.filePath)
                        if (sourceFile.exists()) {
                            val lines = sourceFile.readLines()
                            val propertyLineIndex = location.lineNumber - 1

                            if (propertyLineIndex >= 0 && propertyLineIndex < lines.size) {
                                // Read the complete property declaration including chained methods
                                val propertyDeclaration = readCompletePropertyDeclaration(lines, propertyLineIndex)

                                if (propertyDeclaration.contains("by argument(")) {
                                    // Extract help text from argument(help = "...") pattern - handle both single and multi-line strings
                                    val helpPattern = """argument\s*\(\s*help\s*=\s*"([^"]+(?:\s*\+\s*"[^"]*")*?)"""".toRegex()
                                    val helpMatch = helpPattern.find(propertyDeclaration)
                                    var helpText = helpMatch?.groupValues?.get(1)

                                    // Clean up multi-line help text by removing string concatenation artifacts
                                    helpText = helpText?.replace("""\s*\+\s*""".toRegex(), " ")?.trim()

                                    // Extract default value from .default(...) pattern - improved to handle nested parentheses
                                    val defaultValue = extractDefaultValue(propertyDeclaration)

                                    // Create enhanced description including default value if present
                                    val enhancedDescription = if (defaultValue != null) {
                                        "${helpText ?: "No description available"} (default: $defaultValue)"
                                    } else {
                                        helpText
                                    }

                                    arguments.add(CliParameterDescriptor(propertyName, enhancedDescription))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error reading source file for property ${property.simpleName.asString()}: ${e.message}")

                        // Fallback to the old method if source reading fails
                        val propertyString = property.toString()
                        if (propertyString.contains("argument(")) {
                            val helpPattern = """argument\([^)]*help\s*=\s*"([^"]+)"""".toRegex()
                            val helpMatch = helpPattern.find(propertyString)
                            val helpText = helpMatch?.groupValues?.get(1)
                            arguments.add(CliParameterDescriptor(propertyName, helpText))
                        }
                    }
                }
            }

        return arguments
    }

    private fun readCompletePropertyDeclaration(
        lines: List<String>,
        startLineIndex: Int,
    ): String {
        val declaration = StringBuilder()
        var currentLineIndex = startLineIndex
        var foundByClause = false

        while (currentLineIndex < lines.size) {
            val line = lines[currentLineIndex].trim()

            // If we're past the first line and hit a new property/method declaration, stop
            if (currentLineIndex > startLineIndex &&
                (
                    line.startsWith("val ") || line.startsWith("var ") ||
                        line.startsWith("private val ") || line.startsWith("private var ") ||
                        line.startsWith("override fun ") || line.startsWith("fun ") ||
                        line.startsWith("class ") || line.startsWith("}")
                )
            ) {
                break
            }

            declaration.append(line).append(" ")

            // Check if this line contains "by argument" or "by option"
            if (line.contains("by argument") || line.contains("by option")) {
                foundByClause = true
            }

            currentLineIndex++

            // After finding the "by" clause, continue reading lines that start with "." (chained methods)
            // or are continuation lines (indented and not starting new declarations)
            if (foundByClause) {
                while (currentLineIndex < lines.size) {
                    val nextLine = lines[currentLineIndex].trim()

                    // Stop if we hit a new property/method declaration
                    if (nextLine.startsWith("val ") || nextLine.startsWith("var ") ||
                        nextLine.startsWith("private val ") || nextLine.startsWith("private var ") ||
                        nextLine.startsWith("override fun ") || nextLine.startsWith("fun ") ||
                        nextLine.startsWith("class ") || nextLine.startsWith("}")
                    ) {
                        break
                    }

                    // Continue if the line starts with "." (chained method) or is empty
                    if (nextLine.startsWith(".") || nextLine.isEmpty()) {
                        declaration.append(nextLine).append(" ")
                        currentLineIndex++
                    } else {
                        // Stop if it's not a chained method and not empty
                        break
                    }
                }
                break
            }
        }

        return declaration.toString().trim()
    }

    private fun extractDefaultValue(propertyDeclaration: String): String? {
        val defaultIndex = propertyDeclaration.indexOf(".default(")
        if (defaultIndex == -1) return null

        val startIndex = defaultIndex + ".default(".length
        var openParentheses = 1
        var currentIndex = startIndex

        while (currentIndex < propertyDeclaration.length && openParentheses > 0) {
            when (propertyDeclaration[currentIndex]) {
                '(' -> openParentheses++
                ')' -> openParentheses--
            }
            currentIndex++
        }

        return if (openParentheses == 0) {
            propertyDeclaration.substring(startIndex, currentIndex - 1).trim()
        } else {
            null
        }
    }

    private fun extractOptions(classDecl: KSClassDeclaration): List<CliParameterDescriptor> {
        val options = mutableListOf<CliParameterDescriptor>()

        // Use reflection to get all properties declared in this class (not inherited from CliktCommand)
        val allProperties = classDecl.getAllProperties()

        allProperties
            .filter { property ->
                // Only include properties declared in the project classes, not from CliktCommand
                property.parentDeclaration == classDecl
            }
            .forEach { property ->
                val propertyName = property.simpleName.asString()

                // Read the property declaration from source code to capture chained method calls
                val location = property.location
                if (location is FileLocation) {
                    try {
                        val sourceFile = File(location.filePath)
                        if (sourceFile.exists()) {
                            val lines = sourceFile.readLines()
                            val propertyLineIndex = location.lineNumber - 1

                            if (propertyLineIndex >= 0 && propertyLineIndex < lines.size) {
                                // Read the complete property declaration including chained methods
                                val propertyDeclaration = readCompletePropertyDeclaration(lines, propertyLineIndex)

                                if (propertyDeclaration.contains("by option(")) {
                                    // Extract help text from option(help = "...") pattern - handle both single and multi-line strings
                                    val helpPattern = """option\s*\([^)]*help\s*=\s*"([^"]+(?:\s*\+\s*"[^"]*")*?)"""".toRegex()
                                    val helpMatch = helpPattern.find(propertyDeclaration)
                                    var helpText = helpMatch?.groupValues?.get(1)

                                    // Clean up multi-line help text by removing string concatenation artifacts
                                    helpText = helpText?.replace("""\s*\+\s*""".toRegex(), " ")?.trim()

                                    // Extract default value from .default(...) pattern - improved to handle nested parentheses
                                    val defaultValue = extractDefaultValue(propertyDeclaration)

                                    // Create enhanced description including default value if present
                                    val enhancedDescription = if (defaultValue != null) {
                                        "${helpText ?: "No description available"} (default: $defaultValue)"
                                    } else {
                                        helpText
                                    }

                                    options.add(CliParameterDescriptor(propertyName, enhancedDescription))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error reading source file for property ${property.simpleName.asString()}: ${e.message}")

                        // Fallback to the old method if source reading fails
                        val propertyString = property.toString()
                        if (propertyString.contains("option(")) {
                            val helpPattern = """option\([^)]*help\s*=\s*"([^"]+)"""".toRegex()
                            val helpMatch = helpPattern.find(propertyString)
                            val helpText = helpMatch?.groupValues?.get(1)
                            options.add(CliParameterDescriptor(propertyName, helpText))
                        }
                    }
                }
            }

        return options
    }
}

data class CliCommandDescriptor(
    val name: String?,
    val className: String,
    val description: String?,
    val arguments: List<CliParameterDescriptor>,
    val options: List<CliParameterDescriptor>,
)

data class CliParameterDescriptor(
    val name: String,
    val description: String?,
)

class CliKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = CliToAsciidocKspProcessor(environment)
}
