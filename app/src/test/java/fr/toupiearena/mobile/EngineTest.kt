package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.BattleConfig
import fr.toupiearena.mobile.game.BotController
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.DuelMode
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.session.LocalSession
import org.json.JSONObject
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

    private val endurance get() = top("RM4", "PI4", "P3")
    private fun Top.withBolt(id: String) = withPart(PartCatalog.find(id)!!)

    /** Par images de 1/60 s, comme les tests PC (advance() plafonne chaque appel à 0,1 s). */
    private fun frames(battle: Battle, seconds: Double) {
        var t = 0.0
        while (t < seconds && !battle.isFinished) { battle.advance(1.0 / 60.0); t += 1.0 / 60.0 }
    }

    @Test fun hittingTheRimCostsEndurance() {
        fun rimBattle(speed: Double): Battle {
            val config = BattleConfig(launchSpeed = speed, slopeStrength = 0.0, centerPull = 0.0, friction = 0.0,
                passiveBase = 0.0, // Seul le rebord use la rotation.
                orbitBase = 0.0, orbitPerAttack = 0.0)
            config.startOffset = config.stadiumRadius - config.topRadius - 0.01
            val battle = Battle(listOf(defense, defense), config = config, seed = 1)
            battle.launch(listOf(PI, 0.0)) // La toupie 0 fonce sur le rebord.
            frames(battle, 1.0)
            return battle
        }
        val hit = rimBattle(BattleConfig().rimEscapeSpeed * 0.8).spinner(0)
        assertTrue(hit.isActive)
        assertTrue(hit.spin < hit.maxSpin - 0.3) // Un peu d'endurance perdue…
        assertTrue(hit.spin > hit.maxSpin - 3.0) // … mais seulement un peu.
        // Une toupie qui effleure le bord ne perd rien.
        val soft = rimBattle(BattleConfig().rimDamageThreshold * 0.5).spinner(0)
        assertEquals(soft.maxSpin, soft.spin, 0.0)
    }

    @Test fun stadiumsShareClassicArea() {
        // Aire du contour (formule du lacet) : tous les stadiums ont celle du classique.
        fun area(stadium: Stadium): Double {
            val points = stadium.geometry(stadium.apply(BattleConfig())).outline(2048)
            var sum = 0.0
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                sum += a.x * b.y - b.x * a.y
            }
            return abs(sum) / 2.0
        }
        val classic = area(Stadium.classic)
        for (stadium in Stadium.all) assertTrue(stadium.id, abs(area(stadium) / classic - 1.0) < 0.005)
    }

    @Test fun rulesCarryDuelMode() {
        // Type de duel : réglage global par défaut, transmis tel quel avec les règles de l'hôte.
        MatchRules.defaultDuelMode = DuelMode.Rush
        val rush = MatchRules()
        MatchRules.defaultDuelMode = DuelMode.Timing
        assertEquals(DuelMode.Rush, rush.duelMode)
        assertEquals(DuelMode.Rush, rush.apply().duelMode)
        assertEquals(DuelMode.Rush, MatchRules.fromJson(rush.toJson()).duelMode)
        assertEquals(1, rush.toJson().getInt("duels")) // Même clé et même codage que sur PC.
        assertEquals(DuelMode.Timing, MatchRules.fromJson(JSONObject()).duelMode) // Ancien hôte.
        assertTrue(MatchRules.fromJson(rush.toJson()).sameAs(rush))
        assertTrue(!MatchRules(damage = 2.0).sameAs(MatchRules(damage = 2.0, duelMode = DuelMode.None)))
        assertEquals(BattleConfig().rimDamage * 2.0, MatchRules(damage = 2.0).apply().rimDamage, 1e-12)
    }

    @Test fun vortexPullsTargetToCenter() {
        // L'anneau couvre tout le stadium : la cible, à l'opposé et près du bord, est
        // aspirée jusqu'au centre sans le dépasser, et ne peut pas dasher pendant ce temps.
        val config = staticConfig().copy(startOffset = 0.8)
        val battle = Battle(listOf(defense.withBolt("B4"), attack), Stadium.classic, config, 1)
        battle.launch(listOf(0.0, 0.0))
        battle.advance(0.01)
        val start = battle.spinner(1).position
        assertTrue(battle.requestSpecial(0))
        run(battle, 2.0) { battle.spinner(1).isPulled }
        assertTrue(battle.spinner(1).isPulled)
        assertFalse(battle.canDash(1))
        frames(battle, 3.0)
        val target = battle.spinner(1)
        assertTrue(target.isActive)
        assertFalse(target.isPulled)
        assertTrue(target.position.length() < 0.15 * start.length() + config.topRadius)
        assertTrue(target.position.dot(start) > 0.0) // Pas passée de l'autre côté du centre.
    }

    @Test fun specialHitStopsADashingTarget() {
        // Cible qui fonce sur le lanceur : l'onde de choc la repousse quand même (le dash en
        // cours effaçait le recul à chaque pas).
        val battle = Battle(listOf(attack.withBolt("B1"), endurance), Stadium.classic,
            staticConfig().copy(startOffset = 0.2), 1)
        battle.launch(listOf(0.0, 0.0))
        battle.advance(0.01)
        assertTrue(battle.requestSpecial(0))
        assertTrue(battle.requestDash(1, 0))
        var hit = false
        run(battle, 1.0) {
            hit = hit || battle.takeImpacts().any { it.special }
            hit
        }
        assertTrue(hit)
        val target = battle.spinner(1)
        val away = (target.position - battle.spinner(0).position).normalized()
        assertFalse(target.isDashing)
        assertTrue(target.velocity.dot(away) > 0.5) // Repoussée, pas seulement freinée.
    }

    private fun duelBattle(seed: Long = 1, mode: DuelMode = DuelMode.Timing): Battle {
        val battle = Battle(listOf(attack, defense), config = staticConfig().copy(startOffset = 0.3, duelMode = mode),
            seed = seed)
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

    @Test fun soloAgainstSeveralBots() {
        val pool = PartCatalog.presets
        val melee = LocalSession(pool[0], pool.subList(1, 4), Stadium.classic, Difficulty.Hard)
        assertEquals(4, melee.state.playerCount)
        assertEquals(listOf("Vous", "Bot 1", "Bot 2", "Bot 3"), melee.state.names)
        assertEquals(listOf(false, true, true, true), melee.state.aimed.toList()) // Les bots ont visé.
        val duel = LocalSession(pool[0], pool[1], Stadium.classic, Difficulty.Easy)
        assertEquals(listOf("Vous", "Bot Facile"), duel.state.names)
        assertTrue(runCatching { LocalSession(pool[0], pool.subList(1, 5), Stadium.classic, Difficulty.Easy) }.isFailure)
    }

    @Test fun noDuelModeJustCollides() {
        val battle = duelBattle(1, DuelMode.None)
        assertFalse(battle.duel.active)
        assertTrue(battle.spinner(0).spin < battle.spinner(0).maxSpin) // Choc ordinaire : dégâts.
    }

    @Test fun rushDuelGoesToMostPresses() {
        val battle = duelBattle(1, DuelMode.Rush)
        assertTrue(battle.duel.active)
        val spins = battle.spinners.map { it.spin }
        repeat(5) { assertTrue(battle.duelPress(0, 0.0)) }
        assertTrue(battle.duelPress(1, 0.0))
        assertEquals(4, battle.duel.rope())
        frames(battle, battle.config.rushDuration * 0.5)
        assertTrue(battle.duel.active) // Ni avance décisive, ni fin du temps.
        frames(battle, battle.config.rushDuration * 0.6)
        assertFalse(battle.duel.active)
        // Le second, distancé, perd (l'usure naturelle reprend ensuite pour les deux).
        assertTrue(spins[1] - battle.spinner(1).spin > 3.0)
        assertTrue(spins[0] - battle.spinner(0).spin < 1.0)

        // Une avance décisive termine le duel aussitôt.
        val quick = duelBattle(2, DuelMode.Rush)
        repeat(quick.config.rushLimit) { quick.duelPress(1, 0.0) }
        quick.advance(1.0 / 60.0)
        assertFalse(quick.duel.active)
        assertTrue(quick.elapsed < quick.config.rushDuration)
    }

    @Test fun rushDuelDrawWhenLevel() {
        val battle = duelBattle(1, DuelMode.Rush)
        val spins = battle.spinners.map { it.spin }
        repeat(7) { battle.duelPress(0, 0.0); battle.duelPress(1, 0.0) }
        frames(battle, battle.config.rushDuration + 0.1)
        assertFalse(battle.duel.active)
        assertTrue(spins[0] - battle.spinner(0).spin < 1.0) // Pile au milieu : nul, sans dégâts.
        assertTrue(spins[1] - battle.spinner(1).spin < 1.0)
    }

    @Test fun harderBotsWinRushDuels() {
        var hardWins = 0
        val duels = 100
        for (k in 0 until duels) {
            val battle = duelBattle(k.toLong(), DuelMode.Rush)
            val hardSeat = k % 2
            val bots = (0..1).map { BotController(it, if (it == hardSeat) Difficulty.Hard else Difficulty.Easy, 5L * k + it) }
            val spins = battle.spinners.map { it.spin }
            while (battle.duel.active) {
                bots.forEach { it.update(battle, 1.0 / 60.0) }
                battle.advance(1.0 / 60.0)
            }
            if (battle.spinner(1 - hardSeat).spin < spins[1 - hardSeat]) hardWins++
        }
        println("Rafales gagnées par le bot difficile contre le facile : $hardWins/$duels")
        assertTrue(hardWins > duels * 0.8)
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
