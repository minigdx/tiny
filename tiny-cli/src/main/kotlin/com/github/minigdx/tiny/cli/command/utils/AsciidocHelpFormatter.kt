package com.github.minigdx.tiny.cli.command.utils

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter.ParameterHelp

/**
 * Utility object for converting CLI command help to AsciiDoc format.
 *
 * This converts standard Clikt help output to properly structured AsciiDoc
 * documentation including command sections, usage examples, and parameter tables.
 */
object AsciidocHelpFormatter : HelpFormatter {
    override fun formatHelp(
        error: UsageError?,
        prolog: String,
        epilog: String,
        parameters: List<HelpFormatter.ParameterHelp>,
        programName: String,
    ): String {
        val optionParameters = parameters.filterIsInstance<ParameterHelp.Option>()
        val argumentParameters = parameters.filterIsInstance<ParameterHelp.Argument>()

        return buildString {
            appendSection3Title(programName)
            appendEmptyLine()
            appendLine(prolog)
            appendEmptyLine()
            appendUsageExample(programName, optionParameters, argumentParameters)
            appendEmptyLine()
            appendOptionsSection(optionParameters)
            appendEmptyLine()
            appendArgumentsSection(argumentParameters)
        }.trim()
    }

    private fun StringBuilder.appendSection3Title(title: String) {
        appendLine("=== $title")
    }

    private fun StringBuilder.appendEmptyLine() {
        appendLine()
    }

    private fun StringBuilder.appendUsageExample(
        programName: String,
        optionParameters: List<ParameterHelp.Option>,
        argumentParameters: List<ParameterHelp.Argument>,
    ) {
        val firstOption = optionParameters
            .firstOrNull()
            ?.names
            ?.firstOrNull()
            ?.let { "$it=<value>" }
            ?: ""

        val firstArgument = argumentParameters
            .firstOrNull()
            ?.name
            ?.lowercase()
            ?.let { "<$it>" }
            ?: ""

        appendLine("[source]")
        appendLine("----")
        appendLine("tiny-cli $programName $firstOption $firstArgument".trim())
        appendLine("----")
    }

    private fun StringBuilder.appendOptionsSection(options: List<ParameterHelp.Option>) {
        if (options.isEmpty()) return

        appendLine("==== Options")
        appendEmptyLine()
        options.forEach { option ->
            appendOptionDefinition(option)
        }
    }

    private fun StringBuilder.appendOptionDefinition(option: ParameterHelp.Option) {
        val names = option.names.joinToString(", ")
        appendLine("`$names=<value>`::")
        appendLine(option.help)
    }

    private fun StringBuilder.appendOptionDefinition(argument: ParameterHelp.Argument) {
        appendLine("`<${argument.name.lowercase()}>`::")
        appendLine(argument.help)
    }

    private fun StringBuilder.appendArgumentsSection(arguments: List<ParameterHelp.Argument>) {
        if (arguments.isEmpty()) return

        appendLine("==== Arguments")
        appendEmptyLine()
        arguments.forEach { argument ->
            appendOptionDefinition(argument)
        }
    }
}
