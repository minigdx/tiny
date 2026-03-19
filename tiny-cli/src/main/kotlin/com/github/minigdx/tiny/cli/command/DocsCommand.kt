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
import com.github.minigdx.tiny.cli.command.utils.JsonHelpFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * Command to generate JSON documentation for all CLI commands.
 *
 * This command iterates through all registered Tiny CLI commands and generates
 * a JSON file containing structured documentation including arguments, options,
 * defaults, and usage examples.
 */
@CliAnnotation(hidden = true)
class DocsCommand : CliktCommand(name = "docs") {
    private val outputFile by option(
        "--output",
        "-o",
        help = "Output file path for the generated JSON documentation",
    )
        .file(mustExist = false, canBeDir = false, canBeFile = true)
        .default(File("tiny-cli-commands.json"))

    override fun help(context: Context) = "Generate JSON documentation for all CLI commands"

    override fun run() {
        echo("Generating CLI documentation...")

        val prettyJson = Json { prettyPrint = true }
        val commandJsons = mutableListOf<JsonObject>()

        val commands = listOf(
            CreateCommand(),
            RunCommand(),
            AddCommand(),
            ExportCommand(),
            ServeCommand(),
            PaletteCommand(),
            SfxCommand(),
            UpdateCommand(),
            ResourcesCommand(),
            RecordCommand(),
        )

        commands.forEach { command ->
            try {
                command.context { helpFormatter = { JsonHelpFormatter } }
                command.parse(arrayOf("-h"))
            } catch (e: CliktError) {
                val helpJson = command.getFormattedHelp(e)
                if (helpJson != null) {
                    commandJsons.add(Json.parseToJsonElement(helpJson).jsonObject)
                }
            } catch (e: Exception) {
                echo("Warning: Could not generate docs for ${command.commandName}: ${e.message}", err = true)
            }
        }

        val result = buildJsonObject {
            put(
                "commands",
                buildJsonArray {
                    commandJsons.forEach { add(it) }
                },
            )
        }

        try {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(prettyJson.encodeToString(JsonElement.serializer(), result))
            echo("Documentation generated successfully: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            echo("Error writing documentation file: ${e.message}", err = true)
            throw e
        }
    }
}
