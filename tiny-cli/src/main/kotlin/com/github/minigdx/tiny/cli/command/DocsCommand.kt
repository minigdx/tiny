package com.github.minigdx.tiny.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.mingdx.tiny.doc.CliAnnotation
import com.github.minigdx.tiny.cli.command.utils.AsciidocHelpFormatter
import java.io.File

/**
 * Command to generate AsciiDoc documentation for all CLI commands.
 *
 * This command iterates through all registered Tiny CLI commands and generates
 * comprehensive AsciiDoc documentation including arguments, options, defaults,
 * and usage examples.
 */
@CliAnnotation(hidden = true)
class DocsCommand : CliktCommand(name = "docs") {
    private val outputFile by option(
        "--output",
        "-o",
        help = "Output file path for the generated AsciiDoc documentation",
    )
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("tiny-cli-commands.adoc"))

    override fun help(context: Context) = "Generate AsciiDoc documentation for all CLI commands"

    override fun run() {
        echo("📚 Generating CLI documentation...")

        val documentation = buildString {
            // Document header (level 2 since this will be included in main docs)
            appendLine("== Tiny CLI Commands Reference")
            appendLine()
            appendLine("=== Commands")
            appendLine()

            // Get all command classes to document
            val commands = listOf(
                CreateCommand(),
                RunCommand(),
                DebugCommand(),
                AddCommand(),
                ExportCommand(),
                ServeCommand(),
                PaletteCommand(),
                SfxCommand(),
                UpdateCommand(),
                ResourcesCommand(),
            )

            // Generate documentation for each command
            commands.forEach { command ->
                try {
                    command.context { helpFormatter = { AsciidocHelpFormatter } }
                    command.parse(arrayOf("-h"))
                } catch (e: CliktError) {
                    val asciidocHelp = command.getFormattedHelp(e)
                    appendLine(asciidocHelp)
                    // Use AsciidocHelpFormatter to convert help to AsciiDoc
                    appendLine()
                } catch (e: Exception) {
                    echo("⚠️  Warning: Could not generate docs for ${command.commandName}: ${e.message}", err = true)
                    e.printStackTrace()
                }
            }
        }

        // Write to output file
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(documentation)
            echo("✅ Documentation generated successfully: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            echo("❌ Error writing documentation file: ${e.message}", err = true)
            throw e
        }
    }
}
