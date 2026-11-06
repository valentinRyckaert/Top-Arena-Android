package fr.toupiearena.mobile.game

/**
 * Coup spécial débloqué par le boulon : une vague qui file en ligne droite vers
 * l'adversaire le plus proche (esquivable), ou un anneau qui s'élargit et touche
 * tous les adversaires à portée. Valeurs identiques à la version PC.
 */
data class SpecialMove(
    val id: String,
    val name: String,
    val description: String,
    val shape: Shape = Shape.Wave,
    val damage: Double = 0.0,
    val knockback: Double = 0.0,
    val speed: Double = 1.0,
    val range: Double = 1.5,
    val width: Double = 0.08,
    val effect: Effect = Effect.None,
    val effectStrength: Double = 0.0,
    val effectDuration: Double = 0.0,
    val color: Long = 0xffffff,
) {
    enum class Shape { Wave, Ring }
    enum class Effect { None, PullToCenter, Slow, Teleport, Drain, Disarm }

    val hitsEveryone get() = shape == Shape.Ring

    companion object {
        val all: List<SpecialMove> = listOf(
            SpecialMove("onde", "Onde de choc",
                "Anneau qui repousse violemment toutes les toupies proches : idéal pour éjecter une cible déjà au " +
                    "bord. Dégâts faibles, portée courte.",
                shape = Shape.Ring, damage = 12.5, knockback = 1.25, speed = 1.2, range = 0.75, width = 0.06,
                color = 0x7fd6ff),
            SpecialMove("lance", "Lance d'acier",
                "Trait fin qui inflige les plus lourds dégâts à une cible, sans autre effet. Presque aucun recul, et " +
                    "assez lent pour être esquivé de loin.",
                damage = 24.0, knockback = 0.3, speed = 1.1, range = 1.8, width = 0.05, color = 0xe8eef5),
            SpecialMove("givre", "Souffle de givre",
                "Vague glacée : pendant 3 secondes, la cible est fortement ralentie, ne peut plus piloter et le froid " +
                    "use sa rotation trois fois plus vite. Dégâts moyens.",
                damage = 18.5, knockback = 0.1, speed = 1.0, range = 1.6, width = 0.11, effect = Effect.Slow,
                effectStrength = 0.95, effectDuration = 3.1, color = 0x9be7ff),
            SpecialMove("vortex", "Vortex",
                "Anneau à longue portée qui aspire violemment toutes les toupies vers le centre du stadium : provoque " +
                    "la mêlée, empêche de fuir. Dégâts modérés.",
                shape = Shape.Ring, damage = 17.0, speed = 1.0, range = 1.4, width = 0.07,
                effect = Effect.PullToCenter, effectStrength = 2.0, color = 0xb58cff),
            SpecialMove("distorsion", "Distorsion",
                "Vague lente qui blesse la cible et la téléporte près du bord du stadium, lancée vers l'extérieur : " +
                    "elle risque l'éjection. Facile à esquiver de loin.",
                damage = 23.0, speed = 0.9, range = 1.6, width = 0.1, effect = Effect.Teleport,
                effectStrength = 0.868, knockback = 0.5, color = 0xff7ad9),
            SpecialMove("siphon", "Siphon",
                "Vague qui vole l'endurance de la cible : la toupie récupère 60 % de ce qu'elle inflige. Dégâts " +
                    "modérés, recul faible.",
                damage = 12.0, knockback = 0.15, speed = 1.0, range = 1.4, width = 0.08, effect = Effect.Drain,
                effectStrength = 0.6, color = 0xff5c6c),
            SpecialMove("seisme", "Séisme",
                "Secousse qui blesse toutes les toupies très proches et les empêche de dasher ou de lancer leur coup " +
                    "spécial pendant 6 secondes. Portée très courte.",
                shape = Shape.Ring, damage = 18.0, knockback = 0.5, speed = 1.6, range = 0.55, width = 0.07,
                effect = Effect.Disarm, effectDuration = 6.0, color = 0xffb347),
        )

        fun find(id: String?) = all.firstOrNull { it.id == id }
    }
}
