package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.game.Battle
import fr.toupiearena.mobile.game.MatchFormat
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.MatchSeries
import fr.toupiearena.mobile.game.Participant
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.game.Tournament
import fr.toupiearena.mobile.game.TournamentFormat
import fr.toupiearena.mobile.game.TournamentSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentTest {
    private fun people(n: Int, tops: Int = 1) = (0 until n).map { i ->
        Participant("P$i", if (i == 0) 0 else -1, tops = (0 until tops).map { PartCatalog.presets[(i + it) % 8] })
    }
    private fun settings(format: TournamentFormat, n: Int, match: MatchFormat = MatchFormat.Express) =
        TournamentSettings(format, match, n).fitBots(1)

    private fun playRound(t: Tournament) {
        t.rounds[t.currentRound].forEachIndexed { i, m ->
            if (!m.played) t.record(i, MatchSeries(score = if (m.a < m.b) intArrayOf(2, 1) else intArrayOf(1, 2)))
        }
    }

    @Test fun seriesScoring() {
        val express = MatchSeries()
        express.recordRound(0, Battle.EndReason.RingOut)
        express.recordRound(-1, Battle.EndReason.SpinOut)
        express.recordRound(0, Battle.EndReason.SpinOut)
        assertEquals(0, express.winner)
        val classic = MatchSeries(MatchFormat.Classic, order = arrayOf(listOf(2, 0, 1), emptyList()))
        assertEquals(2, classic.topFor(0))
        classic.recordRound(1, Battle.EndReason.RingOut)
        assertEquals(2, classic.score[1])
        assertEquals(0, classic.topFor(0))
        classic.recordRound(1, Battle.EndReason.SpinOut)
        classic.recordRound(1, Battle.EndReason.SpinOut)
        assertEquals(1, classic.winner)
    }

    @Test fun leagueEveryoneMeetsOnce() {
        for (n in listOf(4, 5, 32)) {
            val t = Tournament.create(settings(TournamentFormat.League, n), people(n), 7)
            assertEquals(t.roundCount, t.rounds.size)
            val pairs = t.rounds.flatten().filter { !it.isBye }.map { minOf(it.a, it.b) to maxOf(it.a, it.b) }.toSet()
            assertEquals(n * (n - 1) / 2, pairs.size)
            while (!t.isFinished) { playRound(t); t.nextRound() }
            assertEquals(0, t.champion)
        }
    }

    @Test fun knockoutBracketAndJson() {
        val t = Tournament.create(settings(TournamentFormat.Knockout, 8, MatchFormat.Classic), people(8, 3), 3)
        assertEquals(3, t.roundCount)
        assertEquals("Finale", t.roundName(2))
        playRound(t)
        t.nextRound()
        val copy = Tournament.fromJson(t.toJson())
        assertNotNull(copy)
        assertEquals(t.toJson().toString(), copy!!.toJson().toString())
        while (!t.isFinished) { playRound(t); t.nextRound() }
        assertEquals(0, t.champion)
    }

    @Test fun botMatchHasWinner() {
        val p = people(2, 3)
        val s = Tournament.playBotMatch(p[0], p[1], MatchFormat.Classic, Stadium.classic, MatchRules(), 11)
        assertTrue(s.isOver)
        assertTrue(s.score[s.winner] >= 4)
    }
}
