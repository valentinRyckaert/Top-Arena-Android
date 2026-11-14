package fr.toupiearena.mobile.ui.holo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.R
import fr.toupiearena.mobile.core.TopType

/** Couleurs de la charte « Holo Combat » (identiques au jeu PC). */
object Holo {
    val void = Color(0xFF05040F)
    val abyss = Color(0xFF0A0820)
    val deep = Color(0xFF110E33)
    val panel = Color(0xFF15113F)
    val panel2 = Color(0xFF1D1858)
    val line = Color(0xFF3A2F8F)
    val lineHi = Color(0xFF5B4BD1)
    val blue = Color(0xFF2F6BFF)
    val cyan = Color(0xFF34D6FF)
    val violet = Color(0xFF7B3CFF)
    val magenta = Color(0xFFC23BFF)
    val green = Color(0xFF3DFF8B)
    val lime = Color(0xFFB8FF2E)
    val text = Color(0xFFEAF0FF)
    val muted = Color(0xFF9296D0)
    val dim = Color(0xFF585C99)
    val danger = Color(0xFFFF3B6B)
    val warn = Color(0xFFFFC23D)
    val ink = Color(0xFF03140A)      // texte sur fond vert
    val field = Color(0xFF07061A)    // champs, pistes
    val card = Color(0xFF100D35)

    fun type(type: TopType): Color = when (type) {
        TopType.Attack -> Color(0xFFFF3D6E)
        TopType.Defense -> Color(0xFF2F7BFF)
        TopType.Endurance -> Color(0xFF3DFF8B)
        TopType.Balance -> Color(0xFFFFC23D)
    }

    val display = FontFamily(
        Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
        Font(R.font.chakra_petch_bold, FontWeight.Bold),
        Font(R.font.chakra_petch_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )
    val body = FontFamily(
        Font(R.font.rajdhani_medium, FontWeight.Medium),
        Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
        Font(R.font.rajdhani_bold, FontWeight.Bold),
    )
}

/** Titres, boutons, chiffres : Chakra Petch gras, espacé (le texte est mis en capitales par l'appelant). */
fun displayStyle(size: TextUnit, color: Color = Holo.text, spacing: Float = 0.12f, italic: Boolean = false) = TextStyle(
    fontFamily = Holo.display, fontWeight = FontWeight.Bold, fontSize = size, color = color,
    letterSpacing = (size.value * spacing).sp, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
)
/** Texte courant : Rajdhani. */
fun bodyStyle(size: TextUnit = 14.sp, color: Color = Holo.text, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Holo.body, fontWeight = weight, fontSize = size, color = color)
/** Petites capitales des libellés (« ATTAQUE », « STADIUM »…). */
fun labelStyle(color: Color = Holo.muted, size: TextUnit = 10.5.sp) =
    TextStyle(fontFamily = Holo.body, fontWeight = FontWeight.Bold, fontSize = size, color = color, letterSpacing = 1.6.sp)

// ---------------------------------------------------------------- formes

/** Rectangle aux coins coupés à 45° (haut gauche, haut droit, bas droit, bas gauche). */
class Chamfer(private val tl: Dp, private val tr: Dp = 0.dp, private val br: Dp = tl, private val bl: Dp = 0.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val (a, b, c, d) = with(density) { listOf(tl.toPx(), tr.toPx(), br.toPx(), bl.toPx()) }
        return Outline.Generic(Path().apply {
            moveTo(a, 0f); lineTo(size.width - b, 0f); lineTo(size.width, b); lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height); lineTo(d, size.height); lineTo(0f, size.height - d); lineTo(0f, a); close()
        })
    }
}

/** Parallélogramme incliné (segments, puces, jauges). */
class Skew(private val slant: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { slant.toPx() }.coerceAtMost(size.width / 2)
        return Outline.Generic(Path().apply {
            moveTo(s, 0f); lineTo(size.width, 0f); lineTo(size.width - s, size.height); lineTo(0f, size.height); close()
        })
    }
}

fun skewPath(x: Float, y: Float, w: Float, h: Float, s: Float) = Path().apply {
    moveTo(x + s, y); lineTo(x + w, y); lineTo(x + w - s, y + h); lineTo(x, y + h); close()
}

// ---------------------------------------------------------------- fond

/** Fond holographique : dégradé radial violet, halos bleu, magenta et vert, lignes de balayage. */
fun DrawScope.holoBackground(cx: Float = 0.5f, cy: Float = 0.35f) {
    val w = size.width; val h = size.height; val m = maxOf(w, h)
    drawRect(Holo.void)
    drawRect(Brush.radialGradient(listOf(Color(0xF21B1260), Color(0xE60C0A2C), Holo.void), Offset(w * cx, h * cy), m * 0.75f))
    fun halo(x: Float, y: Float, r: Float, c: Color, a: Float) =
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), c.copy(alpha = 0f)), Offset(x, y), r), r, Offset(x, y))
    halo(w * 0.12f, h * 0.1f, m * 0.4f, Holo.blue, 0.18f)
    halo(w * 0.9f, h * 0.95f, m * 0.36f, Holo.magenta, 0.15f)
    halo(w * 0.75f, h * 0.15f, m * 0.16f, Holo.green, 0.07f)
    var y = 0f
    val step = 4.dp.toPx() / 2.5f
    while (y < h) { drawLine(Color.White.copy(alpha = 0.022f), Offset(0f, y), Offset(w, y), 1f); y += step }
}

// ---------------------------------------------------------------- icônes

/** Icônes au trait de la charte (grille 24, trait 1,8), mêmes tracés que les maquettes. */
enum class HoloIcon(val path: String, val filled: Boolean = false) {
    Play("M7,4 L19,12 L7,20 Z", true),
    Lan("M3,8 H15 L12,5 M21,16 H9 L12,19"),
    Trophy("M7,4 H17 V9 C17,12 15,14 12,14 C9,14 7,12 7,9 Z M7,6 H4 V8 C4,10 5.5,11 7,11 M17,6 H20 V8 C20,10 18.5,11 17,11 M12,14 V18 M8,20 H16 L15,18 H9 Z"),
    Build("M12,3 L20,7.5 V16.5 L12,21 L4,16.5 V7.5 Z M12,12 L20,7.5 M12,12 L4,7.5 M12,12 V21"),
    Tuto("M2,9 L12,4 L22,9 L12,14 Z M6,11 V16 C8,18 16,18 18,16 V11 M22,9 V15"),
    Gear("M12,8.5 A3.5,3.5 0 1 0 12,15.5 A3.5,3.5 0 1 0 12,8.5 Z M10.5,2 H13.5 L14,5 L16.5,6.2 L19,4.5 L21,6.5 L19.5,9 L20.5,11.5 L23,12 V14 L20.5,14.6 L19.5,17 L21,19.5 L19,21.5 L16.5,20 L14,21 L13.5,23 H10.5 L10,21 L7.5,20 L5,21.5 L3,19.5 L4.5,17 L3.5,14.6 L1,14 V12 L3.5,11.5 L4.5,9 L3,6.5 L5,4.5 L7.5,6.2 L10,5 Z"),
    Help("M9,9 C9,6 15,6 15,9.5 C15,12 12,12 12,15 M12,18.5 V19.5"),
    Update("M20,12 A8,8 0 1 1 17.6,6.3 M20,4 V8 H16"),
    Quit("M6,6 L18,18 M18,6 L6,18"),
    Back("M15,5 L8,12 L15,19"),
    Next("M9,5 L16,12 L9,19"),
    Dash("M3,12 H14 M10,7 L16,12 L10,17 M18,6 V18 M21,8 V16"),
    Bolt("M13,2 L5,13 H11 L10,22 L19,10 H13 Z"),
    User("M12,4 A4,4 0 1 0 12,12 A4,4 0 1 0 12,4 Z M4,21 C5,16 19,16 20,21"),
    Bot("M5,8 H19 V19 H5 Z M12,4 V8 M9,13 V14 M15,13 V14 M9,17 H15"),
    Lock("M6,11 H18 V21 H6 Z M8.5,11 V7.5 A3.5,3.5 0 0 1 15.5,7.5 V11"),
    Wifi("M2,9 C8,3 16,3 22,9 M5.5,12.5 C9.5,8.5 14.5,8.5 18.5,12.5 M9,16 C10.8,14.4 13.2,14.4 15,16 M12,19.5 V19.6"),
    Star("M12,3 L14.6,9 L21,9.5 L16,13.6 L17.6,20 L12,16.6 L6.4,20 L8,13.6 L3,9.5 L9.4,9 Z"),
    Check("M4,12.5 L9.5,18 L20,6"),
    Timer("M12,6 A7.5,7.5 0 1 0 12,21 A7.5,7.5 0 1 0 12,6 Z M12,9.5 V13.5 L14.5,15 M9.5,2.5 H14.5"),
    Sliders("M4,6 H20 M4,12 H20 M4,18 H20 M9,4 V8 M15,10 V14 M7,16 V20"),
    Balance("M12,3 V20 M5,6 H19 M5,6 L2,13 C2,15 8,15 8,13 Z M19,6 L16,13 C16,15 22,15 22,13 Z M8,20 H16"),
    Plus("M12,5 V19 M5,12 H19"),
    Trash("M4,7 H20 M9,7 V4 H15 V7 M6,7 L7,21 H17 L18,7"),
    Save("M5,3 H16 L20,7 V21 H5 Z M8,3 V8 H15 V3 M8,21 V14 H17 V21"),
    Random("M3,7 H7 C12,7 12,17 17,17 H21 M18,14 L21,17 L18,20 M3,17 H7 C9,17 10,15.5 11,13.5 M14,8.2 C15,7.4 16,7 17,7 H21 M18,4 L21,7 L18,10"),
    Pause("M8,5 V19 M16,5 V19"),
    Phone("M7,2 H17 V22 H7 Z M10,19 H14"),
    Hand("M9,11 V4.5 A1.5,1.5 0 0 1 12,4.5 V10 M12,9 A1.5,1.5 0 0 1 15,9 V11 M15,10 A1.5,1.5 0 0 1 18,10 V15 C18,19 15.5,21 12.5,21 C9.5,21 8,19.5 6.5,17 L4.5,13.5 A1.4,1.4 0 0 1 7,12 L9,14.5"),
    ;

    val vector: ImageVector by lazy {
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = if (filled) SolidColor(Color.White) else null,
            stroke = SolidColor(Color.White), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Square, strokeLineJoin = StrokeJoin.Miter,
        ).build()
    }
}

@Composable
fun Icon(icon: HoloIcon, color: Color = Holo.cyan, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val filter = remember(color) { ColorFilter.tint(color) }
    Image(icon.vector, contentDescription = null, modifier = modifier.size(size), colorFilter = filter)
}
