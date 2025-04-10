package com.github.minigdx.tiny.lua

import com.github.minigdx.tiny.engine.GameResourceAccess
import org.luaj.vm2.io.BytesLuaBinInput
import org.luaj.vm2.io.LuaBinInput
import org.luaj.vm2.lib.BaseLib

class TinyBaseLib(private val engine: GameResourceAccess) : BaseLib() {
    override fun findResource(filename: String): LuaBinInput? {
        val content = engine.script(filename)?.content ?: return null
        return BytesLuaBinInput(content)
    }
}
