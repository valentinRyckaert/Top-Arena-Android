package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.net.ClientSession
import fr.toupiearena.mobile.net.HostSession
import fr.toupiearena.mobile.net.LobbyBrowser
import fr.toupiearena.mobile.net.LobbyClient
import fr.toupiearena.mobile.net.LobbyServer
import fr.toupiearena.mobile.net.MatchCodec
import fr.toupiearena.mobile.session.MatchPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

class NetTest {
    // Un fil unique joue le rôle du fil principal de l'application.
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)

    @After fun tearDown() {
        scope.cancel()
        main.close()
    }

    private suspend fun <T> onMain(block: () -> T) = withContext(main) { block() }

    private suspend fun eventually(timeoutMs: Long = 5000, every: () -> Unit = {}, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (onMain { every(); condition() }) return
            delay(16)
        }
        throw AssertionError("Condition non remplie en $timeoutMs ms")
    }

    private fun settings(max: Int = 2) = LobbyServer.Settings("Salon test", "Hôte", max, "glace")

    @Test fun clientJoinsAndPlaysAMatch() = runBlocking {
        val server = LobbyServer(scope, main)
        assertTrue(onMain { runBlocking { server.start(settings(), 0, 0) } }.isSuccess)
        val client = LobbyClient(scope, main)
        var start: JSONObject? = null
        onMain {
            client.matchStarted += { start = it }
            client.connectToHost("127.0.0.1", server.port, "Alice")
        }
        eventually { client.isJoined && client.lobby.players.size == 2 }
        onMain {
            server.setHostTops(listOf(PartCatalog.presets[0]))
            client.selectTops(listOf(PartCatalog.presets[2]))
        }
        eventually { server.lobby.player(1)?.tops?.size == 1 }
        onMain { client.setReady(true) }
        eventually { server.canStart() }

        val host = onMain { HostSession(server, MatchCodec.parseStart(server.startMatch(7))!!) }
        eventually { start != null }
        val guest = onMain { ClientSession(client, MatchCodec.parseStart(start!!)!!) }
        assertEquals(1, guest.localPlayer)
        assertEquals("glace", guest.state.stadium.id)
        onMain {
            host.submitAim(0.0, 1.0)
            guest.submitAim(Math.PI, 1.2)
        }
        // L'hôte simule ; le client reçoit l'état et passe au combat.
        eventually(8000, every = { host.update(1.0 / 60.0); guest.update(1.0 / 60.0) }) {
            guest.state.phase == MatchPhase.Fighting || guest.state.phase == MatchPhase.Finished
        }
        onMain { server.close() }
    }

    @Test fun lobbyIsDiscoveredAndAnswersQueries() = runBlocking {
        val port = 45987
        val server = LobbyServer(scope, main)
        assertTrue(onMain { runBlocking { server.start(settings(3), 0, port) } }.isSuccess)

        // Question directe : l'hôte répond à l'expéditeur.
        val socket = DatagramSocket()
        socket.soTimeout = 3000
        val query = JSONObject().put("type", "query").put("app", "ToupieArena").put("v", 4).toString().toByteArray()
        socket.send(DatagramPacket(query, query.size, InetAddress.getLoopbackAddress(), port))
        val buffer = ByteArray(4096)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        val reply = JSONObject(String(packet.data, 0, packet.length))
        assertEquals("Salon test", reply.getString("name"))
        assertEquals(server.port, reply.getInt("port"))
        socket.close()

        // Annonces UDP : le navigateur liste le lobby.
        val browser = LobbyBrowser(scope, main, port)
        onMain { browser.start(scanNetwork = false) }
        eventually(4000) { browser.findByName("salon TEST") != null }
        val lobby = onMain { browser.findByName("Salon test") }
        assertNotNull(lobby)
        assertEquals(3, lobby!!.maxPlayers)
        onMain {
            browser.stop()
            server.close()
        }
    }
}
