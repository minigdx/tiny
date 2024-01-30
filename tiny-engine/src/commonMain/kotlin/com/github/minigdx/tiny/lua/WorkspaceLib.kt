package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
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

    internal inner class save : TwoArgFunction() {

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val file = findFile(arg1) ?: return NIL
            file.save(arg2.checkjstring()?.encodeToByteArray() ?: ByteArray(0))
            return NIL
        }
    }

    private fun findFile(arg: LuaValue): LocalFile? {
        val filename = arg.checkjstring() ?: return null
        return resources.firstOrNull { it.name == filename }
    }

    internal inner class load : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val file = findFile(arg) ?: return NIL
            val content = file.readAll().decodeToString()
            return valueOf(content)
        }
    }

    internal inner class create : TwoArgFunction() {

        override fun call(@TinyArg("prefix") arg1: LuaValue, @TinyArg("extension") arg2: LuaValue): LuaValue {
            val prefix = arg1.optjstring("new")!!
            val ext = arg2.optjstring("")!!

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
            resources = resources + platform.createLocalFile(fileneameWithExt)
            return valueOf(filename)
        }
    }

    internal inner class list : OneArgFunction() {

        override fun call(): LuaValue = super.call()

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
