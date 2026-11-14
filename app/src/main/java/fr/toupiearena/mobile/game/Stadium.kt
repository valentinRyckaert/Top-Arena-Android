package fr.toupiearena.mobile.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Forme d'un stadium centrée sur l'origine. level(p) vaut 0 au centre et 1 sur
 * le bord ; `inset` réduit la forme (rayon d'une toupie).
 */
data class StadiumGeometry(
    val kind: Kind = Kind.Circle,
    val radius: Double = 1.0,
    val sides: Int = 8,
    val aspect: Double = 1.0,
) {
    enum class Kind { Circle, Polygon, Ellipse }

    private val apothem get() = radius * cos(PI / sides)
    private fun sideNormal(i: Int) = Vec2.fromAngle(i * 2.0 * PI / sides)

    fun level(p: Vec2, inset: Double = 0.0): Double = when (kind) {
        Kind.Circle -> p.length() / max(1e-9, radius - inset)
        Kind.Polygon -> (0 until sides).maxOf { p.dot(sideNormal(it)) } / max(1e-9, apothem - inset)
        Kind.Ellipse -> hypot(p.x / max(1e-9, radius - inset), p.y / max(1e-9, radius * aspect - inset))
    }

    /** Normale sortante du bord le plus proche de p. */
    fun outwardNormal(p: Vec2): Vec2 {
        val n = when (kind) {
            Kind.Circle -> p.normalized()
            Kind.Polygon -> sideNormal((0 until sides).maxBy { p.dot(sideNormal(it)) })
            Kind.Ellipse -> Vec2(p.x / (radius * radius), p.y / (radius * aspect * radius * aspect)).normalized()
        }
        return if (n.lengthSquared() > 0.0) n else Vec2(1.0, 0.0)
    }

    /** Ramène p à l'intérieur de la forme réduite de `inset`. */
    fun projectInside(p: Vec2, inset: Double): Vec2 {
        val l = level(p, inset)
        if (l <= 1.0) return p
        if (kind == Kind.Polygon) {
            var q = p
            for (i in 0 until sides) {
                val n = sideNormal(i)
                val excess = q.dot(n) - (apothem - inset)
                if (excess > 0.0) q -= n * excess
            }
            return q
        }
        return p / l
    }

    /** Distance minimale entre le centre et le bord. */
    val innerRadius: Double
        get() = when (kind) {
            Kind.Circle -> radius
            Kind.Polygon -> apothem
            Kind.Ellipse -> radius * min(1.0, aspect)
        }

    fun outline(segments: Int = 96): List<Vec2> = if (kind == Kind.Polygon) {
        (0 until sides).map { Vec2.fromAngle((it + 0.5) * 2.0 * PI / sides) * radius }
    } else {
        val b = if (kind == Kind.Ellipse) radius * aspect else radius
        (0 until segments).map { val t = it * 2.0 * PI / segments; Vec2(radius * cos(t), b * sin(t)) }
    }
}

/** Type de stadium : forme, rebord et modificateurs (1 = neutre) des constantes de combat. */
data class Stadium(
    val id: String,
    val name: String,
    val description: String,
    /** Nom de code affiché sous le nom (« Noyau Holo »…). */
    val codename: String = "",
    /** Couleur signature (0xRRGGBB) de l'interface. */
    val accent: Long = 0x34d6ff,
    val shape: StadiumGeometry.Kind = StadiumGeometry.Kind.Circle,
    val sides: Int = 8,
    val aspect: Double = 1.0,
    val size: Double = 1.0,
    val hasRim: Boolean = true,
    val rimHeight: Double = 1.0,
    val slope: Double = 1.0,
    val friction: Double = 1.0,
    val speed: Double = 1.0,
    val enduranceDrain: Double = 1.0,
    val damage: Double = 1.0,
    val knockback: Double = 1.0,
    val dashSpeed: Double = 1.0,
    val dashPower: Double = 1.0,
    val floorCenter: Long = 0xd7dce6,
    val floorEdge: Long = 0x69708a,
    val rimColor: Long = 0x464a5c,
) {
    fun apply(base: BattleConfig): BattleConfig = base.copy(
        stadiumRadius = base.stadiumRadius * size,
        rimEscapeSpeed = base.rimEscapeSpeed * rimHeight,
        slopeStrength = base.slopeStrength * slope,
        friction = base.friction * friction,
        orbitBase = base.orbitBase * speed,
        orbitPerAttack = base.orbitPerAttack * speed,
        passiveBase = base.passiveBase * enduranceDrain,
        damageBase = base.damageBase * damage,
        rimDamage = base.rimDamage * damage,
        knockbackBase = base.knockbackBase * knockback,
        knockbackPerAttack = base.knockbackPerAttack * knockback,
        knockbackPerAttackerDefense = base.knockbackPerAttackerDefense * knockback,
        knockbackApproach = base.knockbackApproach * knockback,
        dashSpeed = base.dashSpeed * dashSpeed,
        dashAttackMultiplier = 1.0 + (base.dashAttackMultiplier - 1.0) * dashPower,
    )

    fun geometry(applied: BattleConfig) = StadiumGeometry(shape, applied.stadiumRadius, sides, aspect)

    companion object {
        /** Échelle d'un polygone régulier (rayon circonscrit 1) pour qu'il ait la surface du cercle de rayon 1 du stadium classique. */
        private fun sameAreaAsCircle(sides: Int) = sqrt(PI / (sides / 2.0 * sin(2.0 * PI / sides)))

        val all: List<Stadium> = listOf(
            Stadium("classique", "Classique",
                "Cuvette circulaire en plastique avec rebord. Le stadium de référence, sans avantage pour aucun type.",
                codename = "Noyau Holo", accent = 0x34d6ff),
            Stadium("beton", "Béton",
                "Octogone au sol dur et lisse, entouré d'un très haut rebord. Les toupies vont un peu plus vite et " +
                    "les chocs sont plus violents, mais la surface râpe la pointe.",
                codename = "Bunker-8", accent = 0x3dff8b,
                shape = StadiumGeometry.Kind.Polygon, sides = 8, size = sameAreaAsCircle(8), rimHeight = 2.0, friction = 0.85, speed = 1.05,
                damage = 1.1, knockback = 0.9, enduranceDrain = 1.1,
                floorCenter = 0xc4c6c9, floorEdge = 0x73777c, rimColor = 0x3d4045),
            Stadium("terre", "Terre",
                "Arène de terre battue sans rebord. Le sol accroche la pointe : la rotation s'use plus vite et les " +
                    "chocs frappent fort ; les dashs sont un peu plus courts.",
                codename = "Fosse Sauvage", accent = 0xb8ff2e,
                hasRim = false, dashSpeed = 0.95, damage = 1.4, enduranceDrain = 1.2,
                floorCenter = 0xc39663, floorEdge = 0x6e4b2a, rimColor = 0x2b1b0e),
            Stadium("glace", "Glace",
                "Patinoire elliptique au haut rebord et aux pentes douces. La glace use peu la rotation et les chocs " +
                    "repoussent moins, mais ils frappent plus fort.",
                codename = "Cryo-Ellipse", accent = 0x8ff3ff,
                shape = StadiumGeometry.Kind.Ellipse, aspect = 0.8,
                size = 1.0 / sqrt(0.8), // Surface du cercle classique.
                rimHeight = 1.87, friction = 0.99, slope = 0.75,
                enduranceDrain = 0.75, knockback = 0.83, damage = 1.19, speed = 1.05, dashPower = 1.1,
                floorCenter = 0xf1f8ff, floorEdge = 0x92badb, rimColor = 0x4d6f8c),
            Stadium("sable", "Sable",
                "Hexagone de sable aux rebords très hauts. Le sable ralentit tout et use vite la rotation, mais un " +
                    "dash bien placé frappe très fort.",
                codename = "Dune Hexa", accent = 0xffc23d,
                shape = StadiumGeometry.Kind.Polygon, sides = 6, size = sameAreaAsCircle(6), rimHeight = 1.5, friction = 1.3, speed = 0.9,
                enduranceDrain = 1.3, damage = 1.3, dashSpeed = 0.9, dashPower = 2.0,
                floorCenter = 0xf2dca4, floorEdge = 0xb89455, rimColor = 0x7a5c2c),
            Stadium("volcan", "Volcan",
                "Cratère aux pentes très raides, sans rebord, entouré de lave. Les chocs et les dashs y sont " +
                    "plus puissants.",
                codename = "Caldeira", accent = 0xff6a3d,
                knockback = 0.89, hasRim = false, slope = 1.82, damage = 1.14, dashPower = 1.2,
                enduranceDrain = 1.1, friction = 1.01, speed = 1.05,
                floorCenter = 0x6a4741, floorEdge = 0x2a1512, rimColor = 0xff5a1f),
        )
        val classic get() = all.first()
        fun find(id: String?) = all.firstOrNull { it.id == id }
    }
}
