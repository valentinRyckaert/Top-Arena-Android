package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.BotController
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.Chip
import fr.toupiearena.mobile.ui.holo.DrawerSide
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.Slashes
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.skewPath
import fr.toupiearena.mobile.ui.holo.tr
import kotlin.math.min
import kotlin.random.Random

@Composable
fun MenuScreen(app: AppController) {
    var play by remember { mutableStateOf(false) }
    val version = LocalContext.current.let { remember { it.packageManager.getPackageInfo(it.packageName, 0).versionName.orEmpty() } }
    HoloScreen {
        MenuBattle(app, Modifier.fillMaxHeight().fillMaxWidth(0.62f).align(Alignment.CenterEnd))
        Column(Modifier.width(340.dp + EdgeMargin).fillMaxHeight().padding(start = EdgeMargin, top = 22.dp, bottom = 18.dp)) {
            Logo()
            Text(tr("Assemblez votre toupie · affrontez les bots ou vos amis en réseau local"), style = bodyStyle(12.5.sp, Holo.muted),
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
            HoloButton(tr("Jouer"), { play = true }, Modifier.fillMaxWidth(), BtnVariant.Primary, HoloIcon.Play, height = 52.dp, alignStart = true)
            Spacer(Modifier.height(10.dp))
            HoloButton(tr("Multijoueur en réseau local"), { app.screen = Screen.LanMenu(null) }, Modifier.fillMaxWidth(), icon = HoloIcon.Lan, alignStart = true)
            Spacer(Modifier.height(8.dp))
            HoloButton(tr("Atelier"), { app.screen = Screen.Builder }, Modifier.fillMaxWidth(), icon = HoloIcon.Build, alignStart = true)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoloButton(tr("Paramètres"), { app.screen = Screen.Settings }, Modifier.weight(1f), BtnVariant.Ghost, HoloIcon.Gear, height = 42.dp, textSize = 12f)
                HoloButton(tr("Aide"), { app.screen = Screen.Help }, Modifier.weight(1f), BtnVariant.Ghost, HoloIcon.Help, height = 42.dp, textSize = 12f)
            }
        }
        Text("v$version", style = displayStyle(11.sp, Holo.dim, 0.05f), modifier = Modifier.align(Alignment.BottomEnd).padding(end = EdgeMargin, bottom = 8.dp))
        PlayDrawer(app, play) { play = false }
    }
    if (!app.repositoryLoaded) LaunchedEffect(Unit) { app.message = tr("Le fichier des toupies est illisible et a été ignoré.") }
}

/** Logo « TOP ARENA » : texte décalé en magenta et cyan, bandeau vert « ARENA ». */
@Composable
fun Logo(modifier: Modifier = Modifier) {
    Column(modifier) {
        Box {
            Text("TOP", style = displayStyle(54.sp, Holo.magenta.copy(alpha = 0.75f), 0.03f, italic = true), modifier = Modifier.padding(start = 0.dp))
            Text("TOP", style = displayStyle(54.sp, Holo.cyan.copy(alpha = 0.75f), 0.03f, italic = true), modifier = Modifier.padding(start = 4.dp, top = 1.dp))
            Text("TOP", style = displayStyle(54.sp, Color.White, 0.03f, italic = true).copy(brush = Brush.linearGradient(listOf(Color.White, Color(0xFFBFD3FF), Color(0xFFC9A5FF), Color(0xFF9DFFCB)))),
                modifier = Modifier.padding(start = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(190.dp, 24.dp), contentAlignment = Alignment.CenterStart) {
                Canvas(Modifier.fillMaxSize()) { drawPath(skewPath(0f, 0f, size.width, size.height, 8.dp.toPx()), Brush.horizontalGradient(listOf(Holo.green, Holo.lime))) }
                Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ARENA", style = displayStyle(17.sp, Holo.ink, 0.5f, italic = true))
                    Spacer(Modifier.width(14.dp)); Slashes(Holo.ink, 12.dp)
                }
            }
            Column(Modifier.padding(start = 8.dp)) {
                Box(Modifier.width(80.dp).height(1.dp).padding(0.dp).drawWithCache { onDrawBehind { drawRect(Holo.cyan) } })
                Text(tr("COMBAT DE TOUPIES"), style = bodyStyle(8.sp, Holo.cyan, androidx.compose.ui.text.font.FontWeight.Bold).copy(letterSpacing = 2.sp))
            }
        }
    }
}

/** Menu intermédiaire « Jouer » : bots, tournois (reprise d'un tournoi sauvegardé), tutoriel. */
@Composable
private fun PlayDrawer(app: AppController, visible: Boolean, onClose: () -> Unit) {
    SideDrawer(visible, DrawerSide.End, tr("Jouer"), onClose, width = 470.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayChoice(HoloIcon.Bot, tr("Contre les bots"), tr("1 à 3 bots · duel ou mêlée")) { onClose(); app.screen = Screen.SoloSetup }
            PlayChoice(HoloIcon.Trophy, tr("Tournois"), tr("ligue ou élimination · solo ou réseau"),
                app.savedTournamentLabel()?.let { { Chip(tr("Reprendre · %1", it), Holo.warn, HoloIcon.Save) } }) { onClose(); app.screen = Screen.TournamentSetup }
            PlayChoice(HoloIcon.Tuto, tr("Tutoriel"), tr("un combat guidé, action par action")) { onClose(); app.startTutorial() }
        }
    }
}

@Composable
private fun PlayChoice(icon: HoloIcon, title: String, sub: String, extra: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    HoloCard(Modifier.fillMaxWidth().height(84.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawRect(Holo.field); drawRect(Holo.green, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)) }
                Icon(icon, Holo.green, 24.dp)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title.uppercase(), style = displayStyle(16.sp)); extra?.invoke()
                }
                Text(sub, style = bodyStyle(13.sp, Holo.muted))
            }
            Icon(HoloIcon.Next, Holo.cyan, 18.dp)
        }
    }
}

/**
 * Combat de bots derrière le menu, comme sur PC : deux toupies distinctes (prédéfinies ou
 * à vous, toutes les paires également probables) sur un stadium au hasard ; dès qu'il se
 * termine (1,5 s pour voir le gagnant) ou au bout d'une minute, un autre commence.
 */
@Composable
private fun MenuBattle(app: AppController, modifier: Modifier) {
    var battle by remember { mutableStateOf(newMenuBattle(app)) }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L; var pause = 0.0
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0.016 else min(0.05, (now - last) / 1e9); last = now
            val (b, bots) = battle
            if (b.isFinished) { pause -= dt; if (pause <= 0.0) battle = newMenuBattle(app) }
            else {
                bots.forEach { it.update(b, dt) }
                b.advance(dt)
                b.takeImpacts()
                if (b.isFinished) pause = 1.5 else if (b.elapsed > 60.0) battle = newMenuBattle(app)
            }
            frame = now
        }
    }
    val b = battle.first
    Box(modifier.drawWithCache {
        val r = min(size.width, size.height) / 2 * 0.9f
        onDrawBehind {
            drawCircle(Brush.radialGradient(listOf(Color(0x33FF6A3D), Color.Transparent), center, r * 1.3f), r * 1.3f, center)
            paintStadium(b.stadium, center, r)
        }
    }) {
        Canvas(Modifier.fillMaxSize()) {
            frame // Redessin à chaque image.
            val geo = b.geometry
            val k = min(size.width, size.height) / 2 * 0.9f / geo.radius.toFloat()
            fun toScreen(v: Vec2) = Offset(center.x + v.x.toFloat() * k, center.y + v.y.toFloat() * k)
            b.spinners.forEachIndexed { i, s: Spinner ->
                if (s.status == Spinner.Status.RingOut && b.isFinished) return@forEachIndexed
                var p = toScreen(s.position)
                if (b.duel.sideOf(i) >= 0) { val t = b.elapsed * 60.0 + i * 1.7; p += Offset(kotlin.math.sin(t * 1.3).toFloat(), kotlin.math.cos(t * 1.7).toFloat()) * (b.config.topRadius.toFloat() * k * 0.06f) }
                paintTop(s.top, p, b.config.topRadius.toFloat() * k, s.angle.toFloat(), if (s.isDashing || s.isBoosted) 1f else 0f, if (s.isActive) 1f else 0.5f)
            }
        }
    }
}

private fun newMenuBattle(app: AppController): Pair<Battle, List<BotController>> {
    val tops: List<Top> = app.repository.allTops
    val a = Random.nextInt(tops.size)
    val b = Random.nextInt(tops.size - 1).let { if (it >= a) it + 1 else it }
    val stadium = Stadium.all.random()
    val config = stadium.apply(BattleConfig()).copy(duelMode = MatchRules().duelMode) // Les duels choisis dans les paramètres.
    val battle = Battle(listOf(tops[a], tops[b]), stadium, config, Random.nextLong())
    val bots = listOf(BotController(0, Difficulty.Normal, Random.nextLong()), BotController(1, Difficulty.Normal, Random.nextLong()))
    battle.launch(bots.map { it.chooseLaunchAngle(battle) }, bots.map { it.chooseLaunchPower(battle) })
    return battle to bots
}
