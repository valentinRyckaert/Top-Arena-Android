package fr.toupiearena.mobile.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.toupiearena.mobile.core.Part
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.core.SpinDirection
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.game.SpecialMove
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// Portage de src/ui/TopPainter.cpp (version PC, charte « Holo Combat »).

private const val TAU = 2.0 * PI
private val TAUF = TAU.toFloat()
private val PIF = PI.toFloat()

private fun frac(x: Double) = x - floor(x)

private fun smooth(a: Double, b: Double, x: Double): Double {
    val t = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
}

/** Silhouette de chaque roue métallique : rayon relatif r(θ) ∈ [0, 1]. */
private val wheelProfiles: Map<String, (Double) -> Double> = mapOf(
    "RM1" to { t -> // Rempart : six boucliers
        val u = frac(t * 6 / TAU)
        0.8 + 0.2 * smooth(0.06, 0.16, u) * (1 - smooth(0.84, 0.94, u))
    },
    "RM2" to { t -> // Lame Cyclone : trois crochets
        val u = frac(t * 3 / TAU)
        0.72 + 0.28 * u.pow(2.4) * (1 - smooth(0.96, 1.0, u))
    },
    "RM3" to { t -> // Bastion : huit bastions
        val u = frac(t * 8 / TAU)
        0.86 + 0.14 * smooth(0.12, 0.25, u) * (1 - smooth(0.75, 0.88, u))
    },
    "RM4" to { t -> // Éclipse : disque échancré
        0.97 - 0.22 * max(0.0, cos(t - 0.6)).pow(60) - 0.1 * max(0.0, cos(t + 2.5)).pow(60)
    },
    "RM5" to { t -> // Prisme : hexagone cristallin
        val a = frac(t * 6 / TAU) * TAU / 6 - PI / 6
        cos(PI / 6) / cos(a) * 0.95
    },
    "RM6" to { t -> // Tonnerre : cinq éclairs
        val u = frac(t * 5 / TAU)
        val z = if (u < 0.5) u * 2 else if (u < 0.62) 0.55 else 0.55 + (u - 0.62) * 1.18
        0.7 + 0.3 * z * (1 - smooth(0.95, 1.0, u))
    },
    "RM7" to { t -> // Ouragan : quatre ailes
        val u = frac(t * 4 / TAU)
        0.72 + 0.28 * sin(PI * u.pow(0.6)).pow(1.5)
    },
    "RM8" to { t -> // Orbite : deux masses
        0.84 + 0.16 * max(0.0, cos(2 * t)).pow(8)
    },
    "RM9" to { t -> // Météore : deux éperons
        val u = frac(t * 2 / TAU)
        0.7 + 0.3 * u.pow(3.5) * (1 - smooth(0.97, 1.0, u))
    },
    "RM10" to { t -> // Titan : quatre butoirs
        val u = frac(t * 4 / TAU)
        0.82 + 0.18 * smooth(0.1, 0.18, u) * (1 - smooth(0.62, 0.7, u))
    },
    "RM11" to { t -> // Nébuleuse : bord ondulé
        0.95 + 0.035 * sin(t * 14) + 0.015 * sin(t * 5 + 1)
    },
    "RM12" to { t -> // Faucon : trois serres
        val u = frac(t * 3 / TAU)
        0.7 + 0.3 * max(0.0, sin(PI * u.pow(2.2))).pow(3)
    },
    "RM13" to { t -> // Zéphyr : six ailettes
        val u = frac(t * 6 / TAU)
        0.8 + 0.2 * u.pow(1.2) * (1 - smooth(0.9, 1.0, u))
    },
    "RM14" to { t -> // Golem : sept rocs
        val u = frac(t * 7 / TAU)
        val i = floor(t * 7 / TAU)
        0.84 + (0.1 + 0.06 * sin(i * 2.7)) * smooth(0.05, 0.2, u) * (1 - smooth(0.7, 0.92, u))
    },
)
private val defaultProfile: (Double) -> Double = { t -> 0.86 + 0.14 * max(0.0, cos(3 * t)).pow(4) }

private fun wheelProfile(id: String) = wheelProfiles[id] ?: defaultProfile

private data class WheelKey(val id: String, val mirror: Boolean, val scale: Float, val radius: Float)

private val wheelCache = HashMap<WheelKey, Path>()

/**
 * Contour d'une roue de rayon `radius` × `scale`, non tournée (mis en cache : 200 points par roue) ;
 * la roue horaire est en miroir, pour que le côté tranchant de ses lames soit à l'avant.
 */
private fun wheelPath(id: String, mirror: Boolean, scale: Float, radius: Float): Path {
    if (wheelCache.size > 64) wheelCache.clear() // Rayons variables (animations) : cache borné.
    return wheelCache.getOrPut(WheelKey(id, mirror, scale, radius)) {
        val r = wheelProfile(id)
        Path().apply {
            for (i in 0 until 200) {
                val t = i * TAU / 200
                val rr = r(if (mirror) TAU - t else t) * scale * radius
                val x = (cos(t) * rr).toFloat()
                val y = (sin(t) * rr).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}

/**
 * Chemin de travail réutilisé pour les formes recalculées à chaque image : le canevas copie le
 * tracé à l'enregistrement, on peut donc le vider aussitôt dessiné (pas d'allocation par image).
 */
private val scratch = Path()

internal inline fun shape(path: Path = Path(), build: Path.() -> Unit): Path = path.apply {
    reset()
    build()
    close()
}

internal fun Path.moveTo(p: Offset) = moveTo(p.x, p.y)
internal fun Path.lineTo(p: Offset) = lineTo(p.x, p.y)
internal fun Offset.rotated(a: Float) = Offset(x * cos(a) - y * sin(a), x * sin(a) + y * cos(a))
private fun polar(a: Float, r: Float) = Offset(cos(a) * r, sin(a) * r)
private fun degrees(radians: Float) = radians * 180f / PIF

/** Bande d'anneau entre les rayons r1 et r2 (degrés, sens de Qt : anti-horaire à l'écran). */
private fun Path.arcBand(r1: Float, r2: Float, start: Float, span: Float) {
    // Qt compte les angles vers le haut, Compose vers le bas : on change les signes.
    arcTo(Rect(-r2, -r2, r2, r2), -start, -span, forceMoveTo = true)
    arcTo(Rect(-r1, -r1, r1, r1), -(start + span), span, forceMoveTo = false)
    close()
}

private val emblemCache = HashMap<String, Path>()

/** Emblème abstrait de chaque coup (formes pleines dans [-1, 1]²), sauf la lance. */
private fun emblemPath(special: String): Path = emblemCache.getOrPut(special) {
    val p = Path()
    when (special) {
        "onde" -> { // Ondes en croissants autour d'un noyau.
            p.addOval(Rect(Offset.Zero, 0.2f))
            p.arcBand(0.36f, 0.52f, 110f, 290f)
            p.arcBand(0.66f, 0.78f, -40f, 240f)
            p.arcBand(0.88f, 0.97f, 150f, 130f)
            p
        }
        "givre" -> { // Cristal : six losanges inégaux.
            var union = p
            for (k in 0 until 6) {
                val a = k * PIF / 3 - PIF / 2
                val len = if (k % 2 == 1) 0.62f else 1f
                val w = if (k % 2 == 1) 0.13f else 0.17f
                val d = Offset(cos(a), sin(a))
                val n = Offset(-d.y, d.x)
                val shard = shape {
                    moveTo(d * 0.1f); lineTo(d * (len * 0.55f) + n * w); lineTo(d * len); lineTo(d * (len * 0.55f) - n * w)
                }
                union = Path.combine(PathOperation.Union, union, shard)
            }
            union
        }
        "vortex" -> { // Trois lames en spirale.
            for (k in 0 until 3) {
                val steps = 24
                for (i in 0..steps) { // Bord extérieur…
                    val t = i.toFloat() / steps
                    val q = polar(k * TAUF / 3 + t * 2.3f, 0.12f + 0.86f * t)
                    if (i == 0) p.moveTo(q) else p.lineTo(q)
                }
                for (i in steps downTo 0) { // … puis intérieur, effilé aux deux bouts.
                    val t = i.toFloat() / steps
                    val bulge = sin(PIF * t)
                    p.lineTo(polar(k * TAUF / 3 + t * 2.3f - 0.25f * bulge, 0.12f + 0.86f * t - 0.32f * bulge))
                }
                p.close()
            }
            p
        }
        "distorsion" -> { // Œil découpé en bandes décalées.
            val eye = Path().apply {
                moveTo(-0.95f, 0f)
                cubicTo(-0.45f, -0.95f, 0.45f, -0.95f, 0.95f, 0f)
                cubicTo(0.45f, 0.95f, -0.45f, 0.95f, -0.95f, 0f)
                close()
            }
            val iris = Path.combine(PathOperation.Difference, eye, Path().apply { addOval(Rect(Offset.Zero, 0.3f)) })
            val cuts = floatArrayOf(-0.8f, -0.2f, 0.16f, 0.8f)
            val shifts = floatArrayOf(-0.18f, 0.14f, -0.08f)
            for (i in 0 until 3) {
                val band = Path().apply { addRect(Rect(-1.2f, cuts[i] + 0.035f, 1.2f, cuts[i + 1] - 0.035f)) }
                p.addPath(Path.combine(PathOperation.Intersect, iris, band), Offset(shifts[i], 0f))
            }
            p.addOval(Rect(Offset(0.14f, 0f), 0.13f))
            p
        }
        "siphon" -> { // Deux crocs et une goutte.
            for (side in floatArrayOf(-1f, 1f)) {
                p.moveTo(side * 0.78f, -0.62f)
                p.cubicTo(side * 0.72f, -0.05f, side * 0.45f, 0.45f, side * 0.16f, 0.85f)
                p.cubicTo(side * 0.28f, 0.35f, side * 0.34f, -0.15f, side * 0.36f, -0.62f)
                p.close()
            }
            p.moveTo(0f, -0.72f)
            p.cubicTo(0.12f, -0.5f, 0.2f, -0.34f, 0.2f, -0.22f)
            p.arcTo(Rect(-0.2f, -0.42f, 0.2f, -0.02f), 0f, 180f, forceMoveTo = false)
            p.cubicTo(-0.2f, -0.34f, -0.12f, -0.5f, 0f, -0.72f)
            p
        }
        "seisme" -> { // Plaques séparées par une faille.
            fun plate(vararg xy: Float) {
                p.moveTo(xy[0], xy[1])
                for (i in 2 until xy.size step 2) p.lineTo(xy[i], xy[i + 1])
                p.close()
            }
            plate(-0.95f, 0.62f, -0.55f, -0.3f, -0.16f, -0.02f, -0.34f, 0.62f)
            plate(-0.22f, 0.62f, 0f, -0.8f, 0.36f, -0.12f, 0.16f, 0.62f)
            plate(0.28f, 0.62f, 0.47f, 0.02f, 0.95f, 0.62f)
            p
        }
        else -> p.apply { addOval(Rect(Offset.Zero, 0.4f)) }
    }
}

/** Lance d'acier : dague à lame biseautée (une face claire, une sombre), arête et éclat. */
private fun DrawScope.drawDagger(color: Color, line: Float, opacity: Float) {
    val edgeColor = Color(0xFF141840)
    val edge = pen(line)
    drawPath(shape(scratch) { moveTo(0f, -0.98f); lineTo(0f, 0.36f); lineTo(-0.13f, 0.36f); lineTo(-0.18f, -0.22f) },
        Brush.linearGradient(0f to Color.White, 1f to Color(0xFFB8C6FF), start = Offset(-0.18f, -0.9f), end = Offset(0f, 0.4f)),
        alpha = opacity)
    drawPath(shape(scratch) { moveTo(0f, -0.98f); lineTo(0.18f, -0.22f); lineTo(0.13f, 0.36f); lineTo(0f, 0.36f) },
        Brush.linearGradient(0f to Color(0xFFA8B4F0), 1f to Color(0xFF3A4590), start = Offset(0f, -0.9f), end = Offset(0.18f, 0.4f)),
        alpha = opacity)
    drawPath(shape(scratch) {
        moveTo(0f, -0.98f); lineTo(0.18f, -0.22f); lineTo(0.13f, 0.36f); lineTo(-0.13f, 0.36f); lineTo(-0.18f, -0.22f)
    }, edgeColor, opacity, edge)
    drawLine(Color(255, 255, 255, 230), Offset(0f, -0.9f), Offset(0f, 0.3f), line * 0.8f, StrokeCap.Round, alpha = opacity) // Arête.

    // Garde incurvée, poignée et pommeau, teintés de la couleur du coup.
    val tint = Brush.linearGradient(0f to color.lighter(130), 1f to color.darker(160), start = Offset(0f, 0.3f), end = Offset(0f, 0.6f))
    val guard = shape(scratch) {
        moveTo(-0.5f, 0.3f); quadraticTo(0f, 0.5f, 0.5f, 0.3f); lineTo(0.46f, 0.42f); quadraticTo(0f, 0.6f, -0.46f, 0.42f)
    }
    drawPath(guard, tint, opacity)
    drawPath(guard, edgeColor, opacity, edge)
    drawRect(Color(0xFF262A5A), Offset(-0.07f, 0.5f), Size(0.14f, 0.28f), opacity)
    drawRect(edgeColor, Offset(-0.07f, 0.5f), Size(0.14f, 0.28f), opacity, edge)
    val pommel = shape(scratch) { moveTo(0f, 0.76f); lineTo(0.12f, 0.88f); lineTo(0f, 1f); lineTo(-0.12f, 0.88f) }
    drawPath(pommel, tint, opacity)
    drawPath(pommel, edgeColor, opacity, edge)

    // Éclat à quatre branches près de la pointe.
    val g = Offset(-0.08f, -0.58f)
    drawPath(shape(scratch) {
        moveTo(g + Offset(0f, -0.2f)); lineTo(g + Offset(0.035f, -0.035f)); lineTo(g + Offset(0.2f, 0f))
        lineTo(g + Offset(0.035f, 0.035f)); lineTo(g + Offset(0f, 0.2f)); lineTo(g + Offset(-0.035f, 0.035f))
        lineTo(g + Offset(-0.2f, 0f)); lineTo(g + Offset(-0.035f, -0.035f))
    }, Color.White, opacity)
}

/** Partie intermédiaire centrée sur l'origine, motif tourné de `rot`. */
private fun DrawScope.drawMiddle(middle: Part, R: Float, rot: Float) {
    val px = density
    val col = hsl(middle.hue, 0.95f, 0.62f)
    val disc = Brush.radialGradient(0f to Color(0xFF2A2470), 0.7f to Color(0xFF141038), 1f to Color(0xFF07061A),
        center = Offset.Zero, radius = R)
    drawCircle(disc, R, Offset.Zero)
    drawCircle(Color.Black, R, Offset.Zero, style = Stroke(max(0.6f * px, R * 0.02f)))

    fun at(a: Float, r: Float) = polar(a + rot, r)
    val line = max(0.8f * px, R * 0.045f)
    val shell = Color(0xFF1B1650)
    when (middle.id) {
        "PI2" -> for (i in 0 until 3) { // Griffe
            val a = i * TAUF / 3
            drawPath(shape(scratch) { moveTo(at(a - 0.25f, R * 0.55f)); lineTo(at(a + 0.1f, R)); lineTo(at(a + 0.32f, R * 0.6f)) }, col)
        }
        "PI3" -> for (i in 0 until 6) { // Carapace
            val c = at(i * TAUF / 6, R * 0.7f)
            val hexagon = shape(scratch) {
                for (k in 0 until 6) {
                    val q = c + polar(k * TAUF / 6, R * 0.21f)
                    if (k == 0) moveTo(q) else lineTo(q)
                }
            }
            drawPath(hexagon, shell)
            drawPath(hexagon, col, style = pen(line * 0.8f))
        }
        "PI4" -> for (i in 0 until 3) { // Anneau
            drawCircle(col.copy(alpha = 1f - i * 0.25f), R * (0.95f - i * 0.15f), Offset.Zero,
                style = Stroke(if (i == 0) line else line * 0.5f))
        }
        "PI5" -> for (i in 0 until 4) { // Axe
            val a = i * TAUF / 4
            drawPath(shape(scratch) {
                moveTo(at(a - 0.14f, R * 0.5f)); lineTo(at(a - 0.08f, R * 0.97f)); lineTo(at(a + 0.08f, R * 0.97f)); lineTo(at(a + 0.14f, R * 0.5f))
            }, col)
        }
        "PI6" -> for (i in 0 until 9) { // Turbine
            val a = i * TAUF / 9
            val c1 = at(a + 0.1f, R * 0.95f)
            val end = at(a + 0.55f, R * 0.97f)
            scratch.reset()
            scratch.moveTo(at(a, R * 0.5f))
            scratch.quadraticTo(c1.x, c1.y, end.x, end.y)
            drawPath(scratch, col, style = pen(line, StrokeCap.Round))
        }
        "PI7" -> for (i in 0 until 2) { // Lame
            val a = i * PIF
            drawPath(shape(scratch) { moveTo(at(a - 0.5f, R * 0.5f)); lineTo(at(a - 0.05f, R * 1.02f)); lineTo(at(a + 0.6f, R * 0.62f)) }, col)
        }
        "PI8" -> for (i in 0 until 4) { // Bouclier
            drawArc(col, degrees(rot) - i * 90f + 34f, -68f, false, Offset(-R * 0.86f, -R * 0.86f), Size(R * 1.72f, R * 1.72f),
                style = Stroke(R * 0.16f, cap = StrokeCap.Butt))
        }
        "PI9" -> for (i in 0 until 3) { // Spirale
            scratch.reset()
            scratch.moveTo(at(i * TAUF / 3, R * 0.45f))
            for (k in 1..20) scratch.lineTo(at(i * TAUF / 3 + k * 0.12f, R * (0.45f + k * 0.026f)))
            drawPath(scratch, col, style = pen(line * 1.1f, StrokeCap.Round))
        }
        "PI10" -> for (i in 0 until 2) { // Contrepoids
            val a = i * PIF + PIF / 2
            val c = at(a, R * 0.7f)
            withTransform({ translate(c.x, c.y); rotate(degrees(a + rot) + 90f, Offset.Zero) }) {
                drawOval(col, Offset(-R * 0.28f, -R * 0.17f), Size(R * 0.56f, R * 0.34f))
            }
        }
        else -> for (i in 0 until 4) { // Noyau (et pièce inconnue)
            val c = at(i * TAUF / 4 + 0.78f, R * 0.72f)
            drawCircle(shell, R * 0.13f, c)
            drawCircle(col, R * 0.13f, c, style = Stroke(line * 0.7f))
            drawCircle(col, R * 0.045f, c)
        }
    }
    drawCircle(col.copy(alpha = 0.45f), R, Offset.Zero, style = Stroke(max(0.6f * px, R * 0.02f)))
}

/** Boulon octogonal centré sur l'origine, pastille et emblème de son coup spécial. */
private fun DrawScope.drawBolt(special: String, R: Float, rot: Float) {
    val px = density
    val col = moveColor(special)
    val octagon = shape(scratch) {
        for (i in 0 until 8) {
            val q = polar(i * TAUF / 8 + PIF / 8 + rot, R)
            if (i == 0) moveTo(q) else lineTo(q)
        }
    }
    drawPath(octagon, Brush.linearGradient(0f to Color(0xFFE9EEFF), 0.5f to Color(0xFF7E86BE), 1f to Color(0xFF262A5A),
        start = Offset(-R, -R), end = Offset(R, R)))
    drawPath(octagon, Holo.Void, style = pen(max(0.6f * px, R * 0.04f)))

    val core = Brush.radialGradient(0f to col, 0.6f to Color(0xFF1A1650), 1f to Color(0xFF07061A),
        center = Offset(-R * 0.12f, -R * 0.18f), radius = R * 0.85f)
    drawCircle(core, R * 0.8f, Offset.Zero)
    drawCircle(col, R * 0.8f, Offset.Zero, style = Stroke(max(0.6f * px, R * 0.05f)))
    if (R >= 6f * px) {
        // L'emblème tourne avec la toupie (seul le reflet, plus bas, reste fixe).
        rotateRad(rot, Offset.Zero) { drawEmblem(special, Offset.Zero, R * 0.5f, Color.White, max(0.8f * px, R * 0.08f)) }
    }
    withTransform({ translate(-R * 0.28f, -R * 0.36f); rotate(-35f, Offset.Zero) }) {
        drawOval(Color(255, 255, 255, 90), Offset(-R * 0.26f, -R * 0.12f), Size(R * 0.52f, R * 0.24f))
    }
}

/**
 * Pointe vue de profil : collerette métallique puis embout propre à chaque pointe ;
 * `y` est le haut de la collerette, `W` sa largeur.
 */
private fun DrawScope.drawTip(id: String, cx: Float, y: Float, W: Float) {
    val s = W / 2f
    val metal = Brush.horizontalGradient(0f to Color(0xFF5C649E), 0.45f to Color(0xFFEEF2FF), 1f to Color(0xFF3A3F72),
        startX = cx - s, endX = cx + s)
    val rubber = Brush.horizontalGradient(0f to Color(0xFF3A0F2A), 0.5f to Color(0xFFFF3D6E), 1f to Color(0xFF3A0F2A),
        startX = cx - s, endX = cx + s)
    val edge = pen(max(0.6f * density, W * 0.017f))
    fun poly(fill: Brush, vararg xy: Float) {
        val p = shape(scratch) {
            moveTo(cx + xy[0] * s, y + xy[1] * s)
            for (i in 2 until xy.size step 2) lineTo(cx + xy[i] * s, y + xy[i + 1] * s)
        }
        drawPath(p, fill)
        drawPath(p, Holo.Void, style = edge)
    }
    fun ball(cy: Float, r: Float, shaded: Boolean) {
        val stops = if (shaded) arrayOf(0f to Color.White, 0.5f to Color(0xFF8E98C8), 1f to Color(0xFF2A2F60))
        else arrayOf(0f to Color.White, 1f to Color(0xFF2A2F60))
        drawCircle(Brush.radialGradient(*stops, center = Offset(cx - r * 0.3f, cy - r * 0.4f), radius = r * 1.4f), r, Offset(cx, cy))
        drawCircle(Holo.Void, r, Offset(cx, cy), style = edge)
    }
    poly(metal, -1f, 0f, 1f, 0f, 0.8f, 0.35f, -0.8f, 0.35f) // Collerette.
    when (id) {
        "P1" -> poly(metal, -0.55f, 0.35f, 0.55f, 0.35f, 0.18f, 1.1f, -0.18f, 1.1f)
        "P2" -> poly(rubber, -0.7f, 0.35f, 0.7f, 0.35f, 0.62f, 0.95f, -0.62f, 0.95f)
        "P3" -> poly(metal, -0.35f, 0.35f, 0.35f, 0.35f, 0f, 1.45f)
        "P4" -> ball(y + s * 0.82f, s * 0.5f, true)
        "P5" -> poly(metal, -0.6f, 0.35f, 0.6f, 0.35f, 0.85f, 0.9f, -0.85f, 0.9f)
        "P6" -> poly(metal, -0.45f, 0.35f, 0.45f, 0.35f, 0.05f, 1.3f, -0.05f, 1.3f)
        "P7" -> {
            poly(rubber, -0.7f, 0.35f, 0.7f, 0.35f, 0.6f, 0.8f, -0.6f, 0.8f)
            val studs = SolidColor(Color(0xFFFF3D6E))
            for (i in -2..2) poly(studs, i * 0.25f - 0.08f, 0.8f, i * 0.25f + 0.08f, 0.8f, i * 0.25f, 1.05f)
        }
        "P8" -> {
            val dome = shape(scratch) {
                moveTo(cx - s * 0.75f, y + s * 0.35f)
                arcTo(Rect(cx - s * 0.75f, y - s * 0.35f, cx + s * 0.75f, y + s * 1.05f), -180f, -180f, forceMoveTo = false)
            }
            drawPath(dome, metal)
            drawPath(dome, Holo.Void, style = edge)
        }
        "P9" -> {
            poly(metal, -0.45f, 0.35f, 0.45f, 0.35f, 0.3f, 0.6f, -0.3f, 0.6f)
            ball(y + s * 0.85f, s * 0.3f, false)
        }
        else -> poly(metal, -0.6f, 0.35f, 0.6f, 0.35f, 0.5f, 0.8f, 0.25f, 1.05f, -0.25f, 1.05f, -0.5f, 0.8f) // Sabot
    }
    drawLine(Color(255, 255, 255, 100), Offset(cx - s * 0.9f, y + s * 0.06f), Offset(cx + s * 0.9f, y + s * 0.06f),
        density, StrokeCap.Square)
}

/** Roue seule vue du dessus (carte de l'atelier), centrée sur l'origine. */
private fun DrawScope.drawWheelPart(wheel: Part, R: Float) {
    val px = density
    val mirror = wheel.spin == SpinDirection.Clockwise // Pointes des lames vers l'avant.
    val accent = hsl(wheel.hue, 0.95f, 0.6f)
    val turn = -PIF / 2
    rotateRad(turn, Offset.Zero) { // La silhouette tourne, la lumière reste fixe : dégradés contre-tournés.
        val silhouette = wheelPath(wheel.id, mirror, 1f, R)
        drawPath(silhouette, Brush.radialGradient(0f to Color(0xFFF4F7FF), 0.5f to Color(0xFF9AA4D8), 1f to Color(0xFF1D2150),
            center = Offset(-R * 0.3f, -R * 0.4f).rotated(-turn), radius = R * 1.6f))
        drawPath(silhouette, Holo.Void, style = pen(max(0.6f * px, R * 0.03f)))
        drawPath(silhouette, Brush.linearGradient(
            0f to hsl(wheel.hue, 0.95f, 0.6f, 0.85f), 0.5f to hsl(wheel.hue, 0.8f, 0.22f, 0.6f), 1f to hsl(wheel.hue, 0.95f, 0.6f, 0.9f),
            start = Offset(-R, -R).rotated(-turn), end = Offset(R, R).rotated(-turn)))
        drawPath(wheelPath(wheel.id, mirror, 0.86f, R), Color(255, 255, 255, 115), style = pen(px))
    }
    drawCircle(Color(0xFF0A0826), R * 0.6f, Offset.Zero)
    drawCircle(accent, R * 0.6f, Offset.Zero, style = Stroke(max(px, R * 0.045f)))
    drawCircle(Color(0xFF3A2F8F), R * 0.25f, Offset.Zero, style = pen(px, dashes = floatArrayOf(3f, 3f)))
}

/** Couleur associée à un type de toupie. */
fun typeColor(type: TopType): Color = when (type) {
    TopType.Attack -> Color(0xFFFF3D6E)
    TopType.Defense -> Color(0xFF2F7BFF)
    TopType.Endurance -> Color(0xFF3DFF8B)
    TopType.Balance -> Color(0xFFFFC23D)
}

/** Couleur d'un coup spécial (blanc si inconnu). */
fun moveColor(special: String): Color = SpecialMove.find(special)?.let { rgb(it.color) } ?: Color.White

/**
 * Emblème d'un coup spécial (onde, lance, givre…), centré, de demi-taille `size`.
 * `strokeWidth` en pixels (2 dp par défaut, comme sur PC) ; `opacity` reproduit QPainter::setOpacity.
 */
fun DrawScope.drawEmblem(
    special: String,
    center: Offset,
    size: Float,
    color: Color,
    strokeWidth: Float = 2f * density,
    opacity: Float = 1f,
) {
    if (size <= 0f) return
    val line = max(0.5f * density, strokeWidth * 0.5f) / size // Liseré en pixels.
    withTransform({ translate(center.x, center.y); scale(size, size, Offset.Zero) }) {
        if (special == "lance") {
            drawDagger(color, line, opacity)
        } else {
            // Forme pleine : dégradé de la couleur, liseré plus sombre.
            val path = emblemPath(special)
            drawPath(path, Brush.linearGradient(0f to color.lighter(115), 1f to color.darker(140).copy(alpha = 0.9f),
                start = Offset(-1f, -1f), end = Offset(1f, 1f)), opacity)
            drawPath(path, color.darker(220).copy(alpha = 0.7f), opacity,
                Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * Toupie vue du dessus : roue métallique à silhouette propre, partie intermédiaire à motif et
 * boulon portant l'emblème de son coup spécial.
 * @param angle angle de rotation propre (radians)
 * @param glow  intensité (0..1) d'un halo, utilisé pendant un dash
 */
fun DrawScope.drawTop(top: Top, center: Offset, radius: Float, angle: Float, glow: Float = 0f) {
    if (radius <= 0f) return
    val px = density
    val type = typeColor(top.type)
    val wheel = top.wheel
    val mirror = wheel.spin == SpinDirection.Clockwise // Pointes des lames vers l'avant.
    val accent = hsl(wheel.hue, 0.95f, 0.6f)
    val deep = hsl(wheel.hue, 0.8f, 0.22f)
    translate(center.x, center.y) {
        // Ombre portée.
        val shadowAt = Offset(radius * 0.1f, radius * 0.16f)
        drawCircle(Brush.radialGradient(0.75f to Color(0, 0, 0, 120), 1f to Color(0, 0, 0, 0), center = shadowAt,
            radius = radius * 1.1f), radius * 1.1f, shadowAt)

        if (glow > 0f) {
            drawCircle(Brush.radialGradient(0.45f to type.withAlpha(0.6f * glow), 1f to type.copy(alpha = 0f),
                center = Offset.Zero, radius = radius * 1.9f), radius * 1.9f, Offset.Zero)
        }

        // Roue métallique : la silhouette tourne, la lumière reste fixe (dégradés contre-tournés).
        rotateRad(angle, Offset.Zero) {
            val silhouette = wheelPath(wheel.id, mirror, 1f, radius)
            drawPath(silhouette, Brush.radialGradient(
                0f to Color(0xFFF4F7FF), 0.45f to Color(0xFF9AA4D8), 0.8f to Color(0xFF4B5290), 1f to Color(0xFF1D2150),
                center = Offset(-radius * 0.35f, -radius * 0.4f).rotated(-angle), radius = radius * 1.6f))
            drawPath(silhouette, Holo.Void, style = pen(max(0.8f * px, radius * 0.02f)))
            drawPath(silhouette, Brush.linearGradient(
                0f to accent.copy(alpha = 0.85f), 0.5f to deep.copy(alpha = 0.6f), 1f to accent.copy(alpha = 0.9f),
                start = Offset(-radius, -radius).rotated(-angle), end = Offset(radius, radius).rotated(-angle)))
            drawPath(wheelPath(wheel.id, mirror, 0.86f, radius), Color(255, 255, 255, 115),
                style = pen(max(0.6f * px, radius * 0.014f)))
        }

        if (radius >= 10f * px) {
            // Rainures radiales.
            val groove = Color(5, 4, 15, 140)
            val width = max(0.7f * px, radius * 0.013f)
            for (i in 0 until 24) {
                val d = polar(i * TAUF / 24 + angle, 1f)
                drawLine(groove, d * (radius * 0.66f), d * (radius * 0.76f), width, StrokeCap.Square)
            }
        }
        drawCircle(accent, radius * 0.79f, Offset.Zero, style = Stroke(max(0.8f * px, radius * 0.022f)))
        // Reflet métallique fixe (haut-gauche).
        drawArc(Color(255, 255, 255, 130), -100f, -70f, false, Offset(-radius * 0.87f, -radius * 0.87f),
            Size(radius * 1.74f, radius * 1.74f), style = Stroke(max(px, radius * 0.03f), cap = StrokeCap.Round))

        drawMiddle(top.middle, radius * 0.6f, angle)
        if (top.bolt.id.isNotEmpty()) drawBolt(top.special, radius * 0.34f, angle)
    }
}

/** Petite flèche circulaire indiquant le sens de rotation. */
fun DrawScope.drawSpinArrow(direction: SpinDirection, center: Offset, radius: Float, color: Color) {
    // Arc de 270° (de midi vers 9 h dans le sens horaire), le même pour les deux sens.
    drawArc(color, -90f, 270f, false, Offset(center.x - radius, center.y - radius), Size(2 * radius, 2 * radius),
        style = Stroke(max(1.5f * density, radius * 0.12f), cap = StrokeCap.Round))
    // Pointe de flèche au bout de l'arc (horaire : fin de l'arc à 180°).
    val clockwise = direction == SpinDirection.Clockwise
    val endRad = if (clockwise) PIF else PIF / 2
    val end = Offset(center.x + radius * cos(endRad), center.y - radius * sin(endRad))
    // Direction tangente au point final, dans le sens de rotation affiché.
    val tangent = if (clockwise) Offset(0f, -1f) else Offset(-1f, 0f)
    val normal = Offset(-tangent.y, tangent.x)
    val s = radius * 0.5f
    drawPath(shape(scratch) { moveTo(end + tangent * (s * 0.6f)); lineTo(end - normal * (s * 0.5f)); lineTo(end + normal * (s * 0.5f)) }, color)
}

/**
 * Pièce seule (cartes de l'atelier) : boulon, roue et intermédiaire vus du dessus, pointe de
 * profil, dans un cercle de rayon `radius`.
 */
fun DrawScope.drawPart(part: Part, center: Offset, radius: Float) {
    if (radius <= 0f) return
    translate(center.x, center.y) {
        when (part.kind) {
            PartKind.Bolt -> {
                val c = moveColor(part.special)
                drawCircle(Brush.radialGradient(0.5f to c.copy(alpha = 90 / 255f), 1f to c.copy(alpha = 0f),
                    center = Offset.Zero, radius = radius * 1.2f), radius * 1.2f, Offset.Zero)
                drawBolt(part.special, radius, 0f)
            }
            PartKind.Wheel -> drawWheelPart(part, radius)
            PartKind.Middle -> drawMiddle(part, radius, 0f)
            PartKind.Tip -> drawTip(part.id, 0f, -radius * 0.7f, radius * 1.9f)
        }
    }
}

/**
 * Vue éclatée de profil (boulon, roue, intermédiaire, pointe de haut en bas), centrée sur
 * `origin.x` à partir de `origin.y`, de largeur `width` (hauteur ≈ width).
 * Renvoie un point d'accroche sur chaque pièce, indexé par [PartKind.ordinal].
 */
fun DrawScope.drawProfile(top: Top, origin: Offset, width: Float): List<Offset> {
    val W = width
    val px = density
    val cx = origin.x
    val gap = W * 0.16f
    var y = origin.y
    val anchors = Array(PartKind.entries.size) { Offset.Zero }
    val edge = pen(px)
    val metal = Brush.horizontalGradient(0f to Color(0xFF3A3F72), 0.35f to Color(0xFFEEF2FF), 0.6f to Color(0xFF9AA4D8),
        1f to Color(0xFF262A5A), startX = cx - W * 0.5f, endX = cx + W * 0.5f)

    // Boulon : dôme métallique et pastille de la couleur du coup.
    val dome = shape(scratch) {
        moveTo(cx - W * 0.16f, y + W * 0.12f)
        quadraticTo(cx - W * 0.15f, y, cx, y)
        quadraticTo(cx + W * 0.15f, y, cx + W * 0.16f, y + W * 0.12f)
    }
    drawPath(dome, metal)
    drawPath(dome, Holo.Void, style = edge)
    val move = moveColor(top.special)
    val spot = Offset(cx, y + W * 0.05f)
    drawOval(Brush.radialGradient(0f to move, 1f to move.copy(alpha = 0f), center = spot, radius = W * 0.12f),
        Offset(cx - W * 0.12f, spot.y - W * 0.06f), Size(W * 0.24f, W * 0.12f))
    drawOval(move, Offset(cx - W * 0.07f, spot.y - W * 0.03f), Size(W * 0.14f, W * 0.06f))
    anchors[PartKind.Bolt.ordinal] = Offset(cx, y + W * 0.03f)
    y += W * 0.12f + gap

    // Roue : bandeau dont la largeur suit le profil de la silhouette.
    val wheel = top.wheel
    val r = wheelProfile(wheel.id)
    var lo = 1.0
    var hi = 0.0
    for (i in 0..40) {
        lo = min(lo, r(i * PI / 40))
        hi = max(hi, r(i * PI / 40))
    }
    val l = lo.toFloat()
    val h = hi.toFloat()
    val rim = W * 0.14f
    val band = shape(scratch) {
        moveTo(cx - W * 0.42f, y); lineTo(cx + W * 0.42f, y); lineTo(cx + W * 0.5f * h, y + rim * 0.35f)
        lineTo(cx + W * 0.5f * l, y + rim); lineTo(cx - W * 0.5f * l, y + rim); lineTo(cx - W * 0.5f * h, y + rim * 0.35f)
    }
    drawPath(band, metal)
    drawPath(band, Holo.Void, style = edge)
    drawPath(band, hsl(wheel.hue, 0.95f, 0.6f, 0.45f))
    drawLine(Color(255, 255, 255, 150), Offset(cx - W * 0.42f, y + 2 * px), Offset(cx + W * 0.42f, y + 2 * px), px, StrokeCap.Square)
    drawRect(hsl(wheel.hue, 0.95f, 0.6f, 0.9f), Offset(cx - W * 0.3f, y + rim * 0.45f), Size(W * 0.6f, rim * 0.18f))
    anchors[PartKind.Wheel.ordinal] = Offset(cx + W * 0.375f, y + rim * 0.2f)
    y += rim + gap

    // Partie intermédiaire : disque bombé et liseré de sa teinte.
    val disc = shape(scratch) {
        moveTo(cx - W * 0.3f, y); lineTo(cx + W * 0.3f, y); lineTo(cx + W * 0.26f, y + W * 0.09f); lineTo(cx - W * 0.26f, y + W * 0.09f)
    }
    drawPath(disc, Brush.horizontalGradient(0f to Color(0xFF141038), 0.5f to Color(0xFF3B3490), 1f to Color(0xFF141038),
        startX = cx - W * 0.3f, endX = cx + W * 0.3f))
    drawPath(disc, Holo.Void, style = edge)
    drawRect(hsl(top.middle.hue, 0.95f, 0.62f), Offset(cx - W * 0.27f, y + W * 0.035f), Size(W * 0.54f, W * 0.02f))
    anchors[PartKind.Middle.ordinal] = Offset(cx - W * 0.28f, y + W * 0.045f)
    y += W * 0.09f + gap

    drawTip(top.tip.id, cx, y, W * 0.24f)
    anchors[PartKind.Tip.ordinal] = Offset(cx + W * 0.06f, y + W * 0.1f)
    return anchors.asList()
}
