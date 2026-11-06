package fr.toupiearena.mobile.session

import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.Duel
import fr.toupiearena.mobile.game.ImpactEvent
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Projectile
import fr.toupiearena.mobile.game.Spinner
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.StadiumGeometry
import fr.toupiearena.mobile.game.Vec2

/** L'ordre des valeurs compte : il est transmis en réseau. */
enum class MatchPhase { Aiming, Countdown, Fighting, Finished }

/** Ce que l'arène affiche, que le combat soit simulé ici ou reçu du réseau. */
class MatchState {
    var stadium: Stadium = Stadium.classic
    var config: BattleConfig = BattleConfig()
    var geometry: StadiumGeometry = StadiumGeometry()
    var spinners: List<Spinner> = emptyList()
    var names: List<String> = emptyList()
    var startPositions: List<Vec2> = emptyList()
    var aimed: MutableList<Boolean> = mutableListOf()
    var projectiles: List<Projectile> = emptyList()
    var duel: Duel = Duel()
    var rules: MatchRules = MatchRules()
    /** Match de tournoi : score avant cette manche (places 0 et 1 = camps 0 et 1). */
    var series: MatchSeries? = null
    var phase = MatchPhase.Aiming
    var countdown = 0.0
    var elapsed = 0.0
    var winner = -1
    var endReason = Battle.EndReason.None
    var ranking: List<Int> = emptyList()

    val playerCount get() = spinners.size
}

/**
 * Un combat vu depuis un joueur : solo contre des bots, hôte réseau ou client
 * réseau. L'arène appelle update() à chaque image, transmet les actions du
 * joueur local et affiche state.
 */
abstract class MatchSession {
    val state = MatchState()
    var localPlayer = 0
        protected set
    val localHasAimed get() = state.aimed.getOrElse(localPlayer) { false }
    protected val impacts = mutableListOf<ImpactEvent>()
    private val phaseListeners = mutableListOf<(MatchPhase) -> Unit>()

    /** Appelé à chaque changement de phase (dont la fin du combat). */
    fun onPhaseChanged(listener: (MatchPhase) -> Unit) { phaseListeners += listener }

    abstract fun update(dt: Double)
    /** Valide l'angle et la force de lancement du joueur local. */
    abstract fun submitAim(angle: Double, power: Double)
    abstract fun requestDash()
    abstract fun requestSpecial()
    /** Pilotage du joueur local (vecteur nul = aucun). */
    abstract fun setSteering(direction: Vec2)
    open fun close() {}

    /** Chocs survenus depuis le dernier appel (effets visuels). */
    fun takeImpacts(): List<ImpactEvent> = impacts.toList().also { impacts.clear() }

    protected fun setPhase(phase: MatchPhase) {
        if (state.phase == phase) return
        state.phase = phase
        phaseListeners.toList().forEach { it(phase) }
    }

    /** Initialise l'état à partir d'un combat qui n'a pas encore commencé. */
    protected fun initFromBattle(battle: Battle, names: List<String>) {
        state.stadium = battle.stadium
        state.config = battle.config
        state.geometry = battle.geometry
        state.spinners = battle.spinners.map { it.copy() }
        state.names = List(battle.playerCount) { names.getOrElse(it) { "Joueur ${it + 1}" } }
        state.startPositions = List(battle.playerCount) { battle.startPosition(it) }
        state.aimed = MutableList(battle.playerCount) { false }
        state.projectiles = emptyList()
        state.duel = Duel()
        state.phase = MatchPhase.Aiming
        state.countdown = 0.0
        state.elapsed = 0.0
        state.winner = -1
        state.endReason = Battle.EndReason.None
        state.ranking = emptyList()
        impacts.clear()
    }

    /** Copie l'état dynamique du combat et retourne les nouveaux chocs. */
    protected fun syncFromBattle(battle: Battle): List<ImpactEvent> {
        state.spinners = battle.spinners.map { it.copy() }
        state.elapsed = battle.elapsed
        state.winner = battle.winner
        state.endReason = battle.endReason
        state.ranking = battle.ranking
        state.projectiles = battle.projectiles.map { it.snapshot() }
        state.duel = battle.duel.snapshot()
        return battle.takeImpacts().also { impacts += it }
    }

    companion object { const val COUNTDOWN_SECONDS = 3.0 }
}
