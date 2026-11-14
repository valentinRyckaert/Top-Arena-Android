package fr.toupiearena.mobile.game

/** Duel entre deux toupies qui se percutent en plein dash. L'ordre des valeurs compte : il est transmis en réseau. */
enum class DuelMode(val label: String) {
    /** Appuyer quand le curseur traverse la zone verte (zone de plus en plus étroite). */
    Timing("Timing"),
    /** Rafale : tir à la corde, chaque appui pousse le curseur vers l'adversaire. */
    Rush("Rafale"),
    /** Pas de duel : choc ordinaire. */
    None("Sans duel"),
}

/**
 * Constantes de gameplay, identiques à la version PC (réglées avec son simulateur
 * d'équilibrage). Distances en rayons de stadium, durées en secondes, endurance
 * en points sur 100.
 */
data class BattleConfig(
    // --- Stadium et lancement
    var stadiumRadius: Double = 1.0,
    var topRadius: Double = 0.075,
    var startOffset: Double = 0.55,
    var launchSpeed: Double = 1.0,
    var rimEscapeSpeed: Double = 0.4223,
    var rimRestitution: Double = 0.4,
    /**
     * Rebond contre le rebord : perte d'endurance = rimDamage × vitesse vers le rebord,
     * réduite par la défense comme un choc. En dessous de rimDamageThreshold (toupie qui
     * longe le bord), rien n'est perdu.
     */
    var rimDamage: Double = 4.0,
    var rimDamageThreshold: Double = 0.08,
    // --- Mouvement
    var slopeStrength: Double = 1.3,
    var friction: Double = 0.7,
    var orbitBase: Double = 0.1924,
    var orbitPerAttack: Double = 0.0049,
    var centerPull: Double = 0.1837,
    var steerAcceleration: Double = 0.3,
    var launchPowerMin: Double = 0.5,
    var launchPowerMax: Double = 1.5,
    var wobbleThreshold: Double = 0.25,
    var wobbleStrength: Double = 1.2,
    // --- Endurance
    var maxSpin: Double = 100.0,
    var passiveBase: Double = 1.8491,
    var passivePerEndurance: Double = 0.03,
    var speedDrain: Double = 0.7327,
    // --- Chocs
    var damageBase: Double = 0.4,
    var damagePerDefense: Double = 0.0482,
    var minImpact: Double = 0.8969,
    var impactRef: Double = 2.0,
    var maxImpact: Double = 1.1305,
    var knockbackBase: Double = 0.12,
    var knockbackPerAttack: Double = 0.0715,
    var knockbackPerAttackerDefense: Double = 0.0418,
    var knockbackApproach: Double = 0.1427,
    var knockbackPerDefense: Double = 0.0279,
    var minReduction: Double = 0.15,
    var massPerDefense: Double = 0.1,
    var spinDeflection: Double = 0.1,
    var sameSpinKnockback: Double = 1.1,
    var oppositeSpinDamage: Double = 1.2014,
    var contactCooldown: Double = 0.2337,
    var balanceStability: Double = 0.02,
    // --- Dash
    var dashCost: Double = 3.5722,
    var dashSpeed: Double = 2.6,
    var dashDuration: Double = 0.15,
    var dashCooldown: Double = 1.8,
    var dashAttackMultiplier: Double = 1.1618,
    var dashBoostDuration: Double = 0.35,
    var postDashSpeed: Double = 0.45,
    // --- Coups spéciaux : une fois par partie, pour cette fraction de l'endurance courante.
    var specialCost: Double = 0.25,
    var specialDamageScale: Double = 1.0,
    var specialKnockbackScale: Double = 1.0,
    /** Toupie aspirée (vortex) : rapidité (1/s) avec laquelle sa vitesse rejoint celle de l'aspiration. */
    var pullGrip: Double = 12.0,
    /** Toupie gelée : perte passive × (1 + slowWear × intensité du gel). */
    var slowWear: Double = 2.0,
    var specialsEnabled: Boolean = true,
    // --- Duel (deux toupies qui se percutent en plein dash)
    var duelMode: DuelMode = DuelMode.Timing,
    /** Rafale : durée du duel, et avance (en appuis) qui le gagne aussitôt. */
    var rushDuration: Double = 3.5,
    var rushLimit: Int = 12,
    // Timing :
    var duelZoneStart: Double = 0.45,
    var duelZoneMin: Double = 0.14,
    var duelZoneShrink: Double = 0.72,
    var duelCursorSpeed: Double = 2.2,
    var duelSpeedGain: Double = 1.06,
    var duelStreak: Int = 3,
    var duelTimeout: Double = 3.0,
    var duelDamageMultiplier: Double = 1.5,
    // --- Mêlée (3-4 toupies)
    var knockoutReward: Double = 35.0,
    var knockoutWindow: Double = 4.0,
    // --- Partie
    var maxDuration: Double = 0.0,
    var fixedStep: Double = 1.0 / 240.0,
) {
    val dashDistance get() = dashSpeed * dashDuration
}
