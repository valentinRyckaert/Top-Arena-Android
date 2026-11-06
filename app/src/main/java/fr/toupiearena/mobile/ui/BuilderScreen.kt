package fr.toupiearena.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.toupiearena.mobile.core.Part
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.game.SpecialMove

private fun partLabel(part: Part): String {
    if (part.kind == PartKind.Bolt) return "${part.name} — coup spécial : ${SpecialMove.find(part.special)?.name ?: part.special}"
    val label = "${part.name} (${part.stats.attack} / ${part.stats.defense} / ${part.stats.endurance})"
    return if (part.kind == PartKind.Wheel) "$label — ${part.spin.label.lowercase()}" else label
}

/** Atelier : assembler, enregistrer et supprimer ses toupies. */
@Composable
fun BuilderScreen(app: AppController) {
    val repo = app.repository
    var tops by remember { mutableStateOf(repo.customTops.toList()) }
    var editing by remember { mutableStateOf(PartCatalog.presets[0].copy(name = "")) }
    var status by remember { mutableStateOf("") }

    fun save() {
        if (editing.name.isBlank()) { status = "Donnez un nom à votre toupie."; return }
        if (PartCatalog.presets.any { it.name.equals(editing.name, ignoreCase = true) }) {
            status = "Ce nom est celui d'une toupie prédéfinie."
            return
        }
        repo.upsert(editing)
        status = if (repo.save()) "« ${editing.name} » enregistrée." else "Impossible d'enregistrer les toupies."
        tops = repo.customTops.toList()
        app.bump()
    }

    Page("Assembler une toupie", { app.screen = Screen.Menu }, actions = {
        SecondaryButton("Nouvelle", { editing = PartCatalog.presets[0].copy(name = ""); status = "" })
        HSpace()
        AccentButton("Enregistrer", ::save)
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Section("Mes toupies", Modifier.width(230.dp)) {
                if (tops.isEmpty()) Hint("Aucune toupie enregistrée pour l'instant.")
                tops.forEach { top ->
                    Row(Modifier.fillMaxWidth().background(if (top.name == editing.name) Palette.accentDark else Color.Transparent,
                        RoundedCornerShape(6.dp))) {
                        Text(topLabel(top), Modifier.weight(1f).clickable { editing = top; status = "" }.padding(6.dp), color = Palette.text)
                        Text("✕", Modifier.clickable {
                            repo.remove(top.name)
                            repo.save()
                            tops = repo.customTops.toList()
                            app.bump()
                        }.padding(6.dp), color = Palette.danger)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint("Une toupie = un boulon + une roue métallique + une partie intermédiaire + une pointe. Ses stats " +
                    "sont la somme de celles des pièces ; la roue fixe le sens de rotation, le boulon (sans stats) donne le coup spécial.")
                OutlinedTextField(editing.name, { editing = editing.copy(name = it.take(24)) }, Modifier.fillMaxWidth(),
                    label = { Text("Nom de la toupie") }, singleLine = true)
                for (kind in listOf(PartKind.Bolt, PartKind.Wheel, PartKind.Middle, PartKind.Tip)) {
                    val parts = PartCatalog.parts(kind)
                    Picker(kind.label, parts, parts.indexOfFirst { it.id == editing.part(kind).id }, ::partLabel,
                        { editing = editing.withPart(parts[it]) })
                }
                Section("Aperçu") { TopPreview(editing) }
                if (status.isNotEmpty()) Text(status, color = Palette.accent)
            }
        }
    }
}
