package fr.toupiearena.mobile.core

/** Les quatre emplacements d'une toupie. L'ordre compte : il entre dans l'empreinte du catalogue. */
enum class PartKind(val label: String) {
    Wheel("Roue métallique"), Middle("Partie intermédiaire"), Tip("Pointe"), Bolt("Boulon")
}

/** Une pièce de toupie (boulon, roue métallique, partie intermédiaire ou pointe). */
data class Part(
    val id: String,
    val name: String,
    val kind: PartKind,
    val stats: Stats = Stats(),
    /** Sens de rotation : n'a de sens que pour une roue métallique. */
    val spin: SpinDirection = SpinDirection.Clockwise,
    /** Teinte (0-359) utilisée pour dessiner la pièce. */
    val hue: Int = 0,
    /** Boulon uniquement : identifiant du coup spécial débloqué. */
    val special: String = "",
)
