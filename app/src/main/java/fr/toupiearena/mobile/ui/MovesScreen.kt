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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.ui.holo.Badge
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.StatRow
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.skewPath
import fr.toupiearena.mobile.ui.holo.tr
import fr.toupiearena.mobile.core.TopType

/** Point fort, point faible et jauges de chaque coup (planche des coups spéciaux du PC). */
private class Sheet(val plus: String, val minus: String, val damage: Int, val knockback: Int, val range: Int)
private val SHEETS = mapOf(
    "onde" to Sheet("Gros recul : éjecte une cible déjà au bord", "Dégâts faibles, portée courte", 6, 5, 1),
    "lance" to Sheet("Les plus lourds dégâts (24)", "Aucun effet, presque aucun recul, esquivable de loin", 24, 1, 4),
    "givre" to Sheet("3 s gelée : ralentie, sans pilotage, usée ×3", "Dégâts modérés", 12, 2, 4),
    "vortex" to Sheet("Aspire tout le stadium vers le centre, sans dash possible", "Dégâts modérés, éloigne les cibles du bord", 14, 4, 4),
    "distorsion" to Sheet("Téléporte la cible près du bord, lancée vers l’extérieur", "Lente, facile à esquiver", 8, 4, 3),
    "siphon" to Sheet("Rend 60 % de l’endurance volée", "Dégâts modérés, peu de recul", 11, 1, 4),
    "seisme" to Sheet("Blesse et bloque dash + coup spécial 6 s", "Portée très courte", 13, 2, 1),
)

fun moveColor(move: SpecialMove) = Color(0xFF000000 or move.color)

/** Page des coups spéciaux : la liste à gauche, la fiche du coup choisi à droite. */
@Composable
fun MovesScreen(app: AppController, back: Screen) {
    var selected by rememberSaveable { mutableStateOf(SpecialMove.all.first().id) }
    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Coups spéciaux"), { app.screen = back }, sub = tr("une fois par partie · coûte ¼ de l’endurance"))
            Row(Modifier.fillMaxSize().padding(horizontal = EdgeMargin, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LazyColumn(Modifier.width(250.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SpecialMove.all) { m ->
                        val on = m.id == selected
                        HoloCard(Modifier.fillMaxWidth().height(42.dp), selected = on, accent = moveColor(m), onClick = { selected = m.id }) {
                            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                EmblemBox(m, 32, on)
                                Text(tr(m.name).uppercase(), style = displayStyle(11.5.sp, if (on) Holo.text else Holo.muted), modifier = Modifier.weight(1f), maxLines = 1)
                                Text(if (m.hitsEveryone) tr("anneau") else tr("vague"), style = bodyStyle(11.sp, Holo.dim))
                            }
                        }
                    }
                }
                MoveSheetPanel(SpecialMove.find(selected) ?: SpecialMove.all.first(), Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun EmblemBox(move: SpecialMove, size: Int, on: Boolean = true) {
    Canvas(Modifier.size(size.dp)) {
        drawRect(Holo.field); drawRect(if (on) moveColor(move) else Holo.line, style = Stroke(1.3.dp.toPx()))
        paintEmblem(move.id, center, this.size.minDimension * 0.3f, if (on) Color.White else Holo.dim)
    }
}

@Composable
private fun MoveSheetPanel(move: SpecialMove, modifier: Modifier) {
    val sheet = SHEETS[move.id]
    val col = moveColor(move)
    val bolt = PartCatalog.parts(PartKind.Bolt).firstOrNull { it.special == move.id }
    Panel(modifier, accent = col) {
        Box(Modifier.fillMaxWidth().weight(1f).padding(bottom = 10.dp)) { MoveScene(move, Modifier.fillMaxSize()) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmblemBox(move, 46)
            Column(Modifier.weight(1f)) {
                if (bolt != null) FieldLabel(tr(bolt.name))
                Text(tr(move.name).uppercase(), style = displayStyle(19.sp))
            }
            Badge(if (move.hitsEveryone) tr("Anneau") else tr("Vague"), col, height = 22.dp, textSize = 10f)
        }
        if (sheet != null) {
            Spacer(Modifier.height(8.dp))
            Bullet(tr(sheet.plus), Holo.green, Holo.text); Bullet(tr(sheet.minus), Holo.danger, Holo.muted)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatRow(tr("Dégâts"), sheet.damage * 8 / 24, Holo.type(TopType.Attack), Modifier.weight(1f), 8, "${sheet.damage}")
                StatRow(tr("Recul"), sheet.knockback * 8 / 5, Holo.cyan, Modifier.weight(1f), 8, "${sheet.knockback}")
                StatRow(tr("Portée"), sheet.range * 8 / 4, Holo.violet, Modifier.weight(1f), 8, "${sheet.range}")
            }
        }
        Text(tr("Coûte ¼ de l’endurance du moment · une fois par partie"), style = bodyStyle(11.5.sp, Holo.dim), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Bullet(text: String, mark: Color, color: Color) = Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Canvas(Modifier.size(8.dp, 12.dp)) { drawPath(skewPath(0f, 0f, size.width, size.height, 3.dp.toPx()), mark) }
    Text(text, style = bodyStyle(13.sp, color), modifier = Modifier.padding(start = 8.dp))
}

/** Illustration de l'effet : le lanceur, la cible et le coup en vol. */
@Composable
private fun MoveScene(move: SpecialMove, modifier: Modifier) {
    val caster = PartCatalog.presets.firstOrNull { it.special == move.id } ?: PartCatalog.presets.first()
    val target = PartCatalog.presets[(PartCatalog.presets.indexOf(caster) + 3) % PartCatalog.presets.size]
    Canvas(modifier.clipToBounds()) { // Rien ne déborde du cadre gris.
        drawRect(Holo.field)
        val y = size.height / 2
        // Anneau : toupies plus petites et cible proche, pour que l'onde tienne dans la hauteur du cadre.
        val r = size.height * (if (move.hitsEveryone) 0.09f else 0.16f)
        val a = Offset(size.width * (if (move.hitsEveryone) 0.4f else 0.2f), y)
        val b = if (move.hitsEveryone) Offset(a.x + size.height * 0.42f, y) else Offset(size.width * 0.8f, y)
        paintMoveScene(move, a, b, r)
        paintTop(target, b, r, 1f)
        paintTop(caster, a, r, 0.4f)
    }
}
