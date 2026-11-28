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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppPlatformTheme

val LocalAppPlatformTheme = compositionLocalOf { AppPlatformTheme.XENOS_DEFAULT }
val LocalNintendoSwitchDarkMode = compositionLocalOf { false }

private fun readableOnColor(bg: Color): Color =
    if (bg.luminance() > 0.42f) Color(0xFF0A1118) else Color.White

private fun buildDarkColorScheme(
    config: AppColorSchemeConfig,
    platformTheme: AppPlatformTheme
): ColorScheme {
    val usePlatform = platformTheme != AppPlatformTheme.XENOS_DEFAULT
    val primary = Color(if (usePlatform) platformTheme.accentPrimaryHex else config.effectivePrimaryHex())
    val secondary = Color(if (usePlatform) platformTheme.accentSecondaryHex else config.effectiveSecondaryHex())
    val tertiary = Color(if (usePlatform) platformTheme.accentTertiaryHex else config.effectiveTertiaryHex())

    val preset = config.preset
    val bg = Color(if (usePlatform) platformTheme.bgHex else preset.darkBgHex)
    val surface = Color(if (usePlatform) platformTheme.surfaceHex else preset.darkSurfaceHex)
    val container = Color(if (usePlatform) platformTheme.containerHex else preset.darkContainerHex)
    val elevated = Color(if (usePlatform) platformTheme.elevatedHex else preset.darkElevatedHex)
    val outline = Color(if (usePlatform) platformTheme.outlineHex else preset.darkOutlineHex)
    val outlineVariant = lerp(surface, outline, 0.55f)

    return darkColorScheme(
        primary = primary,
        onPrimary = readableOnColor(primary),
        primaryContainer = lerp(surface, primary, 0.24f),
        onPrimaryContainer = lerp(primary, Color.White, 0.22f),
        secondary = secondary,
        onSecondary = readableOnColor(secondary),
        secondaryContainer = lerp(surface, secondary, 0.24f),
        onSecondaryContainer = lerp(secondary, Color.White, 0.22f),
        tertiary = tertiary,
        onTertiary = readableOnColor(tertiary),
        tertiaryContainer = lerp(surface, tertiary, 0.24f),
        onTertiaryContainer = lerp(tertiary, Color.White, 0.22f),
        background = bg,
        onBackground = SlateTextPrimary,
        surface = surface,
        onSurface = SlateTextPrimary,
        surfaceVariant = container,
        onSurfaceVariant = SlateTextSecondary,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = container,
        surfaceContainerHigh = elevated,
        outline = outline,
        outlineVariant = outlineVariant,
        error = MacroCoral,
        onError = Color(0xFF2B000B)
    )
}

private fun buildLightColorScheme(
    config: AppColorSchemeConfig,
    platformTheme: AppPlatformTheme
): ColorScheme {
    val usePlatform = platformTheme != AppPlatformTheme.XENOS_DEFAULT
    val rawPrimary = Color(if (usePlatform) platformTheme.accentPrimaryHex else config.effectivePrimaryHex())
    val rawSecondary = Color(if (usePlatform) platformTheme.accentSecondaryHex else config.effectiveSecondaryHex())
    val rawTertiary = Color(if (usePlatform) platformTheme.accentTertiaryHex else config.effectiveTertiaryHex())

    fun daylightTone(c: Color): Color =
        if (c.luminance() > 0.38f) lerp(c, Color(0xFF1A1633), 0.42f) else c

    val primary = daylightTone(rawPrimary)
    val secondary = daylightTone(rawSecondary)
    val tertiary = daylightTone(rawTertiary)
    val isXenos = !usePlatform && config.preset == com.example.model.AppThemePreset.XENOS_DEFAULT
    val isSwitch = platformTheme == AppPlatformTheme.NINTENDO_SWITCH

    return lightColorScheme(
        primary = primary,
        onPrimary = readableOnColor(primary),
        primaryContainer = lerp(Color.White, rawPrimary, 0.22f),
        onPrimaryContainer = lerp(rawPrimary, Color(0xFF0A1018), 0.72f),
        secondary = secondary,
        onSecondary = readableOnColor(secondary),
        secondaryContainer = lerp(Color.White, rawSecondary, 0.22f),
        onSecondaryContainer = lerp(rawSecondary, Color(0xFF0A1018), 0.72f),
        tertiary = tertiary,
        onTertiary = readableOnColor(tertiary),
        tertiaryContainer = lerp(Color.White, rawTertiary, 0.22f),
        onTertiaryContainer = lerp(rawTertiary, Color(0xFF0A1018), 0.72f),
        background = when {
            isSwitch -> Color(0xFFEBEBEB)
            isXenos -> Color(0xFFF3F2FA)
            else -> DaylightBackground
        },
        onBackground = if (isSwitch) Color(0xFF2D2D2D) else DaylightTextPrimary,
        surface = if (isSwitch) Color(0xFFFFFFFF) else DaylightSurface,
        onSurface = if (isSwitch) Color(0xFF2D2D2D) else DaylightTextPrimary,
        surfaceVariant = when {
            isSwitch -> Color(0xFFE0E0E0)
            isXenos -> Color(0xFFE7E5F5)
            else -> DaylightContainer
        },
        onSurfaceVariant = if (isSwitch) Color(0xFF636363) else DaylightTextSecondary,
        surfaceContainerLowest = if (isSwitch) Color(0xFFEBEBEB) else DaylightBackground,
        surfaceContainerLow = if (isSwitch) Color(0xFFF5F5F5) else DaylightSurface,
        surfaceContainer = if (isSwitch) Color(0xFFFFFFFF) else DaylightContainer,
        surfaceContainerHigh = if (isSwitch) Color(0xFFF0F0F0) else DaylightContainer,
        outline = when {
            isSwitch -> Color(0xFFCFCFCF)
            isXenos -> Color(0xFFC5C1DE)
            else -> DaylightOutline
        },
        outlineVariant = if (isSwitch) Color(0xFFDDDDDD) else DaylightOutline,
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

private fun buildPlatformShapes(platformTheme: AppPlatformTheme): Shapes {
    val c = platformTheme.cardCornerDp
    return when (platformTheme) {
        AppPlatformTheme.XENOS_DEFAULT -> NexusShapes
        AppPlatformTheme.STEAM_DECK -> Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(12.dp)
        )
        AppPlatformTheme.EPIC_GAMES -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(10.dp),
            extraLarge = RoundedCornerShape(14.dp)
        )
        AppPlatformTheme.XBOX -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        )
        AppPlatformTheme.NINTENDO_SWITCH -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(10.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
        AppPlatformTheme.PLAYSTATION -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
    }.also { _ -> c }
}

@Composable
fun NexusPadTheme(
    darkTheme: Boolean = true,
    platformTheme: AppPlatformTheme = AppPlatformTheme.XENOS_DEFAULT,
    nintendoSwitchDarkMode: Boolean = false,
    colorSchemeConfig: AppColorSchemeConfig = AppColorSchemeConfig(),
    fontConfig: AppFontConfig = AppFontConfig(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Console themes like Steam Deck, Epic, Xbox, and PS5 are natively dark;
    // Nintendo Switch supports dedicated Basic White (light) and Basic Black (dark) modes!
    val effectiveDark = when (platformTheme) {
        AppPlatformTheme.XENOS_DEFAULT -> darkTheme
        AppPlatformTheme.NINTENDO_SWITCH -> nintendoSwitchDarkMode
        else -> true
    }
    val colorScheme = remember(effectiveDark, platformTheme, colorSchemeConfig, dynamicColor) {
        when {
            dynamicColor && platformTheme == AppPlatformTheme.XENOS_DEFAULT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            effectiveDark -> buildDarkColorScheme(colorSchemeConfig, platformTheme)
            else -> buildLightColorScheme(colorSchemeConfig, platformTheme)
        }
    }
    val typography = remember(fontConfig) { buildNexusTypography(fontConfig) }
    val shapes = remember(platformTheme) { buildPlatformShapes(platformTheme) }

    CompositionLocalProvider(
        LocalAppPlatformTheme provides platformTheme,
        LocalNintendoSwitchDarkMode provides nintendoSwitchDarkMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
