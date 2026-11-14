package fr.toupiearena.mobile.ui

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.Hint
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.bodyStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.core.TopRepository
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Participant
import fr.toupiearena.mobile.game.SavedTournament
import fr.toupiearena.mobile.ui.holo.I18n
import fr.toupiearena.mobile.ui.holo.tr
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.game.TournamentSettings
import fr.toupiearena.mobile.net.ClientSession
import fr.toupiearena.mobile.net.HostSession
import fr.toupiearena.mobile.net.LobbyBrowser
import fr.toupiearena.mobile.net.LobbyClient
import fr.toupiearena.mobile.net.LobbyServer
import fr.toupiearena.mobile.net.MatchCodec
import fr.toupiearena.mobile.net.Msg
import fr.toupiearena.mobile.net.TournamentController
import fr.toupiearena.mobile.net.TournamentDirector
import fr.toupiearena.mobile.net.TournamentFollower
import fr.toupiearena.mobile.session.LocalSession
import fr.toupiearena.mobile.session.MatchSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/** Écrans de l'application. */
sealed class Screen {
    data object Menu : Screen()
    data object SoloSetup : Screen()
    data object Builder : Screen()
    data object Settings : Screen()
    data object Help : Screen()
    /** Page des coups spéciaux ; [back] : écran où revenir (atelier, aide). */
    data class Moves(val back: Screen) : Screen()
    data object TournamentSetup : Screen()
    data object TournamentHub : Screen()
    data class LanMenu(val tournament: TournamentSettings?) : Screen()
    data object Lobby : Screen()
    data class Battle(val session: MatchSession, val mode: Mode) : Screen()

    enum class Mode { Solo, LanHost, LanClient, Tournament, Tutorial }
}

/**
 * État global : navigation, toupies enregistrées, réglages et objets réseau.
 * Tout tourne sur le fil principal (comme la boucle d'événements de Qt).
 */
class AppController(private val context: Context, val scope: CoroutineScope) {
    val main = Dispatchers.Main
    val prefs = Prefs(context)
    val repository = TopRepository(File(context.filesDir, "tops.json"))
    val repositoryLoaded = repository.load()
    private val savedSlot = SavedTournament(File(context.filesDir, "solo-tournament.json"))
    /** Tournoi solo sauvegardé (un seul emplacement, comme sur PC). */
    var saved by mutableStateOf(if (savedSlot.exists) savedSlot.load() else null)
        private set

    init {
        I18n.load(context)
        I18n.language = prefs.language
        MatchRules.defaultDuelMode = prefs.duelMode
    }

    var screen by mutableStateOf<Screen>(Screen.Menu)
    /** Incrémenté à chaque changement réseau ou de tournoi (recomposition). */
    var tick by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)

    var server: LobbyServer? = null
        private set
    var client: LobbyClient? = null
        private set
    var tournament: TournamentController? = null
        private set
    val browser = LobbyBrowser(scope, main).apply { onChanged = { bump() } }
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wasInGame = false
    private var lastSolo: (() -> Unit)? = null

    fun bump() { tick++ }

    // --- Solo -------------------------------------------------------------------

    /** Solo contre 1 à 3 bots. */
    fun startSolo(player: Top, bots: List<Top>, difficulty: Difficulty, stadium: Stadium, rules: MatchRules) {
        val start = { open(LocalSession(player, bots, stadium, difficulty, rules), Screen.Mode.Solo) }
        lastSolo = start
        start()
    }

    fun rematch() = (currentBattle()?.session as? LocalSession)?.restart()

    fun startTutorial() = open(TutorialSession(prefs.scheme), Screen.Mode.Tutorial)

    private fun currentBattle() = screen as? Screen.Battle

    private fun open(session: MatchSession, mode: Screen.Mode) {
        currentBattle()?.session?.close()
        screen = Screen.Battle(session, mode)
    }

    /** Quitte un combat (solo, tutoriel) ou abandonne / quitte selon le mode. */
    fun leaveBattle() {
        val battle = currentBattle() ?: return
        when (battle.mode) {
            Screen.Mode.Solo, Screen.Mode.Tutorial -> { battle.session.close(); screen = Screen.Menu }
            Screen.Mode.Tournament -> tournament?.forfeitLocal()
            Screen.Mode.LanHost, Screen.Mode.LanClient -> leaveLobby()
        }
    }

    // --- Tournoi solo ---------------------------------------------------------------

    fun startSoloTournament(settings: TournamentSettings, tops: List<Top>) {
        closeTournament()
        clearSaved()
        val humans = listOf(Participant("Vous", LobbyServer.HOST_ID, Difficulty.Normal, tops))
        val t = Tournament.create(settings, Tournament.addBots(humans, settings, repository.allTops, Random.nextLong()), Random.nextLong())
        attachTournament(TournamentDirector(null, t, Random.nextLong(), scope, main))
    }

    private fun attachTournament(controller: TournamentController) {
        tournament = controller
        controller.onChanged = { bump() }
        controller.onSessionReady = { controller.takeSession()?.let { open(it, Screen.Mode.Tournament) } }
        controller.onLocalMatchOver = {
            currentBattle()?.session?.close()
            screen = Screen.TournamentHub
        }
        screen = Screen.TournamentHub
    }

    /** « Tour 2/3 » ou « journée 4/7 » du tournoi sauvegardé, null s'il n'y en a pas. */
    fun savedTournamentLabel(): String? = saved?.let { t -> roundLabel(t) }

    fun resumeSoloTournament() {
        val t = saved ?: return
        clearSaved() // Le tournoi repris est en cours : la sauvegarde sera refaite en le quittant.
        closeTournament()
        attachTournament(TournamentDirector(null, t, Random.nextLong(), scope, main))
    }

    private val soloTournament get() = tournament?.takeIf { server == null && client == null }?.tournament

    /** Quitte le tournoi solo ; [save] : le garder pour le reprendre plus tard. */
    fun quitSoloTournament(save: Boolean) {
        val t = soloTournament
        if (t != null && !t.isFinished && save) {
            if (!savedSlot.save(t)) { message = tr("Impossible d'écrire la sauvegarde du tournoi."); return }
            saved = t
        } else if (!save) clearSaved()
        currentBattle()?.session?.close()
        closeTournament()
        screen = Screen.Menu
    }

    /** L'application passe en arrière-plan (et peut être tuée) : le tournoi solo en cours est sauvegardé. */
    fun onBackground() {
        val t = soloTournament ?: return
        if (!t.isFinished && savedSlot.save(t)) saved = t
    }

    private fun clearSaved() { savedSlot.clear(); saved = null }

    val isSoloTournament get() = soloTournament != null

    fun closeTournament() {
        tournament?.close()
        tournament = null
    }

    fun leaveTournament() {
        if (server != null || client != null) leaveLobby() else {
            closeTournament()
            screen = Screen.Menu
        }
    }

    // --- Réseau local -----------------------------------------------------------------

    /** Écoute des lobbys (avec le verrou multicast : sans lui, Android filtre les diffusions UDP). */
    fun startBrowsing() {
        if (multicastLock == null) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifi.createMulticastLock("toupie-arena").apply { setReferenceCounted(false); acquire() }
        }
        browser.start(scanNetwork = true)
    }

    fun stopBrowsing() {
        browser.stop()
        if (server == null && client == null) {
            multicastLock?.release()
            multicastLock = null
        }
    }

    fun host(settings: LobbyServer.Settings) {
        leaveLobby(goToMenu = false)
        val s = LobbyServer(scope, main)
        server = s
        scope.launch(main) {
            val result = s.start(settings)
            if (result.isFailure) {
                server = null
                message = tr("Le lobby n'a pas pu être créé : %1", result.exceptionOrNull()?.message.orEmpty())
                return@launch
            }
            s.lobbyChanged += { bump() }
            stopBrowsing()
            screen = Screen.Lobby
            bump()
        }
    }

    fun join(host: String, port: Int) {
        leaveLobby(goToMenu = false)
        val c = LobbyClient(scope, main)
        client = c
        c.onJoined = {
            wasInGame = false
            stopBrowsing()
            screen = Screen.Lobby
            bump()
        }
        c.onJoinFailed = {
            client = null
            message = tr("Impossible de rejoindre : %1", it)
        }
        c.onConnectionLost = {
            client = null
            closeTournament()
            currentBattle()?.session?.close()
            message = it
            screen = Screen.LanMenu(null)
        }
        c.lobbyChanged += {
            // L'hôte a ramené tout le monde au lobby.
            if (wasInGame && !c.lobby.inGame) {
                closeTournament()
                currentBattle()?.session?.close()
                screen = Screen.Lobby
            }
            wasInGame = c.lobby.inGame
            bump()
        }
        c.matchStarted += { start ->
            val setup = MatchCodec.parseStart(start)
            if (setup != null && setup.match < 0) open(ClientSession(c, setup), Screen.Mode.LanClient)
        }
        c.gameMessage += { message ->
            if (message.optString("type") == Msg.Tournament && tournament == null) {
                val follower = TournamentFollower(c)
                follower.apply(message)
                attachTournament(follower)
            }
        }
        c.connectToHost(host, port, prefs.playerName)
    }

    /** Hôte : lance la partie ou le tournoi du lobby. */
    fun startLobby() {
        val s = server ?: return
        if (!s.canStart()) return
        val info = s.lobby
        val settings = info.tournament
        if (settings == null) {
            val setup = MatchCodec.parseStart(s.startMatch(Random.nextLong() and 0xffffffffL)) ?: return s.returnToLobby()
            open(HostSession(s, setup), Screen.Mode.LanHost)
            return
        }
        val full = settings.copy(stadiumId = info.stadiumId, rules = info.rules)
        val humans = info.players.map { Participant(it.name, it.id, Difficulty.Normal, it.tops.take(info.topsNeeded)) }
        val t = Tournament.create(full, Tournament.addBots(humans, full, repository.allTops, Random.nextLong()), Random.nextLong())
        s.startTournament()
        attachTournament(TournamentDirector(s, t, Random.nextLong(), scope, main))
    }

    /** Hôte : fin de partie ou de tournoi, retour de tous au lobby. */
    fun returnToLobby() {
        currentBattle()?.session?.close()
        closeTournament()
        server?.returnToLobby()
        screen = Screen.Lobby
    }

    fun leaveLobby(goToMenu: Boolean = true) {
        currentBattle()?.session?.close()
        closeTournament()
        server?.close("L'hôte a fermé le lobby.")
        server = null
        client?.disconnect()
        client = null
        if (goToMenu) screen = Screen.LanMenu(null)
        bump()
    }

    fun selectTops(tops: List<Top>) {
        server?.setHostTops(tops)
        client?.selectTops(tops)
    }
}

/** Racine de l'interface : un écran à la fois. */
@Composable
fun ToupieApp(app: AppController) {
    androidx.compose.material3.MaterialTheme(androidx.compose.material3.darkColorScheme(primary = Holo.green, onPrimary = Holo.ink,
        background = Holo.void, surface = Holo.panel, onSurface = Holo.text, onBackground = Holo.text, error = Holo.danger)) {
        when (val screen = app.screen) {
            Screen.Menu -> MenuScreen(app)
            Screen.SoloSetup -> SoloSetupScreen(app)
            Screen.Builder -> BuilderScreen(app)
            Screen.Settings -> SettingsScreen(app)
            Screen.Help -> HelpScreen(app)
            is Screen.Moves -> MovesScreen(app, screen.back)
            Screen.TournamentSetup -> TournamentSetupScreen(app)
            Screen.TournamentHub -> TournamentHubScreen(app)
            is Screen.LanMenu -> LanMenuScreen(app, screen.tournament)
            Screen.Lobby -> LobbyScreen(app)
            is Screen.Battle -> BattleFor(app, screen)
        }
        MessageDialog(app)
    }
}

@Composable
private fun BattleFor(app: AppController, screen: Screen.Battle) {
    val session = screen.session
    @Composable
    fun battle(pausable: Boolean, leave: String, changeTops: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) =
        BattleScreen(session, app.prefs.scheme, { app.prefs.scheme = it }, app.prefs.tiltSensitivity, pausable, leave, app::leaveBattle, changeTops, actions)
    when (screen.mode) {
        Screen.Mode.Solo -> {
            val changeTops = { session.close(); app.screen = Screen.SoloSetup }
            battle(true, tr("Quitter le combat"), changeTops) {
                HoloButton(tr("Revanche"), app::rematch, Modifier.weight(1.1f), BtnVariant.Primary, HoloIcon.Random, height = 48.dp)
                HoloButton(tr("Changer de toupies"), changeTops, Modifier.weight(1.2f), height = 48.dp, textSize = 11.5f)
                HoloButton(tr("Menu"), app::leaveBattle, Modifier.weight(0.7f), BtnVariant.Ghost, height = 48.dp)
            }
        }
        Screen.Mode.Tutorial -> battle(true, tr("Quitter le tutoriel")) {
            HoloButton(tr("Recommencer"), app::startTutorial, Modifier.weight(1f), BtnVariant.Primary, HoloIcon.Random, height = 48.dp)
            HoloButton(tr("Menu"), app::leaveBattle, Modifier.weight(0.6f), BtnVariant.Ghost, height = 48.dp)
        }
        Screen.Mode.Tournament -> battle(app.isSoloTournament, tr("Abandonner le match"))
        Screen.Mode.LanHost -> battle(false, tr("Fermer le lobby")) {
            HoloButton(tr("Retour au lobby"), app::returnToLobby, Modifier.weight(1f), BtnVariant.Primary, HoloIcon.Lan, height = 48.dp)
        }
        Screen.Mode.LanClient -> battle(false, tr("Quitter le lobby")) {
            Panel(Modifier.weight(1f)) {
                Text(tr("L'hôte ramène tout le monde au lobby…"), style = bodyStyle(13.sp))
                Hint(tr("Quitter : bouton retour d'Android"), color = Holo.dim)
            }
        }
    }
}

@Composable
private fun MessageDialog(app: AppController) {
    val text = app.message ?: return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { app.message = null },
        confirmButton = { HoloButton(tr("OK"), { app.message = null }, Modifier.width(110.dp), BtnVariant.Primary, height = 40.dp) },
        text = { Text(text, style = bodyStyle(14.sp)) },
        containerColor = Holo.panel,
        shape = fr.toupiearena.mobile.ui.holo.Chamfer(14.dp),
    )
}

/** Toupies nécessaires pour un tournoi au format donné. */
fun topsNeeded(settings: TournamentSettings) = MatchSeries.topsFor(settings.matchFormat)

