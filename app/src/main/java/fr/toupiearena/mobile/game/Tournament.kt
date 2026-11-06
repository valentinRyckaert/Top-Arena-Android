package fr.toupiearena.mobile.game

import fr.toupiearena.mobile.core.Top
import org.json.JSONArray
import org.json.JSONObject

enum class TournamentFormat(val label: String) { League("Ligue"), Knockout("Élimination directe") }

/** Express : une toupie, premier à 2 manches. Classique : trois toupies dans l'ordre choisi, premier à 4 points. */
enum class MatchFormat(val label: String) { Express("Express"), Classic("Classique") }

/** Score d'un match de tournoi, joué en plusieurs manches. */
data class MatchSeries(
    val format: MatchFormat = MatchFormat.Express,
    val score: IntArray = intArrayOf(0, 0),
    var rounds: Int = 0,
    val order: Array<List<Int>> = arrayOf(emptyList(), emptyList()),
) {
    val target get() = if (format == MatchFormat.Classic) 4 else 2

    fun pointsFor(reason: Battle.EndReason) = when {
        reason == Battle.EndReason.Forfeit -> target
        format == MatchFormat.Express -> 1
        reason == Battle.EndReason.RingOut -> 2
        else -> 1
    }

    /** Enregistre une manche (`winnerSide` 0 ou 1, -1 pour un nul). */
    fun recordRound(winnerSide: Int, reason: Battle.EndReason) {
        if (isOver) return
        rounds++
        if (winnerSide == 0 || winnerSide == 1) score[winnerSide] += pointsFor(reason)
        if (rounds >= MAX_ROUNDS && !isOver) score[if (score[1] > score[0]) 1 else 0] = target
    }

    /** Abandon du camp `side` : l'adversaire gagne le match. */
    fun forfeit(side: Int) {
        val other = 1 - side
        score[other] = maxOf(score[other], target)
    }

    val winner get() = if (score[0] >= target) 0 else if (score[1] >= target) 1 else -1
    val isOver get() = winner >= 0

    /** Indice de la toupie jouée par `side` à la prochaine manche. */
    fun topFor(side: Int): Int = order[side].let { if (it.isEmpty()) 0 else it[rounds % it.size] }

    fun deepCopy() = copy(score = score.copyOf(), order = order.copyOf())

    fun toJson(): JSONObject = JSONObject()
        .put("format", format.ordinal)
        .put("score", JSONArray().put(score[0]).put(score[1]))
        .put("rounds", rounds)
        .put("order", JSONArray().put(JSONArray(order[0])).put(JSONArray(order[1])))

    companion object {
        /** Garde-fou contre une suite improbable de manches nulles. */
        const val MAX_ROUNDS = 30
        fun topsFor(format: MatchFormat) = if (format == MatchFormat.Classic) 3 else 1

        fun fromJson(json: JSONObject?): MatchSeries {
            val j = json ?: JSONObject()
            val format = if (j.optInt("format") == 1) MatchFormat.Classic else MatchFormat.Express
            val score = j.optJSONArray("score")
            val order = j.optJSONArray("order")
            fun side(k: Int): List<Int> {
                val a = order?.optJSONArray(k) ?: return emptyList()
                return (0 until a.length()).map { a.optInt(it).coerceIn(0, topsFor(format) - 1) }
            }
            return MatchSeries(format, intArrayOf(score?.optInt(0) ?: 0, score?.optInt(1) ?: 0),
                maxOf(0, j.optInt("rounds")), arrayOf(side(0), side(1)))
        }
    }
}

/** Participant : joueur humain (identifiant réseau, 0 pour l'hôte ou le joueur solo) ou bot (-1). */
data class Participant(
    val name: String,
    var playerId: Int = -1,
    var level: Difficulty = Difficulty.Normal,
    val tops: List<Top> = emptyList(),
) {
    val isBot get() = playerId < 0
}

data class TournamentMatch(
    val a: Int,
    val b: Int, // -1 : exempt
    val score: IntArray = intArrayOf(0, 0),
    var played: Boolean = false,
) {
    val isBye get() = b < 0
    val winner: Int
        get() = when {
            !played -> -1
            isBye -> a
            score[0] == score[1] -> -1
            score[0] > score[1] -> a
            else -> b
        }
}

data class TournamentSettings(
    val format: TournamentFormat = TournamentFormat.League,
    val matchFormat: MatchFormat = MatchFormat.Express,
    val participants: Int = 8,
    val bots: List<Difficulty> = emptyList(),
    val stadiumId: String = "",
    val rules: MatchRules = MatchRules(),
) {
    /** Ajuste le nombre de bots pour `humans` joueurs humains (nouveaux bots : normal). */
    fun fitBots(humans: Int): TournamentSettings {
        val count = maxOf(0, participants - humans)
        return copy(bots = bots.take(count) + List(maxOf(0, count - bots.size)) { Difficulty.Normal })
    }

    fun toJson(): JSONObject = JSONObject()
        .put("format", format.ordinal).put("match", matchFormat.ordinal).put("participants", participants)
        .put("bots", JSONArray(bots.map { it.ordinal })).put("stadium", stadiumId).put("rules", rules.toJson())

    companion object {
        fun fromJson(json: JSONObject?): TournamentSettings {
            val j = json ?: JSONObject()
            val participants = j.optInt("participants", 8).coerceIn(Tournament.MIN_PARTICIPANTS, Tournament.MAX_PARTICIPANTS)
            val bots = j.optJSONArray("bots")?.let { a ->
                (0 until a.length()).map { Difficulty.entries[a.optInt(it, 1).coerceIn(0, 2)] }
            }.orEmpty().take(participants)
            return TournamentSettings(
                if (j.optInt("format") == 1) TournamentFormat.Knockout else TournamentFormat.League,
                if (j.optInt("match") == 1) MatchFormat.Classic else MatchFormat.Express,
                participants, bots, j.optString("stadium"), MatchRules.fromJson(j.optJSONObject("rules")),
            )
        }
    }
}

data class Standing(
    val participant: Int,
    var played: Int = 0,
    var won: Int = 0,
    var lost: Int = 0,
    var points: Int = 0,
    var against: Int = 0,
)

/**
 * Tournoi : ligue (chacun rencontre chacun une fois, le classement additionne
 * les manches ou points gagnés) ou élimination directe (tableau avec exemptions).
 */
class Tournament private constructor(
    val settings: TournamentSettings,
    val participants: MutableList<Participant>,
    val rounds: MutableList<List<TournamentMatch>>,
    currentRound: Int,
) {
    var currentRound = currentRound
        private set

    val roundCount: Int
        get() {
            val n = participants.size
            if (settings.format == TournamentFormat.League) return if (n % 2 == 0) n - 1 else n
            var r = 0
            var size = nextPowerOfTwo(n)
            while (size > 1) { size /= 2; r++ }
            return r
        }

    fun roundName(round: Int): String {
        if (settings.format == TournamentFormat.League) return "Journée ${round + 1}/$roundCount"
        return when (roundCount - round) {
            1 -> "Finale"
            2 -> "Demi-finales"
            3 -> "Quarts de finale"
            4 -> "Huitièmes de finale"
            else -> "Seizièmes de finale"
        }
    }

    fun participantOf(playerId: Int) = if (playerId < 0) -1 else participants.indexOfFirst { it.playerId == playerId }

    /** Match du tour courant de `participant` (-1 : aucun). */
    fun matchOf(participant: Int): Int {
        if (participant < 0 || currentRound >= rounds.size) return -1
        return rounds[currentRound].indexOfFirst { !it.isBye && (it.a == participant || it.b == participant) }
    }

    fun record(match: Int, series: MatchSeries) {
        val round = rounds.getOrNull(currentRound) ?: return
        val m = round.getOrNull(match) ?: return
        m.score[0] = series.score[0]
        m.score[1] = series.score[1]
        m.played = true
    }

    val roundComplete get() = rounds.getOrNull(currentRound)?.all { it.played } ?: false
    val isFinished get() = currentRound == roundCount - 1 && roundComplete

    fun nextRound() {
        if (!roundComplete || isFinished) return
        if (settings.format == TournamentFormat.Knockout) {
            val previous = rounds[currentRound]
            rounds += (0 until previous.size / 2).map { TournamentMatch(previous[2 * it].winner, previous[2 * it + 1].winner) }
        }
        currentRound++
    }

    val champion: Int
        get() = when {
            !isFinished -> -1
            settings.format == TournamentFormat.Knockout -> rounds.last().first().winner
            else -> standings().first().participant
        }

    /** Classement : points, puis victoires, puis différence. */
    fun standings(): List<Standing> {
        val table = participants.indices.map { Standing(it) }
        for (round in rounds) for (m in round) {
            if (!m.played || m.isBye) continue
            val winner = m.winner
            for (side in 0..1) {
                val s = table[if (side == 0) m.a else m.b]
                s.played++
                s.points += m.score[side]
                s.against += m.score[1 - side]
                if (winner >= 0) if (winner == s.participant) s.won++ else s.lost++
            }
        }
        return table.sortedWith(compareByDescending<Standing> { it.points }.thenByDescending { it.won }
            .thenByDescending { it.points - it.against })
    }

    fun toJson(): JSONObject {
        val people = JSONArray()
        for (p in participants) {
            people.put(JSONObject().put("name", p.name).put("id", p.playerId).put("level", p.level.ordinal)
                .put("tops", JSONArray(p.tops.map { it.toJson() })))
        }
        val allRounds = JSONArray()
        for (round in rounds) {
            allRounds.put(JSONArray(round.map { JSONArray(listOf(it.a, it.b, it.score[0], it.score[1], if (it.played) 1 else 0)) }))
        }
        return JSONObject().put("settings", settings.toJson()).put("participants", people)
            .put("rounds", allRounds).put("current", currentRound)
    }

    companion object {
        const val MIN_PARTICIPANTS = 4
        const val MAX_PARTICIPANTS = 32
        const val MAX_HUMANS = 4

        private fun nextPowerOfTwo(n: Int): Int {
            var size = 1
            while (size < n) size *= 2
            return size
        }

        /** Les participants sont tirés au sort avec `seed`. */
        fun create(settings: TournamentSettings, participants: List<Participant>, seed: Long): Tournament {
            val order = participants.indices.toMutableList<Int>().apply { shuffle(java.util.Random(seed)) }
            val rounds = mutableListOf<List<TournamentMatch>>()
            if (settings.format == TournamentFormat.League) {
                // Méthode du cercle : le premier reste fixe, les autres tournent.
                if (order.size % 2 != 0) order += -1
                val n = order.size
                repeat(n - 1) {
                    rounds += (0 until n / 2).map { i ->
                        var a = order[i]
                        var b = order[n - 1 - i]
                        if (a < 0) { a = b; b = -1 }
                        TournamentMatch(a, b, played = b < 0)
                    }
                    order.add(1, order.removeAt(order.size - 1))
                }
            } else {
                // Exemptions en fin de tirage : elles tombent face à des participants réels.
                val size = nextPowerOfTwo(order.size)
                while (order.size < size) order += -1
                rounds += (0 until size / 2).map { i ->
                    TournamentMatch(order[i], order[size - 1 - i], played = order[size - 1 - i] < 0)
                }
            }
            return Tournament(settings, participants.toMutableList(), rounds, 0)
        }

        fun fromJson(json: JSONObject?): Tournament? {
            json ?: return null
            val settings = TournamentSettings.fromJson(json.optJSONObject("settings"))
            val people = json.optJSONArray("participants") ?: return null
            val participants = mutableListOf<Participant>()
            for (i in 0 until people.length()) {
                val o = people.optJSONObject(i) ?: return null
                val topsJson = o.optJSONArray("tops") ?: return null
                val tops = (0 until topsJson.length()).map { Top.fromJson(topsJson.optJSONObject(it)) ?: return null }
                if (tops.isEmpty()) return null
                participants += Participant(o.optString("name"), o.optInt("id", -1),
                    Difficulty.entries[o.optInt("level", 1).coerceIn(0, 2)], tops)
            }
            val count = participants.size
            if (count !in MIN_PARTICIPANTS..MAX_PARTICIPANTS) return null
            val roundsJson = json.optJSONArray("rounds") ?: return null
            val rounds = mutableListOf<List<TournamentMatch>>()
            for (r in 0 until roundsJson.length()) {
                val matches = roundsJson.optJSONArray(r) ?: return null
                rounds += (0 until matches.length()).map { k ->
                    val a = matches.optJSONArray(k) ?: return null
                    val m = TournamentMatch(a.optInt(0, -1), a.optInt(1, -1),
                        intArrayOf(a.optInt(2), a.optInt(3)), a.optInt(4) != 0)
                    if (m.a < 0 || m.a >= count || m.b >= count) return null
                    m
                }
            }
            if (rounds.isEmpty()) return null
            return Tournament(settings, participants, rounds, json.optInt("current").coerceIn(0, rounds.size - 1))
        }

        /** Ordre de passage aléatoire des `count` toupies d'un bot. */
        fun randomOrder(count: Int, seed: Long) = (0 until count).toMutableList().apply { shuffle(java.util.Random(seed)) }.toList()

        /** Complète les humains avec les bots des réglages (toupies différentes tirées au hasard). */
        fun addBots(humans: List<Participant>, settings: TournamentSettings, pool: List<Top>, seed: Long): List<Participant> {
            val rng = java.util.Random(seed)
            val tops = MatchSeries.topsFor(settings.matchFormat)
            return humans + settings.bots.mapIndexed { i, level ->
                val picked = randomOrder(pool.size, rng.nextLong()).take(tops).map { pool[it] }
                Participant("Bot ${i + 1}", -1, level, picked + List(maxOf(0, tops - picked.size)) { pool.first() })
            }
        }

        /** Joue entièrement (sans affichage) un match de tournoi entre deux bots. */
        fun playBotMatch(a: Participant, b: Participant, format: MatchFormat, stadium: Stadium, rules: MatchRules, seed: Long): MatchSeries {
            val series = MatchSeries(format, order = arrayOf(randomOrder(a.tops.size, seed), randomOrder(b.tops.size, seed + 1)))
            val rng = java.util.Random(seed)
            while (!series.isOver) {
                val battle = Battle(listOf(a.tops.getOrElse(series.topFor(0)) { a.tops[0] },
                    b.tops.getOrElse(series.topFor(1)) { b.tops[0] }), stadium, rules.apply(), rng.nextLong())
                val bots = listOf(BotController(0, a.level, rng.nextLong()), BotController(1, b.level, rng.nextLong()))
                battle.launch(bots.map { it.chooseLaunchAngle(battle) }, bots.map { it.chooseLaunchPower(battle) })
                while (!battle.isFinished) {
                    bots.forEach { it.update(battle, 1.0 / 60.0) }
                    battle.advance(1.0 / 60.0)
                }
                series.recordRound(battle.winner, battle.endReason)
            }
            return series
        }
    }
}
