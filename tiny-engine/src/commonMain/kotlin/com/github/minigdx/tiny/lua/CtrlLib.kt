package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.Key
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "ctrl",
    "Access to controllers like touch/mouse events or accessing which key is pressed by the user."
)
class CtrlLib(
    private val inputHandler: InputHandler,
    sprLib: SprLib,
) : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val ctrl = LuaTable()

        ctrl["pressed"] = pressed()
        ctrl["pressing"] = pressing()

        ctrl.set("touch", touch())
        arg2.set("ctrl", ctrl)
        arg2.get("package").get("loaded").set("ctrl", ctrl)
        return ctrl
    }

    private val spr = sprLib.draw()

    @TinyFunction(
        "Get coordinates of the current touch/mouse. " +
            "If the mouse/touch is out-of the screen, " +
            "the coordinates will be the last mouse position/touch. " +
            "The function return those coordinates as a table {x, y}. " +
            "A sprite can be draw directly on the mouse position by passing the sprite number. ",
        example = CTRL_TOUCH_EXAMPLE
    )
    inner class touch : OneArgFunction() {

        @TinyCall("Get the mouse coordinates.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Get the mouse coordinate and draw a sprite on those coordinates.")
        override fun call(@TinyArg("sprN") arg: LuaValue): LuaValue {
            val pos = inputHandler.currentTouch

            val coordinates = LuaTable().apply {
                this.set("x", pos.x.toInt())
                this.set("y", pos.y.toInt())
            }

            // return the coordinates
            return if (arg.isnil()) {
                coordinates
            } else {
                // draw the sprite at the x/y coordinate
                val sprN = arg.checkint()

                spr.call(valueOf(sprN), valueOf(pos.x.toInt()), valueOf(pos.y.toInt()))
                coordinates
            }
        }
    }

    @TinyFunction(
        "Return if the key was pressed during the last frame. " +
            "If you need to check that the key is still pressed, see ctrl.pressing instead."
    )
    inner class pressed : OneArgFunction() {

        @TinyCall("Is the key was pressed?")
        override fun call(@TinyArg("key") arg: LuaValue): LuaValue {
            val int = arg.checkint()
            // get the key by its ordinal.
            val k = Key.values()[int]
            return booleanToInt(inputHandler.isKeyJustPressed(k))
        }
    }

    @TinyFunction(
        "Return if the key is still pressed. ",
        example = CTRL_PRESSING_EXAMPLE
    )
    inner class pressing : OneArgFunction() {
        @TinyCall("Is the key is still pressed?")
        override fun call(@TinyArg("key") arg: LuaValue): LuaValue {
            val int = arg.checkint()
            // get the key by its ordinal.
            val k = Key.values()[int]
            return booleanToInt(inputHandler.isKeyPressed(k))
        }
    }

    private fun booleanToInt(keyPressed: Boolean) = if (keyPressed) {
        ONE
    } else {
        ZERO
    }
}
