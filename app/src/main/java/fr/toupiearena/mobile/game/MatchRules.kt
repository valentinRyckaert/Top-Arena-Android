package fr.toupiearena.mobile.game

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Réglages de partie (section « Personnalisation ») : multiplicateurs (1 = normal). */
data class MatchRules(
    val speed: Double = 1.0,
    val knockback: Double = 1.0,
    val wear: Double = 1.0,
    val damage: Double = 1.0,
    val maxEndurance: Double = 100.0,
    val timeLimit: Double = 0.0,
    val steering: Double = 1.0,
    val specials: Boolean = true,
    val dashPower: Double = 1.0,
) {
    fun apply(base: BattleConfig = BattleConfig()): BattleConfig {
        val v = max(0.1, speed)
        return base.copy(
            launchSpeed = base.launchSpeed * v,
            orbitBase = base.orbitBase * v,
            orbitPerAttack = base.orbitPerAttack * v,
            dashSpeed = base.dashSpeed * v,
            steerAcceleration = base.steerAcceleration * v * steering,
            knockbackBase = base.knockbackBase * knockback,
            knockbackPerAttack = base.knockbackPerAttack * knockback,
            knockbackPerAttackerDefense = base.knockbackPerAttackerDefense * knockback,
            knockbackApproach = base.knockbackApproach * knockback,
            specialKnockbackScale = base.specialKnockbackScale * knockback,
            passiveBase = base.passiveBase * wear,
            damageBase = base.damageBase * damage,
            specialDamageScale = base.specialDamageScale * damage,
            dashAttackMultiplier = 1.0 + (base.dashAttackMultiplier - 1.0) * dashPower,
            maxSpin = max(10.0, maxEndurance),
            maxDuration = max(0.0, timeLimit),
            specialsEnabled = base.specialsEnabled && specials,
        )
    }

    fun sameAs(o: MatchRules): Boolean {
        fun same(a: Double, b: Double) = abs(a - b) < 1e-6
        return same(speed, o.speed) && same(knockback, o.knockback) && same(wear, o.wear) && same(damage, o.damage) &&
            same(maxEndurance, o.maxEndurance) && same(timeLimit, o.timeLimit) && same(steering, o.steering) &&
            specials == o.specials && same(dashPower, o.dashPower)
    }

    val isDefault get() = sameAs(MatchRules())

    /** « Vitesse 150 % · Sans limite… » ou « Règles normales ». */
    fun summary(): String {
        val parts = mutableListOf<String>()
        fun percent(label: String, value: Double) {
            if (abs(value - 1.0) >= 1e-6) parts += "$label ${(value * 100).roundToInt()} %"
        }
        percent("Vitesse", speed)
        percent("Rebonds", knockback)
        percent("Usure", wear)
        percent("Dégâts", damage)
        percent("Dash", dashPower)
        if (abs(maxEndurance - 100.0) >= 1e-6) parts += "Endurance ${maxEndurance.roundToInt()}"
        if (timeLimit > 0.0) parts += "Limite ${timeLimit.roundToInt()} s"
        if (steering <= 0.0) parts += "Sans pilotage" else percent("Pilotage", steering)
        if (!specials) parts += "Sans coups spéciaux"
        return if (parts.isEmpty()) "Règles normales" else parts.joinToString(" · ")
    }

    fun toJson(): JSONObject = JSONObject()
        .put("speed", speed).put("knockback", knockback).put("wear", wear).put("damage", damage)
        .put("maxEndurance", maxEndurance).put("timeLimit", timeLimit).put("steering", steering)
        .put("specials", specials).put("dashPower", dashPower)

    companion object {
        const val MIN_MULTIPLIER = 0.0
        const val MAX_MULTIPLIER = 3.0
        private fun m(v: Double) = v.coerceIn(MIN_MULTIPLIER, MAX_MULTIPLIER)

        fun fromJson(json: JSONObject?): MatchRules {
            val j = json ?: JSONObject()
            return MatchRules(
                speed = m(j.optDouble("speed", 1.0)),
                knockback = m(j.optDouble("knockback", 1.0)),
                wear = m(j.optDouble("wear", 1.0)),
                damage = m(j.optDouble("damage", 1.0)),
                maxEndurance = j.optDouble("maxEndurance", 100.0).coerceIn(10.0, 1000.0),
                timeLimit = j.optDouble("timeLimit", 0.0).coerceIn(0.0, 3600.0),
                steering = m(j.optDouble("steering", 1.0)),
                specials = j.optBoolean("specials", true),
                dashPower = m(j.optDouble("dashPower", 1.0)),
            )
        }
    }
}
