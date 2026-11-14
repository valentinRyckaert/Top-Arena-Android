package fr.toupiearena.mobile.game

import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.core.classify
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** L'ordre des valeurs compte : il est transmis en réseau. */
enum class Difficulty(val label: String) { Easy("Facile"), Normal("Normal"), Hard("Difficile") }

/**
 * Adversaire contrôlé par l'ordinateur, porté de la version PC : plans
 * (chasser, tenir, esquiver) selon la toupie et la course à l'usure, dashs et
 * coup spécial calculés, esquive des vagues et timing en duel selon le niveau.
 */
class BotController(val index: Int, val difficulty: Difficulty, seed: Long = System.nanoTime()) {
    enum class Plan { Hunt, Hold, Evade }

    private class Skill(
        val reaction: Double, val aimError: Double, val dodgeWaves: Double, val rangeFactor: Double,
        val hesitate: Double, val tactics: Boolean, val combos: Boolean, val anticipate: Boolean,
        val duelTiming: Double,
        /** Duel en rafale : appuis par seconde. */
        val rushRate: Double,
    )

    // Rafale : un humain appuie 6 à 9 fois par seconde ; le bot difficile est dans le haut.
    private val skill = when (difficulty) {
        Difficulty.Easy -> Skill(0.45, 0.5, 0.2, 1.3, 0.15, false, false, false, 0.075, 5.5)
        Difficulty.Normal -> Skill(0.25, 0.2, 0.6, 0.9, 0.0, false, false, false, 0.045, 7.0)
        Difficulty.Hard -> Skill(0.12, 0.07, 0.8, 0.9, 0.0, true, true, true, 0.025, 8.5)
    }
    private val rng = java.util.Random(seed)
    var plan = Plan.Hold
        private set
    var specialsAllowed = true
    private var decisionTimer = 0.0
    private var steerTimer = 0.0
    private var rushTimer = 0.0
    private val ignoredThreats = mutableListOf<Int>()
    private val seenThreats = mutableListOf<Int>()

    private fun chance(p: Double) = rng.nextDouble() < p
    private fun gaussian(sigma: Double) = rng.nextGaussian() * sigma

    // --- Lancement -----------------------------------------------------------

    fun chooseLaunchAngle(battle: Battle): Double {
        val self = battle.spinner(index)
        val start = battle.startPosition(index)
        val toCenter = (-start).normalized()
        val orbit = start.perpendicular().normalized() * -self.spinSign.toDouble()
        var centerWeight = 0.6
        var spread = 0.3
        when (classify(self.stats)) {
            TopType.Attack -> { centerWeight = 1.0; spread = 0.35 }
            TopType.Defense -> { centerWeight = 0.7; spread = 0.15 }
            TopType.Endurance -> { centerWeight = 0.5; spread = 0.15 }
            TopType.Balance -> Unit
        }
        if (skill.tactics && battle.playerCount > 2) centerWeight = min(centerWeight, 0.55)
        val direction = toCenter * centerWeight + orbit * (1.0 - centerWeight)
        return direction.angle() + (rng.nextDouble() * 2 - 1) * spread + gaussian(skill.aimError)
    }

    fun chooseLaunchPower(battle: Battle): Double {
        val self = battle.spinner(index)
        var power = when (classify(self.stats)) {
            TopType.Attack -> 1.3
            TopType.Defense -> 0.85
            TopType.Endurance -> 0.75
            TopType.Balance -> 1.0
        }
        if (skill.tactics) {
            if (!battle.stadium.hasRim) power = min(power, 1.0)
            if (battle.playerCount > 2) power = min(power, 0.9)
        }
        val cfg = battle.config
        return (power + gaussian(skill.aimError * 0.5)).coerceIn(cfg.launchPowerMin, cfg.launchPowerMax)
    }

    // --- Analyse -------------------------------------------------------------

    /** Mon temps restant à l'usure / celui de l'adversaire le plus endurant (> 1 : je gagne la course). */
    private fun enduranceRace(battle: Battle): Double {
        val self = battle.spinner(index)
        val mine = self.spin / max(1e-6, battle.passiveLossPerSecond(self))
        var theirs = 0.0
        battle.spinners.forEachIndexed { i, s ->
            if (i != index && s.isActive) theirs = max(theirs, s.spin / max(1e-6, battle.passiveLossPerSecond(s)))
        }
        return if (theirs > 0.0) mine / theirs else 10.0
    }

    private fun chooseTarget(battle: Battle): Int {
        if (!skill.tactics || battle.playerCount <= 2) return battle.nearestOpponent(index)
        // Mêlée : préférer les proies fragiles et proches du bord, sans aller trop loin.
        val self = battle.spinner(index)
        var best = -1
        var bestScore = -1e18
        battle.spinners.forEachIndexed { i, s ->
            if (i == index || !s.isActive) return@forEachIndexed
            val score = battle.geometry.level(s.position) + (1.0 - s.spinRatio) * 0.6 - s.stats.defense / 20.0 -
                (s.position - self.position).length() * 1.2
            if (score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    private fun choosePlan(battle: Battle, target: Int): Plan {
        val self = battle.spinner(index)
        val style = classify(self.stats)
        val race = enduranceRace(battle)
        if (!skill.tactics) {
            if (race < 0.9) return Plan.Hunt
            return when (style) {
                TopType.Attack -> Plan.Hunt
                TopType.Endurance -> Plan.Evade
                else -> Plan.Hold
            }
        }
        val other = if (target >= 0) battle.spinner(target) else null
        if (other != null && other.spinRatio < 0.15 && self.spinRatio > 0.25) return Plan.Hunt
        return when (style) {
            TopType.Attack -> if (race > 1.4) Plan.Hold else Plan.Hunt
            TopType.Defense -> if (race < 0.9) Plan.Hunt else Plan.Hold
            TopType.Endurance -> if (race < 0.85) Plan.Hunt else Plan.Evade
            TopType.Balance -> if (race < 1.0) Plan.Hunt else if (race > 1.25) Plan.Evade else Plan.Hold
        }
    }

    // --- Boucle de décision --------------------------------------------------

    /** À appeler à chaque image pendant le combat. */
    fun update(battle: Battle, dt: Double) {
        if (battle.phase != Battle.Phase.Running || !battle.spinner(index).isActive) return

        // Duel : appuyer quand le curseur passe au centre, avec une erreur de timing selon le niveau.
        val side = battle.duel.sideOf(index)
        if (side >= 0) {
            if (battle.config.duelMode == DuelMode.Rush) {
                // Rafale : appuis à la cadence du niveau, un peu irréguliers.
                rushTimer -= dt
                if (rushTimer <= 0.0) {
                    rushTimer += (0.75 + rng.nextDouble() * 0.5) / skill.rushRate
                    battle.duelPress(index, 0.0)
                }
                return
            }
            val bar = battle.duel.sides[side]
            val next = bar.cursor + bar.direction * bar.speed * dt
            if (!bar.done && (bar.cursor < 0.0) != (next < 0.0))
                battle.duelPress(index, gaussian(skill.duelTiming) * bar.speed)
            return
        }

        val target = chooseTarget(battle)
        steerTimer -= dt
        if (steerTimer <= 0.0) {
            steerTimer = 0.12
            plan = choosePlan(battle, target)
            battle.setSteering(index, chooseSteering(battle, target))
        }

        rushTimer = 0.0 // Premier appui dès le début du prochain duel.

        // Contre-dash réflexe : répondre à un dash adverse provoque un duel (où un bon timing ou une
        // bonne cadence font la différence) ; sans duel, il évite d'encaisser un dash sans répondre.
        if (skill.tactics && battle.canDash(index)) {
            val self = battle.spinner(index)
            for (j in battle.spinners.indices) {
                val other = battle.spinner(j)
                val toMe = self.position - other.position
                if (j != index && other.isActive && other.isDashing &&
                    other.dashDirection.dot(toMe.normalized()) > 0.9 &&
                    toMe.length() < battle.config.dashDistance * 1.5
                ) {
                    battle.requestDash(index, j)
                    return
                }
            }
        }

        decisionTimer -= dt
        if (decisionTimer > 0.0) return
        decisionTimer = skill.reaction * (0.7 + rng.nextDouble() * 0.6)

        if (specialsAllowed && battle.canSpecial(index)) {
            if (specialValue(battle) > 0.0 && !chance(skill.hesitate)) {
                battle.requestSpecial(index)
                return
            }
        }
        if (!battle.canDash(index)) return
        val dashTarget = chooseDashTarget(battle)
        if (dashTarget >= 0) battle.requestDash(index, dashTarget)
    }

    // --- Coup spécial --------------------------------------------------------

    /** Une poussée `push` dans `direction` éjecte-t-elle probablement la toupie en `position` ? */
    private fun likelyEjection(battle: Battle, position: Vec2, direction: Vec2, push: Double): Boolean {
        val g = battle.geometry
        if (!battle.stadium.hasRim) return g.level(position + direction * (push * 0.45)) > 0.8
        val outward = g.outwardNormal(position)
        return g.level(position) > 0.65 && push * direction.dot(outward) > battle.config.rimEscapeSpeed * 1.15
    }

    private fun hitProbability(battle: Battle, move: SpecialMove, target: Int): Double {
        val cfg = battle.config
        val self = battle.spinner(index)
        val other = battle.spinner(target)
        val delta = other.position - self.position
        val distance = delta.length()
        if (move.shape == SpecialMove.Shape.Ring) return if (distance + cfg.topRadius * 0.5 <= move.range) 0.95 else 0.0
        if (distance > move.range) return 0.0
        if (other.isSlowed) return 0.95 // Ne peut pas piloter pour l'éviter.
        val travel = distance / move.speed
        val dir = delta.normalized()
        val lateralSpeed = abs(other.velocity.dot(dir.perpendicular()))
        val drift = lateralSpeed * travel + cfg.steerAcceleration * travel * travel * 0.5
        return (1.1 - drift / (cfg.topRadius + move.width) * 0.6).coerceIn(0.15, 0.95)
    }

    /** Valeur estimée (en points d'endurance) du lancer du coup spécial maintenant. */
    fun specialValue(battle: Battle): Double {
        val cfg = battle.config
        val self = battle.spinner(index)
        val move = SpecialMove.find(self.top.special)
        val nearest = battle.nearestOpponent(index)
        if (move == null || nearest < 0) return -1e9
        val cost = cfg.specialCost * self.spin

        if (difficulty == Difficulty.Easy) {
            // Débutant impulsif : dès que l'adversaire est à portée et qu'il a commencé à s'user.
            val distance = (battle.spinner(nearest).position - self.position).length()
            return if (distance < move.range * 0.9 && self.spinRatio < 0.8) 1.0 else -1.0
        }

        val targets = if (move.shape == SpecialMove.Shape.Ring)
            battle.spinners.indices.filter { it != index && battle.spinner(it).isActive } else listOf(nearest)

        var value = -cost
        for (i in targets) {
            val t = battle.spinner(i)
            val p = hitProbability(battle, move, i)
            if (p <= 0.0) continue
            val mitigation = max(cfg.minReduction, 1.0 - cfg.damagePerDefense * t.stats.defense)
            val damage = min(t.spin, move.damage * cfg.specialDamageScale * mitigation)
            var gain = damage
            if (move.effect == SpecialMove.Effect.Drain) gain += damage * move.effectStrength
            if (move.effect == SpecialMove.Effect.Slow) // Usure due au gel.
                gain += battle.passiveLossPerSecond(t) * cfg.slowWear * move.effectStrength * move.effectDuration
            if (damage >= t.spin) gain += 40.0 // Coup fatal.

            val resistance = max(cfg.minReduction, 1.0 - cfg.knockbackPerDefense * t.stats.defense)
            val push = move.knockback * cfg.specialKnockbackScale * resistance
            val level = battle.geometry.level(t.position)
            val pushDirection = (t.position - self.position).normalized()
            if (move.effect != SpecialMove.Effect.Teleport && push > 0.3 &&
                likelyEjection(battle, t.position, pushDirection, push)
            ) gain += t.spin * 0.7

            if (skill.tactics) {
                val dashReady = self.dashCooldown <= 0.0 && !self.isDisarmed
                val distance = (t.position - self.position).length()
                when (move.effect) {
                    SpecialMove.Effect.Slow -> if (dashReady && plan == Plan.Hunt) gain += 12.0 + t.spin * 0.15
                    SpecialMove.Effect.Teleport ->
                        if (level < 0.5 && classify(self.stats) != TopType.Endurance) gain += t.spin * 0.3
                    SpecialMove.Effect.PullToCenter -> if (plan == Plan.Hunt && distance > 0.45) gain += 15.0
                    SpecialMove.Effect.Disarm -> if (t.stats.attack >= 9 && distance < 0.45) gain += 12.0
                    else -> Unit
                }
            }
            value += p * gain
        }
        if (skill.tactics) {
            val main = targets.firstOrNull() ?: -1
            val p = if (main >= 0) hitProbability(battle, move, main) else 0.0
            if (move.shape == SpecialMove.Shape.Wave && p < 0.6) return -1.0
            // Marge de sécurité : la patience est payante.
            value -= cost * 0.6
        }
        return value
    }

    // --- Pilotage ------------------------------------------------------------

    private fun dodgeDirection(battle: Battle): Vec2 {
        val cfg = battle.config
        val self = battle.spinner(index)
        for (p in battle.projectiles) {
            if (p.owner == index || index in p.hit) continue
            if (p.id !in seenThreats) {
                seenThreats += p.id
                if (!chance(skill.dodgeWaves)) ignoredThreats += p.id
            }
            if (p.id in ignoredThreats) continue
            if (p.ring) {
                val away = self.position - p.origin
                val d = away.length()
                if (d > p.radius && d > p.range - 0.25 && d < p.range + cfg.topRadius) return away.normalized()
                continue
            }
            val toSelf = self.position - p.position
            val along = toSelf.dot(p.direction)
            val horizon = if (skill.anticipate) 2.0 else 1.2
            if (along < 0.0 || along / p.speed > horizon) continue
            val eta = along / p.speed
            val future = if (skill.anticipate) self.position + self.velocity * eta else self.position
            val lateralNow = toSelf - p.direction * along
            val toFuture = future - p.position
            val lateral = toFuture - p.direction * toFuture.dot(p.direction)
            val tolerance = cfg.topRadius + p.width + 0.08
            if (lateral.length() > tolerance && lateralNow.length() > tolerance) continue
            val perp = p.direction.perpendicular()
            var side = if (skill.anticipate && abs(self.velocity.dot(perp)) > 0.05) {
                if (self.velocity.dot(perp) > 0.0) perp else -perp
            } else if (lateralNow.lengthSquared() > 1e-8) lateralNow.normalized() else perp
            if (battle.geometry.level(self.position + side * 0.25) > 0.85) side = -side
            return side
        }
        if (battle.projectiles.isEmpty()) {
            seenThreats.clear()
            ignoredThreats.clear()
        }
        return Vec2.ZERO
    }

    private fun chooseSteering(battle: Battle, target: Int): Vec2 {
        val self = battle.spinner(index)
        val g = battle.geometry
        val level = g.level(self.position)
        val toCenter = (-self.position).normalized()
        if (level > (if (battle.stadium.hasRim) 0.8 else 0.7)) return toCenter
        val dodge = dodgeDirection(battle)
        if (dodge.lengthSquared() > 0.0) return dodge
        if (target < 0) return if (level > 0.3) toCenter * 0.5 else Vec2.ZERO

        val other = battle.spinner(target)
        var targetPos = other.position
        if (skill.anticipate) targetPos += other.velocity * 0.25
        val toTarget = targetPos - self.position
        val distance = toTarget.length()
        return when (plan) {
            Plan.Hunt -> {
                if (!skill.tactics) return toTarget.normalized()
                // Se placer côté centre de la cible : les chocs la poussent vers le bord.
                if (g.level(targetPos) < 0.2) return toTarget.normalized()
                val outward = targetPos.normalized()
                val inside = (self.position - targetPos).dot(outward) < -0.04
                if (inside || distance < 0.2) return toTarget.normalized()
                ((targetPos - outward * 0.22) - self.position).normalized()
            }
            Plan.Hold -> if (skill.tactics && distance < 0.35) {
                (toCenter * 0.8 - toTarget.normalized() * 0.2).normalized()
            } else if (level > 0.25) toCenter * 0.7 else Vec2.ZERO
            Plan.Evade -> {
                var flee = if (distance < 0.55) (-toTarget).normalized() else Vec2.ZERO
                if (skill.tactics && flee.lengthSquared() > 0.0) {
                    val tangent = self.position.perpendicular().normalized()
                    val sideSign = if (tangent.dot(flee) >= 0.0) 1.0 else -1.0
                    flee = (flee * 0.5 + tangent * (sideSign * 0.5)).normalized()
                }
                val safe = if (skill.tactics) 0.4 else 0.5
                var desired = flee * 0.8
                if (level > safe) desired += toCenter * ((level - safe) * 3.0)
                if (desired.lengthSquared() > 1.0) desired.normalized() else desired
            }
        }
    }

    // --- Dash ----------------------------------------------------------------

    private fun chooseDashTarget(battle: Battle): Int {
        if (chance(skill.hesitate)) return -1
        var best = -1
        var bestScore = -1e18
        for (i in battle.spinners.indices) {
            if (i == index || !battle.spinner(i).isActive) continue
            val (wants, score) = wantsToDashAt(battle, i)
            if (wants && score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    /** Valeur estimée d'un dash vers `target` (points d'endurance nets). */
    private fun dashValue(battle: Battle, target: Int): Double {
        val cfg = battle.config
        val self = battle.spinner(index)
        val other = battle.spinner(target)
        val direction = (other.position - self.position).normalized()
        val boosted = self.copy(boostTimer = cfg.dashBoostDuration, velocity = direction * cfg.dashSpeed)
        val approach = max(0.0, cfg.dashSpeed + other.velocity.dot(-direction))
        val dealt = min(other.spin, battle.collisionDamage(boosted, other, approach))
        val taken = battle.collisionDamage(other, boosted, approach)
        var value = dealt - taken - cfg.dashCost
        if (likelyEjection(battle, other.position, direction, battle.knockback(boosted, other, approach)))
            value += other.spin * 0.7
        if (dealt >= other.spin) value += 40.0
        val recoil = battle.knockback(other, boosted, approach)
        val contact = self.position + direction * min(0.35, (other.position - self.position).length())
        if (likelyEjection(battle, contact, -direction, recoil)) value -= self.spin * 0.6
        if (other.isSlowed) value += 6.0
        return value
    }

    private fun wantsToDashAt(battle: Battle, target: Int): Pair<Boolean, Double> {
        val cfg = battle.config
        val self = battle.spinner(index)
        val other = battle.spinner(target)
        val g = battle.geometry
        val delta = other.position - self.position
        val distance = delta.length()
        val direction = delta.normalized()
        val reach = cfg.dashDistance
        var gap = distance - 2.0 * cfg.topRadius
        if (skill.anticipate) {
            val predicted = other.position + other.velocity * (gap / cfg.dashSpeed)
            gap = min(gap, (predicted - self.position).length() - 2.0 * cfg.topRadius)
        }
        if (gap > reach * skill.rangeFactor) return false to 0.0
        val landing = self.position + direction * (min(gap, reach) + 0.02)
        if (g.level(landing) > (if (battle.stadium.hasRim) 0.88 else 0.8)) return false to 0.0

        val opponentEdge = g.level(other.position)
        val incoming = other.velocity.dot(-direction)
        val outward = if (other.position.lengthSquared() > 1e-6) other.position.normalized() else direction
        val alignment = direction.dot(outward)
        val spinAfterDash = self.spin - cfg.dashCost
        if (skill.tactics) {
            val value = dashValue(battle, target)
            val threshold = when (plan) { Plan.Hunt -> 0.0; Plan.Hold -> 2.0; Plan.Evade -> 6.0 }
            return (spinAfterDash > 5.0 && value > threshold) to value
        }
        val score = -distance
        if (difficulty == Difficulty.Easy) return (spinAfterDash > 10.0) to score
        val wants = when (plan) {
            Plan.Hunt -> spinAfterDash > 8.0
            Plan.Hold -> spinAfterDash > 15.0 && ((opponentEdge > 0.55 && alignment > 0.2) || incoming > 0.5)
            Plan.Evade -> spinAfterDash > 25.0 && opponentEdge > 0.7 && alignment > 0.5
        }
        return wants to score
    }
}
