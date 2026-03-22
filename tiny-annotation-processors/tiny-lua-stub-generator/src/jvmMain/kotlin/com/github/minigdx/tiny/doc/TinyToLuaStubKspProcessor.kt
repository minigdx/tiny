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

        val result =
            stub(
                """
                -- DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT // DO NOT EDIT
                -- Tiny stub lua file generated automatically
                -- The file is used only to help Lua editors with autocomplete
                --
                -- An error, an issue? Please consult https://github.com/minigdx/tiny
                --
                -- COLOR SYSTEM:
                --   Color 0 = TRANSPARENT. Never use 0 if you want something visible.
                --   Colors 1..N are defined in the _tiny.json configuration file ("colors" array).
                --   The first hex color in the array is index 1, the second is index 2, etc.
                --   Typical palette example (PICO-8 style, 16 colors):
                --     1 = black, 2 = dark blue, 3 = dark purple, 4 = dark green,
                --     5 = brown, 6 = dark grey, 7 = light grey, 8 = white,
                --     9 = red, 10 = orange, 11 = yellow, 12 = green,
                --     13 = blue, 14 = lavender, 15 = pink, 16 = peach
                --   Check _tiny.json "colors" array for the actual palette of the current game.
                --
                -- DEFAULT COLORS:
                --   gfx.cls() without arguments clears to the closest color to black (#000000).
                --   print() without a color argument uses the closest color to white (#FFFFFF).
                --   To ensure visibility: use a color index DIFFERENT from the cls() color.
                --   Common safe pattern: gfx.cls(1) then draw with colors >= 2.
                --
                -- COMMON MISTAKES:
                --   WRONG: shape.circlef(10, 10, 10, 0)   -- color 0 is transparent, nothing visible!
                --   WRONG: gfx.cls(1) then shape.circlef(10, 10, 10, 1) -- same color as background!
                --   RIGHT: gfx.cls(1) then shape.circlef(10, 10, 10, 8) -- visible: white circle on black
                """.trimIndent(),
            ) {
                libs.forEach {
                    lib {
                        name = it.name
                        description = it.description

                        it.variables.forEach {
                            variable {
                                name = it.name
                                description = it.description
                                hidden = it.hidden
                            }
                        }
                        it.functions.forEach { function ->
                            function {
                                namespace = it.name.takeIf { it.isNotBlank() }
                                name = function.name
                                description = function.description

                                function.calls.forEach { call ->
                                    call {
                                        description = call.description
                                        returnType = call.returnType
                                        call.args.forEach { arg ->
                                            arg {
                                                name = arg.name
                                                type = arg.type
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
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TinyToLuaStubKspProcessor(environment)
    }
}
