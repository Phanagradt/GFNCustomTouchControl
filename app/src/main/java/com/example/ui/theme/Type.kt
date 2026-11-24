package com.example.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import java.io.File

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Medium),
    Font(R.font.orbitron, FontWeight.SemiBold),
    Font(R.font.orbitron, FontWeight.Bold),
)

val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Normal),
    Font(R.font.rajdhani, FontWeight.Medium),
    Font(R.font.rajdhani, FontWeight.SemiBold),
    Font(R.font.rajdhani, FontWeight.Bold),
)

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold),
)

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold),
)

@Volatile
private var cachedCustomFontPath: String = ""

@Volatile
private var cachedCustomFontFamily: FontFamily? = null

private fun loadCustomFontFamily(path: String): FontFamily? {
    if (path.isBlank()) return null
    if (path == cachedCustomFontPath && cachedCustomFontFamily != null) {
        return cachedCustomFontFamily
    }
    val file = File(path)
    if (!file.exists() || !file.isFile) return null
    val loaded = runCatching {
        val tf = Typeface.createFromFile(file)
        if (tf != null && tf != Typeface.DEFAULT) FontFamily(tf) else null
    }.getOrNull()
    if (loaded != null) {
        cachedCustomFontPath = path
        cachedCustomFontFamily = loaded
    }
    return loaded
}

fun buildNexusTypography(fontConfig: AppFontConfig = AppFontConfig()): Typography {
    val (displayFamily, bodyFamily, labelMonoFamily) = when (fontConfig.preset) {
        AppFontPreset.NEXUS_DEFAULT -> Triple(SpaceGroteskFontFamily, InterFontFamily, JetBrainsMonoFontFamily)
        AppFontPreset.CYBER_ORBITRON -> Triple(OrbitronFontFamily, InterFontFamily, OrbitronFontFamily)
        AppFontPreset.TACTICAL_RAJDHANI -> Triple(RajdhaniFontFamily, RajdhaniFontFamily, JetBrainsMonoFontFamily)
        AppFontPreset.MODERN_POPPINS -> Triple(PoppinsFontFamily, PoppinsFontFamily, JetBrainsMonoFontFamily)
        AppFontPreset.ROUNDED_NUNITO -> Triple(NunitoFontFamily, NunitoFontFamily, JetBrainsMonoFontFamily)
        AppFontPreset.MONO_TERMINAL -> Triple(JetBrainsMonoFontFamily, JetBrainsMonoFontFamily, JetBrainsMonoFontFamily)
        AppFontPreset.CUSTOM_UPLOAD -> {
            val custom = loadCustomFontFamily(fontConfig.customFontPath)
            if (custom != null) {
                Triple(custom, custom, custom)
            } else {
                Triple(SpaceGroteskFontFamily, InterFontFamily, JetBrainsMonoFontFamily)
            }
        }
    }

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = labelMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelSmall = TextStyle(
            fontFamily = labelMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        )
    )
}

val NexusTypography = buildNexusTypography()
