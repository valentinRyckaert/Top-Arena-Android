package fr.toupiearena.mobile.net

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Lobbys visibles sur le réseau local (compatible avec les hôtes PC) :
 * annonces UDP, questions UDP auxquelles les hôtes répondent directement, et
 * en secours un balayage TCP du sous-réseau Wi-Fi (le /24 toutes les 4 s, le
 * reste jusqu'à un /20 toutes les 30 s).
 */
class LobbyBrowser(
    private val scope: CoroutineScope,
    private val main: CoroutineDispatcher,
    private val discoveryPort: Int = Protocol.DISCOVERY_PORT,
) {
    private val found = mutableListOf<Pair<LobbyAnnouncement, Boolean>>() // (lobby, vu en UDP)
    private val jobs = mutableListOf<Job>()
    private var listen: DatagramSocket? = null
    private var query: DatagramSocket? = null
    private var scanJob: Job? = null
    var isRunning = false
        private set
    var isScanning = false
        private set
    /** Au moins une annonce UDP reçue d'une autre machine. */
    var udpWorks = false
        private set
    val lobbies: List<LobbyAnnouncement> get() = found.map { it.first }
    var onChanged: () -> Unit = {}

    fun start(scanNetwork: Boolean = true) {
        if (isRunning) return
        isRunning = true
        udpWorks = false
        listen = runCatching {
            DatagramSocket(null).apply { reuseAddress = true; broadcast = true; bind(InetSocketAddress(discoveryPort)) }
        }.getOrNull()
        query = runCatching { DatagramSocket().apply { broadcast = true } }.getOrNull()
        listOfNotNull(listen, query).forEach { socket -> jobs += scope.launch(Dispatchers.IO) { receive(socket) } }
        jobs += scope.launch(Dispatchers.IO) {
            val targets = LobbyServer.broadcastTargets()
            val data = JSONObject().put("type", Msg.Query).put("app", Protocol.APP_TAG).put("v", Protocol.VERSION)
                .toString().toByteArray(Charsets.UTF_8)
            while (isActive) {
                for (t in targets) runCatching { query?.send(DatagramPacket(data, data.size, t, discoveryPort)) }
                delay(QUERY_INTERVAL_MS)
            }
        }
        jobs += scope.launch(main) {
            while (isActive) {
                expire()
                delay(1000)
            }
        }
        if (scanNetwork) {
            jobs += scope.launch(main) {
                var tick = 0
                while (isActive) {
                    if (!isScanning) scan(nearOnly = tick % (SCAN_INTERVAL_MS / NEAR_SCAN_INTERVAL_MS) != 0L)
                    tick++
                    delay(NEAR_SCAN_INTERVAL_MS)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        scanJob?.cancel()
        isScanning = false
        runCatching { listen?.close() }
        runCatching { query?.close() }
        listen = null
        query = null
        if (found.isNotEmpty()) {
            found.clear()
            onChanged()
        }
    }

    /** Relance immédiatement un balayage complet. */
    fun rescan() {
        if (isRunning) scan(nearOnly = false)
    }

    fun findByName(name: String) = lobbies.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    private suspend fun receive(socket: DatagramSocket) {
        val buffer = ByteArray(4096)
        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            if (runCatching { socket.receive(packet) }.isFailure) return
            val o = runCatching { JSONObject(String(packet.data, 0, packet.length, Charsets.UTF_8)) }.getOrNull() ?: continue
            val lobby = LobbyAnnouncement.parse(o, packet.address) ?: continue
            withContext(main) {
                if (!packet.address.isLoopbackAddress) udpWorks = true
                merge(lobby, fromUdp = true)
            }
        }
    }

    private fun scan(nearOnly: Boolean) {
        scanJob?.cancel()
        isScanning = true
        onChanged()
        scanJob = scope.launch(Dispatchers.IO) {
            val (near, far) = localTargets()
            val probes = near.flatMap { h -> PORTS.map { h to it } } + if (nearOnly) emptyList() else far.map { it to PORTS.first() }
            val limit = Semaphore(CONCURRENCY)
            coroutineScope {
                for ((host, port) in probes) launch { limit.withPermit { probe(host, port)?.let { withContext(main) { merge(it, false) } } } }
            }
            withContext(main) {
                isScanning = false
                onChanged()
            }
        }
    }

    private fun probe(host: InetAddress, port: Int): LobbyAnnouncement? = runCatching {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
            s.soTimeout = PROBE_TIMEOUT_MS * 2
            s.getOutputStream().write("{\"type\":\"probe\"}\n".toByteArray())
            val line = s.getInputStream().bufferedReader().readLine() ?: return null
            val info = JSONObject(line)
            if (info.optString("type") != Msg.Info) null else LobbyAnnouncement.parse(info, host)
        }
    }.getOrNull()

    private fun merge(lobby: LobbyAnnouncement, fromUdp: Boolean) {
        if (!isRunning) return
        val index = found.indexOfFirst { it.first.lobbyId == lobby.lobbyId }
        if (index < 0) {
            found += lobby to fromUdp
            onChanged()
            return
        }
        val old = found[index].first
        // Une même annonce peut arriver par plusieurs chemins : on préfère l'adresse du réseau local.
        val merged = if (!old.address.isLoopbackAddress && lobby.address.isLoopbackAddress) lobby.copy(address = old.address) else lobby
        val changed = old.copy(lastSeenMs = merged.lastSeenMs) != merged
        found[index] = merged to fromUdp
        if (changed) onChanged()
    }

    private fun expire() {
        val now = System.currentTimeMillis()
        val removed = found.removeAll { (l, udp) -> now - l.lastSeenMs > if (udp) UDP_LIFETIME_MS else SCAN_LIFETIME_MS }
        if (removed) onChanged()
    }

    companion object {
        private const val UDP_LIFETIME_MS = 6000L
        private const val SCAN_LIFETIME_MS = 45000L
        private const val NEAR_SCAN_INTERVAL_MS = 4000L
        private const val SCAN_INTERVAL_MS = 32000L
        private const val QUERY_INTERVAL_MS = 2000L
        private const val CONCURRENCY = 64
        private const val PROBE_TIMEOUT_MS = 350
        private const val MIN_PREFIX = 20 // Au plus 4094 adresses par interface.
        private val PORTS = listOf(Protocol.DEFAULT_TCP_PORT, Protocol.DEFAULT_TCP_PORT + 1, Protocol.DEFAULT_TCP_PORT + 2)
        /** Interfaces sans intérêt : données mobiles, VPN, Wi-Fi Direct, conteneurs. */
        private val IGNORED = listOf("rmnet", "ccmni", "pdp", "tun", "dummy", "p2p", "docker", "br-", "virbr", "veth")

        /** Adresses à sonder : le /24 de chaque interface (`near`), puis le reste du sous-réseau (`far`). */
        fun localTargets(): Pair<List<InetAddress>, List<InetAddress>> {
            val near = mutableListOf<InetAddress>()
            val far = mutableListOf<InetAddress>()
            val seen = mutableSetOf<Int>()
            val interfaces = runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback || IGNORED.any { iface.name.lowercase().startsWith(it) }) continue
                for (entry in iface.interfaceAddresses) {
                    val address = entry.address as? Inet4Address ?: continue
                    val prefix = maxOf(MIN_PREFIX, entry.networkPrefixLength.toInt())
                    if (prefix >= 31) continue
                    val ip = address.address.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xff) }
                    val mask = -1 shl (32 - prefix)
                    val network = ip and mask
                    val broadcast = network or mask.inv()
                    val block24 = ip and -0x100
                    for (a in network + 1 until broadcast) {
                        if (a == ip || !seen.add(a)) continue
                        val host = InetAddress.getByAddress(byteArrayOf((a ushr 24).toByte(), (a ushr 16).toByte(),
                            (a ushr 8).toByte(), a.toByte()))
                        (if (a and -0x100 == block24) near else far) += host
                    }
                }
            }
            return near to far
        }
    }
}
