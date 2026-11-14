package fr.toupiearena.mobile.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.Stadium

/**
 * Planche de contrôle visuel (captures d'écran) : les 8 toupies prêtes à l'emploi et leur vue
 * éclatée, toutes les pièces par famille, les 6 stadiums.
 * shortcut: pas d'annotation @Preview, la dépendance ui-tooling-preview n'est pas au projet ;
 * l'ajouter (avec ui-tooling en debug) pour l'afficher dans Android Studio.
 */
@Composable
fun ArtGallery(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Color(0xFF0A0820))) {
        val cell = minOf(size.width / 14f, size.height / 8.2f)
        PartCatalog.presets.forEachIndexed { i, top ->
            drawTop(top, Offset(cell * (i + 0.5f), cell * 0.5f), cell * 0.42f, 0.4f, glow = if (i == 0) 1f else 0f)
            drawProfile(top, Offset(cell * (i + 0.5f), cell * 1.05f), cell * 0.8f)
        }
        drawSpinArrow(PartCatalog.presets[0].spin, Offset(cell * 8.5f, cell * 0.5f), cell * 0.25f, Holo.Cyan)
        drawSpinArrow(PartCatalog.presets[2].spin, Offset(cell * 9.5f, cell * 0.5f), cell * 0.25f, Holo.Cyan)
        PartKind.entries.forEachIndexed { row, kind ->
            PartCatalog.parts(kind).forEachIndexed { i, part ->
                drawPart(part, Offset(cell * (i + 0.5f), cell * (2.5f + row * 0.9f)), cell * 0.36f)
            }
        }
        Stadium.all.forEachIndexed { i, stadium ->
            val geometry = stadium.geometry(stadium.apply(BattleConfig()))
            drawStadium(stadium, geometry, Offset(cell * (i * 2.3f + 1.2f), cell * 7.2f), cell * 0.8f)
        }
    }
}
