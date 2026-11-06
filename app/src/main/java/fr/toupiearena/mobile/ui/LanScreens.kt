package fr.toupiearena.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.TournamentSettings
import fr.toupiearena.mobile.net.LobbyServer
import fr.toupiearena.mobile.net.Protocol
import java.net.Inet4Address
import java.net.NetworkInterface

/** Adresses IPv4 de l'appareil (à communiquer aux autres joueurs). */
private fun localAddresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>().map { it.hostAddress.orEmpty() }
}.getOrDefault(emptyList())

/** Multijoueur en réseau local : lobbys détectés, rejoindre par nom ou IP, créer un lobby. */
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
        if (port == null || port !in 1..65535) return run { app.message = "Port invalide : « ${text.substring(colon + 1)} »." }
        if (host.contains(' ')) return run { app.message = "Aucun lobby nommé « $text » n'a été détecté sur le réseau." }
        app.join(host, port)
    }

    Page(if (tournament != null) "Tournoi en réseau local" else "Multijoueur en réseau local",
        { app.screen = if (tournament != null) Screen.TournamentSetup else Screen.Menu }, scroll = false,
        actions = { AccentButton(if (tournament != null) "＋ Créer un tournoi…" else "＋ Créer un lobby…", { creating = true }) }) {
        Text("Votre pseudo : ${app.prefs.playerName} (modifiable dans les paramètres)", color = Palette.muted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (browser.isScanning) "Recherche des lobbys sur le réseau local…" else "Lobbys détectés (PC et mobiles) :",
                Modifier.weight(1f), color = Palette.text)
            SecondaryButton("Actualiser", browser::rescan)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Palette.panel, RoundedCornerShape(12.dp)).padding(8.dp)) {
            if (browser.lobbies.isEmpty()) item { Hint("Aucun lobby détecté pour l'instant. Le téléphone doit être sur le même Wi-Fi que l'hôte.") }
            items(browser.lobbies) { lobby ->
                val joinable = !lobby.inGame && lobby.players < lobby.maxPlayers
                Row(Modifier.fillMaxWidth().clickable(enabled = joinable) { app.join(lobby.address.hostAddress.orEmpty(), lobby.port) }
                    .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val color = if (joinable) Palette.text else Palette.muted
                    Text((if (lobby.tournament) "🏆 " else "") + lobby.name, Modifier.weight(1f), color = color, fontWeight = FontWeight.Bold)
                    Text(lobby.hostName, Modifier.width(110.dp), color = color)
                    Text("${lobby.players}/${lobby.maxPlayers}${if (lobby.inGame) " (en partie)" else ""}", Modifier.width(110.dp), color = color)
                    Text(Stadium.find(lobby.stadiumId)?.name ?: lobby.stadiumId, Modifier.width(80.dp), color = color)
                    Text("${lobby.address.hostAddress}:${lobby.port}", color = Palette.muted, fontSize = 12.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(typed, { typed = it }, Modifier.weight(1f), singleLine = true,
                label = { Text("Nom de lobby ou adresse IP[:port]") })
            HSpace()
            SecondaryButton("Rejoindre", ::joinTyped)
        }
    }
    if (creating) CreateLobbyDialog(app, tournament) { creating = false }
}

@Composable
private fun CreateLobbyDialog(app: AppController, tournament: TournamentSettings?, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(2) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (tournament != null) "Créer un tournoi (lobby des joueurs humains)" else "Créer un lobby") },
        text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it.take(Protocol.MAX_NAME_LENGTH) }, singleLine = true,
                            label = { Text("Nom du lobby") }, placeholder = { Text("Lobby de ${app.prefs.playerName}") })
                        val counts = (Battle.MIN_PLAYERS..Battle.MAX_PLAYERS).toList()
                        Picker(if (tournament != null) "Joueurs humains" else "Nombre de joueurs", counts, players - 2,
                            { "$it joueurs" }, { players = counts[it] })
                        StadiumPicker(stadium, { stadium = it })
                    }
                }
            }
        },
        confirmButton = {
            AccentButton("Créer", {
                onClose()
                app.host(LobbyServer.Settings(name.ifBlank { "Lobby de ${app.prefs.playerName}" }, app.prefs.playerName,
                    players, stadium, tournament))
            })
        },
        dismissButton = { SecondaryButton("Annuler", onClose) },
    )
}

/** Lobby (hôte ou client) : joueurs, toupies, réglages de l'hôte, statut prêt, lancement. */
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
    var picks by remember { mutableStateOf(listOf(0, 1, 2)) }
    var showRules by remember { mutableStateOf(false) }
    val needed = info.topsNeeded

    // Envoie le choix de toupies à l'arrivée, puis à chaque changement (ou du nombre demandé).
    LaunchedEffect(picks, needed) { if (!info.inGame) app.selectTops(picks.take(needed).map { tops[it] }) }

    val subtitle = if (hosting) {
        val endpoints = localAddresses().joinToString("  ·  ") { "$it:${server!!.port}" }
        "Vous êtes l'hôte. Les autres joueurs peuvent rejoindre « ${info.name} » depuis la liste des lobbys ou se connecter à : $endpoints"
    } else "Connecté à l'hôte ${client!!.hostLabel}."

    Page("Lobby « ${info.name} »", { app.leaveLobby() }, "Quitter le lobby", actions = {
        if (hosting) AccentButton(if (info.tournament != null) "Lancer le tournoi" else "Lancer la partie", app::startLobby,
            enabled = server!!.canStart())
        else {
            val ready = me?.ready == true
            AccentButton(if (ready) "Prêt ✓" else "Je suis prêt", { client!!.setReady(!ready) })
        }
    }) {
        Hint(subtitle)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Section("Réglages du lobby", Modifier.width(300.dp)) {
                val counts = (Battle.MIN_PLAYERS..Battle.MAX_PLAYERS).toList()
                if (hosting) Picker("Nombre de joueurs", counts, info.maxPlayers - 2, { "$it joueurs" }, { server!!.setMaxPlayers(counts[it]) })
                else Text("${info.maxPlayers} joueurs", color = Palette.text)
                StadiumPicker(info.stadiumId, { server?.setStadium(it) }, enabled = hosting)
                SecondaryButton("Personnalisation : ${info.rules.summary()}${if (hosting) " — modifier…" else ""}", { showRules = true })
                info.tournament?.let { t ->
                    TournamentSettingsEditor(t, info.maxPlayers, { server?.setTournamentSettings(it) }, readOnly = !hosting)
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Section("Joueurs") {
                    for (slot in 0 until info.maxPlayers) {
                        val p = info.players.getOrNull(slot)
                        Row {
                            if (p == null) Text("Place libre — en attente d'un joueur…", color = Palette.muted)
                            else {
                                Text(p.name + (if (p.host) " (hôte)" else "") + (if (p.id == myId) " — vous" else ""),
                                    Modifier.weight(1f), color = slotColor(slot))
                                Text(p.tops.joinToString(", ") { it.name }.ifEmpty { "—" }, Modifier.weight(1f), color = Palette.text)
                                Text(if (p.host) "Hôte" else if (p.ready) "Prêt" else "Pas prêt", Modifier.width(70.dp),
                                    color = if (p.host || p.ready) Palette.accent else Palette.muted)
                            }
                        }
                    }
                }
                Section(if (needed > 1) "Vos toupies (parmi les vôtres)" else "Votre toupie (parmi les vôtres)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(needed) { k ->
                            Picker("", tops, picks[k], ::topLabel, { i ->
                                picks = picks.toMutableList().also { it[k] = i }
                            }, Modifier.weight(1f))
                        }
                    }
                    TopPreview(tops.getOrNull(picks[0]))
                }
                val count = info.players.size
                Text(when {
                    count < info.maxPlayers -> "En attente de joueurs ($count/${info.maxPlayers})…"
                    hosting && !server!!.canStart() -> "Tous les joueurs doivent choisir leurs toupies et se déclarer prêts."
                    hosting -> "Tout le monde est prêt : vous pouvez lancer !"
                    else -> "Lobby complet. L'hôte lancera quand tout le monde sera prêt."
                }, color = Palette.text)
            }
        }
    }
    if (showRules) RulesDialog(info.rules, { server?.setRules(it) }, !hosting) { showRules = false }
}
