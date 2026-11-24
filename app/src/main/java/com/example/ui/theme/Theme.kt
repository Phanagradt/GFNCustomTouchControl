package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig

private fun readableOnColor(bg: Color): Color =
    if (bg.luminance() > 0.40f) Color(0xFF0A1118) else Color.White

private fun buildDarkColorScheme(config: AppColorSchemeConfig): ColorScheme {
    val primary = Color(config.effectivePrimaryHex())
    val secondary = Color(config.effectiveSecondaryHex())
    val tertiary = Color(config.effectiveTertiaryHex())

    return darkColorScheme(
        primary = primary,
        onPrimary = readableOnColor(primary),
        primaryContainer = lerp(ObsidianSurface, primary, 0.22f),
        onPrimaryContainer = lerp(primary, Color.White, 0.20f),
        secondary = secondary,
        onSecondary = readableOnColor(secondary),
        secondaryContainer = lerp(ObsidianSurface, secondary, 0.22f),
        onSecondaryContainer = lerp(secondary, Color.White, 0.20f),
        tertiary = tertiary,
        onTertiary = readableOnColor(tertiary),
        tertiaryContainer = lerp(ObsidianSurface, tertiary, 0.22f),
        onTertiaryContainer = lerp(tertiary, Color.White, 0.20f),
        background = ObsidianVoid,
        onBackground = SlateTextPrimary,
        surface = ObsidianSurface,
        onSurface = SlateTextPrimary,
        surfaceVariant = ObsidianContainer,
        onSurfaceVariant = SlateTextSecondary,
        surfaceContainerLowest = ObsidianVoid,
        surfaceContainerLow = ObsidianSurface,
        surfaceContainer = ObsidianContainer,
        surfaceContainerHigh = ObsidianElevated,
        outline = ObsidianOutline,
        outlineVariant = Color(0xFF253042),
        error = MacroCoral,
        onError = Color(0xFF2B000B)
    )
}

private fun buildLightColorScheme(config: AppColorSchemeConfig): ColorScheme {
    val rawPrimary = Color(config.effectivePrimaryHex())
    val rawSecondary = Color(config.effectiveSecondaryHex())
    val rawTertiary = Color(config.effectiveTertiaryHex())

    fun daylightTone(c: Color): Color =
        if (c.luminance() > 0.45f) lerp(c, Color(0xFF0E1726), 0.36f) else c

    val primary = daylightTone(rawPrimary)
    val secondary = daylightTone(rawSecondary)
    val tertiary = daylightTone(rawTertiary)

    return lightColorScheme(
        primary = primary,
        onPrimary = readableOnColor(primary),
        primaryContainer = lerp(Color.White, rawPrimary, 0.26f),
        onPrimaryContainer = lerp(rawPrimary, Color(0xFF0A1018), 0.72f),
        secondary = secondary,
        onSecondary = readableOnColor(secondary),
        secondaryContainer = lerp(Color.White, rawSecondary, 0.26f),
        onSecondaryContainer = lerp(rawSecondary, Color(0xFF0A1018), 0.72f),
        tertiary = tertiary,
        onTertiary = readableOnColor(tertiary),
        tertiaryContainer = lerp(Color.White, rawTertiary, 0.26f),
        onTertiaryContainer = lerp(rawTertiary, Color(0xFF0A1018), 0.72f),
        background = DaylightBackground,
        onBackground = DaylightTextPrimary,
        surface = DaylightSurface,
        onSurface = DaylightTextPrimary,
        surfaceVariant = DaylightContainer,
        onSurfaceVariant = DaylightTextSecondary,
        outline = DaylightOutline,
        error = Color(0xFFD32F2F),
        onError = Color.White
    )
}

val NexusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NexusPadTheme(
    darkTheme: Boolean = true,
    colorSchemeConfig: AppColorSchemeConfig = AppColorSchemeConfig(),
    fontConfig: AppFontConfig = AppFontConfig(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, colorSchemeConfig, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> buildDarkColorScheme(colorSchemeConfig)
            else -> buildLightColorScheme(colorSchemeConfig)
        }
    }
    val typography = remember(fontConfig) { buildNexusTypography(fontConfig) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = NexusShapes,
        content = content
    )
}
