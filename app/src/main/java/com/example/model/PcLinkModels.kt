package com.example.model

/**
 * PC / Laptop Remote Play streaming clients supported by XenosPad's touch overlay bridge.
 */
enum class PcStreamClient(
    val displayName: String,
    val shortLabel: String,
    val packageName: String,
    val defaultPort: Int,
    val subtitle: String
) {
    STEAM_LINK(
        displayName = "Steam Link",
        shortLabel = "Steam Link",
        packageName = "com.valvesoftware.steamlink",
        defaultPort = 27036,
        subtitle = "Official Valve Steam Remote Play app with XenosPad touch overlay"
    ),
    MOONLIGHT(
        displayName = "Moonlight / Sunshine",
        shortLabel = "Moonlight",
        packageName = "com.limelight",
        defaultPort = 47989,
        subtitle = "Ultra-low-latency Sunshine / GameStream PC streaming"
    ),
    PARSEC(
        displayName = "Parsec PC",
        shortLabel = "Parsec",
        packageName = "tv.parsec.client",
        defaultPort = 8000,
        subtitle = "Low-latency PC desktop & local co-op streaming"
    ),
    WEB_STREAM(
        displayName = "Browser / Web Stream",
        shortLabel = "Web Play",
        packageName = "",
        defaultPort = 47990,
        subtitle = "Open a local PC web stream URL directly inside XenosPad Web Play"
    )
}

/**
 * User-saved Gaming PC or Laptop profile for Steam Link / PC Remote Play with XenosPad touch controls.
 */
data class SavedPcHost(
    val id: String,
    val name: String,
    val hostAddress: String,
    val macAddress: String = "",
    val client: PcStreamClient = PcStreamClient.STEAM_LINK,
    val customWebUrl: String = "",
    val linkedProfileId: String = "",
    val autoStartOverlay: Boolean = true
)

/**
 * Real LAN host discovered via Steam In-Home Streaming / Sunshine TCP/UDP scan on the user's Wi-Fi.
 */
data class DiscoveredPcHost(
    val ipAddress: String,
    val hostname: String,
    val detectedService: String,
    val rttMs: Int,
    val suggestedClient: PcStreamClient
)

/**
 * Live reachability & RTT ping result for a saved PC/Laptop host.
 */
data class PcHostPingStatus(
    val reachable: Boolean,
    val rttMs: Int?,
    val checkedAtMs: Long,
    val detail: String
)
