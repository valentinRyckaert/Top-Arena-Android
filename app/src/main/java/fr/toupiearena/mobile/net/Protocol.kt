package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.TournamentSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.Socket

/**
 * Protocole du multijoueur en réseau local, identique à la version PC (v4) :
 * annonces UDP + questions/réponses (port 45454), lobby et partie en TCP
 * (45455 et suivants), un message JSON compact par ligne avec un champ "type".
 */
object Protocol {
    const val VERSION = 4
    const val DEFAULT_TCP_PORT = 45455
    const val DISCOVERY_PORT = 45454
    const val PORT_ATTEMPTS = 10
    const val MAX_NAME_LENGTH = 20
    const val STATE_RATE = 30.0
    const val AIM_TIMEOUT = 30.0
    const val APP_TAG = "ToupieArena"

    /** Nettoie un pseudo ou un nom de lobby (espaces, longueur). */
    fun sanitizeName(name: String?, fallback: String): String {
        val cleaned = (name ?: "").replace(Regex("\\s+"), " ").trim().take(MAX_NAME_LENGTH).trim()
        return cleaned.ifEmpty { fallback }
    }
}

object Msg {
    // Client -> hôte
    const val Hello = "hello"
    const val Probe = "probe"
    const val Query = "query"
    const val SelectTop = "top"
    const val Ready = "ready"
    const val Aim = "aim"
    const val Dash = "dash"
    const val Special = "special"
    const val Steer = "steer"
    const val Duel = "duel"
    const val Order = "order"
    const val Forfeit = "forfeit"
    // Hôte -> client
    const val Welcome = "welcome"
    const val Info = "info"
    const val Reject = "reject"
    const val Lobby = "lobby"
    const val Start = "start"
    const val State = "state"
    const val Closed = "closed"
    const val Tournament = "tournament"
}

data class LobbyPlayer(
    val id: Int,
    val name: String,
    var tops: List<Top> = emptyList(),
    var ready: Boolean = false,
    val host: Boolean = false,
)

data class LobbyInfo(
    var lobbyId: String = "",
    var name: String = "",
    var maxPlayers: Int = 2,
    var stadiumId: String = "",
    var inGame: Boolean = false,
    var rules: MatchRules = MatchRules(),
    val players: MutableList<LobbyPlayer> = mutableListOf(),
    var tournament: TournamentSettings? = null,
) {
    fun player(id: Int) = players.firstOrNull { it.id == id }
    /** Nombre de toupies que chaque joueur doit choisir. */
    val topsNeeded get() = tournament?.let { MatchSeries.topsFor(it.matchFormat) } ?: 1

    fun toJson(): JSONObject {
        val json = JSONObject().put("id", lobbyId).put("name", name).put("max", maxPlayers)
            .put("stadium", stadiumId).put("inGame", inGame).put("rules", rules.toJson())
            .put("players", JSONArray(players.map {
                JSONObject().put("id", it.id).put("name", it.name).put("ready", it.ready).put("host", it.host)
                    .put("tops", JSONArray(it.tops.map(Top::toJson)))
            }))
        tournament?.let { json.put("tournament", it.toJson()) }
        return json
    }

    companion object {
        fun fromJson(json: JSONObject?): LobbyInfo {
            val j = json ?: JSONObject()
            val players = j.optJSONArray("players") ?: JSONArray()
            return LobbyInfo(
                j.optString("id"), j.optString("name"), j.optInt("max", 2), j.optString("stadium"),
                j.optBoolean("inGame"), MatchRules.fromJson(j.optJSONObject("rules")),
                (0 until players.length()).mapNotNull { players.optJSONObject(it) }.map { o ->
                    val tops = o.optJSONArray("tops") ?: JSONArray()
                    LobbyPlayer(o.optInt("id", -1), o.optString("name"),
                        (0 until tops.length()).mapNotNull { Top.fromJson(tops.optJSONObject(it)) },
                        o.optBoolean("ready"), o.optBoolean("host"))
                }.toMutableList(),
                if (j.has("tournament")) TournamentSettings.fromJson(j.optJSONObject("tournament")) else null,
            )
        }
    }
}

/** Lobby découvert sur le réseau. */
data class LobbyAnnouncement(
    val lobbyId: String,
    val name: String,
    val hostName: String,
    val stadiumId: String,
    val address: InetAddress,
    val port: Int,
    val players: Int,
    val maxPlayers: Int,
    val inGame: Boolean,
    val tournament: Boolean,
    var lastSeenMs: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Annonce UDP, réponse à une question ou réponse « info » à une sonde TCP. */
        fun parse(o: JSONObject, from: InetAddress): LobbyAnnouncement? {
            if (o.optString("app") != Protocol.APP_TAG || o.optInt("v") != Protocol.VERSION) return null
            val id = o.optString("id")
            val port = o.optInt("port")
            if (id.isEmpty() || port == 0) return null
            return LobbyAnnouncement(id, o.optString("name"), o.optString("host"), o.optString("stadium"), from,
                port, o.optInt("players"), o.optInt("max"), o.optBoolean("inGame"), o.optBoolean("tournament"))
        }
    }
}

/**
 * Canal de messages JSON (un par ligne) au-dessus d'une socket TCP. Lecture et
 * écriture dans des coroutines d'entrée/sortie ; les messages sont remis sur
 * le répartiteur `main` (le fil de la logique de jeu), dans l'ordre.
 */
class JsonChannel(
    private val socket: Socket,
    private val scope: CoroutineScope,
    private val main: CoroutineDispatcher,
    private val onMessage: (JSONObject) -> Unit,
    private val onClosed: () -> Unit,
) {
    private val outgoing = Channel<String>(Channel.UNLIMITED)
    @Volatile private var closed = false

    init {
        scope.launch(Dispatchers.IO) {
            runCatching {
                socket.getOutputStream().bufferedWriter(Charsets.UTF_8).let { out ->
                    for (line in outgoing) {
                        out.write(line)
                        out.write("\n")
                        out.flush()
                    }
                }
            }
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.length > 1_000_000) break // Message aberrant : on coupe.
                    val json = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    withContext(main) { if (!closed) onMessage(json) }
                }
            }
            withContext(main) { closeInternal(notify = true) }
        }
    }

    val remoteAddress: InetAddress? get() = socket.inetAddress

    fun send(message: JSONObject) {
        if (!closed) outgoing.trySend(message.toString())
    }

    /** Ferme après l'envoi des messages en attente. */
    fun close() = closeInternal(notify = false)

    private fun closeInternal(notify: Boolean) {
        if (closed) return
        closed = true
        outgoing.close()
        scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(200) // Laisse partir les derniers messages.
            runCatching { socket.close() }
        }
        if (notify) onClosed()
    }
}
