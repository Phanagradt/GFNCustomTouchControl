package com.example.model

import com.squareup.moshi.JsonClass

/**
 * Better xCloud-style Region Bypass presets for play.geforcenow.com.
 * Uses regional X-Forwarded-For, Accept-Language, and optional Intl timezone/locale
 * headers on GeForce NOW endpoints (not a remote encrypted VPN tunnel).
 */
enum class GfnRegionPreset(
    val displayName: String,
    val shortCode: String,
    val fakeIp: String,
    val acceptLanguage: String,
    val timeZone: String
) {
    OFF("Off (Local IP)", "OFF", "", "", ""),
    US_WEST("US West (California)", "US-W", "104.196.112.42", "en-US,en;q=0.9", "America/Los_Angeles"),
    US_EAST("US East (Virginia)", "US-E", "35.199.48.19", "en-US,en;q=0.9", "America/New_York"),
    US_CENTRAL("US Central (Texas)", "US-C", "34.122.90.14", "en-US,en;q=0.9", "America/Chicago"),
    EU_CENTRAL("EU Central (Frankfurt)", "EU-C", "35.198.114.27", "en-GB,en;q=0.9,de;q=0.8", "Europe/Berlin"),
    EU_WEST("EU West (London)", "EU-W", "35.189.96.18", "en-GB,en;q=0.9", "Europe/London"),
    JAPAN("Japan (Tokyo)", "JP", "34.84.128.55", "ja-JP,ja;q=0.9,en-US;q=0.8", "Asia/Tokyo"),
    SOUTH_KOREA("South Korea (Seoul)", "KR", "34.64.92.31", "ko-KR,ko;q=0.9,en-US;q=0.8", "Asia/Seoul"),
    SINGAPORE("Singapore (SEA)", "SG", "34.87.140.22", "en-SG,en;q=0.9", "Asia/Singapore"),
    TAIWAN("Taiwan (Taipei)", "TW", "35.194.180.64", "zh-TW,zh;q=0.9,en-US;q=0.8", "Asia/Taipei"),
    AUSTRALIA("Australia (Sydney)", "AU", "35.189.44.71", "en-AU,en;q=0.9", "Australia/Sydney");

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
    val showControlsByDefault: Boolean = true
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
