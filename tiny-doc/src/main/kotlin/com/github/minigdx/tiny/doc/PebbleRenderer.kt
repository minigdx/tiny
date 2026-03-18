package com.github.minigdx.tiny.doc

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.FileLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File

fun main(args: Array<String>) {
    require(args.size == 4) {
        "Usage: PebbleRenderer <templateDir> <dataDir> <outputDir> <apiJsonFile>"
    }
    PebbleRenderer(
        templateDir = File(args[0]),
        dataDir = File(args[1]),
        outputDir = File(args[2]),
        apiJsonFile = File(args[3]),
    ).render()
}

class PebbleRenderer(
    private val templateDir: File,
    private val dataDir: File,
    private val outputDir: File,
    private val apiJsonFile: File,
) {
    fun render() {
        val games = parseJsonArray(dataDir.resolve("showcase.json"))
        val tutorials = parseJsonArray(dataDir.resolve("tutorials.json"))
        val functions = flattenApiJson(apiJsonFile)
        val functionsJson = Json.encodeToString(JsonElement.serializer(), toJsonElement(functions))

        val genres = games
            .flatMap { game ->
                @Suppress("UNCHECKED_CAST")
                (game["genres"] as? List<String>) ?: emptyList()
            }
            .toSortedSet()
            .toList()

        val libraries = functions
            .mapNotNull { fn -> fn["library"] as? String }
            .toSortedSet()
            .toList()

        val engine = createEngine()
        outputDir.mkdirs()

        renderTemplate(engine, "pages/index.peb", "index.html", emptyMap())

        renderTemplate(
            engine,
            "pages/showcase.peb",
            "showcase.html",
            mapOf(
                "games" to games,
                "genres" to genres,
                "initialCount" to 6,
            ),
        )

        renderTemplate(
            engine,
            "pages/documentation.peb",
            "documentation.html",
            mapOf(
                "tutorials" to tutorials,
                "functions" to functions,
                "functionsJson" to functionsJson,
            ),
        )

        renderTemplate(
            engine,
            "pages/api.peb",
            "api.html",
            mapOf(
                "functions" to functions,
                "libraries" to libraries,
            ),
        )

        println("Rendered 4 Pebble templates to ${outputDir.absolutePath}")
    }

    private fun createEngine(): PebbleEngine {
        val loader = FileLoader()
        loader.setPrefix(templateDir.absolutePath + "/")
        loader.setSuffix("")
        return PebbleEngine.Builder()
            .loader(loader)
            .autoEscaping(true)
            .build()
    }

    private fun renderTemplate(
        engine: PebbleEngine,
        templateName: String,
        outputName: String,
        context: Map<String, Any>,
    ) {
        val template = engine.getTemplate(templateName)
        File(outputDir, outputName).writer().use { writer ->
            template.evaluate(writer, context)
        }
    }

    /**
     * Flatten the nested tiny-api.json structure into the flat format expected by Pebble templates.
     *
     * Input structure: { libraries: [{ name, functions: [{ name, calls: [{ args }] }], variables: [{ name }] }] }
     * Output structure: [{ library, name, signature, description, parameters: [{ name, type, description }], example }]
     */
    private fun flattenApiJson(file: File): List<Map<String, Any?>> {
        val root = toNative(Json.parseToJsonElement(file.readText())) as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        val libraries = root["libraries"] as List<Map<String, Any?>>

        return libraries.flatMap { lib ->
            val libName = lib["name"] as String
            val functions = flattenFunctions(libName, lib)
            val variables = flattenVariables(libName, lib)
            functions + variables
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun flattenFunctions(
        libName: String,
        lib: Map<String, Any?>,
    ): List<Map<String, Any?>> {
        val functions = lib["functions"] as? List<Map<String, Any?>> ?: emptyList()
        return functions.map { fn ->
            val name = fn["name"] as String
            val calls = fn["calls"] as? List<Map<String, Any?>> ?: emptyList()
            val primaryCall = calls.maxByOrNull { call ->
                (call["args"] as? List<*>)?.size ?: 0
            }
            val args = primaryCall?.let { it["args"] as? List<Map<String, Any?>> } ?: emptyList()
            val argNames = args.joinToString(", ") { arg -> arg["name"] as? String ?: "" }

            mapOf(
                "library" to libName,
                "name" to name,
                "signature" to "$libName.$name($argNames)",
                "description" to (fn["description"] as? String ?: ""),
                "parameters" to args.map { arg ->
                    mapOf(
                        "name" to (arg["name"] as? String ?: ""),
                        "type" to (arg["type"] as? String ?: ""),
                        "description" to (arg["description"] as? String ?: ""),
                    )
                },
                "example" to (fn["example"] as? String ?: ""),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun flattenVariables(
        libName: String,
        lib: Map<String, Any?>,
    ): List<Map<String, Any?>> {
        val variables = lib["variables"] as? List<Map<String, Any?>> ?: emptyList()
        return variables.map { v ->
            val name = v["name"] as String
            mapOf(
                "library" to libName,
                "name" to name,
                "type" to "variable",
                "signature" to "$libName.$name",
                "description" to (v["description"] as? String ?: ""),
                "parameters" to emptyList<Map<String, Any?>>(),
                "example" to (v["example"] as? String ?: ""),
            )
        }
    }

    private fun parseJsonArray(file: File): List<Map<String, Any?>> {
        val element = Json.parseToJsonElement(file.readText())
        @Suppress("UNCHECKED_CAST")
        return toNative(element) as List<Map<String, Any?>>
    }

    private fun toNative(element: JsonElement): Any? =
        when (element) {
            is JsonNull -> null
            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                element.longOrNull != null -> element.longOrNull
                element.doubleOrNull != null -> element.doubleOrNull
                else -> element.content
            }
            is JsonArray -> element.map { toNative(it) }
            is JsonObject -> element.entries.associate { (key, value) -> key to toNative(value) }
        }

    private fun toJsonElement(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.map { toJsonElement(it) })
            is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJsonElement(v) })
            else -> JsonPrimitive(value.toString())
        }
}
