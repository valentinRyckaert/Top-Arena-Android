package fr.toupiearena.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.net.Protocol
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MenuScreen(app: AppController) {
    Surface(Modifier.fillMaxSize(), color = Palette.background) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOUPIE ARENA", color = Palette.accent, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Text("Assemblez votre toupie · affrontez le bot ou vos amis en réseau local", color = Palette.muted,
                    textAlign = TextAlign.Center)
                Hero(Modifier.fillMaxWidth().weight(1f))
            }
            Column(Modifier.width(320.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                AccentButton("▶  Jouer contre le bot", { app.screen = Screen.SoloSetup }, Modifier.fillMaxWidth())
                SecondaryButton("⇄  Multijoueur en réseau local", { app.screen = Screen.LanMenu(null) }, Modifier.fillMaxWidth())
                SecondaryButton("🏆  Tournois", { app.screen = Screen.TournamentSetup }, Modifier.fillMaxWidth())
                SecondaryButton("⚙  Assembler une toupie", { app.screen = Screen.Builder }, Modifier.fillMaxWidth())
                SecondaryButton("🎓  Tutoriel", app::startTutorial, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Paramètres", { app.screen = Screen.Settings }, Modifier.weight(1f))
                    SecondaryButton("Aide", { app.screen = Screen.Help }, Modifier.weight(1f))
                }
            }
        }
    }
    if (!app.repositoryLoaded) LaunchedEffect(Unit) { app.message = "Le fichier des toupies est illisible et a été ignoré." }
}

/** Trois toupies en orbite autour du titre. */
@Composable
private fun Hero(modifier: Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = (it - start) / 1e9f }
    }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.32f
        drawCircle(Palette.accent.copy(alpha = 0.25f), r, c, style = Stroke(3f))
        PartCatalog.presets.take(3).forEachIndexed { i, top ->
            val a = time * 0.9 + i * 2.1
            drawTop(top, c + Offset((cos(a) * r).toFloat(), (sin(a) * r * 0.45f).toFloat()), r * 0.28f, time * 12.0 + i)
        }
    }
}

@Composable
fun SoloSetupScreen(app: AppController) {
    val tops = remember(app.tick) { app.repository.allTops }
    var player by remember { mutableIntStateOf(0) }
    var opponent by remember { mutableIntStateOf(-1) } // -1 : au hasard
    var difficulty by remember { mutableStateOf(Difficulty.Normal) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    var rules by remember { mutableStateOf(MatchRules()) }
    var showRules by remember { mutableStateOf(false) }

    Page("Préparer le combat", { app.screen = Screen.Menu }, actions = {
        SecondaryButton(if (rules.isDefault) "Personnalisation…" else "Personnalisation ✱", { showRules = true })
        HSpace()
        AccentButton("Combattre !", {
            val bot = if (opponent in tops.indices) tops[opponent] else tops[Random.nextInt(tops.size)]
            app.startSolo(tops[player], bot, difficulty, Stadium.find(stadium) ?: Stadium.classic, rules)
        })
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Section("Votre toupie", Modifier.weight(1f)) {
                Picker("", tops, player, ::topLabel, { player = it })
                TopPreview(tops.getOrNull(player))
            }
            Section("Toupie du bot", Modifier.weight(1f)) {
                Picker("", listOf<Any>("Aléatoire") + tops, opponent + 1,
                    { if (it is String) it else topLabel(it as fr.toupiearena.mobile.core.Top) }, { opponent = it - 1 })
                TopPreview(tops.getOrNull(opponent), mystery = "Toupie choisie au hasard au lancement du combat")
            }
        }
        Section("Combat") {
            Picker("Difficulté du bot", Difficulty.entries, difficulty.ordinal, { it.label }, { difficulty = Difficulty.entries[it] })
            StadiumPicker(stadium, { stadium = it })
        }
    }
    if (showRules) RulesDialog(rules, { rules = it }, false) { showRules = false }
}

@Composable
fun SettingsScreen(app: AppController) {
    var scheme by remember { mutableStateOf(app.prefs.scheme) }
    var name by remember { mutableStateOf(app.prefs.playerName) }
    var tilt by remember { mutableFloatStateOf(app.prefs.tiltSensitivity) }
    Page("Paramètres", {
        app.prefs.playerName = Protocol.sanitizeName(name, "Joueur mobile")
        app.screen = Screen.Menu
    }) {
        Section("Commandes du combat") {
            ControlScheme.entries.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(scheme == s, { scheme = s; app.prefs.scheme = s })
                    Column {
                        Text(s.label, color = Palette.text, fontWeight = FontWeight.Bold)
                        Hint(s.description)
                    }
                }
            }
            if (scheme == ControlScheme.Tilt) {
                Text("Inclinaison pour piloter à fond : ${(tilt * 100).toInt()} % de la gravité (≈ ${(tilt * 57).toInt()}°)",
                    color = Palette.text, fontSize = 13.sp)
                Slider(tilt, { tilt = it; app.prefs.tiltSensitivity = it }, valueRange = 0.15f..0.7f)
            }
            Hint("Visée (tous les schémas) : glissez depuis votre toupie, relâchez pour lancer. En duel : touchez l'écran.")
        }
        Section("Réseau local") {
            OutlinedTextField(name, { name = it.take(Protocol.MAX_NAME_LENGTH) }, label = { Text("Votre pseudo") }, singleLine = true)
        }
    }
}

@Composable
fun HelpScreen(app: AppController) {
    val scheme = app.prefs.scheme
    Page("Aide", { app.screen = Screen.Menu }, actions = { SecondaryButton("Paramètres…", { app.screen = Screen.Settings }) }) {
        Section("Visée") {
            Text("Posez le doigt sur l'arène et glissez : la flèche part de votre toupie, sa direction donne celle " +
                "du lancer et sa longueur la force. Relâchez pour lancer.", color = Palette.text)
        }
        Section("Combat — schéma « ${scheme.label} »") {
            val lines = when (scheme) {
                ControlScheme.Buttons -> listOf("Piloter" to "joystick en bas à gauche (maintenu)", "Dash" to "bouton DASH",
                    "Coup spécial" to "bouton SPÉCIAL")
                ControlScheme.Tilt -> listOf("Piloter" to "incliner le téléphone (la position au début du combat sert de neutre)",
                    "Dash" to "bouton DASH", "Coup spécial" to "bouton SPÉCIAL")
                ControlScheme.Gestures -> listOf("Piloter" to "glisser le doigt (depuis l'endroit touché)",
                    "Dash" to "toucher brièvement l'écran", "Coup spécial" to "appui long sans bouger")
            }
            (lines + listOf("Duel" to "toucher l'écran quand le curseur est dans la zone verte",
                "Quitter" to "bouton en haut à gauche ou retour Android")).forEach { (what, how) ->
                Row { Text(what, Modifier.width(130.dp), color = Palette.accent); Text(how, color = Palette.text) }
            }
        }
        Section("Règles") {
            listOf(
                "Une toupie éjectée du stadium ou à court d'endurance perd ; la dernière en jeu gagne.",
                "Chaque choc retire de l'endurance selon l'attaque adverse et votre défense ; la défense réduit aussi le recul.",
                "Le dash fonce sur l'adversaire avec plus d'attaque, contre un peu d'endurance.",
                "Le coup spécial (donné par le boulon) ne se lance qu'une fois par partie et coûte un quart de l'endurance du moment.",
                "Deux dashs qui se percutent déclenchent un duel : le premier qui rate subit 1,5× les dégâts du dash adverse.",
                "Attaque bat Endurance, Endurance bat Défense, Défense bat Attaque ; chaque stadium avantage légèrement certains types.",
            ).forEach { Text("• $it", color = Palette.text) }
        }
    }
}
