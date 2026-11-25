package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import com.example.model.AppLanguage
import com.example.model.ControllerNotation
import com.example.model.CrosshairSettings
import com.example.model.FloatingBubbleConfig
import com.example.model.FloatingBubbleStyle
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GyroAimConfig
import com.example.model.PerfHudConfig
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.TargetPlatformMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.nexusDataStore by preferencesDataStore(name = "nexuspad_settings")

enum class VibrationMode(val displayName: String, val subtitle: String) {
    NONE("No Vibration", "Completely off — zero shake on press"),
    LIGHT("Light Tap", "Subtle keyboard-style feedback"),
    STRONG("Crisp Click", "Firm virtual key click")
}

data class CloudAppearanceSnapshot(
    val gameFilterJson: String,
    val crosshairJson: String,
    val colorSchemeJson: String,
    val fontConfigJson: String,
    val floatingBubbleJson: String,
    val darkTheme: Boolean,
    val clientUpdatedAt: Long
)

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
    val instantReplayDurationSec: Int = 30,
    val recordingResolution: RecordingResolution = RecordingResolution.P720,
    val recordingFps: RecordingFps = RecordingFps.FPS_30,
    val githubRepoSlug: String = "",
    val autoCheckUpdates: Boolean = true,
    val gameFilter: GameFilterState = GameFilterState.default(),
    val crosshair: CrosshairSettings = CrosshairSettings(),
    val colorScheme: AppColorSchemeConfig = AppColorSchemeConfig(),
    val fontConfig: AppFontConfig = AppFontConfig(),
    val floatingBubble: FloatingBubbleConfig = FloatingBubbleConfig(),
    val customTestBgPath: String = "",
    val lastSeenWhatsNewVersionCode: Int = 0,
    val gyroAim: GyroAimConfig = GyroAimConfig(),
    val perfHud: PerfHudConfig = PerfHudConfig(),
    val appearanceUpdatedAtMs: Long = 0L
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
    private val colorSchemeAdapter = moshi.adapter(AppColorSchemeConfig::class.java)
    private val fontConfigAdapter = moshi.adapter(AppFontConfig::class.java)
    private val floatingBubbleAdapter = moshi.adapter(FloatingBubbleConfig::class.java)
    private val gyroAimAdapter = moshi.adapter(GyroAimConfig::class.java)
    private val perfHudAdapter = moshi.adapter(PerfHudConfig::class.java)

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
        val REPLAY_DURATION_SEC = intPreferencesKey("instant_replay_duration_sec")
        val RECORDING_RESOLUTION = stringPreferencesKey("recording_resolution")
        val RECORDING_FPS = stringPreferencesKey("recording_fps")
        val GITHUB_REPO_SLUG = stringPreferencesKey("github_repo_slug")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val GAME_FILTER_JSON = stringPreferencesKey("game_filter_json")
        val CROSSHAIR_JSON = stringPreferencesKey("crosshair_json")
        val COLOR_SCHEME_JSON = stringPreferencesKey("color_scheme_json")
        val FONT_CONFIG_JSON = stringPreferencesKey("font_config_json")
        val FLOATING_BUBBLE_JSON = stringPreferencesKey("floating_bubble_json")
        val CUSTOM_TEST_BG_PATH = stringPreferencesKey("custom_test_bg_path")
        val LAST_SEEN_WHATS_NEW_CODE = intPreferencesKey("last_seen_whats_new_version_code")
        val GYRO_AIM_JSON = stringPreferencesKey("gyro_aim_json")
        val PERF_HUD_JSON = stringPreferencesKey("perf_hud_json")
        val APPEARANCE_UPDATED_AT = longPreferencesKey("appearance_updated_at_ms")
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

        val recResName = prefs[Keys.RECORDING_RESOLUTION] ?: RecordingResolution.P720.name
        val recordingResolution = runCatching { RecordingResolution.valueOf(recResName) }
            .getOrDefault(RecordingResolution.P720)

        val recFpsName = prefs[Keys.RECORDING_FPS] ?: RecordingFps.FPS_30.name
        val recordingFps = runCatching { RecordingFps.valueOf(recFpsName) }
            .getOrDefault(RecordingFps.FPS_30)

        val gameFilter = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
        val crosshair = decodeCrosshairSettings(prefs[Keys.CROSSHAIR_JSON])
        val colorScheme = decodeColorScheme(prefs[Keys.COLOR_SCHEME_JSON])
        val fontConfig = decodeFontConfig(prefs[Keys.FONT_CONFIG_JSON])
        val floatingBubble = decodeFloatingBubble(prefs[Keys.FLOATING_BUBBLE_JSON])
        val gyroAim = decodeGyroAim(prefs[Keys.GYRO_AIM_JSON])
        val perfHud = decodePerfHud(prefs[Keys.PERF_HUD_JSON])

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
            instantReplayDurationSec = (prefs[Keys.REPLAY_DURATION_SEC] ?: 30).coerceIn(10, 3600),
            recordingResolution = recordingResolution,
            recordingFps = recordingFps,
            githubRepoSlug = prefs[Keys.GITHUB_REPO_SLUG] ?: "",
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            gameFilter = gameFilter,
            crosshair = crosshair,
            colorScheme = colorScheme,
            fontConfig = fontConfig,
            floatingBubble = floatingBubble,
            customTestBgPath = prefs[Keys.CUSTOM_TEST_BG_PATH] ?: "",
            lastSeenWhatsNewVersionCode = prefs[Keys.LAST_SEEN_WHATS_NEW_CODE] ?: 0,
            gyroAim = gyroAim,
            perfHud = perfHud,
            appearanceUpdatedAtMs = prefs[Keys.APPEARANCE_UPDATED_AT] ?: 0L
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

    private fun decodeColorScheme(raw: String?): AppColorSchemeConfig {
        if (raw.isNullOrBlank()) return AppColorSchemeConfig()
        return runCatching { colorSchemeAdapter.fromJson(raw) }.getOrNull() ?: AppColorSchemeConfig()
    }

    private fun decodeFontConfig(raw: String?): AppFontConfig {
        if (raw.isNullOrBlank()) return AppFontConfig()
        return runCatching { fontConfigAdapter.fromJson(raw) }.getOrNull() ?: AppFontConfig()
    }

    private fun decodeFloatingBubble(raw: String?): FloatingBubbleConfig {
        if (raw.isNullOrBlank()) return FloatingBubbleConfig()
        return runCatching { floatingBubbleAdapter.fromJson(raw) }.getOrNull() ?: FloatingBubbleConfig()
    }

    private fun decodeGyroAim(raw: String?): GyroAimConfig {
        if (raw.isNullOrBlank()) return GyroAimConfig()
        return runCatching { gyroAimAdapter.fromJson(raw) }.getOrNull() ?: GyroAimConfig()
    }

    private fun decodePerfHud(raw: String?): PerfHudConfig {
        if (raw.isNullOrBlank()) return PerfHudConfig()
        return runCatching { perfHudAdapter.fromJson(raw) }.getOrNull() ?: PerfHudConfig()
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
        context.nexusDataStore.edit {
            it[Keys.DARK_THEME] = dark
            it[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
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

    suspend fun setInstantReplayDurationSec(seconds: Int) {
        context.nexusDataStore.edit { it[Keys.REPLAY_DURATION_SEC] = seconds.coerceIn(10, 3600) }
    }

    suspend fun setRecordingResolution(resolution: RecordingResolution) {
        context.nexusDataStore.edit { it[Keys.RECORDING_RESOLUTION] = resolution.name }
    }

    suspend fun setRecordingFps(fps: RecordingFps) {
        context.nexusDataStore.edit { it[Keys.RECORDING_FPS] = fps.name }
    }

    suspend fun setGithubRepoSlug(slug: String) {
        context.nexusDataStore.edit { it[Keys.GITHUB_REPO_SLUG] = slug.trim() }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.nexusDataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setActiveFilterSlot(slot: Int) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val updated = current.copy(activeSlot = slot.coerceIn(0, 3))
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(updated)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateFilterStyle(style: GameFilterStyle) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val next = current.withUpdatedStyle(style).copy(activeSlot = style.slot.coerceIn(1, 3))
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(next)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun resetFilterSlot(slot: Int) {
        context.nexusDataStore.edit { prefs ->
            val current = decodeFilterState(prefs[Keys.GAME_FILTER_JSON])
            val next = current.withResetSlot(slot)
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(next)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateCrosshair(crosshair: CrosshairSettings) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.CROSSHAIR_JSON] = crosshairAdapter.toJson(crosshair)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateColorScheme(config: AppColorSchemeConfig) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.COLOR_SCHEME_JSON] = colorSchemeAdapter.toJson(config)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateFontConfig(config: AppFontConfig) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.FONT_CONFIG_JSON] = fontConfigAdapter.toJson(config)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateFloatingBubble(config: FloatingBubbleConfig) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.FLOATING_BUBBLE_JSON] = floatingBubbleAdapter.toJson(config)
            prefs[Keys.APPEARANCE_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updateGyroAim(config: GyroAimConfig) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.GYRO_AIM_JSON] = gyroAimAdapter.toJson(config)
        }
    }

    suspend fun updatePerfHud(config: PerfHudConfig) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.PERF_HUD_JSON] = perfHudAdapter.toJson(config)
        }
    }

    suspend fun setCustomTestBgPath(path: String) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_TEST_BG_PATH] = path
        }
    }

    suspend fun setLastSeenWhatsNewVersionCode(versionCode: Int) {
        context.nexusDataStore.edit { prefs ->
            prefs[Keys.LAST_SEEN_WHATS_NEW_CODE] = versionCode
        }
    }

    /**
     * Exports Filters (all 3 Freestyle slots), Crosshair, Color Scheme, Font Preset,
     * Floating Bubble style, and Dark Theme into size-bounded JSON for Cloud Save.
     */
    suspend fun exportCloudAppearanceSnapshot(): CloudAppearanceSnapshot {
        val current = settingsFlow.first()
        var filterJson = filterAdapter.toJson(current.gameFilter)
        if (filterJson.length > MAX_CLOUD_FILTER_JSON_CHARS) {
            val stripped = current.gameFilter.copy(
                styles = current.gameFilter.styles.map { it.copy(customStickerBase64 = null) }
            )
            filterJson = filterAdapter.toJson(stripped)
        }

        var crosshairJson = crosshairAdapter.toJson(current.crosshair)
        if (crosshairJson.length > MAX_CLOUD_CROSSHAIR_JSON_CHARS) {
            crosshairJson = crosshairAdapter.toJson(current.crosshair.copy(customImageBase64 = null))
        }

        val safeFont = current.fontConfig.copy(
            preset = if (current.fontConfig.preset == AppFontPreset.CUSTOM_UPLOAD) {
                AppFontPreset.NEXUS_DEFAULT
            } else {
                current.fontConfig.preset
            },
            customFontPath = "",
            customFontName = ""
        )
        val safeBubble = current.floatingBubble.copy(
            style = if (current.floatingBubble.style == FloatingBubbleStyle.CUSTOM_IMAGE) {
                FloatingBubbleStyle.TACTICAL_RING
            } else {
                current.floatingBubble.style
            },
            customIconPath = ""
        )

        return CloudAppearanceSnapshot(
            gameFilterJson = filterJson.take(MAX_CLOUD_FILTER_JSON_CHARS),
            crosshairJson = crosshairJson.take(MAX_CLOUD_CROSSHAIR_JSON_CHARS),
            colorSchemeJson = colorSchemeAdapter.toJson(current.colorScheme).take(8_000),
            fontConfigJson = fontConfigAdapter.toJson(safeFont).take(4_000),
            floatingBubbleJson = floatingBubbleAdapter.toJson(safeBubble).take(4_000),
            darkTheme = current.darkTheme,
            clientUpdatedAt = current.appearanceUpdatedAtMs
        )
    }

    /**
     * Applies a Cloud Save appearance snapshot atomically to DataStore.
     */
    suspend fun applyCloudAppearanceSnapshot(snapshot: CloudAppearanceSnapshot) {
        val parsedFilter = decodeFilterState(snapshot.gameFilterJson)
        val parsedCrosshair = decodeCrosshairSettings(snapshot.crosshairJson)
        val parsedColor = decodeColorScheme(snapshot.colorSchemeJson)
        val parsedFont = decodeFontConfig(snapshot.fontConfigJson)
        val parsedBubble = decodeFloatingBubble(snapshot.floatingBubbleJson)

        context.nexusDataStore.edit { prefs ->
            prefs[Keys.GAME_FILTER_JSON] = filterAdapter.toJson(parsedFilter)
            prefs[Keys.CROSSHAIR_JSON] = crosshairAdapter.toJson(parsedCrosshair)
            prefs[Keys.COLOR_SCHEME_JSON] = colorSchemeAdapter.toJson(parsedColor)
            prefs[Keys.FONT_CONFIG_JSON] = fontConfigAdapter.toJson(parsedFont)
            prefs[Keys.FLOATING_BUBBLE_JSON] = floatingBubbleAdapter.toJson(parsedBubble)
            prefs[Keys.DARK_THEME] = snapshot.darkTheme
            prefs[Keys.APPEARANCE_UPDATED_AT] = snapshot.clientUpdatedAt.coerceAtLeast(1L)
        }
    }

    companion object {
        private const val MAX_CLOUD_FILTER_JSON_CHARS = 155_000
        private const val MAX_CLOUD_CROSSHAIR_JSON_CHARS = 155_000
    }
}

