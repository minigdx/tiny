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
        val sortedLibs = libs.sortedBy { it.name }.toList()
        return json {
            array("libraries") {
                sortedLibs.forEach { lib ->
                    obj {
                        value("name", lib.name.ifBlank { "std" })
                        value("description", lib.description)
                        value("icon", lib.icon)
                        array("variables") {
                            lib.variables.filterNot { it.hidden }.sortedBy { it.name }.forEach { variable ->
                                obj {
                                    value("name", variable.name)
                                    value("description", variable.description)
                                }
                            }
                        }
                        array("functions") {
                            lib.functions.sortedBy { it.name }.forEach { func ->
                                obj {
                                    value("name", func.name)
                                    value("description", func.description)
                                    value("example", func.example)
                                    array("calls") {
                                        func.calls.forEach { call ->
                                            obj {
                                                value("description", call.description)
                                                value("returnType", call.returnType)
                                                array("args") {
                                                    call.args.forEach { arg ->
                                                        obj {
                                                            value("name", arg.name)
                                                            value("type", arg.type)
                                                            value("description", arg.description)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.generate() + "\n"
    }
}

class TinyToJsonKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TinyToJsonKspProcessor(environment)
    }
}
