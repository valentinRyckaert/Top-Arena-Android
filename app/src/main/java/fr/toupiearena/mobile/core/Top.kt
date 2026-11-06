package fr.toupiearena.mobile.core

import org.json.JSONObject

/** Une toupie : boulon (coup spécial), roue métallique, partie intermédiaire et pointe. */
data class Top(
    val name: String,
    val wheel: Part,
    val middle: Part,
    val tip: Part,
    val bolt: Part = PartCatalog.parts(PartKind.Bolt).first(),
) {
    /** Somme des statistiques des pièces (le boulon n'en donne pas). */
    val stats: Stats get() = wheel.stats + middle.stats + tip.stats
    val type: TopType get() = classify(stats)
    /** Le sens de rotation est imposé par la roue métallique. */
    val spin: SpinDirection get() = wheel.spin
    val special: String get() = bolt.special

    fun part(kind: PartKind) = when (kind) {
        PartKind.Wheel -> wheel
        PartKind.Middle -> middle
        PartKind.Tip -> tip
        PartKind.Bolt -> bolt
    }

    fun withPart(part: Part) = when (part.kind) {
        PartKind.Wheel -> copy(wheel = part)
        PartKind.Middle -> copy(middle = part)
        PartKind.Tip -> copy(tip = part)
        PartKind.Bolt -> copy(bolt = part)
    }

    val isValid: Boolean
        get() = name.isNotBlank() && wheel.kind == PartKind.Wheel && middle.kind == PartKind.Middle &&
            tip.kind == PartKind.Tip && bolt.kind == PartKind.Bolt

    /** Même format que la version PC : nom et identifiants des pièces. */
    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("wheel", wheel.id).put("middle", middle.id)
        .put("tip", tip.id).put("bolt", bolt.id)

    companion object {
        fun fromJson(json: JSONObject?): Top? {
            json ?: return null
            val wheel = PartCatalog.find(json.optString("wheel")) ?: return null
            val middle = PartCatalog.find(json.optString("middle")) ?: return null
            val tip = PartCatalog.find(json.optString("tip")) ?: return null
            val bolt = if (json.has("bolt")) {
                PartCatalog.find(json.optString("bolt"))?.takeIf { it.kind == PartKind.Bolt } ?: return null
            } else {
                PartCatalog.parts(PartKind.Bolt).first() // Toupie d'une version sans boulon.
            }
            return Top(json.optString("name"), wheel, middle, tip, bolt).takeIf { it.isValid }
        }
    }
}
