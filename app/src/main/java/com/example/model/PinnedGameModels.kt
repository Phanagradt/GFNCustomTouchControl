package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * A user-pinned GeForce NOW game entry displayed in the Cockpit Pinned Games Hub.
 * Launching a pinned game automatically loads its assigned control layout profile,
 * Freestyle visual filter slot, and Built-in Region Bypass server before opening Web Play.
 */
@JsonClass(generateAdapter = true)
data class PinnedGame(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val profileId: String,
    val filterSlot: Int = 0, // 0 = Off, 1..3 = Style 1..3
    val regionPreset: GfnRegionPreset = GfnRegionPreset.OFF,
    val accentHex: Long = ControlAccent.LIME.hex,
    val customLaunchUrl: String = "",
    val lastPlayedAtMs: Long = 0L
) {
    fun normalizedLaunchUrl(): String? {
        val trimmed = customLaunchUrl.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            trimmed
        } else {
            null
        }
    }
}

object PinnedGameCatalog {
    val quickSuggestions: List<String> = listOf(
        "Fortnite",
        "Apex Legends",
        "Cyberpunk 2077",
        "Genshin Impact",
        "Call of Duty",
        "Counter-Strike 2",
        "Destiny 2",
        "Rocket League"
    )
}
