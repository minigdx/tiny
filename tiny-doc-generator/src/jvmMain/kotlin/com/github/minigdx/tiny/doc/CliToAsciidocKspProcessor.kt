package com.github.minigdx.tiny.doc

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.getAllSuperTypes

class CliKspProcessor(
    val env: SymbolProcessorEnvironment,
) : SymbolProcessor {
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
                options = extractOptions(classDecl)
            )
        }.sortedBy { it.name }

        val result = asciidoc {
            title = "Tiny CLI Commands"
            section("Commands", "Available commands for the Tiny CLI tool.") {
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
            // Try to extract the return statement from the help method
            // This still uses regex but is more structured with reflection filtering
            val functionBody = func.toString()

            // Look for patterns like: return "Some description"
            val returnPattern = """return\s+"([^"]+)"""".toRegex()
            val match = returnPattern.find(functionBody)
            if (match != null) {
                return match.groupValues[1]
            }

            // Look for patterns like: = "Some description"
            val assignmentPattern = """=\s+"([^"]+)"""".toRegex()
            val assignmentMatch = assignmentPattern.find(functionBody)
            if (assignmentMatch != null) {
                return assignmentMatch.groupValues[1]
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

                // Check if this property uses argument() by examining its type and initialization
                val propertyString = property.toString()

                if (propertyString.contains("argument(")) {
                    // Extract help text from argument(help = "...") pattern
                    val helpPattern = """argument\([^)]*help\s*=\s*"([^"]+)"""".toRegex()
                    val helpMatch = helpPattern.find(propertyString)
                    val helpText = helpMatch?.groupValues?.get(1)

                    arguments.add(CliParameterDescriptor(propertyName, helpText))
                }
            }

        return arguments
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

                // Check if this property uses option() by examining its type and initialization
                val propertyString = property.toString()

                if (propertyString.contains("option(")) {
                    // Extract help text from option(help = "...") pattern
                    val helpPattern = """option\([^)]*help\s*=\s*"([^"]+)"""".toRegex()
                    val helpMatch = helpPattern.find(propertyString)
                    val helpText = helpMatch?.groupValues?.get(1)

                    options.add(CliParameterDescriptor(propertyName, helpText))
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
    val options: List<CliParameterDescriptor>
)

data class CliParameterDescriptor(
    val name: String,
    val description: String?
)

class CliKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = CliKspProcessor(environment)
}
