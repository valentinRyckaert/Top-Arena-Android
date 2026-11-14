package fr.toupiearena.mobile.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.StadiumGeometry
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Portage de src/ui/StadiumPainter.cpp (version PC, charte « Holo Combat »).

private val TAUF = (2 * PI).toFloat()
private val PIF = PI.toFloat()

/** Contour du stadium en pixels, élargi de `grow` pixels. */
private fun outlinePath(geometry: StadiumGeometry, c: Offset, scale: Float, grow: Float = 0f): Path {
    val path = Path()
    geometry.outline().forEachIndexed { i, p ->
        val len = p.length().toFloat()
        val factor = if (len > 1e-9f) (len * scale + grow) / (len * scale) else 1f
        val x = c.x + p.x.toFloat() * scale * factor
        val y = c.y + p.y.toFloat() * scale * factor
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun regular(c: Offset, r: Float, n: Int, rotation: Float) = shape {
    for (i in 0 until n) {
        val a = rotation + i * TAUF / n
        val q = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
        if (i == 0) moveTo(q) else lineTo(q)
    }
}

private fun dir(a: Float) = Offset(cos(a), sin(a))

/** Équivalents de QRandomGenerator::bounded et generateDouble. */
private fun Random.bounded(max: Float) = nextFloat() * max
private fun Random.bounded(max: Int) = nextInt(max)

/** Faille en zigzag depuis le centre (terre, volcan). */
private fun crack(rng: Random, c: Offset, R: Float, angle: Float, from: Float, step: Float, wander: Float): Path {
    var a = angle
    var r = R * from
    val path = Path()
    path.moveTo(c + dir(a) * r)
    while (r < R * 0.93f) {
        r += R * (step + rng.bounded(step))
        a += (rng.nextFloat() - 0.5f) * wander
        path.lineTo(c + dir(a) * min(r, R * 0.95f))
    }
    return path
}

/**
 * Remplissage radial du sol. shortcut: Compose n'a pas de point focal (QRadialGradient à foyer
 * décalé) ; le centre du dégradé est décalé d'autant, quasi identique pour les petits décalages
 * utilisés (≤ 10 % du rayon). À revoir si un foyer marqué est un jour nécessaire.
 */
private fun DrawScope.radialFill(path: Path, c: Offset, R: Float, vararg stops: Pair<Float, Color>, focus: Offset = Offset.Zero) {
    drawPath(path, Brush.radialGradient(*stops, center = c + focus, radius = R))
}

/**
 * Projette un point d'un motif plat (dalles, rides) sur la cuvette vue de dessus, en
 * perspective : le fond rapetisse, le bord s'élargit ; `depth` > 1 règle l'effet.
 */
private fun onBowl(c: Offset, R: Float, flat: Offset, depth: Float): Offset {
    val d = flat - c
    val r = hypot(d.x, d.y) / R
    if (r < 1e-9f) return flat
    val reach = 1.1f // Rayon plat ramené sur le bord du stadium.
    val curved = (min(r, reach) / reach).pow(depth) * 1.04f
    return c + d * (curved / r)
}

/**
 * Ombrage d'une cuvette éclairée d'en haut à gauche : le versant opposé prend la lumière, le
 * versant proche reste dans l'ombre, le fond reste neutre ; le pied du mur s'assombrit.
 */
private fun DrawScope.bowlShading(floor: Path, c: Offset, R: Float) = clipPath(floor) {
    val light = Brush.linearGradient(
        0f to Color(0, 0, 0, 13), 0.45f to Color(0, 0, 0, 0), 0.55f to Color(255, 255, 255, 0), 1f to Color(255, 255, 255, 8),
        start = c - Offset(R, R) * 0.75f, end = c + Offset(R, R) * 0.75f)
    // Seulement sur les pentes : fins anneaux superposés, plus marqués vers le bord.
    val steps = 8
    for (step in 0 until steps) {
        val inner = 0.82f - step * (0.82f - 0.3f) / (steps - 1)
        val ring = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(c, R * 1.3f))
            addOval(Rect(c, R * inner))
        }
        drawPath(ring, light)
    }
    drawRect(Brush.radialGradient(0.62f to Color(0, 0, 0, 0), 1f to Color(0, 0, 0, 150), center = c, radius = R * 1.05f),
        Offset(c.x - R * 1.2f, c.y - R * 1.2f), Size(R * 2.4f, R * 2.4f))
}

private fun DrawScope.rings(c: Offset, R: Float, n: Int, color: Color) {
    for (i in 1..n) drawCircle(color, R * i / (n + 1), c, style = Stroke(density))
}

private fun DrawScope.paintClassic(floor: Path, g: StadiumGeometry, c: Offset, scale: Float, R: Float) {
    val px = density
    val rim = outlinePath(g, c, scale, R * 0.07f)
    strokeGlow(rim, rgb(0x34d6ff, 150), 2f * px)
    drawPath(rim, Brush.linearGradient(0f to rgb(0x3b3490), 1f to rgb(0x0f0c33), start = c - Offset(R, R), end = c + Offset(R, R)))
    // Segments lumineux du rebord.
    for (i in 0 until 24) {
        drawArc(if (i % 3 != 0) rgb(0x7b3cff, 190) else rgb(0x34d6ff), -(i * 15f + 1f), -13f, false,
            Offset(c.x - R * 1.035f, c.y - R * 1.035f), Size(R * 2.07f, R * 2.07f), style = Stroke(R * 0.025f, cap = StrokeCap.Butt))
    }
    radialFill(floor, c, R, 0f to rgb(0x2a3c9e), 0.55f to rgb(0x191660), 1f to rgb(0x07061a))
    clipPath(floor) {
        rings(c, R, 7, rgb(0x34d6ff, 75))
        val spokes = Path()
        for (i in 0 until 24) {
            val d = dir(i * TAUF / 24)
            spokes.moveTo(c + d * (R * 0.12f))
            spokes.lineTo(c + d * R)
        }
        drawPath(spokes, rgb(0x7b3cff, 64), style = pen(px))
        drawCircle(rgb(0x3dff8b, 200), R * 0.55f, c, style = pen(1.5f * px, dashes = floatArrayOf(1.5f, 7f)))
    }
    drawCircle(Brush.radialGradient(0f to rgb(0x34d6ff, 140), 1f to rgb(0x34d6ff, 0), center = c, radius = R * 0.16f), R * 0.16f, c)
    drawCircle(rgb(0x34d6ff), R * 0.1f, c, style = Stroke(2f * px))
    drawPath(regular(c, R * 0.065f, 6, 0f), rgb(0x34d6ff, 210))
    drawPath(floor, Holo.Void, style = pen(3f * px))
}

private fun DrawScope.paintConcrete(floor: Path, g: StadiumGeometry, c: Offset, scale: Float, R: Float, rng: Random) {
    val px = density
    val wall = outlinePath(g, c, scale, R * 0.16f)
    // Mur du même béton que le sol, éclairé d'en haut à gauche.
    drawPath(wall, Brush.linearGradient(0f to rgb(0xa9aeb6), 0.5f to rgb(0x777c86), 1f to rgb(0x464a55),
        start = c - Offset(R, R), end = c + Offset(R, R)))
    drawPath(wall, Holo.Void, style = pen(2f * px))
    // Bandes de sécurité sur le haut mur.
    drawPath(outlinePath(g, c, scale, R * 0.085f), rgb(0x3dff8b, 215),
        style = pen(R * 0.1f, StrokeCap.Butt, dashes = floatArrayOf(0.35f, 0.45f)))
    strokeGlow(outlinePath(g, c, scale, R * 0.02f), rgb(0x3dff8b, 110), 1.5f * px)
    // Béton frais : gris clair, plus sombre au pied du mur.
    radialFill(floor, c, R, 0f to rgb(0xb4b8bf), 0.55f to rgb(0x90959e), 1f to rgb(0x4c505b))
    clipPath(floor) {
        // Grain : granulats clairs et sombres, à peine visibles.
        for (i in 0 until 260) {
            val a = rng.bounded(TAUF)
            val r = sqrt(rng.bounded(1f)) * R * 1.02f
            val color = if (i % 2 == 1) Color(40, 42, 50, 40 + rng.bounded(40)) else Color(235, 238, 242, 30 + rng.bounded(40))
            val size = 0.6f + rng.bounded(1.6f)
            drawCircle(color, size * px, c + dir(a) * r)
        }
        // Quelques fissures fines, brisées, parfois ramifiées : six au hasard, puis quatre sur la moitié gauche.
        val cracks = Path()
        for (k in 0 until 10) {
            val left = k >= 6
            val a0 = if (left) PIF + (rng.nextFloat() - 0.5f) * 1.4f else rng.bounded(TAUF)
            val reach = if (left) 0.4f + rng.bounded(0.45f) else 0.2f + rng.bounded(0.65f)
            var at = c + dir(a0) * (R * reach)
            var heading = rng.bounded(TAUF)
            cracks.moveTo(at)
            val segments = 4 + rng.bounded(4)
            for (i in 0 until segments) {
                heading += (rng.nextFloat() - 0.5f) * 1.1f
                at += dir(heading) * (R * (0.04f + rng.bounded(0.05f)))
                cracks.lineTo(at)
                if (i == segments / 2 && k % 2 == 0) { // Ramification.
                    val branch = heading + if (rng.nextFloat() < 0.5f) -0.9f else 0.9f
                    cracks.lineTo(at + dir(branch) * (R * 0.06f))
                    cracks.moveTo(at)
                }
            }
        }
        withTransform({ translate(0.8f * px, 0.8f * px) }) { drawPath(cracks, Color(255, 255, 255, 30), style = pen(px)) } // Arête éclatée.
        drawPath(cracks, Color(30, 32, 40, 110), style = pen(px, StrokeCap.Round))
        bowlShading(floor, c, R)
    }
    drawPath(floor, Holo.Void, style = pen(3f * px))
}

private fun DrawScope.paintDirt(floor: Path, c: Offset, R: Float, rng: Random) {
    val px = density
    // Pas de rebord : le sol se délite dans le vide.
    for (i in 0 until 44) {
        val a = rng.bounded(TAUF)
        val r = R * (0.98f + rng.bounded(0.16f))
        val color = Color(74, 53, 36, 100 + rng.bounded(130))
        drawPath(regular(c + dir(a) * r, R * (0.012f + rng.bounded(0.028f)), 5, rng.bounded(3f)), color)
    }
    radialFill(floor, c, R, 0f to rgb(0x6b4a2e), 0.7f to rgb(0x3b2716), 1f to rgb(0x1a1008), focus = Offset(-R * 0.04f, -R * 0.05f))
    clipPath(floor) {
        for (k in 0 until 7) {
            val path = crack(rng, c, R, k * TAUF / 7 + rng.bounded(0.4f), 0.12f, 0.06f, 0.5f)
            drawPath(path, rgb(0x2a1a0e, 150), style = pen(1.2f * px)) // Discrète : à peine plus sombre que le sol.
        }
        for (i in 0 until 18) {
            val a = rng.bounded(TAUF)
            val r = R * (0.2f + rng.bounded(0.7f))
            val pebble = regular(c + dir(a) * r, R * (0.016f + rng.bounded(0.034f)), 5 + rng.bounded(3), rng.bounded(3f))
            drawPath(pebble, Color(0xFF2A1A0E))
            drawPath(pebble, Color(0xFF8A6A44), style = pen(px))
        }
    }
    drawCircle(Brush.radialGradient(0f to rgb(0xb8ff2e, 90), 1f to rgb(0xb8ff2e, 0), center = c, radius = R * 0.2f), R * 0.2f, c)
    drawPath(floor, rgb(0xb8ff2e, 130), style = pen(1.5f * px, dashes = floatArrayOf(4f, 5f)))
}

private fun DrawScope.paintIce(floor: Path, g: StadiumGeometry, c: Offset, scale: Float, R: Float, rng: Random) {
    val px = density
    val aspect = if (g.kind == StadiumGeometry.Kind.Ellipse) g.aspect.toFloat() else 1f
    val rim = outlinePath(g, c, scale, R * 0.1f)
    strokeGlow(rim, rgb(0x8ff3ff, 140), 2f * px)
    drawPath(rim, Brush.linearGradient(0f to rgb(0xe6fcff), 0.5f to rgb(0x5ab8e0), 1f to rgb(0x1a3a7a),
        start = c - Offset(R, R), end = c + Offset(R, R)))
    radialFill(floor, c, R, 0f to rgb(0xc9f6ff), 0.5f to rgb(0x4fa6e8), 1f to rgb(0x18286e), focus = Offset(-R * 0.08f, -R * 0.1f))
    clipPath(floor) {
        // Facettes cristallines.
        val pts = ArrayList<Offset>(72)
        for (i in 0 until 6) for (j in 0 until 12) {
            val r = R * (i + 0.5f) / 5.6f
            val a = j * TAUF / 12 + i * 0.26f + (rng.nextFloat() - 0.5f) * 0.2f
            pts += c + Offset(cos(a) * r, sin(a) * r * aspect)
        }
        val facets = Path()
        for (i in 0 until 5) for (j in 0 until 12) {
            facets.moveTo(pts[i * 12 + j])
            facets.lineTo(pts[i * 12 + (j + 1) % 12])
            facets.lineTo(pts[(i + 1) * 12 + j])
            facets.close()
        }
        drawPath(facets, Color(255, 255, 255, 85), style = pen(0.8f * px))
        for (i in 0 until 9) {
            val alpha = 14 + rng.bounded(16)
            drawPath(shape { moveTo(pts[rng.bounded(60)]); lineTo(pts[rng.bounded(60)]); lineTo(c) }, Color(255, 255, 255, alpha))
        }
        withTransform({ translate(c.x - R * 0.3f, c.y - R * 0.25f * aspect); rotate(-18f, Offset.Zero) }) {
            drawOval(Color(255, 255, 255, 90), Offset(-R * 0.35f, -R * 0.08f), Size(R * 0.7f, R * 0.16f))
        }
    }
    drawEmblem("givre", c, R * 0.1f, Color.White, 1.8f * px)
    drawPath(floor, Holo.Void, style = pen(3f * px))
}

private fun DrawScope.paintSand(floor: Path, g: StadiumGeometry, c: Offset, scale: Float, R: Float) {
    val px = density
    val wall = outlinePath(g, c, scale, R * 0.14f)
    drawPath(wall, Brush.linearGradient(0f to rgb(0xe8b86a), 0.5f to rgb(0x9a6a34), 1f to rgb(0x4a2e18),
        start = c - Offset(R, R), end = c + Offset(R, R)))
    drawPath(wall, Holo.Void, style = pen(2f * px))
    // Glyphes gravés le long du mur.
    val glyphLine = PathMeasure().apply { setPath(outlinePath(g, c, scale, R * 0.07f), false) }
    for (i in 0 until 18) {
        val at = glyphLine.getPosition(glyphLine.length * (i + 0.5f) / 18f)
        drawPath(regular(at, R * 0.025f, 3, i * 1.3f), rgb(0x7b3cff, 230), style = pen(1.5f * px))
    }
    radialFill(floor, c, R, 0f to rgb(0xf2c98a), 0.6f to rgb(0xc08a4c), 1f to rgb(0x6a4422))
    clipPath(floor) {
        // Rides du sable qui épousent la cuvette (resserrées et courbées vers le bord).
        val dunes = Path()
        for (i in -11..11) {
            val y0 = i * R * 0.11f
            val samples = 96
            for (k in 0..samples) {
                val x = -R * 1.3f + k * R * 2.6f / samples
                val y = y0 + sin(x / R * 4f * PIF + i) * R * 0.035f
                val q = onBowl(c, R, c + Offset(x, y), 1.2f)
                if (k == 0) dunes.moveTo(q) else dunes.lineTo(q)
            }
        }
        drawPath(dunes, Color(0x6a, 0x44, 0x22, 130), style = pen(1.3f * px))
    }
    bowlShading(floor, c, R)
    drawPath(outlinePath(g, c, scale, -R * 0.02f), rgb(0x7b3cff, 150), style = pen(2f * px))
    drawPath(regular(c, R * 0.2f, 6, 0f), rgb(0x7b3cff), style = pen(2f * px))
    drawPath(regular(c, R * 0.1f, 3, -PIF / 2), rgb(0x7b3cff, 200))
    drawPath(floor, Holo.Void, style = pen(3f * px))
}

private fun DrawScope.paintVolcano(floor: Path, c: Offset, R: Float, rng: Random) {
    val px = density
    drawCircle(Brush.radialGradient(0.7f to rgb(0xff3d1f, 70), 1f to rgb(0xff3d1f, 0), center = c, radius = R * 1.35f), R * 1.35f, c)
    for (i in 0 until 30) {
        val a = rng.bounded(TAUF)
        val r = R * (1f + rng.bounded(0.18f))
        drawPath(regular(c + dir(a) * r, R * (0.015f + rng.bounded(0.03f)), 4, rng.bounded(3f)), rgb(0x2a0e14, 210))
    }
    drawPath(floor, Brush.radialGradient(0f to rgb(0xff6a3d), 0.25f to rgb(0x7a1b2a), 0.6f to rgb(0x2a0a1e), 1f to rgb(0x12040e),
        center = c, radius = R))
    clipPath(floor) {
        // Courbes de niveau serrées : la pente est raide.
        val contours = Path()
        for (i in 1..12) {
            val r = R * (i / 13f).pow(0.7f)
            for (k in 0..72) {
                val a = k * TAUF / 72
                val q = c + dir(a) * (r * (1 + 0.025f * sin(a * 5 + i)))
                if (k == 0) contours.moveTo(q) else contours.lineTo(q)
            }
        }
        drawPath(contours, rgb(0xff6a3d, 90), style = pen(0.9f * px))
        for (k in 0 until 9) {
            val lava = crack(rng, c, R, k * TAUF / 9 + rng.bounded(0.3f), 0.1f, 0.07f, 0.6f)
            drawPath(lava, rgb(0x8a2a1e, 110), style = pen(1.2f * px)) // Discrète : braise sourde, pas de halo.
        }
        for (i in 0 until 22) {
            val a = rng.bounded(TAUF)
            val r = R * rng.bounded(1f)
            val color = rgb(0xffc23d, 100 + rng.bounded(150))
            drawCircle(color, (1f + rng.bounded(2f)) * px, c + dir(a) * r)
        }
    }
    drawCircle(Brush.radialGradient(0f to rgb(0xfff1c2), 0.4f to rgb(0xffc23d, 200), 1f to rgb(0xffc23d, 0), center = c,
        radius = R * 0.18f), R * 0.18f, c)
}

/**
 * Dessine le stadium (sol, motif de surface, rebord ou vide) centré sur `center`, avec `scale`
 * pixels par unité de monde. Le décor est fixe : le dessiner dans son propre Canvas (ou
 * drawWithCache) évite de le recalculer à chaque image.
 *
 * shortcut: le hasard (grain, fissures, cailloux) est tiré d'un Random initialisé par l'id du
 * stadium, et non par QRandomGenerator(qHash(id, 7)) : même aspect, disposition différente du PC.
 * À reprendre seulement s'il faut une disposition identique au pixel près.
 */
fun DrawScope.drawStadium(stadium: Stadium, geometry: StadiumGeometry, center: Offset, scale: Float) {
    val radius = geometry.radius.toFloat() * scale
    if (radius <= 0f) return
    val floor = outlinePath(geometry, center, scale)
    val rng = Random(stadium.id.hashCode())
    when (stadium.id) {
        "beton" -> paintConcrete(floor, geometry, center, scale, radius, rng)
        "terre" -> paintDirt(floor, center, radius, rng)
        "glace" -> paintIce(floor, geometry, center, scale, radius, rng)
        "sable" -> paintSand(floor, geometry, center, scale, radius)
        "volcan" -> paintVolcano(floor, center, radius, rng)
        else -> paintClassic(floor, geometry, center, scale, radius)
    }
}

/** qRound : arrondi au plus proche, moitiés loin de zéro. */
private fun percent(multiplier: Double): String {
    val v = (multiplier - 1.0) * 100.0
    val value = (if (v >= 0) floor(v + 0.5) else ceil(v - 0.5)).toInt()
    return if (value > 0) "+$value %" else "$value %"
}

/** Résumé lisible des caractéristiques (« Cercle · rebord · vitesse +5 % »…). */
fun describeTraits(stadium: Stadium): String {
    val traits = mutableListOf(
        when (stadium.shape) {
            StadiumGeometry.Kind.Circle -> "Cercle"
            StadiumGeometry.Kind.Polygon -> when (stadium.sides) { 6 -> "Hexagone"; 8 -> "Octogone"; else -> "Polygone" }
            StadiumGeometry.Kind.Ellipse -> "Ellipse"
        },
    )
    if (stadium.size < 0.99) traits += "petit"
    traits += when {
        !stadium.hasRim -> "sans rebord"
        stadium.rimHeight > 1.05 -> "rebord haut"
        stadium.rimHeight < 0.95 -> "rebord bas"
        else -> "rebord"
    }
    fun add(label: String, value: Double) {
        if (abs(value - 1.0) > 0.01) traits += "$label ${percent(value)}"
    }
    add("vitesse", stadium.speed)
    add("frottement", stadium.friction)
    add("pente", stadium.slope)
    add("usure", stadium.enduranceDrain)
    add("dégâts", stadium.damage)
    add("recul", stadium.knockback)
    add("portée du dash", stadium.dashSpeed)
    add("puissance du dash", stadium.dashPower)
    if (traits.size == 2) traits += "aucun modificateur"
    return traits.joinToString(" · ")
}
