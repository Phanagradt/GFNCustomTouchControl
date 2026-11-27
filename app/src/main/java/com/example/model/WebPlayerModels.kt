package com.example.model

import com.squareup.moshi.JsonClass

/**
 * Better xCloud-style Region Bypass presets for play.geforcenow.com.
 * Every active region routes to an official NVIDIA-operated GeForce NOW region
 * that supports the Free 1-Hour Session tier (no paid-only Alliance Partner regions).
 * Uses regional X-Forwarded-For, Accept-Language, and optional Intl timezone/locale
 * headers on GeForce NOW endpoints (not a remote encrypted VPN tunnel).
 */
enum class GfnRegionPreset(
    val displayName: String,
    val shortCode: String,
    val fakeIp: String,
    val acceptLanguage: String,
    val timeZone: String,
    val hasFreeOneHourTier: Boolean = true,
    val tierLabel: String = "Free 1h",
    val probeHosts: List<String> = emptyList()
) {
    OFF(
        displayName = "Off (Local IP)",
        shortCode = "OFF",
        fakeIp = "",
        acceptLanguage = "",
        timeZone = "",
        hasFreeOneHourTier = false,
        tierLabel = "",
        probeHosts = listOf("play.geforcenow.com", "www.nvidia.com")
    ),
    NETHERLANDS(
        displayName = "Netherlands (Amsterdam)",
        shortCode = "NL",
        fakeIp = "35.204.118.42",
        acceptLanguage = "nl-NL,nl;q=0.9,en-US;q=0.8",
        timeZone = "Europe/Amsterdam",
        probeHosts = listOf("np-ams-03.cloudmatchbeta.nvidiagrid.net", "ams-nl-ping.vultr.com", "ec2.eu-west-1.amazonaws.com")
    ),
    US_WEST(
        displayName = "US West (California)",
        shortCode = "US-W",
        fakeIp = "104.196.112.42",
        acceptLanguage = "en-US,en;q=0.9",
        timeZone = "America/Los_Angeles",
        probeHosts = listOf("np-sjc6-01.cloudmatchbeta.nvidiagrid.net", "ec2.us-west-1.amazonaws.com", "sjo-ca-us-ping.vultr.com")
    ),
    US_EAST(
        displayName = "US East (Virginia)",
        shortCode = "US-E",
        fakeIp = "35.199.48.19",
        acceptLanguage = "en-US,en;q=0.9",
        timeZone = "America/New_York",
        probeHosts = listOf("np-ash-03.cloudmatchbeta.nvidiagrid.net", "ec2.us-east-1.amazonaws.com", "nj-us-ping.vultr.com")
    ),
    US_CENTRAL(
        displayName = "US Central (Texas)",
        shortCode = "US-C",
        fakeIp = "34.122.90.14",
        acceptLanguage = "en-US,en;q=0.9",
        timeZone = "America/Chicago",
        probeHosts = listOf("np-dal-03.cloudmatchbeta.nvidiagrid.net", "tx-us-ping.vultr.com", "ec2.us-east-2.amazonaws.com")
    ),
    CANADA_EAST(
        displayName = "Canada East (Montreal)",
        shortCode = "CA-E",
        fakeIp = "35.203.92.18",
        acceptLanguage = "en-CA,en;q=0.9,fr-CA;q=0.8",
        timeZone = "America/Toronto",
        probeHosts = listOf("np-mon-01.cloudmatchbeta.nvidiagrid.net", "ec2.ca-central-1.amazonaws.com", "tor-ca-ping.vultr.com")
    ),
    EU_CENTRAL(
        displayName = "Germany (Frankfurt)",
        shortCode = "DE",
        fakeIp = "35.198.114.27",
        acceptLanguage = "de-DE,de;q=0.9,en-GB;q=0.8",
        timeZone = "Europe/Berlin",
        probeHosts = listOf("np-frk-04.cloudmatchbeta.nvidiagrid.net", "ec2.eu-central-1.amazonaws.com", "fra-de-ping.vultr.com")
    ),
    EU_WEST(
        displayName = "UK (London)",
        shortCode = "UK",
        fakeIp = "35.189.96.18",
        acceptLanguage = "en-GB,en;q=0.9",
        timeZone = "Europe/London",
        probeHosts = listOf("np-lon-03.cloudmatchbeta.nvidiagrid.net", "ec2.eu-west-2.amazonaws.com", "lon-gb-ping.vultr.com")
    ),
    EU_SOUTHWEST(
        displayName = "France (Paris)",
        shortCode = "FR",
        fakeIp = "34.155.84.29",
        acceptLanguage = "fr-FR,fr;q=0.9,en-US;q=0.8",
        timeZone = "Europe/Paris",
        probeHosts = listOf("np-par-02.cloudmatchbeta.nvidiagrid.net", "ec2.eu-west-3.amazonaws.com", "par-fr-ping.vultr.com")
    ),
    EU_NORTHEAST(
        displayName = "Sweden (Stockholm)",
        shortCode = "SE",
        fakeIp = "34.88.112.45",
        acceptLanguage = "sv-SE,sv;q=0.9,en-GB;q=0.8",
        timeZone = "Europe/Stockholm",
        probeHosts = listOf("np-sth-01.cloudmatchbeta.nvidiagrid.net", "ec2.eu-north-1.amazonaws.com", "sto-se-ping.vultr.com")
    ),
    EU_EAST(
        displayName = "Poland (Warsaw)",
        shortCode = "PL",
        fakeIp = "34.118.64.21",
        acceptLanguage = "pl-PL,pl;q=0.9,en-GB;q=0.8",
        timeZone = "Europe/Warsaw",
        probeHosts = listOf("np-war-01.cloudmatchbeta.nvidiagrid.net", "waw-pl-ping.vultr.com", "ec2.eu-central-1.amazonaws.com")
    ),
    JAPAN(
        displayName = "Japan (Tokyo)",
        shortCode = "JP",
        fakeIp = "34.84.128.55",
        acceptLanguage = "ja-JP,ja;q=0.9,en-US;q=0.8",
        timeZone = "Asia/Tokyo",
        probeHosts = listOf("np-tyo-01.cloudmatchbeta.nvidiagrid.net", "ec2.ap-northeast-1.amazonaws.com", "hnd-jp-ping.vultr.com")
    );

    val isEnabled: Boolean
        get() = this != OFF && fakeIp.isNotBlank()
}

/**
 * Browser User-Agent profiles for play.geforcenow.com.
 * ChromeOS Desktop unlocks full web keyboard/mouse & gamepad streaming on Android WebView.
 */
enum class WebPlayerUserAgentMode(
    val displayName: String,
    val shortLabel: String,
    val userAgentString: String
) {
    CHROME_OS(
        displayName = "ChromeOS Desktop (Recommended)",
        shortLabel = "ChromeOS",
        userAgentString = "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    WINDOWS_CHROME(
        displayName = "Windows Chrome",
        shortLabel = "Windows",
        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    MAC_CHROME(
        displayName = "macOS Chrome",
        shortLabel = "macOS",
        userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    ),
    ANDROID_CHROME(
        displayName = "Android Chrome",
        shortLabel = "Android",
        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    )
}

@JsonClass(generateAdapter = true)
data class WebStreamQualityConfig(
    val regionPreset: GfnRegionPreset = GfnRegionPreset.OFF,
    val userAgentMode: WebPlayerUserAgentMode = WebPlayerUserAgentMode.CHROME_OS,
    val spoofTimezoneAndLocale: Boolean = true,
    val enableVirtualGamepad: Boolean = true,
    val stretchVideoToFill: Boolean = false,
    val enableCssFilters: Boolean = true,
    val showWebRtcStatsHud: Boolean = true,
    val showControlsByDefault: Boolean = true,
    val antiAfkEnabled: Boolean = false,
    val antiAfkIntervalSec: Int = 45
)

@JsonClass(generateAdapter = true)
data class PhysicalControllerConfig(
    val autoHideTouchControls: Boolean = true,
    val preferPhysicalSlotZero: Boolean = true,
    val swapFaceButtonsAB: Boolean = false,
    val stickDeadzone: Float = 0.12f
)

/**
 * Live WebRTC stream telemetry reported directly from RTCPeerConnection.getStats()
 * inside XenosWebPlayerActivity.
 */
data class WebRtcStreamStats(
    val fps: Int = 0,
    val rttMs: Int = 0,
    val bitrateMbps: Float = 0f,
    val packetLossPercent: Float = 0f,
    val jitterMs: Float = 0f,
    val decodeMs: Float = 0f,
    val resolution: String = "",
    val codec: String = "",
    val connected: Boolean = false,
    val updatedAtMs: Long = 0L
)
