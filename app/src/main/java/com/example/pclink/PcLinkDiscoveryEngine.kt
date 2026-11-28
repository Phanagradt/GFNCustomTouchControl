package com.example.pclink

import android.os.SystemClock
import com.example.model.DiscoveredPcHost
import com.example.model.PcHostPingStatus
import com.example.model.PcStreamClient
import com.example.model.SavedPcHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Real LAN networking utilities for PC / Laptop Remote Play & Steam Link:
 * - Reads device's real Wi-Fi/LAN IPv4 address
 * - Scans the local /24 subnet for real PCs running Steam Remote Play (TCP 27036/27031)
 *   or Sunshine/Moonlight (TCP 47989/47984)
 * - Measures real TCP RTT latency to saved PCs/laptops
 * - Sends genuine IEEE 802.3 Wake-on-LAN (WoL) magic packets over UDP port 9
 */
object PcLinkDiscoveryEngine {

    private const val STEAM_STREAMING_PORT = 27036
    private const val STEAM_ALT_PORT = 27031
    private const val SUNSHINE_HTTP_PORT = 47989
    private const val SUNSHINE_HTTPS_PORT = 47984
    private const val WOL_PORT = 9

    /**
     * Returns the device's active private LAN IPv4 address (e.g., 192.168.1.42) or null if offline.
     */
    fun getLocalIpv4Address(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            var fallback: String? = null
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val addresses = iface.inetAddresses
                for (addr in addresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (iface.name.startsWith("wlan") || iface.name.startsWith("eth")) {
                            return ip
                        }
                        if (fallback == null) fallback = ip
                    }
                }
            }
            fallback
        }.getOrNull()
    }

    /**
     * Measures real TCP reachability & RTT (ms) to a saved PC/laptop.
     */
    suspend fun pingPcHost(host: SavedPcHost): PcHostPingStatus = withContext(Dispatchers.IO) {
        val address = host.hostAddress.trim()
        val now = System.currentTimeMillis()
        if (address.isEmpty()) {
            return@withContext PcHostPingStatus(
                reachable = false,
                rttMs = null,
                checkedAtMs = now,
                detail = "No IP or hostname configured"
            )
        }

        val portsToTry = when (host.client) {
            PcStreamClient.STEAM_LINK -> listOf(STEAM_STREAMING_PORT, STEAM_ALT_PORT, 445, 80)
            PcStreamClient.MOONLIGHT -> listOf(SUNSHINE_HTTP_PORT, SUNSHINE_HTTPS_PORT, 47990)
            PcStreamClient.PARSEC -> listOf(8000, 443, 80)
            PcStreamClient.WEB_STREAM -> listOf(47990, 8080, 443, 80)
        }

        for (port in portsToTry) {
            val rtt = probeTcpPort(address, port, timeoutMs = 450)
            if (rtt != null) {
                return@withContext PcHostPingStatus(
                    reachable = true,
                    rttMs = rtt,
                    checkedAtMs = System.currentTimeMillis(),
                    detail = "Port $port open • ${rtt}ms"
                )
            }
        }

        // Fallback ICMP/echo reachability check (host awake, Steam client may not be running yet)
        val icmpStart = SystemClock.elapsedRealtime()
        val icmpOk = runCatching {
            InetAddress.getByName(address).isReachable(600)
        }.getOrDefault(false)
        if (icmpOk) {
            val rtt = (SystemClock.elapsedRealtime() - icmpStart).toInt().coerceAtLeast(1)
            return@withContext PcHostPingStatus(
                reachable = true,
                rttMs = rtt,
                checkedAtMs = System.currentTimeMillis(),
                detail = "PC awake • Open Steam on PC"
            )
        }

        PcHostPingStatus(
            reachable = false,
            rttMs = null,
            checkedAtMs = System.currentTimeMillis(),
            detail = "Offline or sleeping"
        )
    }

    /**
     * Scans the local /24 Wi-Fi subnet for real PCs/laptops running Steam Remote Play or Sunshine/Moonlight.
     */
    suspend fun scanLanForGamingPcs(
        onProgress: (Float) -> Unit = {}
    ): List<DiscoveredPcHost> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpv4Address() ?: return@withContext emptyList()
        val parts = localIp.split(".")
        if (parts.size != 4) return@withContext emptyList()
        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
        val myOctet = parts[3].toIntOrNull() ?: -1

        val semaphore = Semaphore(36)
        val found = mutableListOf<DiscoveredPcHost>()
        var completed = 0

        coroutineScope {
            val jobs = (1..254).filter { it != myOctet }.map { lastOctet ->
                async {
                    val ip = "$prefix.$lastOctet"
                    semaphore.withPermit {
                        // Check Steam Remote Play (27036) first, then Sunshine/Moonlight (47989)
                        val steamRtt = probeTcpPort(ip, STEAM_STREAMING_PORT, timeoutMs = 170)
                        val sunshineRtt = if (steamRtt == null) {
                            probeTcpPort(ip, SUNSHINE_HTTP_PORT, timeoutMs = 170)
                        } else {
                            null
                        }
                        val match: DiscoveredPcHost? = when {
                            steamRtt != null -> {
                                val resolvedName = resolveHostname(ip)
                                DiscoveredPcHost(
                                    ipAddress = ip,
                                    hostname = resolvedName,
                                    detectedService = "Steam Remote Play",
                                    rttMs = steamRtt,
                                    suggestedClient = PcStreamClient.STEAM_LINK
                                )
                            }
                            sunshineRtt != null -> {
                                val resolvedName = resolveHostname(ip)
                                DiscoveredPcHost(
                                    ipAddress = ip,
                                    hostname = resolvedName,
                                    detectedService = "Sunshine / Moonlight",
                                    rttMs = sunshineRtt,
                                    suggestedClient = PcStreamClient.MOONLIGHT
                                )
                            }
                            else -> null
                        }
                        synchronized(found) {
                            completed++
                            if (match != null) {
                                found.add(match)
                            }
                            if (completed % 12 == 0 || completed >= 253) {
                                onProgress((completed / 253f).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            jobs.awaitAll()
        }

        onProgress(1f)
        found.sortedBy { it.rttMs }
    }

    /**
     * Sends a genuine IEEE 802.3 Wake-on-LAN (WoL) UDP Magic Packet (6x 0xFF + 16x MAC) on UDP port 9.
     */
    suspend fun sendWakeOnLan(
        macAddress: String,
        hostIpHint: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val macBytes = parseMacAddress(macAddress)
                ?: throw IllegalArgumentException("Enter a valid MAC address like AA:BB:CC:DD:EE:FF")

            val magicPacket = ByteArray(6 + 16 * 6)
            for (i in 0 until 6) {
                magicPacket[i] = 0xFF.toByte()
            }
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, magicPacket, 6 + i * 6, 6)
            }

            val targets = buildSet {
                add("255.255.255.255")
                val baseIp = hostIpHint?.takeIf { it.count { c -> c == '.' } == 3 } ?: getLocalIpv4Address()
                if (!baseIp.isNullOrBlank()) {
                    val segs = baseIp.split(".")
                    if (segs.size == 4) {
                        add("${segs[0]}.${segs[1]}.${segs[2]}.255")
                    }
                }
            }

            DatagramSocket().use { socket ->
                socket.broadcast = true
                targets.forEach { broadcastIp ->
                    runCatching {
                        val addr = InetAddress.getByName(broadcastIp)
                        val packet = DatagramPacket(magicPacket, magicPacket.size, addr, WOL_PORT)
                        socket.send(packet)
                    }
                }
            }
            "Wake-on-LAN magic packet sent to ${macAddress.uppercase()}"
        }
    }

    private fun probeTcpPort(host: String, port: Int, timeoutMs: Int): Int? {
        return runCatching {
            val start = SystemClock.elapsedRealtime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (SystemClock.elapsedRealtime() - start).toInt().coerceAtLeast(1)
        }.getOrNull()
    }

    private fun resolveHostname(ip: String): String {
        val raw = runCatching { InetAddress.getByName(ip).hostName }.getOrNull()
        return if (raw.isNullOrBlank() || raw == ip) {
            "Gaming PC ($ip)"
        } else {
            raw.substringBefore(".")
        }
    }

    fun parseMacAddress(input: String): ByteArray? {
        val cleaned = input.trim().replace("-", ":").replace(".", ":")
        val hexParts = if (cleaned.contains(":")) {
            cleaned.split(":")
        } else if (cleaned.length == 12) {
            cleaned.chunked(2)
        } else {
            return null
        }
        if (hexParts.size != 6) return null
        val out = ByteArray(6)
        hexParts.forEachIndexed { idx, part ->
            val value = part.toIntOrNull(16) ?: return null
            if (value !in 0..255) return null
            out[idx] = value.toByte()
        }
        return out
    }
}
