package com.github.minigdx.tiny.cli.debug

import org.luaj.vm2.LuaClosure

class Breakpoint(
    var script: String = "unset",
    var line: Int = -1,
    var pc: Int = -1,
    var init: Boolean = false,
    var enabled: Boolean = false,
    var function: LuaClosure? = null,
    /**
     * Is the breakpoint temporary? (ie: when hit, will be deactivated)
     */
    var temporary: Boolean = false,
) {

    fun hit(other: Breakpoint): Boolean {
        return enabled &&
            other.enabled &&
            (script == other.script) &&
            (function == other.function) &&
            (pc == other.pc)
    }
}
