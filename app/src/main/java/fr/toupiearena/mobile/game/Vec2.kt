package fr.toupiearena.mobile.game

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Petit vecteur 2D (repère écran : Y vers le bas). */
data class Vec2(val x: Double = 0.0, val y: Double = 0.0) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun unaryMinus() = Vec2(-x, -y)
    operator fun times(s: Double) = Vec2(x * s, y * s)
    operator fun div(s: Double) = Vec2(x / s, y / s)
    fun dot(o: Vec2) = x * o.x + y * o.y
    fun length() = hypot(x, y)
    fun lengthSquared() = x * x + y * y
    fun normalized(): Vec2 {
        val len = length()
        return if (len > 1e-12) Vec2(x / len, y / len) else ZERO
    }
    /** Vecteur tourné de +90° (sens horaire à l'écran). */
    fun perpendicular() = Vec2(-y, x)
    fun angle() = atan2(y, x)

    companion object {
        val ZERO = Vec2()
        fun fromAngle(radians: Double) = Vec2(cos(radians), sin(radians))
    }
}
