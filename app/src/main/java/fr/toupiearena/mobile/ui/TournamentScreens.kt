package fr.toupiearena.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import fr.toupiearena.mobile.game.TournamentSettings
import kotlin.math.roundToInt

/**
 * Réglages d'un tournoi : format, type de match, participants (4, 8, 16 ou 32
 * en élimination directe) et niveau de chaque bot (dans une fenêtre).
 */
@Composable
fun TournamentSettingsEditor(settings: TournamentSettings, humans: Int, onChange: (TournamentSettings) -> Unit, readOnly: Boolean = false) {
    var showBots by remember { mutableStateOf(false) }
    fun update(s: TournamentSettings) = onChange(s.fitBots(humans))
    if (readOnly) {
        Text("${settings.format.label} · match ${settings.matchFormat.label.lowercase()} · ${settings.participants} participants",
            color = Palette.text)
    } else {
        Picker("Format", TournamentFormat.entries, settings.format.ordinal, { it.label }, {
            val format = TournamentFormat.entries[it]
            // Élimination directe : tableau complet.
            var n = settings.participants
            if (format == TournamentFormat.Knockout) n = listOf(4, 8, 16, 32).first { it >= n.coerceAtMost(32) }
            update(settings.copy(format = format, participants = maxOf(n, humans)))
        })
        Picker("Type de match", MatchFormat.entries, settings.matchFormat.ordinal, { it.label },
            { update(settings.copy(matchFormat = MatchFormat.entries[it])) })
        if (settings.format == TournamentFormat.Knockout) {
            val counts = listOf(4, 8, 16, 32).filter { it >= humans }
            Picker("Participants", counts, counts.indexOf(settings.participants).coerceAtLeast(0), { "$it participants" },
                { update(settings.copy(participants = counts[it])) })
        } else {
            Text("Participants : ${settings.participants}", color = Palette.text)
            Slider(settings.participants.toFloat(), { update(settings.copy(participants = it.roundToInt())) },
                valueRange = maxOf(Tournament.MIN_PARTICIPANTS, humans).toFloat()..Tournament.MAX_PARTICIPANTS.toFloat())
        }
    }
    Hint(if (settings.matchFormat == MatchFormat.Express)
        "Express : une toupie pour tout le tournoi ; le premier à gagner 2 manches remporte le match."
    else "Classique : trois toupies jouées dans l'ordre choisi avant chaque match ; premier à 4 points " +
        "(arrêt de la toupie adverse : 1 pt, éjection : 2 pts).")
    val counts = Difficulty.entries.map { d -> settings.bots.count { it == d } }
    val summary = Difficulty.entries.filterIndexed { i, _ -> counts[i] > 0 }.joinToString(", ") { "${counts[it.ordinal]} ${it.label.lowercase()}" }
    Text("dont $humans humain(s) et ${settings.bots.size} bot(s)", color = Palette.muted, fontSize = 13.sp)
    if (settings.bots.isNotEmpty()) SecondaryButton("Bots : $summary…", { showBots = true })

    if (showBots) AlertDialog(
        onDismissRequest = { showBots = false }, confirmButton = { AccentButton("Fermer", { showBots = false }) },
        title = { Text("Niveau des bots") },
        text = {
            Column {
                if (!readOnly) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Difficulty.entries.forEach { d ->
                        SecondaryButton("Tous : ${d.label}", { onChange(settings.copy(bots = settings.bots.map { d })) })
                    }
                }
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    itemsIndexed(settings.bots) { i, level ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bot ${i + 1}", Modifier.width(80.dp), color = Palette.text)
                            if (readOnly) Text(level.label, color = Palette.text)
                            else Picker("", Difficulty.entries, level.ordinal, { it.label }, { d ->
                                onChange(settings.copy(bots = settings.bots.toMutableList().also { it[i] = Difficulty.entries[d] }))
                            }, Modifier.weight(1f))
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun TournamentSetupScreen(app: AppController) {
    var solo by remember { mutableStateOf(false) }
    if (!solo) {
        Page("Tournois", { app.screen = Screen.Menu }) {
            ChoiceCard("🏆  Tournoi solo", "Vous contre des bots : ligue ou élimination directe, de 4 à 32 participants.") { solo = true }
            ChoiceCard("⇄  Tournoi en réseau local", "De 2 à 4 joueurs humains, chacun sur son appareil (PC ou mobile), " +
                "complétés par des bots.") { app.screen = Screen.LanMenu(TournamentSettings()) }
        }
        return
    }
    val tops = remember(app.tick) { app.repository.allTops }
    var settings by remember { mutableStateOf(TournamentSettings().fitBots(1)) }
    var picks by remember { mutableStateOf(listOf(0, 1, 2)) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    var rules by remember { mutableStateOf(MatchRules()) }
    var showRules by remember { mutableStateOf(false) }
    val needed = MatchSeries.topsFor(settings.matchFormat)

    Page("Tournoi solo", { solo = false }, "◀ Retour", actions = {
        SecondaryButton(if (rules.isDefault) "Personnalisation…" else "Personnalisation ✱", { showRules = true })
        HSpace()
        AccentButton("Lancer le tournoi", {
            app.startSoloTournament(settings.copy(stadiumId = stadium, rules = rules), picks.take(needed).map { tops[it] })
        })
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Section("Tournoi", Modifier.weight(1f)) { TournamentSettingsEditor(settings, 1, { settings = it }) }
            Section("Vos toupies et le stadium", Modifier.weight(1f)) {
                repeat(needed) { k ->
                    Picker("Toupie ${k + 1}", tops, picks[k], ::topLabel, { i -> picks = picks.toMutableList().also { it[k] = i } })
                }
                StadiumPicker(stadium, { stadium = it })
            }
        }
    }
    if (showRules) RulesDialog(rules, { rules = it }, false) { showRules = false }
}

@Composable
fun ChoiceCard(title: String, description: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().border(2.dp, Palette.border, RoundedCornerShape(14.dp))
        .background(Palette.panel, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(18.dp)) {
        Text(title, color = Palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Palette.muted)
    }
}

/** Tableau de bord du tournoi : tours et résultats, classement (ligue), match du joueur. */
@Composable
fun TournamentHubScreen(app: AppController) {
    val controller = app.tournament ?: return Page("Tournoi", { app.screen = Screen.Menu }) { Hint("Aucun tournoi en cours.") }
    app.tick // Recomposition à chaque changement.
    val t = controller.tournament ?: return Page("Tournoi", { app.leaveTournament() }) { Hint("En attente de l'hôte…") }
    val local = controller.localParticipant
    var ordering by remember { mutableStateOf(false) }

    fun label(p: Int): String {
        if (p < 0) return "exempt"
        val who = t.participants[p]
        return when {
            p == local -> if (who.name == "Vous") "Vous" else "${who.name} (vous)"
            who.isBot -> "${who.name} (${who.level.label})"
            else -> who.name
        }
    }

    val subtitle = if (t.isFinished) {
        val champion = t.champion
        if (champion == local) "🏆 Tournoi terminé : vous êtes champion·ne !" else "🏆 Tournoi terminé — champion·ne : ${label(champion)}"
    } else "${t.roundName(t.currentRound)} · ${t.participants.size} participants · stadium ${(Stadium.find(t.settings.stadiumId) ?: Stadium.classic).name}"

    Page("${t.settings.format.label} · match ${t.settings.matchFormat.label.lowercase()}", { app.leaveTournament() },
        "Quitter le tournoi", scroll = false, actions = {
            if (app.server != null && t.isFinished) AccentButton("Retour au lobby", app::returnToLobby)
        }) {
        Text(subtitle, color = Palette.text)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyColumn(Modifier.weight(1f).fillMaxHeight().background(Palette.panel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                t.rounds.forEachIndexed { r, round ->
                    item { Text(t.roundName(r), color = if (r == t.currentRound) Palette.accent else Palette.muted, fontWeight = FontWeight.Bold) }
                    if (r == t.currentRound || r == t.currentRound - 1 || t.settings.format == TournamentFormat.Knockout) {
                        itemsIndexed(round) { i, m ->
                            val live = if (r == t.currentRound) controller.liveOf(i) else null
                            val text = when {
                                m.isBye -> "${label(m.a)} — exempt(e) ce tour"
                                m.played -> "${label(m.a)}   ${m.score[0]} – ${m.score[1]}   ${label(m.b)}"
                                live != null && live.series.rounds > 0 ->
                                    "${label(m.a)}   ${live.series.score[0]} – ${live.series.score[1]}   ${label(m.b)} (en cours)"
                                else -> "${label(m.a)}   contre   ${label(m.b)}"
                            }
                            Text(text, Modifier.padding(start = 10.dp, top = 2.dp),
                                color = if (local >= 0 && (m.a == local || m.b == local)) Palette.accent else Palette.text, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (t.settings.format == TournamentFormat.League) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight().background(Palette.panel, RoundedCornerShape(12.dp)).padding(10.dp)) {
                    item {
                        Row { listOf("#" to 28, "Participant" to 0, "J" to 30, "V" to 30, "D" to 30, "Pts" to 36).forEach { (h, w) ->
                            Text(h, if (w == 0) Modifier.weight(1f) else Modifier.width(w.dp), color = Palette.accent, fontWeight = FontWeight.Bold)
                        } }
                    }
                    itemsIndexed(t.standings()) { rank, s ->
                        val color = if (s.participant == local) Palette.accent else Palette.text
                        Row {
                            Text("${rank + 1}", Modifier.width(28.dp), color = color, fontSize = 13.sp)
                            Text(label(s.participant), Modifier.weight(1f), color = color, fontSize = 13.sp, maxLines = 1)
                            listOf(s.played, s.won, s.lost).forEach { Text("$it", Modifier.width(30.dp), color = color, fontSize = 13.sp) }
                            Text("${s.points}", Modifier.width(36.dp), color = color, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        // Match du joueur local.
        val match = controller.localMatch
        Section("Votre match") {
            if (match >= 0) {
                val m = t.rounds[t.currentRound][match]
                val opponent = if (m.a == local) m.b else m.a
                val ready = controller.localReady
                Text(if (ready) "Contre ${label(opponent)} — en attente de votre adversaire…"
                else "${t.roundName(t.currentRound)} : vous affrontez ${label(opponent)}.", color = Palette.text)
                val classic = t.settings.matchFormat == MatchFormat.Classic
                AccentButton(if (classic) "Choisir l'ordre de mes toupies et jouer…" else "Prêt — jouer le match", {
                    if (classic) ordering = true else controller.submitOrder(listOf(0))
                }, Modifier.fillMaxWidth(), enabled = !ready)
            } else {
                Text(when {
                    t.isFinished -> "Le tournoi est terminé."
                    local < 0 -> "Vous suivez le tournoi."
                    t.settings.format == TournamentFormat.Knockout && t.matchOf(local) < 0 &&
                        t.rounds[t.currentRound].none { it.a == local } -> "Vous êtes éliminé·e. La suite du tournoi se joue sans vous."
                    else -> "Rien à jouer pour vous dans ce tour : en attente des autres matchs…"
                }, color = Palette.text)
            }
        }
    }
    if (ordering) TopOrderDialog(t.participants.getOrNull(local)?.tops.orEmpty(), { ordering = false }) {
        ordering = false
        controller.submitOrder(it)
    }
}

/** Ordre de passage des toupies (Monter / Descendre). */
@Composable
fun TopOrderDialog(tops: List<Top>, onCancel: () -> Unit, onValidate: (List<Int>) -> Unit) {
    var order by remember { mutableStateOf(tops.indices.toList()) }
    var selected by remember { mutableStateOf(0) }
    fun move(delta: Int) {
        val to = selected + delta
        if (to !in order.indices) return
        order = order.toMutableList().also { val v = it.removeAt(selected); it.add(to, v) }
        selected = to
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Ordre de vos toupies") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Hint("La 1re toupie joue la manche 1, la 2e la manche 2, la 3e la manche 3, puis on recommence. " +
                    "Votre adversaire ne voit pas votre ordre.")
                Row {
                    LazyColumn(Modifier.weight(1f)) {
                        items(order.size) { k ->
                            Text("${k + 1}. ${topLabel(tops[order[k]])}", Modifier.fillMaxWidth().clickable { selected = k }
                                .background(if (k == selected) Palette.accentDark else Color.Transparent).padding(8.dp), color = Palette.text)
                        }
                    }
                    Column {
                        SecondaryButton("▲ Monter", { move(-1) })
                        SecondaryButton("▼ Descendre", { move(1) })
                    }
                }
            }
        },
        confirmButton = { AccentButton("Valider et jouer", { onValidate(order) }) },
        dismissButton = { SecondaryButton("Annuler", onCancel) },
    )
}
