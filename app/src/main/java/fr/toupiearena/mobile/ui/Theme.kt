package fr.toupiearena.mobile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette de la version PC : gris foncé et vert néon adouci. */
object Palette {
    val background = Color(0xFF15181C)
    val backgroundDeep = Color(0xFF0E1013)
    val panel = Color(0xFF1D2126)
    val panelAlt = Color(0xFF252A31)
    val border = Color(0xFF343B44)
    val text = Color(0xFFE3E7EC)
    val muted = Color(0xFF8F99A3)
    val accent = Color(0xFF8EE05A)
    val accentBright = Color(0xFFA9F272)
    val accentDark = Color(0xFF4C7D2E)
    val danger = Color(0xFFFF6B6B)
    val warning = Color(0xFFFFC14D)
}

@Composable
fun ToupieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.accent, onPrimary = Color(0xFF10140D), secondary = Palette.accentBright,
            background = Palette.background, surface = Palette.panel, onSurface = Palette.text,
            onBackground = Palette.text, surfaceVariant = Palette.panelAlt, outline = Palette.border,
            error = Palette.danger,
        ),
        content = content,
    )
}

/** Écran standard : titre, contenu défilant, barre de boutons en bas. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Retour",
    actions: @Composable () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack != null) { onBack?.invoke() } // Retour Android = bouton « Retour » de la page.
    Surface(Modifier.fillMaxSize(), color = Palette.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(title, color = Palette.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(4.dp))
            val body = Modifier.weight(1f).fillMaxWidth()
            Column(if (scroll) body.verticalScroll(rememberScrollState()) else body,
                verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) SecondaryButton(backLabel, onBack)
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier, enabled = enabled, shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier, enabled = enabled, shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Palette.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.text)) {
        Text(text)
    }
}

/** Panneau à bord arrondi avec un titre vert. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Palette.panel, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Palette.accent, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun Hint(text: String) = Text(text, color = Palette.muted, fontSize = 13.sp)

@Composable
fun HSpace(width: Int = 8) = Spacer(Modifier.width(width.dp))
