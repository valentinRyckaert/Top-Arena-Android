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
    /** PullToCenter : aspire la cible vers le centre du stadium (sans le dépasser). */
    enum class Effect { None, PullToCenter, Slow, Teleport, Drain, Disarm }

    val hitsEveryone get() = shape == Shape.Ring

    companion object {
        val all: List<SpecialMove> = listOf(
            SpecialMove("onde", "Onde de choc",
                "Anneau qui repousse violemment toutes les toupies proches : idéal pour éjecter une cible déjà au " +
                    "bord. Dégâts faibles, portée courte.",
                // Le recul fait tout : plus fort, dégâts et portée réduits.
                shape = Shape.Ring, damage = 6.0, knockback = 1.4, speed = 1.2, range = 0.65, width = 0.06,
                color = 0x34d6ff),
            SpecialMove("lance", "Lance d'acier",
                "Trait fin qui inflige les plus lourds dégâts à une cible, sans autre effet. Presque aucun recul, et " +
                    "assez lent pour être esquivé de loin.",
                damage = 24.0, knockback = 0.3, speed = 1.1, range = 1.8, width = 0.05, color = 0xc9d8ff),
            SpecialMove("givre", "Souffle de givre",
                "Vague glacée : pendant 3 secondes, la cible est fortement ralentie, ne peut plus piloter et le froid " +
                    "use sa rotation trois fois plus vite. Dégâts modérés.",
                damage = 18.5, knockback = 0.1, speed = 1.0, range = 1.6, width = 0.11, effect = Effect.Slow,
                effectStrength = 0.95, effectDuration = 3.1, color = 0x8ff3ff),
            SpecialMove("vortex", "Vortex",
                "Anneau qui balaie tout le stadium et aspire violemment toutes les toupies vers le centre : provoque " +
                    "la mêlée et empêche de fuir (pas de dash pendant l'aspiration). Dégâts modérés, mais les cibles " +
                    "s'éloignent du bord.",
                // Portée : tout le stadium, d'où que parte l'anneau. Aspiration (1/s) : 90 % du chemin en 0,9 s.
                shape = Shape.Ring, damage = 20.0, speed = 1.8, range = 2.2, width = 0.07,
                effect = Effect.PullToCenter, effectStrength = 2.5, effectDuration = 0.9, color = 0x8f5bff),
            SpecialMove("distorsion", "Distorsion",
                "Vague lente qui blesse la cible et la téléporte près du bord du stadium, lancée vers l'extérieur : " +
                    "elle risque l'éjection. Facile à esquiver de loin.",
                damage = 23.0, speed = 0.9, range = 1.6, width = 0.1, effect = Effect.Teleport,
                effectStrength = 0.868, knockback = 0.5, color = 0xd24bff),
            SpecialMove("siphon", "Siphon",
                "Vague qui vole l'endurance de la cible : la toupie récupère 60 % de ce qu'elle inflige. Dégâts " +
                    "modérés, recul faible.",
                damage = 12.0, knockback = 0.15, speed = 1.0, range = 1.4, width = 0.08, effect = Effect.Drain,
                effectStrength = 0.6, color = 0xff3b6b),
            SpecialMove("seisme", "Séisme",
                "Secousse qui blesse toutes les toupies très proches et les empêche de dasher ou de lancer leur coup " +
                    "spécial pendant 6 secondes. Portée très courte.",
                shape = Shape.Ring, damage = 18.0, knockback = 0.5, speed = 1.6, range = 0.55, width = 0.07,
                effect = Effect.Disarm, effectDuration = 6.0, color = 0xb8ff2e),
        )

        fun find(id: String?) = all.firstOrNull { it.id == id }
    }
}
