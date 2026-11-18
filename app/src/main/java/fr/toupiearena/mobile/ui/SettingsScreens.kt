package fr.toupiearena.mobile.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.game.DuelMode
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.net.Protocol
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.FieldLabel
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloCard
import fr.toupiearena.mobile.ui.holo.HoloField
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.HoloSlider
import fr.toupiearena.mobile.ui.holo.I18n
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.TiltSteering
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.tr

/** Onglets verticaux à gauche (paramètres, aide). */
@Composable
private fun Rail(items: List<Pair<String, HoloIcon>>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.width(170.dp).fillMaxHeight().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, (label, icon) ->
            val on = i == selected
            HoloCard(Modifier.fillMaxWidth().height(46.dp), selected = on, onClick = { onSelect(i) }) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, if (on) Holo.green else Holo.muted, 18.dp)
                    Text(label.uppercase(), style = displayStyle(12.sp, if (on) Holo.text else Holo.muted))
                }
            }
        }
    }
}

/** Texte du type de duel (mêmes phrases que sur PC, « l'écran » à la place de la touche). */
fun duelDescription(mode: DuelMode): String = when (mode) {
    DuelMode.Timing -> tr("Appuyez sur %1 quand le curseur traverse la zone verte, de plus en plus étroite : un raté perd le duel.", tr("l’écran"))
    DuelMode.Rush -> tr("Tir à la corde : appuyez le plus vite possible sur %1 pour pousser le curseur vers l'adversaire. Au bout de 3,5 s, le plus avancé gagne ; pile au milieu, match nul.", tr("l’écran"))
    DuelMode.None -> tr("Pas de duel : deux toupies qui se percutent en plein dash se repoussent comme lors d'un choc ordinaire.")
}
fun duelLabel(mode: DuelMode) = when (mode) { DuelMode.Timing -> tr("Timing"); DuelMode.Rush -> tr("Rafale"); DuelMode.None -> tr("Sans duel") }

@Composable
fun SettingsScreen(app: AppController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(app.prefs.playerName) }
    fun back() { app.prefs.playerName = Protocol.sanitizeName(name, tr("Joueur mobile")); app.screen = Screen.Menu }
    androidx.activity.compose.BackHandler { back() }
    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Paramètres"), ::back)
            Row(Modifier.fillMaxSize().padding(horizontal = EdgeMargin), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Rail(listOf(tr("Général") to HoloIcon.Gear, tr("Commandes") to HoloIcon.Hand, tr("Réseau") to HoloIcon.Wifi), tab) { tab = it }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (tab) {
                        0 -> GeneralSettings(app)
                        1 -> ControlSettings(app)
                        else -> Panel(Modifier.fillMaxWidth(), title = tr("Réseau local")) {
                            HoloField(name, { name = it.take(Protocol.MAX_NAME_LENGTH) }, Modifier.fillMaxWidth(), label = tr("Votre pseudo"), placeholder = tr("Joueur mobile"))
                            Hint(tr("Affiché dans les lobbys et les tournois en réseau local (PC et mobiles)."), Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettings(app: AppController) {
    var duel by remember { mutableStateOf(MatchRules.defaultDuelMode) }
    Panel(Modifier.fillMaxWidth(), title = tr("Langue")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Segmented(I18n.languages.map { it.second }, I18n.languages.indexOfFirst { it.first == I18n.language }, { i ->
                I18n.language = I18n.languages[i].first; app.prefs.language = I18n.language
            }, Modifier.width(300.dp), height = 34.dp)
            Hint(tr("La langue change aussitôt."))
        }
    }
    Panel(Modifier.fillMaxWidth(), title = tr("Duels")) {
        Segmented(DuelMode.entries.map(::duelLabel), duel.ordinal, { i ->
            duel = DuelMode.entries[i]; app.prefs.duelMode = duel; MatchRules.defaultDuelMode = duel
        }, height = 34.dp)
        Text(duelDescription(duel), style = bodyStyle(13.sp), modifier = Modifier.padding(top = 10.dp))
        Hint(tr("C'est le duel par défaut des combats, des parties en réseau et des tournois ; la personnalisation des règles permet d'en choisir un autre."),
            Modifier.padding(top = 6.dp), Holo.dim)
    }
}

@Composable
private fun ControlSettings(app: AppController) {
    var scheme by remember { mutableStateOf(app.prefs.scheme) }
    var tilt by remember { mutableFloatStateOf(app.prefs.tiltSensitivity) }
    var trying by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ControlScheme.entries.forEach { s ->
            val on = s == scheme
            HoloCard(Modifier.weight(1f).height(200.dp), selected = on, onClick = { scheme = s; app.prefs.scheme = s }) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    SchemeArt(s, Modifier.fillMaxWidth().height(90.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr(s.label).uppercase(), style = displayStyle(11.5.sp), modifier = Modifier.weight(1f), maxLines = 1)
                        if (on) Icon(HoloIcon.Check, Holo.green, 14.dp)
                    }
                    Text(tr(s.summary), style = bodyStyle(11.5.sp, Holo.muted), modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.weight(1f))
                    Text(if (on) tr("Choisi") else tr("Toucher pour choisir"), style = bodyStyle(11.sp, if (on) Holo.green else Holo.dim))
                }
            }
        }
    }
    if (scheme == ControlScheme.Tilt) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HoloSlider((tilt - 0.15f) / 0.55f, { tilt = 0.15f + it * 0.55f; app.prefs.tiltSensitivity = tilt },
                tr("Inclinaison pour piloter à fond"), tr("%1 % · ≈ %2°", (tilt * 100).toInt(), (tilt * 57).toInt()), Modifier.weight(1f))
            HoloButton(if (trying) tr("Arrêter") else tr("Essayer"), { trying = !trying }, Modifier.width(140.dp), BtnVariant.Ghost, HoloIcon.Phone, height = 40.dp, textSize = 11.5f)
        }
        if (trying) TiltPreview(tilt)
    }
    Hint(tr("Visée (tous les schémas) : glisser depuis sa toupie, relâcher pour lancer. Duel : toucher l'écran."))
}

/** Illustration d'un schéma de commandes. */
@Composable
private fun SchemeArt(scheme: ControlScheme, modifier: Modifier) = Canvas(modifier) {
    drawRect(Holo.field)
    val h = size.height; val w = size.width
    when (scheme) {
        ControlScheme.Buttons -> {
            drawCircle(Holo.lineHi, h * 0.26f, Offset(w * 0.22f, h * 0.55f), style = Stroke(1.5f))
            drawCircle(Holo.green, h * 0.11f, Offset(w * 0.25f, h * 0.48f))
            drawCircle(Holo.green.copy(alpha = 0.4f), h * 0.17f, Offset(w * 0.8f, h * 0.58f))
            drawCircle(Holo.magenta.copy(alpha = 0.4f), h * 0.12f, Offset(w * 0.6f, h * 0.68f))
        }
        ControlScheme.Tilt -> {
            val cx = w / 2; val cy = h * 0.45f
            drawLine(Holo.cyan, Offset(cx - h * 0.18f, cy - h * 0.3f), Offset(cx + h * 0.06f, cy - h * 0.36f), 2f)
            drawRect(Holo.cyan, Offset(cx - h * 0.18f, cy - h * 0.3f), androidx.compose.ui.geometry.Size(h * 0.3f, h * 0.55f), style = Stroke(2f))
            drawArc(Holo.green, 20f, 140f, false, Offset(cx - h * 0.5f, cy - h * 0.2f), androidx.compose.ui.geometry.Size(h, h * 0.75f), style = Stroke(2f))
        }
        ControlScheme.Gestures -> {
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.15f, h * 0.7f); quadraticTo(w * 0.5f, h * 0.1f, w * 0.85f, h * 0.55f) }
            drawPath(p, Holo.green, style = Stroke(2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            drawCircle(Holo.cyan.copy(alpha = 0.25f), h * 0.14f, Offset(w * 0.85f, h * 0.55f))
            drawCircle(Holo.cyan, h * 0.14f, Offset(w * 0.85f, h * 0.55f), style = Stroke(1.5f))
        }
    }
}

/** Essai de l'inclinaison : la bille suit le téléphone (même calcul qu'en combat). */
@Composable
private fun TiltPreview(sensitivity: Float) {
    var vec by remember { mutableStateOf(fr.toupiearena.mobile.game.Vec2.ZERO) }
    TiltSteering(sensitivity) { vec = it }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.width(110.dp).height(110.dp)) {
            val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension / 2 - 4f
            drawCircle(Holo.field, r, c); drawCircle(Holo.lineHi, r, c, style = Stroke(1.5f)); drawCircle(Holo.line, r * 0.45f, c, style = Stroke(1f))
            drawLine(Holo.line, Offset(c.x - r, c.y), Offset(c.x + r, c.y)); drawLine(Holo.line, Offset(c.x, c.y - r), Offset(c.x, c.y + r))
            drawCircle(Holo.green, r * 0.22f, Offset(c.x + vec.x.toFloat() * r * 0.8f, c.y + vec.y.toFloat() * r * 0.8f))
        }
        Hint(tr("La position du téléphone au début du combat sert de neutre. Inclinez-le : la bille montre la direction du pilotage."))
    }
}

// ============================== AIDE ==============================

@Composable
fun HelpScreen(app: AppController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scheme = app.prefs.scheme
    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Aide"), { app.screen = Screen.Menu }) {
                HoloButton(tr("Paramètres"), { app.screen = Screen.Settings }, height = 36.dp, icon = HoloIcon.Gear, textSize = 12f)
            }
            Row(Modifier.fillMaxSize().padding(horizontal = EdgeMargin), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Rail(listOf(tr("Commandes") to HoloIcon.Hand, tr("Règles") to HoloIcon.Balance, tr("Coups") to HoloIcon.Bolt,
                    tr("Licences") to HoloIcon.Lock), tab) {
                    if (it == 2) app.screen = Screen.Moves(Screen.Help) else tab = it
                }
                if (tab == 3) {
                    Panel(Modifier.weight(1f).padding(vertical = 12.dp), title = tr("Licences"), index = "v${app.updater.current}") {
                        val context = LocalContext.current
                        val text = remember { licencesText(context) }
                        Text(text, style = bodyStyle(11.5.sp, Holo.muted, FontWeight.Medium), modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                    return@Row
                }
                val rows = if (tab == 0) controlRows(scheme) else ruleRows()
                Panel(Modifier.weight(1f).padding(vertical = 12.dp), title = if (tab == 0) tr("Commandes · %1", tr(scheme.label).lowercase()) else tr("Règles du combat")) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        rows.forEach { (what, how) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                                Text(what.uppercase(), style = displayStyle(11.5.sp, Holo.green), modifier = Modifier.width(140.dp))
                                Text(how, style = bodyStyle(13.sp), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Licence du jeu, composants tiers et textes complets de leurs licences (assets/licences). */
fun licencesText(context: Context): String =
    listOf("LICENSE.txt", "THIRD-PARTY-NOTICES.md", "Apache-2.0.txt", "OFL-ChakraPetch.txt", "OFL-Rajdhani.txt").joinToString("\n\n") { name ->
        "==== $name ====\n\n" + context.assets.open("licences/$name").bufferedReader().use { it.readText() }
    }

fun controlRows(scheme: ControlScheme): List<Pair<String, String>> {
    val steer = when (scheme) {
        ControlScheme.Buttons -> tr("joystick en bas à gauche (maintenu)")
        ControlScheme.Tilt -> tr("incliner le téléphone (la position au début du combat sert de neutre)")
        ControlScheme.Gestures -> tr("glisser le doigt (depuis l'endroit touché)")
    }
    val dash = if (scheme == ControlScheme.Gestures) tr("toucher brièvement l'écran") else tr("bouton DASH")
    val special = if (scheme == ControlScheme.Gestures) tr("appui long sans bouger") else tr("bouton SPÉCIAL")
    return listOf(
        tr("Visée") to tr("glisser depuis sa toupie : la flèche donne la direction, sa longueur la force"),
        tr("Lancer") to tr("relâcher le doigt"),
        tr("Piloter") to steer, tr("Dash") to dash,
        tr("Coup spécial") to tr("%1 — une fois par partie, coûte ¼ de l'endurance du moment", special),
        tr("Duel") to tr("toucher l'écran (timing) ou toucher très vite (rafale)"),
        tr("Menu du combat") to tr("bouton pause en haut à gauche, ou retour d'Android"),
    )
}

private fun ruleRows() = listOf(
    tr("Victoire") to tr("la dernière toupie en jeu gagne : éjection hors du stadium ou endurance épuisée"),
    tr("Chocs") to tr("l'attaque adverse retire de l'endurance ; la défense réduit les dégâts et le recul"),
    tr("Rebord") to tr("le percuter use un peu la rotation ; le longer ne coûte rien"),
    tr("Dash") to tr("fonce sur l'adversaire le plus proche, avec plus d'attaque, contre un peu d'endurance"),
    tr("Coup spécial") to tr("une fois par partie (donné par le boulon), coûte un quart de l'endurance du moment"),
    tr("Duels") to tr("deux dashs qui se croisent : timing, rafale ou aucun, selon les règles"),
    tr("Types") to tr("Attaque bat Endurance, Endurance bat Défense, Défense bat Attaque"),
    tr("Stadiums") to tr("même surface pour tous ; chacun avantage légèrement certains types"),
)
