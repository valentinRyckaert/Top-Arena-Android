package fr.toupiearena.mobile.ui

import android.graphics.Color as AndroidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.StadiumGeometry
import kotlin.math.cos
import kotlin.math.sin

fun hsv(hue: Int, saturation: Int, value: Int): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(hue.toFloat(), saturation / 255f, value / 255f)))

fun rgb(color: Long) = Color(0xFF000000 or color)
fun Color.lighter(f: Float) = Color((red * f).coerceAtMost(1f), (green * f).coerceAtMost(1f), (blue * f).coerceAtMost(1f), alpha)
fun Color.darker(f: Float) = Color(red / f, green / f, blue / f, alpha)

fun typeColor(type: TopType) = when (type) {
    TopType.Attack -> Color(0xFFE2433C)
    TopType.Defense -> Color(0xFF3478DC)
    TopType.Endurance -> Color(0xFF2EAA64)
    TopType.Balance -> Color(0xFFE6AF28)
}

/** Couleur de chaque place (0 à 3), comme sur PC. */
fun slotColor(slot: Int) = listOf(Palette.accent, Color(0xFFFF8A5C), Color(0xFF5CC8FF), Color(0xFFD58CFF))[slot.coerceIn(0, 3)]

/** Toupie vue du dessus : roue à lames, partie intermédiaire, pointe et boulon (couleur du coup spécial). */
fun DrawScope.drawTop(top: Top, center: Offset, radius: Float, angle: Double, glow: Float = 0f, alpha: Float = 1f) {
    val accent = typeColor(top.type)
    if (glow > 0f) {
        drawCircle(Brush.radialGradient(listOf(accent.lighter(1.3f).copy(alpha = 0.55f * glow), Color.Transparent),
            center, radius * 1.8f), radius * 1.8f, center)
    }
    drawCircle(Color.Black.copy(alpha = 0.27f * alpha), radius, center + Offset(radius * 0.12f, radius * 0.16f))
    translate(center.x, center.y) {
        rotateRad(angle.toFloat(), Offset.Zero) {
            val wheel = hsv(top.wheel.hue, 90, 210)
            drawCircle(Brush.radialGradient(listOf(wheel.lighter(1.5f), wheel.darker(1.7f)),
                Offset(-radius * 0.3f, -radius * 0.3f), radius * 1.4f), radius, Offset.Zero, alpha)
            drawCircle(accent.darker(1.5f), radius, Offset.Zero, alpha, Stroke(maxOf(1f, radius * 0.08f)))
            repeat(3) { i ->
                val a = i * 2.0 * Math.PI / 3
                rotateRad(a.toFloat(), Offset.Zero) {
                    val blade = Path().apply {
                        moveTo(radius * 0.35f, -radius * 0.10f)
                        quadraticTo(radius * 0.85f, -radius * 0.45f, radius * 0.98f, 0f)
                        quadraticTo(radius * 0.75f, radius * 0.05f, radius * 0.35f, radius * 0.12f)
                        close()
                    }
                    drawPath(blade, wheel.darker(2f), alpha)
                }
            }
            val middle = hsv(top.middle.hue, 170, 200)
            drawCircle(middle, radius * 0.48f, Offset.Zero, alpha)
            drawCircle(middle.darker(1.6f), radius * 0.48f, Offset.Zero, alpha, Stroke(maxOf(1f, radius * 0.05f)))
            drawLine(Color.White.copy(alpha = alpha), Offset(radius * 0.12f, 0f), Offset(radius * 0.42f, 0f),
                maxOf(1f, radius * 0.08f), StrokeCap.Round)
            drawCircle(hsv(top.tip.hue, 200, 230), radius * 0.24f, Offset.Zero, alpha)
            val boltColor = SpecialMove.find(top.special)?.let { rgb(it.color) } ?: hsv(top.bolt.hue, 120, 220)
            val nut = Path().apply {
                for (k in 0 until 6) {
                    val a = Math.toRadians(60.0 * k + 30.0)
                    val p = Offset((cos(a) * radius * 0.2).toFloat(), (sin(a) * radius * 0.2).toFloat())
                    if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(nut, boltColor, alpha)
            drawCircle(boltColor.darker(1.4f), radius * 0.07f, Offset.Zero, alpha)
        }
    }
}

/** Contour du stadium dans le repère écran. */
fun stadiumPath(geometry: StadiumGeometry, center: Offset, scale: Float, grow: Float = 1f): Path = Path().apply {
    geometry.outline().forEachIndexed { i, p ->
        val x = center.x + (p.x * scale * grow).toFloat()
        val y = center.y + (p.y * scale * grow).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Stadium : rebord (ou lave / vide), sol en dégradé et cercles de repère. */
fun DrawScope.drawStadium(stadium: Stadium, geometry: StadiumGeometry, center: Offset, scale: Float) {
    val rim = rgb(stadium.rimColor)
    if (stadium.hasRim) drawPath(stadiumPath(geometry, center, scale, 1.07f), rim)
    else drawPath(stadiumPath(geometry, center, scale, 1.05f), rim.copy(alpha = 0.55f))
    val radius = (geometry.radius * scale).toFloat()
    drawPath(stadiumPath(geometry, center, scale), Brush.radialGradient(
        listOf(rgb(stadium.floorCenter), rgb(stadium.floorEdge)), center, radius * 1.05f))
    for (f in listOf(0.33f, 0.66f)) drawPath(stadiumPath(geometry, center, scale, f), Color.White.copy(alpha = 0.18f), style = Stroke(1.5f))
}
