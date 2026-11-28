package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ButtonStyleOverride(
    val shape: ControlShape? = null,
    val accent: ControlAccent? = null,
    val sizeDp: Float? = null,
    val widthScale: Float = 1.0f,
    val heightScale: Float = 1.0f,
    val opacity: Float? = null,
    val showBackground: Boolean = true,
    val showStroke: Boolean = true,
    val strokeWidthDp: Float = 1.8f,
    val customBackgroundColorHex: Long? = null,
    val customStrokeColorHex: Long? = null,
    val customImageBase64: String? = null,
    val customImageScale: Float = 1.0f,
    val customImageOpacity: Float = 1.0f,
    val customBackgroundBase64: String? = null,
    val customBackgroundOpacity: Float = 0.90f,
    val customStrokeBase64: String? = null,
    val customStrokeOpacity: Float = 1.0f,
    val blankLabel: Boolean = false,
    val customLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class ButtonThemePreset(
    val id: String,
    val name: String,
    val gameTag: String = "Universal",
    val description: String = "",
    val category: String = "Custom",
    val ownerId: String = "",
    val authorName: String = "",
    val authorAvatarBase64: String? = null,
    val isDraft: Boolean = true,
    val isPublished: Boolean = false,
    val defaultStyle: ButtonStyleOverride = ButtonStyleOverride(),
    val buttonStyles: Map<String, ButtonStyleOverride> = emptyMap(),
    val likesCount: Int = 0,
    val favoritesCount: Int = 0,
    val downloadsCount: Int = 0,
    val commentsCount: Int = 0,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    /**
     * Resolves the effective style for a given binding ID by merging the theme's [defaultStyle]
     * with any per-button override configured by the theme creator.
     */
    fun resolveStyleFor(bindingId: String): ButtonStyleOverride {
        val specific = buttonStyles[bindingId] ?: return defaultStyle
        return ButtonStyleOverride(
            shape = specific.shape ?: defaultStyle.shape,
            accent = specific.accent ?: defaultStyle.accent,
            sizeDp = specific.sizeDp ?: defaultStyle.sizeDp,
            widthScale = if (specific.widthScale != 1.0f) specific.widthScale else defaultStyle.widthScale,
            heightScale = if (specific.heightScale != 1.0f) specific.heightScale else defaultStyle.heightScale,
            opacity = specific.opacity ?: defaultStyle.opacity,
            showBackground = specific.showBackground,
            showStroke = specific.showStroke,
            strokeWidthDp = specific.strokeWidthDp,
            customBackgroundColorHex = specific.customBackgroundColorHex ?: defaultStyle.customBackgroundColorHex,
            customStrokeColorHex = specific.customStrokeColorHex ?: defaultStyle.customStrokeColorHex,
            customImageBase64 = specific.customImageBase64 ?: defaultStyle.customImageBase64,
            customImageScale = if (specific.customImageBase64 != null) specific.customImageScale else defaultStyle.customImageScale,
            customImageOpacity = if (specific.customImageBase64 != null) specific.customImageOpacity else defaultStyle.customImageOpacity,
            customBackgroundBase64 = specific.customBackgroundBase64 ?: defaultStyle.customBackgroundBase64,
            customBackgroundOpacity = if (specific.customBackgroundBase64 != null) specific.customBackgroundOpacity else defaultStyle.customBackgroundOpacity,
            customStrokeBase64 = specific.customStrokeBase64 ?: defaultStyle.customStrokeBase64,
            customStrokeOpacity = if (specific.customStrokeBase64 != null) specific.customStrokeOpacity else defaultStyle.customStrokeOpacity,
            blankLabel = specific.blankLabel || defaultStyle.blankLabel,
            customLabel = specific.customLabel ?: defaultStyle.customLabel
        )
    }

    /**
     * Applies this Button Theme preset automatically to a [VirtualControl].
     * Preserves cluster shapes (analog stick ring, touchpad rect, D-pad cross) unless the control is a button/paddle.
     */
    fun applyToControl(control: VirtualControl): VirtualControl {
        val style = resolveStyleFor(control.binding.id)
        val isButtonLike = control.controlType == ControlType.BUTTON ||
            control.controlType == ControlType.SHOULDER_PADDLE
        val resolvedShape = if (isButtonLike && style.shape != null) style.shape else control.shape
        val resolvedLabel = when {
            style.blankLabel -> ""
            !style.customLabel.isNullOrBlank() -> style.customLabel.take(12)
            else -> control.label
        }
        return control.copy(
            label = resolvedLabel,
            shape = resolvedShape,
            accent = style.accent ?: control.accent,
            sizeDp = if (isButtonLike && style.sizeDp != null) style.sizeDp.coerceIn(48f, 160f) else control.sizeDp,
            widthScale = style.widthScale.coerceIn(0.45f, 2.6f),
            heightScale = style.heightScale.coerceIn(0.45f, 2.6f),
            opacity = style.opacity?.coerceIn(0.25f, 1.0f) ?: control.opacity,
            showBackground = style.showBackground,
            showStroke = style.showStroke,
            strokeWidthDp = style.strokeWidthDp.coerceIn(0.5f, 6.0f),
            customBackgroundColorHex = style.customBackgroundColorHex,
            customStrokeColorHex = style.customStrokeColorHex,
            customImageBase64 = style.customImageBase64,
            customImageScale = style.customImageScale.coerceIn(0.30f, 1.80f),
            customImageOpacity = style.customImageOpacity.coerceIn(0.10f, 1.0f),
            customBackgroundBase64 = style.customBackgroundBase64,
            customBackgroundOpacity = style.customBackgroundOpacity.coerceIn(0.10f, 1.0f),
            customStrokeBase64 = style.customStrokeBase64,
            customStrokeOpacity = style.customStrokeOpacity.coerceIn(0.10f, 1.0f)
        )
    }
}

data class CommunityUserProfile(
    val uid: String = "",
    val username: String = "Player",
    val bio: String = "",
    val avatarBase64: String? = null,
    val avatarUrl: String? = null,
    val adminRole: Boolean = false,
    val verifiedRole: Boolean = false,
    val equippedFrameId: String? = null,
    val equippedNameTagId: String? = null,
    val equippedVerificationIconId: String? = null,
    val themesCount: Int = 0,
    val clipsCount: Int = 0,
    val postsCount: Int = 0,
    val updatedAtMs: Long = 0L
)

data class ThemeComment(
    val id: String = "",
    val themeId: String = "",
    val authorUid: String = "",
    val authorName: String = "Player",
    val authorAvatarBase64: String? = null,
    val text: String = "",
    val createdAtMs: Long = 0L
)

data class ProfileClipPost(
    val id: String = "",
    val ownerId: String = "",
    val authorName: String = "Player",
    val authorAvatarBase64: String? = null,
    val title: String = "",
    val gameTag: String = "",
    val caption: String = "",
    val durationMs: Long = 0L,
    val thumbnailBase64: String? = null,
    val storyboardFrames: List<String> = emptyList(),
    val localMediaUri: String? = null,
    val likesCount: Int = 0,
    val createdAtMs: Long = 0L
)

data class CommunityTopicPost(
    val id: String = "",
    val ownerId: String = "",
    val authorName: String = "Player",
    val authorAvatarBase64: String? = null,
    val topic: String = "General",
    val title: String = "",
    val body: String = "",
    val imageBase64: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAtMs: Long = 0L
)

data class PostComment(
    val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val authorName: String = "Player",
    val authorAvatarBase64: String? = null,
    val text: String = "",
    val createdAtMs: Long = 0L
)

object CommunityConstants {
    val THEME_CATEGORIES = listOf("All", "Horror", "FPS", "Action", "Minimal", "Anime", "Retro", "Custom")
    val DISCUSSION_TOPICS = listOf("All", "Dead by Daylight", "Layouts", "Filters", "Controller Help", "Theme Showcase", "General")

    val PRESET_BG_COLORS: List<Pair<String, Long>> = listOf(
        "Obsidian" to 0xFF0E141EL,
        "Pitch Black" to 0xFF05070AL,
        "Blood Fog" to 0xFF2B0B12L,
        "Crimson" to 0xFF4A1019L,
        "Deep Navy" to 0xFF0D1D38L,
        "Emerald Dark" to 0xFF0B291EL,
        "Amber Forge" to 0xFF332008L,
        "Void Purple" to 0xFF1F1133L,
        "Steel Gray" to 0xFF283242L
    )

    val PRESET_STROKE_COLORS: List<Pair<String, Long>> = listOf(
        "GeForce Lime" to 0xFF8CE000L,
        "DBD Crimson" to 0xFFE53935L,
        "Entity Ember" to 0xFFFF7043L,
        "Tactical Cyan" to 0xFF00E5FFL,
        "Pure White" to 0xFFF5F8FFL,
        "Gold Trim" to 0xFFFFCA28L,
        "Neon Violet" to 0xFFB388FFL,
        "Rose Pink" to 0xFFFF5277L,
        "Stealth Slate" to 0xFF9FB0C7L
    )
}
