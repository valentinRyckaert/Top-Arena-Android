package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import org.json.JSONObject
import kotlin.math.PI

/**
 * Combat côté client : affiche l'état reçu de l'hôte (en prolongeant les
 * trajectoires entre deux réceptions) et lui transmet les actions du joueur.
 */
class ClientSession(private val client: LobbyClient?, setup: MatchSetup) : MatchSession() {
    private var sinceState = 0.0
    private var steer = Vec2.ZERO
    private val listener: (JSONObject) -> Unit = { if (it.optString("type") == Msg.State) applyState(it) }

    init {
        // Un combat jamais lancé fournit stadium, constantes et positions de départ.
        initFromBattle(Battle(setup.tops, setup.stadium, setup.rules.apply(), setup.seed), setup.names)
        state.rules = setup.rules
        state.series = setup.series
        localPlayer = if (client != null) maxOf(0, setup.playerIds.indexOf(client.playerId)) else 0
        client?.gameMessage?.add(listener)
    }

    override fun close() { client?.gameMessage?.remove(listener) }

    fun applyState(message: JSONObject) {
        val before = state.phase
        val localAimed = localHasAimed
        if (!MatchCodec.decodeState(message, state, impacts)) return
        // Ne pas « dé-valider » une visée envoyée mais pas encore prise en compte.
        if (localAimed && state.phase == MatchPhase.Aiming) state.aimed[localPlayer] = true
        sinceState = 0.0
        if (state.phase != before) {
            val now = state.phase
            state.phase = before
            setPhase(now)
        }
    }

    override fun update(dt: Double) {
        sinceState += dt
        if (state.phase == MatchPhase.Countdown) state.countdown = maxOf(0.0, state.countdown - dt)
        if (state.phase != MatchPhase.Fighting || sinceState > MAX_EXTRAPOLATION) return
        // Extrapolation entre deux états : déplacement à vitesse constante.
        state.elapsed += dt
        if (state.duel.active) state.duel.sides.filter { !it.done }.forEach { it.advance(dt) }
        for (p in state.projectiles) {
            if (p.ring) p.radius += p.speed * dt else p.position += p.direction * (p.speed * dt)
        }
        for (s in state.spinners) {
            if (!s.isActive) continue
            s.position += s.velocity * dt
            s.angle = (s.angle + s.spinSign * (6.0 + 30.0 * s.spinRatio) * dt) % (2.0 * PI)
            s.dashCooldown = maxOf(0.0, s.dashCooldown - dt)
            s.boostTimer = maxOf(0.0, s.boostTimer - dt)
        }
    }

    override fun submitAim(angle: Double, power: Double) {
        if (state.phase != MatchPhase.Aiming || localHasAimed) return
        state.aimed[localPlayer] = true
        client?.send(JSONObject().put("type", Msg.Aim).put("angle", angle).put("power", power))
    }

    override fun requestSpecial() {
        if (state.phase == MatchPhase.Fighting) client?.send(JSONObject().put("type", Msg.Special))
    }

    override fun setSteering(direction: Vec2) {
        // N'envoie que les changements.
        if ((direction - steer).lengthSquared() < 1e-9) return
        steer = direction
        client?.send(JSONObject().put("type", Msg.Steer).put("x", direction.x).put("y", direction.y))
    }

    override fun requestDash() {
        if (state.phase != MatchPhase.Fighting) return
        // En duel : appui jugé sur la position du curseur affichée ici.
        val side = state.duel.sideOf(localPlayer)
        if (side >= 0) client?.send(JSONObject().put("type", Msg.Duel).put("c", state.duel.sides[side].cursor))
        else client?.send(JSONObject().put("type", Msg.Dash))
    }

    companion object { private const val MAX_EXTRAPOLATION = 0.15 }
}
