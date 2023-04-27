package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyArgs
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import kotlin.text.Typography.paragraph

@DslMarker
annotation class AsciidocDslMarker

@AsciidocDslMarker
class AsciidocDocument {
    var title: String? = null
    var author: String? = null
    val sections = mutableListOf<AsciidocSection>()

    fun section(title: String? = null, description: String? = null, block: AsciidocSection.() -> Unit) {
        val section = AsciidocSection(title, description)
        section.block()
        sections.add(section)
    }

    fun generate(): String {
        return buildString {
            if (title != null) {
                appendLine("== $title")
                appendLine()
            }
            if (author != null) {
                appendLine("Author: $author")
                appendLine()
            }
            sections.forEach {
                appendLine(it.generate())
            }
        }
    }
}

@AsciidocDslMarker
class AsciidocSection(val title: String?, val description: String?) {

    val childs = mutableListOf<AsciidocLibSection>()

    fun lib(title: String? = null, block: AsciidocLibSection.() -> Unit) {
        val libSection = AsciidocLibSection(title)
        libSection.block()
        childs.add(libSection)
    }

    fun generate(): String {
        return buildString {
            if (title != null) {
                appendLine("=== $title")
                appendLine()
            }

            if (description != null && description.isNotBlank()) {
                appendLine(description)
                appendLine()
            }

            childs.forEach {
                appendLine(it.generate())
            }
        }
    }
}

@AsciidocDslMarker
class AsciidocLibSection(val title: String?) {
    val paragraphs = mutableListOf<String>()

    fun paragraph(text: String) {
        paragraphs.add(text)
    }

    fun code(code: String) {
        paragraph(
            """
                >```lua
                >$code
                >```
               """.trimMargin(">")
        )
    }

    fun example(lua: String?) {
        if (lua == null) return
        paragraph(
            """
                >++++
                ><tiny-editor style="display: none;">
                >$lua
                ></tiny-editor>
                >++++
               """.trimMargin(">")
        )
    }

    fun generate(): String {
        return buildString {
            if (title != null) {
                appendLine("==== $title")
                appendLine()
            }
            paragraphs.forEach {
                appendLine(it)
                appendLine()
            }
        }
    }
}

fun asciidoc(block: AsciidocDocument.() -> Unit): AsciidocDocument {
    val doc = AsciidocDocument()
    doc.block()
    return doc
}

class TinyArgDescriptor(var name: String)
class TinyCallDescriptor(
    var description: String = "",
    var args: List<TinyArgDescriptor> = emptyList()
)

data class TinyFunctionDescriptor(
    var name: String = "",
    var description: String = "",
    var calls: List<TinyCallDescriptor> = emptyList(),
    var example: String? = null,
)

class TinyLibDescriptor(
    var name: String = "",
    var description: String = "",
    var functions: List<TinyFunctionDescriptor> = emptyList()
)

@OptIn(KspExperimental::class)
class KspProcessor(
    val env: SymbolProcessorEnvironment
) : SymbolProcessor {

    inner class LibVisitor : KSDefaultVisitor<TinyLibDescriptor, TinyLibDescriptor>() {
        override fun defaultHandler(node: KSNode, data: TinyLibDescriptor): TinyLibDescriptor = data

        override fun visitAnnotated(annotated: KSAnnotated, data: TinyLibDescriptor): TinyLibDescriptor {
            val libAnnotation = annotated.getAnnotationsByType(TinyLib::class)
            val lib = libAnnotation.firstOrNull() ?: return data

            data.name = lib.name
            data.description = lib.description

            return super.visitAnnotated(annotated, data)
        }

        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: TinyLibDescriptor
        ): TinyLibDescriptor {
            if (!classDeclaration.isAnnotationPresent(TinyLib::class)) return data

            val functions = classDeclaration.declarations.map { declaration ->
                declaration.accept(FunctionVisitor(), mutableListOf())
            }.flatMap { functions -> functions }.filter { f -> f.name.isNotBlank() }

            data.functions = functions.toList()

            return super.visitClassDeclaration(classDeclaration, data)
        }
    }

    /**
     * Visit a LUA Function which is defined in Kotlin as follow:
     *
     *
     * ```
     *     @TinyFunction
     *     inner class clip : LibFunction() {
     *         override fun call(): LuaValue {
     *             resourceAccess.frameBuffer.clipper.reset()
     *             return NONE
     *         }
     *
     *         override fun call(a: LuaValue, b: LuaValue, c: LuaValue, d: LuaValue): LuaValue {
     *             resourceAccess.frameBuffer.clipper.set(a.checkint(), b.checkint(), c.checkint(), d.checkint())
     *             return NONE
     *         }
     *     }
     * ```
     */
    inner class FunctionVisitor : KSDefaultVisitor<MutableList<TinyFunctionDescriptor>, MutableList<TinyFunctionDescriptor>>() {
        override fun defaultHandler(node: KSNode, data: MutableList<TinyFunctionDescriptor>): MutableList<TinyFunctionDescriptor> = data

        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: MutableList<TinyFunctionDescriptor>
        ): MutableList<TinyFunctionDescriptor> {
            val functions = classDeclaration.getAnnotationsByType(TinyFunction::class)
            if (functions.count() == 0) return data

            val defaultName = classDeclaration.accept(
                object : KSDefaultVisitor<Unit, String>() {
                    override fun defaultHandler(node: KSNode, data: Unit): String = ""

                    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): String {
                        return classDeclaration.simpleName.asString()
                    }
                },
                Unit
            )

            val result = functions.map { function ->
                // Get the name of the class
                val name = function.name.ifBlank {
                    classDeclaration.accept(
                        object : KSDefaultVisitor<Unit, String>() {
                            override fun defaultHandler(node: KSNode, data: Unit): String = ""

                            override fun visitClassDeclaration(
                                classDeclaration: KSClassDeclaration,
                                data: Unit
                            ): String {
                                return classDeclaration.simpleName.asString()
                            }
                        },
                        Unit
                    )
                }

                // Get all TinyCall annotated functions.
                val calls = mutableListOf<TinyCallDescriptor>()
                classDeclaration.getDeclaredFunctions()
                    .filter { f -> f.isAnnotationPresent(TinyCall::class) }
                    .forEach { f ->
                        val call = TinyCallDescriptor()
                        call.description = f.getAnnotationsByType(TinyCall::class).firstOrNull()?.description ?: ""
                        f.parameters.map { p ->
                            val multiArgs = p.getAnnotationsByType(TinyArgs::class)
                            val multiArg = multiArgs.firstOrNull()
                            if (multiArg != null) {
                                call.args += multiArg.names.map { n -> TinyArgDescriptor(n) }
                            } else {
                                val args = p.getAnnotationsByType(TinyArg::class)
                                val arg = args.firstOrNull()?.name ?: p.name?.asString() ?: ""
                                call.args += TinyArgDescriptor(arg)
                            }
                        }
                        calls.add(call)
                    }

                val example = function.example.ifBlank { null }
                TinyFunctionDescriptor(
                    example = example,
                    name = name.ifBlank { defaultName },
                    description = function.description,
                    calls = calls,
                )
            }
            data.addAll(result)
            return super.visitAnnotated(classDeclaration, data)
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip last KSP round. Everything should be done in one round
        resolver.getNewFiles().firstOrNull() ?: return emptyList()

        val symbolsWithAnnotation = resolver
            .getSymbolsWithAnnotation(TinyLib::class.qualifiedName!!)

        val libs = symbolsWithAnnotation.map { s ->
            val accept = s.accept(LibVisitor(), TinyLibDescriptor())
            accept
        }

        val file = env.codeGenerator.createNewFile(
            Dependencies(false),
            "/",
            "tiny-api",
            "adoc"
        )

        val result = asciidoc {
            title = "Tiny API"
            libs.forEach { lib ->
                section(lib.name.ifBlank { "std" }, lib.description) {
                    lib.functions.forEach { func ->
                        val prefix = if (lib.name.isBlank()) {
                            func.name
                        } else {
                            "${lib.name}.${func.name}"
                        }
                        lib("$prefix()") {
                            paragraph(func.description)

                            if (func.calls.isNotEmpty()) {
                                val result = func.calls.map { call ->
                                    "$prefix(${call.args.map { it.name }.joinToString(", ")}) " +
                                        "-- ${call.description}"
                                }.joinToString("\n")
                                code(result)
                            }

                            example(func.example)
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
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return KspProcessor(environment)
    }
}
