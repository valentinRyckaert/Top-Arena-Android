package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.Duel
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Projectile
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchState
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToLong

/** Contenu d'un message « start » : participants (dans l'ordre des places) et paramètres. */
data class MatchSetup(
    val stadium: Stadium = Stadium.classic,
    val rules: MatchRules = MatchRules(),
    val seed: Long = 0,
    val playerIds: List<Int> = emptyList(), // -1 : place tenue par un bot (simulé par l'hôte)
    val names: List<String> = emptyList(),
    val tops: List<Top> = emptyList(),
    val botLevels: List<Int> = emptyList(), // Difficulty de chaque place, -1 pour un humain
    val match: Int = -1,                     // Tournoi : indice du match dans le tour
    val series: MatchSeries? = null,
)

/** Encodage des messages de partie, identique à la version PC (tableaux compacts). */
object MatchCodec {
    /** Arrondi pour alléger les messages. */
    private fun r(v: Double) = (v * 10000.0).roundToLong() / 10000.0

    fun parseStart(start: JSONObject): MatchSetup? {
        val players = start.optJSONArray("players") ?: return null
        val ids = mutableListOf<Int>()
        val names = mutableListOf<String>()
        val tops = mutableListOf<Top>()
        val bots = mutableListOf<Int>()
        for (i in 0 until players.length()) {
            val o = players.optJSONObject(i) ?: return null
            tops += Top.fromJson(o.optJSONObject("top")) ?: return null
            ids += o.optInt("id", -1)
            names += o.optString("name")
            bots += o.optInt("bot", -1).coerceIn(-1, 2)
        }
        if (tops.size !in Battle.MIN_PLAYERS..Battle.MAX_PLAYERS) return null
        return MatchSetup(
            Stadium.find(start.optString("stadium")) ?: Stadium.classic,
            MatchRules.fromJson(start.optJSONObject("rules")),
            start.optLong("seed"), ids, names, tops, bots, start.optInt("match", -1),
            if (start.has("series")) MatchSeries.fromJson(start.optJSONObject("series")) else null,
        )
    }

    fun encodeStart(setup: MatchSetup): JSONObject {
        val players = JSONArray()
        setup.tops.forEachIndexed { i, top ->
            players.put(JSONObject().put("id", setup.playerIds.getOrElse(i) { -1 })
                .put("name", setup.names.getOrElse(i) { "" }).put("top", top.toJson())
                .put("bot", setup.botLevels.getOrElse(i) { -1 }))
        }
        val start = JSONObject().put("type", Msg.Start).put("stadium", setup.stadium.id).put("seed", setup.seed)
            .put("rules", setup.rules.toJson()).put("players", players)
        if (setup.match >= 0) start.put("match", setup.match)
        setup.series?.let { start.put("series", it.toJson()) }
        return start
    }

    fun encodeState(state: MatchState, impacts: List<ImpactEvent>): JSONObject {
        val spinners = JSONArray(state.spinners.map { s ->
            JSONArray(listOf(r(s.position.x), r(s.position.y), r(s.velocity.x), r(s.velocity.y), r(s.spin),
                r(s.angle), r(s.dashCooldown), r(s.boostTimer), r(s.dashTimer), s.status.ordinal,
                if (s.specialUsed) 1 else 0, r(s.slowTimer), r(s.disarmTimer)))
        })
        val projectiles = JSONArray(state.projectiles.map { p ->
            JSONArray(listOf(p.id, p.move, p.owner, if (p.ring) 1 else 0, r(p.origin.x), r(p.origin.y),
                r(p.position.x), r(p.position.y), r(p.direction.x), r(p.direction.y), r(p.radius), r(p.traveled),
                r(p.speed), r(p.range), r(p.width), p.color))
        })
        val hits = JSONArray(impacts.map { e ->
            JSONArray(listOf(r(e.point.x), r(e.point.y), r(e.intensity),
                if (e.special) (e.color or 0x1000000L) else 0L, e.duelLoser))
        })
        val duel = JSONArray()
        if (state.duel.active) {
            val d = state.duel
            listOf(d.first, d.second, r(d.point.x), r(d.point.y), r(d.normal.x), r(d.normal.y), r(d.approach))
                .forEach { duel.put(it) }
            for (side in d.sides) {
                duel.put(JSONArray(listOf(r(side.cursor), side.direction, r(side.speed), r(side.zone), side.streak,
                    if (side.done) 1 else 0, r(side.idle), side.presses)))
            }
            duel.put(r(d.elapsed))
        }
        return JSONObject().put("type", Msg.State).put("ph", state.phase.ordinal).put("cd", r(state.countdown))
            .put("el", r(state.elapsed)).put("aim", JSONArray(state.aimed)).put("w", state.winner)
            .put("why", state.endReason.ordinal).put("rank", JSONArray(state.ranking)).put("s", spinners)
            .put("p", projectiles).put("imp", hits).put("d", duel)
    }

    /** Applique un message « state » ; false s'il est incohérent (nombre de joueurs différent). */
    fun decodeState(message: JSONObject, state: MatchState, impacts: MutableList<ImpactEvent>?): Boolean {
        val spinners = message.optJSONArray("s") ?: return false
        if (spinners.length() != state.playerCount) return false
        val updated = state.spinners.mapIndexed { i, old ->
            val a = spinners.optJSONArray(i) ?: return false
            if (a.length() < 13) return false
            old.copy(
                position = Vec2(a.optDouble(0), a.optDouble(1)), velocity = Vec2(a.optDouble(2), a.optDouble(3)),
                spin = a.optDouble(4), angle = a.optDouble(5), dashCooldown = a.optDouble(6),
                boostTimer = a.optDouble(7), dashTimer = a.optDouble(8),
                status = Spinner.Status.entries[a.optInt(9).coerceIn(0, Spinner.Status.entries.size - 1)],
                specialUsed = a.optInt(10) != 0, slowTimer = a.optDouble(11), disarmTimer = a.optDouble(12),
            )
        }
        state.spinners = updated

        val projectiles = message.optJSONArray("p") ?: JSONArray()
        state.projectiles = (0 until projectiles.length()).mapNotNull { k ->
            val a = projectiles.optJSONArray(k) ?: return@mapNotNull null
            if (a.length() < 16) return@mapNotNull null
            Projectile(a.optInt(0), a.optString(1), a.optInt(2), a.optInt(3) != 0,
                Vec2(a.optDouble(4), a.optDouble(5)), Vec2(a.optDouble(6), a.optDouble(7)),
                Vec2(a.optDouble(8), a.optDouble(9)), a.optDouble(10), a.optDouble(11), a.optDouble(12),
                a.optDouble(13), a.optDouble(14), a.optLong(15))
        }

        val aimed = message.optJSONArray("aim")
        if (aimed != null) for (i in state.aimed.indices) if (i < aimed.length()) state.aimed[i] = aimed.optBoolean(i)
        state.phase = MatchPhase.entries[message.optInt("ph").coerceIn(0, MatchPhase.entries.size - 1)]
        state.countdown = message.optDouble("cd", 0.0)
        state.elapsed = message.optDouble("el", 0.0)
        state.winner = message.optInt("w", -1)
        state.endReason = Battle.EndReason.entries[message.optInt("why").coerceIn(0, Battle.EndReason.entries.size - 1)]
        val rank = message.optJSONArray("rank") ?: JSONArray()
        state.ranking = (0 until rank.length()).map { rank.optInt(it) }

        val d = message.optJSONArray("d") ?: JSONArray()
        val duel = Duel()
        if (d.length() >= 9) {
            duel.first = d.optInt(0)
            duel.second = d.optInt(1)
            duel.active = duel.first in 0 until state.playerCount && duel.second in 0 until state.playerCount
            duel.point = Vec2(d.optDouble(2), d.optDouble(3))
            duel.normal = Vec2(d.optDouble(4), d.optDouble(5))
            duel.approach = d.optDouble(6)
            for (k in 0..1) {
                val a = d.optJSONArray(7 + k) ?: continue
                val side = duel.sides[k]
                side.cursor = a.optDouble(0)
                side.direction = if (a.optDouble(1) < 0.0) -1.0 else 1.0
                side.speed = a.optDouble(2)
                side.zone = a.optDouble(3)
                side.streak = a.optInt(4)
                side.done = a.optInt(5) != 0
                side.idle = a.optDouble(6)
                side.presses = a.optInt(7)
            }
            duel.elapsed = d.optDouble(9, 0.0)
        }
        state.duel = duel

        if (impacts != null) {
            val hits = message.optJSONArray("imp") ?: JSONArray()
            for (k in 0 until hits.length()) {
                val a = hits.optJSONArray(k) ?: continue
                val code = a.optLong(3)
                impacts += ImpactEvent(Vec2(a.optDouble(0), a.optDouble(1)), a.optDouble(2), special = code != 0L,
                    color = code and 0xFFFFFFL, duelLoser = a.optInt(4, ImpactEvent.NO_DUEL))
            }
        }
        return true
    }
}
