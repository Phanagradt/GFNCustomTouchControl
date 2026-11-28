package com.example.data

import android.content.Context
import com.example.model.ButtonStyleOverride
import com.example.model.ButtonThemePreset
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-backed local repository for Button Theme Presets (Drafts + Downloaded Workshop themes)
 * and the currently Active Button Theme ID applied automatically when adding controls in Layout Studio.
 */
class ButtonThemeLocalStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "button_themes").apply { mkdirs() }
    private val themesFile = File(dir, "themes_v1.json")
    private val activeIdFile = File(dir, "active_theme_id.txt")
    private val autoDraftFile = File(dir, "auto_draft_v1.json")

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, ButtonThemePreset::class.java)
    private val listAdapter = moshi.adapter<List<ButtonThemePreset>>(listType)
    private val singleAdapter = moshi.adapter(ButtonThemePreset::class.java)

    private val _localThemes = MutableStateFlow<List<ButtonThemePreset>>(emptyList())
    val localThemes: StateFlow<List<ButtonThemePreset>> = _localThemes.asStateFlow()

    private val _activeThemeId = MutableStateFlow<String?>(null)
    val activeThemeId: StateFlow<String?> = _activeThemeId.asStateFlow()

    private val _autoSavedDraft = MutableStateFlow<ButtonThemePreset?>(null)
    val autoSavedDraft: StateFlow<ButtonThemePreset?> = _autoSavedDraft.asStateFlow()

    init {
        reloadSync()
    }

    fun getActiveTheme(): ButtonThemePreset? {
        val id = _activeThemeId.value ?: return null
        return _localThemes.value.firstOrNull { it.id == id }
    }

    fun reloadSync() {
        val loaded = runCatching {
            if (themesFile.exists()) {
                val raw = themesFile.readText()
                listAdapter.fromJson(raw).orEmpty()
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())

        val seeded = if (loaded.isEmpty()) {
            val defaults = defaultStarterThemes()
            writeThemesSync(defaults)
            defaults
        } else {
            loaded
        }
        _localThemes.value = seeded

        val active = runCatching {
            if (activeIdFile.exists()) activeIdFile.readText().trim().takeIf { it.isNotEmpty() } else null
        }.getOrNull()
        _activeThemeId.value = active?.takeIf { id -> seeded.any { it.id == id } }

        _autoSavedDraft.value = runCatching {
            if (autoDraftFile.exists()) singleAdapter.fromJson(autoDraftFile.readText()) else null
        }.getOrNull()
    }

    fun saveAutoDraftSync(theme: ButtonThemePreset) {
        val updated = theme.copy(isDraft = true, isPublished = false, updatedAtMs = System.currentTimeMillis())
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "auto_draft_v1.json.tmp")
            val json = singleAdapter.toJson(updated)
            tmp.writeText(json)
            if (!tmp.renameTo(autoDraftFile)) {
                autoDraftFile.writeText(json)
                tmp.delete()
            }
        }
        _autoSavedDraft.value = updated
        // Also upsert into local drafts list so it is never lost even if the user navigates elsewhere
        val next = _localThemes.value
            .filterNot { it.id == updated.id }
            .let { listOf(updated) + it }
            .sortedByDescending { it.updatedAtMs }
        writeThemesSync(next)
        _localThemes.value = next
    }

    suspend fun saveAutoDraft(theme: ButtonThemePreset) = withContext(Dispatchers.IO) {
        saveAutoDraftSync(theme)
    }

    suspend fun clearAutoDraft() = withContext(Dispatchers.IO) {
        runCatching { if (autoDraftFile.exists()) autoDraftFile.delete() }
        _autoSavedDraft.value = null
    }

    suspend fun upsertTheme(theme: ButtonThemePreset) = withContext(Dispatchers.IO) {
        val updated = theme.copy(updatedAtMs = System.currentTimeMillis())
        val next = _localThemes.value
            .filterNot { it.id == updated.id }
            .let { listOf(updated) + it }
            .sortedByDescending { it.updatedAtMs }
        writeThemesSync(next)
        _localThemes.value = next
    }

    suspend fun deleteTheme(themeId: String) = withContext(Dispatchers.IO) {
        val next = _localThemes.value.filterNot { it.id == themeId }
        writeThemesSync(next)
        _localThemes.value = next
        if (_activeThemeId.value == themeId) {
            setActiveThemeId(null)
        }
        if (_autoSavedDraft.value?.id == themeId) {
            runCatching { if (autoDraftFile.exists()) autoDraftFile.delete() }
            _autoSavedDraft.value = null
        }
    }

    suspend fun setActiveThemeId(themeId: String?) = withContext(Dispatchers.IO) {
        val valid = themeId?.takeIf { id -> _localThemes.value.any { it.id == id } }
        runCatching {
            if (valid == null) {
                if (activeIdFile.exists()) activeIdFile.delete()
            } else {
                activeIdFile.writeText(valid)
            }
        }
        _activeThemeId.value = valid
    }

    private fun writeThemesSync(themes: List<ButtonThemePreset>) {
        runCatching {
            dir.mkdirs()
            val tmp = File(dir, "themes_v1.json.tmp")
            tmp.writeText(listAdapter.toJson(themes))
            if (!tmp.renameTo(themesFile)) {
                themesFile.writeText(listAdapter.toJson(themes))
                tmp.delete()
            }
        }
    }

    companion object {
        private fun defaultStarterThemes(): List<ButtonThemePreset> = listOf(
            ButtonThemePreset(
                id = "builtin_dbd_bloodweb",
                name = "DBD Fog & Bloodweb",
                gameTag = "Dead by Daylight",
                description = "Dark obsidian fog backdrop with crimson Entity strokes and crisp skill-check accents. Built-in starter preset — clone or create your own!",
                category = "Horror",
                ownerId = "builtin",
                authorName = "XenosPad Studio",
                isDraft = false,
                isPublished = false,
                defaultStyle = ButtonStyleOverride(
                    shape = ControlShape.HEX,
                    accent = ControlAccent.CORAL,
                    showBackground = true,
                    showStroke = true,
                    strokeWidthDp = 2.4f,
                    customBackgroundColorHex = 0xFF2B0B12L,
                    customStrokeColorHex = 0xFFE53935L
                ),
                buttonStyles = mapOf(
                    "key_space" to ButtonStyleOverride(
                        shape = ControlShape.CIRCLE,
                        accent = ControlAccent.CORAL,
                        sizeDp = 74f,
                        showBackground = true,
                        showStroke = true,
                        strokeWidthDp = 3.0f,
                        customBackgroundColorHex = 0xFF4A1019L,
                        customStrokeColorHex = 0xFFFFCA28L,
                        customLabel = "SKILL"
                    ),
                    "key_shift" to ButtonStyleOverride(
                        shape = ControlShape.PADDLE,
                        accent = ControlAccent.AMBER,
                        showBackground = true,
                        showStroke = true,
                        strokeWidthDp = 2.2f,
                        customBackgroundColorHex = 0xFF2B0B12L,
                        customStrokeColorHex = 0xFFFF7043L,
                        customLabel = "RUN"
                    ),
                    "mouse_left" to ButtonStyleOverride(
                        shape = ControlShape.CIRCLE,
                        accent = ControlAccent.CORAL,
                        sizeDp = 68f,
                        showBackground = true,
                        showStroke = true,
                        strokeWidthDp = 2.6f,
                        customBackgroundColorHex = 0xFF4A1019L,
                        customStrokeColorHex = 0xFFE53935L,
                        customLabel = "M1"
                    )
                ),
                updatedAtMs = 1_700_000_000_000L
            ),
            ButtonThemePreset(
                id = "builtin_stealth_minimal",
                name = "Stealth Borderless Glass",
                gameTag = "Universal",
                description = "Clean borderless icon-ready layout style with subtle dark tint and zero clutter.",
                category = "Minimal",
                ownerId = "builtin",
                authorName = "XenosPad Studio",
                isDraft = false,
                isPublished = false,
                defaultStyle = ButtonStyleOverride(
                    shape = ControlShape.CIRCLE,
                    accent = ControlAccent.SLATE,
                    opacity = 0.72f,
                    showBackground = true,
                    showStroke = false,
                    customBackgroundColorHex = 0xFF05070AL
                ),
                updatedAtMs = 1_699_000_000_000L
            )
        )
    }
}
