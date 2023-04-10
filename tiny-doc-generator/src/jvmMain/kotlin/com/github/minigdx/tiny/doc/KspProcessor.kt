package com.github.minigdx.tiny.doc

import com.github.mingdx.tiny.doc.TinyFunction
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.PlatformInfo
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import java.io.File

@DslMarker
annotation class AsciidocDslMarker

@AsciidocDslMarker
class AsciidocDocument {
    var title: String? = null
    var author: String? = null
    val sections = mutableListOf<AsciidocSection>()

    fun section(title: String? = null, block: AsciidocSection.() -> Unit) {
        val section = AsciidocSection(title)
        section.block()
        sections.add(section)
    }

    fun generate(): String {
        return buildString {
            if (title != null) {
                appendLine("= $title")
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
class AsciidocSection(val title: String?) {
    val paragraphs = mutableListOf<String>()

    fun paragraph(text: String) {
        paragraphs.add(text)
    }

    fun generate(): String {
        return buildString {
            if (title != null) {
                appendLine("== $title")
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

data class TinyFunctionDesription(
    var name: String = "",
    var example: String? = null,
)

@OptIn(KspExperimental::class)
class KspProcessor(
    val env: SymbolProcessorEnvironment
) : SymbolProcessor {

    inner class FunctionVisitor : KSDefaultVisitor<TinyFunctionDesription, TinyFunctionDesription>() {
        override fun defaultHandler(node: KSNode, data: TinyFunctionDesription): TinyFunctionDesription = data

        override fun visitAnnotated(annotated: KSAnnotated, data: TinyFunctionDesription): TinyFunctionDesription {
            val functions = annotated.getAnnotationsByType(TinyFunction::class)
            val function = functions.firstOrNull() ?: return data

            val name = function.name.ifBlank {
                annotated.accept(object : KSDefaultVisitor<Unit, String>() {
                    override fun defaultHandler(node: KSNode, data: Unit): String = ""

                    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): String {
                        return classDeclaration.simpleName.asString()
                    }
                }, Unit)
            }

            val example = function.example.ifBlank { null }
            return data.apply {
                this.example = example
                this.name = name
            }
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Skip last KSP round. Everything should be done in one round
        resolver.getNewFiles().firstOrNull() ?: return emptyList()

        // FÌXME: visite each libs.
        //   visite each func
        //   visite each args
        /*

        name
        ---------
        func description

        > name(arg1)

        call description

        arg1: description
        arg2: description
        arg3: description


        EXAMPLE


         */
        val symbolsWithAnnotation = resolver
            .getSymbolsWithAnnotation(TinyFunction::class.qualifiedName!!)

        val functions = symbolsWithAnnotation.map { s ->
            val accept = s.accept(FunctionVisitor(), TinyFunctionDesription())
            env.logger.warn(accept.name, s)
            accept
        }

        val file = env.codeGenerator.createNewFile(
            Dependencies(false),
            "/",
            "functions",
            "adoc"
        )
        // FIXME: generate asciidoc
        file.write(functions.map { it.name }.joinToString(", ").toByteArray())
        return emptyList()
    }

    override fun finish() {
        val file = File("/tmp/ksp2.txt")
        file.appendText("COUCUsdsdsO")
        println("FINSH")
    }

    override fun onError() {
        val file = File("/tmp/ksp1.txt")
        file.appendText("COUCUO")
        println("ERROR")
    }
}

class KspProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return KspProcessor(environment)
    }
}
