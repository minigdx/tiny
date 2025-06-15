package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.input.InputHandler
import com.github.minigdx.tiny.input.Key
import com.github.minigdx.tiny.input.TouchSignal
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "ctrl",
    "Access to controllers like touch/mouse events or accessing which key is pressed by the user.",
)
class CtrlLib(
    private val inputHandler: InputHandler,
    sprLib: SprLib,
) : TwoArgFunction() {
    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
        val ctrl = LuaTable()

        ctrl["pressed"] = pressed()
        ctrl["pressing"] = pressing()
        ctrl["touch"] = touch()
        ctrl["touched"] = touched()
        ctrl["touching"] = touching()

        arg2["ctrl"] = ctrl
        arg2["package"]["loaded"]["ctrl"] = ctrl
        return ctrl
    }

    private val spr = sprLib.draw()

    @TinyFunction(
        "Get coordinates of the current touch/mouse. " +
            "If the mouse/touch is out-of the screen, " +
            "the coordinates will be the last mouse position/touch. " +
            "The function return those coordinates as a table {x, y}. " +
            "A sprite can be draw directly on the mouse position by passing the sprite number. ",
        example = CTRL_TOUCH_EXAMPLE,
    )
    inner class touch : OneArgFunction() {
        @TinyCall("Get the mouse coordinates.")
        override fun call(): LuaValue = super.call()

        @TinyCall("Get the mouse coordinate and draw a sprite on those coordinates.")
        override fun call(
            @TinyArg("sprN") arg: LuaValue,
        ): LuaValue {
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
        "Return true if the key was pressed during the last frame. " +
            "If you need to check that the key is still pressed, see `ctrl.pressing` instead.",
        example = CTRL_PRESSING_EXAMPLE,
    )
    inner class pressed : OneArgFunction() {

        private val values = Key.entries.toTypedArray()

        @TinyCall("Is the key was pressed?")
        override fun call(
            @TinyArg("key") arg: LuaValue,
        ): LuaValue {
            val int = arg.checkint()
            if (int >= values.size || int < 0) return BFALSE

            // get the key by its ordinal.
            val k = values[int]
            return valueOf(inputHandler.isKeyJustPressed(k))
        }
    }

    @TinyFunction(
        "Return true if the key is still pressed. ",
        example = CTRL_PRESSING_EXAMPLE,
    )
    inner class pressing : OneArgFunction() {
        @TinyCall("Is the key is still pressed?")
        override fun call(
            @TinyArg("key") arg: LuaValue,
        ): LuaValue {
            val values = Key.values()
            val int = arg.checkint()
            if (int >= values.size || int < 0) return BFALSE
            // get the key by its ordinal.
            val k = values[int]
            return valueOf(inputHandler.isKeyPressed(k))
        }
    }

    @TinyFunction(
        "Return the position of the touch (as `{x, y}`)" +
            "if the screen was touched or the mouse button was pressed during the last frame. " +
            "`nil` otherwise.\n" +
            "The touch can be : \n\n" +
            "- 0: left click or one finger\n" +
            "- 1: right click or two fingers\n" +
            "- 2: middle click or three fingers\n\n" +
            "If you need to check that the touch/mouse button is still active, see `ctrl.touching` instead.",
        example = CTRL_TOUCHED_EXAMPLE,
    )
    inner class touched : OneArgFunction() {
        @TinyCall("Is the screen was touched or mouse button was pressed?")
        override fun call(
            @TinyArg("touch") arg: LuaValue,
        ): LuaValue {
            val values = TouchSignal.values()
            val int = arg.checkint()
            if (int >= values.size || int < 0) return BFALSE
            // get the key by its ordinal.
            val touchSignal = TouchSignal.values()[int]
            val touched = inputHandler.isJustTouched(touchSignal)

            val coordinates =
                touched?.let {
                    val result = LuaTable()
                    result["x"] = touched.x.toInt()
                    result["y"] = touched.y.toInt()
                    result
                } ?: NIL

            return coordinates
        }
    }

    @TinyFunction(
        "Return the position of the touch (as `{x, y}`)" +
            "if the screen is still touched or the mouse button is still pressed. " +
            "`nil` otherwise.\n" +
            "The touch can be : \n\n" +
            "- 0: left click or one finger\n" +
            "- 1: right click or two fingers\n" +
            "- 2: middle click or three fingers\n\n",
        example = CTRL_TOUCHING_EXAMPLE,
    )
    inner class touching : OneArgFunction() {
        @TinyCall("Is the screen is still touched or mouse button is still pressed?")
        override fun call(
            @TinyArg("touch") arg: LuaValue,
        ): LuaValue {
            val values = TouchSignal.values()
            val int = arg.checkint()
            if (int >= values.size || int < 0) return BFALSE
            // get the key by its ordinal.
            val touchSignal = TouchSignal.values()[int]
            val touched = inputHandler.isTouched(touchSignal)

            val coordinates =
                touched?.let {
                    val result = LuaTable()
                    result["x"] = touched.x.toInt()
                    result["y"] = touched.y.toInt()
                    result
                } ?: NIL

            return coordinates
        }
    }
}
