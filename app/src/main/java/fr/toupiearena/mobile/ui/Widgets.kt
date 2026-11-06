package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Stadium
import kotlin.math.roundToInt

/** Choix dans une liste : un champ qui ouvre une fenêtre avec les options (pratique au doigt). */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: Int, text: (T) -> String, onSelect: (Int) -> Unit,
               modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        if (label.isNotEmpty()) Text(label, color = Palette.muted, fontSize = 12.sp)
        Text(options.getOrNull(selected)?.let(text) ?: "—",
            Modifier.fillMaxWidth().border(1.dp, Palette.border, RoundedCornerShape(8.dp))
                .background(Palette.backgroundDeep, RoundedCornerShape(8.dp)).clickable { open = true }.padding(10.dp),
            color = Palette.text, maxLines = 1)
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false }, confirmButton = {},
        title = { Text(label.ifEmpty { "Choisir" }) },
        text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                itemsIndexed(options) { i, option ->
                    Text(text(option), Modifier.fillMaxWidth().clickable { onSelect(i); open = false }
                        .background(if (i == selected) Palette.accentDark else Color.Transparent).padding(10.dp),
                        color = Palette.text)
                }
            }
        },
    )
}

fun topLabel(top: Top) = "${top.name}  [${top.type.label}]"

/** Aperçu d'une toupie : dessin, type, sens, coup spécial et barres de stats. */
@Composable
fun TopPreview(top: Top?, modifier: Modifier = Modifier, mystery: String = "") {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(72.dp)) {
            if (top != null) drawTop(top, Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, 0.4)
            else drawCircle(Palette.panelAlt, size.minDimension * 0.42f)
        }
        HSpace(12)
        if (top == null) {
            Text(mystery, color = Palette.muted)
            return
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val move = SpecialMove.find(top.special)
            Text("${top.type.label} · ${top.spin.label.lowercase()} · ✦ ${move?.name ?: "—"}",
                color = typeColor(top.type), fontSize = 12.sp)
            StatBar("Attaque", top.stats.attack, Color(0xFFE2433C))
            StatBar("Défense", top.stats.defense, Color(0xFF3478DC))
            StatBar("Endurance", top.stats.endurance, Color(0xFF2EAA64))
        }
    }
}

@Composable
fun StatBar(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(76.dp), color = Palette.text, fontSize = 12.sp)
        Box(Modifier.weight(1f).height(8.dp).background(Palette.panelAlt, RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(value.toFloat() / PartCatalog.maxStatValue).height(8.dp).background(color, RoundedCornerShape(4.dp)))
        }
        Text(" $value", Modifier.width(28.dp), color = Palette.text, fontSize = 12.sp)
    }
}

@Composable
fun StadiumPicker(selected: String, onSelect: (String) -> Unit, enabled: Boolean = true) {
    val all = Stadium.all
    val index = all.indexOfFirst { it.id == selected }.coerceAtLeast(0)
    if (enabled) Picker("Stadium", all, index, { it.name }, { onSelect(all[it].id) })
    else Text("Stadium : ${all[index].name}", color = Palette.text)
    Hint(all[index].description)
}

/** Section « Personnalisation » : réglages de la partie (lecture seule pour les clients d'un lobby). */
@Composable
fun RulesEditor(rules: MatchRules, onChange: (MatchRules) -> Unit, readOnly: Boolean = false) {
    @Composable
    fun percent(label: String, value: Double, set: (Double) -> MatchRules) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.width(150.dp), color = Palette.text, fontSize = 13.sp)
            Slider(value.toFloat(), { onChange(set((it * 20).roundToInt() / 20.0)) }, Modifier.weight(1f),
                enabled = !readOnly, valueRange = 0f..3f)
            Text("${(value * 100).roundToInt()} %", Modifier.width(56.dp), color = Palette.text, fontSize = 13.sp)
        }
    }
    percent("Vitesse", rules.speed) { rules.copy(speed = maxOf(0.25, it)) }
    percent("Rebonds après un choc", rules.knockback) { rules.copy(knockback = it) }
    percent("Usure", rules.wear) { rules.copy(wear = it) }
    percent("Dégâts", rules.damage) { rules.copy(damage = it) }
    percent("Puissance des dashs", rules.dashPower) { rules.copy(dashPower = it) }
    percent("Pilotage", rules.steering) { rules.copy(steering = it) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Endurance max", Modifier.width(150.dp), color = Palette.text, fontSize = 13.sp)
        Slider(rules.maxEndurance.toFloat(), { onChange(rules.copy(maxEndurance = (it / 10).roundToInt() * 10.0)) },
            Modifier.weight(1f), enabled = !readOnly, valueRange = 10f..300f)
        Text("${rules.maxEndurance.roundToInt()}", Modifier.width(56.dp), color = Palette.text, fontSize = 13.sp)
    }
    val limits = listOf(0.0, 30.0, 60.0, 90.0, 120.0, 180.0)
    if (readOnly) Text("Temps maximal : ${if (rules.timeLimit > 0) "${rules.timeLimit.roundToInt()} s" else "aucun"}", color = Palette.text)
    else Picker("Temps maximal", limits, limits.indexOfFirst { it == rules.timeLimit }.coerceAtLeast(0),
        { if (it > 0) "${it.roundToInt()} s" else "Aucun" }, { onChange(rules.copy(timeLimit = limits[it])) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(rules.specials, { onChange(rules.copy(specials = it)) }, enabled = !readOnly)
        Text("Coups spéciaux (une fois par partie, coûte un quart de l'endurance)", color = Palette.text, fontSize = 13.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(rules.summary(), Modifier.weight(1f), color = Palette.muted, fontSize = 12.sp)
        if (!readOnly) SecondaryButton("Réinitialiser", { onChange(MatchRules()) })
    }
}

/** Fenêtre de personnalisation (bouton « Personnalisation… »). */
@Composable
fun RulesDialog(rules: MatchRules, onChange: (MatchRules) -> Unit, readOnly: Boolean, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose, confirmButton = { AccentButton("Fermer", onClose) },
        title = { Text("Personnalisation", fontWeight = FontWeight.Bold) },
        text = { LazyColumn(Modifier.heightIn(max = 320.dp)) { item { Column { RulesEditor(rules, onChange, readOnly) } } } },
    )
}

