package com.example.webplayer

import com.example.model.GfnRegionPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

enum class PingQuality {
    EXCELLENT, // < 60ms
    GOOD,      // 60..119ms
    FAIR,      // 120..189ms
    POOR,      // >= 190ms
    UNKNOWN
}

data class RegionPingResult(
    val pingMs: Int? = null,
    val isMeasuring: Boolean = false,
    val measuredAtMs: Long = 0L
) {
    val quality: PingQuality
        get() = RegionPingMonitor.classifyQuality(pingMs)
}

/**
 * Measures real TCP round-trip latency (SYN -> SYN-ACK handshake on port 443) from the
 * user's actual device/network to each geo-pinned GeForce NOW target region server.
 *
 * DNS resolution runs before timing begins so DNS lookup latency never inflates the ping.
 * Each region runs concurrently on Dispatchers.IO and updates StateFlow as results arrive.
 */
object RegionPingMonitor {

    private const val PROBE_PORT = 443
    private const val CONNECT_TIMEOUT_MS = 1_800
    private const val SAMPLES_PER_HOST = 2
    private const val DEFAULT_CACHE_TTL_MS = 60_000L

    private val _pings = MutableStateFlow<Map<GfnRegionPreset, RegionPingResult>>(emptyMap())
    val pings: StateFlow<Map<GfnRegionPreset, RegionPingResult>> = _pings.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @Volatile
    private var lastFullRefreshMs: Long = 0L

    @Volatile
    private var activeJob: Job? = null

    fun classifyQuality(pingMs: Int?): PingQuality = when {
        pingMs == null || pingMs <= 0 -> PingQuality.UNKNOWN
        pingMs < 60 -> PingQuality.EXCELLENT
        pingMs < 120 -> PingQuality.GOOD
        pingMs < 190 -> PingQuality.FAIR
        else -> PingQuality.POOR
    }

    fun formatBadgeText(result: RegionPingResult?): String = when {
        result == null -> "..."
        result.isMeasuring && result.pingMs == null -> "..."
        result.pingMs != null -> "${result.pingMs}ms"
        else -> "--"
    }

    /**
     * Triggers a fresh parallel ping sweep if no results exist yet or the cached sweep is stale.
     */
    fun refreshIfStale(scope: CoroutineScope, maxAgeMs: Long = DEFAULT_CACHE_TTL_MS) {
        val now = System.currentTimeMillis()
        val hasAnyResults = _pings.value.isNotEmpty()
        if (hasAnyResults && (now - lastFullRefreshMs) < maxAgeMs) return
        if (activeJob?.isActive == true) return
        refreshNow(scope)
    }

    /**
     * Forces an immediate parallel TCP ping sweep across all GfnRegionPreset servers.
     */
    fun refreshNow(scope: CoroutineScope) {
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            _pings.update { current ->
                GfnRegionPreset.entries.associateWith { region ->
                    val prev = current[region]
                    RegionPingResult(
                        pingMs = prev?.pingMs,
                        isMeasuring = true,
                        measuredAtMs = prev?.measuredAtMs ?: 0L
                    )
                }
            }
            try {
                coroutineScope {
                    GfnRegionPreset.entries.map { region ->
                        async(Dispatchers.IO) {
                            val measured = measureRegionRttMs(region)
                            val now = System.currentTimeMillis()
                            _pings.update { current ->
                                current + (region to RegionPingResult(
                                    pingMs = measured,
                                    isMeasuring = false,
                                    measuredAtMs = now
                                ))
                            }
                        }
                    }.awaitAll()
                }
                lastFullRefreshMs = System.currentTimeMillis()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Probes each region's geo-pinned hosts in priority order until one responds with a real TCP RTT.
     */
    suspend fun measureRegionRttMs(region: GfnRegionPreset): Int? = withContext(Dispatchers.IO) {
        for (host in region.probeHosts) {
            val rtt = measureHostTcpRttMs(host)
            if (rtt != null) return@withContext rtt
        }
        null
    }

    /**
     * Measures clean TCP connect RTT (excluding DNS resolution) using the best of 2 samples
     * to filter single-packet wireless jitter.
     */
    private fun measureHostTcpRttMs(host: String): Int? {
        val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return null
        var bestMs: Int? = null
        repeat(SAMPLES_PER_HOST) {
            val sample = runCatching {
                val startNs = System.nanoTime()
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(address, PROBE_PORT), CONNECT_TIMEOUT_MS)
                }
                val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceIn(1, 999)
                elapsedMs
            }.getOrNull()
            if (sample != null) {
                bestMs = if (bestMs == null) sample else minOf(bestMs!!, sample)
            } else if (bestMs == null) {
                // First connect failed/timed out on this host -> try fallback host quickly
                return null
            }
        }
        return bestMs
    }
}
