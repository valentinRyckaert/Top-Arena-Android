package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.net.ClientSession
import fr.toupiearena.mobile.net.HostSession
import fr.toupiearena.mobile.net.LobbyClient
import fr.toupiearena.mobile.net.LobbyServer
import fr.toupiearena.mobile.net.MatchCodec
import fr.toupiearena.mobile.session.MatchPhase
import fr.toupiearena.mobile.session.MatchSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Cross-play avec la version PC : actif seulement si TOUPIE_PC_INTEROP désigne
 * le programme d'essai côté PC (hôte ou client d'un lobby via toupie_net).
 */
class CrossPlayTest {
    private val pcProgram = System.getenv("TOUPIE_PC_INTEROP").orEmpty()
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)

    @After fun tearDown() {
        scope.cancel()
        main.close()
    }

    private suspend fun <T> onMain(block: () -> T) = withContext(main) { block() }

    private fun startPc(vararg args: String): Process {
        assumeTrue("TOUPIE_PC_INTEROP non défini", File(pcProgram).canExecute())
        return ProcessBuilder(pcProgram, *args).redirectErrorStream(true).start()
    }

    private suspend fun drive(session: () -> MatchSession?, pc: Process) {
        val end = System.currentTimeMillis() + 20000
        while (System.currentTimeMillis() < end && pc.isAlive) {
            onMain { session()?.update(1.0 / 60.0) }
            delay(16)
        }
        pc.waitFor(5, TimeUnit.SECONDS)
    }

    @Test fun mobileJoinsPcLobby() = runBlocking {
        val pc = startPc("host", "45480")
        delay(800)
        val client = LobbyClient(scope, main)
        var session: MatchSession? = null
        onMain {
            client.onJoined = {
                client.selectTops(listOf(PartCatalog.presets[0]))
                client.setReady(true)
            }
            client.matchStarted += { start: JSONObject ->
                session = ClientSession(client, MatchCodec.parseStart(start)!!).also { it.submitAim(0.5, 1.0) }
            }
            client.connectToHost("127.0.0.1", 45480, "Téléphone")
        }
        drive({ session }, pc)
        val output = pc.inputStream.bufferedReader().readText()
        println(output)
        assertTrue(output.contains("COMBAT EN COURS"))
        assertTrue(session!!.state.phase == MatchPhase.Fighting || session!!.state.phase == MatchPhase.Finished)
        onMain { client.disconnect() }
    }

    @Test fun pcJoinsMobileLobby() = runBlocking {
        val server = LobbyServer(scope, main)
        assertTrue(onMain { runBlocking { server.start(LobbyServer.Settings("Lobby mobile", "Téléphone", 2, "terre"), 45481, 0) } }.isSuccess)
        var session: MatchSession? = null
        onMain {
            server.setHostTops(listOf(PartCatalog.presets[2]))
            server.lobbyChanged += {
                if (session == null && server.canStart()) {
                    session = HostSession(server, MatchCodec.parseStart(server.startMatch(9))!!).also { it.submitAim(0.0, 1.0) }
                }
            }
        }
        val pc = startPc("client", server.port.toString())
        drive({ session }, pc)
        val output = pc.inputStream.bufferedReader().readText()
        println(output)
        assertTrue(output.contains("lobby rejoint")) // Le client PC signale l'arrivée avant de recevoir le nom du lobby.
        assertTrue(output.contains("COMBAT EN COURS"))
        onMain { server.close() }
    }
}
