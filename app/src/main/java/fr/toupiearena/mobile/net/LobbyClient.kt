package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

/** Connexion d'un joueur au lobby d'un hôte (PC ou mobile). */
class LobbyClient(private val scope: CoroutineScope, private val main: CoroutineDispatcher) {
    private var channel: JsonChannel? = null
    private var connectJob: Job? = null
    private var closeReason = ""
    var playerId = -1
        private set
    var lobby = LobbyInfo()
        private set
    var hostLabel = ""
        private set
    val isJoined get() = playerId >= 0

    var onJoined: () -> Unit = {}
    val lobbyChanged = mutableListOf<() -> Unit>()
    val matchStarted = mutableListOf<(JSONObject) -> Unit>()
    /** Messages « state » et « tournament » de l'hôte. */
    val gameMessage = mutableListOf<(JSONObject) -> Unit>()
    /** Échec de connexion ou refus (avant d'avoir rejoint). */
    var onJoinFailed: (String) -> Unit = {}
    /** Perte de connexion après avoir rejoint. */
    var onConnectionLost: (String) -> Unit = {}

    fun connectToHost(host: String, port: Int, playerName: String) {
        disconnect()
        hostLabel = "$host:$port"
        closeReason = ""
        connectJob = scope.launch(main) {
            val socket = withContext(Dispatchers.IO) {
                runCatching { Socket().apply { connect(InetSocketAddress(host, port), 5000); tcpNoDelay = true } }
            }.getOrElse {
                fail("Connexion impossible : ${it.message ?: it.javaClass.simpleName}")
                return@launch
            }
            channel = JsonChannel(socket, scope, main, ::onMessage) {
                fail(closeReason.ifEmpty { "Connexion avec l'hôte perdue." })
            }
            send(JSONObject().put("type", Msg.Hello).put("app", Protocol.APP_TAG).put("version", Protocol.VERSION)
                .put("catalog", PartCatalog.fingerprint).put("name", playerName))
            kotlinx.coroutines.delay(5000)
            if (!isJoined && channel != null) fail("L'hôte ne répond pas.")
        }
    }

    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        channel?.close()
        channel = null
        playerId = -1
        lobby = LobbyInfo()
    }

    fun selectTops(tops: List<Top>) = send(JSONObject().put("type", Msg.SelectTop).put("tops", JSONArray(tops.map(Top::toJson))))
    fun setReady(ready: Boolean) = send(JSONObject().put("type", Msg.Ready).put("ready", ready))
    fun send(message: JSONObject) { channel?.send(message) }

    private fun fail(reason: String) {
        val wasJoined = isJoined
        disconnect()
        if (wasJoined) onConnectionLost(reason) else onJoinFailed(reason)
    }

    private fun onMessage(message: JSONObject) {
        when (message.optString("type")) {
            Msg.Welcome -> {
                playerId = message.optInt("playerId", -1)
                onJoined()
            }
            Msg.Reject -> {
                closeReason = message.optString("reason")
                fail(closeReason)
            }
            Msg.Closed -> closeReason = message.optString("reason").ifEmpty { "L'hôte a fermé le lobby." }
            Msg.Lobby -> {
                lobby = LobbyInfo.fromJson(message.optJSONObject("lobby"))
                lobbyChanged.toList().forEach { it() }
            }
            Msg.Start -> matchStarted.toList().forEach { it(message) }
            Msg.State, Msg.Tournament -> gameMessage.toList().forEach { it(message) }
        }
    }
}
