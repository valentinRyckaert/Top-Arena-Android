package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.toupiearena.mobile.core.Part
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.Projectile
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.ui.art.drawEmblem
import fr.toupiearena.mobile.ui.art.drawPart
import fr.toupiearena.mobile.ui.art.drawProjectiles
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.art.drawStadium as artStadium
import fr.toupiearena.mobile.ui.art.drawTop as artTop

// Point d'entrée unique des écrans vers l'art procédural « Holo Combat » (ui/art, porté du PC).

fun DrawScope.paintTop(top: Top, center: Offset, radius: Float, angle: Float = 0.6f, glow: Float = 0f, alpha: Float = 1f) {
    if (alpha >= 1f) return artTop(top, center, radius, angle, glow)
    drawIntoCanvas { c ->
        c.saveLayer(Rect(center, radius * 1.6f), Paint().apply { this.alpha = alpha })
        artTop(top, center, radius, angle, glow)
        c.restore()
    }
}

/** Stadium vu de dessus, inscrit dans un cercle de rayon [radius]. Dessin coûteux : à mettre en cache. */
fun DrawScope.paintStadium(stadium: Stadium, center: Offset, radius: Float) {
    val geometry = stadium.geometry(stadium.apply(BattleConfig()))
    artStadium(stadium, geometry, center, radius / geometry.radius.toFloat())
}

fun DrawScope.paintEmblem(special: String, center: Offset, size: Float, color: Color) = drawEmblem(special, center, size, color)

/** Vignette d'une toupie (« ? » en pointillés pour une toupie tirée au hasard). */
@Composable
fun TopView(top: Top?, size: Dp, modifier: Modifier = Modifier, angle: Float = 0.6f) = Canvas(modifier.size(size)) {
    val r = this.size.minDimension / 2 * 0.86f
    if (top != null) paintTop(top, center, r, angle)
    else drawCircle(Holo.cyan, r * 0.8f, center, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
}

/** Vignette d'un stadium. */
@Composable
fun StadiumView(stadium: Stadium, size: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    paintStadium(stadium, center, this.size.minDimension / 2 * 0.92f)
}

/** Vignette d'une pièce seule. */
@Composable
fun PartView(part: Part, size: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    drawPart(part, center, this.size.minDimension / 2 * 0.86f)
}

/** Coup spécial en vol, du lanceur [a] vers la cible [b] (même dessin qu'en combat). */
fun DrawScope.paintMoveScene(move: SpecialMove, a: Offset, b: Offset, r: Float) {
    val k = r / BattleConfig().topRadius.toFloat() // Échelle monde → écran : la toupie a son rayon de combat.
    val target = Vec2(((b.x - a.x) / k).toDouble(), ((b.y - a.y) / k).toDouble())
    val projectile = Projectile(move = move.id, ring = move.hitsEveryone, color = move.color, width = move.width, range = move.range,
        radius = if (move.hitsEveryone) target.length() * 0.7 else 0.0,
        position = if (move.hitsEveryone) Vec2.ZERO else target * 0.62, direction = target.normalized())
    drawProjectiles(listOf(projectile), { v -> Offset(a.x + v.x.toFloat() * k, a.y + v.y.toFloat() * k) }, k, BattleConfig().topRadius.toFloat(), 1f, 1.0)
}
