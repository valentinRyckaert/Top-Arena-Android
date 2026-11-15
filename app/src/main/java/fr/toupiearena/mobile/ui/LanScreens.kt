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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.TournamentSettings
import fr.toupiearena.mobile.net.LobbyAnnouncement
import fr.toupiearena.mobile.net.LobbyPlayer
import fr.toupiearena.mobile.net.LobbyServer
import fr.toupiearena.mobile.net.Protocol
import fr.toupiearena.mobile.ui.holo.Badge
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.Chip
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
import fr.toupiearena.mobile.ui.holo.HoloToggle
import fr.toupiearena.mobile.ui.holo.Icon
import fr.toupiearena.mobile.ui.holo.IconBtn
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.SideDrawer
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.TypeBadge
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.labelStyle
import fr.toupiearena.mobile.ui.holo.skewPath
import fr.toupiearena.mobile.ui.holo.tr
import java.net.Inet4Address
import java.net.NetworkInterface

/** Adresses IPv4 de l'appareil (à communiquer aux autres joueurs). */
private fun localAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>().map { it.hostAddress.orEmpty() }
}.getOrDefault(emptyList())

private val PLAYER_COUNTS = (Battle.MIN_PLAYERS..Battle.MAX_PLAYERS).toList()

/** Multijoueur en réseau local : lobbys détectés, rejoindre par nom ou IP ; créer un lobby dans un tiroir. */
@Composable
fun LanMenuScreen(app: AppController, tournament: TournamentSettings?) {
    DisposableEffect(Unit) {
        app.startBrowsing()
        onDispose { app.stopBrowsing() }
    }
    app.tick
    val browser = app.browser
    var typed by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    fun joinTyped() {
        val text = typed.trim()
        if (text.isEmpty()) return
        browser.findByName(text)?.let { return app.join(it.address.hostAddress.orEmpty(), it.port) }
        val colon = text.lastIndexOf(':')
        val host = if (colon > 0) text.substring(0, colon) else text
        val port = if (colon > 0) text.substring(colon + 1).toIntOrNull() else Protocol.DEFAULT_TCP_PORT
        if (port == null || port !in 1..65535) return run { app.message = tr("Port invalide : « %1 ».", text.substring(colon + 1)) }
        if (host.contains(' ')) return run { app.message = tr("Aucun lobby nommé « %1 » n'a été détecté sur le réseau.", text) }
        app.join(host, port)
    }

    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (tournament != null) tr("Tournoi en réseau") else tr("Multijoueur"),
                { app.screen = if (tournament != null) Screen.TournamentSetup else Screen.Menu }, sub = tr("réseau local")) {
                IconBtn(HoloIcon.Update, browser::rescan)
                HoloButton(if (tournament != null) tr("Créer un tournoi") else tr("Créer un lobby"), { creating = true }, Modifier.width(200.dp),
                    BtnVariant.Primary, HoloIcon.Plus, height = 40.dp, textSize = 12f)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(tr("Pseudo : %1", app.prefs.playerName), Holo.green, HoloIcon.User)
                Text(tr("modifiable dans les Paramètres"), style = bodyStyle(11.sp, Holo.dim), modifier = Modifier.weight(1f))
                Text(if (browser.isScanning) tr("Recherche en cours…") else tr("Recherche en continu · annonces UDP et balayage du Wi-Fi"),
                    style = bodyStyle(11.sp, Holo.muted))
                Canvas(Modifier.size(10.dp)) { drawCircle(Holo.green, size.minDimension / 2) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = EdgeMargin), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (browser.lobbies.isEmpty()) item {
                    Hint(tr("Aucun lobby détecté pour l'instant. Le téléphone doit être sur le même Wi-Fi que l'hôte."), Modifier.padding(top = 20.dp))
                }
                items(browser.lobbies) { lobby -> LobbyRow(lobby) { app.join(lobby.address.hostAddress.orEmpty(), lobby.port) } }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HoloField(typed, { typed = it }, Modifier.weight(1f), placeholder = tr("ex. « Lobby de Léa » ou 192.168.1.20:45455"), onDone = ::joinTyped)
                HoloButton(tr("Rejoindre"), ::joinTyped, Modifier.width(150.dp), height = 40.dp, textSize = 12f)
            }
        }
        CreateLobbyDrawer(app, tournament, creating) { creating = false }
    }
}

@Composable
private fun LobbyRow(lobby: LobbyAnnouncement, onJoin: () -> Unit) {
    val joinable = !lobby.inGame && lobby.players < lobby.maxPlayers
    val stadium = Stadium.find(lobby.stadiumId) ?: Stadium.classic
    HoloCard(Modifier.fillMaxWidth().height(54.dp), onClick = if (joinable) onJoin else null) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StadiumView(stadium, 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(lobby.name.uppercase(), style = displayStyle(12.sp, if (joinable) Holo.text else Holo.muted), maxLines = 1)
                    if (lobby.tournament) Icon(HoloIcon.Trophy, Holo.warn, 13.dp)
                }
                Text("${lobby.hostName} · ${lobby.address.hostAddress}", style = bodyStyle(10.5.sp, Holo.muted), maxLines = 1)
            }
            Column(Modifier.width(90.dp)) {
                Canvas(Modifier.size((lobby.maxPlayers * 14).dp, 10.dp)) {
                    repeat(lobby.maxPlayers) { i ->
                        val on = i < lobby.players
                        val p = skewPath(i * 14.dp.toPx(), 0f, 11.dp.toPx(), size.height, 3.dp.toPx())
                        if (on) drawPath(p, Holo.cyan) else drawPath(p, Holo.cyan, style = Stroke(1.dp.toPx()))
                    }
                }
                Text(if (lobby.inGame) tr("en partie") else tr("%1/%2 joueurs", lobby.players, lobby.maxPlayers),
                    style = bodyStyle(10.sp, if (lobby.inGame) Holo.warn else Holo.muted))
            }
            if (joinable) HoloButton(tr("Rejoindre"), onJoin, Modifier.width(130.dp), BtnVariant.Primary, height = 36.dp, textSize = 11f)
            else Text(if (lobby.inGame) tr("en partie") else tr("complet"), style = bodyStyle(11.sp, Holo.dim), modifier = Modifier.width(130.dp), textAlign = TextAlign.End)
        }
    }
}

/** Grille des stadiums (deux colonnes), pour les tiroirs. */
@Composable
private fun StadiumGrid(selected: String, onSelect: ((String) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Stadium.all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { s -> StadiumCard(s, s.id == selected, Modifier.weight(1f), 44.dp, onSelect?.let { { it(s.id) } }) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreateLobbyDrawer(app: AppController, tournament: TournamentSettings?, visible: Boolean, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var players by remember { mutableIntStateOf(2) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    var settings by remember { mutableStateOf(tournament?.fitBots(2)) }
    val fallback = tr("Lobby de %1", app.prefs.playerName)
    SideDrawer(visible, DrawerSide.End, if (tournament != null) tr("Créer un tournoi") else tr("Créer un lobby"), onClose, width = 440.dp,
        sub = tr("visible sur le Wi-Fi"), footer = {
            HoloButton(tr("Créer et attendre les joueurs"), {
                onClose()
                app.host(LobbyServer.Settings(name.ifBlank { fallback }, app.prefs.playerName, players, stadium, settings?.fitBots(players)))
            }, Modifier.weight(1f), BtnVariant.Primary, HoloIcon.Play, height = 46.dp)
        }) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HoloField(name, { name = it.take(Protocol.MAX_NAME_LENGTH) }, label = tr("Nom du lobby"), placeholder = fallback)
            Segmented(PLAYER_COUNTS.map { "$it" }, players - Battle.MIN_PLAYERS, { players = PLAYER_COUNTS[it]; settings = settings?.fitBots(players) },
                label = if (tournament != null) tr("Joueurs humains") else tr("Joueurs"), height = 32.dp)
            FieldLabel(tr("Stadium"))
            StadiumGrid(stadium) { stadium = it }
            settings?.let { s -> TournamentSettingsForm(s, players, { settings = it }) }
        }
    }
}

/** Lobby (hôte ou client) : places des joueurs, toupies, réglages de l'hôte, prêt, lancement. */
@Composable
fun LobbyScreen(app: AppController) {
    app.tick
    val server = app.server
    val client = app.client
    val info = server?.lobby ?: client?.lobby ?: return
    val hosting = server != null
    val myId = if (hosting) LobbyServer.HOST_ID else client!!.playerId
    val me = info.player(myId)
    val tops = remember(app.tick) { app.repository.allTops }
    var picks by remember { mutableStateOf(listOf(tops[0], tops[1 % tops.size], tops[2 % tops.size])) }
    var showSettings by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var showTops by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<Int?>(null) }
    val needed = info.topsNeeded

    // Envoie le choix de toupies dès qu'on figure dans le lobby, puis à chaque changement (ou du nombre demandé).
    LaunchedEffect(picks, needed, me != null) { if (!info.inGame) app.selectTops(picks.take(needed)) }

    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Lobby · %1", info.name), { app.leaveLobby() }, backIcon = HoloIcon.Quit) {
                IconBtn(HoloIcon.Sliders, { showSettings = true }, on = !info.rules.isDefault)
                if (hosting) HoloButton(if (info.tournament != null) tr("Lancer le tournoi") else tr("Lancer"), app::startLobby, Modifier.width(190.dp),
                    BtnVariant.Primary, HoloIcon.Play, height = 40.dp, enabled = server!!.canStart(), textSize = 12f)
                else {
                    val ready = me?.ready == true
                    HoloButton(if (ready) tr("Prêt") else tr("Je suis prêt"), { client!!.setReady(!ready) }, Modifier.width(190.dp),
                        if (ready) BtnVariant.Primary else BtnVariant.Secondary, HoloIcon.Check, height = 40.dp, textSize = 12f)
                }
            }
            Row(Modifier.padding(horizontal = EdgeMargin, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hosting) {
                    Chip(tr("Hôte"), Holo.green, HoloIcon.Star)
                    Chip(tr("%1 · visible en LAN", localAddresses().joinToString("  ·  ") { "$it:${server!!.port}" }), Holo.cyan, HoloIcon.Wifi)
                } else Chip(tr("Connecté à %1", client!!.hostLabel), Holo.cyan, HoloIcon.Wifi)
                if (info.tournament != null) Chip(tr("Tournoi"), Holo.warn, HoloIcon.Trophy)
            }
            // Places en grille : deux colonnes.
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = EdgeMargin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until info.maxPlayers).chunked(2).forEach { row ->
                    Row(Modifier.height(118.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { slot -> PlayerSlot(info.players.getOrNull(slot), slot, myId, Modifier.weight(1f).fillMaxHeight()) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HoloButton(if (needed > 1) tr("Vos toupies : %1", picks.take(needed).joinToString(", ") { it.name })
                    else tr("Votre toupie : %1", picks[0].name), { if (needed > 1) showTops = true else picking = 0 },
                    Modifier.width(300.dp), icon = HoloIcon.Build, height = 40.dp, textSize = 11f)
                val count = info.players.size
                val ready = info.players.count { it.host || it.ready }
                Text(when {
                    count < info.maxPlayers -> tr("En attente de joueurs (%1/%2)…", count, info.maxPlayers)
                    hosting && !server!!.canStart() -> tr("%1 joueurs sur %2 prêts · %3", ready, count, (Stadium.find(info.stadiumId) ?: Stadium.classic).displayName())
                    hosting -> tr("Tout le monde est prêt : vous pouvez lancer !")
                    else -> tr("Lobby complet. L'hôte lancera quand tout le monde sera prêt.")
                }, style = bodyStyle(12.sp, Holo.warn), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                HoloButton(if (hosting) tr("Fermer le lobby") else tr("Quitter le lobby"), { app.leaveLobby() }, Modifier.width(200.dp),
                    BtnVariant.Danger, HoloIcon.Quit, height = 40.dp, textSize = 11.5f)
            }
        }
        // Réglages de l'hôte (en lecture seule chez un client).
        SideDrawer(showSettings, DrawerSide.End, tr("Réglages de l'hôte"), { showSettings = false }, width = 440.dp,
            sub = if (hosting) tr("visibles par tous") else tr("lecture seule")) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel(tr("Stadium"))
                StadiumGrid(info.stadiumId, if (hosting) ({ server!!.setStadium(it) }) else null)
                Segmented(PLAYER_COUNTS.map { "$it" }, info.maxPlayers - Battle.MIN_PLAYERS, { if (hosting) server!!.setMaxPlayers(PLAYER_COUNTS[it]) },
                    label = tr("Nombre de joueurs"), height = 32.dp, enabled = hosting)
                HoloToggle(tr("Coups spéciaux"), info.rules.specials, { if (hosting) server!!.setRules(info.rules.copy(specials = it)) })
                HoloButton(tr("Personnalisation…"), { showRules = true }, Modifier.fillMaxWidth(), icon = HoloIcon.Sliders, height = 40.dp, textSize = 12f)
                Hint(rulesSummary(info.rules), color = Holo.dim)
                info.tournament?.let { t -> TournamentSettingsForm(t, info.maxPlayers, { server?.setTournamentSettings(it) }, readOnly = !hosting) }
            }
        }
        RulesDrawer(showRules, info.rules, { server?.setRules(it) }, { showRules = false }, readOnly = !hosting)
        SideDrawer(showTops, DrawerSide.Start, tr("Vos toupies"), { showTops = false }, width = 430.dp, sub = tr("ordre choisi avant chaque match")) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(needed) { k -> TopRow(picks[k], name = tr("Toupie %1 · %2", k + 1, picks[k].name), chevron = true) { picking = k } }
            }
        }
        val k = picking
        TopPickerDrawer(k != null, tr("Toupie %1", (k ?: 0) + 1), tops, k?.let { picks[it] }, { t ->
            if (t != null && k != null) picks = picks.toMutableList().also { it[k] = t }
        }, { picking = null })
    }
}

/** Place d'un joueur : toupie, nom, état ; place libre en pointillés. */
// Objets du lobby / tournoi modifiés sur place : ne jamais sauter la recomposition.
@NonSkippableComposable
@Composable
private fun PlayerSlot(p: LobbyPlayer?, slot: Int, myId: Int, modifier: Modifier) {
    if (p == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Holo.lineHi, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("Emplacement libre").uppercase(), style = labelStyle(Holo.muted))
                Text(tr("en attente d'un joueur sur le réseau local…"), style = bodyStyle(11.sp, Holo.dim))
            }
        }
        return
    }
    val mine = p.id == myId
    val top: Top? = p.tops.firstOrNull()
    HoloCard(modifier, selected = mine) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("J${slot + 1}", style = labelStyle(Holo.dim, 9.sp))
                TopView(top, 54.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(p.name.uppercase(), style = displayStyle(14.sp), maxLines = 1)
                Text(listOfNotNull(if (mine) tr("vous") else null, if (p.host) tr("hôte") else null).joinToString(" · "), style = bodyStyle(10.5.sp, Holo.muted))
                if (top != null) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.tops.joinToString(", ") { it.name }.uppercase(), style = displayStyle(10.sp), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    TypeBadge(top.type)
                }
            }
            val (text, color) = when {
                p.host || p.ready -> tr("Prêt") to Holo.green
                p.tops.isEmpty() -> tr("Choisit sa toupie…") to Holo.warn
                else -> tr("Pas prêt") to Holo.muted
            }
            Badge(text.uppercase(), color, Modifier.align(Alignment.Top).padding(top = 8.dp), height = 20.dp, textSize = 9.5f)
        }
    }
}
