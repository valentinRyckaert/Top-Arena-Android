package fr.toupiearena.mobile.game

import fr.toupiearena.mobile.core.Stats
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.core.classify
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** État d'une toupie pendant un combat. */
data class Spinner(
    val top: Top,
    val stats: Stats = top.stats,
    val spinSign: Int = top.spin.sign,
    var position: Vec2 = Vec2.ZERO,
    var velocity: Vec2 = Vec2.ZERO,
    var spin: Double = 100.0,
    var maxSpin: Double = 100.0,
    var angle: Double = 0.0,
    var dashTimer: Double = 0.0,
    var dashCooldown: Double = 0.0,
    var boostTimer: Double = 0.0,
    var dashDirection: Vec2 = Vec2.ZERO,
    var escaping: Boolean = false,
    var lastHitBy: Int = -1,
    var lastHitTime: Double = -1e9,
    var steer: Vec2 = Vec2.ZERO,
    var specialUsed: Boolean = false,
    var slowTimer: Double = 0.0,
    var slowStrength: Double = 0.0,
    var disarmTimer: Double = 0.0,
    var status: Status = Status.Waiting,
) {
    /** L'ordre des valeurs compte : il est transmis en réseau. */
    enum class Status { Waiting, Spinning, RingOut, Stopped, Forfeit }

    val spinRatio get() = if (maxSpin > 0.0) spin / maxSpin else 0.0
    val isActive get() = status == Status.Spinning
    val isDashing get() = dashTimer > 0.0
    val isBoosted get() = boostTimer > 0.0
    val isSlowed get() = slowTimer > 0.0
    val isDisarmed get() = disarmTimer > 0.0
}

/** Vague d'un coup spécial en cours de propagation. */
data class Projectile(
    var id: Int = 0,
    var move: String = "",
    var owner: Int = 0,
    var ring: Boolean = false,
    var origin: Vec2 = Vec2.ZERO,
    var position: Vec2 = Vec2.ZERO,
    var direction: Vec2 = Vec2.ZERO,
    var radius: Double = 0.0,
    var traveled: Double = 0.0,
    var speed: Double = 1.0,
    var range: Double = 1.0,
    var width: Double = 0.08,
    var color: Long = 0xffffff,
    val hit: MutableList<Int> = mutableListOf(),
) {
    fun snapshot() = copy(hit = hit.toMutableList())
}

/** Un choc (effets visuels), éventuellement la fin d'un duel. */
data class ImpactEvent(
    val point: Vec2,
    val intensity: Double,
    val first: Int = 0,
    val second: Int = 1,
    val special: Boolean = false,
    val color: Long = 0,
    /** Fin de duel : perdant (-1 = nul) ; NO_DUEL sinon. */
    val duelLoser: Int = NO_DUEL,
) {
    companion object { const val NO_DUEL = -2 }
}

/** Barre d'un duelliste : curseur de -1 à 1, zone verte centrée sur 0. */
data class DuelSide(
    var cursor: Double = -1.0,
    var direction: Double = 1.0,
    var speed: Double = 0.0,
    var zone: Double = 0.0,
    var streak: Int = 0,
    var idle: Double = 0.0,
    var done: Boolean = false,
) {
    /** Aller-retour sur la barre. */
    fun advance(h: Double) {
        cursor += direction * speed * h
        while (cursor > 1.0 || cursor < -1.0) {
            cursor = if (cursor > 1.0) 2.0 - cursor else -2.0 - cursor
            direction = -direction
        }
    }
}

/** Duel entre deux toupies qui se percutent en plein dash. */
data class Duel(
    var active: Boolean = false,
    var first: Int = -1,
    var second: Int = -1,
    var point: Vec2 = Vec2.ZERO,
    var normal: Vec2 = Vec2.ZERO,
    var approach: Double = 0.0,
    val sides: Array<DuelSide> = arrayOf(DuelSide(), DuelSide()),
) {
    /** 0 ou 1 pour un duelliste, -1 sinon. */
    fun sideOf(spinner: Int) = when {
        !active -> -1
        spinner == first -> 0
        spinner == second -> 1
        else -> -1
    }
    fun snapshot() = copy(sides = arrayOf(sides[0].copy(), sides[1].copy()))
}

/**
 * Simulation d'un combat entre 2 et 4 toupies, portée à l'identique de la
 * version PC : pas de temps fixe, la dernière toupie en jeu l'emporte.
 */
class Battle(
    tops: List<Top>,
    val stadium: Stadium = Stadium.classic,
    config: BattleConfig = BattleConfig(),
    seed: Long = Random.nextLong(),
) {
    enum class Phase { Ready, Running, Finished }
    /** L'ordre des valeurs compte : il est transmis en réseau. */
    enum class EndReason { None, RingOut, SpinOut, Timeout, Forfeit }

    val config: BattleConfig = stadium.apply(config)
    val geometry: StadiumGeometry = stadium.geometry(this.config)
    val spinners: List<Spinner>
    private val pairCooldowns: DoubleArray
    private val rng = Random(seed)
    var phase = Phase.Ready
        private set
    var endReason = EndReason.None
        private set
    var winner = -1
        private set
    var elapsed = 0.0
        private set
    private var accumulator = 0.0
    private val eliminated = mutableListOf<Int>()
    var ranking: List<Int> = emptyList()
        private set
    private val impacts = mutableListOf<ImpactEvent>()
    val projectiles = mutableListOf<Projectile>()
    private var nextProjectileId = 1
    val duel = Duel()

    init {
        val count = tops.size.coerceIn(MIN_PLAYERS, MAX_PLAYERS)
        spinners = List(count) { i ->
            Spinner(top = tops.getOrElse(i) { tops[0] }, spin = this.config.maxSpin, maxSpin = this.config.maxSpin)
        }
        pairCooldowns = DoubleArray(count * count)
        spinners.forEachIndexed { i, s -> s.position = startPosition(i) }
    }

    val playerCount get() = spinners.size
    val isFinished get() = phase == Phase.Finished
    fun spinner(i: Int) = spinners[i]

    fun startPosition(index: Int): Vec2 {
        val angle = PI + index * 2.0 * PI / playerCount
        return Vec2.fromAngle(angle) * (config.startOffset * geometry.innerRadius)
    }

    /** Lance toutes les toupies (angles en radians, forces entre launchPowerMin et launchPowerMax). */
    fun launch(angles: List<Double>, powers: List<Double> = emptyList()) {
        if (phase != Phase.Ready) return
        spinners.forEachIndexed { i, s ->
            if (s.status == Spinner.Status.Forfeit) return@forEachIndexed
            val power = powers.getOrElse(i) { 1.0 }.coerceIn(config.launchPowerMin, config.launchPowerMax)
            s.position = startPosition(i)
            s.velocity = Vec2.fromAngle(angles.getOrElse(i) { 0.0 }) * (config.launchSpeed * power)
            s.status = Spinner.Status.Spinning
        }
        phase = Phase.Running
        checkEnd()
    }

    fun nearestOpponent(index: Int): Int {
        var best = -1
        var bestDistance = 1e18
        for (j in spinners.indices) {
            if (j == index || !spinners[j].isActive) continue
            val d = (spinners[j].position - spinners[index].position).lengthSquared()
            if (d < bestDistance) { bestDistance = d; best = j }
        }
        return best
    }

    private fun inDuel(i: Int) = duel.sideOf(i) >= 0

    fun canDash(index: Int): Boolean {
        if (index !in spinners.indices) return false
        val s = spinners[index]
        return phase == Phase.Running && s.isActive && !s.isDashing && !s.isDisarmed && !inDuel(index) &&
            s.dashCooldown <= 0.0 && s.spin > config.dashCost && nearestOpponent(index) >= 0
    }

    fun setSteering(index: Int, direction: Vec2) {
        if (index !in spinners.indices) return
        val len = direction.length()
        spinners[index].steer = if (len > 1.0) direction / len else direction
    }

    fun canSpecial(index: Int): Boolean {
        if (index !in spinners.indices || !config.specialsEnabled) return false
        val s = spinners[index]
        return phase == Phase.Running && s.isActive && !s.isDisarmed && !inDuel(index) && !s.specialUsed &&
            SpecialMove.find(s.top.special) != null && nearestOpponent(index) >= 0
    }

    /** Lance le coup spécial vers l'adversaire le plus proche (une fois par partie). */
    fun requestSpecial(index: Int): Boolean {
        if (!canSpecial(index)) return false
        val self = spinners[index]
        val move = SpecialMove.find(self.top.special)!!
        val target = spinners[nearestOpponent(index)]
        val dir = (target.position - self.position).normalized()
        projectiles += Projectile(
            id = nextProjectileId++, move = move.id, owner = index, ring = move.shape == SpecialMove.Shape.Ring,
            origin = self.position, direction = dir, position = self.position + dir * config.topRadius,
            radius = config.topRadius, speed = move.speed, range = move.range, width = move.width,
            color = move.color,
        )
        self.specialUsed = true
        self.spin *= 1.0 - config.specialCost
        return true
    }

    /** Dash vers `target` (par défaut l'adversaire le plus proche) ; en duel, sert d'appui. */
    fun requestDash(index: Int, target: Int = -1): Boolean {
        val side = duel.sideOf(index)
        if (side >= 0) return duelPress(index, duel.sides[side].cursor)
        if (!canDash(index)) return false
        val t = if (target !in spinners.indices || target == index || !spinners[target].isActive)
            nearestOpponent(index) else target
        val self = spinners[index]
        self.dashDirection = (spinners[t].position - self.position).normalized()
        self.velocity = self.dashDirection * config.dashSpeed
        self.dashTimer = config.dashDuration
        self.boostTimer = config.dashBoostDuration
        self.dashCooldown = config.dashCooldown
        self.spin -= config.dashCost
        return true
    }

    /** Ajoute une vitesse à une toupie en jeu (scénarios du tutoriel). */
    fun applyImpulse(index: Int, velocity: Vec2) {
        if (index in spinners.indices && spinners[index].isActive && !inDuel(index))
            spinners[index].velocity += velocity
    }

    /** Abandon (joueur déconnecté) : la toupie est éliminée. */
    fun forfeit(index: Int) {
        if (index !in spinners.indices || phase == Phase.Finished) return
        val s = spinners[index]
        if (s.status == Spinner.Status.Waiting) {
            s.status = Spinner.Status.Forfeit
            eliminated += index
        } else if (s.isActive) {
            eliminate(index, Spinner.Status.Forfeit)
        }
        if (phase == Phase.Running) checkEnd()
    }

    /** Avance de dt secondes (au plus 0,1 s par appel), en pas fixes. */
    fun advance(dt: Double) {
        if (phase != Phase.Running) return
        accumulator += min(dt, 0.1)
        while (accumulator >= config.fixedStep && phase == Phase.Running) {
            step(config.fixedStep)
            accumulator -= config.fixedStep
        }
    }

    /** Chocs survenus depuis le dernier appel (vidés). */
    fun takeImpacts(): List<ImpactEvent> = impacts.toList().also { impacts.clear() }

    // --- Formules ------------------------------------------------------------

    fun passiveLossPerSecond(s: Spinner): Double {
        val enduranceFactor = max(config.minReduction, 1.0 - config.passivePerEndurance * s.stats.endurance)
        val frost = if (s.isSlowed) 1.0 + config.slowWear * s.slowStrength else 1.0
        return config.passiveBase * enduranceFactor * stability(s) * frost * (1.0 + config.speedDrain * s.velocity.length())
    }

    fun effectiveAttack(s: Spinner): Double {
        val boost = if (s.isBoosted) config.dashAttackMultiplier else 1.0
        return s.stats.attack * boost * (0.6 + 0.4 * s.spinRatio)
    }

    fun collisionDamage(attacker: Spinner, target: Spinner, approach: Double): Double {
        val impact = min(config.maxImpact, config.minImpact + approach / config.impactRef)
        val mitigation = max(config.minReduction, 1.0 - config.damagePerDefense * target.stats.defense)
        val spinFactor = if (attacker.spinSign != target.spinSign) config.oppositeSpinDamage else 1.0
        return config.damageBase * effectiveAttack(attacker) * impact * mitigation * spinFactor * stability(target)
    }

    fun knockback(attacker: Spinner, target: Spinner, approach: Double): Double {
        val push = config.knockbackBase + config.knockbackPerAttack * effectiveAttack(attacker) +
            config.knockbackPerAttackerDefense * attacker.stats.defense + config.knockbackApproach * approach
        val spinFactor = if (attacker.spinSign == target.spinSign) config.sameSpinKnockback else 1.0
        val resistance = max(config.minReduction, 1.0 - config.knockbackPerDefense * target.stats.defense)
        return push * spinFactor * resistance * stability(target)
    }

    private fun stability(s: Spinner) =
        if (classify(s.stats) == TopType.Balance) 1.0 - config.balanceStability else 1.0

    private fun mass(s: Spinner) = 1.0 + config.massPerDefense * s.stats.defense

    // --- Simulation ----------------------------------------------------------

    private fun step(h: Double) {
        spinners.forEachIndexed { i, s -> if (s.isActive && !inDuel(i)) integrate(s, h) }
        updateDuel(h)
        for (k in pairCooldowns.indices) pairCooldowns[k] = max(0.0, pairCooldowns[k] - h)
        for (i in spinners.indices) for (j in i + 1 until playerCount) resolveCollision(i, j)
        updateProjectiles(h)

        // Éliminations de ce pas, les plus faibles d'abord (départage du classement).
        val order = spinners.indices.filter { spinners[it].isActive }.sortedBy { spinners[it].spin }
        for (i in order) {
            val s = spinners[i]
            applyRim(s)
            if (s.spin <= 0.0) {
                s.spin = 0.0
                eliminate(i, Spinner.Status.Stopped)
            } else if (geometry.level(s.position) > 1.0) {
                eliminate(i, Spinner.Status.RingOut)
            }
        }
        elapsed += h
        checkEnd()
    }

    private fun eliminate(index: Int, status: Spinner.Status) {
        val s = spinners[index]
        s.status = status
        s.dashTimer = 0.0
        eliminated += index
        if (inDuel(index)) duel.active = false // Abandon pendant un duel.
        // Mêlée : la toupie qui a provoqué l'élimination récupère de l'endurance,
        // seulement si le combat continue.
        val active = spinners.count { it.isActive }
        if (active >= 2 && status != Spinner.Status.Forfeit && s.lastHitBy >= 0 &&
            elapsed - s.lastHitTime <= config.knockoutWindow
        ) {
            val striker = spinners[s.lastHitBy]
            if (striker.isActive) striker.spin = min(striker.maxSpin, striker.spin + config.knockoutReward)
        }
    }

    /** Avec rebord, une toupie lente rebondit ; une toupie rapide le franchit. Sans rebord, rien ne la retient. */
    private fun applyRim(s: Spinner) {
        if (!stadium.hasRim) return
        if (geometry.level(s.position, config.topRadius) <= 1.0) {
            s.escaping = false
            return
        }
        if (s.escaping) return
        val normal = geometry.outwardNormal(s.position)
        val outward = s.velocity.dot(normal)
        if (outward > config.rimEscapeSpeed) {
            s.escaping = true
            return
        }
        if (outward > 0.0) s.velocity -= normal * (outward * (1.0 + config.rimRestitution))
        s.position = geometry.projectInside(s.position, config.topRadius)
    }

    private fun integrate(s: Spinner, h: Double) {
        val ratio = s.spinRatio
        if (s.isDashing) {
            s.velocity = s.dashDirection * config.dashSpeed
            s.dashTimer -= h
            if (s.dashTimer <= 0.0) {
                s.dashTimer = 0.0
                s.velocity = s.dashDirection * config.postDashSpeed
            }
        } else {
            val control = if (s.isSlowed) 1.0 - s.slowStrength else 1.0
            // Pente de la cuvette : ramène vers le centre.
            var acceleration = s.position * -config.slopeStrength
            // Propulsion due à la rotation, dans le sens inverse de celle-ci.
            val r = s.position.length()
            if (r > 1e-6) {
                val clockwiseTangent = s.position.perpendicular() / r
                val nearCenter = min(1.0, r / 0.15)
                val orbit = (config.orbitBase + config.orbitPerAttack * s.stats.attack) * (0.4 + 0.6 * ratio) * nearCenter
                acceleration += clockwiseTangent * (-s.spinSign * orbit * control)
                acceleration -= s.position / r * (config.centerPull * nearCenter)
            }
            // Pilotage : légère correction de trajectoire.
            if (!s.isSlowed && s.steer.lengthSquared() > 1e-9) acceleration += s.steer * config.steerAcceleration
            val friction = config.friction * (if (s.isSlowed) 1.0 + 2.0 * s.slowStrength else 1.0)
            acceleration -= s.velocity * friction
            // Une toupie à bout de souffle vacille.
            if (ratio < config.wobbleThreshold) {
                val strength = config.wobbleStrength * (1.0 - ratio / config.wobbleThreshold)
                acceleration += Vec2.fromAngle(rng.nextDouble(0.0, 2.0 * PI)) * strength
            }
            s.velocity += acceleration * h
        }
        s.position += s.velocity * h
        s.angle = (s.angle + s.spinSign * (6.0 + 30.0 * ratio) * h) % (2.0 * PI)
        s.spin -= passiveLossPerSecond(s) * h
        s.dashCooldown = max(0.0, s.dashCooldown - h)
        s.boostTimer = max(0.0, s.boostTimer - h)
        s.slowTimer = max(0.0, s.slowTimer - h)
        s.disarmTimer = max(0.0, s.disarmTimer - h)
    }

    private fun resolveCollision(i: Int, j: Int) {
        val a = spinners[i]
        val b = spinners[j]
        if (!a.isActive || !b.isActive) return
        val delta = b.position - a.position
        val distance = delta.length()
        val minDistance = 2.0 * config.topRadius
        if (distance >= minDistance) return
        val n = if (distance > 1e-9) delta / distance else Vec2(1.0, 0.0)

        // Duellistes : immobiles, ils repoussent simplement les autres toupies.
        val aFrozen = inDuel(i)
        val bFrozen = inDuel(j)
        if (aFrozen && bFrozen) return
        val overlap = minDistance - distance
        if (aFrozen || bFrozen) {
            val moving = if (aFrozen) b else a
            val away = if (aFrozen) n else -n
            moving.position += away * overlap
            val toward = -moving.velocity.dot(away)
            if (toward > 0.0) moving.velocity += away * (toward * (1.0 + config.rimRestitution))
            return
        }
        val ma = mass(a)
        val mb = mass(b)
        val total = ma + mb
        a.position -= n * (overlap * mb / total)
        b.position += n * (overlap * ma / total)

        // Le choc absorbe la vitesse de rapprochement ; le recul fait la séparation.
        val closing = (a.velocity - b.velocity).dot(n)
        val aTowardB = a.velocity.dot(n)
        val bTowardA = -b.velocity.dot(n)
        if (aTowardB > 0.0) a.velocity -= n * aTowardB
        if (bTowardA > 0.0) b.velocity += n * bTowardA

        val cooldownIndex = i * playerCount + j
        if (pairCooldowns[cooldownIndex] > 0.0) return // Contact prolongé : un seul choc.

        val approach = max(0.0, closing)
        if (a.isDashing && b.isDashing && !duel.active) {
            startDuel(i, j, n, approach)
            return
        }
        val damageToA = collisionDamage(b, a, approach)
        val damageToB = collisionDamage(a, b, approach)
        a.velocity -= n * knockback(b, a, approach)
        b.velocity += n * knockback(a, b, approach)

        // Déviation latérale due à la rotation.
        val t = n.perpendicular()
        val deflection = config.spinDeflection * (0.5 + approach)
        val fromA = a.spinSign * a.spinRatio * deflection
        val fromB = b.spinSign * b.spinRatio * deflection
        b.velocity += t * fromA
        a.velocity -= t * fromA
        a.velocity -= t * fromB
        b.velocity += t * fromB

        a.spin -= damageToA
        b.spin -= damageToB
        a.lastHitBy = j
        b.lastHitBy = i
        a.lastHitTime = elapsed
        b.lastHitTime = elapsed
        a.dashTimer = 0.0
        b.dashTimer = 0.0
        pairCooldowns[cooldownIndex] = config.contactCooldown
        impacts += ImpactEvent(a.position + n * config.topRadius, min(1.0, (damageToA + damageToB) / 20.0), i, j)
    }

    // --- Coups spéciaux ------------------------------------------------------

    private fun distanceToSegment(p: Vec2, a: Vec2, b: Vec2): Double {
        val ab = b - a
        val len2 = ab.lengthSquared()
        val t = if (len2 > 1e-12) ((p - a).dot(ab) / len2).coerceIn(0.0, 1.0) else 0.0
        return (p - (a + ab * t)).length()
    }

    private fun updateProjectiles(h: Double) {
        for (p in projectiles) {
            val move = SpecialMove.find(p.move)
            if (move == null) {
                p.traveled = p.range + 1.0
                continue
            }
            if (p.ring) {
                val previous = p.radius
                p.radius += p.speed * h
                p.traveled = p.radius
                for (j in spinners.indices) {
                    val t = spinners[j]
                    if (j == p.owner || !t.isActive || inDuel(j) || j in p.hit) continue
                    val d = (t.position - p.origin).length()
                    if (d + config.topRadius >= previous - p.width && d - config.topRadius <= p.radius + p.width &&
                        d <= p.range + config.topRadius
                    ) {
                        p.hit += j
                        applySpecialHit(p, move, j)
                    }
                }
            } else {
                val previous = p.position
                p.position += p.direction * (p.speed * h)
                p.traveled += p.speed * h
                for (j in spinners.indices) {
                    val t = spinners[j]
                    if (j == p.owner || !t.isActive || inDuel(j)) continue
                    if (distanceToSegment(t.position, previous, p.position) <= config.topRadius + p.width) {
                        p.hit += j
                        applySpecialHit(p, move, j)
                        p.traveled = p.range + 1.0 // Absorbée.
                        break
                    }
                }
                if (geometry.level(p.position) > 1.2) p.traveled = p.range + 1.0
            }
        }
        projectiles.removeAll { it.traveled > it.range }
    }

    private fun applySpecialHit(p: Projectile, move: SpecialMove, target: Int) {
        val t = spinners[target]
        val owner = spinners[p.owner]
        val mitigation = max(config.minReduction, 1.0 - config.damagePerDefense * t.stats.defense)
        val damage = move.damage * config.specialDamageScale * mitigation * stability(t)
        t.spin -= damage
        val direction = if (p.ring) (t.position - p.origin).normalized() else p.direction
        val resistance = max(config.minReduction, 1.0 - config.knockbackPerDefense * t.stats.defense)
        t.velocity += direction * (move.knockback * config.specialKnockbackScale * resistance * stability(t))

        when (move.effect) {
            SpecialMove.Effect.None -> Unit
            SpecialMove.Effect.PullToCenter -> {
                t.velocity = (-t.position).normalized() * move.effectStrength
                t.dashTimer = 0.0
            }
            SpecialMove.Effect.Slow -> {
                t.slowTimer = move.effectDuration
                t.slowStrength = move.effectStrength
                t.dashTimer = 0.0
            }
            SpecialMove.Effect.Teleport -> {
                val dir = Vec2.fromAngle(rng.nextDouble(0.0, 2.0 * PI))
                val level = max(1e-6, geometry.level(dir))
                t.position = dir * (move.effectStrength / level)
                t.velocity = dir * (move.knockback * config.specialKnockbackScale)
                t.dashTimer = 0.0
                t.escaping = false
            }
            SpecialMove.Effect.Drain ->
                if (owner.isActive) owner.spin = min(owner.maxSpin, owner.spin + damage * move.effectStrength)
            SpecialMove.Effect.Disarm -> {
                t.disarmTimer = move.effectDuration
                t.dashTimer = 0.0
            }
        }
        t.lastHitBy = p.owner
        t.lastHitTime = elapsed
        impacts += ImpactEvent(t.position, min(1.0, 0.4 + damage / 20.0), p.owner, target, special = true, color = move.color)
    }

    // --- Duels ---------------------------------------------------------------

    private fun startDuel(i: Int, j: Int, normal: Vec2, approach: Double) {
        duel.active = true
        duel.first = i
        duel.second = j
        duel.normal = normal
        duel.point = spinners[i].position + normal * config.topRadius
        duel.approach = approach
        for (side in duel.sides) {
            side.cursor = -1.0; side.direction = 1.0; side.speed = config.duelCursorSpeed
            side.zone = config.duelZoneStart; side.streak = 0; side.idle = 0.0; side.done = false
        }
        for (index in intArrayOf(i, j)) {
            val s = spinners[index]
            s.velocity = Vec2.ZERO
            s.dashTimer = 0.0
            s.lastHitBy = if (index == i) j else i
            s.lastHitTime = elapsed
        }
        impacts += ImpactEvent(duel.point, 1.0, i, j)
    }

    private fun updateDuel(h: Double) {
        if (!duel.active) return
        val timedOut = BooleanArray(2)
        for (k in 0..1) {
            val side = duel.sides[k]
            // Les duellistes tournent sur place (affichage) sans s'user.
            val s = spinners[if (k == 0) duel.first else duel.second]
            s.angle = (s.angle + s.spinSign * (6.0 + 30.0 * s.spinRatio) * h) % (2.0 * PI)
            if (side.done) continue
            side.advance(h)
            side.idle += h
            timedOut[k] = side.idle > config.duelTimeout
        }
        if (timedOut[0] && timedOut[1]) resolveDuel(-1)
        else if (timedOut[0] || timedOut[1]) resolveDuel(if (timedOut[0]) 0 else 1)
    }

    /** Appui d'un duelliste ; `cursor` est la position du curseur qu'il a vue. */
    fun duelPress(index: Int, cursor: Double): Boolean {
        val k = duel.sideOf(index)
        if (k < 0 || phase != Phase.Running || duel.sides[k].done) return false
        val side = duel.sides[k]
        side.idle = 0.0
        if (abs(cursor.coerceIn(-1.0, 1.0)) > side.zone) {
            resolveDuel(k)
            return true
        }
        if (side.zone <= config.duelZoneMin + 1e-9) {
            side.streak++
            side.done = side.streak >= config.duelStreak
        } else {
            side.zone = max(config.duelZoneMin, side.zone * config.duelZoneShrink)
        }
        side.speed *= config.duelSpeedGain
        if (duel.sides[0].done && duel.sides[1].done) resolveDuel(-1)
        return true
    }

    private fun resolveDuel(loserSide: Int) {
        duel.active = false
        val a = spinners[duel.first]
        val b = spinners[duel.second]
        val n = duel.normal
        val event: ImpactEvent
        if (loserSide < 0) {
            // Nul : les toupies se repoussent sans dégâts.
            a.velocity -= n * knockback(b, a, duel.approach)
            b.velocity += n * knockback(a, b, duel.approach)
            event = ImpactEvent(duel.point, 0.6, duel.first, duel.second, duelLoser = -1)
        } else {
            val loser = if (loserSide == 0) a else b
            val winner = (if (loserSide == 0) b else a).copy()
            winner.boostTimer = max(winner.boostTimer, 1e-3) // Dégâts d'un dash.
            val damage = config.duelDamageMultiplier * collisionDamage(winner, loser, duel.approach)
            loser.spin -= damage
            val away = if (loserSide == 0) -n else n
            loser.velocity += away * knockback(winner, loser, duel.approach)
            event = ImpactEvent(duel.point, min(1.0, 0.4 + damage / 20.0), duel.first, duel.second,
                duelLoser = if (loserSide == 0) duel.first else duel.second)
        }
        a.boostTimer = 0.0
        b.boostTimer = 0.0
        pairCooldowns[min(duel.first, duel.second) * playerCount + max(duel.first, duel.second)] = config.contactCooldown
        impacts += event
    }

    // --- Fin -----------------------------------------------------------------

    private fun checkEnd() {
        if (phase != Phase.Running) return
        val active = spinners.indices.filter { spinners[it].isActive }
        fun reasonFor(loser: Spinner) = when (loser.status) {
            Spinner.Status.RingOut -> EndReason.RingOut
            Spinner.Status.Forfeit -> EndReason.Forfeit
            else -> EndReason.SpinOut
        }
        when {
            active.size == 1 -> {
                val last = if (eliminated.isEmpty()) active[0] else eliminated.last()
                finish(active[0], reasonFor(spinners[last]))
            }
            active.isEmpty() -> {
                // Toutes éliminées au même pas : la dernière éliminée gagne, sauf égalité parfaite.
                val last = eliminated.last()
                val previous = if (eliminated.size > 1) eliminated[eliminated.size - 2] else -1
                val tie = previous >= 0 && abs(spinners[last].spin - spinners[previous].spin) < 1e-9
                finish(if (tie) -1 else last, reasonFor(spinners[last]))
            }
            config.maxDuration > 0.0 && elapsed >= config.maxDuration -> {
                val sorted = active.sortedByDescending { spinners[it].spin }
                val tie = abs(spinners[sorted[0]].spin - spinners[sorted[1]].spin) < 0.5
                finish(if (tie) -1 else sorted[0], EndReason.Timeout)
            }
        }
    }

    private fun finish(winner: Int, reason: EndReason) {
        phase = Phase.Finished
        this.winner = winner
        endReason = reason
        duel.active = false
        spinners.forEach { it.dashTimer = 0.0 }
        // Classement : toupies encore en jeu (par endurance), puis éliminées de la plus tardive à la plus précoce.
        val result = spinners.indices.filter { spinners[it].isActive }.sortedByDescending { spinners[it].spin }.toMutableList()
        for (i in eliminated.asReversed()) if (i !in result) result += i
        if (winner >= 0 && result.firstOrNull() != winner) {
            result.remove(winner)
            result.add(0, winner)
        }
        ranking = result
    }

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 4
    }
}
