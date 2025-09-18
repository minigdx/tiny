package com.github.minigdx.tiny.doc

@DslMarker
annotation class LuaStubDslMarker

fun stub(
    description: String,
    block: LuaStubDocument.() -> Unit,
): LuaStubDocument {
    val doc =
        LuaStubDocument().apply {
            this.description = description
        }
    block(doc)
    return doc
}

@LuaStubDslMarker
class LuaStubDocument {
    var description: String = ""
    var libs: Sequence<LuaStubLib> = emptySequence()

    fun lib(block: LuaStubLib.() -> Unit) {
        val lib = LuaStubLib()
        lib.block()
        libs += lib
    }

    fun generate(): String {
        return buildString {
            append(description)
            appendLine()
            appendLine()
            libs.forEach {
                appendLine()
                append(it.generate())
            }
        }
    }
}

@LuaStubDslMarker
class LuaStubLib {
    var name: String = ""
    var description: String = ""
    var functions: List<LuaStubFunction> = emptyList()
    var variables: List<LuaStubVariable> = emptyList()

    fun function(block: LuaStubFunction.() -> Unit) {
        val func = LuaStubFunction()
        func.block()
        functions += func
    }

    fun variable(block: LuaStubVariable.() -> Unit) {
        val variable = LuaStubVariable()
        variable.block()
        variable.namespace = name
        variables += variable
    }

    fun generate(): String {
        return buildString {
            appendLine(comment(description))
            if (name.isNotBlank()) {
                appendLine("$name = {}")
            }

            variables.forEach {
                append(it.generate())
            }

            functions.forEach {
                append(it.generate())
            }
            appendLine()
        }
    }
}

@LuaStubDslMarker
class LuaStubVariable {
    fun generate(): String {
        return buildString {
            appendLine(comment(description))
            appendLine("$namespace.$name = any")
        }
    }

    var namespace: String? = null
    var name: String = ""
    var description: String = ""
    var hidden: Boolean = false
}

@LuaStubDslMarker
class LuaStubFunction {
    var namespace: String? = null
    var name: String = ""
    var description: String = ""
    var calls: List<LuaStubCall> = emptyList()

    fun call(block: LuaStubCall.() -> Unit) {
        val call = LuaStubCall()
        call.block()
        calls += call
    }

    fun generate(): String {
        return buildString {
            appendLine(comment(description))
            appendLine(calls.joinToString("\n") { it.generate() })
            if (namespace == null) {
                appendLine("function $name() end")
            } else {
                appendLine("$namespace.$name = function() end")
            }
        }
    }
}

@LuaStubDslMarker
class LuaStubCall {
    var description: String = ""
    var returnType: String = "any"
    var args: List<LuaStubArg> = emptyList()

    fun arg(block: LuaStubArg.() -> Unit) {
        val arg = LuaStubArg()
        arg.block()
        args += arg
    }

    fun generate(): String {
        return buildString {
            // ---@overload fun([param: type[, param: type...]]): [return_value[,return_value]] -- description
            val args = args.joinToString(", ") { it.generate() }
            append(comment("@overload fun($args): $returnType -- $description"))
        }
    }
}

@LuaStubDslMarker
class LuaStubArg {
    var name: String = ""
    var type: String = ""
    var description: String = ""

    fun generate(): String {
        return "$name: $type"
    }
}

private fun comment(
    content: String,
    headline: String = "---",
): String {
    return content.split("\n").joinToString("\n") { line -> "$headline $line" }
}
