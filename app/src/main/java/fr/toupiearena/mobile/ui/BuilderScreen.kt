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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.Part
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.core.SpinDirection
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.DrawerSide
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloField
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.IconBtn
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.Stats3
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.tr

private val SLOTS = listOf(PartKind.Bolt to Holo.cyan, PartKind.Wheel to Color(0xFFFF3D6E), PartKind.Middle to Holo.warn, PartKind.Tip to Holo.magenta)
private fun statsText(p: Part) = "A${p.stats.attack} · D${p.stats.defense} · E${p.stats.endurance}"
private fun spinText(s: SpinDirection) = tr(s.label).lowercase()
private fun partSub(p: Part) = when (p.kind) {
    PartKind.Bolt -> "${specialName(p.special)} · ${if (SpecialMove.find(p.special)?.hitsEveryone == true) tr("anneau") else tr("vague")}"
    PartKind.Wheel -> "${statsText(p)} · ${spinText(p.spin)}"
    else -> statsText(p)
}

/** Atelier : établi au centre ; vos toupies, les pièces et les statistiques dans des tiroirs. */
@Composable
fun BuilderScreen(app: AppController) {
    val repo = app.repository
    var editing by remember { mutableStateOf(PartCatalog.presets[0].copy(name = "")) }
    var status by remember { mutableStateOf("") }
    var mine by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(false) }
    var partKind by remember { mutableStateOf<PartKind?>(null) }
    var version by remember { mutableStateOf(0) } // Liste des toupies à relire après enregistrement.

    fun save() {
        val name = editing.name.trim()
        status = when {
            name.isEmpty() -> tr("Donnez un nom à votre toupie.")
            PartCatalog.presets.any { it.name.equals(name, ignoreCase = true) } -> tr("Ce nom est celui d'une toupie prédéfinie.")
            else -> { repo.upsert(editing.copy(name = name)); if (repo.save()) tr("« %1 » enregistrée.", name).also { version++; app.bump() } else tr("Impossible d'enregistrer les toupies.") }
        }
    }
    fun cycle(kind: PartKind, step: Int) {
        val parts = PartCatalog.parts(kind); val i = parts.indexOfFirst { it.id == editing.part(kind).id }
        editing = editing.withPart(parts[(i + step + parts.size) % parts.size]); status = ""
    }

    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Atelier"), { app.screen = Screen.Menu }, sub = tr("Assembler une toupie")) {
                HoloButton(tr("Mes toupies"), { mine = true }, Modifier.width(180.dp), height = 36.dp, icon = HoloIcon.Build, textSize = 12f)
                IconBtn(HoloIcon.Balance, { stats = true })
                HoloButton(tr("Enregistrer"), ::save, Modifier.width(170.dp), BtnVariant.Primary, height = 36.dp, icon = HoloIcon.Save, textSize = 12f)
            }
            Row(Modifier.fillMaxSize().padding(horizontal = EdgeMargin, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.width(420.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HoloField(editing.name, { editing = editing.copy(name = it.take(24)); status = "" }, placeholder = tr("Nom de la toupie"))
                    for (row in SLOTS.chunked(2)) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((kind, col) in row) PartCard(editing.part(kind), col, Modifier.weight(1f).fillMaxHeight(), { partKind = kind }, { cycle(kind, -1) }, { cycle(kind, 1) })
                    }
                    Text(status.ifEmpty { tr("Toucher une pièce pour voir toutes les autres.") }, style = bodyStyle(12.5.sp, if (status.isEmpty()) Holo.dim else Holo.green), maxLines = 1)
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { SpinningPreview(editing) }
                Column(Modifier.width(180.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(tr("Type obtenu"), Modifier.padding(top = 26.dp))
                    Text(tr(editing.type.label).uppercase(), style = displayStyle(21.sp, Holo.type(editing.type)))
                    Stats3(editing.stats.attack, editing.stats.defense, editing.stats.endurance, gap = 8.dp)
                    Spacer(Modifier.weight(1f))
                    SpecialCard(editing) { app.screen = Screen.Moves(Screen.Builder) }
                }
            }
        }
        val tops = remember(version) { repo.allTops }
        SideDrawer(mine, DrawerSide.Start, tr("Mes toupies"), { mine = false }, width = 410.dp, sub = tr("%1 prédéfinies · %2 à vous", PartCatalog.presets.size, repo.customTops.size),
            footer = {
                HoloButton(tr("Nouvelle"), { editing = PartCatalog.presets[0].copy(name = ""); status = ""; mine = false }, Modifier.weight(1f), icon = HoloIcon.Plus, height = 44.dp, textSize = 12f)
                val custom = repo.customTops.any { it.name == editing.name }
                HoloButton(tr("Supprimer"), { repo.remove(editing.name); repo.save(); version++; app.bump(); editing = PartCatalog.presets[0].copy(name = "") },
                    Modifier.weight(1f), BtnVariant.Danger, HoloIcon.Trash, height = 44.dp, enabled = custom, textSize = 12f)
            }) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tops) { t ->
                    val custom = PartCatalog.presets.none { it.name == t.name }
                    TopRow(t, selected = t.name == editing.name, height = 44.dp, star = custom) { editing = t; status = ""; mine = false }
                }
            }
        }
        val kind = partKind
        SideDrawer(kind != null, DrawerSide.End, kind?.let { tr(it.label) } ?: "", { partKind = null }, width = 500.dp,
            sub = kind?.let { tr("%1 pièces", PartCatalog.parts(it).size) + if (it == PartKind.Wheel) " · " + tr("fixe le sens de rotation") else "" }) {
            if (kind != null) LazyVerticalGrid(GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PartCatalog.parts(kind)) { p ->
                    val on = p.id == editing.part(kind).id
                    HoloCard(Modifier.fillMaxWidth().height(52.dp), selected = on, accent = SLOTS.first { it.first == kind }.second, onClick = { editing = editing.withPart(p); status = "" }) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PartView(p, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(tr(p.name).uppercase(), style = displayStyle(11.sp), maxLines = 1)
                                Text(partSub(p), style = bodyStyle(11.sp, Holo.muted), maxLines = 1)
                            }
                            if (on) Icon(HoloIcon.Check, Holo.green, 12.dp)
                        }
                    }
                }
            }
        }
        SideDrawer(stats, DrawerSide.End, tr("Statistiques"), { stats = false }, width = 420.dp, sub = tr("total %1 points", editing.stats.attack + editing.stats.defense + editing.stats.endurance)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Radar(editing, Modifier.size(190.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    FieldLabel(tr("Type obtenu"))
                    Text(tr(editing.type.label).uppercase(), style = displayStyle(19.sp, Holo.type(editing.type)))
                    Hint(typeHint(editing.type), Modifier.padding(top = 4.dp))
                }
            }
            Stats3(editing.stats.attack, editing.stats.defense, editing.stats.endurance, Modifier.padding(top = 10.dp))
        }
    }
}

private fun typeHint(type: TopType) = when (type) {
    TopType.Attack -> tr("Bat l'Endurance, craint la Défense.")
    TopType.Defense -> tr("Bat l'Attaque, craint l'Endurance.")
    TopType.Endurance -> tr("Bat la Défense, craint l'Attaque.")
    TopType.Balance -> tr("Sans point fort ni faiblesse marqués.")
}

@Composable
private fun PartCard(part: Part, col: Color, modifier: Modifier, onOpen: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    HoloCard(modifier, onClick = onOpen) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row { FieldLabel(tr(part.kind.label), Modifier.weight(1f), col); Text(part.id, style = displayStyle(10.sp, Holo.dim, 0.05f)) }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PartView(part, 46.dp)
                Column(Modifier.weight(1f)) {
                    Text(tr(part.name).uppercase(), style = displayStyle(12.sp), maxLines = 1)
                    Text(partSub(part), style = bodyStyle(11.sp, Holo.muted), maxLines = 1)
                }
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconBtn(HoloIcon.Back, onPrev, width = 36.dp, height = 28.dp, iconSize = 14.dp)
                IconBtn(HoloIcon.Next, onNext, width = 36.dp, height = 28.dp, iconSize = 14.dp)
            }
        }
    }
}

@Composable
private fun SpecialCard(top: Top, onAll: () -> Unit) {
    val move = SpecialMove.find(top.special)
    HoloCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.size(40.dp)) {
                    drawRect(Holo.field); move?.let { drawRect(moveColor(it), style = Stroke(1.3.dp.toPx())) }
                    paintEmblem(top.special, center, size.minDimension * 0.3f, Color.White)
                }
                Column {
                    FieldLabel(tr("Coup spécial"))
                    Text(specialName(top.special).uppercase(), style = displayStyle(12.sp), maxLines = 1)
                    Text(if (move?.hitsEveryone == true) tr("anneau") else tr("vague"), style = bodyStyle(11.sp, Holo.muted))
                }
            }
            HoloButton(tr("Tous les coups"), onAll, Modifier.fillMaxWidth().padding(top = 8.dp), icon = HoloIcon.Help, height = 32.dp, textSize = 10.5f)
        }
    }
}

/** Aperçu qui tourne. */
@Composable
private fun SpinningPreview(top: Top) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(top.spin) {
        var last = 0L
        while (true) withFrameNanos { t -> if (last != 0L) angle += top.spin.sign * (t - last) / 1e9f * 2.4f; last = t }
    }
    Canvas(Modifier.size(250.dp).graphicsLayer()) { // Couche à part : seul l'aperçu est redessiné.
        val r = size.minDimension / 2
        drawCircle(Holo.violet.copy(alpha = 0.6f), r * 0.98f, style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 18f))))
        drawArc(Holo.type(top.type).copy(alpha = 0.6f), 160f, 110f, false, Offset(center.x - r, center.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(2.5.dp.toPx()))
        paintTop(top, center, r * 0.7f, angle)
    }
}

/** Radar attaque / défense / endurance. */
@Composable
private fun Radar(top: Top, modifier: Modifier) = Canvas(modifier) {
    val c = center; val r = size.minDimension / 2 * 0.72f
    val ang = listOf(-90.0, 30.0, 150.0).map { Math.toRadians(it) }
    fun pt(i: Int, k: Float) = Offset(c.x + kotlin.math.cos(ang[i]).toFloat() * r * k, c.y + kotlin.math.sin(ang[i]).toFloat() * r * k)
    for (k in listOf(1f, 0.66f, 0.33f)) drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(pt(0, k).x, pt(0, k).y); lineTo(pt(1, k).x, pt(1, k).y); lineTo(pt(2, k).x, pt(2, k).y); close() }, Holo.line, style = Stroke(1f))
    val v = listOf(top.stats.attack, top.stats.defense, top.stats.endurance).map { it / 15f }
    val shape = androidx.compose.ui.graphics.Path().apply { moveTo(pt(0, v[0]).x, pt(0, v[0]).y); lineTo(pt(1, v[1]).x, pt(1, v[1]).y); lineTo(pt(2, v[2]).x, pt(2, v[2]).y); close() }
    val col = Holo.type(top.type)
    drawPath(shape, col.copy(alpha = 0.3f)); drawPath(shape, col, style = Stroke(2.dp.toPx()))
    for (i in 0..2) drawCircle(Color.White, 3.dp.toPx(), pt(i, v[i]))
}
