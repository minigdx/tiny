package com.github.minigdx.tiny.doc

@DslMarker
annotation class JsonDslMarker

private sealed interface JsonEntry {
    fun render(indent: Int): String
}

private class JsonValueEntry(val name: String, val value: String?) : JsonEntry {
    override fun render(indent: Int): String {
        val pad = " ".repeat(indent)
        val v = if (value != null) "\"${escapeJson(value)}\"" else "null"
        return "$pad\"$name\": $v"
    }
}

private class JsonObjectEntry(val name: String, val obj: JsonObject) : JsonEntry {
    override fun render(indent: Int): String {
        val pad = " ".repeat(indent)
        return "$pad\"$name\": ${obj.generate(indent)}"
    }
}

private class JsonArrayEntry(val name: String, val arr: JsonArray) : JsonEntry {
    override fun render(indent: Int): String {
        val pad = " ".repeat(indent)
        return "$pad\"$name\": ${arr.generate(indent)}"
    }
}

private fun escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

@JsonDslMarker
class JsonObject {
    private val entries = mutableListOf<JsonEntry>()

    fun value(name: String, value: String?) {
        entries.add(JsonValueEntry(name, value))
    }

    fun obj(name: String, block: JsonObject.() -> Unit) {
        val child = JsonObject()
        child.block()
        entries.add(JsonObjectEntry(name, child))
    }

    fun array(name: String, block: JsonArray.() -> Unit) {
        val child = JsonArray()
        child.block()
        entries.add(JsonArrayEntry(name, child))
    }

    fun generate(indent: Int = 0): String {
        val pad = " ".repeat(indent)
        val innerPad = " ".repeat(indent + 2)
        if (entries.isEmpty()) {
            return "{\n$pad}"
        }
        val content = entries.joinToString(",\n") { it.render(indent + 2) }
        return "{\n$content\n$pad}"
    }
}

@JsonDslMarker
class JsonArray {
    private val elements = mutableListOf<JsonObject>()

    fun obj(block: JsonObject.() -> Unit) {
        val child = JsonObject()
        child.block()
        elements.add(child)
    }

    fun generate(indent: Int = 0): String {
        val pad = " ".repeat(indent)
        val innerIndent = indent + 2
        if (elements.isEmpty()) {
            return "[\n$pad]"
        }
        val innerPad = " ".repeat(innerIndent)
        val content = elements.joinToString(",\n") { "$innerPad${it.generate(innerIndent)}" }
        return "[\n$content\n$pad]"
    }
}

fun json(block: JsonObject.() -> Unit): JsonObject {
    val root = JsonObject()
    root.block()
    return root
}
