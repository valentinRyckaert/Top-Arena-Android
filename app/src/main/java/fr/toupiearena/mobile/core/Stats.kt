package fr.toupiearena.mobile.core

/** Statistiques d'une pièce ou d'une toupie complète. */
data class Stats(val attack: Int = 0, val defense: Int = 0, val endurance: Int = 0) {
    operator fun plus(o: Stats) = Stats(attack + o.attack, defense + o.defense, endurance + o.endurance)
    val total get() = attack + defense + endurance
}

/** Type d'une toupie, déduit de sa statistique dominante. */
enum class TopType(val label: String) {
    Attack("Attaque"), Defense("Défense"), Endurance("Endurance"), Balance("Équilibre")
}

/** Sens de rotation (vu du dessus). L'ordre des valeurs compte : il entre dans l'empreinte du catalogue. */
enum class SpinDirection(val label: String) {
    Clockwise("Horaire"), CounterClockwise("Anti-horaire");

    /** +1 horaire, -1 anti-horaire (repère écran, Y vers le bas). */
    val sign get() = if (this == Clockwise) 1 else -1
}

/** La stat strictement la plus élevée l'emporte ; égalité sur le maximum : équilibre. */
fun classify(s: Stats): TopType = when {
    s.attack > s.defense && s.attack > s.endurance -> TopType.Attack
    s.defense > s.attack && s.defense > s.endurance -> TopType.Defense
    s.endurance > s.attack && s.endurance > s.defense -> TopType.Endurance
    else -> TopType.Balance
}
