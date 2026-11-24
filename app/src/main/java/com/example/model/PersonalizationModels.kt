package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.squareup.moshi.JsonClass
import java.io.File
import java.io.FileOutputStream

enum class AppThemePreset(
    val displayName: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long
) {
    GEFORCE_LIME("GeForce Lime", 0xFF8CE000, 0xFF00E5FF, 0xFFFFB300),
    CYBER_PINK("Cyber Neon", 0xFFFF3D9A, 0xFF00F0FF, 0xFFB388FF),
    SOLAR_GOLD("Solar Gold", 0xFFFFB300, 0xFFFF6D00, 0xFF00E5FF),
    CRIMSON_OPS("Crimson Ops", 0xFFFF4D6D, 0xFFFFB703, 0xFF38BDF8),
    ARCTIC_ICE("Arctic Ice", 0xFF38BDF8, 0xFF818CF8, 0xFF34D399),
    AMETHYST_PRO("Amethyst Pro", 0xFFA855F7, 0xFF22D3EE, 0xFFF472B6),
    EMERALD_PULSE("Emerald Pulse", 0xFF10B981, 0xFF38BDF8, 0xFFF59E0B),
    CUSTOM_MIX("Custom Mix", 0xFF8CE000, 0xFF00E5FF, 0xFFFFB300)
}

@JsonClass(generateAdapter = true)
data class AppColorSchemeConfig(
    val preset: AppThemePreset = AppThemePreset.GEFORCE_LIME,
    val useThreeColors: Boolean = true,
    val customPrimaryHex: Long = 0xFF8CE000,
    val customSecondaryHex: Long = 0xFF00E5FF,
    val customTertiaryHex: Long = 0xFFFFB300
) {
    fun effectivePrimaryHex(): Long =
        if (preset == AppThemePreset.CUSTOM_MIX) customPrimaryHex else preset.primaryHex

    fun effectiveSecondaryHex(): Long =
        if (preset == AppThemePreset.CUSTOM_MIX) customSecondaryHex else preset.secondaryHex

    fun effectiveTertiaryHex(): Long = when {
        preset != AppThemePreset.CUSTOM_MIX -> preset.tertiaryHex
        useThreeColors -> customTertiaryHex
        else -> blendColors(customPrimaryHex, customSecondaryHex, 0.5f)
    }

    companion object {
        val swatchColors: List<Long> = listOf(
            0xFF8CE000, // GeForce Lime
            0xFF00E5FF, // Tactical Cyan
            0xFFFFB300, // Turbo Amber
            0xFFFF3D9A, // Cyber Pink
            0xFFFF4D6D, // Crimson
            0xFFA855F7, // Amethyst
            0xFF38BDF8, // Sky Blue
            0xFF10B981, // Emerald
            0xFFFF6D00, // Solar Orange
            0xFF6366F1, // Indigo
            0xFFFACC15, // Volt Yellow
            0xFFEC4899  // Rose
        )

        fun blendColors(c1: Long, c2: Long, ratio: Float): Long {
            val t = ratio.coerceIn(0f, 1f)
            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF
            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF
            val r = (r1 + (r2 - r1) * t).toLong().coerceIn(0, 255)
            val g = (g1 + (g2 - g1) * t).toLong().coerceIn(0, 255)
            val b = (b1 + (b2 - b1) * t).toLong().coerceIn(0, 255)
            return (0xFFL shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}

enum class AppFontPreset(val displayName: String, val subtitle: String) {
    NEXUS_DEFAULT("Nexus Default", "Space Grotesk + Inter"),
    CYBER_ORBITRON("Orbitron Sci-Fi", "Futuristic geometric HUD"),
    TACTICAL_RAJDHANI("Rajdhani Tactical", "Crisp esports condensed"),
    MODERN_POPPINS("Poppins Modern", "Clean geometric rounded"),
    ROUNDED_NUNITO("Nunito Soft", "Friendly smooth strokes"),
    MONO_TERMINAL("JetBrains Mono", "Developer monospace HUD"),
    CUSTOM_UPLOAD("Custom Font (.ttf/.otf)", "Uploaded from DaFont or device")
}

@JsonClass(generateAdapter = true)
data class AppFontConfig(
    val preset: AppFontPreset = AppFontPreset.NEXUS_DEFAULT,
    val customFontPath: String = "",
    val customFontName: String = ""
)

enum class FloatingBubbleStyle(val displayName: String) {
    TACTICAL_RING("Tactical Ring"),
    CYBER_PILL("Cyber Pill"),
    HEX_BADGE("Hex Shield"),
    STEALTH_ORB("Stealth Orb"),
    CUSTOM_IMAGE("Custom Image")
}

@JsonClass(generateAdapter = true)
data class FloatingBubbleConfig(
    val style: FloatingBubbleStyle = FloatingBubbleStyle.TACTICAL_RING,
    val sizeDp: Float = 56f,
    val opacity: Float = 0.92f,
    val useThemeAccent: Boolean = true,
    val customAccentHex: Long = 0xFF8CE000,
    val showStatusDot: Boolean = true,
    val customIconPath: String = ""
) {
    fun clampedSizeDp(): Float = sizeDp.coerceIn(36f, 88f)
    fun clampedOpacity(): Float = opacity.coerceIn(0.25f, 1.0f)
}

object CustomAssetStorage {

    fun saveCustomFontFromUri(context: Context, uri: Uri): Result<Pair<String, String>> {
        return runCatching {
            val displayName = resolveDisplayName(context, uri) ?: "custom_font.ttf"
            val ext = displayName.substringAfterLast('.', "ttf").lowercase()
            val safeExt = if (ext == "otf") "otf" else "ttf"
            val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val temp = File(dir, "font_candidate_${System.currentTimeMillis()}.$safeExt")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { out -> input.copyTo(out) }
            } ?: error("Cannot read selected font file")

            if (temp.length() < 1024L || temp.length() > 25L * 1024L * 1024L) {
                temp.delete()
                error("Please select a valid .ttf or .otf font file (under 25 MB)")
            }

            // Validate that Android's native Typeface engine can actually parse the TTF/OTF file
            val loaded = runCatching { Typeface.createFromFile(temp) }.getOrNull()
            if (loaded == null || loaded == Typeface.DEFAULT) {
                temp.delete()
                error("Invalid or unsupported font file — use .ttf or .otf (unzip DaFont .zip files first)")
            }

            // Replace active custom font cleanly
            dir.listFiles()?.forEach { f ->
                if (f != temp) f.delete()
            }
            val cleanName = displayName.substringBeforeLast('.').take(40).ifBlank { "Custom Font" }
            temp.absolutePath to cleanName
        }
    }

    fun saveCustomTestBackgroundFromUri(context: Context, uri: Uri): Result<String> {
        return runCatching {
            val bitmap = decodeScaledBitmap(context, uri, maxEdge = 1600)
                ?: error("Could not decode selected image")
            val dir = File(context.filesDir, "custom_backgrounds").apply { mkdirs() }
            val outFile = File(dir, "test_bg_${System.currentTimeMillis()}.jpg")
            dir.listFiles()?.forEach { old ->
                if (old.name.startsWith("test_bg_")) old.delete()
            }
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
            outFile.absolutePath
        }
    }

    fun saveCustomBubbleIconFromUri(context: Context, uri: Uri): Result<String> {
        return runCatching {
            val raw = decodeScaledBitmap(context, uri, maxEdge = 320)
                ?: error("Could not decode selected image")
            val side = minOf(raw.width, raw.height).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(
                raw,
                (raw.width - side) / 2,
                (raw.height - side) / 2,
                side,
                side
            )
            if (cropped != raw) raw.recycle()
            val dir = File(context.filesDir, "custom_backgrounds").apply { mkdirs() }
            val outFile = File(dir, "bubble_icon_${System.currentTimeMillis()}.png")
            dir.listFiles()?.forEach { old ->
                if (old.name.startsWith("bubble_icon_")) old.delete()
            }
            FileOutputStream(outFile).use { out ->
                cropped.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            cropped.recycle()
            outFile.absolutePath
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxEdge: Int): Bitmap? {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val maxDim = maxOf(w, h)
                if (maxDim > maxEdge) {
                    val ratio = maxEdge.toFloat() / maxDim.toFloat()
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return null

        val maxDim = maxOf(raw.width, raw.height)
        return if (maxDim > maxEdge) {
            val ratio = maxEdge.toFloat() / maxDim.toFloat()
            val scaled = Bitmap.createScaledBitmap(
                raw,
                (raw.width * ratio).toInt().coerceAtLeast(1),
                (raw.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != raw) raw.recycle()
            scaled
        } else {
            raw
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
