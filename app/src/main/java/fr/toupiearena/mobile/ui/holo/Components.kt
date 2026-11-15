package fr.toupiearena.mobile.ui.holo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.TopType

/** Marge latérale des écrans (encoche de la caméra en paysage). */
val EdgeMargin = 24.dp

/** Écran plein, fond holographique. */
@Composable
fun HoloScreen(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) =
    Box(modifier.fillMaxSize()) {
        // Couche à part : le fond (dégradés, lignes de balayage) n'est pas redessiné avec les éléments animés.
        Spacer(Modifier.matchParentSize().graphicsLayer().drawBehind { holoBackground() })
        content()
    }

/** « /// » décoratif. */
@Composable
fun Slashes(color: Color = Holo.green, height: Dp = 14.dp, count: Int = 3) =
    Canvas(Modifier.size(width = height * (0.55f + (count - 1) * 0.5f) + 4.dp, height = height)) {
        val h = size.height; val gap = h * 0.5f
        repeat(count) { i -> drawPath(skewPath(i * gap, 0f, 4.dp.toPx() + h * 0.35f, h, h * 0.35f), color.copy(alpha = 1f - i * 0.22f)) }
    }

// ---------------------------------------------------------------- barre du haut

@Composable
fun TopBar(
    title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, sub: String? = null,
    backIcon: HoloIcon = HoloIcon.Back, actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(52.dp).padding(horizontal = EdgeMargin).drawBehind {
            drawLine(Holo.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            drawLine(Holo.cyan, Offset(0f, size.height), Offset(180.dp.toPx(), size.height), 2.dp.toPx())
        },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) IconBtn(backIcon, onBack)
        Slashes(height = 16.dp)
        Text(title.uppercase(), style = displayStyle(18.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (sub != null) Text(sub, style = bodyStyle(13.sp, Holo.muted, androidx.compose.ui.text.font.FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** Bouton carré à icône (retour, menu, réglages…). */
@Composable
fun IconBtn(icon: HoloIcon, onClick: () -> Unit, modifier: Modifier = Modifier, on: Boolean = false, badge: Int? = null,
            color: Color = Holo.cyan, width: Dp = 44.dp, height: Dp = 40.dp, iconSize: Dp = 20.dp, enabled: Boolean = true) {
    val shape = Chamfer(10.dp)
    Box(modifier.size(width, height)) {
        Box(Modifier.fillMaxSize().clip(shape).background(if (on) Holo.green.copy(alpha = 0.16f) else Holo.field.copy(alpha = 0.85f))
            .border(1.2.dp, if (on) Holo.green else Holo.lineHi, shape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, if (!enabled) Holo.dim else if (on) Holo.green else color, iconSize)
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(16.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(Holo.magenta), contentAlignment = Alignment.Center) { Text("$badge", style = displayStyle(9.sp, spacing = 0f)) }
    }
}

// ---------------------------------------------------------------- boutons

enum class BtnVariant(val fill: List<Color>?, val text: Color, val stroke: Color) {
    Primary(listOf(Holo.green, Holo.lime), Holo.ink, Color(0xFFB9FFD6)),
    Secondary(listOf(Color(0xFF231C6B), Color(0xFF151049)), Holo.text, Holo.lineHi),
    Ghost(null, Holo.muted, Holo.line),
    Danger(listOf(Color(0xFF3A0F2A), Color(0xFF220820)), Color(0xFFFFC3D2), Holo.danger),
    Holo_(listOf(Holo.blue, Holo.violet, Holo.magenta), Color.White, Color(0xFFC9B8FF)),
}

@Composable
fun HoloButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, variant: BtnVariant = BtnVariant.Secondary,
    icon: HoloIcon? = null, height: Dp = 46.dp, enabled: Boolean = true, alignStart: Boolean = false, textSize: Float = if (height >= 48.dp) 15f else 13f,
) {
    val shape = Chamfer(height * 0.3f)
    val glow = variant == BtnVariant.Primary || variant == BtnVariant.Holo_
    Box(
        modifier.height(height).drawBehind {
            if (glow && enabled) {
                val o = shape.createOutline(size, layoutDirection, this)
                translate(0f, 5.dp.toPx()) { drawOutline(o, (if (variant == BtnVariant.Primary) Holo.green else Holo.violet).copy(alpha = 0.3f)) }
            }
        }.clip(shape)
            .background(variant.fill?.let { Brush.linearGradient(it) } ?: SolidColor(Holo.void.copy(alpha = 0.4f)), alpha = if (enabled) 1f else 0.4f)
            .border(1.3.dp, variant.stroke.copy(alpha = if (enabled) 1f else 0.4f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center,
    ) {
        if (variant == BtnVariant.Primary) Box(Modifier.fillMaxWidth().height(height * 0.4f).align(Alignment.TopCenter).padding(3.dp).background(Color.White.copy(alpha = 0.16f)))
        Row(Modifier.padding(horizontal = height * 0.3f + 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, if (variant == BtnVariant.Primary) variant.text else Holo.cyan, (textSize + 3).dp)
            Text(text.uppercase(), style = displayStyle(textSize.sp, variant.text.copy(alpha = if (enabled) 1f else 0.5f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- cadres et cartes

/** Cadre de contenu : coins coupés, accents de couleur aux angles, titre en petites capitales. */
@Composable
fun Panel(
    modifier: Modifier = Modifier, title: String? = null, index: String? = null, accent: Color = Holo.cyan, selected: Boolean = false,
    onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    val c = 14.dp
    val shape = Chamfer(c)
    Column(
        modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xF01A1556), Color(0xF00D0A2E))))
            .border(if (selected) 1.8.dp else 1.1.dp, if (selected) accent else Holo.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                val cp = c.toPx(); val sw = 2.4.dp.toPx()
                drawPath(Path().apply { moveTo(0f, cp + 16.dp.toPx()); lineTo(0f, cp); lineTo(cp, 0f); lineTo(cp + 34.dp.toPx(), 0f) }, accent, style = Stroke(sw))
                drawPath(Path().apply {
                    moveTo(size.width, size.height - cp - 16.dp.toPx()); lineTo(size.width, size.height - cp); lineTo(size.width - cp, size.height); lineTo(size.width - cp - 34.dp.toPx(), size.height)
                }, Holo.violet, style = Stroke(sw))
            }.padding(start = 14.dp, end = 12.dp, top = if (title != null) 8.dp else 12.dp, bottom = 12.dp),
    ) {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp).drawBehind {
                drawLine(Holo.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title.uppercase(), style = displayStyle(11.5.sp), modifier = Modifier.weight(1f), maxLines = 1)
                if (index != null) Text(index.uppercase(), style = displayStyle(10.sp, accent))
            }
        }
        content()
    }
}

/** Carte simple (ligne de liste, case à cocher visuelle). */
@Composable
fun HoloCard(
    modifier: Modifier = Modifier, selected: Boolean = false, accent: Color = Holo.green, fill: Color? = null,
    onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit,
) {
    val shape = Chamfer(10.dp)
    Box(modifier.clip(shape).background(fill ?: if (selected) Color(0xFF1E2A6A) else Holo.card)
        .border(if (selected) 1.8.dp else 1.dp, if (selected) accent else Holo.line, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), content = content)
}

// ---------------------------------------------------------------- choix et réglages

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, color: Color = Holo.muted) =
    Text(text.uppercase(), style = labelStyle(color), modifier = modifier, maxLines = 1)

/** Contrôle segmenté (options inclinées, l'option choisie en vert). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, label: String? = null,
              height: Dp = 36.dp, textSize: Float = 12f, enabled: Boolean = true) {
    Column(modifier) {
        if (label != null) FieldLabel(label, Modifier.padding(bottom = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            options.forEachIndexed { i, o ->
                val on = i == selected; val shape = Skew(9.dp)
                Box(Modifier.weight(1f).height(height).clip(shape)
                    .background(if (on) Brush.horizontalGradient(listOf(Holo.green, Holo.lime)) else SolidColor(Holo.field))
                    .border(1.dp, if (on) Holo.green else Holo.line, shape).clickable(enabled = enabled) { onSelect(i) },
                    contentAlignment = Alignment.Center) {
                    Text(o.uppercase(), style = displayStyle(textSize.sp, if (on) Holo.ink else if (enabled) Holo.muted else Holo.dim), maxLines = 1)
                }
            }
        }
    }
}

/** Curseur : libellé, valeur à droite, piste inclinée, poignée à coins coupés. */
@Composable
fun HoloSlider(value: Float, onChange: (Float) -> Unit, label: String, valueText: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            FieldLabel(label, Modifier.weight(1f)); Text(valueText, style = displayStyle(12.5.sp, Holo.cyan, 0.05f))
        }
        Canvas(Modifier.fillMaxWidth().height(28.dp).pointerInput(enabled) {
            if (!enabled) return@pointerInput
            fun set(x: Float) = onChange((x / size.width).coerceIn(0f, 1f))
            detectTapGestures { set(it.x) }
        }.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectHorizontalDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) } // Le vertical fait défiler.
        }) {
            val y = size.height / 2; val th = 6.dp.toPx(); val kx = size.width * value
            drawPath(skewPath(0f, y - th / 2, size.width, th, 3.dp.toPx()), Holo.field)
            drawPath(skewPath(0f, y - th / 2, size.width, th, 3.dp.toPx()), Holo.line, style = Stroke(1f))
            drawPath(skewPath(0f, y - th / 2, kx, th, 3.dp.toPx()), Brush.horizontalGradient(listOf(Holo.blue, Holo.cyan)))
            val kw = 18.dp.toPx(); val kh = 22.dp.toPx(); val k = Path().apply {
                val x0 = (kx - kw / 2).coerceIn(0f, size.width - kw); val c = 5.dp.toPx()
                moveTo(x0 + c, y - kh / 2); lineTo(x0 + kw, y - kh / 2); lineTo(x0 + kw, y + kh / 2 - c); lineTo(x0 + kw - c, y + kh / 2); lineTo(x0, y + kh / 2); lineTo(x0, y - kh / 2 + c); close()
            }
            drawPath(k, if (enabled) Holo.text else Holo.dim); drawPath(k, Holo.cyan, style = Stroke(1.6.dp.toPx()))
        }
    }
}

/** Interrupteur incliné. */
@Composable
fun HoloToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    Row(modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = bodyStyle(14.sp, if (checked) Holo.text else Holo.muted))
            if (hint != null) Text(hint, style = bodyStyle(11.5.sp, Holo.dim, androidx.compose.ui.text.font.FontWeight.Medium))
        }
        Canvas(Modifier.size(58.dp, 26.dp)) {
            val s = 8.dp.toPx()
            drawPath(skewPath(0f, 0f, size.width, size.height, s), if (checked) Holo.green.copy(alpha = 0.25f) else Holo.field)
            drawPath(skewPath(0f, 0f, size.width, size.height, s), if (checked) Holo.green else Holo.line, style = Stroke(1.3.dp.toPx()))
            val kw = 24.dp.toPx(); val x = if (checked) size.width - kw - 4.dp.toPx() else 4.dp.toPx()
            drawPath(skewPath(x, 4.dp.toPx(), kw, size.height - 8.dp.toPx(), 6.dp.toPx()), if (checked) Holo.green else Holo.dim)
        }
    }
}

/** Puce (résumé, filtre). */
@Composable
fun Chip(text: String, color: Color = Holo.cyan, icon: HoloIcon? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = Skew(7.dp)
    Row(modifier.height(24.dp).clip(shape).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.7f), shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) Icon(icon, color, 12.dp)
        Text(text.uppercase(), style = displayStyle(10.5.sp, color, 0.1f), maxLines = 1)
    }
}

/** Badge de type (« ATTAQUE »…), ou badge libre. */
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier, height: Dp = 16.dp, textSize: Float = 9f) {
    val shape = Skew(5.dp)
    Row(modifier.height(height).clip(shape).background(color.copy(alpha = 0.16f)).border(1.dp, color, shape).padding(start = 4.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(6.dp, height - 7.dp).clip(Skew(2.dp)).background(color))
        Text(text.uppercase(), style = displayStyle(textSize.sp, color, 0.1f), maxLines = 1)
    }
}
@Composable
fun TypeBadge(type: TopType, modifier: Modifier = Modifier) = Badge(tr(type.label), Holo.type(type), modifier)

/** Barre de stat segmentée. */
@Composable
fun StatRow(label: String, value: Int, color: Color, modifier: Modifier = Modifier, max: Int = 15, valueText: String = "$value") {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) { FieldLabel(label, Modifier.weight(1f)); Text(valueText, style = displayStyle(12.sp, color, 0.02f)) }
        Canvas(Modifier.fillMaxWidth().height(9.dp).padding(top = 3.dp)) {
            val gap = 2.dp.toPx(); val sw = (size.width - (max - 1) * gap) / max
            repeat(max) { i -> drawPath(skewPath(i * (sw + gap), 0f, sw + 2.dp.toPx(), size.height, 2.dp.toPx()), if (i < value) color else Holo.line.copy(alpha = 0.5f)) }
        }
    }
}
@Composable
fun Stats3(attack: Int, defense: Int, endurance: Int, modifier: Modifier = Modifier, gap: Dp = 8.dp) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        StatRow(tr(TopType.Attack.label), attack, Holo.type(TopType.Attack))
        StatRow(tr(TopType.Defense.label), defense, Holo.type(TopType.Defense))
        StatRow(tr(TopType.Endurance.label), endurance, Holo.type(TopType.Endurance))
    }
}

/** Champ de texte (une ligne). */
@Composable
fun HoloField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "",
              trailing: HoloIcon? = null, onTrailing: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
    Column(modifier) {
        if (label != null) FieldLabel(label, Modifier.padding(bottom = 6.dp))
        val shape = Chamfer(10.dp)
        Row(Modifier.fillMaxWidth().height(40.dp).clip(shape).background(Holo.field).border(1.1.dp, Holo.line, shape).padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = bodyStyle(15.sp, Holo.dim), maxLines = 1)
                BasicTextField(value, onChange, singleLine = true, textStyle = bodyStyle(15.sp), cursorBrush = SolidColor(Holo.cyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone?.invoke() }), modifier = Modifier.fillMaxWidth())
            }
            if (trailing != null) IconBtn(trailing, { onTrailing?.invoke() }, width = 34.dp, height = 30.dp, iconSize = 16.dp)
        }
    }
}

/** Texte d'aide grisé. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = Holo.muted) =
    Text(text, style = bodyStyle(12.5.sp, color, androidx.compose.ui.text.font.FontWeight.Medium), modifier = modifier)

/** Titre de section avec « /// ». */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = Holo.cyan) = Row(modifier, verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)) { Slashes(height = 11.dp); Text(text.uppercase(), style = displayStyle(12.sp, color)) }
