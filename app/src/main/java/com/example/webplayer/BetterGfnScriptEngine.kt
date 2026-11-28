package com.example.webplayer

import android.view.KeyEvent
import com.example.model.BlackWhiteTone
import com.example.model.ColorMatrixBuilder
import com.example.model.ColorblindMode
import com.example.model.GameFilterStyle
import com.example.model.GfnRegionPreset
import com.example.model.MoodFilterPreset
import com.example.model.PhysicalControllerConfig
import com.example.model.SpecialFxMode
import com.example.model.WebRtcStreamStats
import com.example.model.WebStreamQualityConfig
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class DomKeyMapping(
    val code: String,
    val key: String,
    val keyCode: Int
)

/**
 * Generates pure, deterministic JavaScript payloads for XenosPad's Better-GFN Web Player:
 * 1. Better xCloud-style Region Bypass (X-Forwarded-For + Accept-Language + optional Intl timezone/locale)
 * 2. Standard W3C Virtual Xbox 360 Gamepad API (navigator.getGamepads() + gamepadconnected)
 * 3. Direct KeyboardEvent / MouseEvent / PointerEvent / WheelEvent dispatcher
 * 4. Real RTCPeerConnection.getStats() stream telemetry bridge
 * 5. Better xCloud-style SVG <feConvolveMatrix> Clarity Boost + <feColorMatrix> + Vignette/Letterbox video compositor
 */
object BetterGfnScriptEngine {

    const val GFN_WEB_URL = "https://play.geforcenow.com"
    const val XBOX_CLOUD_URL = "https://www.xbox.com/play"

    fun isXboxCloudUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.US)
        return lower.contains("xbox.com") ||
            lower.contains("xboxlive.com") ||
            lower.contains("gamepass.com") ||
            lower.contains("xcloud.microsoft.com")
    }

    fun isTrustedGfnOrigin(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.US)
        return lower.startsWith("https://") && (
            lower.contains("geforcenow.com") ||
                lower.contains("nvidia.com") ||
                lower.contains("nvidiagrid.net") ||
                lower.contains("xbox.com") ||
                lower.contains("xboxlive.com") ||
                lower.contains("gamepass.com") ||
                lower.contains("xcloud.microsoft.com")
            )
    }

    /**
     * HTTP headers attached to top-level WebView navigation requests when Region Bypass is active.
     */
    fun buildNavigationHeaders(config: WebStreamQualityConfig): Map<String, String> {
        val region = config.regionPreset
        if (!region.isEnabled) return emptyMap()
        val headers = LinkedHashMap<String, String>()
        headers["X-Forwarded-For"] = region.fakeIp
        if (region.acceptLanguage.isNotBlank()) {
            headers["Accept-Language"] = region.acceptLanguage
        }
        return headers
    }

    /**
     * Maps Android gamepad KeyEvent keyCodes to standard W3C Gamepad API button indices (0..16).
     * Returns null for non-gamepad keys so they route through KeyboardEvent instead.
     */
    fun mapKeyCodeToGamepadButtonIndex(androidKeyCode: Int): Int? = when (androidKeyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> 0
        KeyEvent.KEYCODE_BUTTON_B -> 1
        KeyEvent.KEYCODE_BUTTON_X -> 2
        KeyEvent.KEYCODE_BUTTON_Y -> 3
        KeyEvent.KEYCODE_BUTTON_L1 -> 4
        KeyEvent.KEYCODE_BUTTON_R1 -> 5
        KeyEvent.KEYCODE_BUTTON_L2 -> 6
        KeyEvent.KEYCODE_BUTTON_R2 -> 7
        KeyEvent.KEYCODE_BUTTON_SELECT -> 8
        KeyEvent.KEYCODE_BUTTON_START -> 9
        KeyEvent.KEYCODE_BUTTON_THUMBL -> 10
        KeyEvent.KEYCODE_BUTTON_THUMBR -> 11
        KeyEvent.KEYCODE_DPAD_UP -> 12
        KeyEvent.KEYCODE_DPAD_DOWN -> 13
        KeyEvent.KEYCODE_DPAD_LEFT -> 14
        KeyEvent.KEYCODE_DPAD_RIGHT -> 15
        KeyEvent.KEYCODE_BUTTON_MODE -> 16
        else -> null
    }

    /**
     * Maps Android KeyEvent keyCodes to W3C DOM KeyboardEvent (code, key, legacy keyCode).
     */
    fun mapKeyCodeToDomKey(androidKeyCode: Int): DomKeyMapping {
        // Letters A..Z
        if (androidKeyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            val offset = androidKeyCode - KeyEvent.KEYCODE_A
            val upper = ('A'.code + offset).toChar()
            val lower = ('a'.code + offset).toChar()
            return DomKeyMapping("Key$upper", lower.toString(), 65 + offset)
        }
        // Digits 0..9
        if (androidKeyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            val digit = androidKeyCode - KeyEvent.KEYCODE_0
            return DomKeyMapping("Digit$digit", digit.toString(), 48 + digit)
        }
        // Function keys F1..F12
        if (androidKeyCode in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12) {
            val num = androidKeyCode - KeyEvent.KEYCODE_F1 + 1
            return DomKeyMapping("F$num", "F$num", 111 + num)
        }
        return when (androidKeyCode) {
            KeyEvent.KEYCODE_SPACE -> DomKeyMapping("Space", " ", 32)
            KeyEvent.KEYCODE_ENTER -> DomKeyMapping("Enter", "Enter", 13)
            KeyEvent.KEYCODE_ESCAPE -> DomKeyMapping("Escape", "Escape", 27)
            KeyEvent.KEYCODE_TAB -> DomKeyMapping("Tab", "Tab", 9)
            KeyEvent.KEYCODE_DEL -> DomKeyMapping("Backspace", "Backspace", 8)
            KeyEvent.KEYCODE_FORWARD_DEL -> DomKeyMapping("Delete", "Delete", 46)
            KeyEvent.KEYCODE_SHIFT_LEFT -> DomKeyMapping("ShiftLeft", "Shift", 16)
            KeyEvent.KEYCODE_SHIFT_RIGHT -> DomKeyMapping("ShiftRight", "Shift", 16)
            KeyEvent.KEYCODE_CTRL_LEFT -> DomKeyMapping("ControlLeft", "Control", 17)
            KeyEvent.KEYCODE_CTRL_RIGHT -> DomKeyMapping("ControlRight", "Control", 17)
            KeyEvent.KEYCODE_ALT_LEFT -> DomKeyMapping("AltLeft", "Alt", 18)
            KeyEvent.KEYCODE_ALT_RIGHT -> DomKeyMapping("AltRight", "Alt", 18)
            KeyEvent.KEYCODE_CAPS_LOCK -> DomKeyMapping("CapsLock", "CapsLock", 20)
            KeyEvent.KEYCODE_DPAD_UP -> DomKeyMapping("ArrowUp", "ArrowUp", 38)
            KeyEvent.KEYCODE_DPAD_DOWN -> DomKeyMapping("ArrowDown", "ArrowDown", 40)
            KeyEvent.KEYCODE_DPAD_LEFT -> DomKeyMapping("ArrowLeft", "ArrowLeft", 37)
            KeyEvent.KEYCODE_DPAD_RIGHT -> DomKeyMapping("ArrowRight", "ArrowRight", 39)
            KeyEvent.KEYCODE_PAGE_UP -> DomKeyMapping("PageUp", "PageUp", 33)
            KeyEvent.KEYCODE_PAGE_DOWN -> DomKeyMapping("PageDown", "PageDown", 34)
            KeyEvent.KEYCODE_MINUS -> DomKeyMapping("Minus", "-", 189)
            KeyEvent.KEYCODE_EQUALS -> DomKeyMapping("Equal", "=", 187)
            KeyEvent.KEYCODE_LEFT_BRACKET -> DomKeyMapping("BracketLeft", "[", 219)
            KeyEvent.KEYCODE_RIGHT_BRACKET -> DomKeyMapping("BracketRight", "]", 221)
            KeyEvent.KEYCODE_BACKSLASH -> DomKeyMapping("Backslash", "\\", 220)
            KeyEvent.KEYCODE_SEMICOLON -> DomKeyMapping("Semicolon", ";", 186)
            KeyEvent.KEYCODE_APOSTROPHE -> DomKeyMapping("Quote", "'", 222)
            KeyEvent.KEYCODE_GRAVE -> DomKeyMapping("Backquote", "`", 192)
            KeyEvent.KEYCODE_COMMA -> DomKeyMapping("Comma", ",", 188)
            KeyEvent.KEYCODE_PERIOD -> DomKeyMapping("Period", ".", 190)
            KeyEvent.KEYCODE_SLASH -> DomKeyMapping("Slash", "/", 191)
            else -> DomKeyMapping("Unidentified", "Unidentified", 0)
        }
    }

    /**
     * Returns true ONLY when the active filter requires pixel-neighborhood convolution,
     * discrete posterization LUT, SVG bloom blur, or Daltonization colorblind matrix.
     *
     * Standard color/tone filters (Brightness, Contrast, Exposure, Shadows, Highlights, Gamma,
     * Color Vibrance/Temperature/Tint, Night Mode, Mood, B&W, Sepia, Old Film, Vignette,
     * Letterbox, Splitscreen, Stickers) run on Chromium's zero-overhead hardware CSS compositor
     * fast path (60 FPS locked with zero SVG offscreen FBO readbacks).
     */
    fun requiresSvgFilterPipeline(style: GameFilterStyle?): Boolean {
        if (style == null) return false
        if (buildSharpenKernelMatrix(style) != null) return true
        if (style.detailsEnabled && style.detailsBloom > 0.02f) return true
        if (style.painterlyEnabled) return true
        if (style.specialFxEnabled && style.specialFxMode == SpecialFxMode.RETRO && style.specialFxIntensity > 0.05f) {
            return true
        }
        if (style.colorblindEnabled && style.colorblindIntensity > 0.01f) return true
        return false
    }

    /**
     * Converts an active XenosPad GameFilterStyle into a compact hardware-composited CSS filter
     * chain that executes in a single fused GPU pass inside Chromium's Viz compositor at full 60 FPS.
     */
    fun buildCssFilterString(style: GameFilterStyle?): String {
        if (style == null) return "none"
        val parts = ArrayList<String>()

        var brightness = 1f
        var contrast = 1f
        var saturate = 1f
        var hueDeg = 0f
        var sepia = 0f
        var grayscale = 0f

        if (style.brightnessContrastEnabled) {
            brightness += style.effectiveExposure * 0.65f +
                style.shadows * 0.22f +
                style.gamma * 0.18f +
                style.highlights * 0.10f
            contrast += style.contrast * 0.85f
        }

        if (style.detailsEnabled) {
            contrast += style.detailsClarity * 0.28f + style.detailsHdrToning * 0.14f
            saturate += style.detailsHdrToning * 0.24f
        }

        if (style.sharpenEnabled) {
            contrast += style.sharpenAmount * 0.12f
        }
        if (style.sharpenPlusEnabled) {
            contrast += style.sharpenPlusIntensity * 0.15f
        }

        if (style.colorEnabled) {
            saturate += style.colorVibrance * 0.95f
            if (abs(style.temperature) > 0.015f) {
                hueDeg += -style.temperature * 18f
                if (style.temperature > 0f) {
                    sepia = maxOf(sepia, style.temperature * 0.22f)
                }
            }
        }

        if (style.nightModeEnabled && style.nightModeIntensity > 0.01f) {
            val n = style.nightModeIntensity.coerceIn(0f, 1f)
            sepia = maxOf(sepia, (n * 0.55f).coerceIn(0f, 0.72f))
            brightness -= n * 0.08f
        }

        if (style.moodEnabled && style.moodIntensity > 0.01f) {
            val m = style.moodIntensity.coerceIn(0f, 1f)
            when (style.moodPreset) {
                MoodFilterPreset.WARM_SUNSET -> {
                    sepia = maxOf(sepia, m * 0.28f)
                    saturate += m * 0.25f
                    hueDeg -= m * 10f
                }
                MoodFilterPreset.COLD_THRILLER -> {
                    contrast += m * 0.16f
                    saturate -= m * 0.12f
                    hueDeg += m * 14f
                }
                MoodFilterPreset.WASHED_RETRO -> {
                    contrast -= m * 0.18f
                    brightness += m * 0.08f
                    saturate -= m * 0.25f
                    sepia = maxOf(sepia, m * 0.22f)
                }
                MoodFilterPreset.CYBER_NEON -> {
                    contrast += m * 0.24f
                    saturate += m * 0.55f
                    hueDeg += m * 18f
                }
                MoodFilterPreset.EMERALD_CINEMA -> {
                    contrast += m * 0.12f
                    saturate += m * 0.16f
                    hueDeg += m * 10f
                }
                MoodFilterPreset.GOLDEN_NOIR -> {
                    contrast += m * 0.28f
                    grayscale = maxOf(grayscale, m * 0.55f)
                    sepia = maxOf(sepia, m * 0.35f)
                }
            }
        }

        if (style.oldFilmEnabled && style.oldFilmStrength > 0.01f) {
            val f = style.oldFilmStrength.coerceIn(0f, 1f)
            brightness += style.oldFilmExposure * 0.35f * f + style.oldFilmGamma * 0.15f * f
            contrast += style.oldFilmContrast * 0.45f * f
            sepia = maxOf(sepia, f * 0.42f)
            saturate -= f * 0.30f
        }

        if (style.sepiaEnabled && style.sepiaIntensity > 0.01f) {
            sepia = maxOf(sepia, style.sepiaIntensity.coerceIn(0f, 1f))
        }

        if (style.blackWhiteEnabled && style.blackWhiteIntensity > 0.01f) {
            val bw = style.blackWhiteIntensity.coerceIn(0f, 1f)
            grayscale = maxOf(grayscale, bw)
            when (style.blackWhiteTone) {
                BlackWhiteTone.WARM -> sepia = maxOf(sepia, bw * 0.24f)
                BlackWhiteTone.HIGH_CONTRAST -> contrast += bw * 0.45f
                else -> Unit
            }
        }

        val finalBrightness = brightness.coerceIn(0.35f, 2.25f)
        val finalContrast = contrast.coerceIn(0.45f, 2.35f)
        val finalSaturate = saturate.coerceIn(0f, 2.55f)
        val finalHue = hueDeg.coerceIn(-45f, 45f)
        val finalSepia = sepia.coerceIn(0f, 1f)
        val finalGray = grayscale.coerceIn(0f, 1f)

        if (abs(finalBrightness - 1f) > 0.015f) {
            parts += String.format(Locale.US, "brightness(%.2f)", finalBrightness)
        }
        if (abs(finalContrast - 1f) > 0.015f) {
            parts += String.format(Locale.US, "contrast(%.2f)", finalContrast)
        }
        if (abs(finalSaturate - 1f) > 0.015f) {
            parts += String.format(Locale.US, "saturate(%.2f)", finalSaturate)
        }
        if (abs(finalHue) > 0.5f) {
            parts += String.format(Locale.US, "hue-rotate(%.1fdeg)", finalHue)
        }
        if (finalSepia > 0.015f) {
            parts += String.format(Locale.US, "sepia(%.2f)", finalSepia)
        }
        if (finalGray > 0.015f) {
            parts += String.format(Locale.US, "grayscale(%.2f)", finalGray)
        }

        return if (parts.isEmpty()) "none" else parts.joinToString(" ")
    }

    /**
     * Converts XenosPad's exact 4x5 ColorMatrixBuilder values (including Exposure, Contrast,
     * Shadows, Highlights, Gamma, Vibrance, Temperature, Hue Tint, Night Mode, Mood LUTs,
     * B&W tones, Sepia, and optional Colorblind Daltonization) into a 20-float SVG
     * <feColorMatrix type="matrix"> string (5th column normalized to 0..1).
     */
    fun buildSvgColorMatrixValues(style: GameFilterStyle): String {
        val m = ColorMatrixBuilder.buildColorMatrixValues(style).copyOf()
        // Normalize Android ColorMatrix translation column (0..255 -> 0..1 for SVG feColorMatrix)
        m[4] = m[4] / 255f
        m[9] = m[9] / 255f
        m[14] = m[14] / 255f
        m[19] = m[19] / 255f

        // Fold Details Clarity & HDR Toning micro-contrast into the matrix when enabled
        if (style.detailsEnabled) {
            val clarityBoost = (1f + style.detailsClarity * 0.25f).coerceIn(0.65f, 1.45f)
            val clarityShift = 0.5f * (1f - clarityBoost)
            for (row in 0..2) {
                val base = row * 5
                m[base] *= clarityBoost
                m[base + 1] *= clarityBoost
                m[base + 2] *= clarityBoost
                m[base + 4] = m[base + 4] * clarityBoost + clarityShift
            }
        }

        // Compose Colorblind Daltonization linear shift if enabled
        if (style.colorblindEnabled && style.colorblindIntensity > 0.01f) {
            val t = style.colorblindIntensity.coerceIn(0f, 1f)
            val d = when (style.colorblindMode) {
                ColorblindMode.PROTANOPIA -> floatArrayOf(
                    1f - 0.15f * t, 0.20f * t, 0.05f * t,
                    0.35f * t, 1f - 0.10f * t, 0.0f,
                    0.35f * t, 0.10f * t, 1f + 0.10f * t
                )
                ColorblindMode.DEUTERANOPIA -> floatArrayOf(
                    1f + 0.15f * t, 0.25f * t, 0.0f,
                    0.15f * t, 1f - 0.20f * t, 0.15f * t,
                    0.0f, 0.30f * t, 1f + 0.10f * t
                )
                ColorblindMode.TRITANOPIA -> floatArrayOf(
                    1f + 0.10f * t, 0.0f, 0.30f * t,
                    0.0f, 1f + 0.10f * t, 0.25f * t,
                    0.15f * t, 0.15f * t, 1f - 0.25f * t
                )
            }
            val orig = m.copyOf()
            for (r in 0..2) {
                val dr = r * 3
                val out = r * 5
                for (c in 0..4) {
                    if (c == 3) continue
                    m[out + c] = d[dr] * orig[c] + d[dr + 1] * orig[5 + c] + d[dr + 2] * orig[10 + c]
                }
            }
        }

        return m.joinToString(" ") { v -> String.format(Locale.US, "%.4f", v) }
    }

    /**
     * Builds a Better xCloud-style Clarity Boost 3x3 unsharp-mask kernel for SVG <feConvolveMatrix>.
     * Sum of weights is always 1.0 so overall brightness is preserved while edges sharpen cleanly.
     */
    fun buildSharpenKernelMatrix(style: GameFilterStyle): String? {
        var strength = 0f
        if (style.sharpenEnabled && style.sharpenAmount > 0.01f) {
            strength += style.sharpenAmount * 0.75f
        }
        if (style.sharpenPlusEnabled && style.sharpenPlusIntensity > 0.01f) {
            val detailBoost = 1f + style.sharpenPlusTextureDetails.coerceIn(-0.4f, 0.6f) * 0.4f
            strength += style.sharpenPlusIntensity * 0.85f * detailBoost
        }
        if (style.detailsEnabled && style.detailsSharpen > 0.01f) {
            strength += style.detailsSharpen * 0.55f
        }
        if (style.painterlyEnabled && style.painterlyEdgeSharpening > 0.01f) {
            strength += style.painterlyEdgeSharpening * 0.65f
        }
        if (style.specialFxEnabled && style.specialFxMode == SpecialFxMode.SKETCH && style.specialFxIntensity > 0.05f) {
            strength += style.specialFxIntensity * 1.15f
        }
        val s = strength.coerceIn(0f, 1.95f)
        if (s < 0.03f) return null
        val center = 1f + 4f * s
        val neg = -s
        return String.format(
            Locale.US,
            "0 %.3f 0 %.3f %.3f %.3f 0 %.3f 0",
            neg, neg, center, neg, neg
        )
    }

    /**
     * Generates ultra-fast browser video filter JavaScript running inside Chromium's hardware
     * Viz GPU compositor:
     * - Fast Path (60 FPS locked): For Color, Brightness/Contrast, Night Mode, Mood, B&W, Sepia,
     *   Vignette, Old Film, Letterbox, and Splitscreen, applies pure hardware-composited CSS
     *   filter directly on <video> with ZERO SVG offscreen render-target readbacks.
     * - Full SVG Path (only when Sharpen, Bloom, Painterly, or Colorblind Daltonization is on):
     *   injects tight-bounded SVG <filter x="0%" y="0%" width="100%" height="100%"> with capped
     *   fast blur radius.
     * - Zero DOM-thrashing: uses O(1) getElementsByTagName('video'), caches applied style
     *   signatures, and debounces MutationObserver instead of calling querySelectorAll('*').
     */
    fun buildVideoStyleScript(
        style: GameFilterStyle?,
        stretchToFill: Boolean,
        enableCssFilters: Boolean
    ): String {
        val activeStyle = if (enableCssFilters) style else null
        val hasActiveFilter = activeStyle != null
        val useSvgPipeline = activeStyle != null && requiresSvgFilterPipeline(activeStyle)
        val cssFilterExpr = if (activeStyle != null) buildCssFilterString(activeStyle) else "none"
        val svgMatrix = if (activeStyle != null && useSvgPipeline) buildSvgColorMatrixValues(activeStyle) else ""
        val sharpenKernel = if (activeStyle != null && useSvgPipeline) buildSharpenKernelMatrix(activeStyle) else null
        val objectFit = if (stretchToFill) "fill" else "contain"

        val vignetteAlpha = when {
            activeStyle == null -> 0f
            activeStyle.vignetteEnabled && abs(activeStyle.vignetteIntensity) > 0.01f ->
                activeStyle.vignetteIntensity.coerceIn(-1f, 1f)
            activeStyle.oldFilmEnabled && activeStyle.oldFilmVignette > 0.01f ->
                (activeStyle.oldFilmVignette * activeStyle.oldFilmStrength).coerceIn(0f, 1f)
            else -> 0f
        }
        val letterboxH = if (activeStyle != null && activeStyle.letterboxEnabled) {
            (activeStyle.letterboxHeight.coerceIn(0f, 0.38f) * 100f)
        } else {
            0f
        }
        val letterboxW = if (activeStyle != null && activeStyle.letterboxEnabled) {
            (activeStyle.letterboxWidth.coerceIn(0f, 0.38f) * 100f)
        } else {
            0f
        }
        val splitPos = if (activeStyle != null && activeStyle.splitscreenEnabled) {
            (activeStyle.splitscreenPosition.coerceIn(0.05f, 0.95f) * 100f)
        } else {
            -1f
        }

        val bloomStrength = if (activeStyle != null && activeStyle.detailsEnabled) {
            activeStyle.detailsBloom.coerceIn(0f, 1f)
        } else {
            0f
        }
        val oldFilmStrength = if (activeStyle != null && activeStyle.oldFilmEnabled) {
            activeStyle.oldFilmStrength.coerceIn(0f, 1f)
        } else {
            0f
        }
        val oldFilmDirt = if (activeStyle != null && activeStyle.oldFilmEnabled) {
            activeStyle.oldFilmDirtStrength.coerceIn(0f, 1f)
        } else {
            0f
        }
        val dofNear = if (activeStyle != null && activeStyle.dofEnabled) {
            activeStyle.dofNearBlur.coerceIn(0f, 1f)
        } else {
            0f
        }
        val dofFar = if (activeStyle != null && activeStyle.dofEnabled) {
            activeStyle.dofFarBlur.coerceIn(0f, 1f)
        } else {
            0f
        }
        val dofFocus = if (activeStyle != null && activeStyle.dofEnabled) {
            activeStyle.dofFocusDepth.coerceIn(0.15f, 0.85f)
        } else {
            0.5f
        }
        val fxModeName = if (activeStyle != null && activeStyle.specialFxEnabled && activeStyle.specialFxIntensity > 0.02f) {
            activeStyle.specialFxMode.name
        } else {
            "NONE"
        }
        val fxIntensity = if (activeStyle != null && activeStyle.specialFxEnabled) {
            activeStyle.specialFxIntensity.coerceIn(0f, 1f)
        } else {
            0f
        }
        val tintRgba = if (
            activeStyle != null &&
            !useSvgPipeline &&
            activeStyle.colorEnabled &&
            activeStyle.tintIntensity > 0.015f
        ) {
            val (r, g, b) = activeStyle.resolvedTintRgb()
            val alpha = (activeStyle.tintIntensity * 0.24f).coerceIn(0f, 0.26f)
            String.format(
                Locale.US,
                "rgba(%d,%d,%d,%.3f)",
                (r * 255f).roundToInt().coerceIn(0, 255),
                (g * 255f).roundToInt().coerceIn(0, 255),
                (b * 255f).roundToInt().coerceIn(0, 255),
                alpha
            )
        } else {
            ""
        }
        val moodOverlayCss = if (
            activeStyle != null &&
            !useSvgPipeline &&
            activeStyle.moodEnabled &&
            activeStyle.moodIntensity > 0.02f
        ) {
            val a1 = (activeStyle.moodIntensity * 0.14f).coerceIn(0f, 0.16f)
            val a2 = (activeStyle.moodIntensity * 0.18f).coerceIn(0f, 0.20f)
            when (activeStyle.moodPreset) {
                MoodFilterPreset.WARM_SUNSET ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(255,138,61,%.3f), rgba(140,45,102,%.3f))", a1, a2)
                MoodFilterPreset.COLD_THRILLER ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(29,107,153,%.3f), rgba(10,36,56,%.3f))", a1, a2)
                MoodFilterPreset.WASHED_RETRO ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(230,213,184,%.3f), rgba(93,83,74,%.3f))", a1, a2)
                MoodFilterPreset.CYBER_NEON ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(0,229,255,%.3f), rgba(213,0,249,%.3f))", a1, a2)
                MoodFilterPreset.EMERALD_CINEMA ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(22,163,106,%.3f), rgba(8,42,36,%.3f))", a1, a2)
                MoodFilterPreset.GOLDEN_NOIR ->
                    String.format(Locale.US, "linear-gradient(180deg, rgba(217,164,65,%.3f), rgba(20,18,14,%.3f))", a1, a2)
            }
        } else {
            ""
        }
        val posterizeSteps = when {
            activeStyle == null || !useSvgPipeline -> 0
            activeStyle.painterlyEnabled -> (12 - (activeStyle.painterlyFilterRadius.coerceIn(0.1f, 1f) * 7f).roundToInt()).coerceIn(4, 12)
            activeStyle.specialFxEnabled && activeStyle.specialFxMode == SpecialFxMode.RETRO && activeStyle.specialFxIntensity > 0.05f ->
                (14 - (activeStyle.specialFxIntensity * 9f).roundToInt()).coerceIn(4, 14)
            else -> 0
        }
        val posterizeTable = if (posterizeSteps >= 2) {
            (0 until posterizeSteps).joinToString(" ") { idx ->
                String.format(Locale.US, "%.3f", idx.toFloat() / (posterizeSteps - 1).toFloat())
            }
        } else {
            ""
        }

        val activeFilterExpr = when {
            !hasActiveFilter -> "none"
            useSvgPipeline -> "url(#xenos-gfn-video-filter)"
            else -> cssFilterExpr
        }
        val sharpenKernelJs = if (sharpenKernel != null) "'${escapeJs(sharpenKernel)}'" else "null"

        return """
            (function() {
              try {
                window.__xenosVideoStyle = {
                  active: $hasActiveFilter,
                  useSvg: $useSvgPipeline,
                  filter: '${escapeJs(activeFilterExpr)}',
                  cssFallback: '${escapeJs(cssFilterExpr)}',
                  svgMatrix: '${escapeJs(svgMatrix)}',
                  sharpenKernel: $sharpenKernelJs,
                  posterizeTable: '${escapeJs(posterizeTable)}',
                  bloomStrength: ${String.format(Locale.US, "%.3f", bloomStrength)},
                  objectFit: '$objectFit',
                  vignetteAlpha: ${String.format(Locale.US, "%.3f", vignetteAlpha)},
                  tintRgba: '${escapeJs(tintRgba)}',
                  moodOverlayCss: '${escapeJs(moodOverlayCss)}',
                  letterboxH: ${String.format(Locale.US, "%.2f", letterboxH)},
                  letterboxW: ${String.format(Locale.US, "%.2f", letterboxW)},
                  splitPos: ${String.format(Locale.US, "%.1f", splitPos)},
                  oldFilmStrength: ${String.format(Locale.US, "%.3f", oldFilmStrength)},
                  oldFilmDirt: ${String.format(Locale.US, "%.3f", oldFilmDirt)},
                  dofNear: ${String.format(Locale.US, "%.3f", dofNear)},
                  dofFar: ${String.format(Locale.US, "%.3f", dofFar)},
                  dofFocus: ${String.format(Locale.US, "%.3f", dofFocus)},
                  fxMode: '$fxModeName',
                  fxIntensity: ${String.format(Locale.US, "%.3f", fxIntensity)}
                };

                function ensureSvgFilter(cfg) {
                  var svgId = 'xenos-gfn-svg-defs';
                  var svg = document.getElementById(svgId);
                  if (!cfg.active || !cfg.useSvg) {
                    if (svg && svg.parentNode) svg.parentNode.removeChild(svg);
                    return;
                  }
                  if (!svg) {
                    svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                    svg.id = svgId;
                    svg.setAttribute('aria-hidden', 'true');
                    svg.style.cssText = 'position:fixed;width:0;height:0;pointer-events:none;opacity:0;';
                    (document.body || document.documentElement).appendChild(svg);
                  }
                  // Tight 0%..100% bounds prevent Chromium from allocating 144% oversized offscreen textures
                  var inner = '<defs><filter id="xenos-gfn-video-filter" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox" color-interpolation-filters="sRGB">';
                  var lastIn = 'SourceGraphic';
                  if (cfg.sharpenKernel) {
                    inner += '<feConvolveMatrix in="' + lastIn + '" order="3" preserveAlpha="true" kernelMatrix="' + cfg.sharpenKernel + '" result="sharp"/>';
                    lastIn = 'sharp';
                  }
                  inner += '<feColorMatrix in="' + lastIn + '" type="matrix" values="' + cfg.svgMatrix + '" result="graded"/>';
                  lastIn = 'graded';
                  if (cfg.posterizeTable) {
                    inner += '<feComponentTransfer in="' + lastIn + '" result="poster">' +
                      '<feFuncR type="discrete" tableValues="' + cfg.posterizeTable + '"/>' +
                      '<feFuncG type="discrete" tableValues="' + cfg.posterizeTable + '"/>' +
                      '<feFuncB type="discrete" tableValues="' + cfg.posterizeTable + '"/>' +
                      '</feComponentTransfer>';
                    lastIn = 'poster';
                  }
                  if (cfg.bloomStrength > 0.02) {
                    // Capped stdDeviation (1.0..2.4) keeps GPU blur ~8x faster than wide kernels
                    var blurStd = (1.0 + cfg.bloomStrength * 1.4).toFixed(2);
                    var bGain = (cfg.bloomStrength * 0.42).toFixed(3);
                    inner += '<feGaussianBlur in="' + lastIn + '" stdDeviation="' + blurStd + '" result="bloomBlur"/>';
                    inner += '<feColorMatrix in="bloomBlur" type="matrix" values="' + bGain + ' 0 0 0 0  0 ' + bGain + ' 0 0 0  0 0 ' + bGain + ' 0 0  0 0 0 1 0" result="bloomSoft"/>';
                    inner += '<feBlend in="' + lastIn + '" in2="bloomSoft" mode="screen" result="bloomOut"/>';
                    lastIn = 'bloomOut';
                  }
                  inner += '</filter></defs>';
                  if (svg.__lastInner !== inner) {
                    svg.innerHTML = inner;
                    svg.__lastInner = inner;
                  }
                }

                function ensureStyleSheet(cfg) {
                  var styleId = 'xenos-gfn-video-style';
                  var tag = document.getElementById(styleId);
                  if (!tag) {
                    tag = document.createElement('style');
                    tag.id = styleId;
                    (document.head || document.documentElement).appendChild(tag);
                  }
                  var filterCss = cfg.active ? cfg.filter : 'none';
                  var gpuHint = (cfg.active && filterCss !== 'none')
                    ? ' will-change: filter !important; transform: translateZ(0) !important;'
                    : '';
                  var rule = 'video { filter: ' + filterCss + ' !important; -webkit-filter: ' + filterCss + ' !important; object-fit: ' + cfg.objectFit + ' !important;' + gpuHint + ' }';
                  if (tag.textContent !== rule) tag.textContent = rule;
                }

                function ensureFxOverlay(cfg) {
                  var fxId = 'xenos-gfn-fx-layer';
                  var fx = document.getElementById(fxId);
                  var needsFx = cfg.active && (
                    Math.abs(cfg.vignetteAlpha) > 0.01 ||
                    !!cfg.tintRgba ||
                    !!cfg.moodOverlayCss ||
                    cfg.letterboxH > 0.1 ||
                    cfg.letterboxW > 0.1 ||
                    cfg.splitPos > 0 ||
                    cfg.oldFilmStrength > 0.02 ||
                    cfg.dofNear > 0.02 ||
                    cfg.dofFar > 0.02 ||
                    (cfg.fxMode && cfg.fxMode !== 'NONE' && cfg.fxIntensity > 0.02)
                  );
                  if (!needsFx) {
                    if (fx && fx.parentNode) fx.parentNode.removeChild(fx);
                    return;
                  }
                  if (!fx) {
                    fx = document.createElement('div');
                    fx.id = fxId;
                    fx.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:2147483640;overflow:hidden;contain:strict;';
                    (document.body || document.documentElement).appendChild(fx);
                  }
                  var bgLayers = [];
                  if (Math.abs(cfg.vignetteAlpha) > 0.01) {
                    var a = Math.min(0.92, Math.abs(cfg.vignetteAlpha) * 0.85).toFixed(2);
                    var col = cfg.vignetteAlpha > 0 ? 'rgba(0,0,0,' + a + ')' : 'rgba(255,255,255,' + a + ')';
                    bgLayers.push('radial-gradient(ellipse at center, transparent 48%, ' + col + ' 100%)');
                  }
                  if (cfg.tintRgba) {
                    bgLayers.push('linear-gradient(' + cfg.tintRgba + ', ' + cfg.tintRgba + ')');
                  }
                  if (cfg.moodOverlayCss) {
                    bgLayers.push(cfg.moodOverlayCss);
                  }
                  if (cfg.oldFilmStrength > 0.02) {
                    var fa = Math.min(0.32, cfg.oldFilmStrength * 0.24).toFixed(2);
                    bgLayers.push('linear-gradient(180deg, rgba(168,125,68,' + fa + '), rgba(92,62,28,' + fa + '))');
                  }
                  if (cfg.fxMode === 'RETRO' && cfg.fxIntensity > 0.02) {
                    var ra = Math.min(0.36, cfg.fxIntensity * 0.28).toFixed(2);
                    bgLayers.push('repeating-linear-gradient(0deg, rgba(0,0,0,' + ra + ') 0px, rgba(0,0,0,' + ra + ') 1.5px, transparent 1.5px, transparent 4.5px)');
                  } else if (cfg.fxMode === 'HALFTONE' && cfg.fxIntensity > 0.02) {
                    var ha = Math.min(0.34, cfg.fxIntensity * 0.26).toFixed(2);
                    bgLayers.push('radial-gradient(circle, rgba(0,0,0,' + ha + ') 1.4px, transparent 1.6px)');
                  } else if (cfg.fxMode === 'SKETCH' && cfg.fxIntensity > 0.02) {
                    var sa = Math.min(0.24, cfg.fxIntensity * 0.20).toFixed(2);
                    bgLayers.push('linear-gradient(135deg, rgba(245,239,224,' + sa + '), rgba(225,216,195,' + sa + '))');
                  }
                  var bgStr = bgLayers.length ? bgLayers.join(', ') : 'none';
                  if (fx.__lastBg !== bgStr) {
                    fx.style.background = bgStr;
                    fx.__lastBg = bgStr;
                  }
                  var bgSize = (cfg.fxMode === 'HALFTONE') ? '10px 10px' : 'auto';
                  if (fx.__lastBgSize !== bgSize) {
                    fx.style.backgroundSize = bgSize;
                    fx.__lastBgSize = bgSize;
                  }

                  var bars = '';
                  if (cfg.dofFar > 0.02) {
                    var topH = Math.max(8, Math.min(45, (cfg.dofFocus - 0.14) * 100)).toFixed(1);
                    var topBlur = (cfg.dofFar * 3.8).toFixed(1);
                    bars += '<div style="position:absolute;top:0;left:0;right:0;height:' + topH + '%;backdrop-filter:blur(' + topBlur + 'px);-webkit-backdrop-filter:blur(' + topBlur + 'px);-webkit-mask-image:linear-gradient(to bottom, #000 35%, transparent 100%);mask-image:linear-gradient(to bottom, #000 35%, transparent 100%);"></div>';
                  }
                  if (cfg.dofNear > 0.02) {
                    var botH = Math.max(8, Math.min(45, (0.86 - cfg.dofFocus) * 100)).toFixed(1);
                    var botBlur = (cfg.dofNear * 3.8).toFixed(1);
                    bars += '<div style="position:absolute;bottom:0;left:0;right:0;height:' + botH + '%;backdrop-filter:blur(' + botBlur + 'px);-webkit-backdrop-filter:blur(' + botBlur + 'px);-webkit-mask-image:linear-gradient(to top, #000 35%, transparent 100%);mask-image:linear-gradient(to top, #000 35%, transparent 100%);"></div>';
                  }
                  if (cfg.oldFilmStrength > 0.02 && cfg.oldFilmDirt > 0.05) {
                    var lineAlpha = Math.min(0.42, cfg.oldFilmStrength * cfg.oldFilmDirt * 0.45).toFixed(2);
                    bars += '<div style="position:absolute;top:0;bottom:0;left:28%;width:1.5px;background:rgba(255,244,220,' + lineAlpha + ');opacity:0.75;"></div>';
                    bars += '<div style="position:absolute;top:0;bottom:0;left:73%;width:1px;background:rgba(255,244,220,' + lineAlpha + ');opacity:0.55;"></div>';
                  }
                  if (cfg.letterboxH > 0.1) {
                    bars += '<div style="position:absolute;top:0;left:0;right:0;height:' + cfg.letterboxH + '%;background:#000;"></div>';
                    bars += '<div style="position:absolute;bottom:0;left:0;right:0;height:' + cfg.letterboxH + '%;background:#000;"></div>';
                  }
                  if (cfg.letterboxW > 0.1) {
                    bars += '<div style="position:absolute;top:0;bottom:0;left:0;width:' + cfg.letterboxW + '%;background:#000;"></div>';
                    bars += '<div style="position:absolute;top:0;bottom:0;right:0;width:' + cfg.letterboxW + '%;background:#000;"></div>';
                  }
                  if (cfg.splitPos > 0) {
                    bars += '<div style="position:absolute;top:0;bottom:0;left:' + cfg.splitPos + '%;width:2px;background:#8CE000;box-shadow:0 0 8px #8CE000;"></div>';
                    bars += '<div style="position:absolute;top:14px;left:calc(' + cfg.splitPos + '% + 8px);padding:2px 6px;border-radius:5px;background:rgba(10,15,22,0.78);color:#8CE000;font:700 10px monospace;">FILTERED</div>';
                  }
                  if (fx.__lastBars !== bars) {
                    fx.innerHTML = bars;
                    fx.__lastBars = bars;
                  }
                }

                function applyToVideoList(vids, cfg, sig) {
                  var filterCss = cfg.active ? cfg.filter : 'none';
                  var count = 0;
                  for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    count++;
                    if (v.__xenosStyleSig === sig) continue;
                    v.__xenosStyleSig = sig;
                    v.style.setProperty('filter', filterCss, 'important');
                    v.style.setProperty('-webkit-filter', filterCss, 'important');
                    v.style.setProperty('object-fit', cfg.objectFit, 'important');
                    if (cfg.active && filterCss !== 'none') {
                      v.style.setProperty('will-change', 'filter', 'important');
                      v.style.setProperty('transform', 'translateZ(0)', 'important');
                    } else {
                      v.style.removeProperty('will-change');
                      v.style.removeProperty('transform');
                    }
                  }
                  return count;
                }

                function applyToShadowVideos(root, cfg, sig) {
                  if (!root || !root.querySelectorAll) return 0;
                  var found = applyToVideoList(root.querySelectorAll('video'), cfg, sig);
                  if (found > 0) return found;
                  var all = root.querySelectorAll('*');
                  for (var j = 0; j < all.length; j++) {
                    if (all[j].shadowRoot) {
                      found += applyToShadowVideos(all[j].shadowRoot, cfg, sig);
                    }
                  }
                  return found;
                }

                window.__xenosApplyVideoStyle = function() {
                  var cfg = window.__xenosVideoStyle || { active: false, useSvg: false, filter: 'none', objectFit: 'contain' };
                  var sig = (cfg.active ? '1|' : '0|') + cfg.filter + '|' + cfg.objectFit;
                  ensureSvgFilter(cfg);
                  ensureStyleSheet(cfg);
                  ensureFxOverlay(cfg);
                  // O(1) live HTMLCollection avoids expensive querySelectorAll('*') scans during streaming
                  var mainVids = document.getElementsByTagName('video');
                  if (mainVids && mainVids.length > 0) {
                    applyToVideoList(mainVids, cfg, sig);
                  } else {
                    applyToShadowVideos(document, cfg, sig);
                  }
                };

                window.__xenosApplyVideoStyle();

                if (!window.__xenosVideoObserver && document.documentElement) {
                  var applyTimer = null;
                  function scheduleApply() {
                    if (applyTimer) return;
                    applyTimer = setTimeout(function() {
                      applyTimer = null;
                      if (window.__xenosApplyVideoStyle) window.__xenosApplyVideoStyle();
                    }, 350);
                  }
                  window.__xenosVideoObserver = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                      var added = mutations[i].addedNodes;
                      if (added && added.length > 0) {
                        for (var k = 0; k < added.length; k++) {
                          if (added[k].nodeType === 1) {
                            scheduleApply();
                            return;
                          }
                        }
                      }
                    }
                  });
                  window.__xenosVideoObserver.observe(document.documentElement, { childList: true, subtree: true });
                  document.addEventListener('fullscreenchange', scheduleApply);
                  setInterval(function() {
                    var cfg = window.__xenosVideoStyle;
                    if (!cfg) return;
                    var mainVids = document.getElementsByTagName('video');
                    var sig = (cfg.active ? '1|' : '0|') + cfg.filter + '|' + cfg.objectFit;
                    if (mainVids && mainVids.length > 0 && mainVids[0].__xenosStyleSig === sig) {
                      return;
                    }
                    if (window.__xenosApplyVideoStyle) window.__xenosApplyVideoStyle();
                  }, 4000);
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Core bootstrap script injected on every page start/finish:
     * - Updates runtime config (region bypass headers, locale/timezone, virtual gamepad enabled)
     * - Installs Region Bypass fetch/XHR header injection for NVIDIA/GeForce NOW hosts
     * - Installs W3C standard Virtual Xbox 360 Gamepad API (17 buttons + 4 axes)
     * - Installs Direct Keyboard / Mouse / Wheel dispatcher
     * - Installs RTCPeerConnection.getStats() live telemetry reporter
     */
    fun buildBootstrapScript(
        config: WebStreamQualityConfig,
        physicalConfig: PhysicalControllerConfig = PhysicalControllerConfig()
    ): String {
        val region = config.regionPreset
        val regionEnabled = region.isEnabled
        val fakeIp = escapeJs(region.fakeIp)
        val acceptLang = escapeJs(region.acceptLanguage)
        val primaryLocale = escapeJs(region.acceptLanguage.substringBefore(',').ifBlank { "en-US" })
        val timeZone = escapeJs(region.timeZone)
        val spoofLocale = regionEnabled && config.spoofTimezoneAndLocale
        val virtualPadEnabled = config.enableVirtualGamepad
        val antiAfkEnabled = config.antiAfkEnabled
        val antiAfkIntervalMs = (config.antiAfkIntervalSec.coerceIn(15, 180)) * 1000
        val preferPhysical = physicalConfig.preferPhysicalSlotZero
        val swapFaceAB = physicalConfig.swapFaceButtonsAB
        val physDeadzone = String.format(Locale.US, "%.3f", physicalConfig.stickDeadzone.coerceIn(0f, 0.35f))

        return """
            (function() {
              'use strict';
              window.__xenosConfig = {
                regionEnabled: $regionEnabled,
                fakeIp: '$fakeIp',
                acceptLang: '$acceptLang',
                primaryLocale: '$primaryLocale',
                timeZone: '$timeZone',
                spoofLocale: $spoofLocale,
                virtualPadEnabled: $virtualPadEnabled,
                antiAfkEnabled: $antiAfkEnabled,
                antiAfkIntervalMs: $antiAfkIntervalMs,
                preferPhysicalSlotZero: $preferPhysical,
                swapFaceButtonsAB: $swapFaceAB,
                physStickDeadzone: $physDeadzone
              };

              if (window.__xenosBetterGfnInstalled) {
                if (window.__xenosPad && window.__xenosPad.syncConnectedState) {
                  window.__xenosPad.syncConnectedState();
                }
                if (window.__xenosAntiAfk && window.__xenosAntiAfk.sync) {
                  window.__xenosAntiAfk.sync();
                }
                return;
              }
              window.__xenosBetterGfnInstalled = true;

              // 1. Better xCloud-style Region Bypass (GeForce NOW + Xbox Cloud Gaming hosts)
              function isCloudBypassHost(urlStr) {
                try {
                  var u = new URL(urlStr, window.location.href);
                  var h = (u.hostname || '').toLowerCase();
                  return h.endsWith('geforcenow.com') ||
                    h.endsWith('nvidia.com') ||
                    h.endsWith('nvidiagrid.net') ||
                    h.endsWith('xbox.com') ||
                    h.endsWith('xboxlive.com') ||
                    h.endsWith('gamepass.com') ||
                    h.endsWith('xcloud.microsoft.com');
                } catch (e) {
                  return false;
                }
              }

              if (typeof window.fetch === 'function') {
                var origFetch = window.fetch.bind(window);
                window.fetch = function(input, init) {
                  try {
                    var cfg = window.__xenosConfig;
                    var urlStr = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                    if (cfg && cfg.regionEnabled && cfg.fakeIp && isCloudBypassHost(urlStr)) {
                      var nextInit = Object.assign({}, init || {});
                      var headers = new Headers(nextInit.headers || (input && input.headers) || {});
                      headers.set('X-Forwarded-For', cfg.fakeIp);
                      if (cfg.acceptLang) headers.set('Accept-Language', cfg.acceptLang);
                      nextInit.headers = headers;
                      return origFetch(input, nextInit);
                    }
                  } catch (e) {}
                  return origFetch(input, init);
                };
              }

              if (typeof XMLHttpRequest !== 'undefined') {
                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                  this.__xenosUrl = typeof url === 'string' ? url : '';
                  return origOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function() {
                  try {
                    var cfg = window.__xenosConfig;
                    if (cfg && cfg.regionEnabled && cfg.fakeIp && isCloudBypassHost(this.__xenosUrl || '')) {
                      this.setRequestHeader('X-Forwarded-For', cfg.fakeIp);
                      if (cfg.acceptLang) this.setRequestHeader('Accept-Language', cfg.acceptLang);
                    }
                  } catch (e) {}
                  return origSend.apply(this, arguments);
                };
              }

              // Optional Intl timezone & navigator.language override for regional GFN storefronts
              try {
                var cfg0 = window.__xenosConfig;
                if (cfg0.spoofLocale && cfg0.timeZone) {
                  var OrigDTF = Intl.DateTimeFormat;
                  if (OrigDTF && OrigDTF.prototype && OrigDTF.prototype.resolvedOptions) {
                    var origResolved = OrigDTF.prototype.resolvedOptions;
                    OrigDTF.prototype.resolvedOptions = function() {
                      var res = origResolved.apply(this, arguments);
                      if (window.__xenosConfig && window.__xenosConfig.spoofLocale && window.__xenosConfig.timeZone) {
                        res.timeZone = window.__xenosConfig.timeZone;
                      }
                      return res;
                    };
                  }
                  Object.defineProperty(navigator, 'language', {
                    get: function() { return (window.__xenosConfig && window.__xenosConfig.primaryLocale) || 'en-US'; },
                    configurable: true
                  });
                  Object.defineProperty(navigator, 'languages', {
                    get: function() { return [(window.__xenosConfig && window.__xenosConfig.primaryLocale) || 'en-US', 'en']; },
                    configurable: true
                  });
                }
              } catch (e) {}

              // 2. Standard W3C Virtual Xbox 360 Gamepad (17 buttons + 4 analog axes)
              // Matches Chromium's standard XInput Xbox 360 descriptor so GeForce NOW Web Play
              // binds Player 1 cleanly, returns fresh snapshots per poll, and replays
              // gamepadconnected whenever GFN attaches its session listener or starts a stream.
              var padButtons = [];
              for (var b = 0; b < 17; b++) {
                padButtons.push({ pressed: false, touched: false, value: 0.0 });
              }
              var padAxes = [0.0, 0.0, 0.0, 0.0];
              var padTimestamp = performance.now();
              var padSlotIndex = 0;
              var padStreamAnnounced = false;
              var padVibStub = {
                type: 'dual-rumble',
                playEffect: function() { return Promise.resolve('complete'); },
                reset: function() { return Promise.resolve('complete'); }
              };

              function snapshotVirtualPad(slotIndex) {
                var idx = (typeof slotIndex === 'number') ? slotIndex : padSlotIndex;
                var btnsCopy = new Array(padButtons.length);
                for (var i = 0; i < padButtons.length; i++) {
                  var src = padButtons[i];
                  btnsCopy[i] = { pressed: !!src.pressed, touched: !!src.touched, value: src.value };
                }
                return {
                  id: 'Xbox 360 Controller (XInput STANDARD GAMEPAD) (Vendor: 045e Product: 028e)',
                  index: idx,
                  connected: true,
                  timestamp: padTimestamp,
                  mapping: 'standard',
                  axes: [padAxes[0], padAxes[1], padAxes[2], padAxes[3]],
                  buttons: btnsCopy,
                  vibrationActuator: padVibStub,
                  hapticActuators: []
                };
              }

              var nativeProtoGetGamepads = (typeof Navigator !== 'undefined' && Navigator.prototype && typeof Navigator.prototype.getGamepads === 'function')
                ? Navigator.prototype.getGamepads
                : (typeof navigator.getGamepads === 'function' ? navigator.getGamepads : null);

              function readPhysicalGamepads(cfg) {
                var real = [];
                if (!nativeProtoGetGamepads) return real;
                try {
                  var raw = nativeProtoGetGamepads.call(navigator);
                  if (raw) {
                    for (var i = 0; i < raw.length; i++) {
                      if (raw[i] && raw[i].connected) {
                        real.push(wrapPhysicalGamepad(raw[i], cfg));
                      }
                    }
                  }
                } catch (e) {}
                return real;
              }

              function wrapPhysicalGamepad(gp, cfg) {
                if (!gp) return null;
                var dz = (cfg && typeof cfg.physStickDeadzone === 'number') ? cfg.physStickDeadzone : 0;
                var swap = !!(cfg && cfg.swapFaceButtonsAB);
                if (!swap && dz <= 0.005) return gp;
                try {
                  var axes = [];
                  for (var a = 0; a < (gp.axes ? gp.axes.length : 0); a++) {
                    var v = gp.axes[a] || 0;
                    axes.push(Math.abs(v) < dz ? 0 : v);
                  }
                  var btns = [];
                  for (var i = 0; i < (gp.buttons ? gp.buttons.length : 0); i++) {
                    btns.push(gp.buttons[i]);
                  }
                  if (swap && btns.length >= 4) {
                    var tmp0 = btns[0]; btns[0] = btns[1]; btns[1] = tmp0;
                    var tmp2 = btns[2]; btns[2] = btns[3]; btns[3] = tmp2;
                  }
                  return {
                    id: gp.id,
                    index: gp.index,
                    connected: gp.connected,
                    timestamp: gp.timestamp,
                    mapping: gp.mapping,
                    axes: axes,
                    buttons: btns,
                    vibrationActuator: gp.vibrationActuator || null,
                    hapticActuators: gp.hapticActuators || []
                  };
                } catch (e) {
                  return gp;
                }
              }

              function patchedGetGamepads() {
                var cfg = window.__xenosConfig || {};
                var real = readPhysicalGamepads(cfg);

                // If a real physical Bluetooth/USB gamepad is connected and prioritized,
                // keep it in slot 0 so GeForce NOW natively binds Player 1 to the hardware controller.
                if (real.length > 0 && cfg.preferPhysicalSlotZero) {
                  var out = [null, null, null, null];
                  for (var k = 0; k < real.length && k < 4; k++) {
                    out[k] = real[k];
                  }
                  if (cfg.virtualPadEnabled && real.length < 4) {
                    padSlotIndex = real.length;
                    out[real.length] = snapshotVirtualPad(padSlotIndex);
                  }
                  return out;
                }

                if (cfg.virtualPadEnabled) {
                  padSlotIndex = 0;
                  var result = [snapshotVirtualPad(0), null, null, null];
                  for (var j = 0; j < real.length && j < 3; j++) {
                    result[j + 1] = real[j];
                  }
                  return result;
                }
                return real;
              }

              try {
                if (typeof Navigator !== 'undefined' && Navigator.prototype) {
                  Object.defineProperty(Navigator.prototype, 'getGamepads', {
                    value: patchedGetGamepads,
                    writable: true,
                    configurable: true
                  });
                  if ('webkitGetGamepads' in Navigator.prototype) {
                    Object.defineProperty(Navigator.prototype, 'webkitGetGamepads', {
                      value: patchedGetGamepads,
                      writable: true,
                      configurable: true
                    });
                  }
                }
              } catch (e) {}
              try {
                Object.defineProperty(navigator, 'getGamepads', {
                  value: patchedGetGamepads,
                  writable: true,
                  configurable: true
                });
                if ('webkitGetGamepads' in navigator) {
                  Object.defineProperty(navigator, 'webkitGetGamepads', {
                    value: patchedGetGamepads,
                    writable: true,
                    configurable: true
                  });
                }
              } catch (e) {
                try { navigator.getGamepads = patchedGetGamepads; } catch (e2) {}
              }

              function buildGamepadConnectedEvent() {
                var padSnap = snapshotVirtualPad(padSlotIndex);
                try {
                  if (typeof GamepadEvent === 'function') {
                    return new GamepadEvent('gamepadconnected', { gamepad: padSnap });
                  }
                } catch (e) {}
                var ev = new Event('gamepadconnected');
                try {
                  Object.defineProperty(ev, 'gamepad', { value: padSnap, enumerable: true, configurable: true });
                } catch (e2) {
                  ev.gamepad = padSnap;
                }
                return ev;
              }

              var padDispatched = false;
              function notifyGamepadConnected(force) {
                if (!(window.__xenosConfig && window.__xenosConfig.virtualPadEnabled)) return;
                if (padDispatched && !force) return;
                padDispatched = true;
                try {
                  var ev = buildGamepadConnectedEvent();
                  window.dispatchEvent(ev);
                  if (typeof window.ongamepadconnected === 'function') {
                    window.ongamepadconnected(ev);
                  }
                } catch (e) {}
              }

              // Intercept late window.addEventListener('gamepadconnected', ...) so when GeForce NOW
              // boots its WebRTC game session well after page load, it immediately sees our virtual pad.
              try {
                var origWinAddEventListener = window.addEventListener;
                if (typeof origWinAddEventListener === 'function') {
                  window.addEventListener = function(type, listener, options) {
                    var res = origWinAddEventListener.apply(this, arguments);
                    if (type === 'gamepadconnected' && listener && window.__xenosConfig && window.__xenosConfig.virtualPadEnabled) {
                      setTimeout(function() {
                        try {
                          var ev = buildGamepadConnectedEvent();
                          if (typeof listener === 'function') {
                            listener.call(window, ev);
                          } else if (listener && typeof listener.handleEvent === 'function') {
                            listener.handleEvent(ev);
                          }
                        } catch (e) {}
                      }, 30);
                    }
                    return res;
                  };
                }
              } catch (e) {}

              function focusStreamForGamepad() {
                try {
                  var media = findStreamMediaElement(document);
                  if (media && typeof media.focus === 'function' && document.activeElement !== media) {
                    media.focus({ preventScroll: true });
                  }
                } catch (e) {}
              }

              window.__xenosPad = {
                syncConnectedState: function() {
                  if (window.__xenosConfig && window.__xenosConfig.virtualPadEnabled) {
                    notifyGamepadConnected(true);
                  }
                },
                setButton: function(idx, isDown) {
                  if (idx < 0 || idx >= padButtons.length) return;
                  if (!padStreamAnnounced && detectInGameStream()) {
                    padStreamAnnounced = true;
                    notifyGamepadConnected(true);
                    focusStreamForGamepad();
                  } else {
                    notifyGamepadConnected(false);
                  }
                  padButtons[idx] = { pressed: !!isDown, touched: !!isDown, value: isDown ? 1.0 : 0.0 };
                  padTimestamp = performance.now();
                },
                setAxes: function(isLeft, x, y) {
                  if (!padStreamAnnounced && detectInGameStream()) {
                    padStreamAnnounced = true;
                    notifyGamepadConnected(true);
                    focusStreamForGamepad();
                  } else {
                    notifyGamepadConnected(false);
                  }
                  var cx = Math.max(-1, Math.min(1, Number(x) || 0));
                  var cy = Math.max(-1, Math.min(1, Number(y) || 0));
                  if (isLeft) {
                    padAxes[0] = cx;
                    padAxes[1] = cy;
                  } else {
                    padAxes[2] = cx;
                    padAxes[3] = cy;
                  }
                  padTimestamp = performance.now();
                },
                reset: function() {
                  for (var i = 0; i < padButtons.length; i++) {
                    padButtons[i] = { pressed: false, touched: false, value: 0.0 };
                  }
                  padAxes[0] = 0; padAxes[1] = 0;
                  padAxes[2] = 0; padAxes[3] = 0;
                  padTimestamp = performance.now();
                }
              };
              setTimeout(function() { notifyGamepadConnected(false); }, 800);

              // 3. Direct Keyboard / Mouse / Wheel Dispatcher + Android WebView W3C Pointer Lock Polyfill
              var pointerX = Math.round(window.innerWidth / 2);
              var pointerY = Math.round(window.innerHeight / 2);
              var mouseButtonsMask = 0;
              var lastNativeMouseDownAt = [0, 0, 0];
              var lastNativeMouseUpAt = [0, 0, 0];
              var moveRemX = 0;
              var moveRemY = 0;

              // Virtual Pointer Lock state (Android WebView does not support native requestPointerLock;
              // play.geforcenow.com requires document.pointerLockElement + pointerlockchange + movementX/Y
              // to move the remote GeForce NOW mouse cursor in menus AND turn 3D game cameras 360 degrees!)
              var virtualLockEl = null;
              var lastRequestedLockEl = null;
              var activationClickSent = false;
              var lastReportedInGame = null;
              var lastRtcVideoActive = false;

              function isNodeConnected(el) {
                return !!(el && (el.isConnected === true || (document.contains && document.contains(el))));
              }

              function findActiveStreamVideo(root) {
                if (!root || !root.querySelectorAll) return null;
                var vids = root.querySelectorAll('video');
                for (var i = 0; i < vids.length; i++) {
                  var v = vids[i];
                  try {
                    var rect = v.getBoundingClientRect();
                    var bigEnough = rect.width >= window.innerWidth * 0.45 && rect.height >= window.innerHeight * 0.45;
                    var hasStream = !!(v.srcObject) || (v.videoWidth >= 320 && v.videoHeight >= 180 && v.readyState >= 2);
                    if (bigEnough && hasStream) return v;
                  } catch (e) {}
                }
                var all = root.querySelectorAll('*');
                for (var j = 0; j < all.length; j++) {
                  if (all[j].shadowRoot) {
                    var nested = findActiveStreamVideo(all[j].shadowRoot);
                    if (nested) return nested;
                  }
                }
                return null;
              }

              function findStreamMediaElement(root) {
                var activeVid = findActiveStreamVideo(root);
                if (activeVid) return activeVid;
                if (!root || !root.querySelector) return null;
                var direct = root.querySelector('video') || root.querySelector('canvas');
                if (direct) return direct;
                var all = root.querySelectorAll('*');
                for (var i = 0; i < all.length; i++) {
                  if (all[i].shadowRoot) {
                    var found = findStreamMediaElement(all[i].shadowRoot);
                    if (found) return found;
                  }
                }
                return null;
              }

              function findStreamLockHost() {
                if (isNodeConnected(lastRequestedLockEl)) return lastRequestedLockEl;
                var vid = findActiveStreamVideo(document) || findStreamMediaElement(document);
                if (vid) return vid;
                return document.body || document.documentElement;
              }

              function detectInGameStream() {
                if (findActiveStreamVideo(document)) return true;
                return !!lastRtcVideoActive;
              }

              function firePointerLockChange() {
                try {
                  var ev = new Event('pointerlockchange', { bubbles: true, cancelable: false });
                  document.dispatchEvent(ev);
                  if (typeof document.onpointerlockchange === 'function') {
                    document.onpointerlockchange(ev);
                  }
                } catch (e) {}
              }

              function acquireVirtualPointerLock(el) {
                var target = (el && isNodeConnected(el)) ? el : findStreamLockHost();
                if (!target) return null;
                lastRequestedLockEl = target;
                if (virtualLockEl !== target) {
                  virtualLockEl = target;
                  try { if (typeof target.focus === 'function') target.focus({ preventScroll: true }); } catch (e) {}
                  firePointerLockChange();
                }
                return virtualLockEl;
              }

              function releaseVirtualPointerLock() {
                if (virtualLockEl !== null) {
                  virtualLockEl = null;
                  firePointerLockChange();
                }
              }

              function reportStreamState(force) {
                try {
                  var inGame = detectInGameStream();
                  if (!inGame) {
                    activationClickSent = false;
                    padStreamAnnounced = false;
                    releaseVirtualPointerLock();
                  }
                  if (force || inGame !== lastReportedInGame) {
                    var enteringGame = inGame && !lastReportedInGame;
                    lastReportedInGame = inGame;
                    if (enteringGame && window.__xenosConfig && window.__xenosConfig.virtualPadEnabled) {
                      padStreamAnnounced = true;
                      notifyGamepadConnected(true);
                      setTimeout(function() { notifyGamepadConnected(true); }, 650);
                    }
                    if (window.XenosWebBridge && window.XenosWebBridge.onStreamStateChanged) {
                      window.XenosWebBridge.onStreamStateChanged(inGame);
                    }
                  }
                } catch (e) {}
              }

              try {
                var lockDescriptor = {
                  get: function() {
                    if (virtualLockEl && !isNodeConnected(virtualLockEl)) {
                      virtualLockEl = null;
                    }
                    return virtualLockEl;
                  },
                  configurable: true
                };
                Object.defineProperty(Document.prototype, 'pointerLockElement', lockDescriptor);
                Object.defineProperty(document, 'pointerLockElement', lockDescriptor);
                Object.defineProperty(document, 'webkitPointerLockElement', lockDescriptor);
                Object.defineProperty(document, 'mozPointerLockElement', lockDescriptor);
              } catch (e) {}

              try {
                Element.prototype.requestPointerLock = function() {
                  acquireVirtualPointerLock(this);
                  reportStreamState(false);
                  return Promise.resolve();
                };
                if ('webkitRequestPointerLock' in Element.prototype) {
                  Element.prototype.webkitRequestPointerLock = Element.prototype.requestPointerLock;
                }
                Document.prototype.exitPointerLock = function() {
                  releaseVirtualPointerLock();
                };
                document.exitPointerLock = function() {
                  releaseVirtualPointerLock();
                };
              } catch (e) {}

              setInterval(function() { reportStreamState(false); }, 350);

              window.addEventListener('mousedown', function(e) {
                if (e && e.isTrusted && e.button >= 0 && e.button <= 2) {
                  lastNativeMouseDownAt[e.button] = performance.now();
                }
              }, true);
              window.addEventListener('mouseup', function(e) {
                if (e && e.isTrusted && e.button >= 0 && e.button <= 2) {
                  lastNativeMouseUpAt[e.button] = performance.now();
                }
              }, true);

              function streamTarget() {
                if (virtualLockEl && isNodeConnected(virtualLockEl)) return virtualLockEl;
                return findStreamLockHost() || document.activeElement || document.body || document;
              }

              function deepElementFromPoint(root, x, y) {
                try {
                  if (!root || typeof root.elementFromPoint !== 'function') return null;
                  var el = root.elementFromPoint(x, y);
                  var guard = 0;
                  while (el && el.shadowRoot && guard < 8) {
                    var inner = el.shadowRoot.elementFromPoint(x, y);
                    if (!inner || inner === el) break;
                    el = inner;
                    guard++;
                  }
                  return el;
                } catch (e) {
                  return null;
                }
              }

              function pointTarget(x, y) {
                if (virtualLockEl && isNodeConnected(virtualLockEl)) return virtualLockEl;
                var hit = deepElementFromPoint(document, x, y);
                if (hit) return hit;
                return streamTarget();
              }

              function centerCoordsFor(el) {
                try {
                  if (el && typeof el.getBoundingClientRect === 'function') {
                    var r = el.getBoundingClientRect();
                    if (r.width > 10 && r.height > 10) {
                      return {
                        x: Math.round(r.left + r.width / 2),
                        y: Math.round(r.top + r.height / 2)
                      };
                    }
                  }
                } catch (e) {}
                return {
                  x: Math.round(window.innerWidth / 2),
                  y: Math.round(window.innerHeight / 2)
                };
              }

              function ensureGamePointerLock() {
                if (virtualLockEl && isNodeConnected(virtualLockEl)) {
                  return virtualLockEl;
                }
                if (isNodeConnected(lastRequestedLockEl)) {
                  return acquireVirtualPointerLock(lastRequestedLockEl);
                }
                var host = findStreamLockHost();
                if (host && !activationClickSent) {
                  activationClickSent = true;
                  try {
                    if (typeof host.focus === 'function') host.focus({ preventScroll: true });
                    var c = centerCoordsFor(host);
                    var clickOpts = {
                      button: 0,
                      buttons: 0,
                      detail: 1,
                      clientX: c.x,
                      clientY: c.y,
                      screenX: c.x,
                      screenY: c.y,
                      view: window,
                      bubbles: true,
                      cancelable: true,
                      composed: true
                    };
                    // Trigger GFN's own "click stream to requestPointerLock()" listener once if waiting
                    host.dispatchEvent(new MouseEvent('click', clickOpts));
                  } catch (e) {}
                }
                if (virtualLockEl && isNodeConnected(virtualLockEl)) {
                  return virtualLockEl;
                }
                return acquireVirtualPointerLock(host);
              }

              function forceMovementProps(ev, intDx, intDy) {
                try {
                  if (ev.movementX !== intDx) {
                    Object.defineProperty(ev, 'movementX', { value: intDx, configurable: true });
                  }
                  if (ev.movementY !== intDy) {
                    Object.defineProperty(ev, 'movementY', { value: intDy, configurable: true });
                  }
                } catch (e) {}
                return ev;
              }

              function emitLockedMoveStep(t, c, stepX, stepY) {
                var opts = {
                  movementX: stepX,
                  movementY: stepY,
                  clientX: c.x,
                  clientY: c.y,
                  pageX: c.x + (window.scrollX || 0),
                  pageY: c.y + (window.scrollY || 0),
                  screenX: c.x,
                  screenY: c.y,
                  buttons: mouseButtonsMask,
                  view: window,
                  bubbles: true,
                  cancelable: true,
                  composed: true
                };
                if (typeof PointerEvent !== 'undefined') {
                  var pOpts = Object.assign({
                    pointerType: 'mouse',
                    isPrimary: true,
                    pointerId: 1,
                    width: 1,
                    height: 1,
                    pressure: mouseButtonsMask ? 0.5 : 0
                  }, opts);
                  try {
                    t.dispatchEvent(forceMovementProps(new PointerEvent('pointerrawupdate', pOpts), stepX, stepY));
                  } catch (e) {}
                  t.dispatchEvent(forceMovementProps(new PointerEvent('pointermove', pOpts), stepX, stepY));
                }
                t.dispatchEvent(forceMovementProps(new MouseEvent('mousemove', opts), stepX, stepY));
              }

              function dispatchLockedRelativeMove(mx, my) {
                var t = ensureGamePointerLock() || streamTarget();
                moveRemX += mx;
                moveRemY += my;
                var intDx = moveRemX > 0 ? Math.floor(moveRemX) : Math.ceil(moveRemX);
                var intDy = moveRemY > 0 ? Math.floor(moveRemY) : Math.ceil(moveRemY);
                if (intDx === 0 && intDy === 0) return;
                moveRemX -= intDx;
                moveRemY -= intDy;

                var c = centerCoordsFor(t);
                pointerX = c.x;
                pointerY = c.y;

                // Sub-step large high-sensitivity deltas (max 42px per event) so WebRTC/GFN
                // glitch filters never clamp or drop fast 180°-360° camera turns.
                var maxStep = 42;
                var steps = Math.max(1, Math.min(16, Math.ceil(Math.max(Math.abs(intDx), Math.abs(intDy)) / maxStep)));
                var sentX = 0;
                var sentY = 0;
                for (var s = 1; s <= steps; s++) {
                  var targetX = Math.round((intDx * s) / steps);
                  var targetY = Math.round((intDy * s) / steps);
                  var stepX = targetX - sentX;
                  var stepY = targetY - sentY;
                  if (stepX !== 0 || stepY !== 0) {
                    emitLockedMoveStep(t, c, stepX, stepY);
                    sentX += stepX;
                    sentY += stepY;
                  }
                }
              }

              function dispatchRelativeMove(mx, my) {
                var t = pointTarget(pointerX, pointerY);
                moveRemX += mx;
                moveRemY += my;
                var intDx = moveRemX > 0 ? Math.floor(moveRemX) : Math.ceil(moveRemX);
                var intDy = moveRemY > 0 ? Math.floor(moveRemY) : Math.ceil(moveRemY);
                if (intDx === 0 && intDy === 0) return;
                moveRemX -= intDx;
                moveRemY -= intDy;

                var maxStep = 42;
                var steps = Math.max(1, Math.min(16, Math.ceil(Math.max(Math.abs(intDx), Math.abs(intDy)) / maxStep)));
                var sentX = 0;
                var sentY = 0;
                for (var s = 1; s <= steps; s++) {
                  var targetX = Math.round((intDx * s) / steps);
                  var targetY = Math.round((intDy * s) / steps);
                  var stepX = targetX - sentX;
                  var stepY = targetY - sentY;
                  if (stepX === 0 && stepY === 0) continue;
                  var opts = {
                    movementX: stepX,
                    movementY: stepY,
                    clientX: pointerX,
                    clientY: pointerY,
                    pageX: pointerX + (window.scrollX || 0),
                    pageY: pointerY + (window.scrollY || 0),
                    screenX: pointerX,
                    screenY: pointerY,
                    buttons: mouseButtonsMask,
                    view: window,
                    bubbles: true,
                    cancelable: true,
                    composed: true
                  };
                  if (typeof PointerEvent !== 'undefined') {
                    t.dispatchEvent(forceMovementProps(new PointerEvent('pointermove', Object.assign({ pointerType: 'mouse', isPrimary: true, pointerId: 1, width: 1, height: 1, pressure: mouseButtonsMask ? 0.5 : 0 }, opts)), stepX, stepY));
                  }
                  t.dispatchEvent(forceMovementProps(new MouseEvent('mousemove', opts), stepX, stepY));
                  sentX += stepX;
                  sentY += stepY;
                }
              }

              function dispatchLockedMouseButton(button, isDown) {
                var t = ensureGamePointerLock() || streamTarget();
                var c = centerCoordsFor(t);
                pointerX = c.x;
                pointerY = c.y;
                var type = isDown ? 'mousedown' : 'mouseup';
                var pType = isDown ? 'pointerdown' : 'pointerup';
                var opts = {
                  button: button,
                  buttons: mouseButtonsMask,
                  detail: 1,
                  clientX: c.x,
                  clientY: c.y,
                  pageX: c.x + (window.scrollX || 0),
                  pageY: c.y + (window.scrollY || 0),
                  screenX: c.x,
                  screenY: c.y,
                  view: window,
                  bubbles: true,
                  cancelable: true,
                  composed: true
                };
                if (isDown && t && typeof t.focus === 'function') {
                  try { t.focus({ preventScroll: true }); } catch (e) {}
                }
                if (typeof PointerEvent !== 'undefined') {
                  t.dispatchEvent(new PointerEvent(pType, Object.assign({
                    pointerType: 'mouse',
                    isPrimary: true,
                    pointerId: 1,
                    width: 1,
                    height: 1,
                    pressure: isDown ? 0.5 : 0
                  }, opts)));
                }
                t.dispatchEvent(new MouseEvent(type, opts));
              }

              function dispatchSyntheticMouseButton(button, isDown) {
                var t = pointTarget(pointerX, pointerY);
                var type = isDown ? 'mousedown' : 'mouseup';
                var pType = isDown ? 'pointerdown' : 'pointerup';
                var opts = {
                  button: button,
                  buttons: mouseButtonsMask,
                  detail: 1,
                  clientX: pointerX,
                  clientY: pointerY,
                  pageX: pointerX + (window.scrollX || 0),
                  pageY: pointerY + (window.scrollY || 0),
                  screenX: pointerX,
                  screenY: pointerY,
                  view: window,
                  bubbles: true,
                  cancelable: true,
                  composed: true
                };
                if (isDown && t && typeof t.focus === 'function') {
                  try { t.focus({ preventScroll: true }); } catch (e) {}
                }
                if (typeof PointerEvent !== 'undefined') {
                  t.dispatchEvent(new PointerEvent(pType, Object.assign({ pointerType: 'mouse', isPrimary: true, pointerId: 1, width: 1, height: 1, pressure: isDown ? 0.5 : 0 }, opts)));
                }
                t.dispatchEvent(new MouseEvent(type, opts));
                var media = findStreamMediaElement(document);
                if (media && media !== t && !media.contains(t) && !t.contains(media)) {
                  if (typeof PointerEvent !== 'undefined') {
                    media.dispatchEvent(new PointerEvent(pType, Object.assign({ pointerType: 'mouse', isPrimary: true, pointerId: 1, width: 1, height: 1, pressure: isDown ? 0.5 : 0 }, opts)));
                  }
                  media.dispatchEvent(new MouseEvent(type, opts));
                }
                if (!isDown && !document.pointerLockElement) {
                  if (button === 0) {
                    t.dispatchEvent(new MouseEvent('click', opts));
                  } else if (button === 2) {
                    t.dispatchEvent(new MouseEvent('contextmenu', opts));
                  } else if (button === 1) {
                    t.dispatchEvent(new MouseEvent('auxclick', opts));
                  }
                }
              }

              window.__xenosInput = {
                key: function(code, key, keyCode, isDown, shift, ctrl, alt) {
                  try {
                    var t = streamTarget();
                    var type = isDown ? 'keydown' : 'keyup';
                    var ev = new KeyboardEvent(type, {
                      code: code,
                      key: key,
                      keyCode: keyCode,
                      which: keyCode,
                      bubbles: true,
                      cancelable: true,
                      composed: true,
                      shiftKey: !!shift,
                      ctrlKey: !!ctrl,
                      altKey: !!alt
                    });
                    t.dispatchEvent(ev);
                    if (t !== window) window.dispatchEvent(ev);
                  } catch (e) {}
                },
                gameMouseMove: function(dx, dy) {
                  try {
                    var mx = Number(dx) || 0;
                    var my = Number(dy) || 0;
                    dispatchLockedRelativeMove(mx, my);
                  } catch (e) {}
                },
                gameMouseButton: function(button, isDown) {
                  try {
                    var bit = button === 0 ? 1 : (button === 2 ? 2 : 4);
                    if (isDown) mouseButtonsMask |= bit; else mouseButtonsMask &= ~bit;
                    dispatchLockedMouseButton(button, isDown);
                  } catch (e) {}
                },
                gameMouseWheel: function(deltaY) {
                  try {
                    var t = ensureGamePointerLock() || streamTarget();
                    var c = centerCoordsFor(t);
                    t.dispatchEvent(new WheelEvent('wheel', {
                      deltaY: Number(deltaY) || 0,
                      deltaMode: 0,
                      clientX: c.x,
                      clientY: c.y,
                      bubbles: true,
                      cancelable: true,
                      composed: true
                    }));
                  } catch (e) {}
                },
                releasePointerLock: function() {
                  try { releaseVirtualPointerLock(); } catch (e) {}
                },
                mouseButton: function(button, isDown, cssX, cssY) {
                  try {
                    if (typeof cssX === 'number' && !isNaN(cssX)) {
                      pointerX = Math.max(0, Math.min(window.innerWidth, cssX));
                    }
                    if (typeof cssY === 'number' && !isNaN(cssY)) {
                      pointerY = Math.max(0, Math.min(window.innerHeight, cssY));
                    }
                    var bit = button === 0 ? 1 : (button === 2 ? 2 : 4);
                    if (isDown) mouseButtonsMask |= bit; else mouseButtonsMask &= ~bit;
                    dispatchSyntheticMouseButton(button, isDown);
                  } catch (e) {}
                },
                ensureMouseButton: function(button, isDown, cssX, cssY) {
                  try {
                    if (typeof cssX === 'number' && !isNaN(cssX)) {
                      pointerX = Math.max(0, Math.min(window.innerWidth, cssX));
                    }
                    if (typeof cssY === 'number' && !isNaN(cssY)) {
                      pointerY = Math.max(0, Math.min(window.innerHeight, cssY));
                    }
                    var bit = button === 0 ? 1 : (button === 2 ? 2 : 4);
                    if (isDown) mouseButtonsMask |= bit; else mouseButtonsMask &= ~bit;
                    var now = performance.now();
                    var lastTs = isDown ? (lastNativeMouseDownAt[button] || 0) : (lastNativeMouseUpAt[button] || 0);
                    if (now - lastTs < 85 && !document.pointerLockElement) {
                      return;
                    }
                    dispatchSyntheticMouseButton(button, isDown);
                  } catch (e) {}
                },
                mouseMove: function(dx, dy) {
                  try {
                    var mx = Number(dx) || 0;
                    var my = Number(dy) || 0;
                    if (detectInGameStream() || virtualLockEl) {
                      dispatchLockedRelativeMove(mx, my);
                    } else {
                      pointerX = Math.max(0, Math.min(window.innerWidth, pointerX + mx));
                      pointerY = Math.max(0, Math.min(window.innerHeight, pointerY + my));
                      dispatchRelativeMove(mx, my);
                    }
                  } catch (e) {}
                },
                syncPointer: function(cssX, cssY, dx, dy, forceRelative) {
                  try {
                    pointerX = Math.max(0, Math.min(window.innerWidth, Number(cssX) || 0));
                    pointerY = Math.max(0, Math.min(window.innerHeight, Number(cssY) || 0));
                    if (document.pointerLockElement || forceRelative || mouseButtonsMask !== 0) {
                      dispatchRelativeMove(Number(dx) || 0, Number(dy) || 0);
                    }
                  } catch (e) {}
                },
                wheel: function(deltaY) {
                  try {
                    var t = pointTarget(pointerX, pointerY);
                    t.dispatchEvent(new WheelEvent('wheel', {
                      deltaY: Number(deltaY) || 0,
                      deltaMode: 0,
                      clientX: pointerX,
                      clientY: pointerY,
                      bubbles: true,
                      cancelable: true,
                      composed: true
                    }));
                  } catch (e) {}
                },
                reset: function() {
                  mouseButtonsMask = 0;
                  moveRemX = 0;
                  moveRemY = 0;
                  if (window.__xenosPad) window.__xenosPad.reset();
                }
              };

              // 4. Real RTCPeerConnection.getStats() Live Telemetry Reporter
              if (typeof window.RTCPeerConnection === 'function') {
                var trackedPcs = [];
                var OrigPC = window.RTCPeerConnection;
                var WrappedPC = function() {
                  var pc = new (Function.prototype.bind.apply(OrigPC, [null].concat(Array.prototype.slice.call(arguments))))();
                  trackedPcs.push(pc);
                  return pc;
                };
                WrappedPC.prototype = OrigPC.prototype;
                Object.setPrototypeOf(WrappedPC, OrigPC);
                window.RTCPeerConnection = WrappedPC;

                var prevBytes = 0;
                var prevTs = 0;
                var prevDecodeSum = 0;
                var prevFramesDecoded = 0;

                setInterval(function() {
                  try {
                    var activePc = null;
                    for (var i = trackedPcs.length - 1; i >= 0; i--) {
                      var candidate = trackedPcs[i];
                      if (candidate && candidate.connectionState !== 'closed') {
                        activePc = candidate;
                        break;
                      }
                    }
                    if (!activePc || !window.XenosWebBridge || !window.XenosWebBridge.onWebRtcStats) {
                      lastRtcVideoActive = false;
                      reportStreamState(false);
                      return;
                    }
                    activePc.getStats().then(function(report) {
                      var fps = 0, rttMs = 0, bitrateMbps = 0, lossPct = 0, jitterMs = 0, decodeMs = 0;
                      var res = '', codec = '';
                      var codecs = {};
                      var videoIn = null;
                      var pair = null;
                      report.forEach(function(stat) {
                        if (stat.type === 'codec' && stat.mimeType) {
                          codecs[stat.id] = stat.mimeType.replace('video/', '').toUpperCase();
                        } else if (stat.type === 'inbound-rtp' && (stat.kind === 'video' || stat.mediaType === 'video')) {
                          videoIn = stat;
                        } else if (stat.type === 'candidate-pair' && (stat.nominated || stat.state === 'succeeded')) {
                          if (!pair || stat.nominated) pair = stat;
                        }
                      });
                      if (videoIn) {
                        fps = Math.round(videoIn.framesPerSecond || 0);
                        if (videoIn.frameWidth && videoIn.frameHeight) {
                          res = videoIn.frameWidth + 'x' + videoIn.frameHeight;
                        }
                        if (videoIn.codecId && codecs[videoIn.codecId]) {
                          codec = codecs[videoIn.codecId];
                        }
                        var totalPkts = (videoIn.packetsReceived || 0) + (videoIn.packetsLost || 0);
                        if (totalPkts > 0) {
                          lossPct = Math.round(((videoIn.packetsLost || 0) * 10000) / totalPkts) / 100;
                        }
                        jitterMs = Math.round((videoIn.jitter || 0) * 10000) / 10;
                        if (videoIn.bytesReceived && videoIn.timestamp) {
                          if (prevTs > 0 && videoIn.timestamp > prevTs) {
                            var dt = (videoIn.timestamp - prevTs) / 1000;
                            var dBytes = Math.max(0, videoIn.bytesReceived - prevBytes);
                            bitrateMbps = Math.round(((dBytes * 8) / (dt * 1000000)) * 10) / 10;
                          }
                          prevBytes = videoIn.bytesReceived;
                          prevTs = videoIn.timestamp;
                        }
                        if (videoIn.totalDecodeTime && videoIn.framesDecoded) {
                          var dFrames = videoIn.framesDecoded - prevFramesDecoded;
                          var dTime = videoIn.totalDecodeTime - prevDecodeSum;
                          if (dFrames > 0 && dTime >= 0) {
                            decodeMs = Math.round((dTime / dFrames) * 10000) / 10;
                          }
                          prevFramesDecoded = videoIn.framesDecoded;
                          prevDecodeSum = videoIn.totalDecodeTime;
                        }
                      }
                      if (pair && typeof pair.currentRoundTripTime === 'number') {
                        rttMs = Math.round(pair.currentRoundTripTime * 1000);
                      }
                      var isConnected = activePc.connectionState === 'connected' || fps > 0;
                      lastRtcVideoActive = isConnected && (fps > 0 || !!res);
                      reportStreamState(false);
                      var payload = JSON.stringify({
                        fps: fps,
                        rttMs: rttMs,
                        bitrateMbps: bitrateMbps,
                        packetLossPercent: lossPct,
                        jitterMs: jitterMs,
                        decodeMs: decodeMs,
                        resolution: res,
                        codec: codec,
                        connected: isConnected
                      });
                      window.XenosWebBridge.onWebRtcStats(payload);
                    }).catch(function() {});
                  } catch (e) {}
                }, 1000);
              }

              // 5. Anti-AFK / Queue Keeper Heartbeat
              var lastUserActivityMs = performance.now();
              var antiAfkTimer = null;
              var antiAfkPulseCount = 0;
              function markUserActive() { lastUserActivityMs = performance.now(); }
              ['keydown', 'mousedown', 'pointerdown', 'touchstart'].forEach(function(evtName) {
                window.addEventListener(evtName, function(e) {
                  if (e && e.isTrusted) markUserActive();
                }, true);
              });

              function sendAntiAfkPulse() {
                var cfg = window.__xenosConfig;
                if (!cfg || !cfg.antiAfkEnabled) return;
                if (performance.now() - lastUserActivityMs < 16000) return;
                antiAfkPulseCount++;
                try {
                  if (detectInGameStream() || virtualLockEl) {
                    // Subtle zero-net-movement camera nudge (+1px then -1px) keeps GFN AFK timer reset
                    dispatchLockedRelativeMove(1, 0);
                    setTimeout(function() {
                      try { dispatchLockedRelativeMove(-1, 0); } catch (e) {}
                    }, 90);
                  } else {
                    // Lobby / Free-tier queue keep-alive tick
                    var t = document.body || document.documentElement;
                    if (t) {
                      t.dispatchEvent(new MouseEvent('mousemove', {
                        clientX: Math.round(window.innerWidth / 2) + (antiAfkPulseCount % 2),
                        clientY: Math.round(window.innerHeight / 2),
                        bubbles: true
                      }));
                    }
                  }
                } catch (e) {}
              }

              function syncAntiAfkTimer() {
                if (antiAfkTimer) {
                  clearInterval(antiAfkTimer);
                  antiAfkTimer = null;
                }
                var cfg = window.__xenosConfig;
                if (cfg && cfg.antiAfkEnabled) {
                  var intervalMs = Math.max(15000, Math.min(180000, Number(cfg.antiAfkIntervalMs) || 45000));
                  antiAfkTimer = setInterval(sendAntiAfkPulse, intervalMs);
                }
              }

              window.__xenosAntiAfk = {
                sync: syncAntiAfkTimer,
                pulseNow: sendAntiAfkPulse,
                getPulseCount: function() { return antiAfkPulseCount; }
              };
              syncAntiAfkTimer();
            })();
        """.trimIndent()
    }

    fun buildGamepadButtonJs(buttonIndex: Int, isDown: Boolean): String =
        "window.__xenosPad && window.__xenosPad.setButton($buttonIndex, $isDown);"

    fun buildAnalogStickJs(isLeft: Boolean, x: Float, y: Float): String {
        val sx = String.format(Locale.US, "%.4f", x.coerceIn(-1f, 1f))
        val sy = String.format(Locale.US, "%.4f", y.coerceIn(-1f, 1f))
        return "window.__xenosPad && window.__xenosPad.setAxes($isLeft, $sx, $sy);"
    }

    fun buildKeyboardJs(androidKeyCode: Int, metaState: Int, isDown: Boolean): String {
        val mapping = mapKeyCodeToDomKey(androidKeyCode)
        val shift = (metaState and KeyEvent.META_SHIFT_ON) != 0
        val ctrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        val alt = (metaState and KeyEvent.META_ALT_ON) != 0
        return "window.__xenosInput && window.__xenosInput.key('${escapeJs(mapping.code)}', '${escapeJs(mapping.key)}', ${mapping.keyCode}, $isDown, $shift, $ctrl, $alt);"
    }

    fun buildMouseButtonJs(button: Int, isDown: Boolean): String =
        "window.__xenosInput && window.__xenosInput.mouseButton(${button.coerceIn(0, 2)}, $isDown);"

    fun buildEnsureMouseButtonJs(
        button: Int,
        isDown: Boolean,
        cssX: Float,
        cssY: Float
    ): String {
        val sx = String.format(Locale.US, "%.2f", cssX)
        val sy = String.format(Locale.US, "%.2f", cssY)
        return "window.__xenosInput && window.__xenosInput.ensureMouseButton(${button.coerceIn(0, 2)}, $isDown, $sx, $sy);"
    }

    fun buildMouseMoveJs(dxPx: Float, dyPx: Float): String {
        val sdx = String.format(Locale.US, "%.2f", dxPx)
        val sdy = String.format(Locale.US, "%.2f", dyPx)
        return "window.__xenosInput && window.__xenosInput.mouseMove($sdx, $sdy);"
    }

    fun buildSyncMouseMoveJs(
        cssX: Float,
        cssY: Float,
        dxCss: Float,
        dyCss: Float,
        forceRelative: Boolean
    ): String {
        val sx = String.format(Locale.US, "%.2f", cssX)
        val sy = String.format(Locale.US, "%.2f", cssY)
        val sdx = String.format(Locale.US, "%.2f", dxCss)
        val sdy = String.format(Locale.US, "%.2f", dyCss)
        return "window.__xenosInput && window.__xenosInput.syncPointer($sx, $sy, $sdx, $sdy, $forceRelative);"
    }

    fun buildGameMouseMoveJs(dxCss: Float, dyCss: Float): String {
        val sdx = String.format(Locale.US, "%.2f", dxCss)
        val sdy = String.format(Locale.US, "%.2f", dyCss)
        return "window.__xenosInput && window.__xenosInput.gameMouseMove($sdx, $sdy);"
    }

    fun buildGameMouseButtonJs(button: Int, isDown: Boolean): String =
        "window.__xenosInput && window.__xenosInput.gameMouseButton(${button.coerceIn(0, 2)}, $isDown);"

    fun buildGameMouseWheelJs(scrollUp: Boolean): String {
        val delta = if (scrollUp) -120 else 120
        return "window.__xenosInput && window.__xenosInput.gameMouseWheel($delta);"
    }

    fun buildReleasePointerLockJs(): String =
        "window.__xenosInput && window.__xenosInput.releasePointerLock();"

    fun buildMouseWheelJs(scrollUp: Boolean): String {
        val delta = if (scrollUp) -120 else 120
        return "window.__xenosInput && window.__xenosInput.wheel($delta);"
    }

    fun buildResetAllInputsJs(): String =
        "window.__xenosInput && window.__xenosInput.reset();"

    /**
     * Parses the JSON object posted by window.XenosWebBridge.onWebRtcStats(...) without throwing.
     */
    fun parseWebRtcStatsJson(rawJson: String, nowMs: Long = System.currentTimeMillis()): WebRtcStreamStats {
        if (rawJson.isBlank()) return WebRtcStreamStats()
        fun extractNum(key: String): Double {
            val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
            return regex.find(rawJson)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
        }
        fun extractStr(key: String): String {
            val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            return regex.find(rawJson)?.groupValues?.getOrNull(1) ?: ""
        }
        fun extractBool(key: String): Boolean {
            val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
            return regex.find(rawJson)?.groupValues?.getOrNull(1) == "true"
        }

        return WebRtcStreamStats(
            fps = extractNum("fps").roundToInt().coerceIn(0, 240),
            rttMs = extractNum("rttMs").roundToInt().coerceAtLeast(0),
            bitrateMbps = extractNum("bitrateMbps").toFloat().coerceAtLeast(0f),
            packetLossPercent = extractNum("packetLossPercent").toFloat().coerceIn(0f, 100f),
            jitterMs = extractNum("jitterMs").toFloat().coerceAtLeast(0f),
            decodeMs = extractNum("decodeMs").toFloat().coerceAtLeast(0f),
            resolution = extractStr("resolution"),
            codec = extractStr("codec"),
            connected = extractBool("connected"),
            updatedAtMs = nowMs
        )
    }

    private fun escapeJs(input: String): String =
        input.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
}
