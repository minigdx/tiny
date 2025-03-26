package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyLib
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class TinyToLuaStubKspProcessor(
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
            "_tiny.stub",
            "lua",
        )

        val libs = symbolsWithAnnotation.map { s ->
            val accept = s.accept(TinyLibVisitor(), TinyLibDescriptor())
            accept
        }

        val result = stub(
            """
            -- DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT
            -- Tiny stub lua file generated automatically
            -- The file is used only to help Lua editors with autocomplete
            -- 
            -- An error, an issue? Please consult https://github.com/minigdx/tiny
            """.trimIndent(),
        ) {
            libs.forEach {
                lib {
                    name = it.name
                    description = it.description

                    it.functions.forEach { function ->
                        function {
                            namespace = it.name.takeIf { it.isNotBlank() }
                            name = function.name
                            description = function.description

                            function.calls.forEach { call ->
                                call {
                                    description = call.description
                                    call.args.forEach { arg ->
                                        arg {
                                            name = arg.name
                                            type = "any"
                                            description = arg.description
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        file.write(result.generate().toByteArray(charset = Charsets.UTF_8))

        return emptyList()
    }
}

class TinyToLuaStubKspProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return TinyToLuaStubKspProcessor(environment)
    }
}
