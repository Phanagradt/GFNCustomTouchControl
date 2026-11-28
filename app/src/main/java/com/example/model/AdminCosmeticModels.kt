package com.example.model

object AdminConfig {
    const val PRIMARY_ADMIN_EMAIL = "november112561@gmail.com"

    fun isPrimaryAdminEmail(email: String?): Boolean {
        val normalized = email?.trim() ?: return false
        return normalized.equals(PRIMARY_ADMIN_EMAIL, ignoreCase = true)
    }
}

enum class CosmeticItemType(val label: String, val pluralLabel: String) {
    FRAME("Profile Frame", "Frames"),
    NAME_TAG("Name Tag", "Name Tags"),
    VERIFICATION_ICON("Verification Icon", "Icons");

    companion object {
        fun fromWire(value: String?): CosmeticItemType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FRAME
    }
}

enum class ItemAvailability(val label: String, val shortBadge: String) {
    FREE_SHOP("Free in Shop", "FREE SHOP"),
    EXCLUSIVE("Exclusive (Admin Only)", "EXCLUSIVE");

    companion object {
        fun fromWire(value: String?): ItemAvailability =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXCLUSIVE
    }
}

enum class VerificationIconPreset(val wireId: String, val label: String) {
    CHECK("CHECK", "Check"),
    STAR("STAR", "Star"),
    BOLT("BOLT", "Bolt"),
    SHIELD("SHIELD", "Shield"),
    WORKSPACE("WORKSPACE", "Crown"),
    DIAMOND("DIAMOND", "Gem"),
    VERIFIED("VERIFIED", "Seal"),
    CUSTOM("CUSTOM", "Upload");

    companion object {
        fun fromWire(value: String?): VerificationIconPreset =
            entries.firstOrNull { it.wireId.equals(value, ignoreCase = true) } ?: VERIFIED
    }
}

data class CosmeticItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: CosmeticItemType = CosmeticItemType.FRAME,
    val availability: ItemAvailability = ItemAvailability.FREE_SHOP,
    val primaryColorHex: Long = 0xFF8CE000L,
    val secondaryColorHex: Long = 0xFF00E5FFL,
    val textColorHex: Long = 0xFFFFFFFFL,
    val borderWidthDp: Float = 3.0f,
    val tagText: String = "",
    val iconPreset: String = VerificationIconPreset.VERIFIED.wireId,
    val imageBase64: String? = null,
    val createdByUid: String = "",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

object CosmeticPalettes {
    val PRIMARY_SWATCHES: List<Pair<String, Long>> = listOf(
        "Royal Gold" to 0xFFD4AF37L,
        "Verified Blue" to 0xFF1D9BF0L,
        "GeForce Lime" to 0xFF8CE000L,
        "Tactical Cyan" to 0xFF00E5FFL,
        "DBD Crimson" to 0xFFE53935L,
        "Neon Violet" to 0xFF9C4DFFL,
        "Rose Ember" to 0xFFFF4081L,
        "Solar Amber" to 0xFFFFB300L,
        "Emerald" to 0xFF00C853L,
        "Obsidian" to 0xFF151C28L
    )

    val SECONDARY_SWATCHES: List<Pair<String, Long>> = listOf(
        "Amber Glow" to 0xFFFFD54FL,
        "Sky Pulse" to 0xFF40C4FFL,
        "Dark Gold" to 0xFF4A3500L,
        "Midnight" to 0xFF0B1324L,
        "Crimson Deep" to 0xFF5D0F18L,
        "Void Indigo" to 0xFF28124BL,
        "Lime Frost" to 0xFFC6FF00L,
        "Pure White" to 0xFFFFFFFFL,
        "Steel" to 0xFF37474FL,
        "Coral" to 0xFFFF7043L
    )

    val TEXT_SWATCHES: List<Pair<String, Long>> = listOf(
        "White" to 0xFFFFFFFFL,
        "Gold" to 0xFFFFD54FL,
        "Lime" to 0xFF8CE000L,
        "Cyan" to 0xFF00E5FFL,
        "Dark Ink" to 0xFF0A0F18L
    )
}
