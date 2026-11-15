package fr.toupiearena.mobile.core

import java.security.MessageDigest
import fr.toupiearena.mobile.core.SpinDirection.Clockwise as CW
import fr.toupiearena.mobile.core.SpinDirection.CounterClockwise as CCW

/**
 * Catalogue des pièces, identique à celui de la version PC (même ordre, mêmes
 * valeurs) : l'empreinte doit correspondre pour jouer ensemble en réseau.
 */
object PartCatalog {
    const val WHEEL_BUDGET = 10
    const val MIDDLE_BUDGET = 6
    const val TIP_BUDGET = 7

    private fun wheel(id: String, name: String, s: Stats, spin: SpinDirection, hue: Int) =
        Part(id, name, PartKind.Wheel, s, spin, hue)
    private fun middle(id: String, name: String, s: Stats, hue: Int) = Part(id, name, PartKind.Middle, s, hue = hue)
    private fun tip(id: String, name: String, s: Stats, hue: Int) = Part(id, name, PartKind.Tip, s, hue = hue)
    private fun bolt(id: String, name: String, special: String, hue: Int) =
        Part(id, name, PartKind.Bolt, hue = hue, special = special)

    val all: List<Part> = listOf(
        // Roues métalliques (budget 10) : elles fixent le sens de rotation.
        wheel("RM1", "Rempart", Stats(3, 5, 2), CW, 210),
        wheel("RM2", "Lame Cyclone", Stats(6, 2, 2), CW, 0),
        wheel("RM3", "Bastion", Stats(2, 6, 2), CCW, 120),
        wheel("RM4", "Éclipse", Stats(2, 2, 6), CCW, 275),
        wheel("RM5", "Prisme", Stats(4, 3, 3), CW, 45),
        wheel("RM6", "Tonnerre", Stats(5, 3, 2), CCW, 25),
        wheel("RM7", "Ouragan", Stats(2, 3, 5), CW, 190),
        wheel("RM8", "Orbite", Stats(4, 2, 4), CCW, 320),
        wheel("RM9", "Météore", Stats(7, 2, 1), CW, 15),
        wheel("RM10", "Titan", Stats(1, 7, 2), CCW, 100),
        wheel("RM11", "Nébuleuse", Stats(1, 2, 7), CW, 250),
        wheel("RM12", "Faucon", Stats(5, 2, 3), CW, 340),
        wheel("RM13", "Zéphyr", Stats(3, 2, 5), CCW, 170),
        wheel("RM14", "Golem", Stats(3, 6, 1), CW, 80),
        // Parties intermédiaires (budget 6).
        middle("PI1", "Noyau", Stats(2, 3, 1), 200),
        middle("PI2", "Griffe", Stats(4, 1, 1), 10),
        middle("PI3", "Carapace", Stats(1, 4, 1), 130),
        middle("PI4", "Anneau", Stats(1, 1, 4), 280),
        middle("PI5", "Axe", Stats(2, 2, 2), 50),
        middle("PI6", "Turbine", Stats(3, 1, 2), 30),
        middle("PI7", "Lame", Stats(3, 2, 1), 0),
        middle("PI8", "Bouclier", Stats(1, 3, 2), 140),
        middle("PI9", "Spirale", Stats(2, 1, 3), 300),
        middle("PI10", "Contrepoids", Stats(1, 2, 3), 230),
        // Pointes (budget 7).
        tip("P1", "Pivot", Stats(1, 4, 2), 220),
        tip("P2", "Gomme", Stats(4, 1, 2), 5),
        tip("P3", "Aiguille", Stats(1, 1, 5), 290),
        tip("P4", "Bille", Stats(2, 3, 2), 160),
        tip("P5", "Plateau", Stats(3, 2, 2), 40),
        tip("P6", "Pointe Fine", Stats(1, 2, 4), 260),
        tip("P7", "Crampon", Stats(5, 1, 1), 20),
        tip("P8", "Dôme", Stats(1, 5, 1), 210),
        tip("P9", "Rotule", Stats(2, 2, 3), 180),
        tip("P10", "Sabot", Stats(3, 3, 1), 60),
        // Boulons (aucune statistique) : chacun débloque un coup spécial.
        bolt("B1", "Boulon Tempête", "onde", 200),
        bolt("B2", "Boulon Perçant", "lance", 0),
        bolt("B3", "Boulon Polaire", "givre", 190),
        bolt("B4", "Boulon Abysse", "vortex", 270),
        bolt("B5", "Boulon Spectral", "distorsion", 315),
        bolt("B6", "Boulon Vampire", "siphon", 350),
        bolt("B7", "Boulon Tellurique", "seisme", 30),
    )

    fun parts(kind: PartKind) = all.filter { it.kind == kind }
    fun find(id: String) = all.firstOrNull { it.id == id }

    val presets: List<Top> by lazy {
        listOf(
            arrayOf("Vortyx", "RM2", "PI2", "P2", "B2"),
            arrayOf("Bastaron", "RM3", "PI3", "P1", "B1"),
            arrayOf("Celsa", "RM4", "PI4", "P3", "B6"),
            arrayOf("Orvane", "RM5", "PI5", "P4", "B3"),
            arrayOf("Celestia", "RM1", "PI1", "P1", "B7"),
            arrayOf("Kyron", "RM9", "PI7", "P7", "B5"),
            arrayOf("Gorrath", "RM10", "PI8", "P8", "B4"),
            arrayOf("Sylphae", "RM13", "PI9", "P9", "B3"),
        ).map { r -> Top(r[0], find(r[1])!!, find(r[2])!!, find(r[3])!!, find(r[4])!!) }
    }

    fun budget(kind: PartKind) = when (kind) {
        PartKind.Wheel -> WHEEL_BUDGET
        PartKind.Middle -> MIDDLE_BUDGET
        PartKind.Tip -> TIP_BUDGET
        PartKind.Bolt -> 0
    }

    /** Valeur maximale atteignable par une stat (échelle des barres). */
    val maxStatValue: Int by lazy {
        listOf(Stats::attack, Stats::defense, Stats::endurance).maxOf { f ->
            listOf(PartKind.Wheel, PartKind.Middle, PartKind.Tip).sumOf { k -> parts(k).maxOf { f(it.stats) } }
        }
    }

    /** Empreinte du catalogue, calculée exactement comme sur PC (SHA-1, 12 caractères). */
    val fingerprint: String by lazy {
        val sha = MessageDigest.getInstance("SHA-1")
        for (p in all) {
            sha.update("${p.id}:${p.kind.ordinal}:${p.stats.attack}:${p.stats.defense}:${p.stats.endurance}:${p.spin.ordinal};"
                .toByteArray(Charsets.UTF_8))
        }
        sha.digest().joinToString("") { "%02x".format(it) }.take(12)
    }
}
