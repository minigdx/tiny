package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyLib
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class TinyToAsciidocKspProcessor(
    val env: SymbolProcessorEnvironment,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip last KSP round. Everything should be done in one round
        resolver.getNewFiles().firstOrNull() ?: return emptyList()

        val symbolsWithAnnotation =
            resolver
                .getSymbolsWithAnnotation(TinyLib::class.qualifiedName!!)

        val sourceFiles =
            symbolsWithAnnotation
                .filterIsInstance<KSClassDeclaration>()
                .mapNotNull { it.containingFile }
                .toList()
                .toTypedArray()

        val file =
            env.codeGenerator.createNewFile(
                Dependencies(true, *sourceFiles),
                "/",
                "tiny-api",
                "adoc",
            )

        val libs =
            symbolsWithAnnotation.map { s ->
                val accept = s.accept(TinyLibVisitor(), TinyLibDescriptor())
                accept
            }

        val result =
            asciidoc {
                title = "Tiny API"
                libs.forEach { lib ->
                    section(lib.name.ifBlank { "std" }, lib.description) {
                        lib.variables.filterNot { it.hidden }.forEach { variable ->
                            val prefix =
                                if (lib.name.isBlank()) {
                                    variable.name
                                } else {
                                    "${lib.name}.${variable.name}"
                                }
                            lib(prefix) {
                                paragraph(variable.description)
                                example(
                                    prefix,
                                    """
                                    function _update()
                                        gfx.cls()
                                        print($prefix, 10, 10) -- ${variable.description}
                                    end
                                    """.trimIndent(),
                                )
                            }
                        }
                        lib.functions.forEach { func ->
                            val prefix =
                                if (lib.name.isBlank()) {
                                    func.name
                                } else {
                                    "${lib.name}.${func.name}"
                                }
                            lib("$prefix()") {
                                paragraph(func.description)

                                if (func.calls.isNotEmpty()) {
                                    val result = func.calls.joinToString("\n") { call ->
                                        "$prefix(${call.args.joinToString(", ") { it.name }}) -- ${call.description}"
                                    }
                                    code(result)
                                }

                                val args = func.calls.flatMap { it.args }
                                        .filter { it.description.isNotBlank() }
                                        .sortedBy { it.name }

                                if (args.isNotEmpty()) {
                                    tableArgs(
                                        args.map {
                                            it.name to it.description
                                        },
                                    )
                                }

                                example(func.name, func.example, func.spritePath, func.levelPath)
                            }
                        }
                    }
                }
            }.generate()

        file.write(result.toByteArray())
        return emptyList()
    }
}

class KspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = TinyToAsciidocKspProcessor(environment)
}
