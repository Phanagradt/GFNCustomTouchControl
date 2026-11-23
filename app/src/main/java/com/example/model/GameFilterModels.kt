package com.example.model

import androidx.compose.ui.graphics.ColorMatrix
import com.squareup.moshi.JsonClass
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class FilterTintPreset(
    val displayName: String,
    val argbHex: Long,
    val hueDegrees: Float
) {
    NEUTRAL("Neutral", 0x00FFFFFFL, 0f),
    WARM_AMBER("Warm Amber", 0xFFFF9800L, 36f),
    COOL_CYAN("Cool Cyan", 0xFF00E5FFL, 186f),
    EMERALD_NIGHT("NVIDIA Green", 0xFF76B900L, 82f),
    CRIMSON_TACTICAL("Crimson", 0xFFFF4D6DL, 349f),
    VIOLET_DUSK("Violet Dusk", 0xFFB388FFL, 262f)
}

enum class FreestyleCategory(val displayName: String) {
    ESSENTIAL("Essential"),
    ACCESSIBILITY("Utility"),
    ARTISTIC("Artistic")
}

enum class ColorblindMode(val displayName: String, val shortCode: String, val shaderIndex: Int) {
    PROTANOPIA("Protanopia (Red-Weak)", "Protanopia", 1),
    DEUTERANOPIA("Deuteranopia (Green-Weak)", "Deuteranopia", 2),
    TRITANOPIA("Tritanopia (Blue-Weak)", "Tritanopia", 3)
}

enum class BlackWhiteTone(val displayName: String, val shaderIndex: Int) {
    MONOCHROME("Pure Mono", 0),
    WARM("Warm Silver", 1),
    COOL("Cool Steel", 2),
    HIGH_CONTRAST("Noir Punch", 3)
}

enum class MoodFilterPreset(val displayName: String, val shaderIndex: Int) {
    WARM_SUNSET("Warm Sunset", 1),
    COLD_THRILLER("Cold Thriller", 2),
    WASHED_RETRO("Washed Retro", 3),
    CYBER_NEON("Cyber Neon", 4),
    EMERALD_CINEMA("Emerald Cinema", 5),
    GOLDEN_NOIR("Golden Noir", 6)
}

enum class SpecialFxMode(val displayName: String, val shaderIndex: Int) {
    RETRO("Retro Pixel", 1),
    SKETCH("Pencil Sketch", 2),
    HALFTONE("Half-Tone", 3)
}

enum class StickerPreset(val displayName: String) {
    TACTICAL_HUD("Tactical HUD"),
    CINEMA_REC("Cinema REC"),
    NEON_CORNERS("Neon Frame"),
    RETRO_CRT("CRT Bezel"),
    CUSTOM_IMAGE("Custom Image")
}

@JsonClass(generateAdapter = true)
data class GameFilterStyle(
    val slot: Int,
    val name: String,

    // Legacy / Brightness-Contrast compatibility fields (preserved for existing saves & tests)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val shadows: Float = 0f,
    val colorVibrance: Float = 0f,
    val temperature: Float = 0f,
    val tintPreset: FilterTintPreset = FilterTintPreset.NEUTRAL,
    val tintIntensity: Float = 0f,

    // 1A. Sharpen
    val sharpenEnabled: Boolean = false,
    val sharpenAmount: Float = 0.45f,          // 0..1
    val sharpenIgnoreFilmGrain: Float = 0.20f, // 0..1

    // 1B. Sharpen+ (Texture-Aware)
    val sharpenPlusEnabled: Boolean = false,
    val sharpenPlusIntensity: Float = 0.45f,      // 0..1
    val sharpenPlusTextureDetails: Float = 0.30f, // -1..1

    // 1C. Brightness / Contrast (Full GeForce suite)
    val brightnessContrastEnabled: Boolean = true,
    val exposure: Float = 0f,    // -1..1
    val highlights: Float = 0f,  // -1..1
    val gamma: Float = 0f,       // -1..1

    // 1D. Details
    val detailsEnabled: Boolean = false,
    val detailsSharpen: Float = 0.25f,   // 0..1
    val detailsClarity: Float = 0.30f,   // -1..1
    val detailsHdrToning: Float = 0.35f, // -1..1
    val detailsBloom: Float = 0.0f,      // 0..1

    // 1E. Color (Full Spectrum Hue Tint + Temperature + Vibrance)
    val colorEnabled: Boolean = true,
    val tintHue: Float = 36f, // 0..360 degrees

    // 2A. Colorblind
    val colorblindEnabled: Boolean = false,
    val colorblindMode: ColorblindMode = ColorblindMode.DEUTERANOPIA,
    val colorblindIntensity: Float = 0.65f, // 0..1

    // 2B. Night Mode
    val nightModeEnabled: Boolean = false,
    val nightModeIntensity: Float = 0.45f, // 0..1

    // 2C. Splitscreen Comparison
    val splitscreenEnabled: Boolean = false,
    val splitscreenPosition: Float = 0.50f, // 0..1 (left = original, right = filtered)

    // 3A. Black & White
    val blackWhiteEnabled: Boolean = false,
    val blackWhiteTone: BlackWhiteTone = BlackWhiteTone.MONOCHROME,
    val blackWhiteIntensity: Float = 0.85f, // 0..1

    // 3B. Sepia
    val sepiaEnabled: Boolean = false,
    val sepiaIntensity: Float = 0.65f, // 0..1

    // 3C. Vignette
    val vignetteEnabled: Boolean = false,
    val vignetteIntensity: Float = 0.45f, // -1..1 (positive darkens, negative lightens)

    // 3D. Depth of Field / Tilt-Shift
    val dofEnabled: Boolean = false,
    val dofBlurCurve: Float = 0.45f,  // 0.1..1
    val dofFocusDepth: Float = 0.50f, // 0..1
    val dofNearBlur: Float = 0.45f,   // 0..1
    val dofFarBlur: Float = 0.55f,    // 0..1

    // 3E. Old Film
    val oldFilmEnabled: Boolean = false,
    val oldFilmGamma: Float = 0.10f,        // -1..1
    val oldFilmExposure: Float = -0.05f,    // -1..1
    val oldFilmContrast: Float = 0.18f,     // -1..1
    val oldFilmVignette: Float = 0.45f,     // 0..1
    val oldFilmStrength: Float = 0.70f,     // 0..1
    val oldFilmDirtStrength: Float = 0.45f, // 0..1

    // 3F. Mood LUTs
    val moodEnabled: Boolean = false,
    val moodPreset: MoodFilterPreset = MoodFilterPreset.WARM_SUNSET,
    val moodIntensity: Float = 0.60f, // 0..1

    // 3G. Special FX
    val specialFxEnabled: Boolean = false,
    val specialFxMode: SpecialFxMode = SpecialFxMode.RETRO,
    val specialFxIntensity: Float = 0.75f, // 0..1

    // 3H. Painterly
    val painterlyEnabled: Boolean = false,
    val painterlyEdgeSharpening: Float = 0.45f, // 0..1
    val painterlyFilterRadius: Float = 0.45f,   // 0.1..1
    val painterlyEdgeThreshold: Float = 0.30f,  // 0.05..1

    // 3I. Letterbox
    val letterboxEnabled: Boolean = false,
    val letterboxHeight: Float = 0.12f, // 0..0.40
    val letterboxWidth: Float = 0.0f,   // 0..0.40

    // 3J. Stickers
    val stickersEnabled: Boolean = false,
    val stickerPreset: StickerPreset = StickerPreset.TACTICAL_HUD,
    val stickerScale: Float = 1.0f,   // 0.3..1.5
    val stickerOpacity: Float = 0.85f, // 0.1..1.0
    val stickerX: Float = 0.50f,       // 0.1..0.9 (for custom badge positioning)
    val stickerY: Float = 0.50f,       // 0.1..0.9
    val customStickerBase64: String? = null
) {
    /** Effective combined exposure accounting for both new exposure and legacy brightness. */
    val effectiveExposure: Float
        get() = (exposure + brightness).coerceIn(-1f, 1f)

    /** Resolves tint RGB (0..1) from either custom hue wheel or legacy preset. */
    fun resolvedTintRgb(): Triple<Float, Float, Float> {
        if (tintPreset != FilterTintPreset.NEUTRAL && tintIntensity > 0.001f) {
            val r = ((tintPreset.argbHex shr 16) and 0xFFL) / 255f
            val g = ((tintPreset.argbHex shr 8) and 0xFFL) / 255f
            val b = (tintPreset.argbHex and 0xFFL) / 255f
            return Triple(r, g, b)
        }
        return hueToRgb(tintHue)
    }

    /** Count of actively enabled Freestyle modules for badge display. */
    fun activeModuleCount(): Int {
        var count = 0
        if (sharpenEnabled && sharpenAmount > 0.01f) count++
        if (sharpenPlusEnabled && sharpenPlusIntensity > 0.01f) count++
        if (brightnessContrastEnabled && (abs(effectiveExposure) > 0.01f || abs(contrast) > 0.01f || abs(highlights) > 0.01f || abs(shadows) > 0.01f || abs(gamma) > 0.01f)) count++
        if (detailsEnabled && (detailsSharpen > 0.01f || abs(detailsClarity) > 0.01f || abs(detailsHdrToning) > 0.01f || detailsBloom > 0.01f)) count++
        if (colorEnabled && (abs(colorVibrance) > 0.01f || abs(temperature) > 0.01f || tintIntensity > 0.01f)) count++
        if (colorblindEnabled && colorblindIntensity > 0.01f) count++
        if (nightModeEnabled && nightModeIntensity > 0.01f) count++
        if (splitscreenEnabled) count++
        if (blackWhiteEnabled && blackWhiteIntensity > 0.01f) count++
        if (sepiaEnabled && sepiaIntensity > 0.01f) count++
        if (vignetteEnabled && abs(vignetteIntensity) > 0.01f) count++
        if (dofEnabled && (dofNearBlur > 0.01f || dofFarBlur > 0.01f)) count++
        if (oldFilmEnabled && oldFilmStrength > 0.01f) count++
        if (moodEnabled && moodIntensity > 0.01f) count++
        if (specialFxEnabled && specialFxIntensity > 0.01f) count++
        if (painterlyEnabled) count++
        if (letterboxEnabled && (letterboxHeight > 0.005f || letterboxWidth > 0.005f)) count++
        if (stickersEnabled) count++
        return count
    }

    companion object {
        fun hueToRgb(hueDeg: Float): Triple<Float, Float, Float> {
            val h = ((hueDeg % 360f) + 360f) % 360f
            val c = 1f
            val x = c * (1f - abs((h / 60f) % 2f - 1f))
            return when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        }
    }
}

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
                name = "Cloud Clarity+",
                sharpenPlusEnabled = true,
                sharpenPlusIntensity = 0.45f,
                sharpenPlusTextureDetails = 0.35f,
                brightnessContrastEnabled = true,
                exposure = 0.08f,
                contrast = 0.16f,
                highlights = -0.10f,
                shadows = 0.42f,
                gamma = 0.05f,
                detailsEnabled = true,
                detailsSharpen = 0.20f,
                detailsClarity = 0.30f,
                detailsHdrToning = 0.35f,
                detailsBloom = 0.0f,
                colorEnabled = true,
                colorVibrance = 0.22f,
                temperature = 0.04f,
                tintPreset = FilterTintPreset.NEUTRAL,
                tintIntensity = 0f
            )
            2 -> GameFilterStyle(
                slot = 2,
                name = "Vivid HDR",
                sharpenEnabled = true,
                sharpenAmount = 0.35f,
                sharpenIgnoreFilmGrain = 0.25f,
                brightnessContrastEnabled = true,
                exposure = 0.06f,
                contrast = 0.24f,
                highlights = 0.12f,
                shadows = 0.18f,
                gamma = 0.0f,
                detailsEnabled = true,
                detailsSharpen = 0.25f,
                detailsClarity = 0.40f,
                detailsHdrToning = 0.55f,
                detailsBloom = 0.12f,
                colorEnabled = true,
                colorVibrance = 0.50f,
                temperature = 0.08f,
                tintPreset = FilterTintPreset.NEUTRAL,
                tintIntensity = 0f
            )
            else -> GameFilterStyle(
                slot = 3,
                name = "Night Cinema",
                brightnessContrastEnabled = true,
                exposure = -0.08f,
                contrast = 0.18f,
                shadows = 0.24f,
                colorEnabled = true,
                colorVibrance = -0.06f,
                temperature = 0.0f,
                nightModeEnabled = true,
                nightModeIntensity = 0.35f,
                vignetteEnabled = true,
                vignetteIntensity = 0.30f,
                moodEnabled = true,
                moodPreset = MoodFilterPreset.COLD_THRILLER,
                moodIntensity = 0.30f
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
     * Builds a 4x5 row-major ColorMatrix array (20 floats) combining Exposure/Brightness,
     * Contrast, Shadows, Highlights, Gamma, Color/Vibrance, Temperature, Tint, Night Mode,
     * Colorblind Daltonization, Black & White, Sepia, and Mood grades.
     * Used as fallback for Compose ColorFilter surfaces and deterministic JVM unit tests.
     */
    fun buildColorMatrixValues(style: GameFilterStyle): FloatArray {
        val bcOn = style.brightnessContrastEnabled
        val colorOn = style.colorEnabled

        val sat = if (colorOn) (1f + style.colorVibrance).coerceIn(0f, 2.2f) else 1f
        val invSat = 1f - sat
        val lumR = 0.2126f
        val lumG = 0.7152f
        val lumB = 0.0722f

        val contrast = if (bcOn) (1f + style.contrast).coerceIn(0.4f, 2.0f) else 1f
        val contrastTranslate = 128f * (1f - contrast)
        val effExposure = if (bcOn) style.effectiveExposure else 0f
        val brightnessTranslate = effExposure.coerceIn(-1f, 1f) * 125f
        val shadowsTranslate = if (bcOn) style.shadows.coerceIn(-1f, 1f) * 68f else 0f
        val highlightsTranslate = if (bcOn) style.highlights.coerceIn(-1f, 1f) * 28f else 0f
        val gammaTranslate = if (bcOn) style.gamma.coerceIn(-1f, 1f) * 32f else 0f
        var baseTranslate = contrastTranslate + brightnessTranslate + shadowsTranslate + highlightsTranslate + gammaTranslate

        val temp = if (colorOn) style.temperature.coerceIn(-1f, 1f) else 0f
        var redScale = (1f + temp * 0.28f).coerceIn(0.55f, 1.45f)
        var greenScale = (1f + temp * 0.04f).coerceIn(0.75f, 1.25f)
        var blueScale = (1f - temp * 0.30f).coerceIn(0.55f, 1.45f)

        // Night Mode blue-light suppression
        if (style.nightModeEnabled && style.nightModeIntensity > 0.005f) {
            val n = style.nightModeIntensity.coerceIn(0f, 1f)
            redScale *= (1f + n * 0.08f)
            greenScale *= (1f - n * 0.10f)
            blueScale *= (1f - n * 0.52f)
        }

        // Mood LUT approximation
        if (style.moodEnabled && style.moodIntensity > 0.005f) {
            val m = style.moodIntensity.coerceIn(0f, 1f)
            when (style.moodPreset) {
                MoodFilterPreset.WARM_SUNSET -> {
                    redScale *= 1f + 0.18f * m
                    greenScale *= 1f + 0.04f * m
                    blueScale *= 1f - 0.16f * m
                }
                MoodFilterPreset.COLD_THRILLER -> {
                    redScale *= 1f - 0.14f * m
                    greenScale *= 1f + 0.03f * m
                    blueScale *= 1f + 0.20f * m
                }
                MoodFilterPreset.WASHED_RETRO -> {
                    baseTranslate += 18f * m
                    redScale *= 1f + 0.05f * m
                    blueScale *= 1f - 0.08f * m
                }
                MoodFilterPreset.CYBER_NEON -> {
                    redScale *= 1f + 0.14f * m
                    greenScale *= 1f - 0.06f * m
                    blueScale *= 1f + 0.18f * m
                }
                MoodFilterPreset.EMERALD_CINEMA -> {
                    redScale *= 1f - 0.10f * m
                    greenScale *= 1f + 0.14f * m
                    blueScale *= 1f + 0.04f * m
                }
                MoodFilterPreset.GOLDEN_NOIR -> {
                    redScale *= 1f + 0.16f * m
                    greenScale *= 1f + 0.10f * m
                    blueScale *= 1f - 0.22f * m
                }
            }
        }

        // Tint matrix blend
        val tintAlpha = if (!colorOn) {
            0f
        } else if (style.tintPreset == FilterTintPreset.NEUTRAL && style.tintIntensity <= 0.001f) {
            0f
        } else {
            style.tintIntensity.coerceIn(0f, 0.70f)
        }
        val (tintRf, tintGf, tintBf) = style.resolvedTintRgb()
        val tintR = tintRf * 255f
        val tintG = tintGf * 255f
        val tintB = tintBf * 255f

        var r0 = (invSat * lumR + sat) * contrast * redScale
        var r1 = (invSat * lumG) * contrast * redScale
        var r2 = (invSat * lumB) * contrast * redScale

        var g0 = (invSat * lumR) * contrast * greenScale
        var g1 = (invSat * lumG + sat) * contrast * greenScale
        var g2 = (invSat * lumB) * contrast * greenScale

        var b0 = (invSat * lumR) * contrast * blueScale
        var b1 = (invSat * lumG) * contrast * blueScale
        var b2 = (invSat * lumB + sat) * contrast * blueScale

        // Black & White blend
        if (style.blackWhiteEnabled && style.blackWhiteIntensity > 0.005f) {
            val bw = style.blackWhiteIntensity.coerceIn(0f, 1f)
            val wr = when (style.blackWhiteTone) {
                BlackWhiteTone.WARM -> 1.06f
                BlackWhiteTone.COOL -> 0.94f
                else -> 1.0f
            }
            val wb = when (style.blackWhiteTone) {
                BlackWhiteTone.WARM -> 0.90f
                BlackWhiteTone.COOL -> 1.08f
                else -> 1.0f
            }
            r0 = r0 * (1f - bw) + lumR * wr * contrast * bw
            r1 = r1 * (1f - bw) + lumG * wr * contrast * bw
            r2 = r2 * (1f - bw) + lumB * wr * contrast * bw
            g0 = g0 * (1f - bw) + lumR * contrast * bw
            g1 = g1 * (1f - bw) + lumG * contrast * bw
            g2 = g2 * (1f - bw) + lumB * contrast * bw
            b0 = b0 * (1f - bw) + lumR * wb * contrast * bw
            b1 = b1 * (1f - bw) + lumG * wb * contrast * bw
            b2 = b2 * (1f - bw) + lumB * wb * contrast * bw
        }

        // Sepia blend
        if (style.sepiaEnabled && style.sepiaIntensity > 0.005f) {
            val sp = style.sepiaIntensity.coerceIn(0f, 1f)
            r0 = r0 * (1f - sp) + 0.393f * sp
            r1 = r1 * (1f - sp) + 0.769f * sp
            r2 = r2 * (1f - sp) + 0.189f * sp
            g0 = g0 * (1f - sp) + 0.349f * sp
            g1 = g1 * (1f - sp) + 0.686f * sp
            g2 = g2 * (1f - sp) + 0.168f * sp
            b0 = b0 * (1f - sp) + 0.272f * sp
            b1 = b1 * (1f - sp) + 0.534f * sp
            b2 = b2 * (1f - sp) + 0.131f * sp
        }

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

    /** Helper used by shader uniforms and tests to verify LMS Daltonization shift cleanly. */
    fun applyDaltonization(
        r: Float,
        g: Float,
        b: Float,
        mode: ColorblindMode,
        intensity: Float
    ): Triple<Float, Float, Float> {
        val t = intensity.coerceIn(0f, 1f)
        if (t <= 0.001f) return Triple(r, g, b)
        val l = 17.8824f * r + 43.5161f * g + 4.11935f * b
        val m = 3.45565f * r + 27.1554f * g + 3.86714f * b
        val s = 0.0299566f * r + 0.184309f * g + 1.46709f * b
        val (ls, ms, ss) = when (mode) {
            ColorblindMode.PROTANOPIA -> Triple(2.02344f * m - 2.52581f * s, m, s)
            ColorblindMode.DEUTERANOPIA -> Triple(l, 0.494207f * l + 1.24827f * s, s)
            ColorblindMode.TRITANOPIA -> Triple(l, m, -0.395913f * l + 0.801109f * m)
        }
        val simR = (0.080944f * ls - 0.130504f * ms + 0.116721f * ss).coerceIn(0f, 1f)
        val simG = (-0.0102485f * ls + 0.0540194f * ms - 0.113615f * ss).coerceIn(0f, 1f)
        val simB = (-0.000365294f * ls - 0.00412163f * ms + 0.693513f * ss).coerceIn(0f, 1f)
        val errR = r - simR
        val errG = g - simG
        val errB = b - simB
        val corrR = (r + (0.0f * errR + 0.7f * errG + 0.7f * errB) * t).coerceIn(0f, 1f)
        val corrG = (g + (0.7f * errR + 0.0f * errG + 0.0f * errB) * t).coerceIn(0f, 1f)
        val corrB = (b + (0.7f * errR + 0.7f * errG + 0.0f * errB) * t).coerceIn(0f, 1f)
        return Triple(corrR, corrG, corrB)
    }

    /** Unused trig imports safeguard removed if needed — pure helpers for Hue rotation checks. */
    @Suppress("unused")
    fun safeCosSin(deg: Float): Pair<Float, Float> {
        val rad = Math.toRadians(deg.toDouble())
        return cos(rad).toFloat() to sin(rad).toFloat()
    }
}
