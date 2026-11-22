package com.example.model

import androidx.compose.ui.graphics.ColorMatrix
import com.squareup.moshi.JsonClass

enum class FilterTintPreset(
    val displayName: String,
    val argbHex: Long
) {
    NEUTRAL("Neutral", 0x00FFFFFFL),
    WARM_AMBER("Warm Amber", 0xFFFF9800L),
    COOL_CYAN("Cool Cyan", 0xFF00E5FFL),
    EMERALD_NIGHT("NVIDIA Green", 0xFF76B900L),
    CRIMSON_TACTICAL("Crimson", 0xFFFF4D6DL),
    VIOLET_DUSK("Violet Dusk", 0xFFB388FFL)
}

@JsonClass(generateAdapter = true)
data class GameFilterStyle(
    val slot: Int,
    val name: String,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val shadows: Float = 0f,
    val colorVibrance: Float = 0f,
    val temperature: Float = 0f,
    val tintPreset: FilterTintPreset = FilterTintPreset.NEUTRAL,
    val tintIntensity: Float = 0f
)

@JsonClass(generateAdapter = true)
data class GameFilterState(
    val activeSlot: Int = 0, // 0 = Off, 1..3 = Style 1..3
    val styles: List<GameFilterStyle> = defaultStyles()
) {
    val isEnabled: Boolean
        get() = activeSlot in 1..3

    val activeStyle: GameFilterStyle?
        get() = if (activeSlot in 1..3) {
            styles.firstOrNull { it.slot == activeSlot } ?: defaultStyleForSlot(activeSlot)
        } else {
            null
        }

    fun styleForSlot(slot: Int): GameFilterStyle =
        styles.firstOrNull { it.slot == slot } ?: defaultStyleForSlot(slot)

    fun withUpdatedStyle(updated: GameFilterStyle): GameFilterState {
        val nextList = (1..3).map { s ->
            if (s == updated.slot) updated else styleForSlot(s)
        }
        return copy(styles = nextList)
    }

    fun withResetSlot(slot: Int): GameFilterState {
        if (slot !in 1..3) return this
        val currentName = styleForSlot(slot).name
        val clean = GameFilterStyle(slot = slot, name = currentName)
        return withUpdatedStyle(clean)
    }

    companion object {
        fun defaultStyleForSlot(slot: Int): GameFilterStyle = when (slot) {
            1 -> GameFilterStyle(
                slot = 1,
                name = "DBD Clarity",
                brightness = 0.10f,
                contrast = 0.16f,
                shadows = 0.42f,
                colorVibrance = 0.18f,
                temperature = 0.04f,
                tintPreset = FilterTintPreset.NEUTRAL,
                tintIntensity = 0f
            )
            2 -> GameFilterStyle(
                slot = 2,
                name = "Vivid HDR",
                brightness = 0.06f,
                contrast = 0.24f,
                shadows = 0.15f,
                colorVibrance = 0.48f,
                temperature = 0.08f,
                tintPreset = FilterTintPreset.NEUTRAL,
                tintIntensity = 0f
            )
            else -> GameFilterStyle(
                slot = 3,
                name = "Night Ops",
                brightness = -0.14f,
                contrast = 0.14f,
                shadows = 0.22f,
                colorVibrance = -0.08f,
                temperature = -0.20f,
                tintPreset = FilterTintPreset.COOL_CYAN,
                tintIntensity = 0.16f
            )
        }

        fun defaultStyles(): List<GameFilterStyle> = listOf(
            defaultStyleForSlot(1),
            defaultStyleForSlot(2),
            defaultStyleForSlot(3)
        )

        fun default(): GameFilterState = GameFilterState(
            activeSlot = 0,
            styles = defaultStyles()
        )
    }
}

object ColorMatrixBuilder {

    /**
     * Builds a 4x5 row-major ColorMatrix array (20 floats) combining:
     * Color/Vibrance (saturation), Contrast, Shadows lift/depth, Brightness,
     * Temperature, and Tint Color/Intensity. Pure math so it runs cleanly in JVM tests.
     */
    fun buildColorMatrixValues(style: GameFilterStyle): FloatArray {
        val sat = (1f + style.colorVibrance).coerceIn(0f, 2.2f)
        val invSat = 1f - sat
        val lumR = 0.2126f
        val lumG = 0.7152f
        val lumB = 0.0722f

        val contrast = (1f + style.contrast).coerceIn(0.4f, 1.8f)
        val contrastTranslate = 128f * (1f - contrast)
        val brightnessTranslate = style.brightness.coerceIn(-0.5f, 0.5f) * 125f
        val shadowsTranslate = style.shadows.coerceIn(-0.5f, 1.0f) * 68f
        val baseTranslate = contrastTranslate + brightnessTranslate + shadowsTranslate

        val temp = style.temperature.coerceIn(-0.5f, 0.5f)
        val tempRedScale = (1f + temp * 0.28f).coerceIn(0.6f, 1.4f)
        val tempGreenScale = (1f + temp * 0.04f).coerceIn(0.8f, 1.2f)
        val tempBlueScale = (1f - temp * 0.30f).coerceIn(0.6f, 1.4f)

        // Tint matrix blend
        val tintAlpha = if (style.tintPreset == FilterTintPreset.NEUTRAL) {
            0f
        } else {
            style.tintIntensity.coerceIn(0f, 0.60f)
        }
        val tintR = ((style.tintPreset.argbHex shr 16) and 0xFFL).toFloat()
        val tintG = ((style.tintPreset.argbHex shr 8) and 0xFFL).toFloat()
        val tintB = (style.tintPreset.argbHex and 0xFFL).toFloat()

        // Base saturation rows
        val r0 = (invSat * lumR + sat) * contrast * tempRedScale
        val r1 = (invSat * lumG) * contrast * tempRedScale
        val r2 = (invSat * lumB) * contrast * tempRedScale

        val g0 = (invSat * lumR) * contrast * tempGreenScale
        val g1 = (invSat * lumG + sat) * contrast * tempGreenScale
        val g2 = (invSat * lumB) * contrast * tempGreenScale

        val b0 = (invSat * lumR) * contrast * tempBlueScale
        val b1 = (invSat * lumG) * contrast * tempBlueScale
        val b2 = (invSat * lumB + sat) * contrast * tempBlueScale

        val keep = 1f - tintAlpha
        return floatArrayOf(
            r0 * keep, r1 * keep, r2 * keep, 0f, baseTranslate * keep + tintR * tintAlpha,
            g0 * keep, g1 * keep, g2 * keep, 0f, baseTranslate * keep + tintG * tintAlpha,
            b0 * keep, b1 * keep, b2 * keep, 0f, baseTranslate * keep + tintB * tintAlpha,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun toComposeColorMatrix(style: GameFilterStyle): ColorMatrix =
        ColorMatrix(buildColorMatrixValues(style))
}
