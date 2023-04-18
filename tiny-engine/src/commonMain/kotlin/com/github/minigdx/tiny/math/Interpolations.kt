package com.github.minigdx.tiny.math

import com.github.minigdx.tiny.Percent
import com.github.minigdx.tiny.Seconds
import com.github.minigdx.tiny.math.Interpolations.HALF_PI
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

inline fun pow(x: Float, y: Float) = x.pow(y)

interface Interpolation {

    fun interpolate(a: Float, b: Float, percent: Percent): Float {
        return a + (b - a) * interpolate(percent)
    }

    // Underlying implementations are inspired of
    // https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Interpolation.java
    fun interpolate(percent: Percent): Float
}

private class PowInterpolation(private val power: Int) : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return if (percent <= 0.5f) pow(
            (percent * 2f),
            power.toFloat()
        ) / 2 else pow(
            ((percent - 1) * 2f),
            power.toFloat()
        ) / (if (power % 2 == 0) -2 else 2) + 1
    }

    override fun toString(): String = "pow$power"
}

private class PowInInterpolation(private val power: Int) : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return pow(percent, power.toFloat())
    }

    override fun toString(): String = "powIn$power"
}

private class PowOutInterpolation(private val power: Int) : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return pow(percent - 1, power.toFloat()) * (if (power % 2 == 0) -1 else 1) + 1
    }

    override fun toString(): String = "powOut$power"
}

private class SineInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return (1 - cos(percent * PI.toFloat())) / 2
    }

    override fun toString(): String = "sine"
}

private class SineInInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return 1.0f - cos(percent * HALF_PI)
    }

    override fun toString(): String = "sineIn"
}

private class SineOutInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return sin(percent * HALF_PI)
    }

    override fun toString(): String = "sineOut"
}

private class CircleInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        var a = percent
        if (a <= 0.5f) {
            a *= 2f
            return (1 - sqrt(1 - a * a)) / 2
        }
        a--
        a *= 2f
        return (sqrt(1 - a * a) + 1) / 2
    }

    override fun toString(): String = "circle"
}

private class CircleInInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return 1 - sqrt(1 - percent * percent)
    }

    override fun toString(): String = "circleIn"
}

private class CircleOutInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        var a = percent
        a--
        return sqrt(1 - a * a)
    }

    override fun toString(): String = "circleOut"
}

private open class ElasticInterpolation(
    protected val value: Float,
    protected val power: Float,
    bounces: Int,
    protected val scale: Float
) : Interpolation {

    protected val bounces: Float = bounces * PI.toFloat() * if (bounces % 2 == 0) 1f else -1f

    override fun interpolate(percent: Percent): Float {
        var a = percent
        if (a <= 0.5f) {
            a *= 2f
            return pow(
                value,
                (power * (a - 1))
            ) * sin(a * bounces) * scale / 2
        }
        a = 1 - a
        a *= 2f
        return 1 - pow(
            value,
            (power * (a - 1))
        ) * sin(a * bounces) * scale / 2
    }
    override fun toString(): String = "elastic"
}

private class ElasticInInterpolation(
    value: Float,
    power: Float,
    bounces: Int,
    scale: Float
) : ElasticInterpolation(value, power, bounces, scale) {

    override fun interpolate(percent: Percent): Float {
        return if (percent >= 0.99f) 1f else pow(value, power * (percent - 1)) * sin(percent * bounces) * scale
    }

    override fun toString(): String = "elasticIn"
}

private class ElasticOutInterpolation(
    value: Float,
    power: Float,
    bounces: Int,
    scale: Float
) : ElasticInterpolation(value, power, bounces, scale) {

    override fun interpolate(percent: Percent): Float {
        var a = percent
        if (a == 0f) return 0f
        a = 1f - a
        return 1f - pow(value, power * (a - 1)) * sin(a * bounces) * scale
    }

    override fun toString(): String = "elasticOut"
}

private class LinearInterpolation : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return percent
    }

    override fun toString(): String = "linear"
}

private open class ExpInterpolation(val value: Float, val power: Float) : Interpolation {

    val min: Float = pow(value, -power)
    val scale: Float = 1f / (1f - min)

    override fun interpolate(percent: Percent): Float {
        return if (percent <= 0.5f) {
            (pow(value, (power * (percent * 2f - 1f))) - min) * scale / 2f
        } else {
            (2f - (pow(value, (-power * (percent * 2f - 1f))) - min) * scale) / 2
        }
    }

    override fun toString(): String = "exp${power.toInt()}"
}

private class ExpInInterpolation(value: Float, power: Float) : ExpInterpolation(value, power) {

    override fun interpolate(percent: Percent): Float {
        return (pow(value, (power * (percent - 1f))) - min) * scale
    }

    override fun toString(): String = "expIn${power.toInt()}"
}

private class ExpOutInterpolation(value: Float, power: Float) : ExpInterpolation(value, power) {

    override fun interpolate(percent: Percent): Float {
        return 1f - (pow(value, (-power * percent)) - min) * scale
    }

    override fun toString(): String = "expOut${power.toInt()}"
}

private class Bounce(bounces: Int) : BounceOut(bounces) {

    private fun out(a: Float): Float {
        val test: Float = a + widths.get(0) / 2
        return if (test < widths.get(0)) test / (widths.get(0) / 2) - 1 else super.interpolate(a)
    }

    override fun interpolate(percent: Percent): Float {
        return if (percent <= 0.5f) (1 - out(1 - percent * 2)) / 2 else out(percent * 2 - 1) / 2 + 0.5f
    }

    override fun toString(): String = "bounce"
}

private open class BounceOut(bounces: Int) : Interpolation {

    val widths: FloatArray
    val heights: FloatArray

    init {
        require(!(bounces < 2 || bounces > 5)) { "bounces cannot be < 2 or > 5: $bounces" }
        widths = FloatArray(bounces)
        heights = FloatArray(bounces)
        heights[0] = 1f
        when (bounces) {
            2 -> {
                widths[0] = 0.6f
                widths[1] = 0.4f
                heights[1] = 0.33f
            }

            3 -> {
                widths[0] = 0.4f
                widths[1] = 0.4f
                widths[2] = 0.2f
                heights[1] = 0.33f
                heights[2] = 0.1f
            }

            4 -> {
                widths[0] = 0.34f
                widths[1] = 0.34f
                widths[2] = 0.2f
                widths[3] = 0.15f
                heights[1] = 0.26f
                heights[2] = 0.11f
                heights[3] = 0.03f
            }

            5 -> {
                widths[0] = 0.3f
                widths[1] = 0.3f
                widths[2] = 0.2f
                widths[3] = 0.1f
                widths[4] = 0.1f
                heights[1] = 0.45f
                heights[2] = 0.3f
                heights[3] = 0.15f
                heights[4] = 0.06f
            }
        }
        widths[0] *= 2f
    }

    override fun interpolate(percent: Percent): Float {
        var a = percent
        if (a == 1f) return 1f
        a += widths[0] / 2f
        var width = 0f
        var height = 0f
        var i = 0
        val n = widths.size
        while (i < n) {
            width = widths[i]
            if (a <= width) {
                height = heights[i]
                break
            }
            a -= width
            i++
        }
        a /= width
        val z = 4 / width * height * a
        return 1 - (z - z * a) * width
    }

    override fun toString(): String = "bounceOut"
}

private class BounceIn(bounces: Int) : BounceOut(bounces) {

    override fun interpolate(percent: Percent): Float {
        return 1f - super.interpolate(1f - percent)
    }

    override fun toString(): String = "bounceIn"
}

private class SwingInterpolation(scale: Float) : Interpolation {

    private val scale: Float

    init {
        this.scale = scale * 2
    }

    override fun interpolate(percent: Percent): Float {
        var a = percent
        if (a <= 0.5f) {
            a *= 2f
            return a * a * ((scale + 1f) * a - scale) / 2f
        }
        a--
        a *= 2f
        return a * a * ((scale + 1f) * a + scale) / 2f + 1f
    }

    override fun toString(): String = "swing"
}

private class SwingOutInterpolation(private val scale: Float) : Interpolation {

    override fun interpolate(percent: Percent): Float {
        var a = percent
        a--
        return a * a * ((scale + 1) * a + scale) + 1
    }

    override fun toString(): String = "swingOut"
}

private class SwingInInterpolation(private val scale: Float) : Interpolation {

    override fun interpolate(percent: Percent): Float {
        return percent * percent * ((scale + 1) * percent - scale)
    }

    override fun toString(): String = "swingIn"
}

object Interpolations {

    internal const val HALF_PI: Float = (PI * 0.5f).toFloat()

    val pow2: Interpolation = PowInterpolation(2)
    val pow3: Interpolation = PowInterpolation(3)
    val pow4: Interpolation = PowInterpolation(4)
    val pow5: Interpolation = PowInterpolation(5)

    val powIn2: Interpolation = PowInInterpolation(2)
    val powIn3: Interpolation = PowInInterpolation(3)
    val powIn4: Interpolation = PowInInterpolation(4)
    val powIn5: Interpolation = PowInInterpolation(5)

    val powOut2: Interpolation = PowOutInterpolation(2)
    val powOut3: Interpolation = PowOutInterpolation(3)
    val powOut4: Interpolation = PowOutInterpolation(4)
    val powOut5: Interpolation = PowOutInterpolation(5)

    val sin: Interpolation = SineInterpolation()
    val sinIn: Interpolation = SineInInterpolation()
    val sinOut: Interpolation = SineOutInterpolation()

    val circle: Interpolation = CircleInterpolation()
    val circleIn: Interpolation = CircleInInterpolation()
    val circleOut: Interpolation = CircleOutInterpolation()

    val elastic: Interpolation = ElasticInterpolation(2f, 10f, 7, 1f)
    val elasticIn: Interpolation = ElasticInInterpolation(2f, 10f, 6, 1f)
    val elasticOut: Interpolation = ElasticOutInterpolation(2f, 10f, 7, 1f)

    val swing: Interpolation = SwingInterpolation(1.5f)
    val swingIn: Interpolation = SwingInInterpolation(2f)
    val swingOut: Interpolation = SwingOutInterpolation(2f)

    val bounce: Interpolation = Bounce(4)
    val bounceIn: Interpolation = BounceIn(4)
    val bounceOut: Interpolation = BounceOut(4)

    val exp10: Interpolation = ExpInterpolation(2f, 10f)
    val exp10In: Interpolation = ExpInInterpolation(2f, 10f)
    val exp10Out: Interpolation = ExpOutInterpolation(2f, 10f)

    val exp5: Interpolation = ExpInterpolation(2f, 5f)
    val exp5In: Interpolation = ExpInInterpolation(2f, 5f)
    val exp5Out: Interpolation = ExpOutInterpolation(2f, 5f)

    val linear: Interpolation = LinearInterpolation()

    val all = listOf(
        pow2, pow3, pow4, pow5,
        powIn2, powIn3, powIn4, powIn5,
        powOut2, powOut3, powOut4, powOut5,

        sin, sinIn, sinOut,

        circle, circleIn, circleOut,

        elastic, elasticIn, elasticOut,

        swing, swingIn, swingOut,

        bounce, bounceIn, bounceOut,

        exp10, exp10In, exp10Out,
        exp5, exp5In, exp5Out,

        linear
    )

    fun lerp(target: Float, current: Float, step: Float = 0.9f): Float {
        return target + step * (current - target)
    }

    fun lerp(target: Float, current: Float, step: Float = 0.9f, deltaTime: Seconds): Float {
        return lerp(target, current, 1 - step.pow(deltaTime))
    }
}
