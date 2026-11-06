package fr.toupiearena.mobile.net

import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BotController
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import org.json.JSONObject
import kotlin.random.Random

/**
 * Combat côté hôte : l'hôte simule (il fait autorité), applique les actions
 * reçues des clients et diffuse l'état à ses participants. Les places sans
 * joueur (playerId -1) sont tenues par des bots. Sans serveur : combat local.
 */
class HostSession(private val server: LobbyServer?, private val setup: MatchSetup) : MatchSession() {
    private val battle = Battle(setup.tops, setup.stadium, setup.rules.apply(), setup.seed)
    private val angles = MutableList(battle.playerCount) { (-battle.startPosition(it)).angle() } // Défaut : vers le centre.
    private val powers = MutableList(battle.playerCount) { 1.0 }
    private val pendingImpacts = mutableListOf<ImpactEvent>()
    private val bots = mutableListOf<BotController>()
    private var aimTimer = 0.0
    private var sendTimer = 0.0
    private val onGame: (Int, JSONObject) -> Unit = ::onGameMessage
    private val onLeft: (Int) -> Unit = ::onPlayerLeft

    init {
        initFromBattle(battle, setup.names)
        state.rules = setup.rules
        state.series = setup.series
        localPlayer = maxOf(0, slotOf(LobbyServer.HOST_ID))
        for (i in 0 until battle.playerCount) {
            val level = setup.botLevels.getOrElse(i) { -1 }
            if (level >= 0) {
                val bot = BotController(i, Difficulty.entries[level], Random.nextLong())
                bots += bot
                angles[i] = bot.chooseLaunchAngle(battle)
                powers[i] = bot.chooseLaunchPower(battle)
                state.aimed[i] = true
            }
        }
        server?.gameMessage?.add(onGame)
        server?.playerLeft?.add(onLeft)
        sendState()
    }

    override fun close() {
        server?.gameMessage?.remove(onGame)
        server?.playerLeft?.remove(onLeft)
    }

    private fun slotOf(playerId: Int) = setup.playerIds.indexOf(playerId)

    private fun setAim(slot: Int, angle: Double, power: Double) {
        if (state.phase != MatchPhase.Aiming || slot !in angles.indices || state.aimed[slot]) return
        angles[slot] = angle
        powers[slot] = power
        state.aimed[slot] = true
        sendTimer = 1.0 // Diffusion immédiate.
    }

    override fun submitAim(angle: Double, power: Double) = setAim(localPlayer, angle, power)
    override fun requestDash() { if (state.phase == MatchPhase.Fighting) battle.requestDash(localPlayer) }
    override fun requestSpecial() { if (state.phase == MatchPhase.Fighting) battle.requestSpecial(localPlayer) }
    override fun setSteering(direction: Vec2) = battle.setSteering(localPlayer, direction)

    private fun onGameMessage(playerId: Int, message: JSONObject) {
        val slot = slotOf(playerId)
        if (slot < 0) return
        when (val type = message.optString("type")) {
            Msg.Aim -> setAim(slot, message.optDouble("angle", 0.0), message.optDouble("power", 1.0))
            Msg.Steer -> battle.setSteering(slot, Vec2(message.optDouble("x", 0.0), message.optDouble("y", 0.0)))
            else -> if (state.phase == MatchPhase.Fighting) when (type) {
                Msg.Dash -> battle.requestDash(slot)
                Msg.Special -> battle.requestSpecial(slot)
                Msg.Duel -> battle.duelPress(slot, message.optDouble("c", 0.0))
            }
        }
    }

    private fun onPlayerLeft(playerId: Int) {
        val slot = slotOf(playerId)
        if (slot < 0 || state.phase == MatchPhase.Finished) return
        battle.forfeit(slot)
        state.aimed[slot] = true
        syncFromBattle(battle)
        if (battle.isFinished) setPhase(MatchPhase.Finished)
        sendTimer = 1.0
    }

    override fun update(dt: Double) {
        val before = state.phase
        when (state.phase) {
            MatchPhase.Aiming -> {
                aimTimer += dt
                // Sans serveur (combat local), le joueur prend tout son temps.
                if (!state.aimed.contains(false) || (server != null && aimTimer >= Protocol.AIM_TIMEOUT)) {
                    for (i in state.aimed.indices) state.aimed[i] = true // Les retardataires partent vers le centre.
                    state.countdown = COUNTDOWN_SECONDS
                    setPhase(MatchPhase.Countdown)
                }
            }
            MatchPhase.Countdown -> {
                state.countdown -= dt
                if (state.countdown <= 0.0) {
                    state.countdown = 0.0
                    battle.launch(angles, powers)
                    pendingImpacts += syncFromBattle(battle)
                    setPhase(if (battle.isFinished) MatchPhase.Finished else MatchPhase.Fighting)
                }
            }
            MatchPhase.Fighting -> {
                bots.forEach { it.update(battle, dt) }
                battle.advance(dt)
                pendingImpacts += syncFromBattle(battle)
                if (battle.isFinished) setPhase(MatchPhase.Finished)
            }
            MatchPhase.Finished -> Unit
        }
        // Après la fin, un envoi par seconde suffit à confirmer le résultat.
        sendTimer += dt
        val interval = if (state.phase == MatchPhase.Finished) 1.0 else 1.0 / Protocol.STATE_RATE
        if (state.phase != before || sendTimer >= interval) sendState()
    }

    private fun sendState() {
        sendTimer = 0.0
        if (server != null) {
            // Seuls les participants reçoivent l'état (plusieurs matchs de tournoi en parallèle).
            val message = MatchCodec.encodeState(state, pendingImpacts)
            setup.playerIds.filter { it >= 0 && it != LobbyServer.HOST_ID }.forEach { server.sendTo(it, message) }
        }
        pendingImpacts.clear()
    }
}
