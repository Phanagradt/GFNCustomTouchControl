package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GeForceLime,
    onPrimary = OnGeForceLime,
    primaryContainer = GeForceLimeContainer,
    onPrimaryContainer = GeForceLime,
    secondary = TacticalCyan,
    onSecondary = OnTacticalCyan,
    secondaryContainer = TacticalCyanContainer,
    onSecondaryContainer = TacticalCyan,
    tertiary = TurboAmber,
    onTertiary = OnTurboAmber,
    tertiaryContainer = TurboAmberContainer,
    onTertiaryContainer = TurboAmber,
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

private val LightColorScheme = lightColorScheme(
    primary = DaylightLimePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F7A6),
    onPrimaryContainer = Color(0xFF172B00),
    secondary = DaylightCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2F3FC),
    onSecondaryContainer = Color(0xFF002026),
    tertiary = DaylightAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE5B4),
    onTertiaryContainer = Color(0xFF2B1900),
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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexusTypography,
        shapes = NexusShapes,
        content = content
    )
}
