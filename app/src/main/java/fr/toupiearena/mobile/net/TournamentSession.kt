package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Un tournoi vu depuis un joueur : dirigé ici (solo, hôte) ou suivi (client). */
abstract class TournamentController(protected val localPlayerId: Int) {
    /** Match humain du tour courant, en préparation ou en cours. */
    data class LiveMatch(val match: Int, val series: MatchSeries, val ready: BooleanArray)

    var tournament: Tournament? = null
        protected set
    var live: List<LiveMatch> = emptyList()
        protected set
    private var pendingSession: MatchSession? = null

    var onChanged: () -> Unit = {}
    /** Nouvelle manche pour le joueur local (récupérer avec takeSession()). */
    var onSessionReady: () -> Unit = {}
    var onLocalMatchOver: () -> Unit = {}

    fun liveOf(match: Int) = live.firstOrNull { it.match == match }
    val localParticipant get() = tournament?.participantOf(localPlayerId) ?: -1

    /** Match du tour courant que le joueur local doit encore jouer (-1 : aucun). */
    val localMatch: Int
        get() {
            val t = tournament ?: return -1
            val m = t.matchOf(localParticipant)
            return if (m < 0 || t.rounds[t.currentRound][m].played) -1 else m
        }

    val localReady: Boolean
        get() {
            val t = tournament ?: return false
            val m = localMatch
            val l = liveOf(m) ?: return false
            return l.ready[if (t.rounds[t.currentRound][m].a == localParticipant) 0 else 1]
        }

    abstract fun submitOrder(order: List<Int>)
    abstract fun forfeitLocal()
    open fun close() {}

    fun takeSession(): MatchSession? = pendingSession.also { pendingSession = null }
    protected fun offerSession(session: MatchSession): MatchSession? {
        pendingSession = session
        onSessionReady()
        return pendingSession.also { pendingSession = null } // Non récupérée : reste ici.
    }
}

/**
 * Fait tourner un tournoi : simule les matchs entre bots, joue les manches des
 * matchs humains (en parallèle en réseau) et enchaîne les tours. Avec un
 * serveur, diffuse l'état aux clients et lance leurs manches.
 */
class TournamentDirector(
    private val server: LobbyServer?,
    tournament: Tournament,
    seed: Long,
    private val scope: CoroutineScope,
    private val main: CoroutineDispatcher,
) : TournamentController(LobbyServer.HOST_ID) {
    private class Match(val id: Int, val index: Int, val series: MatchSeries, val ready: BooleanArray) {
        var owned: MatchSession? = null // Manche simulée ici (sinon affichée par l'interface).
        var session: MatchSession? = null
    }

    private val rng = java.util.Random(seed)
    private val matches = mutableListOf<Match>()
    private var nextId = 1
    private var advancing = false
    private val ticker: Job
    private val onGame: (Int, JSONObject) -> Unit = { id, m -> onGameMessage(id, m) }
    private val onLeft: (Int) -> Unit = ::onPlayerLeft

    init {
        this.tournament = tournament
        server?.gameMessage?.add(onGame)
        server?.playerLeft?.add(onLeft)
        // Les manches qui ne sont pas affichées ici sont simulées par ce minuteur.
        ticker = scope.launch(main) {
            var last = System.nanoTime()
            while (isActive) {
                delay(16)
                val now = System.nanoTime()
                val dt = minOf(0.05, (now - last) / 1e9)
                last = now
                matches.mapNotNull { it.owned }.toList().forEach { it.update(dt) }
            }
        }
        startRound()
    }

    private val t get() = tournament!!
    private fun round() = t.rounds[t.currentRound]

    override fun close() {
        ticker.cancel()
        matches.forEach { it.owned?.close() }
        matches.clear()
        server?.gameMessage?.remove(onGame)
        server?.playerLeft?.remove(onLeft)
    }

    private fun startRound() {
        matches.forEach { it.owned?.close() }
        matches.clear()
        val settings = t.settings
        val stadium = Stadium.find(settings.stadiumId) ?: Stadium.classic
        val tops = MatchSeries.topsFor(settings.matchFormat)
        round().forEachIndexed { i, m ->
            if (m.played) return@forEachIndexed
            val a = t.participants[m.a]
            val b = t.participants[m.b]
            if (a.isBot && b.isBot) {
                t.record(i, Tournament.playBotMatch(a, b, settings.matchFormat, stadium, settings.rules, rng.nextLong()))
                return@forEachIndexed
            }
            val series = MatchSeries(settings.matchFormat)
            val ready = BooleanArray(2)
            for (side in 0..1) if ((if (side == 0) a else b).isBot) {
                series.order[side] = Tournament.randomOrder(tops, rng.nextLong())
                ready[side] = true
            }
            matches += Match(nextId++, i, series, ready)
        }
        publish()
        scheduleNextRound()
    }

    private fun scheduleNextRound() {
        if (advancing || !t.roundComplete || t.isFinished) return
        advancing = true
        scope.launch(main) {
            delay(1200)
            advancing = false
            t.nextRound()
            startRound()
        }
    }

    override fun submitOrder(order: List<Int>) = setReady(localParticipant, order)

    private fun findByParticipant(p: Int) = matches.firstOrNull { val m = round()[it.index]; p >= 0 && (m.a == p || m.b == p) }

    private fun setReady(participant: Int, order: List<Int>) {
        val match = findByParticipant(participant) ?: return
        if (match.session != null) return
        val side = if (round()[match.index].a == participant) 0 else 1
        if (match.ready[side]) return
        val count = t.participants[participant].tops.size
        match.series.order[side] = if (order.sorted() == (0 until count).toList()) order else (0 until count).toList()
        match.ready[side] = true
        publish()
        if (match.ready[0] && match.ready[1]) playNextBattle(match)
    }

    private fun playNextBattle(match: Match) {
        match.owned?.close()
        match.owned = null
        val settings = t.settings
        val tm = round()[match.index]
        val people = listOf(t.participants[tm.a], t.participants[tm.b])
        val setup = MatchSetup(
            Stadium.find(settings.stadiumId) ?: Stadium.classic, settings.rules, rng.nextLong() and 0xffffffffL,
            people.map { it.playerId }, people.map { it.name },
            people.mapIndexed { side, p -> p.tops.getOrElse(match.series.topFor(side)) { p.tops[0] } },
            people.map { if (it.isBot) it.level.ordinal else -1 }, match.index, match.series.deepCopy(),
        )
        // Le message de départ précède le premier état envoyé par la session.
        if (server != null) {
            val start = MatchCodec.encodeStart(setup)
            setup.playerIds.filter { it >= 0 && it != LobbyServer.HOST_ID }.forEach { server.sendTo(it, start) }
        }
        val session = HostSession(server, setup)
        match.session = session
        val id = match.id
        session.onPhaseChanged { if (it == MatchPhase.Finished) onBattleFinished(id) }
        match.owned = if (LobbyServer.HOST_ID in setup.playerIds) offerSession(session) else session
    }

    private fun onBattleFinished(id: Int) {
        val match = matches.firstOrNull { it.id == id } ?: return
        val state = match.session?.state ?: return
        match.series.recordRound(state.winner, state.endReason)
        publish()
        scope.launch(main) {
            delay(PAUSE_MS)
            val m = matches.firstOrNull { it.id == id } ?: return@launch
            if (m.series.isOver) finishMatch(id) else playNextBattle(m)
        }
    }

    override fun forfeitLocal() = forfeit(localParticipant)

    private fun forfeit(participant: Int) {
        val match = findByParticipant(participant) ?: return
        match.series.forfeit(if (round()[match.index].a == participant) 0 else 1)
        finishMatch(match.id)
    }

    private fun finishMatch(id: Int) {
        val match = matches.firstOrNull { it.id == id } ?: return
        val tm = round()[match.index]
        val local = localParticipant
        val wasLocal = local >= 0 && (tm.a == local || tm.b == local)
        t.record(match.index, match.series)
        match.owned?.close()
        matches.remove(match)
        publish()
        if (wasLocal) onLocalMatchOver()
        scheduleNextRound()
    }

    private fun publish() {
        live = matches.map { LiveMatch(it.index, it.series.deepCopy(), it.ready.copyOf()) }
        onChanged()
        server?.broadcast(JSONObject().put("type", Msg.Tournament).put("t", t.toJson()).put("live",
            JSONArray(live.map { JSONObject().put("m", it.match).put("series", it.series.toJson())
                .put("r", JSONArray(listOf(it.ready[0], it.ready[1]))) })))
    }

    private fun onGameMessage(playerId: Int, message: JSONObject) {
        val participant = t.participantOf(playerId)
        when (message.optString("type")) {
            Msg.Order -> {
                val a = message.optJSONArray("order") ?: JSONArray()
                setReady(participant, (0 until a.length()).map { a.optInt(it) })
            }
            Msg.Forfeit -> forfeit(participant)
        }
    }

    private fun onPlayerLeft(playerId: Int) {
        // Le joueur parti est remplacé par un bot pour la suite du tournoi.
        val participant = t.participantOf(playerId)
        if (participant < 0) return
        val p = t.participants[participant]
        p.playerId = -1
        p.level = Difficulty.Normal
        setReady(participant, Tournament.randomOrder(p.tops.size, rng.nextLong()))
        publish()
    }

    companion object { const val PAUSE_MS = 3500L }
}

/** Client réseau : suit le tournoi de l'hôte et joue ses manches. */
class TournamentFollower(private val client: LobbyClient) : TournamentController(client.playerId) {
    private var playing = -1
    private val onGame: (JSONObject) -> Unit = { if (it.optString("type") == Msg.Tournament) apply(it) }
    private val onStart: (JSONObject) -> Unit = { start ->
        val setup = MatchCodec.parseStart(start)
        if (setup != null && setup.match >= 0) {
            playing = setup.match
            offerSession(ClientSession(client, setup))
        }
    }

    init {
        client.gameMessage += onGame
        client.matchStarted += onStart
    }

    override fun close() {
        client.gameMessage -= onGame
        client.matchStarted -= onStart
    }

    fun apply(message: JSONObject) {
        tournament = Tournament.fromJson(message.optJSONObject("t")) ?: return
        val array = message.optJSONArray("live") ?: JSONArray()
        live = (0 until array.length()).mapNotNull { array.optJSONObject(it) }.map { o ->
            val r = o.optJSONArray("r")
            LiveMatch(o.optInt("m", -1), MatchSeries.fromJson(o.optJSONObject("series")),
                booleanArrayOf(r?.optBoolean(0) ?: false, r?.optBoolean(1) ?: false))
        }
        onChanged()
        if (playing >= 0 && liveOf(playing) == null) {
            playing = -1
            onLocalMatchOver()
        }
    }

    override fun submitOrder(order: List<Int>) = client.send(JSONObject().put("type", Msg.Order).put("order", JSONArray(order)))
    override fun forfeitLocal() = client.send(JSONObject().put("type", Msg.Forfeit))
}
