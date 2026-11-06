package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.game.TournamentSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.UUID

/**
 * Serveur d'un lobby, exécuté chez l'hôte (joueur 0) : accepte les clients,
 * tient l'état du lobby, l'annonce en UDP, répond aux questions de découverte
 * et relaie les messages de jeu. Compatible avec les clients PC.
 */
class LobbyServer(private val scope: CoroutineScope, private val main: CoroutineDispatcher) {
    data class Settings(
        val lobbyName: String,
        val hostName: String,
        val maxPlayers: Int = 2,
        val stadiumId: String = Stadium.classic.id,
        val tournament: TournamentSettings? = null,
    )

    private class Client(var id: Int, val channel: JsonChannel)

    private var server: ServerSocket? = null
    private var udp: DatagramSocket? = null
    private var queries: DatagramSocket? = null
    private val jobs = mutableListOf<Job>()
    private val clients = mutableListOf<Client>()
    private var nextId = 1
    var lobby = LobbyInfo()
        private set
    var port = 0
        private set

    val lobbyChanged = mutableListOf<() -> Unit>()
    /** Message de jeu (visée, dash, duel, ordre des toupies…) reçu d'un client pendant une partie. */
    val gameMessage = mutableListOf<(Int, JSONObject) -> Unit>()
    val playerLeft = mutableListOf<(Int) -> Unit>()

    val isListening get() = server != null

    /** Écoute TCP sur le premier port libre à partir de `firstPort` (0 = quelconque) ; UDP si discoveryPort != 0. */
    suspend fun start(settings: Settings, firstPort: Int = Protocol.DEFAULT_TCP_PORT,
                      discoveryPort: Int = Protocol.DISCOVERY_PORT): Result<Unit> {
        close()
        val socket = withContext(Dispatchers.IO) {
            val ports = if (firstPort == 0) listOf(0) else (firstPort until firstPort + Protocol.PORT_ATTEMPTS).toList()
            ports.firstNotNullOfOrNull { p ->
                runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(p)) } }.getOrNull()
            }
        } ?: return Result.failure(IllegalStateException("Aucun port TCP libre à partir de $firstPort"))
        server = socket
        port = socket.localPort
        lobby = LobbyInfo(
            lobbyId = UUID.randomUUID().toString().take(8),
            name = Protocol.sanitizeName(settings.lobbyName, "Lobby"),
            maxPlayers = settings.maxPlayers.coerceIn(Battle.MIN_PLAYERS, Battle.MAX_PLAYERS),
            stadiumId = Stadium.find(settings.stadiumId)?.id ?: Stadium.classic.id,
            tournament = settings.tournament?.fitBots(settings.maxPlayers.coerceIn(Battle.MIN_PLAYERS, Battle.MAX_PLAYERS)),
        )
        lobby.players += LobbyPlayer(HOST_ID, Protocol.sanitizeName(settings.hostName, "Hôte"), ready = true, host = true)
        nextId = 1
        jobs += scope.launch(Dispatchers.IO) {
            while (isActive) {
                val s = runCatching { socket.accept() }.getOrNull() ?: break
                withContext(main) { onNewConnection(s) }
            }
        }
        if (discoveryPort != 0) startDiscovery(discoveryPort)
        notifyChanged()
        return Result.success(Unit)
    }

    private fun startDiscovery(discoveryPort: Int) {
        udp = runCatching { DatagramSocket().apply { broadcast = true } }.getOrNull()
        // Réponse directe aux questions des clients (passe les pare-feu qui bloquent les annonces).
        queries = runCatching {
            DatagramSocket(null).apply { reuseAddress = true; broadcast = true; bind(InetSocketAddress(discoveryPort)) }
        }.getOrNull()
        val q = queries
        if (q != null) {
            jobs += scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(4096)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    if (runCatching { q.receive(packet) }.isFailure) break
                    val o = runCatching { JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8)) }.getOrNull()
                        ?: continue
                    if (o.optString("type") == Msg.Query && o.optString("app") == Protocol.APP_TAG) {
                        val reply = withContext(main) { announcementJson() }.toString().toByteArray(Charsets.UTF_8)
                        runCatching { q.send(DatagramPacket(reply, reply.size, packet.address, packet.port)) }
                    }
                }
            }
        }
        jobs += scope.launch(Dispatchers.IO) {
            var targets = emptyList<InetAddress>()
            var count = 0
            while (isActive) {
                if (count++ % 15 == 0) targets = broadcastTargets()
                val data = withContext(main) { announcementJson() }.toString().toByteArray(Charsets.UTF_8)
                for (t in targets) runCatching { udp?.send(DatagramPacket(data, data.size, t, discoveryPort)) }
                delay(1000)
            }
        }
    }

    fun close(reason: String = "") {
        jobs.forEach { it.cancel() }
        jobs.clear()
        for (c in clients) {
            if (c.id >= 0) c.channel.send(JSONObject().put("type", Msg.Closed).put("reason", reason))
            c.channel.close()
        }
        clients.clear()
        runCatching { server?.close() }
        runCatching { udp?.close() }
        runCatching { queries?.close() }
        server = null
        udp = null
        queries = null
    }

    fun setHostTops(tops: List<Top>) {
        lobby.player(HOST_ID)?.let { it.tops = tops.take(3); lobbyUpdated() }
    }

    fun setMaxPlayers(maxPlayers: Int) {
        val value = maxPlayers.coerceIn(maxOf(Battle.MIN_PLAYERS, lobby.players.size), Battle.MAX_PLAYERS)
        if (value != lobby.maxPlayers) {
            lobby.maxPlayers = value
            lobby.tournament = lobby.tournament?.fitBots(value)
            lobbyUpdated()
        }
    }

    fun setStadium(stadiumId: String) {
        if (Stadium.find(stadiumId) != null && stadiumId != lobby.stadiumId) {
            lobby.stadiumId = stadiumId
            lobbyUpdated()
        }
    }

    fun setRules(rules: MatchRules) {
        if (!rules.sameAs(lobby.rules)) {
            lobby.rules = rules
            lobbyUpdated()
        }
    }

    /** Lobby de tournoi uniquement. */
    fun setTournamentSettings(settings: TournamentSettings) {
        if (lobby.tournament == null) return
        val s = settings.copy(participants = settings.participants.coerceIn(Tournament.MIN_PARTICIPANTS,
            Tournament.MAX_PARTICIPANTS)).fitBots(lobby.maxPlayers)
        if (s != lobby.tournament) {
            lobby.tournament = s
            lobbyUpdated()
        }
    }

    /** Lobby complet, toutes les toupies choisies, tous les clients prêts. */
    fun canStart(): Boolean = !lobby.inGame && lobby.players.size == lobby.maxPlayers &&
        lobby.players.all { it.tops.size >= lobby.topsNeeded && (it.host || it.ready) }

    /** Lance la partie : diffuse le message de départ et le renvoie. */
    fun startMatch(seed: Long): JSONObject {
        lobby.inGame = true
        val start = MatchCodec.encodeStart(MatchSetup(
            Stadium.find(lobby.stadiumId) ?: Stadium.classic, lobby.rules, seed,
            lobby.players.map { it.id }, lobby.players.map { it.name }, lobby.players.map { it.tops.first() },
        ))
        broadcast(start)
        lobbyUpdated()
        return start
    }

    fun startTournament() {
        lobby.inGame = true
        lobbyUpdated()
    }

    /** Fin de partie : retour de tous les joueurs au lobby. */
    fun returnToLobby() {
        lobby.inGame = false
        lobby.players.filter { !it.host }.forEach { it.ready = false }
        lobbyUpdated()
    }

    fun sendTo(playerId: Int, message: JSONObject) = clients.filter { it.id == playerId }.forEach { it.channel.send(message) }
    fun broadcast(message: JSONObject) = clients.filter { it.id >= 0 }.forEach { it.channel.send(message) }

    // -------------------------------------------------------------------------

    private fun onNewConnection(socket: java.net.Socket) {
        lateinit var client: Client
        val channel = JsonChannel(socket, scope, main, { onMessage(client, it) }, { onDisconnected(client) })
        client = Client(-1, channel)
        clients += client
        // Un client qui ne se présente pas est déconnecté.
        scope.launch(main) {
            delay(5000)
            if (client.id < 0 && client in clients) reject(client, "Délai de connexion dépassé.")
        }
    }

    private fun reject(client: Client, reason: String) {
        client.channel.send(JSONObject().put("type", Msg.Reject).put("reason", reason))
        client.channel.close()
        clients.remove(client)
    }

    private fun onMessage(client: Client, message: JSONObject) {
        val type = message.optString("type")
        if (client.id < 0) {
            if (type == Msg.Hello) handleHello(client, message)
            else if (type == Msg.Probe) {
                // Découverte par balayage TCP : description du lobby puis fermeture.
                client.channel.send(announcementJson().put("type", Msg.Info))
                client.channel.close()
                clients.remove(client)
            }
            return
        }
        val player = lobby.player(client.id) ?: return
        when (type) {
            Msg.SelectTop -> {
                if (lobby.inGame) return
                val list = message.optJSONArray("tops")
                player.tops = if (list == null) emptyList() else
                    (0 until list.length()).mapNotNull { Top.fromJson(list.optJSONObject(it)) }.take(3)
                player.ready = false
                lobbyUpdated()
            }
            Msg.Ready -> {
                if (lobby.inGame || player.tops.size < lobby.topsNeeded) return
                player.ready = message.optBoolean("ready")
                lobbyUpdated()
            }
            Msg.Aim, Msg.Dash, Msg.Special, Msg.Steer, Msg.Duel, Msg.Order, Msg.Forfeit ->
                if (lobby.inGame) gameMessage.toList().forEach { it(client.id, message) }
        }
    }

    private fun handleHello(client: Client, message: JSONObject) {
        when {
            message.optInt("version") != Protocol.VERSION || message.optString("app") != Protocol.APP_TAG ->
                return reject(client, "Version du jeu incompatible avec celle de l'hôte.")
            message.optString("catalog") != PartCatalog.fingerprint ->
                return reject(client, "Catalogue de pièces différent de celui de l'hôte.")
            lobby.inGame -> return reject(client, "Une partie est déjà en cours dans ce lobby.")
            lobby.players.size >= lobby.maxPlayers ->
                return reject(client, "Le lobby est complet (${lobby.maxPlayers} joueurs).")
        }
        val id = nextId++
        val name = uniqueName(Protocol.sanitizeName(message.optString("name"), "Joueur ${id + 1}"))
        client.id = id
        lobby.players += LobbyPlayer(id, name)
        client.channel.send(JSONObject().put("type", Msg.Welcome).put("playerId", id))
        lobbyUpdated()
    }

    private fun onDisconnected(client: Client) {
        if (!clients.remove(client)) return
        if (client.id >= 0) {
            lobby.players.removeAll { it.id == client.id }
            playerLeft.toList().forEach { it(client.id) }
            lobbyUpdated()
        }
    }

    private fun uniqueName(name: String): String {
        fun taken(c: String) = lobby.players.any { it.name.equals(c, ignoreCase = true) }
        var candidate = name
        var i = 2
        while (taken(candidate)) candidate = "$name (${i++})"
        return candidate
    }

    private fun lobbyUpdated() {
        broadcast(JSONObject().put("type", Msg.Lobby).put("lobby", lobby.toJson()))
        notifyChanged()
    }

    private fun notifyChanged() = lobbyChanged.toList().forEach { it() }

    private fun announcementJson(): JSONObject = JSONObject()
        .put("app", Protocol.APP_TAG).put("v", Protocol.VERSION).put("id", lobby.lobbyId).put("name", lobby.name)
        .put("host", lobby.player(HOST_ID)?.name ?: "").put("port", port).put("players", lobby.players.size)
        .put("max", lobby.maxPlayers).put("stadium", lobby.stadiumId).put("inGame", lobby.inGame)
        .put("tournament", lobby.tournament != null)

    companion object {
        const val HOST_ID = 0

        /** Diffusion générale, diffusion dirigée sur chaque interface et boucle locale. */
        fun broadcastTargets(): List<InetAddress> {
            val targets = linkedSetOf(InetAddress.getByName("255.255.255.255"), InetAddress.getLoopbackAddress())
            runCatching {
                for (iface in NetworkInterface.getNetworkInterfaces()) {
                    if (!iface.isUp || iface.isLoopback) continue
                    iface.interfaceAddresses.mapNotNullTo(targets) { it.broadcast }
                }
            }
            return targets.toList()
        }
    }
}
