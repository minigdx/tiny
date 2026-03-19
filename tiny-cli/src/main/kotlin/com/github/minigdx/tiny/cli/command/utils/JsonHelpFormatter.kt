package com.github.minigdx.tiny.cli.command.utils

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter.ParameterHelp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Formats CLI command help as a JSON object.
 *
 * Each call to [formatHelp] returns a JSON string representing a single command
 * with its name, description, usage, options, and arguments.
 */
object JsonHelpFormatter : HelpFormatter {
    override fun formatHelp(
        error: UsageError?,
        prolog: String,
        epilog: String,
        parameters: List<HelpFormatter.ParameterHelp>,
        programName: String,
    ): String {
        val options = parameters.filterIsInstance<ParameterHelp.Option>()
        val arguments = parameters.filterIsInstance<ParameterHelp.Argument>()

        val command = buildJsonObject {
            put("name", programName)
            put("description", prolog)
            put("usage", buildUsage(programName, options, arguments))
            put(
                "options",
                buildJsonArray {
                    options.forEach { option ->
                        add(
                            buildJsonObject {
                                put(
                                    "names",
                                    buildJsonArray {
                                        option.names.forEach { add(JsonPrimitive(it)) }
                                    },
                                )
                                put("help", option.help)
                            },
                        )
                    }
                },
            )
            put(
                "arguments",
                buildJsonArray {
                    arguments.forEach { arg ->
                        add(
                            buildJsonObject {
                                put("name", arg.name)
                                put("help", arg.help)
                            },
                        )
                    }
                },
            )
        }
        return Json.encodeToString(JsonElement.serializer(), command)
    }

    private fun buildUsage(
        programName: String,
        options: List<ParameterHelp.Option>,
        arguments: List<ParameterHelp.Argument>,
    ): String {
        val parts = mutableListOf("tiny-cli", programName)
        options.firstOrNull()?.names?.firstOrNull()?.let { parts.add("$it=<value>") }
        arguments.firstOrNull()?.name?.lowercase()?.let { parts.add("<$it>") }
        return parts.joinToString(" ")
    }
}
