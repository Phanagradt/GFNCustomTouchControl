package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.model.BlackWhiteTone
import com.example.model.ColorblindMode
import com.example.model.FilterTintPreset
import com.example.model.GameFilterStyle
import com.example.model.MoodFilterPreset
import com.example.model.SpecialFxMode
import com.example.model.StickerPreset
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Computes Android WindowManager dimAmount for negative exposure/shadows so underlying
 * streams darken smoothly in Quick Optical mode while NexusPad controls above stay bright.
 */
fun computeOverlayDimAmount(style: GameFilterStyle?): Float {
    if (style == null || !style.brightnessContrastEnabled) return 0f
    val darkFromExposure = (-style.effectiveExposure).coerceAtLeast(0f) * 0.85f
    val darkFromNegativeShadows = (-style.shadows).coerceAtLeast(0f) * 0.45f
    return (darkFromExposure + darkFromNegativeShadows).coerceIn(0f, 0.58f)
}

/**
 * Computes Android 12+ WindowManager blurBehindRadius when Depth of Field is enabled in
 * Quick Optical mode (0 when Live GPU Shader is handling real multi-tap bokeh).
 */
fun computeOverlayBlurRadius(style: GameFilterStyle?, gpuShaderActive: Boolean): Int {
    if (style == null || gpuShaderActive || !style.dofEnabled) return 0
    val strength = ((style.dofNearBlur + style.dofFarBlur) * 0.5f).coerceIn(0f, 1f)
    return (strength * 28f).roundToInt().coerceIn(0, 32)
}

/**
 * Renders Stickers (built-in tactical frames, cinema REC overlays, neon corners, CRT bezel,
 * or custom uploaded PNG stickers) on top of either the Live GPU Shader or Quick Optical HUD.
 */
@Composable
fun StickerOverlayCanvas(
    style: GameFilterStyle,
    modifier: Modifier = Modifier
) {
    if (!style.stickersEnabled) return

    val customBitmap: ImageBitmap? = remember(style.customStickerBase64) {
        val raw = style.customStickerBase64
        if (raw.isNullOrBlank()) {
            null
        } else {
            runCatching {
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = style.stickerOpacity.coerceIn(0.1f, 1f)
        val scale = style.stickerScale.coerceIn(0.3f, 1.5f)
        val clipLeft = if (style.splitscreenEnabled) size.width * style.splitscreenPosition.coerceIn(0f, 1f) else 0f

        clipRect(left = clipLeft, top = 0f, right = size.width, bottom = size.height) {
            when (style.stickerPreset) {
                StickerPreset.TACTICAL_HUD -> drawTacticalHudFrame(alpha, scale)
                StickerPreset.CINEMA_REC -> drawCinemaRecFrame(alpha, scale)
                StickerPreset.NEON_CORNERS -> drawNeonCornerFrame(alpha, scale)
                StickerPreset.RETRO_CRT -> drawRetroCrtBezel(alpha, scale)
                StickerPreset.CUSTOM_IMAGE -> {
                    if (customBitmap != null) {
                        val baseDim = minOf(size.width, size.height) * 0.34f * scale
                        val aspect = customBitmap.width.toFloat() / customBitmap.height.coerceAtLeast(1).toFloat()
                        val drawW = (if (aspect >= 1f) baseDim else baseDim * aspect).roundToInt().coerceAtLeast(16)
                        val drawH = (if (aspect >= 1f) baseDim / aspect else baseDim).roundToInt().coerceAtLeast(16)
                        val cx = (size.width * style.stickerX.coerceIn(0.1f, 0.9f)).roundToInt()
                        val cy = (size.height * style.stickerY.coerceIn(0.1f, 0.9f)).roundToInt()
                        drawImage(
                            image = customBitmap,
                            dstOffset = IntOffset(cx - drawW / 2, cy - drawH / 2),
                            dstSize = IntSize(drawW, drawH),
                            alpha = alpha
                        )
                    } else {
                        drawTacticalHudFrame(alpha * 0.7f, scale)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawTacticalHudFrame(alpha: Float, scale: Float) {
    val margin = 16f * scale
    val arm = 36f * scale
    val stroke = 3.2f * scale
    val color = GeForceLime.copy(alpha = alpha)
    val subColor = TacticalCyan.copy(alpha = alpha * 0.55f)

    // 4 tactical corner brackets
    drawLine(color, Offset(margin, margin), Offset(margin + arm, margin), stroke)
    drawLine(color, Offset(margin, margin), Offset(margin, margin + arm), stroke)

    drawLine(color, Offset(size.width - margin, margin), Offset(size.width - margin - arm, margin), stroke)
    drawLine(color, Offset(size.width - margin, margin), Offset(size.width - margin, margin + arm), stroke)

    drawLine(color, Offset(margin, size.height - margin), Offset(margin + arm, size.height - margin), stroke)
    drawLine(color, Offset(margin, size.height - margin), Offset(margin, size.height - margin - arm), stroke)

    drawLine(color, Offset(size.width - margin, size.height - margin), Offset(size.width - margin - arm, size.height - margin), stroke)
    drawLine(color, Offset(size.width - margin, size.height - margin), Offset(size.width - margin, size.height - margin - arm), stroke)

    // Side compass ticks
    val midY = size.height / 2f
    for (i in -3..3) {
        val y = midY + i * 16f * scale
        val len = if (i == 0) 14f * scale else 7f * scale
        drawLine(subColor, Offset(margin, y), Offset(margin + len, y), 2f)
        drawLine(subColor, Offset(size.width - margin, y), Offset(size.width - margin - len, y), 2f)
    }
}

private fun DrawScope.drawCinemaRecFrame(alpha: Float, scale: Float) {
    val margin = 20f * scale
    val stroke = 2.5f * scale
    val white = Color.White.copy(alpha = alpha * 0.85f)
    val arm = 28f * scale

    // Viewfinder safe-area brackets
    drawLine(white, Offset(margin, margin), Offset(margin + arm, margin), stroke)
    drawLine(white, Offset(margin, margin), Offset(margin, margin + arm), stroke)
    drawLine(white, Offset(size.width - margin, margin), Offset(size.width - margin - arm, margin), stroke)
    drawLine(white, Offset(size.width - margin, margin), Offset(size.width - margin, margin + arm), stroke)
    drawLine(white, Offset(margin, size.height - margin), Offset(margin + arm, size.height - margin), stroke)
    drawLine(white, Offset(margin, size.height - margin), Offset(margin, size.height - margin - arm), stroke)
    drawLine(white, Offset(size.width - margin, size.height - margin), Offset(size.width - margin - arm, size.height - margin), stroke)
    drawLine(white, Offset(size.width - margin, size.height - margin), Offset(size.width - margin, size.height - margin - arm), stroke)

    // Red REC indicator dot + pill top-left
    drawCircle(
        color = MacroCoral.copy(alpha = alpha),
        radius = 7f * scale,
        center = Offset(margin + 22f * scale, margin + 22f * scale)
    )
    // Center hairline cross
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(white.copy(alpha = alpha * 0.4f), Offset(cx - 12f * scale, cy), Offset(cx + 12f * scale, cy), 1.5f)
    drawLine(white.copy(alpha = alpha * 0.4f), Offset(cx, cy - 12f * scale), Offset(cx, cy + 12f * scale), 1.5f)
}

private fun DrawScope.drawNeonCornerFrame(alpha: Float, scale: Float) {
    val inset = 8f * scale
    val brush = Brush.linearGradient(
        colors = listOf(
            TacticalCyan.copy(alpha = alpha),
            GeForceLime.copy(alpha = alpha),
            Color(0xFFFF4D6D).copy(alpha = alpha)
        )
    )
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(18f * scale, 18f * scale),
        style = Stroke(width = 4.5f * scale)
    )
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset + 5f, inset + 5f),
        size = Size((size.width - (inset + 5f) * 2f).coerceAtLeast(1f), (size.height - (inset + 5f) * 2f).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(14f * scale, 14f * scale),
        style = Stroke(width = 1.4f * scale)
    )
}

private fun DrawScope.drawRetroCrtBezel(alpha: Float, scale: Float) {
    val thick = 16f * scale
    drawRect(
        color = Color(0xFF0A0D12).copy(alpha = alpha * 0.92f),
        topLeft = Offset.Zero,
        size = Size(size.width, thick)
    )
    drawRect(
        color = Color(0xFF0A0D12).copy(alpha = alpha * 0.92f),
        topLeft = Offset(0f, size.height - thick),
        size = Size(size.width, thick)
    )
    drawRect(
        color = Color(0xFF0A0D12).copy(alpha = alpha * 0.92f),
        topLeft = Offset.Zero,
        size = Size(thick, size.height)
    )
    drawRect(
        color = Color(0xFF0A0D12).copy(alpha = alpha * 0.92f),
        topLeft = Offset(size.width - thick, 0f),
        size = Size(thick, size.height)
    )
    drawRoundRect(
        color = Color(0xFF39FF88).copy(alpha = alpha * 0.55f),
        topLeft = Offset(thick, thick),
        size = Size((size.width - thick * 2f).coerceAtLeast(1f), (size.height - thick * 2f).coerceAtLeast(1f)),
        cornerRadius = CornerRadius(24f * scale, 24f * scale),
        style = Stroke(width = 2.5f)
    )
}

/**
 * Full-screen overlay companion canvas.
 * - When [gpuShaderActive] is true, the OpenGL ES 2.0 Freestyle shader renders all 17 pixel
 *   effects directly from the captured game stream, so this layer draws only Stickers above it.
 * - When [gpuShaderActive] is false (Quick Optical Mode without capture prompt), this layer
 *   renders multi-pass optical effects (Splitscreen clip, Letterbox, Vignette, animated Old Film
 *   grain/scratches, Half-Tone/Retro scanlines, Night Mode, Colorblind contrast accent, Mood,
 *   Exposure/Shadows/Temperature/Tint, and Stickers) — never a lazy blank tint sheet.
 */
@Composable
fun ScreenFilterOverlayCanvas(
    style: GameFilterStyle,
    gpuShaderActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (gpuShaderActive) {
        StickerOverlayCanvas(style = style, modifier = modifier)
        return
    }

    var filmTick by remember { mutableLongStateOf(0L) }
    val animateFilm = style.oldFilmEnabled && style.oldFilmDirtStrength > 0.02f
    LaunchedEffect(animateFilm) {
        while (animateFilm) {
            delay(75L)
            filmTick++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val splitActive = style.splitscreenEnabled
        val splitX = if (splitActive) size.width * style.splitscreenPosition.coerceIn(0f, 1f) else 0f

        clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
            // 1. Brightness / Contrast / Shadows / Highlights / Gamma optical balance
            if (style.brightnessContrastEnabled) {
                val lift = (style.shadows.coerceAtLeast(0f) * 0.12f) +
                    (style.effectiveExposure.coerceAtLeast(0f) * 0.15f) +
                    (style.gamma.coerceAtLeast(0f) * 0.09f) +
                    (style.highlights.coerceAtLeast(0f) * 0.08f)
                if (lift > 0.004f) {
                    drawRect(
                        color = Color.White.copy(alpha = lift.coerceIn(0f, 0.22f)),
                        blendMode = BlendMode.Screen
                    )
                }

                val dark = ((-style.effectiveExposure).coerceAtLeast(0f) * 0.24f) +
                    ((-style.shadows).coerceAtLeast(0f) * 0.16f) +
                    ((-style.gamma).coerceAtLeast(0f) * 0.12f)
                if (dark > 0.004f) {
                    drawRect(color = Color.Black.copy(alpha = dark.coerceIn(0f, 0.36f)))
                }

                if (style.contrast > 0.03f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.White.copy(alpha = (style.contrast * 0.06f).coerceIn(0f, 0.06f)),
                                0.62f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = (style.contrast * 0.24f).coerceIn(0f, 0.22f))
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.72f
                        )
                    )
                }
            }

            // 2. Details: Bloom glow + HDR contrast sheen
            if (style.detailsEnabled) {
                if (style.detailsBloom > 0.02f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF6E0).copy(alpha = (style.detailsBloom * 0.16f).coerceIn(0f, 0.16f)),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.35f),
                            radius = size.maxDimension * 0.65f
                        ),
                        blendMode = BlendMode.Screen
                    )
                }
            }

            // 3. Color: Temperature + Tint Hue
            if (style.colorEnabled) {
                if (abs(style.temperature) > 0.015f) {
                    val tempColor = if (style.temperature > 0f) Color(0xFFFF9838) else Color(0xFF39B8FF)
                    drawRect(color = tempColor.copy(alpha = (abs(style.temperature) * 0.22f).coerceIn(0f, 0.18f)))
                }
                if (style.tintIntensity > 0.01f &&
                    (style.tintPreset != FilterTintPreset.NEUTRAL || style.tintHue > 0f)
                ) {
                    val (r, g, b) = style.resolvedTintRgb()
                    drawRect(color = Color(r, g, b, (style.tintIntensity * 0.36f).coerceIn(0f, 0.30f)))
                }
            }

            // 4. Colorblind high-contrast correction tint
            if (style.colorblindEnabled && style.colorblindIntensity > 0.01f) {
                val cbColor = when (style.colorblindMode) {
                    ColorblindMode.PROTANOPIA -> Color(0xFF28B5FF)
                    ColorblindMode.DEUTERANOPIA -> Color(0xFFFF66C4)
                    ColorblindMode.TRITANOPIA -> Color(0xFFFFB733)
                }
                drawRect(color = cbColor.copy(alpha = (style.colorblindIntensity * 0.24f).coerceIn(0f, 0.24f)))
            }

            // 5. Night Mode blue-light filter
            if (style.nightModeEnabled && style.nightModeIntensity > 0.01f) {
                drawRect(color = Color(0xFFFF8C1A).copy(alpha = (style.nightModeIntensity * 0.34f).coerceIn(0f, 0.34f)))
            }

            // 6. Mood cinematic split-tone gradients
            if (style.moodEnabled && style.moodIntensity > 0.01f) {
                val m = style.moodIntensity.coerceIn(0f, 1f)
                val (topCol, botCol) = when (style.moodPreset) {
                    MoodFilterPreset.WARM_SUNSET -> Color(0xFFFF8A3D) to Color(0xFF8C2D66)
                    MoodFilterPreset.COLD_THRILLER -> Color(0xFF1D6B99) to Color(0xFF0A2438)
                    MoodFilterPreset.WASHED_RETRO -> Color(0xFFE6D5B8) to Color(0xFF5D534A)
                    MoodFilterPreset.CYBER_NEON -> Color(0xFF00E5FF) to Color(0xFFD500F9)
                    MoodFilterPreset.EMERALD_CINEMA -> Color(0xFF16A36A) to Color(0xFF082A24)
                    MoodFilterPreset.GOLDEN_NOIR -> Color(0xFFD9A441) to Color(0xFF14120E)
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topCol.copy(alpha = m * 0.22f),
                            botCol.copy(alpha = m * 0.26f)
                        )
                    )
                )
            }

            // 7. Black & White / Sepia optical grades
            if (style.blackWhiteEnabled && style.blackWhiteIntensity > 0.01f) {
                val bwTint = when (style.blackWhiteTone) {
                    BlackWhiteTone.WARM -> Color(0xFF9C9588)
                    BlackWhiteTone.COOL -> Color(0xFF8492A3)
                    else -> Color(0xFF888888)
                }
                drawRect(color = bwTint.copy(alpha = (style.blackWhiteIntensity * 0.28f).coerceIn(0f, 0.28f)))
            }
            if (style.sepiaEnabled && style.sepiaIntensity > 0.01f) {
                drawRect(color = Color(0xFF9E6B38).copy(alpha = (style.sepiaIntensity * 0.34f).coerceIn(0f, 0.34f)))
            }

            // 8. Old Film (vintage warm fade + animated flicker & vertical cinema scratches)
            if (style.oldFilmEnabled && style.oldFilmStrength > 0.01f) {
                val f = style.oldFilmStrength.coerceIn(0f, 1f)
                drawRect(color = Color(0xFFA87D44).copy(alpha = f * 0.22f))
                if (style.oldFilmDirtStrength > 0.02f) {
                    val dirt = style.oldFilmDirtStrength.coerceIn(0f, 1f)
                    val seed = (filmTick * 1103515245L + 12345L) and 0x7FFFFFFFL
                    val scratchX = ((seed % 1000L) / 1000f) * size.width
                    drawLine(
                        color = Color(0xFFFFF4DC).copy(alpha = dirt * f * 0.42f),
                        start = Offset(scratchX, 0f),
                        end = Offset(scratchX, size.height),
                        strokeWidth = 1.6f
                    )
                    val flickerAlpha = (((seed / 7L) % 100L) / 100f) * 0.08f * dirt * f
                    drawRect(color = Color.Black.copy(alpha = flickerAlpha))
                }
                if (style.oldFilmVignette > 0.02f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = (style.oldFilmVignette * f * 0.72f).coerceIn(0f, 0.72f))
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.68f
                        )
                    )
                }
            }

            // 9. Special FX optical scanline / halftone texture (batched draw calls for 60fps UI)
            if (style.specialFxEnabled && style.specialFxIntensity > 0.02f) {
                val fx = style.specialFxIntensity.coerceIn(0f, 1f)
                when (style.specialFxMode) {
                    SpecialFxMode.RETRO -> {
                        val step = 10f
                        val lineCol = Color.Black.copy(alpha = fx * 0.24f)
                        val linePoints = ArrayList<Offset>(((size.height / step).toInt() + 1) * 2)
                        var y = 0f
                        while (y < size.height) {
                            linePoints.add(Offset(0f, y))
                            linePoints.add(Offset(size.width, y))
                            y += step
                        }
                        drawPoints(
                            points = linePoints,
                            pointMode = androidx.compose.ui.graphics.PointMode.Lines,
                            color = lineCol,
                            strokeWidth = 2.0f
                        )
                    }
                    SpecialFxMode.HALFTONE -> {
                        val dotCol = Color.Black.copy(alpha = fx * 0.16f)
                        val step = 28f
                        val dotPoints = ArrayList<Offset>(((size.width / step).toInt() + 1) * ((size.height / step).toInt() + 1))
                        var y = step / 2f
                        while (y < size.height) {
                            var x = step / 2f
                            while (x < size.width) {
                                dotPoints.add(Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                        drawPoints(
                            points = dotPoints,
                            pointMode = androidx.compose.ui.graphics.PointMode.Points,
                            color = dotCol,
                            strokeWidth = 4.2f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    SpecialFxMode.SKETCH -> {
                        drawRect(color = Color(0xFFF5EFE0).copy(alpha = fx * 0.18f))
                    }
                }
            }

            // 10. Vignette (darken or brighten corners)
            if (style.vignetteEnabled && abs(style.vignetteIntensity) > 0.01f) {
                val vigCol = if (style.vignetteIntensity > 0f) Color.Black else Color.White
                val vigAlpha = (abs(style.vignetteIntensity) * 0.75f).coerceIn(0f, 0.78f)
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.42f to Color.Transparent,
                            1.0f to vigCol.copy(alpha = vigAlpha)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.68f
                    )
                )
            }

            // 11. Letterbox cinematic black bars
            if (style.letterboxEnabled) {
                val barH = size.height * style.letterboxHeight.coerceIn(0f, 0.40f)
                val barW = size.width * style.letterboxWidth.coerceIn(0f, 0.40f)
                if (barH > 1f) {
                    drawRect(Color.Black, Offset.Zero, Size(size.width, barH))
                    drawRect(Color.Black, Offset(0f, size.height - barH), Size(size.width, barH))
                }
                if (barW > 1f) {
                    drawRect(Color.Black, Offset.Zero, Size(barW, size.height))
                    drawRect(Color.Black, Offset(size.width - barW, 0f), Size(barW, size.height))
                }
            }
        }

        // Splitscreen vertical divider line
        if (splitActive) {
            drawLine(
                color = GeForceLime.copy(alpha = 0.92f),
                start = Offset(splitX, 0f),
                end = Offset(splitX, size.height),
                strokeWidth = 3f
            )
        }
    }

    // Stickers layer on top
    StickerOverlayCanvas(style = style, modifier = modifier)
}
