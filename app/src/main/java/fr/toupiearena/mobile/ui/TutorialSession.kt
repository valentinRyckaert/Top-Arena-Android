package fr.toupiearena.mobile.ui

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Vec2
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import kotlin.math.abs

/** Actions tactiles du joueur, filtrées par le tutoriel. */
enum class TouchAction { Aim, Launch, Steer, Dash, Special }

/**
 * Tutoriel : combat scénarisé contre une toupie d'entraînement à la trajectoire
 * prévue (lancer fixe, puis approche lente). Chaque bulle met le jeu en pause et
 * présente une action ; seule l'action présentée reprend la partie. Le duel est
 * guidé : un raté n'est pas compté, le joueur gagne et l'adversaire est éjecté.
 */
class TutorialSession(private val scheme: ControlScheme) : MatchSession() {
    enum class Step { Aim, Steer, Dash, Special, Duel, AfterDuel, DuelIntro, DuelPlay, DuelMiss, Ejection, Done }

    private val battle = Battle(listOf(PartCatalog.presets[0], PartCatalog.presets[1]), Stadium.classic,
        BattleConfig(duelTimeout = 1e9), SEED) // Pas de défaite par inactivité en duel.
    var step = Step.Aim
        private set
    var isPaused = true
        private set
    private var duelHits = 0
    private var stepTimer = 0.0
    private val angles = mutableListOf(0.0, (-battle.startPosition(1)).angle())
    private val powers = mutableListOf(1.0, 0.7)

    init {
        initFromBattle(battle, listOf("Vous", "Toupie d'entraînement"))
        state.aimed[1] = true
    }

    private fun next(s: Step) {
        step = s
        isPaused = false
        stepTimer = 0.0
    }

    private fun distance() = (battle.spinner(1).position - battle.spinner(0).position).length()

    private val steerHow
        get() = when (scheme) {
            ControlScheme.Buttons -> "déplacez le joystick à gauche"
            ControlScheme.Tilt -> "inclinez le téléphone"
            ControlScheme.Gestures -> "glissez le doigt sur l'écran"
        }
    private val dashHow get() = if (scheme == ControlScheme.Gestures) "touchez l'écran" else "appuyez sur DASH"
    private val specialHow get() = if (scheme == ControlScheme.Gestures) "faites un appui long sur l'écran" else "appuyez sur SPÉCIAL"
    private val duelHow get() = "touchez l'écran"

    /** Texte de la bulle (vide : aucune) et toupie désignée (-1 : aucune). */
    val bubble: String
        get() = when {
            step == Step.Aim -> "Bienvenue dans le tutoriel !\nVotre toupie est à gauche. Posez le doigt sur l'arène et " +
                "glissez : la flèche donne la direction, sa longueur la force. Relâchez pour lancer."
            step == Step.Done -> "Bravo ! Duel gagné et toupie adverse éjectée : vous connaissez toutes les actions."
            !isPaused -> ""
            step == Step.Steer -> "Votre toupie avance seule, dans le sens inverse de sa rotation, et revient vers le " +
                "centre. Pour la piloter légèrement, $steerHow."
            step == Step.Dash -> "L'adversaire est proche : $dashHow pour dasher. Le dash fonce sur lui avec plus " +
                "d'attaque, contre un peu d'endurance."
            step == Step.Special -> "Votre coup spécial (donné par le boulon) : $specialHow. Une seule fois par partie, " +
                "il coûte un quart de l'endurance du moment et son effet compte autant que ses dégâts."
            step == Step.Duel -> "Il fonce sur vous ! Répondez par un dash ($dashHow) : deux dashs qui se percutent " +
                "déclenchent un duel."
            step == Step.DuelIntro -> "Duel ! Les toupies restent bloquées. En bas, un curseur blanc parcourt votre " +
                "barre : $duelHow quand il est dans la zone verte. Elle rétrécit à chaque réussite ; le premier qui " +
                "rate subit 1,5× les dégâts du dash adverse.\nTouchez l'écran pour commencer."
            step == Step.DuelMiss -> "Raté : le curseur était dans le rouge. Attendez qu'il passe au centre, dans la " +
                "zone verte. Pour cette fois, l'erreur ne compte pas !\nTouchez l'écran pour reprendre."
            else -> ""
        }

    val bubbleTarget get() = when (step) {
        Step.Dash, Step.Special, Step.Duel -> 1
        Step.Done -> -1
        else -> 0
    }

    /** False si l'action est ignorée à cette étape ; l'action présentée fait avancer le tutoriel. */
    fun handleAction(action: TouchAction): Boolean {
        when (step) {
            Step.Aim -> return action == TouchAction.Aim || action == TouchAction.Launch
            Step.Done, Step.Ejection -> return action == TouchAction.Steer
            else -> Unit
        }
        if (!isPaused) {
            if (step == Step.DuelPlay && action == TouchAction.Dash) {
                // Duel guidé : un appui hors de la zone verte n'est pas compté.
                val side = state.duel.sides[maxOf(0, state.duel.sideOf(0))]
                if (abs(side.cursor) <= side.zone) return true
                step = Step.DuelMiss
                isPaused = true
                return false
            }
            return action == TouchAction.Steer || (action == TouchAction.Dash && step == Step.Special)
        }
        return when (step) {
            Step.Steer -> (action == TouchAction.Steer).also { if (it) next(Step.Dash) }
            Step.Dash -> (action == TouchAction.Dash).also { if (it) next(Step.Special) }
            Step.Special -> (action == TouchAction.Special).also { if (it) next(Step.Duel) }
            Step.Duel -> (action == TouchAction.Dash).also {
                if (it) {
                    battle.requestDash(1, 0) // Le dash du joueur part au même instant.
                    next(Step.AfterDuel)
                }
            }
            Step.DuelIntro, Step.DuelMiss -> {
                // Reprise : cet appui ne compte pas comme un appui de duel.
                if (action == TouchAction.Dash) next(Step.DuelPlay)
                false
            }
            else -> false
        }
    }

    override fun submitAim(angle: Double, power: Double) {
        if (step != Step.Aim || state.phase != MatchPhase.Aiming) return
        angles[0] = angle
        powers[0] = power
        state.aimed[0] = true
        state.countdown = COUNTDOWN_SECONDS
        next(Step.Steer)
        setPhase(MatchPhase.Countdown)
    }

    override fun requestDash() {
        if (state.phase != MatchPhase.Fighting || isPaused) return
        battle.requestDash(0)
        if (step == Step.DuelPlay && ++duelHits >= DUEL_HITS && battle.duel.active) {
            // Le joueur gagne toujours : la toupie d'entraînement rate, puis est éjectée.
            battle.duelPress(1, 1.0)
            val away = (battle.spinner(1).position - battle.spinner(0).position).normalized()
            battle.applyImpulse(1, away * EJECTION)
            next(Step.Ejection)
        }
    }

    override fun requestSpecial() { if (state.phase == MatchPhase.Fighting && !isPaused) battle.requestSpecial(0) }
    override fun setSteering(direction: Vec2) = battle.setSteering(0, direction)

    override fun update(dt: Double) {
        when (state.phase) {
            MatchPhase.Aiming, MatchPhase.Finished -> return
            MatchPhase.Countdown -> {
                state.countdown -= dt
                if (state.countdown <= 0.0) {
                    state.countdown = 0.0
                    battle.launch(angles, powers)
                    syncFromBattle(battle)
                    setPhase(MatchPhase.Fighting)
                }
                return
            }
            MatchPhase.Fighting -> Unit
        }
        if (isPaused) return // Bulle affichée : le jeu attend l'action présentée.
        stepTimer += dt
        when (step) {
            Step.Steer -> if (stepTimer > 1.0) isPaused = true
            Step.Dash -> if (stepTimer > 1.5 && battle.canDash(0) && (distance() < 0.55 || stepTimer > 8.0)) isPaused = true
            Step.Special -> if (stepTimer > 1.5 && battle.canSpecial(0)) isPaused = true
            Step.Duel -> if (battle.canDash(0) && battle.canDash(1) && distance() < 0.7) isPaused = true
            Step.AfterDuel -> when {
                battle.duel.active -> {
                    step = Step.DuelIntro
                    isPaused = true
                    syncFromBattle(battle) // Barres de duel visibles pendant l'explication.
                    return
                }
                stepTimer > 2.0 -> next(Step.Duel) // Dashs manqués : on recommence.
            }
            else -> Unit
        }
        if (isPaused) return
        if (step < Step.AfterDuel) {
            // Trajectoire prévue : la toupie d'entraînement se rapproche lentement du joueur.
            battle.setSteering(1, (battle.spinner(0).position - battle.spinner(1).position).normalized() * APPROACH)
        } else {
            battle.setSteering(1, Vec2.ZERO)
        }
        battle.advance(dt)
        syncFromBattle(battle)
        if (battle.isFinished) {
            step = Step.Done
            setPhase(MatchPhase.Finished)
        }
    }

    companion object {
        private const val SEED = 7L
        private const val APPROACH = 0.6
        private const val DUEL_HITS = 3
        private const val EJECTION = 3.0
    }
}
