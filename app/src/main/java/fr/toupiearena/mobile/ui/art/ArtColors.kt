package fr.toupiearena.mobile.ui.art

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Couleurs « Holo Combat » de la version PC (src/ui/Theme.h) utilisées par les dessins.
 *
 * Unités : les longueurs fixes du PC sont des pixels logiques Qt, soit des dp ici ; les
 * dessins les multiplient par `density`. Les longueurs relatives au rayon sont reprises telles quelles.
 */
object Holo {
    val Void = Color(0xFF05040F)
    val Field = Color(0xFF07061A)
    val AccentBright = Color(0xFFB8FF2E)
    val Cyan = Color(0xFF34D6FF)
    val Magenta = Color(0xFFC23BFF)
    val Warning = Color(0xFFFFC23D)
}

/** QColor::fromRgb(value) avec une opacité 0-255. */
internal fun rgb(value: Long, alpha: Int = 255) = Color(((alpha.toLong() and 0xFF) shl 24) or (value and 0xFFFFFF))

/** QColor::fromHslF, teinte en degrés ramenée dans 0-359. */
internal fun hsl(hue: Int, s: Float, l: Float, a: Float = 1f) = Color.hsl((((hue % 360) + 360) % 360).toFloat(), s, l, a)

/** withAlpha du PC : opacité bornée à 0-1. */
internal fun Color.withAlpha(a: Float) = copy(alpha = a.coerceIn(0f, 1f))

/** QColor::lighter(factor) : valeur HSV × factor / 100, l'excès est retiré de la saturation. */
internal fun Color.lighter(factor: Int) = scaleValue(factor / 100f)

/** QColor::darker(factor) : valeur HSV × 100 / factor. */
internal fun Color.darker(factor: Int) = scaleValue(100f / factor)

private fun Color.scaleValue(f: Float): Color {
    val max = maxOf(red, green, blue)
    val d = max - minOf(red, green, blue)
    var s = if (max > 0f) d / max else 0f
    var v = max * f
    if (v > 1f) {
        s = (s - (v - 1f)).coerceAtLeast(0f)
        v = 1f
    }
    val h = when {
        d == 0f -> 0f
        max == red -> ((green - blue) / d).mod(6f)
        max == green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return Color.hsv(h, s, v, alpha)
}

/**
 * Équivalent d'un QPen : extrémités carrées et jonctions biseautées par défaut ; les tirets
 * sont, comme sous Qt, exprimés en multiples de l'épaisseur.
 */
internal fun pen(
    width: Float,
    cap: StrokeCap = StrokeCap.Square,
    join: StrokeJoin = StrokeJoin.Bevel,
    dashes: FloatArray? = null,
) = Stroke(width, cap = cap, join = join,
    pathEffect = dashes?.let { d -> PathEffect.dashPathEffect(FloatArray(d.size) { d[it] * width }) })

/**
 * Theme::strokeGlow : halo néon (trois contours élargis d'opacité fixe 26, 36, 46), puis contour net.
 * `opacity` reproduit QPainter::setOpacity.
 */
internal fun DrawScope.strokeGlow(path: Path, color: Color, width: Float, opacity: Float = 1f) {
    for (k in 3 downTo 1) {
        drawPath(path, color.copy(alpha = (26 + 10 * (3 - k)) / 255f), opacity,
            Stroke(width + k * width * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawPath(path, color, opacity, Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
