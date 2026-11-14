package fr.toupiearena.mobile.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import fr.toupiearena.mobile.game.Duel
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.Projectile
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Portage de src/ui/BattleEffects.cpp (version PC, charte « Holo Combat »).

/** Coordonnées du monde (rayons de stadium) → pixels. */
typealias ToScreen = (Vec2) -> Offset

private const val TRAIL_LENGTH = 36
private const val SPARK_LIFETIME = 0.4
private const val EMBER_LIFETIME = 0.35
private val PIF = PI.toFloat()

private fun pseudoRandom(seed: Int): Float {
    val x = sin(seed * 12.9898) * 43758.5453
    return (x - floor(x)).toFloat()
}

private fun dir(a: Float) = Offset(cos(a), sin(a))

/**
 * Effets visuels d'un combat, partagés par l'arène et le combat de démonstration du menu :
 * étincelles des chocs (toupies, rebord, coups spéciaux), gerbes des duels et traînées des toupies.
 * Les coups spéciaux en vol et les états sont dessinés par [drawProjectiles] et [drawStatus].
 */
class BattleEffects {
    private class Spark(val position: Vec2, val intensity: Float, val color: Color?) { var age = 0.0 }

    /** Étincelle projetée par le frottement d'un duel (coordonnées du monde). */
    private class Ember(var x: Double, var y: Double, var vx: Double, var vy: Double) { var age = 0.0 }

    private val trails = ArrayList<ArrayDeque<Vec2>>()
    private val sparks = ArrayList<Spark>()
    private val embers = ArrayList<Ember>()

    fun reset() {
        trails.clear()
        sparks.clear()
        embers.clear()
    }

    /**
     * Avance d'une image : nouveaux chocs, gerbes du duel en cours et, si les toupies bougent
     * (combat lancé), allongement de leurs traînées.
     */
    fun update(dt: Double, impacts: List<ImpactEvent>, spinners: List<Spinner>, duel: Duel, moving: Boolean) {
        // Chocs entre toupies, sur le rebord (first == second == -1) ou de coups spéciaux : même étincelle.
        for (impact in impacts) {
            sparks += Spark(impact.point, impact.intensity.toFloat(), if (impact.special) rgb(impact.color) else null)
        }

        // Duel : gerbes d'étincelles de part et d'autre du point de friction.
        if (duel.active) {
            val side = duel.normal.perpendicular()
            for (k in 0 until 6) {
                val sign = if (k % 2 == 0) 1.0 else -1.0
                val spread = (Random.nextDouble() - 0.5) * 1.2
                val d = (side * sign + duel.normal * spread).normalized()
                val speed = 0.4 + 0.8 * Random.nextDouble()
                embers += Ember(duel.point.x, duel.point.y, d.x * speed, d.y * speed)
            }
        }
        for (e in embers) {
            e.age += dt
            e.x += e.vx * dt
            e.y += e.vy * dt
            e.vx *= 0.92
            e.vy *= 0.92
        }
        embers.removeAll { it.age > EMBER_LIFETIME }

        if (moving) {
            while (trails.size < spinners.size) trails += ArrayDeque<Vec2>()
            while (trails.size > spinners.size) trails.removeAt(trails.lastIndex)
            for ((i, s) in spinners.withIndex()) {
                if (!s.isActive) continue
                trails[i].addLast(s.position) // Vec2 immuable : la référence suffit.
                while (trails[i].size > TRAIL_LENGTH) trails[i].removeFirst()
            }
        }

        for (s in sparks) s.age += dt
        sparks.removeAll { it.age > SPARK_LIFETIME }
    }

    /** Traînées lumineuses (sous les toupies), à la couleur du type. */
    fun DrawScope.drawTrails(spinners: List<Spinner>, toScreen: ToScreen, radius: Float) {
        for (i in 0 until min(trails.size, spinners.size)) {
            val trail = trails[i]
            val base = typeColor(spinners[i].top.type)
            for (k in 1 until trail.size) {
                val f = k.toFloat() / trail.size
                val a = toScreen(trail[k - 1])
                val b = toScreen(trail[k])
                drawLine(base.withAlpha(0.12f * f), a, b, radius * 1.1f * f, StrokeCap.Round)
                drawLine(base.withAlpha(0.45f * f), a, b, radius * 0.35f * f, StrokeCap.Round)
            }
        }
    }

    /** Étincelles des chocs et des duels (par-dessus les toupies). */
    fun DrawScope.drawSparks(toScreen: ToScreen, radius: Float, scale: Float) {
        val px = density
        // Étincelles de duel.
        for (e in embers) {
            val t = (e.age / EMBER_LIFETIME).toFloat()
            val head = toScreen(Vec2(e.x, e.y))
            val tail = head - Offset(e.vx.toFloat(), e.vy.toFloat()) * (scale * 0.04f)
            val color = if ((e.age * 100).toInt() % 3 != 0) Holo.Warning else Color.White
            drawLine(color.withAlpha(1f - t), tail, head, 2.2f * px, StrokeCap.Round)
        }

        // Étincelles de choc.
        for (spark in sparks) {
            val t = (spark.age / SPARK_LIFETIME).toFloat()
            val c = toScreen(spark.position)
            val size = radius * (0.6f + 1.6f * t) * (0.6f + spark.intensity)
            if (size <= 0f) continue
            val color = spark.color ?: Holo.Warning
            drawCircle(Brush.radialGradient(0f to Color.White.withAlpha(0.8f * (1f - t)), 1f to color.withAlpha(0f),
                center = c, radius = size), size, c)
            for (k in 0 until 8) {
                val d = dir(k * PIF / 4f + spark.intensity)
                drawLine(color.withAlpha(1f - t), c + d * (size * 0.5f), c + d * (size * if (k % 2 == 1) 1f else 1.35f),
                    2.2f * px, StrokeCap.Round)
            }
        }
    }
}

/** Theme::chamfer(rect, cut) : coins haut-gauche et bas-droit coupés à 45°. */
private fun chamfer(r: Rect, cut: Float): Path {
    val c = min(cut, min(r.width, r.height) / 2f)
    return shape {
        moveTo(r.left + c, r.top); lineTo(r.right, r.top); lineTo(r.right, r.bottom - c)
        lineTo(r.right - c, r.bottom); lineTo(r.left, r.bottom); lineTo(r.left, r.top + c)
    }
}

/** Icône « lock » de la charte (grille 24 × 24), tracée dans `rect`. */
private fun DrawScope.drawLockIcon(rect: Rect, color: Color) {
    val s = min(rect.width, rect.height) / 24f
    val icon = Path().apply {
        moveTo(6f, 11f); lineTo(18f, 11f); lineTo(18f, 21f); lineTo(6f, 21f); close()
        moveTo(8.5f, 11f); lineTo(8.5f, 7.5f)
        arcTo(Rect(8.5f, 4f, 15.5f, 11f), 180f, 180f, forceMoveTo = false)
        lineTo(15.5f, 11f)
    }
    withTransform({ translate(rect.center.x - 12f * s, rect.center.y - 12f * s); scale(s, s, Offset.Zero) }) {
        drawPath(icon, color, style = Stroke(1.9f, cap = StrokeCap.Square, join = StrokeJoin.Miter))
    }
}

/** Toupie gelée (coque de glace) ou sonnée (anneau en pointillés et cadenas). */
fun DrawScope.drawStatus(s: Spinner, pos: Offset, radius: Float, time: Double) {
    if (!s.isActive) return
    val px = density
    if (s.isSlowed) {
        // Gelée : coque de glace hexagonale.
        val r = radius * 1.38f
        val spin = (time * 0.3).toFloat()
        val ice = Color(0xFF8FF3FF)
        val shell = shape {
            for (k in 0 until 6) {
                val q = pos + dir(k * PIF / 3 + spin) * r
                if (k == 0) moveTo(q) else lineTo(q)
            }
        }
        drawPath(shell, ice.withAlpha(0.18f))
        drawPath(shell, Color(255, 255, 255, 210), style = pen(1.5f * px))
        val inner = shape {
            for (k in 0 until 6) {
                val q = pos + dir(k * PIF / 3 + spin + 0.5f) * (r * 0.84f)
                if (k == 0) moveTo(q) else lineTo(q)
            }
        }
        drawPath(inner, ice.withAlpha(0.7f), style = pen(px))
    }
    if (s.isDisarmed) {
        drawCircle(Holo.AccentBright, radius * 1.3f, pos, style = pen(1.8f * px, dashes = floatArrayOf(2f, 3f)))
        val lock = Rect(Offset(pos.x + radius * 0.9f, pos.y - radius * 1.6f), Size(18f * px, 18f * px))
        drawPath(chamfer(lock.inflate(3f * px), 4f * px), Holo.Field.withAlpha(0.9f))
        drawLockIcon(lock, Holo.AccentBright)
    }
}

/**
 * Coups spéciaux en vol, chacun avec sa signature (anneaux, lance, givre…).
 * @param scale        pixels par unité de monde
 * @param topRadius    rayon d'une toupie (unités du monde)
 * @param stadiumRadius rayon du stadium (unités du monde)
 * @param time         temps écoulé (s), anime tourbillons, éclats et gouttes
 */
fun DrawScope.drawProjectiles(
    projectiles: List<Projectile>,
    toScreen: ToScreen,
    scale: Float,
    topRadius: Float,
    stadiumRadius: Float,
    time: Double,
) {
    val px = density
    val tm = time.toFloat()
    for (proj in projectiles) {
        val color = rgb(proj.color)
        val fade = (1.0 - proj.traveled / max(1e-6, proj.range)).coerceIn(0.0, 1.0).toFloat()
        val alpha = 0.4f + 0.6f * fade
        val width = proj.width.toFloat()
        if (proj.ring) {
            val c = toScreen(proj.origin)
            val r = proj.radius.toFloat() * scale
            drawCircle(color.withAlpha(0.3f), proj.range.toFloat() * scale, c, style = pen(px, dashes = floatArrayOf(2f, 6f)))
            when (proj.move) {
                "vortex" -> {
                    // Le front de l'anneau part du lanceur et touche au passage…
                    drawCircle(color.withAlpha(0.25f * alpha), r, c, style = Stroke(width * scale * 3f))
                    drawCircle(color.withAlpha(alpha), r, c, style = Stroke(2f * px))
                    // … et le tourbillon, au centre du stadium, y aspire les toupies touchées.
                    // shortcut: dessiné tant que l'anneau vole, comme sur PC v2.0.0 ; quand le moteur exposera
                    // pullTimer / isPulled, le garder visible pendant toute l'aspiration.
                    val hub = toScreen(Vec2.ZERO)
                    val reach = stadiumRadius * scale * 0.5f
                    for (k in 0 until 4) {
                        val arm = Path()
                        for (i in 0..36) {
                            val q = hub + dir(k * PIF / 2 + i * 0.14f + tm * 3f) * (reach * (1f - i / 40f))
                            if (i == 0) arm.moveTo(q) else arm.lineTo(q)
                        }
                        strokeGlow(arm, color.withAlpha(0.8f * alpha), 2.2f * px)
                    }
                    for (k in 0 until 18) {
                        val f = (time * 0.9 + k / 18.0).rem(1.0).toFloat() // 0 dehors, 1 au centre
                        val a = k * 2.4f + f * 3f + tm * 3f
                        drawCircle(Color.White.withAlpha(alpha * (0.3f + 0.7f * f)), (1.5f + 1.5f * f) * px,
                            hub + dir(a) * (reach * 1.1f * (1f - f)))
                    }
                    drawCircle(Color(0, 0, 0, 200), reach * 0.14f, hub)
                    drawCircle(color, reach * 0.14f, hub, style = Stroke(2f * px))
                }
                "seisme" -> {
                    // Failles qui courent jusqu'au front de l'onde.
                    for (k in 0 until 10) {
                        var a = k * PIF / 5 + pseudoRandom(proj.id * 31 + k)
                        var rr = r * 0.15f
                        val fault = Path()
                        fault.moveTo(c + dir(a) * rr)
                        var step = 0
                        while (rr < r && step < 12) {
                            rr += r * (0.1f + 0.08f * pseudoRandom(proj.id + k * 7 + step))
                            a += (pseudoRandom(k * 13 + step) - 0.5f) * 0.5f
                            fault.lineTo(c + dir(a) * min(rr, r))
                            step++
                        }
                        strokeGlow(fault, color.withAlpha(alpha), 1.8f * px)
                    }
                    drawCircle(color.withAlpha(alpha), r, c, style = pen(2f * px, dashes = floatArrayOf(3f, 4f)))
                }
                else -> {
                    // Onde de choc : anneaux, décalage chromatique et éclats.
                    drawCircle(color.withAlpha(0.25f * alpha), r, c, style = Stroke(width * scale * 3f))
                    drawCircle(Holo.Magenta.withAlpha(0.5f * alpha), r + 4f * px, c, style = Stroke(1.5f * px))
                    drawCircle(color.withAlpha(alpha), r, c, style = Stroke(max(2.5f * px, width * scale * 0.8f)))
                    drawCircle(color.withAlpha(0.45f * alpha), r * 0.7f, c, style = Stroke(1.5f * px))
                    for (k in 0 until 14) {
                        val d = dir(k * PIF / 7 + pseudoRandom(proj.id + k) * 0.3f)
                        val side = Offset(-d.y, d.x)
                        val base = c + d * (r + 6f * px)
                        drawPath(shape {
                            moveTo(base + side * (3f * px)); lineTo(base + d * (12f * px)); lineTo(base - side * (3f * px))
                        }, color.withAlpha(alpha))
                    }
                }
            }
            continue
        }

        // Vague : un front qui file vers sa cible, avec une signature propre.
        val c = toScreen(proj.position)
        val angle = atan2(proj.direction.y, proj.direction.x).toFloat() * 180f / PIF
        val half = (width + topRadius * 0.6f) * scale
        withTransform({ translate(c.x, c.y); rotate(angle, Offset.Zero) }) {
            // QPainter::setOpacity : chaque tracé est atténué de `alpha`.
            when (proj.move) {
                "lance" -> {
                    for (i in 0 until 7) {
                        val y = (pseudoRandom(proj.id * 5 + i) - 0.5f) * half * 1.6f
                        val x0 = -half * (2.5f + 3f * pseudoRandom(i + 3))
                        drawLine(color.withAlpha(0.6f), Offset(x0, y), Offset(x0 + half * 1.6f, y), 1.3f * px,
                            StrokeCap.Square, alpha = alpha)
                    }
                    val shaft = Path().apply { moveTo(-half * 4f, 0f); lineTo(-half * 0.3f, 0f) }
                    strokeGlow(shaft, color, 3f * px, alpha)
                    val head = shape {
                        moveTo(half * 0.9f, 0f); lineTo(-half * 0.45f, -half * 0.6f); lineTo(-half * 0.15f, 0f); lineTo(-half * 0.45f, half * 0.6f)
                    }
                    drawPath(head, Color.White, alpha)
                    drawPath(head, color, alpha, pen(2f * px))
                }
                "givre" -> {
                    drawPath(shape {
                        moveTo(half * 0.2f, -half * 0.5f); lineTo(half * 0.2f, half * 0.5f)
                        lineTo(-half * 4f, half * 1.4f); lineTo(-half * 4f, -half * 1.4f)
                    }, Brush.linearGradient(0f to color.withAlpha(0.5f), 1f to color.withAlpha(0f),
                        start = Offset.Zero, end = Offset(-half * 4f, 0f)), alpha)
                    for (i in 0 until 8) {
                        val x = -half * ((pseudoRandom(proj.id + i) * 4.0 + time * 3.0).rem(4.0)).toFloat()
                        val y = (pseudoRandom(i * 11 + proj.id) - 0.5f) * (half * 0.6f - x * 0.6f)
                        drawEmblem("givre", Offset(x, y), half * 0.18f, Color.White, 1.2f * px, alpha)
                    }
                    val front = Path().apply { moveTo(-half * 0.2f, -half); quadraticTo(half * 0.6f, 0f, -half * 0.2f, half) }
                    strokeGlow(front, color, 3f * px, alpha)
                }
                "distorsion" -> {
                    for (i in 0 until 7) {
                        val jitter = sin(tm * 37f + i * 13f) * half * 0.5f
                        val bar = if (i % 2 == 1) Holo.Magenta else Holo.Cyan
                        drawRect(bar.withAlpha(0.75f), Offset(-half * 3.2f + jitter, -half + i * half * 0.3f),
                            Size(half * 2.8f, half * 0.16f), alpha)
                    }
                    val front = Path().apply { moveTo(-half * 0.3f, -half); quadraticTo(half * 0.5f, 0f, -half * 0.3f, half) }
                    strokeGlow(front, Holo.Magenta, 3f * px, alpha)
                }
                "siphon" -> {
                    for (k in -1..1) {
                        val tendril = Path()
                        for (i in 0..24) {
                            val x = -half * 4f * i / 24f
                            val y = k * half * 0.35f + sin(i * 0.6f - tm * 10f + k) * half * 0.25f
                            if (i == 0) tendril.moveTo(0f, y) else tendril.lineTo(x, y)
                        }
                        strokeGlow(tendril, color, (if (k != 0) 1.2f else 2.6f) * px, alpha)
                    }
                    for (i in 0 until 5) {
                        val x = -half * ((i * 0.8 + time * 4.0).rem(4.0)).toFloat()
                        val drop = Path().apply {
                            moveTo(x, -half * 0.22f)
                            cubicTo(x + half * 0.12f, -half * 0.05f, x + half * 0.14f, half * 0.08f, x, half * 0.14f)
                            cubicTo(x - half * 0.14f, half * 0.08f, x - half * 0.12f, -half * 0.05f, x, -half * 0.22f)
                        }
                        drawPath(drop, color, alpha)
                    }
                }
                else -> for (k in 4 downTo 0) {
                    val back = k * half * 0.45f
                    val w = half * (1f - k * 0.12f)
                    val arc = Path().apply { moveTo(-back - w * 0.35f, -w); quadraticTo(-back + w * 0.55f, 0f, -back - w * 0.35f, w) }
                    drawPath(arc, color.withAlpha(if (k == 0) 0.95f else 0.18f), alpha,
                        Stroke((if (k == 0) 4f else 6f) * px, cap = StrokeCap.Round))
                }
            }
        }
    }
}
