package fr.toupiearena.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.DuelMode
import fr.toupiearena.mobile.game.DuelSide
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import fr.toupiearena.mobile.ui.art.BattleEffects
import fr.toupiearena.mobile.ui.art.ToScreen
import fr.toupiearena.mobile.ui.art.drawProjectiles
import fr.toupiearena.mobile.ui.art.drawStatus
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.Chip
import fr.toupiearena.mobile.ui.holo.DrawerSide
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.IconBtn
import fr.toupiearena.mobile.ui.holo.Joystick
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.RoundAction
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.Skew
import fr.toupiearena.mobile.ui.holo.TiltSteering
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.enduranceBar
import fr.toupiearena.mobile.ui.holo.labelStyle
import fr.toupiearena.mobile.ui.holo.skewPath
import fr.toupiearena.mobile.ui.holo.tr
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import fr.toupiearena.mobile.ui.art.drawStadium as artStadium

private const val MIN_ARROW = 0.14 // Longueur de flèche (monde) à la force minimale.
private const val MAX_ARROW = 0.6  // … et à la force maximale.

/** Ce que l'écran suit lui-même : bandeaux passagers et bilan du joueur local. */
private class BattleHud {
    var flash = 0.0
    var duelResult = ""
    var duelResultColor = Holo.text
    var duelResultTimer = 0.0
    var lastPhase = MatchPhase.Aiming
    var hits = 0
    var dashes = 0
    private var wasBoosted = false

    fun tick(session: MatchSession, impacts: List<ImpactEvent>, dt: Double, effects: BattleEffects) {
        val state = session.state
        val local = session.localPlayer
        if (state.phase != lastPhase) {
            if (state.phase == MatchPhase.Fighting) flash = 0.8
            if (state.phase == MatchPhase.Aiming) { effects.reset(); hits = 0; dashes = 0 } // Revanche, manche suivante.
            lastPhase = state.phase
        }
        for (impact in impacts) {
            if (impact.duelLoser == ImpactEvent.NO_DUEL) {
                if (impact.first >= 0 && (impact.first == local || impact.second == local)) hits++
                continue
            }
            val involved = impact.first == local || impact.second == local
            when {
                impact.duelLoser < 0 -> { duelResult = tr("Duel nul !"); duelResultColor = Holo.text }
                involved -> {
                    val lost = impact.duelLoser == local
                    duelResult = if (lost) tr("Duel perdu…") else tr("Duel gagné !")
                    duelResultColor = if (lost) Holo.danger else Holo.green
                }
                else -> {
                    duelResult = tr("%1 perd le duel", seatName(session, impact.duelLoser))
                    duelResultColor = seatColor(session, impact.duelLoser)
                }
            }
            duelResultTimer = 1.6
        }
        val boosted = state.spinners.getOrNull(local)?.isBoosted == true
        if (boosted && !wasBoosted) dashes++
        wasBoosted = boosted
        flash = max(0.0, flash - dt)
        duelResultTimer = max(0.0, duelResultTimer - dt)
    }
}

/** Repère de l'arène : conversion monde (rayons de stadium) -> pixels. */
private class ArenaFrame(width: Float, height: Float, radius: Double) {
    val scale = (min(width, height) / 2.0 * 0.92 / max(0.1, radius)).toFloat()
    val center = Offset(width / 2f, height / 2f)
    fun toScreen(p: Vec2) = Offset(center.x + (p.x * scale).toFloat(), center.y + (p.y * scale).toFloat())
    fun toWorld(o: Offset) = Vec2(((o.x - center.x) / scale).toDouble(), ((o.y - center.y) / scale).toDouble())
}

private val SEATS = listOf(Holo.danger, Holo.warn, Holo.magenta, Holo.cyan)

/** Vert pour soi, puis rouge, jaune, magenta pour les autres. */
private fun seatColor(session: MatchSession, i: Int): Color {
    val local = session.localPlayer
    return if (i == local) Holo.green else SEATS[(if (i > local) i - 1 else i).coerceIn(0, SEATS.lastIndex)]
}

private fun seatName(session: MatchSession, i: Int): String {
    if (i == session.localPlayer) return tr("Vous")
    val n = session.state.names.getOrElse(i) { "?" }
    return if (n.startsWith("Bot ")) tr("Bot %1", tr(n.removePrefix("Bot "))) else tr(n)
}

/**
 * Écran de combat (paysage) : arène au centre, fiches des joueurs de part et d'autre, commandes
 * sous les pouces. Vise en glissant depuis sa toupie (direction + force), relâcher lance.
 * Le bouton pause ouvre le menu du combat ; en solo il met le jeu en pause.
 */
@Composable
fun BattleScreen(
    session: MatchSession,
    scheme: ControlScheme,
    onScheme: (ControlScheme) -> Unit,
    tiltSensitivity: Float,
    pausable: Boolean,
    leaveLabel: String,
    onLeave: () -> Unit,
    onChangeTops: (() -> Unit)? = null,
    finishedActions: @Composable RowScope.() -> Unit = {},
) {
    val tutorial = session as? TutorialSession
    val effects = remember(session) { BattleEffects() }
    val hud = remember(session) { BattleHud() }
    var frame by remember { mutableLongStateOf(0L) }
    var hudTick by remember { mutableLongStateOf(0L) }
    var controls by remember { mutableStateOf(scheme) }
    var menu by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    var rules by remember { mutableStateOf(false) }
    var calibration by remember { mutableIntStateOf(0) }
    val tilt = remember { floatArrayOf(0f, 0f) } // Dernière inclinaison, pour l'indicateur.
    var arenaSize by remember { mutableStateOf(IntSize.Zero) }
    var aim by remember(session) {
        mutableStateOf(Pair((-session.state.startPositions.getOrElse(session.localPlayer) { Vec2() }).angle(), 1.0))
    }
    var aiming by remember(session) { mutableStateOf(false) }
    val density = LocalDensity.current

    BackHandler(enabled = !menu) { if (session.state.phase == MatchPhase.Finished) onLeave() else menu = true }

    // Boucle de jeu : une mise à jour par image (sauf en pause, hors réseau).
    LaunchedEffect(session) {
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016 else min(0.05, (now - last) / 1e9)
                last = now
                if (!(pausable && (menu || help || rules))) {
                    session.update(dt)
                    val impacts = session.takeImpacts()
                    hud.tick(session, impacts, dt, effects)
                    effects.update(dt, impacts, session.state.spinners, session.state.duel, session.state.phase == MatchPhase.Fighting)
                }
                frame = now
                if (now - hudTick >= 50_000_000L) hudTick = now
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

    // Interface recomposée 20 fois par seconde ; l'arène et les jauges de timing se redessinent à chaque image.
    hudTick
    val state = session.state
    val paused = pausable && (menu || help || rules)
    val fighting = state.phase == MatchPhase.Fighting
    val localInDuel = state.duel.sideOf(session.localPlayer) >= 0
    val tutorialPaused = tutorial?.isPaused == true && state.phase != MatchPhase.Aiming

    if (controls == ControlScheme.Tilt && fighting && !paused) key(calibration) {
        TiltSteering(tiltSensitivity) { tilt[0] = it.x.toFloat(); tilt[1] = it.y.toFloat(); steer(it) }
    }

    HoloScreen(
        // Toucher l'écran : appui en duel, reprise du tutoriel, gestes.
        Modifier.pointerInput(session, controls) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (session.state.phase != MatchPhase.Fighting || menu || help || rules) return@awaitEachGesture
                if (session.state.duel.sideOf(session.localPlayer) >= 0 || (tutorial?.isPaused == true && controls != ControlScheme.Gestures)) {
                    dash()
                    return@awaitEachGesture
                }
                if (controls != ControlScheme.Gestures) return@awaitEachGesture
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
        // --- Arène ------------------------------------------------------------------------
        Box(Modifier.align(Alignment.Center).fillMaxHeight().padding(vertical = 4.dp).aspectRatio(1f).onSizeChanged { arenaSize = it }) {
            key(state.stadium.id, state.geometry.radius) {
                // Couche à part : le stadium, coûteux, n'est dessiné qu'une fois.
                Spacer(Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                    val f = ArenaFrame(size.width, size.height, state.geometry.radius)
                    onDrawBehind { artStadium(state.stadium, state.geometry, f.center, f.scale) }
                })
            }
            val textMeasurer = rememberTextMeasurer()
            Canvas(
                Modifier.fillMaxSize().graphicsLayer().pointerInput(session) { // Couche à part : seul l'arène est redessinée.
                    // Visée : glisser depuis sa toupie, relâcher pour lancer.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (session.state.phase != MatchPhase.Aiming || session.localHasAimed) return@awaitEachGesture
                        if (!allowed(TouchAction.Aim)) return@awaitEachGesture
                        aiming = true
                        var dragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            val f = ArenaFrame(arenaSize.width.toFloat(), arenaSize.height.toFloat(), session.state.geometry.radius)
                            val v = f.toWorld(change.position) - session.state.startPositions[session.localPlayer]
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
                drawArena(session, effects, ArenaFrame(size.width, size.height, state.geometry.radius), aim, textMeasurer)
            }
            tutorial?.let { TutorialBubble(it) }
        }

        ArenaBanners(session, hud, aiming)
        if (state.duel.active && fighting) DuelHud(session) { frame }

        // --- Fiches : sièges pairs à gauche, impairs à droite ------------------------------
        Column(Modifier.align(Alignment.TopStart).padding(start = EdgeMargin, top = 10.dp).width(184.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBtn(HoloIcon.Pause, { menu = true })
            state.spinners.indices.filter { it % 2 == 0 }.forEach { HudCard(session, it) }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(end = EdgeMargin, top = 8.dp).width(184.dp), horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Clock(session)
            state.spinners.indices.filter { it % 2 == 1 }.forEach { HudCard(session, it) }
        }
        state.series?.takeIf { state.phase != MatchPhase.Finished }?.let { series ->
            Column(Modifier.align(Alignment.TopCenter).background(Holo.panel.copy(alpha = 0.9f), Skew(8.dp)).border(1.dp, Holo.line, Skew(8.dp))
                .padding(horizontal = 22.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Manche %1", series.rounds + 1).uppercase(), style = labelStyle(Holo.muted, 8.5.sp))
                Text("${series.score[0]} – ${series.score[1]}", style = displayStyle(13.sp, Holo.green))
            }
        }

        // --- Commandes ----------------------------------------------------------------------
        if (fighting && !paused) {
            val me = state.spinners[session.localPlayer]
            // Tutoriel : le bouton présenté est mis en avant, l'autre assombri.
            val focus = if (tutorial?.isPaused != true) null else when (tutorial.step) {
                TutorialSession.Step.Dash, TutorialSession.Step.Duel -> TouchAction.Dash
                TutorialSession.Step.Special -> TouchAction.Special
                else -> null
            }
            fun Modifier.focus(a: TouchAction) = when (focus) {
                null -> this
                a -> drawBehind {
                    drawCircle(Holo.cyan, size.minDimension / 2 + 5.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                }
                else -> alpha(0.35f)
            }
            if (!localInDuel) when (controls) {
                ControlScheme.Buttons -> Joystick({ steer(it) }, Modifier.align(Alignment.BottomStart).padding(start = EdgeMargin, bottom = 14.dp), 112.dp)
                ControlScheme.Tilt -> Row(Modifier.align(Alignment.BottomStart).padding(start = EdgeMargin, bottom = 14.dp),
                    verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TiltIndicator(tilt[0], tilt[1])
                    HoloButton(tr("Recalibrer"), { calibration++ }, Modifier.width(120.dp), BtnVariant.Ghost, height = 36.dp, textSize = 11f)
                }
                ControlScheme.Gestures -> {
                    Column(Modifier.align(Alignment.BottomStart).padding(start = EdgeMargin, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(tr("Glisser · piloter"), Holo.cyan, HoloIcon.Hand)
                        Chip(tr("Toucher · dash"), Holo.green, HoloIcon.Dash)
                    }
                    Chip(tr("Appui long · spécial"), Holo.magenta, HoloIcon.Bolt, Modifier.align(Alignment.BottomEnd).padding(end = EdgeMargin, bottom = 16.dp))
                }
            }
            if (localInDuel) {
                // Tout l'écran sert de bouton : celui-ci n'est qu'un repère (l'appui passe par l'écran).
                Column(Modifier.align(Alignment.BottomEnd).padding(end = EdgeMargin, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RoundAction(tr("Appui"), 92.dp, true, {}, color = Holo.cyan, icon = HoloIcon.Hand)
                    Text(tr("ou n'importe où"), style = bodyStyle(10.sp, Holo.dim))
                }
            } else if (controls != ControlScheme.Gestures) {
                Row(Modifier.align(Alignment.BottomEnd).padding(end = EdgeMargin, bottom = 12.dp), verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.config.specialsEnabled) {
                        val canSpecial = !me.specialUsed && me.isActive && !me.isDisarmed
                        RoundAction(tr("Spécial"), 68.dp, canSpecial, { special() }, Modifier.focus(TouchAction.Special), Holo.magenta,
                            art = { c, r, col -> paintEmblem(me.top.special, c, r, col) })
                    }
                    val cooldown = if (me.dashCooldown > 0.0) (1.0 - me.dashCooldown / state.config.dashCooldown).toFloat().coerceIn(0f, 1f) else null
                    RoundAction(tr("Dash"), 92.dp, true, { dash() }, Modifier.focus(TouchAction.Dash), icon = HoloIcon.Dash, cooldown = cooldown)
                }
            }
        }

        if (state.phase == MatchPhase.Finished) FinishedOverlay(session, hud, tutorial, finishedActions)

        // --- Menu du combat -----------------------------------------------------------------
        SideDrawer(menu, DrawerSide.Start, tr("Menu du combat"), { menu = false }, width = 400.dp,
            sub = if (pausable) tr("en pause") else tr("en réseau"),
            footer = { HoloButton(leaveLabel, { menu = false; onLeave() }, Modifier.weight(1f), BtnVariant.Danger, HoloIcon.Quit, height = 44.dp) }) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HoloButton(tr("Reprendre"), { menu = false }, Modifier.fillMaxWidth(), BtnVariant.Primary, HoloIcon.Play, height = 48.dp)
                if (onChangeTops != null) HoloButton(tr("Changer de toupies"), onChangeTops, Modifier.fillMaxWidth(), icon = HoloIcon.Build, height = 42.dp)
                Segmented(listOf(tr("Joystick"), tr("Inclinaison"), tr("Gestes")), controls.ordinal, {
                    controls = ControlScheme.entries[it]; onScheme(controls); session.setSteering(Vec2.ZERO)
                }, label = tr("Commandes"), height = 34.dp, textSize = 11f)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HoloButton(tr("Aide"), { help = true }, Modifier.weight(1f), BtnVariant.Ghost, HoloIcon.Help, height = 40.dp, textSize = 12f)
                    HoloButton(tr("Règles"), { rules = true }, Modifier.weight(1f), BtnVariant.Ghost, HoloIcon.Sliders, height = 40.dp, textSize = 12f)
                }
                Hint(if (pausable) tr("Le combat est en pause tant que ce menu est ouvert.")
                    else tr("En réseau, le combat continue pendant que ce menu est ouvert."), color = Holo.dim)
            }
        }
        SideDrawer(help, DrawerSide.End, tr("Aide"), { help = false }, width = 440.dp, sub = tr(controls.label)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                controlRows(controls).forEach { (what, how) ->
                    Row {
                        Text(what.uppercase(), style = labelStyle(Holo.cyan), modifier = Modifier.width(120.dp).padding(top = 2.dp))
                        Text(how, style = bodyStyle(12.5.sp, Holo.text), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        RulesDrawer(rules, state.rules, {}, { rules = false }, readOnly = true)
    }
}

// ---------------------------------------------------------------------------
// Dessin de l'arène
// ---------------------------------------------------------------------------

private fun DrawScope.drawArena(session: MatchSession, effects: BattleEffects, f: ArenaFrame, aim: Pair<Double, Double>, textMeasurer: TextMeasurer) {
    val state = session.state
    val radius = (state.config.topRadius * f.scale).toFloat()
    val toScreen: ToScreen = f::toScreen

    // Points de départ.
    if (state.phase == MatchPhase.Aiming || state.phase == MatchPhase.Countdown) {
        state.startPositions.forEachIndexed { i, p ->
            val aimed = state.aimed.getOrElse(i) { false }
            drawCircle(seatColor(session, i).copy(alpha = if (aimed) 0.8f else 0.45f), radius * 1.5f, f.toScreen(p),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        }
    }
    // Flèche de visée : plus longue = plus puissante.
    if (state.phase == MatchPhase.Aiming && !session.localHasAimed) {
        val cfg = state.config
        val t = ((aim.second - cfg.launchPowerMin) / max(1e-6, cfg.launchPowerMax - cfg.launchPowerMin)).toFloat()
        val start = state.startPositions[session.localPlayer]
        val dir = Vec2.fromAngle(aim.first)
        val from = f.toScreen(start + dir * (cfg.topRadius * 1.3))
        val to = f.toScreen(start + dir * (MIN_ARROW + t * (MAX_ARROW - MIN_ARROW)))
        drawLine(Holo.green.copy(alpha = 0.18f), from, to, (8f + 6f * t) * density, StrokeCap.Round)
        drawLine(Holo.green, from, to, 2.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        val d = (to - from) / max(1f, (to - from).getDistance())
        val n = Offset(-d.y, d.x)
        val head = (9f + 4f * t) * density
        drawPath(Path().apply {
            moveTo((to + d * head).x, (to + d * head).y)
            lineTo((to + n * (head * 0.6f)).x, (to + n * (head * 0.6f)).y)
            lineTo((to - n * (head * 0.6f)).x, (to - n * (head * 0.6f)).y)
            close()
        }, Holo.green)
        val label = textMeasurer.measure(tr("Force %1 %", (aim.second * 100).roundToInt()).uppercase(), displayStyle(9.sp, Holo.green, 0.08f))
        drawText(label, topLeft = to + d * (head * 1.6f) - Offset(label.size.width / 2f, label.size.height / 2f) + n * (head * 1.2f))
    }
    with(effects) { drawTrails(state.spinners, toScreen, radius) }
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
        val out = s.status == Spinner.Status.RingOut
        if (out) {
            pos = f.toScreen(s.position + state.geometry.outwardNormal(s.position) * (state.config.topRadius * 1.6))
            r *= 0.75f
            alpha = 0.35f
        }
        val glow = if (s.isBoosted) (s.boostTimer / state.config.dashBoostDuration).toFloat() else 0f
        paintTop(s.top, pos, r, s.angle.toFloat(), glow, alpha)
        drawStatus(s, pos, r, state.elapsed)
        if (!out) nameTag(seatName(session, i).uppercase(), seatColor(session, i), Offset(pos.x, pos.y - r * 1.35f), textMeasurer)
    }
    drawProjectiles(state.projectiles, toScreen, f.scale, state.config.topRadius.toFloat(), state.geometry.radius.toFloat(), state.elapsed)
    with(effects) { drawSparks(toScreen, radius, f.scale) }
}

/** Étiquette inclinée au-dessus d'une toupie (bas centré sur [anchor]). */
private fun DrawScope.nameTag(text: String, color: Color, anchor: Offset, textMeasurer: TextMeasurer) {
    val layout = textMeasurer.measure(text, displayStyle(8.sp, Holo.ink, 0.08f))
    val w = layout.size.width + 12.dp.toPx()
    val h = layout.size.height + 2.dp.toPx()
    val x = anchor.x - w / 2
    val y = anchor.y - h
    drawPath(skewPath(x, y, w, h, 4.dp.toPx()), color)
    drawText(layout, topLeft = Offset(x + 6.dp.toPx(), y + 1.dp.toPx()))
}

// ---------------------------------------------------------------------------
// Bandeaux, fiches, duels
// ---------------------------------------------------------------------------

// L'état de la session n'est pas observable : ces éléments se recomposent avec l'écran, à chaque image.
@NonSkippableComposable
@Composable
private fun BoxScope.ArenaBanners(session: MatchSession, hud: BattleHud, aiming: Boolean) {
    val state = session.state
    when (state.phase) {
        MatchPhase.Aiming -> if (session !is TutorialSession) { // Le tutoriel l'explique dans sa bulle.
            val hint = if (session.localHasAimed) tr("En attente des autres joueurs (%1/%2)", state.aimed.count { it }, state.playerCount)
            else if (aiming) tr("Relâchez pour lancer") else tr("Glissez depuis votre toupie, relâchez pour lancer")
            Banner(hint, Modifier.align(Alignment.TopCenter).padding(top = 40.dp), 12.sp, display = false)
        }
        MatchPhase.Countdown -> BigText(max(1, ceil(state.countdown).toInt()).toString(), 80.sp, Holo.text)
        MatchPhase.Fighting -> {
            if (hud.flash > 0) BigText(tr("Partez !").uppercase(), 48.sp, Holo.warn)
            if (hud.duelResultTimer > 0) Banner(hud.duelResult, Modifier.align(Alignment.TopCenter).padding(top = 52.dp), 26.sp, hud.duelResultColor)
        }
        MatchPhase.Finished -> Unit
    }
}

/** Grand texte au centre de l'arène (compte à rebours, départ). */
@Composable
private fun BoxScope.BigText(text: String, size: TextUnit, color: Color) =
    Text(text, Modifier.align(Alignment.Center), style = displayStyle(size, color, 0.04f, italic = true).copy(shadow = Shadow(Holo.cyan, blurRadius = 28f)))

@Composable
private fun Banner(text: String, modifier: Modifier, size: TextUnit, color: Color = Holo.text, display: Boolean = true) {
    Text(text, modifier.background(Holo.void.copy(alpha = 0.72f), Skew(10.dp)).border(1.dp, Holo.line, Skew(10.dp)).padding(horizontal = 20.dp, vertical = 4.dp),
        style = if (display) displayStyle(size, color, 0.06f, italic = true) else bodyStyle(size, color), textAlign = TextAlign.Center)
}

@NonSkippableComposable
@Composable
private fun Clock(session: MatchSession) {
    val state = session.state
    val left = state.config.maxDuration - state.elapsed
    val s = if (state.config.maxDuration > 0.0) ceil(max(0.0, left)).toInt() else state.elapsed.toInt()
    Text("%d:%02d".format(s / 60, s % 60), style = displayStyle(22.sp, if (state.config.maxDuration > 0.0 && left < 10.0) Holo.warn else Holo.text, 0.02f))
    Text("${state.stadium.displayName()} · ${tr(state.stadium.codename)}".uppercase(), style = labelStyle(stadiumColor(state.stadium), 8.5.sp), maxLines = 1)
}

/** Fiche d'un joueur : toupie, endurance, état. */
@NonSkippableComposable
@Composable
private fun HudCard(session: MatchSession, index: Int) {
    val state = session.state
    val s = state.spinners[index]
    val local = index == session.localPlayer
    val color = seatColor(session, index)
    val out = s.status != Spinner.Status.Waiting && !s.isActive
    HoloCard(Modifier.fillMaxWidth().alpha(if (out) 0.55f else 1f), selected = local, accent = color, fill = Holo.card.copy(alpha = 0.85f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopView(s.top, 24.dp, angle = s.angle.toFloat())
                Column(Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(seatName(session, index).uppercase(), style = displayStyle(10.sp, color, 0.08f), maxLines = 1)
                    Text(s.top.displayName(), style = bodyStyle(10.sp, Holo.muted), maxLines = 1)
                }
                Text("${s.spin.roundToInt()}", style = displayStyle(16.sp, Holo.text, 0f))
            }
            Canvas(Modifier.fillMaxWidth().height(7.dp).padding(horizontal = 2.dp)) { enduranceBar(0f, 0f, size.width, size.height, s.spinRatio.toFloat()) }
            val (text, c) = spinnerStatus(session, s, index)
            Text(text, style = bodyStyle(9.5.sp, c), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun spinnerStatus(session: MatchSession, s: Spinner, index: Int): Pair<String, Color> {
    val state = session.state
    val specialReady = state.config.specialsEnabled && !s.specialUsed && !s.isDisarmed
    return when {
        s.status == Spinner.Status.RingOut -> tr("Éjectée") to Holo.danger
        s.status == Spinner.Status.Stopped -> tr("À l'arrêt") to Holo.danger
        s.status == Spinner.Status.Forfeit -> tr("Déconnectée") to Holo.danger
        state.duel.sideOf(index) >= 0 -> tr("DUEL !") to Holo.warn
        s.isSlowed -> tr("Gelée (%1 s)", "%.1f".format(s.slowTimer)) to Color(0xFF8FF3FF)
        s.isDisarmed -> tr("Sonnée (%1 s)", "%.1f".format(s.disarmTimer)) to Holo.warn
        state.phase == MatchPhase.Aiming -> if (state.aimed.getOrElse(index) { false }) tr("Prêt à lancer") to Holo.green else tr("Vise…") to Holo.muted
        s.isBoosted -> tr("DASH !") to Holo.warn
        s.dashCooldown > 0.0 -> tr("Dash dans %1 s", "%.1f".format(s.dashCooldown)) to Holo.muted
        s.spin <= state.config.dashCost -> tr("Dash impossible") to Holo.danger
        index == session.localPlayer && specialReady -> tr("Dash prêt · spécial prêt") to Holo.green
        index != session.localPlayer && specialReady -> tr("%1 prêt", specialName(s.top.special)) to Holo.muted
        else -> tr("Dash prêt") to Holo.green
    }
}

/** Duel : bandeau, puis jauges (timing) ou corde (rafale) sous l'arène. */
@NonSkippableComposable
@Composable
private fun BoxScope.DuelHud(session: MatchSession, frame: () -> Long) {
    val state = session.state
    val duel = state.duel
    val cfg = state.config
    val rush = cfg.duelMode == DuelMode.Rush
    val mineSide = duel.sideOf(session.localPlayer)
    val me = max(0, mineSide)
    val other = 1 - me
    val seat = { side: Int -> if (side == 0) duel.first else duel.second }
    if ((session as? TutorialSession)?.isPaused != true) Text(if (rush) tr("Duel · rafale").uppercase() else tr("Duel !").uppercase(),
        Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Holo.violet.copy(alpha = 0.85f), Holo.magenta.copy(alpha = 0.6f), Color.Transparent)), Skew(14.dp))
            .padding(horizontal = 56.dp, vertical = 2.dp),
        style = displayStyle(26.sp, Holo.text, 0.1f, italic = true))
    Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp).width(540.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (rush) {
            val lead = duel.rope() * (if (me == 0) 1 else -1)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("${seatName(session, seat(me)).uppercase()} ${duel.sides[me].presses}", style = displayStyle(12.sp, Holo.green, 0.04f))
                Spacer(Modifier.weight(1f))
                Text("%.1f s".format(max(0.0, cfg.rushDuration - duel.elapsed)), style = displayStyle(12.sp, Holo.warn, 0.04f))
                Spacer(Modifier.weight(1f))
                Text("${duel.sides[other].presses} ${seatName(session, seat(other)).uppercase()}", style = displayStyle(12.sp, Holo.danger, 0.04f))
            }
            Canvas(Modifier.fillMaxWidth().height(28.dp).padding(top = 6.dp)) {
                ropeBar((0.5f + lead / (2f * cfg.rushLimit)).coerceIn(0f, 1f))
            }
            Text(tr("Touchez le plus vite possible : %1 appuis d'avance gagnent aussitôt", cfg.rushLimit), style = bodyStyle(12.sp), modifier = Modifier.padding(top = 4.dp))
            Hint(tr("Au bout de %1 s, le plus avancé gagne · pile au milieu : nul", "%.1f".format(cfg.rushDuration)), color = Holo.muted)
        } else {
            Row(Modifier.fillMaxWidth()) {
                FieldLabel(seatName(session, seat(other)), color = seatColor(session, seat(other)))
                Spacer(Modifier.weight(1f))
                FieldLabel(tr("série %1", duel.sides[other].streak))
            }
            Canvas(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 24.dp).graphicsLayer()) { frame(); timingBar(session.state.duel.sides[other]) }
            Canvas(Modifier.fillMaxWidth().height(36.dp).padding(top = 8.dp).graphicsLayer()) { frame(); timingBar(session.state.duel.sides[me]) }
            Text(if (mineSide >= 0 && duel.sides[me].done) tr("Série réussie ! En attente de l'adversaire…") else tr("Touchez l'écran quand le curseur est dans le vert"),
                style = bodyStyle(12.sp), modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(tr("Série à taille minimale"))
                Canvas(Modifier.padding(start = 6.dp).size((cfg.duelStreak * 14).dp, 10.dp)) {
                    repeat(cfg.duelStreak) { k ->
                        val c = Offset(5.dp.toPx() + k * 14.dp.toPx(), size.height / 2)
                        val d = Path().apply { moveTo(c.x, c.y - 5.dp.toPx()); lineTo(c.x + 5.dp.toPx(), c.y); lineTo(c.x, c.y + 5.dp.toPx()); lineTo(c.x - 5.dp.toPx(), c.y); close() }
                        if (k < duel.sides[me].streak) drawPath(d, Holo.green) else drawPath(d, Holo.green, style = Stroke(1.5.dp.toPx()))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(tr("Le premier qui rate subit 1,5× les dégâts du dash"), style = bodyStyle(10.5.sp, Holo.muted))
            }
        }
    }
}

/** Jauge de duel « timing » : zone verte au centre, curseur blanc. */
private fun DrawScope.timingBar(side: DuelSide) {
    val w = size.width
    val h = size.height
    val s = h * 0.6f
    drawPath(skewPath(0f, 0f, w, h, s), Brush.horizontalGradient(listOf(Color(0xFF6A1238), Color(0xFFB0244F), Color(0xFF6A1238))))
    val half = w / 2
    val zone = if (side.done) half else (side.zone * half).toFloat()
    if (zone > 0f) drawPath(skewPath(half - zone, 0f, 2 * zone, h, s), Brush.horizontalGradient(listOf(Holo.green, Holo.lime), half - zone, half + zone))
    val x = half + (side.cursor.coerceIn(-1.0, 1.0) * half).toFloat()
    drawLine(Color.White.copy(alpha = 0.35f), Offset(x, -4f), Offset(x, h + 4f), max(6f, h * 0.35f), StrokeCap.Round)
    drawLine(Color.White, Offset(x, -4f), Offset(x, h + 4f), max(2f, h * 0.12f), StrokeCap.Round)
}

/** Corde de la rafale : [pos] (0..1) est l'avance du joueur, vers l'adversaire à droite. */
private fun DrawScope.ropeBar(pos: Float) {
    val w = size.width
    val h = size.height
    val s = h * 0.6f
    val x = w * pos
    drawPath(skewPath(0f, 0f, w, h, s), Brush.horizontalGradient(listOf(Color(0xFF8A1A48), Holo.danger)))
    if (x > 0f) drawPath(skewPath(0f, 0f, x, h, s), Brush.horizontalGradient(listOf(Holo.green, Color(0xFF1FA868)), 0f, max(1f, x)))
    drawLine(Color.White.copy(alpha = 0.5f), Offset(w / 2, -4f), Offset(w / 2, h + 4f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    drawLine(Color.White, Offset(x, -2f), Offset(x, h + 2f), 3.dp.toPx())
    val t = 6.dp.toPx()
    drawPath(Path().apply { moveTo(x - t, -t - 2f); lineTo(x + t, -t - 2f); lineTo(x, -2f); close() }, Color.White)
}

/** Repère de l'inclinaison : la position du point suit le pilotage. */
@Composable
private fun TiltIndicator(x: Float, y: Float) = Canvas(Modifier.size(96.dp)) {
    val r = size.minDimension / 2 - 2.dp.toPx()
    drawCircle(Holo.field.copy(alpha = 0.75f), r, center)
    drawCircle(Holo.lineHi, r, center, style = Stroke(1.5.dp.toPx()))
    drawLine(Holo.line, Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.dp.toPx())
    drawLine(Holo.line, Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.dp.toPx())
    drawCircle(Holo.green, r * 0.16f, center + Offset(x, y) * (r * 0.8f))
}

// ---------------------------------------------------------------------------
// Tutoriel et résultat
// ---------------------------------------------------------------------------

private fun tutorialStep(step: TutorialSession.Step): Pair<String, Int> = when (step) {
    TutorialSession.Step.Aim -> tr("Visée") to 1
    TutorialSession.Step.Steer -> tr("Pilotage") to 2
    TutorialSession.Step.Dash -> tr("Dash") to 3
    TutorialSession.Step.Special -> tr("Coup spécial") to 4
    TutorialSession.Step.Duel, TutorialSession.Step.AfterDuel -> tr("Duel") to 5
    TutorialSession.Step.DuelIntro, TutorialSession.Step.DuelPlay, TutorialSession.Step.DuelMiss -> tr("Duel") to 6
    TutorialSession.Step.Ejection, TutorialSession.Step.Done -> tr("Éjection") to 7
}

@NonSkippableComposable
@Composable
private fun BoxScope.TutorialBubble(tutorial: TutorialSession) {
    val text = tutorial.bubble
    if (text.isEmpty() || tutorial.state.phase == MatchPhase.Countdown || tutorial.state.phase == MatchPhase.Finished) return
    val target = tutorial.bubbleTarget
    val targetY = if (target >= 0) tutorial.state.spinners.getOrNull(target)?.position?.y ?: 0.0 else 0.0
    // En haut si la toupie désignée est en bas, et inversement.
    val alignment = if (target < 0) Alignment.Center else if (targetY > 0) Alignment.TopCenter else Alignment.BottomCenter
    val (title, n) = tutorialStep(tutorial.step)
    Panel(Modifier.align(alignment).padding(10.dp).widthIn(max = 360.dp), accent = Holo.cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), style = displayStyle(12.sp, Holo.cyan), modifier = Modifier.weight(1f))
            Text("$n/7", style = labelStyle(Holo.muted))
        }
        Text(text, style = bodyStyle(13.sp), modifier = Modifier.padding(top = 4.dp))
        if (tutorial.isPaused && tutorial.state.phase == MatchPhase.Fighting) {
            Text(tr("Jeu en pause").uppercase(), style = labelStyle(Holo.cyan, 9.sp),
                modifier = Modifier.padding(top = 6.dp).border(1.dp, Holo.cyan, Skew(5.dp)).padding(horizontal = 12.dp, vertical = 2.dp))
        }
    }
}

@NonSkippableComposable
@Composable
private fun FinishedOverlay(session: MatchSession, hud: BattleHud, tutorial: TutorialSession?, actions: @Composable RowScope.() -> Unit) {
    val state = session.state
    val local = session.localPlayer
    val winner = state.winner
    val melee = state.playerCount > 2
    val series = state.series?.let { it.deepCopy().apply { recordRound(winner, state.endReason) } }
    val rank = state.ranking.indexOf(local) + 1
    val (title, color) = when {
        series != null && series.isOver -> if (series.winner == local) tr("Match remporté !") to Holo.green else tr("Match perdu") to Holo.danger
        series != null -> if (winner == local) tr("Manche gagnée") to Holo.text else if (winner < 0) tr("Manche nulle") to Holo.text else tr("Manche perdue") to Holo.danger
        winner == local -> tr("Victoire !") to Holo.text
        winner < 0 -> tr("Match nul") to Holo.text
        melee && rank > 0 -> tr("%1e place", rank) to Holo.warn
        else -> tr("Défaite…") to Holo.danger
    }
    val won = winner == local
    val reason = endReasonText(state.endReason, melee)
    val tag = when {
        melee && winner >= 0 -> tr("Mêlée à %1 · %2 l'emporte", state.playerCount, seatName(session, winner))
        winner >= 0 && !won -> "$reason · ${seatName(session, winner)}"
        else -> reason
    }
    val shown = if (winner >= 0) winner else local
    val top = state.spinners[shown].top
    // Rotation par la couche graphique : ni recomposition ni redessin de la toupie à chaque image.
    val spin = rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "angle")

    Box(Modifier.fillMaxSize().background(Holo.void.copy(alpha = 0.88f))) {
        Row(Modifier.fillMaxSize().padding(horizontal = EdgeMargin, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(0.38f), horizontalAlignment = Alignment.CenterHorizontally) {
                val glow = seatColor(session, shown)
                Box(Modifier.size(200.dp).drawBehind {
                    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), center, size.minDimension / 2), size.minDimension / 2, center)
                    drawCircle(glow.copy(alpha = 0.6f), size.minDimension * 0.44f, center, style = Stroke(2.dp.toPx()))
                }, contentAlignment = Alignment.Center) { TopView(top, 160.dp, Modifier.graphicsLayer { rotationZ = spin.value }) }
                Text(top.displayName().uppercase(), style = displayStyle(14.sp), modifier = Modifier.padding(top = 6.dp))
                Text(listOf(top.wheel, top.middle, top.tip, top.bolt).joinToString(" · ") { tr(it.name) }, style = bodyStyle(10.5.sp, Holo.muted), maxLines = 1)
            }
            Column(Modifier.weight(0.62f).fillMaxHeight().padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(0.3f))
                Text(title.uppercase(), style = displayStyle(34.sp, color, 0.06f, italic = true))
                if (tag.isNotEmpty()) Text(tag.uppercase(), style = labelStyle(Holo.ink, 9.5.sp),
                    modifier = Modifier.background(Brush.horizontalGradient(if (won) listOf(Holo.green, Holo.lime) else listOf(Holo.danger, Holo.warn)), Skew(8.dp))
                        .padding(horizontal = 18.dp, vertical = 3.dp))
                if (melee && state.ranking.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.ranking.forEachIndexed { k, i ->
                            HoloCard(Modifier.fillMaxWidth(0.8f).height(28.dp), selected = i == local) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${k + 1}", style = displayStyle(12.sp, Holo.warn))
                                    TopView(state.spinners[i].top, 20.dp)
                                    Text(seatName(session, i).uppercase(), style = displayStyle(10.sp, seatColor(session, i)), modifier = Modifier.weight(1f))
                                    Text(state.spinners[i].top.displayName(), style = bodyStyle(10.sp, Holo.muted))
                                }
                            }
                        }
                    }
                } else {
                    val endurance = state.spinners[local].spin.roundToInt().toString()
                    val stats = if (series != null) listOf(tr("Score") to "${series.score[0]} – ${series.score[1]}", tr("Série") to tr("à %1", series.target),
                        tr("Chocs") to "${hud.hits}", tr("Endurance") to endurance)
                    else listOf(tr("Durée") to "%02d:%02d".format(state.elapsed.toInt() / 60, state.elapsed.toInt() % 60),
                        tr("Chocs") to "${hud.hits}", tr("Dashs") to "${hud.dashes}", tr("Endurance") to endurance)
                    Panel(Modifier.fillMaxWidth()) {
                        Row {
                            stats.forEachIndexed { k, (label, value) ->
                                Column(Modifier.weight(1f)) {
                                    FieldLabel(label)
                                    Text(value, style = displayStyle(18.sp, if (k == stats.lastIndex) Holo.green else Holo.text, 0.02f))
                                }
                            }
                        }
                    }
                }
                when {
                    tutorial != null -> Hint(tutorial.bubble)
                    series != null && !series.isOver -> Hint(tr("Manche suivante dans quelques secondes…"))
                }
                Spacer(Modifier.weight(0.7f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

private fun endReasonText(reason: Battle.EndReason, melee: Boolean) = when (reason) {
    Battle.EndReason.RingOut -> if (melee) tr("Dernière élimination : éjection") else tr("Éjection hors du stadium")
    Battle.EndReason.SpinOut -> if (melee) tr("Dernière élimination : plus d'endurance") else tr("Plus d'endurance")
    Battle.EndReason.Timeout -> tr("Temps écoulé : victoire à l'endurance")
    Battle.EndReason.Forfeit -> tr("Abandon (joueur déconnecté)")
    Battle.EndReason.None -> ""
}

