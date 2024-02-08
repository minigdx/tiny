package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.file.LocalFile
import com.github.minigdx.tiny.platform.Platform
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib("ws", "Workspace manipulation library. It allows you to save/load/download files")
class WorkspaceLib(
    private var resources: List<LocalFile> = DEFAULT,
    private val platform: Platform,
) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ws = LuaTable()
        ws["save"] = save()
        ws["list"] = list()
        ws["create"] = create()
        ws["load"] = load()
        ws["download"] = download()
        arg2.set("ws", ws)
        arg2.get("package").get("loaded").set("ws", ws)
        return ws
    }

    @TinyFunction(
        "Save the content into a local file, " +
            "on desktop or in the local storage on the web platform.",
    )
    internal inner class save : TwoArgFunction() {

        @TinyCall("Save the content into the file name.")
        override fun call(@TinyArg("name") arg1: LuaValue, @TinyArg("content") arg2: LuaValue): LuaValue {
            val file = findFile(arg1) ?: return NIL
            file.save(arg2.checkjstring()?.encodeToByteArray() ?: ByteArray(0))
            return NIL
        }
    }

    private fun findFile(arg: LuaValue): LocalFile? {
        val filename = arg.checkjstring() ?: return null
        return resources.firstOrNull { it.name == filename }
    }

    @TinyFunction("Load and get the content of the file name")
    internal inner class load : OneArgFunction() {

        @TinyCall("Load and get the content of the file name")
        override fun call(@TinyArg("name") arg: LuaValue): LuaValue {
            val file = findFile(arg) ?: return NIL
            val content = file.readAll()?.decodeToString() ?: return NIL
            return valueOf(content)
        }
    }

    @TinyFunction("Create a local file. The name is generated so the name is unique.")
    internal inner class create : TwoArgFunction() {

        @TinyCall("Create a local file with the prefix and the extension. The name of the file created.")
        override fun call(@TinyArg("prefix") arg1: LuaValue, @TinyArg("extension") arg2: LuaValue): LuaValue {
            val prefix = arg1.optjstring("new")!!
            val ext = arg2.optjstring("")!!

            val (filename, filenameWithExt) = findAvailableName(prefix, ext)

            resources = resources + platform.createLocalFile(filenameWithExt)
            return valueOf(filename)
        }

        private fun findAvailableName(prefix: String, ext: String): Pair<String, String> {
            var nameAvailable = false
            var index = 0
            var filename = ""

            while (!nameAvailable) {
                filename = "$prefix-$index"

                if (findFile(valueOf(filename)) != null) {
                    index++
                } else {
                    nameAvailable = true
                }
            }
            val fileneameWithExt = if (ext.isBlank()) {
                filename
            } else {
                "$filename.$ext"
            }
            return Pair(filename, fileneameWithExt)
        }
    }

    @TinyFunction("List all files available in the workspace.")
    internal inner class list : OneArgFunction() {

        @TinyCall("List all files available in the workspace.")
        override fun call(): LuaValue = super.call()

        @TinyCall("List all files available in the workspace and filter by the file extension.")
        override fun call(@TinyArg("extension") arg: LuaValue): LuaValue {
            val ext = arg.optjstring(null).let { it?.lowercase() }
            val result = LuaTable()
            resources.forEach {
                if ((ext == null || it.name.endsWith(ext))) {
                    result.insert(0, valueOf(it.name))
                }
            }
            return result
        }
    }

    internal inner class download : OneArgFunction() {

        override fun call(arg: LuaValue): LuaValue {
            return NIL
        }
    }

    companion object {
        var DEFAULT = emptyList<LocalFile>()
    }
}
