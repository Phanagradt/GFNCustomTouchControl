package com.example.webplayer

import android.view.KeyEvent
import com.example.model.ColorMatrixBuilder
import com.example.model.ColorblindMode
import com.example.model.GameFilterStyle
import com.example.model.GfnRegionPreset
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

    fun isTrustedGfnOrigin(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase(Locale.US)
        return lower.startsWith("https://") && (
            lower.contains("geforcenow.com") ||
                lower.contains("nvidia.com") ||
                lower.contains("nvidiagrid.net")
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
     * Converts an active XenosPad GameFilterStyle into a compact CSS filter chain
     * used as fast fallback or paired with the Better xCloud-style SVG filter pipeline.
     */
    fun buildCssFilterString(style: GameFilterStyle?): String {
        if (style == null) return "none"
        val parts = ArrayList<String>()

        if (style.brightnessContrastEnabled) {
            val brightness = (1f + style.effectiveExposure * 0.65f + style.shadows * 0.22f + style.gamma * 0.18f)
                .coerceIn(0.35f, 2.2f)
            val contrast = (1f + style.contrast * 0.85f +
                (if (style.detailsEnabled) style.detailsClarity * 0.22f else 0f) +
                (if (style.sharpenEnabled) style.sharpenAmount * 0.14f else 0f) +
                (if (style.sharpenPlusEnabled) style.sharpenPlusIntensity * 0.18f else 0f)
                ).coerceIn(0.45f, 2.3f)

            if (abs(brightness - 1f) > 0.015f) {
                parts += String.format(Locale.US, "brightness(%.2f)", brightness)
            }
            if (abs(contrast - 1f) > 0.015f) {
                parts += String.format(Locale.US, "contrast(%.2f)", contrast)
            }
        }

        if (style.colorEnabled) {
            val saturate = (1f + style.colorVibrance * 0.95f +
                (if (style.detailsEnabled) style.detailsHdrToning * 0.20f else 0f)
                ).coerceIn(0f, 2.4f)
            if (abs(saturate - 1f) > 0.015f) {
                parts += String.format(Locale.US, "saturate(%.2f)", saturate)
            }
            if (abs(style.temperature) > 0.02f) {
                val hueDeg = (-style.temperature * 18f).coerceIn(-35f, 35f)
                parts += String.format(Locale.US, "hue-rotate(%.1fdeg)", hueDeg)
            }
        }

        if (style.nightModeEnabled && style.nightModeIntensity > 0.01f) {
            val sepiaNight = (style.nightModeIntensity * 0.55f).coerceIn(0f, 0.7f)
            parts += String.format(Locale.US, "sepia(%.2f)", sepiaNight)
        }

        if (style.sepiaEnabled && style.sepiaIntensity > 0.01f) {
            parts += String.format(Locale.US, "sepia(%.2f)", style.sepiaIntensity.coerceIn(0f, 1f))
        }

        if (style.blackWhiteEnabled && style.blackWhiteIntensity > 0.01f) {
            parts += String.format(Locale.US, "grayscale(%.2f)", style.blackWhiteIntensity.coerceIn(0f, 1f))
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
        val s = strength.coerceIn(0f, 1.85f)
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
     * Generates Better xCloud-style browser video filter JavaScript that runs inside Chromium's
     * hardware video compositor (zero MediaProjection screen capture):
     * - Injects inline SVG <filter id="xenos-gfn-video-filter"> with <feConvolveMatrix> (Clarity Boost
     *   Sharpen) + <feColorMatrix type="matrix"> (all 18-module color/tint/mood/colorblind/night grades)
     * - Applies !important stylesheet + Shadow DOM traversal + MutationObserver + fullscreenchange +
     *   watchdog interval so dynamic GeForce NOW stream <video> elements always receive the filter
     * - Renders Vignette, Old Film frame, and Letterbox bars in a zero-cost pointer-events:none overlay
     */
    fun buildVideoStyleScript(
        style: GameFilterStyle?,
        stretchToFill: Boolean,
        enableCssFilters: Boolean
    ): String {
        val activeStyle = if (enableCssFilters) style else null
        val hasActiveFilter = activeStyle != null
        val cssFallback = if (activeStyle != null) buildCssFilterString(activeStyle) else "none"
        val svgMatrix = if (activeStyle != null) buildSvgColorMatrixValues(activeStyle) else ""
        val sharpenKernel = if (activeStyle != null) buildSharpenKernelMatrix(activeStyle) else null
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

        val svgFilterExpr = if (hasActiveFilter) "url(#xenos-gfn-video-filter)" else "none"
        val sharpenKernelJs = if (sharpenKernel != null) "'${escapeJs(sharpenKernel)}'" else "null"

        return """
            (function() {
              try {
                window.__xenosVideoStyle = {
                  active: $hasActiveFilter,
                  filter: '${escapeJs(svgFilterExpr)}',
                  cssFallback: '${escapeJs(cssFallback)}',
                  svgMatrix: '${escapeJs(svgMatrix)}',
                  sharpenKernel: $sharpenKernelJs,
                  objectFit: '$objectFit',
                  vignetteAlpha: ${String.format(Locale.US, "%.3f", vignetteAlpha)},
                  letterboxH: ${String.format(Locale.US, "%.2f", letterboxH)},
                  letterboxW: ${String.format(Locale.US, "%.2f", letterboxW)},
                  splitPos: ${String.format(Locale.US, "%.1f", splitPos)}
                };

                function ensureSvgFilter(cfg) {
                  var svgId = 'xenos-gfn-svg-defs';
                  var svg = document.getElementById(svgId);
                  if (!cfg.active) {
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
                  var inner = '<defs><filter id="xenos-gfn-video-filter" color-interpolation-filters="sRGB">';
                  if (cfg.sharpenKernel) {
                    inner += '<feConvolveMatrix order="3" preserveAlpha="true" kernelMatrix="' + cfg.sharpenKernel + '" result="sharp"/>';
                    inner += '<feColorMatrix in="sharp" type="matrix" values="' + cfg.svgMatrix + '"/>';
                  } else {
                    inner += '<feColorMatrix type="matrix" values="' + cfg.svgMatrix + '"/>';
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
                  var rule = 'video { filter: ' + filterCss + ' !important; -webkit-filter: ' + filterCss + ' !important; object-fit: ' + cfg.objectFit + ' !important; }';
                  if (tag.textContent !== rule) tag.textContent = rule;
                }

                function ensureFxOverlay(cfg) {
                  var fxId = 'xenos-gfn-fx-layer';
                  var fx = document.getElementById(fxId);
                  var needsFx = cfg.active && (Math.abs(cfg.vignetteAlpha) > 0.01 || cfg.letterboxH > 0.1 || cfg.letterboxW > 0.1 || cfg.splitPos > 0);
                  if (!needsFx) {
                    if (fx && fx.parentNode) fx.parentNode.removeChild(fx);
                    return;
                  }
                  if (!fx) {
                    fx = document.createElement('div');
                    fx.id = fxId;
                    fx.style.cssText = 'position:fixed;inset:0;pointer-events:none;z-index:2147483640;overflow:hidden;';
                    (document.body || document.documentElement).appendChild(fx);
                  }
                  var bg = 'none';
                  if (Math.abs(cfg.vignetteAlpha) > 0.01) {
                    var a = Math.min(0.92, Math.abs(cfg.vignetteAlpha) * 0.85).toFixed(2);
                    var col = cfg.vignetteAlpha > 0 ? 'rgba(0,0,0,' + a + ')' : 'rgba(255,255,255,' + a + ')';
                    bg = 'radial-gradient(ellipse at center, transparent 48%, ' + col + ' 100%)';
                  }
                  fx.style.background = bg;
                  var bars = '';
                  if (cfg.letterboxH > 0.1) {
                    bars += '<div style="position:absolute;top:0;left:0;right:0;height:' + cfg.letterboxH + '%;background:#000;"></div>';
                    bars += '<div style="position:absolute;bottom:0;left:0;right:0;height:' + cfg.letterboxH + '%;background:#000;"></div>';
                  }
                  if (cfg.letterboxW > 0.1) {
                    bars += '<div style="position:absolute;top:0;bottom:0;left:0;width:' + cfg.letterboxW + '%;background:#000;"></div>';
                    bars += '<div style="position:absolute;top:0;bottom:0;right:0;width:' + cfg.letterboxW + '%;background:#000;"></div>';
                  }
                  if (cfg.splitPos > 0) {
                    bars += '<div style="position:absolute;top:0;bottom:0;left:' + cfg.splitPos + '%;width:2px;background:#8E9CFF;box-shadow:0 0 8px #8E9CFF;"></div>';
                  }
                  if (fx.__lastBars !== bars) {
                    fx.innerHTML = bars;
                    fx.__lastBars = bars;
                  }
                }

                function walkAndApply(root, cfg) {
                  if (!root || !root.querySelectorAll) return;
                  var filterCss = cfg.active ? cfg.filter : 'none';
                  var vids = root.querySelectorAll('video');
                  for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    v.style.setProperty('filter', filterCss, 'important');
                    v.style.setProperty('-webkit-filter', filterCss, 'important');
                    v.style.setProperty('object-fit', cfg.objectFit, 'important');
                  }
                  var all = root.querySelectorAll('*');
                  for (var j = 0; j < all.length; j++) {
                    if (all[j].shadowRoot) {
                      walkAndApply(all[j].shadowRoot, cfg);
                    }
                  }
                }

                window.__xenosApplyVideoStyle = function() {
                  var cfg = window.__xenosVideoStyle || { active: false, filter: 'none', objectFit: 'contain' };
                  ensureSvgFilter(cfg);
                  ensureStyleSheet(cfg);
                  ensureFxOverlay(cfg);
                  walkAndApply(document, cfg);
                };

                window.__xenosApplyVideoStyle();

                if (!window.__xenosVideoObserver && document.documentElement) {
                  window.__xenosVideoObserver = new MutationObserver(function() {
                    if (window.__xenosApplyVideoStyle) window.__xenosApplyVideoStyle();
                  });
                  window.__xenosVideoObserver.observe(document.documentElement, { childList: true, subtree: true });
                  document.addEventListener('fullscreenchange', function() {
                    if (window.__xenosApplyVideoStyle) window.__xenosApplyVideoStyle();
                  });
                  setInterval(function() {
                    if (window.__xenosApplyVideoStyle) window.__xenosApplyVideoStyle();
                  }, 1500);
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
    fun buildBootstrapScript(config: WebStreamQualityConfig): String {
        val region = config.regionPreset
        val regionEnabled = region.isEnabled
        val fakeIp = escapeJs(region.fakeIp)
        val acceptLang = escapeJs(region.acceptLanguage)
        val primaryLocale = escapeJs(region.acceptLanguage.substringBefore(',').ifBlank { "en-US" })
        val timeZone = escapeJs(region.timeZone)
        val spoofLocale = regionEnabled && config.spoofTimezoneAndLocale
        val virtualPadEnabled = config.enableVirtualGamepad

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
                virtualPadEnabled: $virtualPadEnabled
              };

              if (window.__xenosBetterGfnInstalled) {
                if (window.__xenosPad && window.__xenosPad.syncConnectedState) {
                  window.__xenosPad.syncConnectedState();
                }
                return;
              }
              window.__xenosBetterGfnInstalled = true;

              // 1. Better xCloud-style Region Bypass (NVIDIA / GeForce NOW hosts only)
              function isNvidiaHost(urlStr) {
                try {
                  var u = new URL(urlStr, window.location.href);
                  var h = (u.hostname || '').toLowerCase();
                  return h.endsWith('geforcenow.com') || h.endsWith('nvidia.com') || h.endsWith('nvidiagrid.net');
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
                    if (cfg && cfg.regionEnabled && cfg.fakeIp && isNvidiaHost(urlStr)) {
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
                    if (cfg && cfg.regionEnabled && cfg.fakeIp && isNvidiaHost(this.__xenosUrl || '')) {
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
              var buttons = [];
              for (var b = 0; b < 17; b++) {
                buttons.push({ pressed: false, touched: false, value: 0.0 });
              }
              var virtualPad = {
                id: 'Xbox 360 Controller (XenosPad Virtual Standard Gamepad)',
                index: 0,
                connected: true,
                timestamp: performance.now(),
                mapping: 'standard',
                axes: [0.0, 0.0, 0.0, 0.0],
                buttons: buttons,
                vibrationActuator: null,
                hapticActuators: []
              };
              var padDispatched = false;
              var origGetGamepads = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : function() { return []; };

              navigator.getGamepads = function() {
                var real = [];
                try {
                  var raw = origGetGamepads();
                  if (raw) {
                    for (var i = 0; i < raw.length; i++) real.push(raw[i]);
                  }
                } catch (e) {}
                if (window.__xenosConfig && window.__xenosConfig.virtualPadEnabled) {
                  virtualPad.connected = true;
                  var result = [virtualPad, null, null, null];
                  for (var j = 0; j < real.length && j < 3; j++) {
                    if (real[j]) result[j + 1] = real[j];
                  }
                  return result;
                }
                return real;
              };

              function notifyGamepadConnected() {
                if (padDispatched || !(window.__xenosConfig && window.__xenosConfig.virtualPadEnabled)) return;
                padDispatched = true;
                try {
                  var ev = new Event('gamepadconnected');
                  Object.defineProperty(ev, 'gamepad', { value: virtualPad });
                  window.dispatchEvent(ev);
                } catch (e) {}
              }

              window.__xenosPad = {
                syncConnectedState: function() {
                  if (window.__xenosConfig && window.__xenosConfig.virtualPadEnabled) {
                    notifyGamepadConnected();
                  }
                },
                setButton: function(idx, isDown) {
                  if (idx < 0 || idx >= virtualPad.buttons.length) return;
                  notifyGamepadConnected();
                  virtualPad.buttons[idx] = { pressed: !!isDown, touched: !!isDown, value: isDown ? 1.0 : 0.0 };
                  virtualPad.timestamp = performance.now();
                },
                setAxes: function(isLeft, x, y) {
                  notifyGamepadConnected();
                  var cx = Math.max(-1, Math.min(1, Number(x) || 0));
                  var cy = Math.max(-1, Math.min(1, Number(y) || 0));
                  if (isLeft) {
                    virtualPad.axes[0] = cx;
                    virtualPad.axes[1] = cy;
                  } else {
                    virtualPad.axes[2] = cx;
                    virtualPad.axes[3] = cy;
                  }
                  virtualPad.timestamp = performance.now();
                },
                reset: function() {
                  for (var i = 0; i < virtualPad.buttons.length; i++) {
                    virtualPad.buttons[i] = { pressed: false, touched: false, value: 0.0 };
                  }
                  virtualPad.axes[0] = 0; virtualPad.axes[1] = 0;
                  virtualPad.axes[2] = 0; virtualPad.axes[3] = 0;
                  virtualPad.timestamp = performance.now();
                }
              };
              setTimeout(notifyGamepadConnected, 800);

              // 3. Direct Keyboard / Mouse / Wheel Event Dispatcher
              var pointerX = Math.round(window.innerWidth / 2);
              var pointerY = Math.round(window.innerHeight / 2);
              var mouseButtonsMask = 0;
              var lastNativeMouseDownAt = [0, 0, 0];
              var lastNativeMouseUpAt = [0, 0, 0];

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

              function findStreamMediaElement(root) {
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

              function streamTarget() {
                return document.pointerLockElement ||
                  findStreamMediaElement(document) ||
                  document.activeElement ||
                  document.body ||
                  document;
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
                if (document.pointerLockElement) return document.pointerLockElement;
                var hit = deepElementFromPoint(document, x, y);
                if (hit) return hit;
                return streamTarget();
              }

              function dispatchRelativeMove(mx, my) {
                var t = pointTarget(pointerX, pointerY);
                var opts = {
                  movementX: mx,
                  movementY: my,
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
                  t.dispatchEvent(new PointerEvent('pointermove', Object.assign({ pointerType: 'mouse', isPrimary: true, pointerId: 1, width: 1, height: 1, pressure: mouseButtonsMask ? 0.5 : 0 }, opts)));
                }
                var ev = new MouseEvent('mousemove', opts);
                t.dispatchEvent(ev);
                if (t !== document) document.dispatchEvent(ev);
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
                    pointerX = Math.max(0, Math.min(window.innerWidth, pointerX + mx));
                    pointerY = Math.max(0, Math.min(window.innerHeight, pointerY + my));
                    dispatchRelativeMove(mx, my);
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
                    if (!activePc || !window.XenosWebBridge || !window.XenosWebBridge.onWebRtcStats) return;
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
                      var payload = JSON.stringify({
                        fps: fps,
                        rttMs: rttMs,
                        bitrateMbps: bitrateMbps,
                        packetLossPercent: lossPct,
                        jitterMs: jitterMs,
                        decodeMs: decodeMs,
                        resolution: res,
                        codec: codec,
                        connected: activePc.connectionState === 'connected' || fps > 0
                      });
                      window.XenosWebBridge.onWebRtcStats(payload);
                    }).catch(function() {});
                  } catch (e) {}
                }, 1000);
              }
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
