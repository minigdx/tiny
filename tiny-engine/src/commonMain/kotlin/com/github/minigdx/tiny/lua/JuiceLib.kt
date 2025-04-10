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
        "- linear ",
)
class JuiceLib : TwoArgFunction() {
    @TinyFunction(name = "pow2", example = JUICE_EXAMPLE)
    @TinyFunction(name = "pow3", example = JUICE_EXAMPLE)
    @TinyFunction(name = "pow4", example = JUICE_EXAMPLE)
    @TinyFunction(name = "pow5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powIn2", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powIn3", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powIn4", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powIn5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powOut2", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powOut3", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powOut4", example = JUICE_EXAMPLE)
    @TinyFunction(name = "powOut5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "sine", example = JUICE_EXAMPLE)
    @TinyFunction(name = "sineIn", example = JUICE_EXAMPLE)
    @TinyFunction(name = "sineOut", example = JUICE_EXAMPLE)
    @TinyFunction(name = "circle", example = JUICE_EXAMPLE)
    @TinyFunction(name = "circleIn", example = JUICE_EXAMPLE)
    @TinyFunction(name = "circleOut", example = JUICE_EXAMPLE)
    @TinyFunction(name = "elastic", example = JUICE_EXAMPLE)
    @TinyFunction(name = "elasticIn", example = JUICE_EXAMPLE)
    @TinyFunction(name = "elasticOut", example = JUICE_EXAMPLE)
    @TinyFunction(name = "swing", example = JUICE_EXAMPLE)
    @TinyFunction(name = "swingIn", example = JUICE_EXAMPLE)
    @TinyFunction(name = "swingOut", example = JUICE_EXAMPLE)
    @TinyFunction(name = "bounce", example = JUICE_EXAMPLE)
    @TinyFunction(name = "bounceIn", example = JUICE_EXAMPLE)
    @TinyFunction(name = "bounceOut", example = JUICE_EXAMPLE)
    @TinyFunction(name = "exp10", example = JUICE_EXAMPLE)
    @TinyFunction(name = "expIn10", example = JUICE_EXAMPLE)
    @TinyFunction(name = "expOut10", example = JUICE_EXAMPLE)
    @TinyFunction(name = "exp5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "expIn5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "expOut5", example = JUICE_EXAMPLE)
    @TinyFunction(name = "linear", example = JUICE_EXAMPLE)
    inner class InterpolationLib(private val interpolation: Interpolation) : LibFunction() {
        @TinyCall("Give a percentage (progress) of the interpolation")
        override fun call(
            @TinyArg("progress") a: LuaValue,
        ): LuaValue {
            return valueOf(interpolation.interpolate(a.tofloat()).toDouble())
        }

        @TinyCall("Interpolate the value given a start and an end value.")
        override fun call(
            @TinyArg("start") a: LuaValue,
            @TinyArg("end") b: LuaValue,
            @TinyArg(
                "progress",
                "Progress value. " +
                    "Needs to be between 0 (start of the interpolation) " +
                    "and 1 (end of the interpolation)",
            )
            c: LuaValue,
        ): LuaValue {
            return valueOf(
                interpolation.interpolate(
                    a.tofloat(),
                    b.tofloat(),
                    c.tofloat(),
                ).toDouble(),
            )
        }
    }

    override fun call(
        arg1: LuaValue,
        arg2: LuaValue,
    ): LuaValue {
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
