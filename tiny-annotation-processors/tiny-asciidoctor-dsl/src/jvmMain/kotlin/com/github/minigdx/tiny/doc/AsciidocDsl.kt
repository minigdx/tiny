package com.github.minigdx.tiny.doc

typealias ArgName = String
typealias ArgDescription = String

@DslMarker
annotation class AsciidocDslMarker

@AsciidocDslMarker
class AsciidocDocument {
    var title: String? = null
    var author: String? = null
    val sections = mutableListOf<AsciidocSection>()

    fun section(
        title: String? = null,
        description: String? = null,
        block: AsciidocSection.() -> Unit,
    ) {
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

    fun lib(
        title: String? = null,
        block: AsciidocLibSection.() -> Unit,
    ) {
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

            if (!description.isNullOrBlank()) {
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

    fun code(
        code: String,
        language: String = "lua",
    ) {
        paragraph(
            """
                >```$language
                >$code
                >```
               """.trimMargin(">"),
        )
    }

    // List<Name
    fun tableArgs(args: List<Pair<ArgName, ArgDescription>>) {
        val rows = args.joinToString("\n") {
            "|${it.first} |${it.second}"
        }
        paragraph(
            """
        >[cols="1,1"]
        >|===
        >|Argument name |Argument description
        >
        >$rows      
        >|===
        """.trimMargin(">"),
        )
    }

    fun example(
        functionName: String,
        lua: String?,
        spritePath: String? = null,
        levelPath: String? = null,
    ) {
        if (lua == null) return
        val spr = spritePath?.let { """sprite="$it"""" } ?: ""
        val lvl = levelPath?.let { """level="$it"""" } ?: ""
        paragraph(
            """
                >++++
                ><tiny-editor style="display: none;" $spr $lvl>
                >${lua.replace("##function##", functionName)}
                ></tiny-editor>
                >++++
               """.trimMargin(">"),
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
