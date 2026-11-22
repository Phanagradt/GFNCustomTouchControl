package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ControllerNotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexusDataStore by preferencesDataStore(name = "nexuspad_settings")

data class OverlaySettings(
    val hapticsEnabled: Boolean = true,
    val globalOpacity: Float = 0.85f,
    val gridSnapEnabled: Boolean = true,
    val controllerNotation: ControllerNotation = ControllerNotation.XBOX,
    val darkTheme: Boolean = true,
    val showTelemetryOverlay: Boolean = true,
    val autoLaunchGeForceNow: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val GLOBAL_OPACITY = floatPreferencesKey("global_opacity")
        val GRID_SNAP = booleanPreferencesKey("grid_snap_enabled")
        val NOTATION = stringPreferencesKey("controller_notation")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SHOW_TELEMETRY = booleanPreferencesKey("show_telemetry_overlay")
        val AUTO_LAUNCH_GFN = booleanPreferencesKey("auto_launch_gfn")
    }

    val settingsFlow: Flow<OverlaySettings> = context.nexusDataStore.data.map { prefs ->
        val notationName = prefs[Keys.NOTATION] ?: ControllerNotation.XBOX.name
        val notation = runCatching { ControllerNotation.valueOf(notationName) }
            .getOrDefault(ControllerNotation.XBOX)
        OverlaySettings(
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            globalOpacity = prefs[Keys.GLOBAL_OPACITY] ?: 0.85f,
            gridSnapEnabled = prefs[Keys.GRID_SNAP] ?: true,
            controllerNotation = notation,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            showTelemetryOverlay = prefs[Keys.SHOW_TELEMETRY] ?: true,
            autoLaunchGeForceNow = prefs[Keys.AUTO_LAUNCH_GFN] ?: false
        )
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setGlobalOpacity(opacity: Float) {
        context.nexusDataStore.edit { it[Keys.GLOBAL_OPACITY] = opacity.coerceIn(0.25f, 1.0f) }
    }

    suspend fun setGridSnapEnabled(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.GRID_SNAP] = enabled }
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
}
