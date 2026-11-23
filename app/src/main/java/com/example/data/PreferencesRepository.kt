package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.ControllerNotation
import com.example.model.CrosshairSettings
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.TargetPlatformMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexusDataStore by preferencesDataStore(name = "nexuspad_settings")

enum class VibrationMode(val displayName: String, val subtitle: String) {
    NONE("No Vibration", "Completely off — zero shake on press"),
    LIGHT("Light Tap", "Subtle keyboard-style feedback"),
    STRONG("Crisp Click", "Firm virtual key click")
}

data class OverlaySettings(
    val vibrationMode: VibrationMode = VibrationMode.LIGHT,
    val globalOpacity: Float = 0.85f,
    val gridSnapEnabled: Boolean = true,
    val gridDivisions: Int = 12,
    val controllerNotation: ControllerNotation = ControllerNotation.XBOX,
    val darkTheme: Boolean = true,
    val showTelemetryOverlay: Boolean = true,
    val autoLaunchGeForceNow: Boolean = false,
    val floatingKeyboardDefault: Boolean = false,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val targetMode: TargetPlatformMode = TargetPlatformMode.GEFORCE_NOW,
    val gameFilter: GameFilterState = GameFilterState.default(),
    val crosshair: CrosshairSettings = CrosshairSettings()
) {
    val hapticsEnabled: Boolean
        get() = vibrationMode != VibrationMode.NONE
}

class PreferencesRepository(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val filterAdapter = moshi.adapter(GameFilterState::class.java)
    private val crosshairAdapter = moshi.adapter(CrosshairSettings::class.java)

    private object Keys {
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val VIBRATION_MODE = stringPreferencesKey("vibration_mode")
        val GLOBAL_OPACITY = floatPreferencesKey("global_opacity")
        val GRID_SNAP = booleanPreferencesKey("grid_snap_enabled")
        val GRID_DIVISIONS = intPreferencesKey("grid_divisions")
        val NOTATION = stringPreferencesKey("controller_notation")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry_overlay")
        val AUTO_LAUNCH_GFN = booleanPreferencesKey("auto_launch_gfn")
        val FLOATING_KB = booleanPreferencesKey("floating_keyboard_default")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val TARGET_MODE = stringPreferencesKey("target_platform_mode")
        val GAME_FILTER_JSON = stringPreferencesKey("game_filter_json")
        val CROSSHAIR_JSON = stringPreferencesKey("crosshair_json")
    }

    val settingsFlow: Flow<OverlaySettings> = context.nexusDataStore.data.map { prefs ->
        val notationName = prefs[Keys.NOTATION] ?: ControllerNotation.XBOX.name
        val notation = runCatching { ControllerNotation.valueOf(notationName) }
            .getOrDefault(ControllerNotation.XBOX)

        val storedVib = prefs[Keys.VIBRATION_MODE]
        val legacyHaptics = prefs[Keys.HAPTICS]
        val vibrationMode = when {
            storedVib != null -> runCatching { VibrationMode.valueOf(storedVib) }
                .getOrDefault(VibrationMode.LIGHT)
            legacyHaptics == false -> VibrationMode.NONE
            else -> VibrationMode.LIGHT
        }

        val langName = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
        val appLanguage = runCatching { AppLanguage.valueOf(langName) }
            .getOrDefault(AppLanguage.SYSTEM)

        val targetModeName = prefs[Keys.TARGET_MODE] ?: TargetPlatformMode.GEFORCE_NOW.name
        val targetMode = runCatching { TargetPlatformMode.valueOf(targetModeName) }
            .getOrDefault(TargetPlatformMode.GEFORCE_NOW)

        val gameFilter = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
        val crosshair = decodeCrosshairSettings(prefs[Keys.CROSSHAIR_JSON])

        OverlaySettings(
            vibrationMode = vibrationMode,
            globalOpacity = prefs[Keys.GLOBAL_OPACITY] ?: 0.85f,
            gridSnapEnabled = prefs[Keys.GRID_SNAP] ?: true,
            gridDivisions = (prefs[Keys.GRID_DIVISIONS] ?: 12).coerceIn(4, 32),
            controllerNotation = notation,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            showTelemetryOverlay = prefs[Keys.SHOW_TELEMETRY] ?: true,
            autoLaunchGeForceNow = prefs[Keys.AUTO_LAUNCH_GFN] ?: false,
            floatingKeyboardDefault = prefs[Keys.FLOATING_KB] ?: false,
            appLanguage = appLanguage,
            targetMode = targetMode,
            gameFilter = gameFilter,
            crosshair = crosshair
        )
    }

    private fun decodeFilterState(raw: String?): GameFilterState {
        if (raw.isNullOrBlank()) return GameFilterState.default()
        val parsed = runCatching { filterAdapter.fromJson(raw) }.getOrNull()
        if (parsed == null || parsed.styles.isEmpty()) return GameFilterState.default()
        val normalizedStyles = (1..3).map { parsed.styleForSlot(it) }
        return parsed.copy(
            activeSlot = parsed.activeSlot.coerceIn(0, 3),
            styles = normalizedStyles
        )
    }

    private fun decodeCrosshairSettings(raw: String?): CrosshairSettings {
        if (raw.isNullOrBlank()) return CrosshairSettings()
        return runCatching { crosshairAdapter.fromJson(raw) }.getOrNull() ?: CrosshairSettings()
    }

    suspend fun setVibrationMode(mode: VibrationMode) {
        context.nexusDataStore.edit {
            it[Keys.VIBRATION_MODE] = mode.name
            it[Keys.HAPTICS] = mode != VibrationMode.NONE
        }
    }

    suspend fun setGlobalOpacity(opacity: Float) {
        context.nexusDataStore.edit { it[Keys.GLOBAL_OPACITY] = opacity.coerceIn(0.25f, 1.0f) }
    }

    suspend fun setGridSnapEnabled(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.GRID_SNAP] = enabled }
    }

    suspend fun setGridDivisions(divisions: Int) {
        context.nexusDataStore.edit { it[Keys.GRID_DIVISIONS] = divisions.coerceIn(4, 32) }
    }

    suspend fun setControllerNotation(notation: ControllerNotation) {
        context.nexusDataStore.edit { it[Keys.NOTATION] = notation.name }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.nexusDataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setShowTelemetryOverlay(show: Boolean) {
        context.nexusDataStore.edit { it[Keys.SHOW_TELEMETRY] = show }
    }

    suspend fun setAutoLaunchGeForceNow(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_LAUNCH_GFN] = enabled }
    }

    suspend fun setFloatingKeyboardDefault(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.FLOATING_KB] = enabled }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.nexusDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setTargetPlatformMode(mode: TargetPlatformMode) {
        context.nexusDataStore.edit { it[Keys.TARGET_MODE] = mode.name }
    }

    suspend fun setActiveFilterSlot(slot: Int) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val updated = current.copy(activeSlot = slot.coerceIn(0, 3))
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(updated)
        }
    }

    suspend fun updateFilterStyle(style: GameFilterStyle) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val next = current.withUpdatedStyle(style).copy(activeSlot = style.slot.coerceIn(1, 3))
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(next)
        }
    }

    suspend fun resetFilterSlot(slot: Int) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val next = current.withResetSlot(slot)
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(next)
        }
    }

    suspend fun updateCrosshair(crosshair: CrosshairSettings) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.CROSSHAIR_JSON] = crosshairAdapter.toJson(crosshair)
        }
    }
}
