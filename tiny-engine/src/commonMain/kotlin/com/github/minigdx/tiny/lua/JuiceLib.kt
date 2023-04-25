package com.github.minigdx.tiny.lua

import com.github.mingdx.tiny.doc.TinyArg
import com.github.mingdx.tiny.doc.TinyCall
import com.github.mingdx.tiny.doc.TinyFunction
import com.github.mingdx.tiny.doc.TinyLib
import com.github.minigdx.tiny.math.Interpolation
import com.github.minigdx.tiny.math.Interpolations
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.TwoArgFunction

@TinyLib(
    "juice",
    "Easing functions to 'juice' a game."
)
class JuiceLib : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
        val func = LuaTable()
        Interpolations.all
            .forEach { interpolation ->
                func.set(interpolation.toString(), InterpolationLib(interpolation))
            }
        arg2.set("juice", func)
        arg2.get("package").get("loaded").set("juice", func)
        return func
    }

    @TinyFunction(
        name = "pow2",
        description =
        """Interpolation to juice your game.
All interpolations available: 

- pow2, pow3, pow4, pow5,
- powIn2, powIn3, powIn4, powIn5,
- powOut2, powOut3, powOut4, powOut5,
- sine, sineIn, sineOut,
- circle, circleIn, circleOut,
- elastic, elasticIn, elasticOut,
- swing, swingIn, swingOut,
- bounce, bounceIn, bounceOut,
- exp10, expIn10, expOut10,
- exp5, expIn5, expOut5,
- linear      
""",
        example = JUICE_EXAMPLE
    )
    inner class InterpolationLib(private val interpolation: Interpolation) : LibFunction() {

        @TinyCall("Give a percentage (progress) of the interpolation")
        override fun call(@TinyArg("progress") a: LuaValue): LuaValue {
            return valueOf(interpolation.interpolate(a.tofloat()).toDouble())
        }

        @TinyCall("Interpolate the value given a start and an end value.")
        override fun call(
            @TinyArg("start") a: LuaValue,
            @TinyArg("end") b: LuaValue,
            @TinyArg("progress") c: LuaValue
        ): LuaValue {
            return valueOf(
                interpolation.interpolate(
                    a.tofloat(),
                    b.tofloat(),
                    c.tofloat()
                ).toDouble()
            )
        }
    }
}
