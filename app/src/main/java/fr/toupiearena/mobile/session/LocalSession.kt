package fr.toupiearena.mobile.session

import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BotController
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Vec2
import kotlin.random.Random

/**
 * Combat en solo : le joueur (indice 0) contre un ou plusieurs bots.
 * `tops` : toupie du joueur puis celles des bots (2 à 4 au total), tous du même niveau.
 */
class LocalSession(
    private val tops: List<Top>,
    private val names: List<String>,
    private val stadium: Stadium,
    private val difficulty: Difficulty,
    private val rules: MatchRules = MatchRules(),
) : MatchSession() {
    /** Le joueur contre 1 à 3 bots, nommés comme sur PC (« Bot Facile » seul, « Bot 1 »… en mêlée). */
    constructor(player: Top, opponents: List<Top>, stadium: Stadium, difficulty: Difficulty,
                rules: MatchRules = MatchRules()) :
        this(listOf(player) + opponents, soloNames(opponents.size, difficulty), stadium, difficulty, rules)

    /** Le joueur contre un seul bot. */
    constructor(player: Top, opponent: Top, stadium: Stadium, difficulty: Difficulty,
                rules: MatchRules = MatchRules()) : this(player, listOf(opponent), stadium, difficulty, rules)

    companion object {
        const val MAX_BOTS = Battle.MAX_PLAYERS - 1

        private fun soloNames(bots: Int, difficulty: Difficulty): List<String> {
            require(bots in 1..MAX_BOTS) { "1 à $MAX_BOTS bots" }
            // Seul, le bot porte sa difficulté ; en mêlée, un numéro.
            return listOf("Vous") + (1..bots).map { "Bot ${if (bots == 1) difficulty.label else it.toString()}" }
        }
    }

    private lateinit var battle: Battle
    private val bots = mutableListOf<BotController>()
    private val angles = MutableList(tops.size) { 0.0 }
    private val powers = MutableList(tops.size) { 1.0 }

    init { restart() }

    fun restart() {
        battle = Battle(tops, stadium, rules.apply(), Random.nextLong())
        initFromBattle(battle, names)
        state.rules = rules
        localPlayer = 0
        // Les bots choisissent leur angle à l'avance, sans voir celui du joueur.
        bots.clear()
        for (i in 1 until battle.playerCount) {
            val bot = BotController(i, difficulty, Random.nextLong())
            bots += bot
            angles[i] = bot.chooseLaunchAngle(battle)
            powers[i] = bot.chooseLaunchPower(battle)
            state.aimed[i] = true
        }
        state.phase = MatchPhase.Countdown // Force la notification de la phase de visée.
        setPhase(MatchPhase.Aiming)
    }

    override fun submitAim(angle: Double, power: Double) {
        if (state.phase != MatchPhase.Aiming) return
        angles[0] = angle
        powers[0] = power
        state.aimed[0] = true
        state.countdown = COUNTDOWN_SECONDS
        setPhase(MatchPhase.Countdown)
    }

    override fun requestDash() { if (state.phase == MatchPhase.Fighting) battle.requestDash(0) }
    override fun requestSpecial() { if (state.phase == MatchPhase.Fighting) battle.requestSpecial(0) }
    override fun setSteering(direction: Vec2) = battle.setSteering(0, direction)

    override fun update(dt: Double) {
        when (state.phase) {
            MatchPhase.Aiming, MatchPhase.Finished -> Unit
            MatchPhase.Countdown -> {
                state.countdown -= dt
                if (state.countdown <= 0.0) {
                    state.countdown = 0.0
                    battle.launch(angles, powers)
                    syncFromBattle(battle)
                    setPhase(MatchPhase.Fighting)
                }
            }
            MatchPhase.Fighting -> {
                bots.forEach { it.update(battle, dt) }
                battle.advance(dt)
                syncFromBattle(battle)
                if (battle.isFinished) setPhase(MatchPhase.Finished)
            }
        }
    }
}
