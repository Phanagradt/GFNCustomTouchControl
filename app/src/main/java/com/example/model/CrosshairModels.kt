package com.example.model

import com.squareup.moshi.JsonClass

enum class CrosshairStyle(val displayName: String) {
    CLASSIC_PLUS("Classic +"),
    DOT_ONLY("Precision Dot"),
    CIRCLE_DOT("Holo Ring"),
    T_SHAPE("T-Bar FPS"),
    X_MARK("Sniper X"),
    CUSTOM_IMAGE("Custom Image")
}

@JsonClass(generateAdapter = true)
data class CrosshairSettings(
    val enabled: Boolean = false,
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_PLUS,
    val colorHex: Long = 0xFF8CE000L,
    val opacity: Float = 0.92f,
    val thicknessDp: Float = 2.5f,
    val lengthDp: Float = 9.0f,
    val gapDp: Float = 5.0f,
    val outlineEnabled: Boolean = true,
    val outlineThicknessDp: Float = 1.5f,
    val outlineColorHex: Long = 0xFF000000L,
    val centerDot: Boolean = true,
    val offsetXdp: Float = 0f,
    val offsetYdp: Float = 0f,
    val customImageBase64: String? = null,
    val customImageSizeDp: Float = 38f
)

data class CrosshairPreset(
    val id: String,
    val name: String,
    val applyTo: (CrosshairSettings) -> CrosshairSettings
)

object CrosshairPresets {
    val colorSwatches: List<Pair<String, Long>> = listOf(
        "Lime" to 0xFF8CE000L,
        "Cyan" to 0xFF00E5FFL,
        "White" to 0xFFFFFFFFL,
        "Crimson" to 0xFFFF5277L,
        "Amber" to 0xFFFFB300L,
        "Green" to 0xFF00FF66L,
        "Magenta" to 0xFFFF4DF0L
    )

    val outlineSwatches: List<Pair<String, Long>> = listOf(
        "Black" to 0xFF000000L,
        "Navy" to 0xFF0B1320L,
        "White" to 0xFFFFFFFFL
    )

    val presets: List<CrosshairPreset> = listOf(
        CrosshairPreset("pro_lime", "Pro Lime +") { current ->
            current.copy(
                enabled = true,
                style = CrosshairStyle.CLASSIC_PLUS,
                colorHex = 0xFF8CE000L,
                thicknessDp = 2.5f,
                lengthDp = 9.0f,
                gapDp = 5.0f,
                outlineEnabled = true,
                outlineThicknessDp = 1.5f,
                outlineColorHex = 0xFF000000L,
                centerDot = true
            )
        },
        CrosshairPreset("headshot_dot", "Headshot Dot") { current ->
            current.copy(
                enabled = true,
                style = CrosshairStyle.DOT_ONLY,
                colorHex = 0xFF00E5FFL,
                thicknessDp = 4.0f,
                outlineEnabled = true,
                outlineThicknessDp = 1.6f,
                outlineColorHex = 0xFF000000L,
                centerDot = true
            )
        },
        CrosshairPreset("holo_ring", "Holo Ring") { current ->
            current.copy(
                enabled = true,
                style = CrosshairStyle.CIRCLE_DOT,
                colorHex = 0xFFFFB300L,
                thicknessDp = 2.2f,
                lengthDp = 11.0f,
                gapDp = 6.0f,
                outlineEnabled = true,
                outlineThicknessDp = 1.2f,
                outlineColorHex = 0xFF000000L,
                centerDot = true
            )
        },
        CrosshairPreset("t_bar_red", "Red T-Bar") { current ->
            current.copy(
                enabled = true,
                style = CrosshairStyle.T_SHAPE,
                colorHex = 0xFFFF5277L,
                thicknessDp = 2.5f,
                lengthDp = 10.0f,
                gapDp = 5.5f,
                outlineEnabled = true,
                outlineThicknessDp = 1.5f,
                outlineColorHex = 0xFF000000L,
                centerDot = false
            )
        },
        CrosshairPreset("sniper_x", "Sniper X") { current ->
            current.copy(
                enabled = true,
                style = CrosshairStyle.X_MARK,
                colorHex = 0xFFFFFFFFL,
                thicknessDp = 2.0f,
                lengthDp = 11.0f,
                gapDp = 5.0f,
                outlineEnabled = true,
                outlineThicknessDp = 1.5f,
                outlineColorHex = 0xFF000000L,
                centerDot = true
            )
        }
    )
}
