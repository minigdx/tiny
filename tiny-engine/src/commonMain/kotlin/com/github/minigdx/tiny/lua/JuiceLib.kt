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
    "Easing functions to 'juice' a game. " +
        "Interpolation to juice your game.\n" +
        "All interpolations available: \n" +
        "\n" +
        "- pow2, pow3, pow4, pow5,\n" +
        "- powIn2, powIn3, powIn4, powIn5,\n" +
        "- powOut2, powOut3, powOut4, powOut5,\n" +
        "- sine, sineIn, sineOut,\n" +
        "- circle, circleIn, circleOut,\n" +
        "- elastic, elasticIn, elasticOut,\n" +
        "- swing, swingIn, swingOut,\n" +
        "- bounce, bounceIn, bounceOut,\n" +
        "- exp10, expIn10, expOut10,\n" +
        "- exp5, expIn5, expOut5,\n" +
        "- linear "

)
class JuiceLib : TwoArgFunction() {
    @TinyFunction(name = "pow2", example = JUICE_EXAMPLE)
    @TinyFunction(name = "pow3")
    @TinyFunction(name = "pow4")
    @TinyFunction(name = "pow5")
    @TinyFunction(name = "powIn2")
    @TinyFunction(name = "powIn3")
    @TinyFunction(name = "powIn4")
    @TinyFunction(name = "powIn5")
    @TinyFunction(name = "powOut2")
    @TinyFunction(name = "powOut3")
    @TinyFunction(name = "powOut4")
    @TinyFunction(name = "powOut5")
    @TinyFunction(name = "sine")
    @TinyFunction(name = "sineIn")
    @TinyFunction(name = "sineOut")
    @TinyFunction(name = "circle")
    @TinyFunction(name = "circleIn")
    @TinyFunction(name = "circleOut")
    @TinyFunction(name = "elastic")
    @TinyFunction(name = "elasticIn")
    @TinyFunction(name = "elasticOut")
    @TinyFunction(name = "swing")
    @TinyFunction(name = "swingIn")
    @TinyFunction(name = "swingOut")
    @TinyFunction(name = "bounce")
    @TinyFunction(name = "bounceIn")
    @TinyFunction(name = "bounceOut")
    @TinyFunction(name = "exp10")
    @TinyFunction(name = "expIn10")
    @TinyFunction(name = "expOut10")
    @TinyFunction(name = "exp5")
    @TinyFunction(name = "expIn5")
    @TinyFunction(name = "expOut5")
    @TinyFunction(name = "linear")
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
}
