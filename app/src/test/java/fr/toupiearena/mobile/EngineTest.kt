package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.BotController
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.Stadium
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class EngineTest {
    private fun top(w: String, m: String, t: String) =
        Top("test", PartCatalog.find(w)!!, PartCatalog.find(m)!!, PartCatalog.find(t)!!)
    private val attack get() = top("RM2", "PI2", "P2")
    private val defense get() = top("RM3", "PI3", "P1")

    /** Combat figé : aucun mouvement propre. */
    private fun staticConfig() = BattleConfig(launchSpeed = 0.0, slopeStrength = 0.0, centerPull = 0.0,
        orbitBase = 0.0, orbitPerAttack = 0.0)

    private fun run(battle: Battle, seconds: Double, until: () -> Boolean = { false }) {
        var t = 0.0
        while (t < seconds && !battle.isFinished && !until()) { battle.advance(1.0 / 240.0); t += 1.0 / 240.0 }
    }

    @Test fun catalogMatchesPcVersion() {
        // Même empreinte que toupie_arena (PC) : indispensable pour jouer ensemble en réseau.
        assertEquals("6df6fa087f7d", PartCatalog.fingerprint)
        assertEquals(8, PartCatalog.presets.size)
        assertEquals(16, PartCatalog.maxStatValue) // Défense : Titan 7 + Carapace 4 + Dôme 5.
    }

    @Test fun topJsonRoundTrip() {
        val original = PartCatalog.presets[2]
        assertEquals(original, Top.fromJson(original.toJson()))
        assertEquals(null, Top.fromJson(original.toJson().put("wheel", "XX")))
    }

    @Test fun dashCostsEnduranceAndBoostsAttack() {
        val battle = Battle(listOf(attack, defense), config = staticConfig(), seed = 1)
        battle.launch(listOf(0.0, PI))
        val before = battle.spinner(0).spin
        assertTrue(battle.requestDash(0))
        assertTrue(battle.spinner(0).spin < before)
        assertTrue(battle.effectiveAttack(battle.spinner(0)) > battle.spinner(0).stats.attack * 0.99)
        assertFalse(battle.canDash(0))
    }

    @Test fun specialIsSingleUseAndCostsQuarter() {
        val battle = Battle(listOf(attack, defense), config = staticConfig(), seed = 1)
        battle.launch(listOf(0.0, PI))
        battle.advance(0.02)
        val before = battle.spinner(0).spin
        assertTrue(battle.requestSpecial(0))
        assertEquals(before * 0.75, battle.spinner(0).spin, 1e-9)
        assertFalse(battle.canSpecial(0))
    }

    private fun duelBattle(seed: Long = 1): Battle {
        val battle = Battle(listOf(attack, defense), config = staticConfig().copy(startOffset = 0.3), seed = seed)
        battle.launch(listOf(0.0, PI))
        battle.requestDash(0)
        battle.requestDash(1)
        run(battle, 0.5) { battle.duel.active }
        return battle
    }

    @Test fun simultaneousDashesStartDuelAndMissCostsMore() {
        val battle = duelBattle()
        assertTrue(battle.duel.active)
        val winner = battle.spinner(1).copy(boostTimer = 1.0)
        val expected = 1.5 * battle.collisionDamage(winner, battle.spinner(0), battle.duel.approach)
        val before = battle.spinner(0).spin
        assertTrue(battle.duelPress(0, 1.0)) // Dans le rouge.
        assertFalse(battle.duel.active)
        assertEquals(expected, before - battle.spinner(0).spin, 1e-9)
    }

    @Test fun duelDrawAfterFinalStreak() {
        val battle = duelBattle()
        val spins = battle.spinners.map { it.spin }
        var presses = 0
        while (battle.duel.active && presses < 40) {
            battle.duelPress(0, 0.0); battle.duelPress(1, 0.0); presses++
        }
        assertFalse(battle.duel.active)
        assertTrue(presses > 3)
        assertEquals(spins, battle.spinners.map { it.spin })
    }

    @Test fun finalEliminationKeepsWinnerEndurance() {
        val battle = Battle(listOf(attack, top("RM4", "PI4", "P3")), Stadium.find("terre")!!,
            staticConfig().copy(startOffset = 0.3), 1)
        battle.launch(listOf(0.0, PI))
        battle.advance(0.01)
        assertTrue(battle.requestDash(0))
        var spin = battle.spinner(0).spin
        repeat(600) { if (!battle.isFinished) { spin = battle.spinner(0).spin; battle.advance(1.0 / 60.0) } }
        assertTrue(battle.isFinished)
        assertEquals(0, battle.winner)
        assertTrue(battle.spinner(0).spin <= spin)
    }

    private fun winRate(strong: Difficulty, weak: Difficulty, fights: Int): Double {
        val pool = PartCatalog.presets
        val rng = java.util.Random(2024)
        var score = 0.0
        repeat(fights) { k ->
            val strongSeat = k % 2
            val battle = Battle(listOf(pool[rng.nextInt(pool.size)], pool[rng.nextInt(pool.size)]), seed = k.toLong())
            val bots = (0..1).map { BotController(it, if (it == strongSeat) strong else weak, 77L * k + it) }
            battle.launch(bots.map { it.chooseLaunchAngle(battle) }, bots.map { it.chooseLaunchPower(battle) })
            while (!battle.isFinished) { bots.forEach { it.update(battle, 1.0 / 30.0) }; battle.advance(1.0 / 30.0) }
            score += if (battle.winner == strongSeat) 1.0 else if (battle.winner < 0) 0.5 else 0.0
        }
        return score / fights
    }

    @Test fun harderBotsWinMoreOften() {
        val normal = winRate(Difficulty.Normal, Difficulty.Easy, 200)
        val hard = winRate(Difficulty.Hard, Difficulty.Normal, 200)
        println("Normal contre facile : %.1f %%, difficile contre normal : %.1f %%".format(normal * 100, hard * 100))
        assertTrue(normal > 0.6)
        assertTrue(hard > 0.58)
    }

    @Test fun stadiumGeometryLevels() {
        for (stadium in Stadium.all) {
            val g = stadium.geometry(stadium.apply(BattleConfig()))
            assertEquals(0.0, g.level(fr.toupiearena.mobile.game.Vec2()), 1e-9)
            val edge = g.outline().first()
            assertTrue(abs(g.level(edge) - 1.0) < 0.05)
        }
    }
}
