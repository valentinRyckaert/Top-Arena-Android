package fr.toupiearena.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.DuelMode
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.game.TournamentFormat
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.DrawerSide
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloSlider
import fr.toupiearena.mobile.ui.holo.HoloToggle
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.TypeBadge
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.tr
import kotlin.math.roundToInt

/** Nom de pièce, toupie prédéfinie, stadium ou coup traduit (le catalogue reste en français). */
fun Top.displayName() = if (PartCatalog.presets.any { it.name == name }) tr(name) else name
fun Stadium.displayName() = tr(name)
fun specialName(id: String) = SpecialMove.find(id)?.let { tr(it.name) } ?: id

/** Ligne de liste : vignette, nom, badge de type, détail à droite. */
@Composable
fun TopRow(top: Top?, modifier: Modifier = Modifier, selected: Boolean = false, height: Dp = 48.dp, name: String? = null,
           star: Boolean = false, trailing: String? = null, chevron: Boolean = false, onClick: (() -> Unit)? = null) {
    HoloCard(modifier.fillMaxWidth().height(height), selected = selected, onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TopView(top, height - 10.dp)
            Column(Modifier.weight(1f)) {
                Text((name ?: top?.displayName() ?: tr("Aléatoire")).uppercase(), style = displayStyle(12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (top != null && height >= 40.dp) TypeBadge(top.type, Modifier.padding(top = 3.dp))
                else if (top == null && height >= 48.dp) Text(tr("Toupie tirée au hasard au lancement"), style = bodyStyle(11.5.sp, Holo.muted))
            }
            if (star) Icon(HoloIcon.Star, Holo.cyan, 13.dp)
            if (trailing != null) Text(trailing, style = bodyStyle(12.sp, Holo.muted))
            if (chevron) Icon(HoloIcon.Next, Holo.cyan, 14.dp)
        }
    }
}

/** Tiroir de choix d'une toupie : les vôtres (étoile) puis les prédéfinies ; « Aléatoire » en option. */
@Composable
fun TopPickerDrawer(visible: Boolean, title: String, all: List<Top>, selected: Top?, onPick: (Top?) -> Unit, onClose: () -> Unit, allowRandom: Boolean = false) {
    val custom = all.filter { t -> PartCatalog.presets.none { it.name == t.name } }
    val presets = all - custom.toSet()
    SideDrawer(visible, DrawerSide.Start, title, onClose, width = 430.dp, sub = tr("%1 prédéfinies · %2 à vous", presets.size, custom.size)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (allowRandom) item { TopRow(null, selected = selected == null, height = 44.dp) { onPick(null); onClose() } }
            if (custom.isNotEmpty()) item { FieldLabel(tr("Mes toupies"), color = Holo.cyan) }
            items(custom) { t -> TopRow(t, selected = t == selected, height = 44.dp, star = true) { onPick(t); onClose() } }
            item { FieldLabel(tr("Prédéfinies"), Modifier.padding(top = 4.dp), color = Holo.cyan) }
            items(presets) { t -> TopRow(t, selected = t == selected, height = 44.dp) { onPick(t); onClose() } }
        }
    }
}

/** Bande défilante des stadiums. */
@Composable
fun StadiumStrip(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Stadium.all) { s -> StadiumCard(s, s.id == selected, Modifier.width(140.dp)) { onSelect(s.id) } }
    }
}

@Composable
fun StadiumCard(stadium: Stadium, selected: Boolean, modifier: Modifier = Modifier, height: Dp = 52.dp, onClick: (() -> Unit)? = null) {
    HoloCard(modifier.height(height), selected = selected, onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            StadiumView(stadium, height)
            Column(Modifier.weight(1f)) {
                Text(stadium.displayName().uppercase(), style = displayStyle(11.5.sp), maxLines = 1)
                Text(tr(stadium.codename), style = bodyStyle(9.5.sp, stadiumColor(stadium), androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
            }
            if (selected) Icon(HoloIcon.Check, Holo.green, 10.dp, Modifier.align(Alignment.Top).padding(top = 6.dp))
        }
    }
}

/** Couleur signature d'un stadium (nom de code). */
fun stadiumColor(stadium: Stadium): Color = when (stadium.id) {
    "beton" -> Holo.green; "terre" -> Holo.lime; "glace" -> Color(0xFF8FF3FF); "sable" -> Holo.warn; "volcan" -> Color(0xFFFF6A3D); else -> Holo.cyan
}

// ---------------------------------------------------------------- personnalisation

/** Formulaire des règles (combat, lobby, tournoi). Le duel suit les Paramètres tant qu'on n'y touche pas. */
@Composable
fun RulesForm(rules: MatchRules, onChange: (MatchRules) -> Unit, readOnly: Boolean = false) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        @Composable fun percent(label: String, value: Double, set: (Double) -> MatchRules) =
            HoloSlider((value / 3.0).toFloat(), { onChange(set((it * 60).roundToInt() / 20.0)) }, label, "${(value * 100).roundToInt()} %", enabled = !readOnly)
        percent(tr("Vitesse"), rules.speed) { rules.copy(speed = maxOf(0.25, it)) }
        percent(tr("Rebonds après un choc"), rules.knockback) { rules.copy(knockback = it) }
        percent(tr("Usure"), rules.wear) { rules.copy(wear = it) }
        percent(tr("Dégâts"), rules.damage) { rules.copy(damage = it) }
        percent(tr("Puissance des dashs"), rules.dashPower) { rules.copy(dashPower = it) }
        percent(tr("Pilotage"), rules.steering) { rules.copy(steering = it) }
        HoloSlider(((rules.maxEndurance - 10) / 290).toFloat(), { onChange(rules.copy(maxEndurance = 10 + (it * 29).roundToInt() * 10.0)) },
            tr("Endurance de départ"), "${rules.maxEndurance.roundToInt()}", enabled = !readOnly)
        val limits = listOf(0.0, 30.0, 60.0, 90.0, 120.0, 180.0)
        Segmented(limits.map { if (it > 0) "${it.roundToInt()} s" else tr("Aucun") }, limits.indexOfFirst { it == rules.timeLimit }.coerceAtLeast(0),
            { if (!readOnly) onChange(rules.copy(timeLimit = limits[it])) }, label = tr("Temps maximal"), height = 32.dp, textSize = 10.5f, enabled = !readOnly)
        HoloToggle(tr("Coups spéciaux"), rules.specials, { if (!readOnly) onChange(rules.copy(specials = it)) }, hint = tr("une fois par partie, coûte un quart de l'endurance"))
        val follows = rules.duelMode == MatchRules.defaultDuelMode
        Segmented(DuelMode.entries.map(::duelLabel), rules.duelMode.ordinal, { if (!readOnly) onChange(rules.copy(duelMode = DuelMode.entries[it])) },
            label = if (follows) tr("Duels · suivent les Paramètres") else tr("Duels"), height = 32.dp, textSize = 11f, enabled = !readOnly)
        Hint(rulesSummary(rules), color = Holo.dim)
    }
}

/** Résumé traduit des règles (comme MatchRules.summary). */
fun rulesSummary(r: MatchRules): String {
    val parts = mutableListOf<String>()
    fun percent(label: String, value: Double) { if (kotlin.math.abs(value - 1.0) >= 1e-6) parts += "$label ${(value * 100).roundToInt()} %" }
    percent(tr("Vitesse"), r.speed); percent(tr("Rebonds"), r.knockback); percent(tr("Usure"), r.wear)
    percent(tr("Dégâts"), r.damage); percent(tr("Dash"), r.dashPower)
    if (kotlin.math.abs(r.maxEndurance - 100.0) >= 1e-6) parts += "${tr("Endurance")} ${r.maxEndurance.roundToInt()}"
    if (r.timeLimit > 0.0) parts += tr("Limite %1 s", r.timeLimit.roundToInt())
    if (r.steering <= 0.0) parts += tr("Sans pilotage") else percent(tr("Pilotage"), r.steering)
    if (!r.specials) parts += tr("Sans coups spéciaux")
    if (r.duelMode != MatchRules.defaultDuelMode) parts += tr("Duels : %1", duelLabel(r.duelMode))
    return if (parts.isEmpty()) tr("Règles normales") else parts.joinToString(" · ")
}

/** Tiroir « Personnalisation » (règles du combat). */
@Composable
fun RulesDrawer(visible: Boolean, rules: MatchRules, onChange: (MatchRules) -> Unit, onClose: () -> Unit, readOnly: Boolean = false) {
    SideDrawer(visible, DrawerSide.End, tr("Personnalisation"), onClose, width = 440.dp, sub = if (readOnly) tr("règles de l'hôte") else tr("règles du combat"),
        footer = if (readOnly) null else ({
            HoloButton(tr("Rétablir"), { onChange(MatchRules()) }, Modifier.width(150.dp), BtnVariant.Ghost, height = 44.dp, textSize = 12f)
            HoloButton(tr("Valider"), onClose, Modifier.weight(1f), BtnVariant.Primary, HoloIcon.Check, height = 44.dp)
        })) { Box(Modifier.weight(1f)) { RulesForm(rules, onChange, readOnly) } }
}


/** Nom d'un tour (« Demi-finales », « Journée 3/7 »). */
fun roundName(t: Tournament, round: Int = t.currentRound): String =
    if (t.settings.format == TournamentFormat.League) tr("Journée %1/%2", round + 1, t.roundCount)
    else when (t.roundCount - round) {
        1 -> tr("Finale"); 2 -> tr("Demi-finales"); 3 -> tr("Quarts de finale"); 4 -> tr("Huitièmes de finale"); else -> tr("Seizièmes de finale")
    }

/** Avancement court (puce « Reprendre ») : « tour 2/3 » ou « journée 4/7 ». */
fun roundLabel(t: Tournament): String =
    if (t.settings.format == TournamentFormat.League) roundName(t).lowercase() else tr("tour %1/%2", t.currentRound + 1, t.roundCount)
