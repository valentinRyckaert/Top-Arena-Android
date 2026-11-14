package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.ui.holo.Holo

// Point d'entrée unique des écrans vers l'art procédural (toupies, stadiums) : les écrans ne
// dépendent que de ces fonctions.

fun DrawScope.paintTop(top: Top, center: Offset, radius: Float, angle: Float = 0.6f, glow: Float = 0f, alpha: Float = 1f) =
    drawTop(top, center, radius, angle.toDouble(), glow, alpha)

/** Stadium vu de dessus, inscrit dans un cercle de rayon [radius]. */
fun DrawScope.paintStadium(stadium: Stadium, center: Offset, radius: Float) {
    val geometry = stadium.geometry(stadium.apply(BattleConfig()))
    drawStadium(stadium, geometry, center, radius / geometry.radius.toFloat())
}

/** Vignette d'une toupie (« ? » en pointillés pour une toupie tirée au hasard). */
@Composable
fun TopView(top: Top?, size: Dp, modifier: Modifier = Modifier, angle: Float = 0.6f) = Canvas(modifier.size(size)) {
    val r = this.size.minDimension / 2 * 0.86f
    if (top != null) paintTop(top, center, r, angle)
    else drawCircle(Holo.cyan, r * 0.8f, center, style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
}

/** Vignette d'un stadium. */
@Composable
fun StadiumView(stadium: Stadium, size: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    paintStadium(stadium, center, this.size.minDimension / 2 * 0.92f)
}

/** Emblème du coup spécial (boulon). */
fun DrawScope.paintEmblem(special: String, center: Offset, size: Float, color: androidx.compose.ui.graphics.Color) =
    drawCircle(color, size * 0.6f, center, style = Stroke(size * 0.16f))

/** Coup spécial en vol, du lanceur [a] vers la cible [b] (anneau autour du lanceur, ou vague). */
fun DrawScope.paintMoveScene(move: fr.toupiearena.mobile.game.SpecialMove, a: Offset, b: Offset, r: Float) {
    val col = moveColor(move)
    if (move.hitsEveryone) for (k in 1..3) drawCircle(col.copy(alpha = 1f - k * 0.25f), r * (1.2f + k * 0.8f), a, style = Stroke(r * 0.12f))
    else drawLine(col, a, b, r * 0.18f)
}
