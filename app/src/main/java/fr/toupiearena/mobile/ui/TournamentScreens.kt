package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchFormat
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.game.TournamentFormat
import fr.toupiearena.mobile.game.TournamentMatch
import fr.toupiearena.mobile.game.TournamentSettings
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.DrawerSide
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.IconBtn
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.labelStyle
import fr.toupiearena.mobile.ui.holo.tr

private val KNOCKOUT_SIZES = listOf(4, 8, 16, 32)

private fun formatLabel(f: TournamentFormat) = if (f == TournamentFormat.League) tr("Ligue") else tr("Élimination")
private fun matchLabel(f: MatchFormat) = if (f == MatchFormat.Express) tr("Express") else tr("Classique")
private fun matchRule(f: MatchFormat) = if (f == MatchFormat.Express) tr("Premier à 2 manches") else tr("Premier à 4 points · 3 toupies")

/** Nombre de tours d'un tournoi de [n] participants. */
private fun roundsFor(format: TournamentFormat, n: Int) =
    if (format == TournamentFormat.League) (if (n % 2 == 0) n - 1 else n) else KNOCKOUT_SIZES.indexOfFirst { it >= n } + 2

/**
 * Réglages d'un tournoi : format, participants, type de match et niveau des bots
 * (solo, création d'un tournoi en réseau, réglages de l'hôte).
 */
@Composable
fun TournamentSettingsForm(settings: TournamentSettings, humans: Int, onChange: (TournamentSettings) -> Unit, readOnly: Boolean = false) {
    fun update(s: TournamentSettings) { if (!readOnly) onChange(s.fitBots(humans)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Segmented(TournamentFormat.entries.map(::formatLabel), settings.format.ordinal, {
            val format = TournamentFormat.entries[it]
            var n = maxOf(settings.participants, humans)
            if (format == TournamentFormat.Knockout) n = KNOCKOUT_SIZES.first { it >= n.coerceAtMost(32) } // Tableau complet.
            update(settings.copy(format = format, participants = n))
        }, label = tr("Format"), height = 32.dp, enabled = !readOnly)
        FieldLabel(tr("Participants"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val min = maxOf(Tournament.MIN_PARTICIPANTS, humans)
            fun step(d: Int) {
                val n = if (settings.format == TournamentFormat.Knockout) {
                    val sizes = KNOCKOUT_SIZES.filter { it >= humans }
                    sizes.getOrElse(sizes.indexOf(settings.participants) + d) { settings.participants }
                } else (settings.participants + d).coerceIn(min, Tournament.MAX_PARTICIPANTS)
                update(settings.copy(participants = n))
            }
            IconBtn(HoloIcon.Back, { step(-1) }, width = 40.dp, height = 34.dp, iconSize = 16.dp, enabled = !readOnly)
            Box(Modifier.width(64.dp).height(34.dp), contentAlignment = Alignment.Center) {
                Text("${settings.participants}", style = displayStyle(18.sp, Holo.text, 0f))
            }
            IconBtn(HoloIcon.Next, { step(1) }, width = 40.dp, height = 34.dp, iconSize = 16.dp, enabled = !readOnly)
            val bots = settings.participants - humans
            val rounds = roundsFor(settings.format, settings.participants)
            Text(if (humans > 1) tr("%1 humains + %2 bots · %3 tours", humans, bots, rounds) else tr("vous + %1 bots · %2 tours", bots, rounds),
                style = bodyStyle(12.sp, Holo.muted))
        }
        Segmented(MatchFormat.entries.map(::matchLabel), settings.matchFormat.ordinal, { update(settings.copy(matchFormat = MatchFormat.entries[it])) },
            label = tr("Matchs"), height = 32.dp, enabled = !readOnly)
        Hint(tr("Express : premier à 2 manches · Classique : 3 toupies, ordre choisi avant chaque match"), color = Holo.dim)
        if (settings.bots.isNotEmpty()) {
            val level = settings.bots.distinct().singleOrNull()?.ordinal ?: -1 // -1 : niveaux mélangés.
            Segmented(Difficulty.entries.map { tr(it.label) }, level, { d -> update(settings.copy(bots = settings.bots.map { Difficulty.entries[d] })) },
                label = tr("Difficulté des bots"), height = 32.dp, enabled = !readOnly)
        }
    }
}

/** Accueil des tournois : reprise du tournoi sauvegardé, tournoi solo, tournoi en réseau local. */
@Composable
fun TournamentSetupScreen(app: AppController) {
    var solo by remember { mutableStateOf(false) }
    if (solo) return SoloTournamentConfig(app) { solo = false }
    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Tournois"), { app.screen = Screen.Menu }, sub = tr("ligue ou élimination directe"))
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                app.saved?.let { t ->
                    ChoiceCard(HoloIcon.Save, tr("Reprendre"), Holo.warn, listOf(
                        "${formatLabel(t.settings.format)} · ${tr("%1 participants", t.participants.size)}",
                        "${matchLabel(t.settings.matchFormat)} · ${roundLabel(t)}",
                    ), tr("Reprendre"), true, Modifier.weight(1f), app::resumeSoloTournament)
                }
                ChoiceCard(HoloIcon.Trophy, tr("Tournoi solo"), Holo.green, listOf(
                    tr("Contre des bots"), tr("Ligue ou élimination"), tr("4 à 32 participants"), tr("matchs express ou classiques"),
                ), tr("Configurer"), app.saved == null, Modifier.weight(1f)) { solo = true }
                ChoiceCard(HoloIcon.Lan, tr("En réseau local"), Holo.cyan, listOf(
                    tr("2 à 4 humains"), tr("Chacun sur son appareil"), tr("PC et mobiles mélangés"), tr("complété par des bots"),
                ), tr("Configurer"), false, Modifier.weight(1f)) { app.screen = Screen.LanMenu(TournamentSettings()) }
            }
        }
    }
}

@Composable
private fun ChoiceCard(icon: HoloIcon, title: String, accent: Color, lines: List<String>, button: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Panel(modifier.fillMaxHeight(), accent = accent, selected = primary, onClick = onClick) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(accent.copy(alpha = 0.15f), size.minDimension / 2)
                    drawCircle(accent.copy(alpha = 0.6f), size.minDimension / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                }
                Icon(icon, accent, 28.dp)
            }
            Text(title.uppercase(), style = displayStyle(15.sp), modifier = Modifier.padding(top = 12.dp))
            Text(lines.first(), style = bodyStyle(12.sp, accent))
            lines.drop(1).forEach { Text(it, style = bodyStyle(11.5.sp, Holo.muted), textAlign = TextAlign.Center) }
        }
        HoloButton(button, onClick, Modifier.fillMaxWidth(), if (primary) BtnVariant.Primary else BtnVariant.Secondary,
            if (primary) HoloIcon.Play else HoloIcon.Next, height = 42.dp, textSize = 12f)
    }
}

/** Configuration d'un tournoi solo ; « Lancer » en haut à droite, règles dans un tiroir. */
@Composable
private fun SoloTournamentConfig(app: AppController, onBack: () -> Unit) {
    val tops = remember(app.tick) { app.repository.allTops }
    var settings by remember { mutableStateOf(TournamentSettings(format = TournamentFormat.Knockout).fitBots(1)) }
    var picks by remember { mutableStateOf(listOf(tops[0], tops[1 % tops.size], tops[2 % tops.size])) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    var rules by remember { mutableStateOf(MatchRules()) }
    var showRules by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<Int?>(null) }
    val needed = MatchSeries.topsFor(settings.matchFormat)

    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Tournoi solo"), onBack, sub = tr("contre des bots")) {
                IconBtn(HoloIcon.Sliders, { showRules = true }, on = !rules.isDefault)
                HoloButton(tr("Lancer"), { app.startSoloTournament(settings.copy(stadiumId = stadium, rules = rules), picks.take(needed)) },
                    Modifier.width(150.dp), BtnVariant.Primary, HoloIcon.Play, height = 40.dp, textSize = 12f)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    TournamentSettingsForm(settings, 1, { settings = it })
                }
                Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Panel(Modifier.fillMaxWidth().weight(1f), title = if (needed > 1) tr("Vos toupies") else tr("Votre toupie"),
                        index = if (needed > 1) tr("%1 toupies", needed) else tr("1 toupie")) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(needed) { k -> TopRow(picks[k], height = if (needed > 1) 38.dp else 48.dp, chevron = true) { picking = k } }
                        }
                        if (needed == 1) Hint(tr("En classique : trois toupies, ordre choisi avant chaque match."), Modifier.padding(top = 6.dp), Holo.dim)
                    }
                    FieldLabel(tr("Stadium"))
                    StadiumStrip(stadium, { stadium = it }, Modifier.height(52.dp))
                }
            }
        }
        val k = picking
        TopPickerDrawer(k != null, tr("Toupie %1", (k ?: 0) + 1), tops, k?.let { picks[it] }, { t ->
            if (t != null && k != null) picks = picks.toMutableList().also { it[k] = t }
        }, { picking = null })
        RulesDrawer(showRules, rules, { rules = it }, { showRules = false })
    }
}

/** Tableau de bord du tournoi : tableau (élimination) ou classement (ligue), « Votre match » sous le pouce droit. */
@Composable
fun TournamentHubScreen(app: AppController) {
    app.tick // Recomposition à chaque changement.
    val controller = app.tournament
    val t = controller?.tournament
    var results by remember { mutableStateOf(false) }
    var ordering by remember { mutableStateOf(false) }
    var quitting by remember { mutableStateOf(false) }
    val solo = app.isSoloTournament
    fun quit() { if (t == null || t.isFinished) app.leaveTournament() else quitting = true }

    HoloScreen {
        if (controller == null || t == null) {
            Column(Modifier.fillMaxSize()) {
                TopBar(tr("Tournoi"), { app.leaveTournament() }, backIcon = HoloIcon.Quit)
                Hint(if (controller == null) tr("Aucun tournoi en cours.") else tr("En attente de l'hôte…"), Modifier.padding(EdgeMargin))
            }
            return@HoloScreen
        }
        val local = controller.localParticipant
        fun name(p: Int): String = when {
            p < 0 -> tr("exempt")
            p == local -> tr("Vous")
            else -> t.participants[p].name
        }
        androidx.activity.compose.BackHandler { quit() }
        Column(Modifier.fillMaxSize()) {
            TopBar(formatLabel(t.settings.format), ::quit, backIcon = HoloIcon.Quit,
                sub = "${tr("%1 participants", t.participants.size)} · ${matchLabel(t.settings.matchFormat).lowercase()}") {
                if (app.server != null && t.isFinished) HoloButton(tr("Retour au lobby"), app::returnToLobby, Modifier.width(190.dp), BtnVariant.Primary, HoloIcon.Lan, height = 40.dp, textSize = 12f)
                HoloButton(tr("Résultats"), { results = true }, Modifier.width(150.dp), BtnVariant.Ghost, HoloIcon.Trophy, height = 40.dp, textSize = 12f)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(start = EdgeMargin, end = EdgeMargin, top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (t.settings.format == TournamentFormat.Knockout) Bracket(t, local, ::name) else Standings(t, local, ::name)
                }
                YourMatch(app, t, local, ::name, Modifier.width(290.dp).fillMaxHeight()) { ordering = true }
            }
        }

        SideDrawer(results, DrawerSide.Start, tr("Tours et résultats"), { results = false }, width = 420.dp,
            sub = tr("tour %1/%2", t.currentRound + 1, t.roundCount)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                t.rounds.forEachIndexed { r, round ->
                    item {
                        FieldLabel("${roundName(t, r)} · ${round.count { it.played }}/${round.size}", Modifier.padding(top = 6.dp),
                            if (r == t.currentRound) Holo.cyan else Holo.muted)
                    }
                    itemsIndexed(round) { i, m ->
                        val live = if (r == t.currentRound) controller.liveOf(i) else null
                        val mine = local >= 0 && (m.a == local || m.b == local)
                        HoloCard(Modifier.fillMaxWidth().height(34.dp), selected = mine) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(name(m.a).uppercase(), style = displayStyle(10.5.sp, if (m.a == local) Holo.green else Holo.text), modifier = Modifier.weight(1f), maxLines = 1)
                                Text(when {
                                    m.isBye -> tr("exempt")
                                    m.played -> "${m.score[0]} – ${m.score[1]}"
                                    live != null && live.series.rounds > 0 -> "${live.series.score[0]} – ${live.series.score[1]}"
                                    else -> tr("à jouer")
                                }, style = displayStyle(11.sp, Holo.cyan, 0.04f), modifier = Modifier.width(90.dp), textAlign = TextAlign.Center)
                                Text(if (m.isBye) "" else name(m.b).uppercase(), style = displayStyle(10.5.sp, if (m.b == local) Holo.green else Holo.text),
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        val myTops = t.participants.getOrNull(local)?.tops.orEmpty()
        TopOrderDrawer(ordering, myTops, { ordering = false }) { ordering = false; controller.submitOrder(it) }

        if (quitting) androidx.compose.ui.window.Dialog({ quitting = false }, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Panel(Modifier.width(540.dp), title = tr("Quitter le tournoi ?"), accent = Holo.warn) {
                Text(if (solo) tr("Sauvegardez-le pour le reprendre plus tard depuis Tournois (un seul emplacement).")
                else tr("Vous quitterez aussi le lobby ; vos matchs restants seront perdus."), style = bodyStyle(13.sp, Holo.muted))
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoloButton(tr("Annuler"), { quitting = false }, Modifier.weight(1f), BtnVariant.Ghost, height = 42.dp, textSize = 11.5f)
                    if (solo) {
                        HoloButton(tr("Quitter"), { quitting = false; app.quitSoloTournament(false) }, Modifier.weight(1f), BtnVariant.Danger, height = 42.dp, textSize = 11.5f)
                        HoloButton(tr("Sauvegarder"), { quitting = false; app.quitSoloTournament(true) }, Modifier.weight(1.3f), BtnVariant.Primary, HoloIcon.Save, height = 42.dp, textSize = 11.5f)
                    } else HoloButton(tr("Quitter"), { quitting = false; app.leaveTournament() }, Modifier.weight(1f), BtnVariant.Danger, height = 42.dp, textSize = 11.5f)
                }
            }
        }
    }
}

/** Ligne d'un participant dans une case du tableau. */
@Composable
private fun BracketLine(t: Tournament, p: Int, score: Int?, won: Boolean, local: Int, name: (Int) -> String) {
    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TopView(t.participants.getOrNull(p)?.tops?.firstOrNull(), 14.dp)
        Text(name(p).uppercase(), style = displayStyle(9.5.sp, if (p == local) Holo.green else if (won) Holo.text else Holo.muted, 0.06f),
            modifier = Modifier.weight(1f), maxLines = 1)
        if (score != null) Text("$score", style = displayStyle(10.sp, if (won) Holo.text else Holo.dim, 0f))
    }
}

/** Tableau à élimination directe, défilant horizontalement et verticalement. */
// Objets du lobby / tournoi modifiés sur place : ne jamais sauter la recomposition.
@NonSkippableComposable
@Composable
private fun Bracket(t: Tournament, local: Int, name: (Int) -> String) {
    val cell: Dp = 54.dp
    val first = t.rounds.firstOrNull()?.size ?: 0
    Row(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        for (r in 0 until t.roundCount) {
            val round: List<TournamentMatch>? = t.rounds.getOrNull(r)
            val count = round?.size ?: (first shr r).coerceAtLeast(1)
            Column(Modifier.width(150.dp)) {
                FieldLabel(roundName(t, r), Modifier.padding(bottom = 6.dp), if (r == t.currentRound) Holo.cyan else Holo.muted)
                // Chaque case est centrée face aux deux cases qui l'alimentent.
                val slot = cell * (1 shl r) + 8.dp * ((1 shl r) - 1)
                repeat(count) { i ->
                    Box(Modifier.height(slot + if (i < count - 1) 8.dp else 0.dp), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.height(slot), contentAlignment = Alignment.Center) {
                            val m = round?.getOrNull(i)
                            val mine = m != null && local >= 0 && (m.a == local || m.b == local)
                            HoloCard(Modifier.fillMaxWidth().height(cell), selected = mine && !m!!.played) {
                                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
                                    if (m == null) {
                                        Text(tr("À déterminer").uppercase(), style = labelStyle(Holo.dim, 9.sp))
                                        Text(tr("À déterminer").uppercase(), style = labelStyle(Holo.dim, 9.sp))
                                    } else {
                                        val score = if (m.played && !m.isBye) m.score else null
                                        BracketLine(t, m.a, score?.get(0), m.played && m.winner == m.a, local, name)
                                        if (m.isBye) Text(tr("exempt"), style = bodyStyle(10.sp, Holo.dim))
                                        else BracketLine(t, m.b, score?.get(1), m.played && m.winner == m.b, local, name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Classement de la ligue : podium coloré, vous en vert. */
// Objets du lobby / tournoi modifiés sur place : ne jamais sauter la recomposition.
@NonSkippableComposable
@Composable
private fun Standings(t: Tournament, local: Int, name: (Int) -> String) {
    val podium = listOf(Holo.warn, Color(0xFFC9D2F0), Color(0xFFE08A4A))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)) {
            Spacer(Modifier.weight(1f))
            listOf("J", "V", "D", "Pts", "Diff.").forEach { Text(tr(it).uppercase(), style = labelStyle(Holo.dim, 9.sp), modifier = Modifier.width(44.dp), textAlign = TextAlign.Center) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            itemsIndexed(t.standings()) { rank, s ->
                val me = s.participant == local
                HoloCard(Modifier.fillMaxWidth().height(40.dp), selected = me) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${rank + 1}", style = displayStyle(13.sp, podium.getOrElse(rank) { Holo.muted }, 0f), modifier = Modifier.width(22.dp))
                        TopView(t.participants[s.participant].tops.firstOrNull(), 26.dp)
                        Column(Modifier.weight(1f)) {
                            Text(name(s.participant).uppercase(), style = displayStyle(10.5.sp, if (me) Holo.green else Holo.text), maxLines = 1)
                            Text(t.participants[s.participant].tops.firstOrNull()?.name.orEmpty(), style = bodyStyle(10.sp, Holo.muted), maxLines = 1)
                        }
                        val diff = s.points - s.against
                        listOf("${s.played}", "${s.won}", "${s.lost}", "${s.points}").forEach {
                            Text(it, style = displayStyle(11.sp, Holo.text, 0f), modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                        }
                        Text(if (diff > 0) "+$diff" else "$diff", style = displayStyle(11.sp, if (diff > 0) Holo.green else if (diff < 0) Holo.danger else Holo.muted, 0f),
                            modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** « Votre match » : adversaire, règle du match et bouton Jouer (ordre des toupies en classique). */
// Objets du lobby / tournoi modifiés sur place : ne jamais sauter la recomposition.
@NonSkippableComposable
@Composable
private fun YourMatch(app: AppController, t: Tournament, local: Int, name: (Int) -> String, modifier: Modifier, onOrder: () -> Unit) {
    val controller = app.tournament ?: return
    val match = controller.localMatch
    Panel(modifier, title = if (t.isFinished) tr("Champion") else tr("Votre match"), index = if (t.isFinished) null else roundName(t)) {
        if (t.isFinished) {
            val champion = t.champion
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                TopView(t.participants.getOrNull(champion)?.tops?.firstOrNull(), 110.dp)
                Text(name(champion).uppercase(), style = displayStyle(16.sp, if (champion == local) Holo.green else Holo.warn))
                Text(if (champion == local) tr("Vous remportez le tournoi !") else tr("remporte le tournoi"), style = bodyStyle(12.sp, Holo.muted))
            }
            return@Panel
        }
        if (match < 0) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Hint(when {
                    local < 0 -> tr("Vous suivez le tournoi.")
                    t.settings.format == TournamentFormat.Knockout && t.rounds[t.currentRound].none { it.a == local || it.b == local } ->
                        tr("Vous êtes éliminé·e. La suite du tournoi se joue sans vous.")
                    else -> tr("Rien à jouer pour vous dans ce tour : en attente des autres matchs…")
                }, color = Holo.muted)
            }
            return@Panel
        }
        val m = t.rounds[t.currentRound][match]
        val opponent = if (m.a == local) m.b else m.a
        val ready = controller.localReady
        val classic = t.settings.matchFormat == MatchFormat.Classic
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(local, -2, opponent).forEach { p ->
                if (p == -2) VsBadge()
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val top: Top? = t.participants.getOrNull(p)?.tops?.firstOrNull()
                    TopView(top, 74.dp)
                    Text(name(p).uppercase(), style = displayStyle(11.sp, if (p == local) Holo.green else Holo.danger), maxLines = 1)
                    val who = t.participants[p]
                    Text(listOfNotNull(top?.name, if (who.isBot) tr(who.level.label).lowercase() else null).joinToString(" · "),
                        style = bodyStyle(10.sp, Holo.muted), maxLines = 1)
                }
            }
        }
        Text("${matchRule(t.settings.matchFormat)} · ${(Stadium.find(t.settings.stadiumId) ?: Stadium.classic).displayName()}",
            style = bodyStyle(11.5.sp, Holo.text), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
        if (ready) Hint(tr("En attente de votre adversaire…"), Modifier.fillMaxWidth().padding(bottom = 8.dp), Holo.warn)
        HoloButton(if (classic) tr("Ordre des toupies et jouer") else tr("Jouer le match"), { if (classic) onOrder() else controller.submitOrder(listOf(0)) },
            Modifier.fillMaxWidth(), BtnVariant.Primary, if (classic) HoloIcon.Sliders else HoloIcon.Play, height = 46.dp, enabled = !ready, textSize = 13f)
    }
}

/** Ordre de passage des toupies d'un match classique (monter / descendre). */
@Composable
private fun TopOrderDrawer(visible: Boolean, tops: List<Top>, onClose: () -> Unit, onValidate: (List<Int>) -> Unit) {
    var order by remember(tops) { mutableStateOf(tops.indices.toList()) }
    var selected by remember { mutableIntStateOf(0) }
    fun move(delta: Int) {
        val to = selected + delta
        if (to !in order.indices) return
        order = order.toMutableList().also { val v = it.removeAt(selected); it.add(to, v) }
        selected = to
    }
    SideDrawer(visible, DrawerSide.End, tr("Ordre des toupies"), onClose, width = 420.dp, sub = tr("secret pour l'adversaire"),
        footer = { HoloButton(tr("Valider et jouer"), { onValidate(order) }, Modifier.weight(1f), BtnVariant.Primary, HoloIcon.Play, height = 44.dp) }) {
        Hint(tr("La 1re toupie joue la manche 1, la 2e la manche 2, la 3e la manche 3, puis on recommence."), color = Holo.muted)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                order.forEachIndexed { k, i ->
                    TopRow(tops[i], selected = k == selected, height = 44.dp, name = "${k + 1}. ${tops[i].name}") { selected = k }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBtn(HoloIcon.Back, { move(-1) }, Modifier.rotate(90f))
                IconBtn(HoloIcon.Back, { move(1) }, Modifier.rotate(-90f))
            }
        }
    }
}
