package fr.toupiearena.mobile.ui.holo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class DrawerSide { Start, End }

/**
 * Tiroir latéral rétractable : voile sur l'écran (toucher pour fermer), panneau qui glisse
 * depuis un bord ; glisser vers le bord, ✕ ou le retour d'Android le referment.
 * Gauche = choisir, consulter ; droite = régler.
 */
@Composable
fun SideDrawer(
    visible: Boolean, side: DrawerSide, title: String, onClose: () -> Unit, width: Dp = 440.dp, sub: String? = null,
    accent: Color = Holo.cyan, footer: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(visible, onClose)
    val start = side == DrawerSide.Start
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x9E03020A))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose))
        }
        AnimatedVisibility(
            visible, modifier = Modifier.align(if (start) Alignment.CenterStart else Alignment.CenterEnd),
            enter = slideInHorizontally { if (start) -it else it }, exit = slideOutHorizontally { if (start) -it else it },
        ) {
            val shape = if (start) Chamfer(0.dp, 26.dp, 26.dp, 0.dp) else Chamfer(26.dp, 0.dp, 0.dp, 26.dp)
            Column(
                Modifier.width(width).fillMaxHeight().clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B1660), Color(0xFF0B0928))))
                    .border(1.3.dp, Holo.lineHi, shape)
                    .drawBehind {
                        val x = if (start) size.width - 2.dp.toPx() else 2.dp.toPx()
                        drawLine(accent.copy(alpha = 0.8f), Offset(x, 40.dp.toPx()), Offset(x, size.height - 40.dp.toPx()), 3.dp.toPx())
                    }
                    .pointerInput(start) {
                        var dx = 0f
                        detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = { if (if (start) dx < -80f else dx > 80f) onClose() }) { _, d -> dx += d }
                    }
                    .padding(start = if (start) EdgeMargin else 22.dp, end = if (start) 16.dp else EdgeMargin),
            ) {
                Row(Modifier.fillMaxWidth().height(50.dp).drawBehind {
                    drawLine(Holo.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slashes(accent.takeIf { it != Holo.cyan } ?: Holo.green)
                    Text(title.uppercase(), style = displayStyle(15.sp), maxLines = 1)
                    if (sub != null) Text(sub, style = bodyStyle(12.sp, Holo.muted, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Spacer(Modifier.weight(1f))
                    IconBtn(HoloIcon.Quit, onClose, color = Holo.muted, width = 40.dp, height = 38.dp, iconSize = 16.dp)
                }
                Column(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp), content = content)
                if (footer != null) Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically, content = footer)
            }
        }
    }
}
