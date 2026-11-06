package fr.toupiearena.mobile.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.DuelSide
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val MIN_ARROW = 0.14 // Longueur de flèche (monde) à la force minimale.
private const val MAX_ARROW = 0.6  // … et à la force maximale.
private const val TRAIL_LENGTH = 36
private const val SPARK_LIFETIME = 0.4
private const val EMBER_LIFETIME = 0.35

/** Effets visuels de l'arène (traînées, étincelles, bandeaux). */
private class ArenaVisuals {
    class Spark(val position: Vec2, val intensity: Double, val color: Color?, var age: Double = 0.0)
    class Ember(var position: Vec2, var velocity: Vec2, var age: Double = 0.0)

    val trails = mutableMapOf<Int, ArrayDeque<Vec2>>()
    val sparks = mutableListOf<Spark>()
    val embers = mutableListOf<Ember>()
    var flash = 0.0
    var duelResult = ""
    var duelResultColor = Palette.text
    var duelResultTimer = 0.0
    var lastPhase = MatchPhase.Aiming

    fun tick(session: MatchSession, dt: Double) {
        val state = session.state
        if (state.phase != lastPhase) {
            if (state.phase == MatchPhase.Fighting) flash = 0.8
            if (state.phase == MatchPhase.Aiming) { trails.clear(); sparks.clear(); embers.clear() }
            lastPhase = state.phase
        }
        val local = session.localPlayer
        for (impact in session.takeImpacts()) {
            sparks += Spark(impact.point, impact.intensity, if (impact.special) rgb(impact.color) else null)
            if (impact.duelLoser == ImpactEvent.NO_DUEL) continue
            val involved = impact.first == local || impact.second == local
            when {
                impact.duelLoser < 0 -> { duelResult = "Duel nul !"; duelResultColor = Palette.text }
                involved -> {
                    val lost = impact.duelLoser == local
                    duelResult = if (lost) "Duel perdu…" else "Duel gagné !"
                    duelResultColor = if (lost) Palette.danger else Palette.accent
                }
                else -> {
                    duelResult = "${state.names.getOrElse(impact.duelLoser) { "?" }} perd le duel"
                    duelResultColor = slotColor(impact.duelLoser)
                }
            }
            duelResultTimer = 1.6
        }
        if (state.phase == MatchPhase.Fighting) {
            state.spinners.forEachIndexed { i, s ->
                if (!s.isActive) return@forEachIndexed
                val trail = trails.getOrPut(i) { ArrayDeque() }
                trail.addLast(s.position)
                while (trail.size > TRAIL_LENGTH) trail.removeFirst()
            }
        }
        // Duel : gerbes d'étincelles de part et d'autre du point de friction.
        if (state.duel.active) {
            val side = state.duel.normal.perpendicular()
            repeat(6) { k ->
                val sign = if (k % 2 == 0) 1.0 else -1.0
                val dir = (side * sign + state.duel.normal * ((Random.nextDouble() - 0.5) * 1.2)).normalized()
                embers += Ember(state.duel.point, dir * (0.4 + 0.8 * Random.nextDouble()))
            }
        }
        sparks.forEach { it.age += dt }
        sparks.removeAll { it.age > SPARK_LIFETIME }
        embers.forEach { it.age += dt; it.position += it.velocity * dt; it.velocity = it.velocity * 0.92 }
        embers.removeAll { it.age > EMBER_LIFETIME }
        flash = max(0.0, flash - dt)
        duelResultTimer = max(0.0, duelResultTimer - dt)
    }
}

/** Repère de l'arène : conversion monde (rayons de stadium) -> pixels. */
private class ArenaFrame(val size: IntSize, radius: Double) {
    val scale = (min(size.width, size.height) / 2.0 * 0.9 / max(0.1, radius)).toFloat()
    val center = Offset(size.width / 2f, size.height / 2f)
    fun toScreen(p: Vec2) = Offset(center.x + (p.x * scale).toFloat(), center.y + (p.y * scale).toFloat())
    fun toWorld(o: Offset) = Vec2(((o.x - center.x) / scale).toDouble(), ((o.y - center.y) / scale).toDouble())
}

/**
 * Écran de combat (paysage) : arène au centre, panneaux des joueurs et commandes
 * de part et d'autre. Vise en glissant depuis sa toupie (direction + force),
 * relâcher lance. Les commandes du combat suivent le schéma choisi.
 */
@Composable
fun BattleScreen(
    session: MatchSession,
    scheme: ControlScheme,
    tiltSensitivity: Float,
    status: String,
    leaveLabel: String,
    onLeave: () -> Unit,
    finishedActions: @Composable () -> Unit = {},
) {
    val tutorial = session as? TutorialSession
    val visuals = remember(session) { ArenaVisuals() }
    var frame by remember { mutableLongStateOf(0L) }
    var arenaSize by remember { mutableStateOf(IntSize.Zero) }
    var aim by remember(session) {
        mutableStateOf(Pair((-session.state.startPositions.getOrElse(session.localPlayer) { Vec2() }).angle(), 1.0))
    }
    var aiming by remember(session) { mutableStateOf(false) }
    val density = LocalDensity.current

    BackHandler(onBack = onLeave)

    // Boucle de jeu : une mise à jour par image.
    LaunchedEffect(session) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016 else min(0.05, (now - last) / 1e9)
                last = now
                session.update(dt)
                visuals.tick(session, dt)
                frame = now
            }
        }
    }

    fun allowed(a: TouchAction) = tutorial?.handleAction(a) ?: true
    fun steer(v: Vec2) {
        if (v.lengthSquared() > 0.04 && !allowed(TouchAction.Steer)) return
        session.setSteering(v)
    }
    fun dash() { if (allowed(TouchAction.Dash)) session.requestDash() }
    fun special() { if (allowed(TouchAction.Special)) session.requestSpecial() }

    frame // Recomposition à chaque image.
    val state = session.state
    val fighting = state.phase == MatchPhase.Fighting
    val localInDuel = state.duel.sideOf(session.localPlayer) >= 0
    val tutorialPaused = tutorial?.isPaused == true && state.phase != MatchPhase.Aiming

    if (scheme == ControlScheme.Tilt && fighting) TiltSteering(tiltSensitivity) { steer(it) }

    Box(
        Modifier.fillMaxSize().background(Palette.backgroundDeep)
            // Toucher l'écran : appui en duel, reprise du tutoriel, gestes.
            .pointerInput(session, scheme) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (session.state.phase != MatchPhase.Fighting) return@awaitEachGesture
                    if (session.state.duel.sideOf(session.localPlayer) >= 0 || (tutorial?.isPaused == true && scheme != ControlScheme.Gestures)) {
                        dash()
                        return@awaitEachGesture
                    }
                    if (scheme != ControlScheme.Gestures) return@awaitEachGesture
                    val start = down.position
                    val startTime = down.uptimeMillis
                    val full = with(density) { 60.dp.toPx() }
                    var moved = false
                    var special = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.position - start
                        if (delta.getDistance() > viewConfiguration.touchSlop) moved = true
                        if (moved) steer(Vec2((delta.x / full).toDouble(), (delta.y / full).toDouble()).let {
                            if (it.length() > 1.0) it.normalized() else it
                        })
                        if (!moved && !special && change.uptimeMillis - startTime > 400) {
                            special = true
                            special()
                        }
                        if (!change.pressed) break
                    }
                    if (moved) steer(Vec2.ZERO) else if (!special) dash()
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            // --- Colonne gauche : joueurs 0 et 2, joystick ------------------------------
            Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton(leaveLabel, onLeave)
                state.spinners.indices.filter { it % 2 == 0 }.forEach { SpinnerPanel(session, it) }
                Spacer(Modifier.weight(1f))
                if (fighting && scheme == ControlScheme.Buttons && !localInDuel) Joystick { steer(it) }
                Text(status, color = Palette.muted, fontSize = 11.sp)
            }
            // --- Arène ----------------------------------------------------------------------
            Box(Modifier.fillMaxHeight().aspectRatio(1f).onSizeChanged { arenaSize = it }) {
                val textMeasurer = rememberTextMeasurer()
                Canvas(
                    Modifier.fillMaxSize().pointerInput(session) {
                        // Visée : glisser depuis sa toupie, relâcher pour lancer.
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (session.state.phase != MatchPhase.Aiming || session.localHasAimed) return@awaitEachGesture
                            if (!allowed(TouchAction.Aim)) return@awaitEachGesture
                            aiming = true
                            var position = down.position
                            var dragged = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                position = change.position
                                change.consume()
                                val f = ArenaFrame(arenaSize, session.state.geometry.radius)
                                val start = session.state.startPositions[session.localPlayer]
                                val v = f.toWorld(position) - start
                                if (v.length() > 0.05) {
                                    dragged = true
                                    val cfg = session.state.config
                                    val t = ((v.length() - MIN_ARROW) / (MAX_ARROW - MIN_ARROW)).coerceIn(0.0, 1.0)
                                    aim = v.angle() to cfg.launchPowerMin + t * (cfg.launchPowerMax - cfg.launchPowerMin)
                                }
                                if (!change.pressed) break
                            }
                            aiming = false
                            if (dragged && allowed(TouchAction.Launch)) session.submitAim(aim.first, aim.second)
                        }
                    },
                ) {
                    frame // Redessin à chaque image.
                    if (size.minDimension <= 0f) return@Canvas
                    // Repère calculé à partir de la taille réelle du dessin.
                    val arenaFrame = ArenaFrame(IntSize(size.width.toInt(), size.height.toInt()), state.geometry.radius)
                    drawStadium(state.stadium, state.geometry, arenaFrame.center, arenaFrame.scale)
                    drawArena(session, visuals, arenaFrame, aim, textMeasurer)
                }
                ArenaOverlay(session, visuals, aiming)
                tutorial?.let { TutorialBubble(it) }
            }
            // --- Colonne droite : joueurs 1 et 3, chrono, boutons ---------------------------
            Column(Modifier.weight(1f).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End) {
                TimerText(session)
                state.spinners.indices.filter { it % 2 == 1 }.forEach { SpinnerPanel(session, it) }
                Spacer(Modifier.weight(1f))
                if (fighting && scheme != ControlScheme.Gestures && !(tutorialPaused && localInDuel)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val canSpecial = state.config.specialsEnabled &&
                            !state.spinners[session.localPlayer].specialUsed && !localInDuel
                        if (state.config.specialsEnabled) RoundAction("SPÉCIAL", 74, canSpecial) { special() }
                        HSpace(10)
                        RoundAction(if (localInDuel) "APPUI" else "DASH", 104, true) { dash() }
                    }
                }
            }
        }
        if (state.phase == MatchPhase.Finished) FinishedOverlay(session, tutorial, finishedActions, onLeave, leaveLabel)
    }
}

// ---------------------------------------------------------------------------
// Dessin de l'arène
// ---------------------------------------------------------------------------

private fun DrawScope.drawArena(
    session: MatchSession, visuals: ArenaVisuals, f: ArenaFrame, aim: Pair<Double, Double>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val state = session.state
    val radius = (state.config.topRadius * f.scale).toFloat()

    // Points de départ.
    if (state.phase == MatchPhase.Aiming || state.phase == MatchPhase.Countdown) {
        state.startPositions.forEachIndexed { i, p ->
            val aimed = state.aimed.getOrElse(i) { false }
            drawCircle(slotColor(i).copy(alpha = if (aimed) 0.8f else 0.45f), radius * 1.5f, f.toScreen(p), style = Stroke(3f))
        }
    }
    // Flèche de visée : plus longue et épaisse = plus puissante.
    if (state.phase == MatchPhase.Aiming && !session.localHasAimed) {
        val cfg = state.config
        val t = ((aim.second - cfg.launchPowerMin) / max(1e-6, cfg.launchPowerMax - cfg.launchPowerMin)).toFloat()
        val start = state.startPositions[session.localPlayer]
        val dir = Vec2.fromAngle(aim.first)
        val from = f.toScreen(start + dir * (cfg.topRadius * 1.3))
        val to = f.toScreen(start + dir * (MIN_ARROW + t * (MAX_ARROW - MIN_ARROW)))
        val color = slotColor(session.localPlayer)
        drawLine(color.copy(alpha = 0.3f), from, to, 14f + 10f * t, StrokeCap.Round)
        drawLine(color, from, to, 5f + 5f * t, StrokeCap.Round)
        val d = (to - from) / max(1f, (to - from).getDistance())
        val n = Offset(-d.y, d.x)
        val size = 18f + 8f * t
        drawPath(Path().apply {
            moveTo((to + d * size).x, (to + d * size).y)
            lineTo((to + n * (size * 0.65f)).x, (to + n * (size * 0.65f)).y)
            lineTo((to - n * (size * 0.65f)).x, (to - n * (size * 0.65f)).y)
            close()
        }, color)
    }
    // Traînées.
    visuals.trails.forEach { (i, trail) ->
        val base = typeColor(state.spinners.getOrNull(i)?.top?.type ?: return@forEach)
        val points = trail.toList()
        for (k in 1 until points.size) {
            val a = k.toFloat() / points.size
            drawLine(base.copy(alpha = 0.35f * a), f.toScreen(points[k - 1]), f.toScreen(points[k]), radius * 0.5f * a, StrokeCap.Round)
        }
    }
    // Toupies.
    state.spinners.forEachIndexed { i, s ->
        var pos = f.toScreen(s.position)
        var r = radius
        var alpha = if (s.status != Spinner.Status.Waiting && !s.isActive) 0.4f else 1f
        if (state.duel.sideOf(i) >= 0) {
            val t = state.elapsed * 60.0 + i * 1.7
            pos += Offset((sin(t * 1.3) * r * 0.06).toFloat(), (cos(t * 1.7) * r * 0.06).toFloat())
        }
        if (s.isActive && s.spinRatio < state.config.wobbleThreshold) {
            val amount = r * 0.15f * (1f - (s.spinRatio / state.config.wobbleThreshold).toFloat())
            pos += Offset((cos(s.angle * 3) * amount).toFloat(), (sin(s.angle * 3) * amount).toFloat())
        }
        if (s.status == Spinner.Status.RingOut) {
            pos = f.toScreen(s.position + state.geometry.outwardNormal(s.position) * (state.config.topRadius * 1.6))
            r *= 0.75f
            alpha = 0.35f
        }
        val glow = if (s.isBoosted) (s.boostTimer / state.config.dashBoostDuration).toFloat() else 0f
        drawTop(s.top, pos, r, s.angle, glow, alpha)
        val label = if (i == session.localPlayer) "VOUS" else state.names.getOrElse(i) { "" }
        val layout = textMeasurer.measure(label, TextStyle(color = slotColor(i), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - r - layout.size.height - 4f))
    }
    // Vagues des coups spéciaux.
    for (p in state.projectiles) {
        val color = rgb(p.color)
        val fade = (1.0 - p.traveled / max(1e-6, p.range)).coerceIn(0.0, 1.0).toFloat()
        if (p.ring) {
            val c = f.toScreen(p.origin)
            drawCircle(color.copy(alpha = 0.25f * (0.3f + 0.7f * fade)), (p.radius * f.scale).toFloat(), c,
                style = Stroke((p.width * f.scale * 3).toFloat()))
            drawCircle(color.copy(alpha = 0.4f + 0.6f * fade), (p.radius * f.scale).toFloat(), c,
                style = Stroke(max(2f, (p.width * f.scale * 0.8).toFloat())))
        } else {
            val c = f.toScreen(p.position)
            val half = ((p.width + state.config.topRadius * 0.6) * f.scale).toFloat()
            val d = Offset(p.direction.x.toFloat(), p.direction.y.toFloat())
            val n = Offset(-d.y, d.x)
            for (k in 4 downTo 0) {
                val back = c - d * (k * half * 0.45f)
                val w = half * (1f - k * 0.12f)
                val path = Path().apply {
                    val a = back - d * (w * 0.35f) - n * w
                    val ctrl = back + d * (w * 0.55f)
                    val b = back - d * (w * 0.35f) + n * w
                    moveTo(a.x, a.y)
                    quadraticTo(ctrl.x, ctrl.y, b.x, b.y)
                }
                drawPath(path, color.copy(alpha = (if (k == 0) 0.95f else 0.18f) * (0.4f + 0.6f * fade)),
                    style = Stroke(if (k == 0) 5f else 8f, cap = StrokeCap.Round))
            }
        }
    }
    // Étincelles de duel et de choc.
    for (e in visuals.embers) {
        val t = (e.age / EMBER_LIFETIME).toFloat()
        val head = f.toScreen(e.position)
        val tail = head - Offset((e.velocity.x * f.scale * 0.04).toFloat(), (e.velocity.y * f.scale * 0.04).toFloat())
        drawLine(Color(0xFFFFD25A).copy(alpha = 1f - t), tail, head, 3f, StrokeCap.Round)
    }
    for (s in visuals.sparks) {
        val t = (s.age / SPARK_LIFETIME).toFloat()
        val c = f.toScreen(s.position)
        val size = radius * (0.6f + 1.6f * t) * (0.6f + s.intensity.toFloat())
        val color = (s.color ?: Color(0xFFFFE678)).copy(alpha = 1f - t)
        drawCircle(color, size, c, style = Stroke(3f))
        repeat(6) { k ->
            val a = k * PI / 3 + s.intensity
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(color, c + dir * (size * 0.6f), c + dir * (size * 1.2f), 3f)
        }
    }
    // Barres de duel : la mienne en bas, celle de l'adversaire au-dessus en plus petit.
    if (state.duel.active && state.phase == MatchPhase.Fighting) {
        val mine = state.duel.sideOf(session.localPlayer)
        val big = size.width * 0.8f
        val small = big * 0.6f
        if (mine >= 0) {
            drawDuelBar(Offset(size.width / 2 - small / 2, size.height - 70f), small, 14f, state.duel.sides[1 - mine], state.config.duelStreak)
            drawDuelBar(Offset(size.width / 2 - big / 2, size.height - 40f), big, 28f, state.duel.sides[mine], state.config.duelStreak)
        } else {
            for (k in 0..1) drawDuelBar(Offset(size.width / 2 - small / 2, size.height - 70f + k * 34f), small, 14f,
                state.duel.sides[k], state.config.duelStreak)
        }
    }
}

private fun DrawScope.drawDuelBar(topLeft: Offset, width: Float, height: Float, side: DuelSide, streak: Int) {
    val r = androidx.compose.ui.geometry.CornerRadius(height / 2)
    drawRoundRect(Color(0xFFC83A3A), topLeft, androidx.compose.ui.geometry.Size(width, height), r)
    val half = width / 2
    val zone = if (side.done) half else (side.zone * half).toFloat()
    drawRoundRect(Color(0xFF54D66E), Offset(topLeft.x + half - zone, topLeft.y),
        androidx.compose.ui.geometry.Size(2 * zone, height), r)
    val x = topLeft.x + half + (side.cursor.coerceIn(-1.0, 1.0) * half).toFloat()
    drawLine(Color.White.copy(alpha = 0.35f), Offset(x, topLeft.y - 5), Offset(x, topLeft.y + height + 5), height * 0.4f, StrokeCap.Round)
    drawLine(Color.White, Offset(x, topLeft.y - 5), Offset(x, topLeft.y + height + 5), height * 0.14f + 2f, StrokeCap.Round)
    val dot = height * 0.28f
    repeat(streak) { k ->
        val c = Offset(topLeft.x + width + 12f + dot + k * dot * 2.6f, topLeft.y + height / 2)
        if (k < side.streak) drawCircle(Color(0xFF54D66E), dot, c) else drawCircle(Color(0xFF54D66E), dot, c, style = Stroke(2f))
    }
}

// ---------------------------------------------------------------------------
// Superpositions et panneaux
// ---------------------------------------------------------------------------

// L'état de la session n'est pas observable : ces éléments se recomposent avec l'écran, à chaque image.
@NonSkippableComposable
@Composable
private fun ArenaOverlay(session: MatchSession, visuals: ArenaVisuals, aiming: Boolean) {
    val state = session.state
    Box(Modifier.fillMaxSize()) {
        when (state.phase) {
            MatchPhase.Aiming -> if (session !is TutorialSession) { // Le tutoriel l'explique dans sa bulle.
                val hint = if (session.localHasAimed) {
                    "En attente des autres joueurs (${state.aimed.count { it }}/${state.playerCount})"
                } else if (aiming) "Relâchez pour lancer" else "Glissez depuis votre toupie pour viser, relâchez pour lancer"
                Banner(hint, Modifier.align(Alignment.TopCenter).padding(top = 6.dp), 14)
            }
            MatchPhase.Countdown -> Banner(max(1, ceil(state.countdown).toInt()).toString(), Modifier.align(Alignment.Center), 64)
            MatchPhase.Fighting -> {
                if (visuals.flash > 0) Banner("Partez !", Modifier.align(Alignment.Center), 44, Palette.warning)
                if (visuals.duelResultTimer > 0) Banner(visuals.duelResult, Modifier.align(Alignment.TopCenter).padding(top = 40.dp), 30, visuals.duelResultColor)
                val mine = state.duel.sideOf(session.localPlayer)
                if (state.duel.active && mine >= 0) {
                    val text = if (state.duel.sides[mine].done) "Série réussie ! En attente de l'adversaire…"
                    else "DUEL ! Touchez l'écran quand le curseur est dans le vert"
                    Banner(text, Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp), 13, Palette.accent)
                }
            }
            MatchPhase.Finished -> Unit
        }
        state.series?.let { series ->
            if (state.phase != MatchPhase.Finished) {
                Text("Manche ${series.rounds + 1} · ${series.score[0]} – ${series.score[1]}", color = Palette.accent,
                    fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun Banner(text: String, modifier: Modifier, size: Int, color: Color = Color.White) {
    Text(text, modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@NonSkippableComposable
@Composable
private fun TimerText(session: MatchSession) {
    val state = session.state
    val text = if (state.config.maxDuration > 0.0) {
        ceil(max(0.0, state.config.maxDuration - state.elapsed)).toInt().toString()
    } else {
        val s = state.elapsed.toInt()
        "%d:%02d".format(s / 60, s % 60)
    }
    Text(text, color = Palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Stadium ${state.stadium.name}", color = Palette.muted, fontSize = 11.sp)
}

/** Panneau d'un joueur : nom, toupie, endurance, coup spécial, état. */
@NonSkippableComposable
@Composable
private fun SpinnerPanel(session: MatchSession, index: Int) {
    val state = session.state
    val s = state.spinners[index]
    val local = index == session.localPlayer
    val accent = slotColor(index)
    Column(
        Modifier.widthIn(max = 230.dp).fillMaxWidth()
            .border(if (local) 2.dp else 1.dp, accent.copy(alpha = if (local) 0.9f else 0.35f), RoundedCornerShape(10.dp))
            .background(Palette.panel.copy(alpha = 0.92f), RoundedCornerShape(10.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("${if (local) "Vous" else state.names.getOrElse(index) { "" }} — ${s.top.name} (${s.top.type.label})",
            color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        val ratio = s.spinRatio.coerceIn(0.0, 1.0).toFloat()
        Box(Modifier.fillMaxWidth().height(16.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().background(
                if (ratio > 0.5f) Color(0xFF46C86E) else if (ratio > 0.25f) Color(0xFFEBB932) else Color(0xFFE6463C), RoundedCornerShape(5.dp)))
            Text("Endurance ${s.spin.toInt()}", Modifier.align(Alignment.Center), color = Color.White, fontSize = 10.sp, lineHeight = 12.sp)
        }
        SpecialMove.find(s.top.special)?.takeIf { state.config.specialsEnabled }?.let { move ->
            val available = !s.specialUsed && s.isActive && !s.isDisarmed
            Text(if (s.specialUsed) "${move.name} — utilisé" else "${move.name} (coûte ¼ de l'endurance)",
                Modifier.fillMaxWidth().background(if (available) rgb(move.color) else Color(0xFF464C54), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                color = if (available) Color(0xFF14161A) else Color(0xFFA0A6AE), fontSize = 10.sp, maxLines = 1)
        }
        val (text, color) = spinnerStatus(state, s, index)
        Text(text, color = color, fontSize = 11.sp, maxLines = 1)
    }
}

private fun spinnerStatus(state: fr.toupiearena.mobile.session.MatchState, s: Spinner, index: Int): Pair<String, Color> = when {
    s.status == Spinner.Status.RingOut -> "Éjectée" to Palette.danger
    s.status == Spinner.Status.Stopped -> "À l'arrêt" to Palette.danger
    s.status == Spinner.Status.Forfeit -> "Déconnectée" to Palette.danger
    state.duel.sideOf(index) >= 0 -> "DUEL !" to Palette.warning
    s.isSlowed -> "Gelée (%.1f s)".format(s.slowTimer) to Color(0xFF9BE7FF)
    s.isDisarmed -> "Sonnée (%.1f s)".format(s.disarmTimer) to Color(0xFFFFB347)
    state.phase == MatchPhase.Aiming -> if (state.aimed.getOrElse(index) { false }) "Prêt à lancer" to Palette.accent else "Vise…" to Palette.muted
    s.isBoosted -> "DASH !" to Palette.warning
    s.dashCooldown > 0.0 -> "Dash dans %.1f s".format(s.dashCooldown) to Palette.muted
    s.spin <= state.config.dashCost -> "Dash impossible" to Palette.danger
    else -> "Dash prêt" to Palette.accent
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    Text(text, Modifier.border(1.dp, Palette.border, RoundedCornerShape(8.dp))
        .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }.padding(horizontal = 10.dp, vertical = 6.dp),
        color = Palette.text, fontSize = 12.sp)
}

/** Gros bouton rond, déclenché dès l'appui (réactivité en combat). */
@Composable
private fun RoundAction(label: String, sizeDp: Int, enabled: Boolean, onPress: () -> Unit) {
    Box(
        Modifier.size(sizeDp.dp).background(if (enabled) Palette.accent.copy(alpha = 0.85f) else Palette.panelAlt, CircleShape)
            .border(2.dp, if (enabled) Palette.accentBright else Palette.border, CircleShape)
            .pointerInput(enabled) { detectTapGestures(onPress = { if (enabled) onPress() }) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) Color(0xFF10140D) else Palette.muted, fontWeight = FontWeight.Bold,
            fontSize = (sizeDp / 6).sp)
    }
}

/** Joystick virtuel : vecteur de pilotage (longueur ≤ 1) tant que le doigt est posé. */
@Composable
private fun Joystick(onSteer: (Vec2) -> Unit) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        Modifier.size(140.dp).background(Palette.panelAlt.copy(alpha = 0.7f), CircleShape).border(2.dp, Palette.border, CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val radius = size.width / 2f
                    val center = Offset(radius, radius)
                    var position = down.position
                    while (true) {
                        val v = (position - center) / radius
                        val len = v.getDistance()
                        knob = if (len > 1f) v / len else v
                        onSteer(Vec2(knob.x.toDouble(), knob.y.toDouble()))
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        position = change.position
                        if (!change.pressed) break
                    }
                    knob = Offset.Zero
                    onSteer(Vec2.ZERO)
                }
            },
    ) {
        val radius = constraints.maxWidth / 2f
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Palette.accent.copy(alpha = 0.85f), radius * 0.35f, center + knob * (radius * 0.65f))
        }
    }
}

/** Pilotage par inclinaison : la position au début du combat sert de neutre. */
@Composable
private fun TiltSteering(sensitivity: Float, onSteer: (Vec2) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(sensitivity) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        @Suppress("DEPRECATION")
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        var neutral: Vec2? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Repère écran du téléphone tenu en portrait, puis rotation de l'affichage.
                val d = Vec2(-event.values[0].toDouble(), event.values[1].toDouble())
                val screen = when (rotation) {
                    Surface.ROTATION_90 -> Vec2(d.y, -d.x)
                    Surface.ROTATION_270 -> Vec2(-d.y, d.x)
                    Surface.ROTATION_180 -> -d
                    else -> d
                }
                val zero = neutral ?: screen.also { neutral = it }
                var v = (screen - zero) / (SensorManager.GRAVITY_EARTH * sensitivity.toDouble())
                if (v.length() < 0.12) v = Vec2.ZERO
                onSteer(if (v.length() > 1.0) v.normalized() else v)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose {
            manager.unregisterListener(listener)
            onSteer(Vec2.ZERO)
        }
    }
}

@NonSkippableComposable
@Composable
private fun TutorialBubble(tutorial: TutorialSession) {
    val text = tutorial.bubble
    if (text.isEmpty() || tutorial.state.phase == MatchPhase.Countdown || tutorial.state.phase == MatchPhase.Finished) return
    val target = tutorial.bubbleTarget
    val targetY = if (target >= 0) tutorial.state.spinners.getOrNull(target)?.position?.y ?: 0.0 else 0.0
    // En haut si la toupie désignée est en bas, et inversement.
    val alignment = if (target < 0) Alignment.Center else if (targetY > 0) Alignment.TopCenter else Alignment.BottomCenter
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = alignment) {
        Column(Modifier.widthIn(max = 360.dp).border(2.dp, Palette.accent, RoundedCornerShape(12.dp))
            .background(Palette.panel.copy(alpha = 0.95f), RoundedCornerShape(12.dp)).padding(12.dp)) {
            Text(text, color = Palette.text, fontSize = 14.sp)
            if (tutorial.isPaused && tutorial.state.phase == MatchPhase.Fighting) {
                Text("⏸ Jeu en pause", Modifier.align(Alignment.End), color = Palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@NonSkippableComposable
@Composable
private fun FinishedOverlay(
    session: MatchSession, tutorial: TutorialSession?, actions: @Composable () -> Unit,
    onLeave: () -> Unit, leaveLabel: String,
) {
    val state = session.state
    val local = session.localPlayer
    val winner = state.winner
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (winner == local) "Victoire !" else if (winner < 0) "Match nul" else "Défaite…",
                color = if (winner == local) Palette.accent else if (winner < 0) Palette.text else Palette.danger,
                fontSize = 40.sp, fontWeight = FontWeight.Bold)
            if (winner >= 0 && winner != local) Text("${state.names[winner]} remporte le combat", color = slotColor(winner))
            Text(endReasonText(state.endReason, state.playerCount), color = Palette.text)
            state.series?.let { before ->
                val after = before.deepCopy().apply { recordRound(winner, state.endReason) }
                Text("Score du match : ${after.score[0]} – ${after.score[1]}", color = Palette.accent, fontWeight = FontWeight.Bold)
                Text(when {
                    !after.isOver -> "Manche suivante dans quelques secondes…"
                    after.winner == local -> "Match remporté !"
                    else -> "Match perdu"
                }, color = Palette.text)
            }
            if (state.playerCount > 2) state.ranking.forEachIndexed { rank, i ->
                Text("${rank + 1}. ${state.names[i]} (${state.spinners[i].top.name})", color = Palette.text)
            }
            tutorial?.let { Text(it.bubble, Modifier.widthIn(max = 420.dp), color = Palette.accent, textAlign = TextAlign.Center) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(leaveLabel, onLeave)
                actions()
            }
        }
    }
}

private fun endReasonText(reason: Battle.EndReason, players: Int) = when (reason) {
    Battle.EndReason.RingOut -> if (players > 2) "Dernière élimination : éjection hors du stadium" else "Éjection hors du stadium"
    Battle.EndReason.SpinOut -> if (players > 2) "Dernière élimination : plus d'endurance" else "Plus d'endurance"
    Battle.EndReason.Timeout -> "Temps écoulé : victoire à l'endurance restante"
    Battle.EndReason.Forfeit -> "Abandon (joueur déconnecté)"
    Battle.EndReason.None -> ""
}

