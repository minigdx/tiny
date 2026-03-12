package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyLib
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class TinyToJsonKspProcessor(
    val env: SymbolProcessorEnvironment,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip last KSP round. Everything should be done in one round
        resolver.getNewFiles().firstOrNull() ?: return emptyList()

        val symbolsWithAnnotation = resolver.getSymbolsWithAnnotation(TinyLib::class.qualifiedName!!)

        val sourceFiles = symbolsWithAnnotation
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { it.containingFile }
            .toList()
            .toTypedArray()

        val file = env.codeGenerator.createNewFile(
            Dependencies(true, *sourceFiles),
            "/",
            "tiny-api",
            "json",
        )

        val libs = symbolsWithAnnotation.map { s ->
            s.accept(TinyLibVisitor(), TinyLibDescriptor())
        }

        val json = generateJson(libs)
        file.write(json.toByteArray(charset = Charsets.UTF_8))

        return emptyList()
    }

    private fun generateJson(libs: Sequence<TinyLibDescriptor>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"libraries\": [\n")

        val sortedLibs = libs.sortedBy { it.name }.toList()
        sortedLibs.forEachIndexed { libIndex, lib ->
            sb.append("    {\n")
            sb.append("      \"name\": ${jsonString(lib.name.ifBlank { "std" })},\n")
            sb.append("      \"description\": ${jsonString(lib.description)},\n")
            sb.append("      \"icon\": ${jsonString(lib.icon)},\n")

            // Variables
            sb.append("      \"variables\": [\n")
            val visibleVars = lib.variables.filterNot { it.hidden }.sortedBy { it.name }
            visibleVars.forEachIndexed { varIndex, variable ->
                sb.append("        {\n")
                sb.append("          \"name\": ${jsonString(variable.name)},\n")
                sb.append("          \"description\": ${jsonString(variable.description)}\n")
                sb.append("        }")
                if (varIndex < visibleVars.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("      ],\n")

            // Functions
            sb.append("      \"functions\": [\n")
            val sortedFunctions = lib.functions.sortedBy { it.name }
            sortedFunctions.forEachIndexed { funcIndex, func ->
                sb.append("        {\n")
                sb.append("          \"name\": ${jsonString(func.name)},\n")
                sb.append("          \"description\": ${jsonString(func.description)},\n")

                // Example
                if (func.example != null) {
                    sb.append("          \"example\": ${jsonString(func.example!!)},\n")
                } else {
                    sb.append("          \"example\": null,\n")
                }

                // Calls (overloads)
                sb.append("          \"calls\": [\n")
                func.calls.forEachIndexed { callIndex, call ->
                    sb.append("            {\n")
                    sb.append("              \"description\": ${jsonString(call.description)},\n")
                    sb.append("              \"returnType\": ${jsonString(call.returnType)},\n")
                    sb.append("              \"args\": [\n")
                    call.args.forEachIndexed { argIndex, arg ->
                        sb.append("                {\n")
                        sb.append("                  \"name\": ${jsonString(arg.name)},\n")
                        sb.append("                  \"type\": ${jsonString(arg.type)},\n")
                        sb.append("                  \"description\": ${jsonString(arg.description)}\n")
                        sb.append("                }")
                        if (argIndex < call.args.size - 1) sb.append(",")
                        sb.append("\n")
                    }
                    sb.append("              ]\n")
                    sb.append("            }")
                    if (callIndex < func.calls.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("          ]\n")

                sb.append("        }")
                if (funcIndex < sortedFunctions.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("      ]\n")

            sb.append("    }")
            if (libIndex < sortedLibs.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}\n")
        return sb.toString()
    }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}

class TinyToJsonKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TinyToJsonKspProcessor(environment)
    }
}
