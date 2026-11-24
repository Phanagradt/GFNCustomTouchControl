package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.CaptureBridge
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.data.VibrationMode
import com.example.filter.GameFilterBridge
import com.example.input.InputBridge
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import com.example.model.AppLanguage
import com.example.model.BindingDomain
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerNotation
import com.example.model.ControllerProfile
import com.example.model.CrosshairPreset
import com.example.model.CrosshairSettings
import com.example.model.CrosshairStyle
import com.example.model.CustomAssetStorage
import com.example.model.FloatingBubbleConfig
import com.example.model.FloatingBubbleStyle
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.PresetProfiles
import com.example.model.ProfileTemplate
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.StickState
import com.example.model.StickerPreset
import com.example.model.TargetPlatformMode
import com.example.model.TelemetryEvent
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.update.ApkUpgradeCompatibility
import com.example.update.GitHubUpdateManager
import com.example.update.UpdateUiState
import com.example.util.ButtonImageCodec
import com.example.util.SystemIntegrations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

data class SystemReadiness(
    val canDrawOverlays: Boolean = false,
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isGeForceNowInstalled: Boolean = false,
    val canInstallPackages: Boolean = true
)

data class EditorUiState(
    val profile: ControllerProfile? = null,
    val selectedControlId: String? = null,
    val isPlayTestMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val undoStack: List<ControllerProfile> = emptyList()
) {
    val selectedControl: VirtualControl?
        get() = profile?.controls?.firstOrNull { it.id == selectedControlId }
}

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NexusDatabase.getInstance(application)
    private val profileRepo = ProfileRepository(database.profileDao())
    private val prefsRepo = PreferencesRepository(application)

    val profiles: StateFlow<List<ControllerProfile>> = profileRepo.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ControllerProfile?> = profileRepo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<OverlaySettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverlaySettings())

    val isOverlayRunning: StateFlow<Boolean> = InputBridge.isOverlayRunning
    val isImeConnected: StateFlow<Boolean> = InputBridge.isImeConnected
    val telemetry: StateFlow<List<TelemetryEvent>> = InputBridge.telemetry
    val pressedControlIds: StateFlow<Set<String>> = InputBridge.pressedControlIds
    val toggledControlIds: StateFlow<Set<String>> = InputBridge.toggledControlIds
    val leftStick: StateFlow<StickState> = InputBridge.leftStick
    val rightStick: StateFlow<StickState> = InputBridge.rightStick
    val liveFilterState: StateFlow<GameFilterState> = GameFilterBridge.liveFilterState
    val isGpuShaderActive: StateFlow<Boolean> = GameFilterBridge.isGpuShaderActive

    private val _readiness = MutableStateFlow(SystemReadiness())
    val readiness: StateFlow<SystemReadiness> = _readiness.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _hasLoadedSettings = MutableStateFlow(false)
    val hasLoadedSettings: StateFlow<Boolean> = _hasLoadedSettings.asStateFlow()

    private var updateJob: Job? = null
    private var didAutoCheckUpdates = false

    init {
        viewModelScope.launch {
            profileRepo.ensureSeeded()
        }
        viewModelScope.launch {
            prefsRepo.settingsFlow.collect { s ->
                _hasLoadedSettings.value = true
                InputBridge.setTargetMode(s.targetMode)
                CaptureBridge.setReplayDurationSec(s.instantReplayDurationSec)
                CaptureBridge.setRecordingResolution(s.recordingResolution)
                CaptureBridge.setRecordingFps(s.recordingFps)
                GameFilterBridge.syncFromPersisted(s.gameFilter)

                if (!didAutoCheckUpdates && s.autoCheckUpdates) {
                    val effectiveRepo = s.githubRepoSlug.ifBlank { GitHubUpdateManager.defaultBuiltInRepo() }
                    if (GitHubUpdateManager.normalizeRepoSlug(effectiveRepo) != null) {
                        didAutoCheckUpdates = true
                        checkForUpdates(explicitRepoInput = effectiveRepo, silent = true)
                    }
                }
            }
        }
        refreshSystemStatus()
    }

    fun refreshSystemStatus() {
        val ctx = getApplication<Application>()
        _readiness.value = SystemReadiness(
            canDrawOverlays = SystemIntegrations.canDrawOverlays(ctx),
            isImeEnabled = SystemIntegrations.isImeEnabled(ctx),
            isImeSelected = SystemIntegrations.isImeSelected(ctx),
            isAccessibilityEnabled = SystemIntegrations.isAccessibilityEnabled(ctx),
            hasNotificationPermission = SystemIntegrations.hasNotificationPermission(ctx),
            isGeForceNowInstalled = SystemIntegrations.isGeForceNowInstalled(ctx),
            canInstallPackages = GitHubUpdateManager.canInstallPackages(ctx)
        )
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun setActiveProfile(id: String) {
        viewModelScope.launch {
            profileRepo.setActiveProfile(id)
            _snackbarMessage.value = "Active overlay profile updated"
        }
    }

    fun createProfile(
        name: String,
        gameTitle: String,
        genre: GameGenre,
        template: ProfileTemplate = ProfileTemplate.BLANK,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val created = PresetProfiles.createProfileFromTemplate(
                name = name.ifBlank { template.title },
                gameTitle = gameTitle.ifBlank { "GeForce NOW Stream" },
                genre = genre,
                template = template
            )
            profileRepo.saveProfile(created)
            profileRepo.setActiveProfile(created.id)
            onCreated(created.id)
        }
    }

    fun duplicateProfile(source: ControllerProfile) {
        viewModelScope.launch {
            val copy = profileRepo.duplicateProfile(source)
            _snackbarMessage.value = "Duplicated \"${copy.name}\""
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            profileRepo.deleteProfile(id)
            _snackbarMessage.value = "Profile removed"
        }
    }

    fun exportProfileJson(profile: ControllerProfile): String =
        profileRepo.exportProfileJson(profile)

    fun importProfileJson(json: String) {
        viewModelScope.launch {
            val result = profileRepo.importProfileJson(json)
            result.onSuccess { imported ->
                _snackbarMessage.value = "Imported \"${imported.name}\" (${imported.controls.size} controls)"
            }.onFailure {
                _snackbarMessage.value = "Could not import JSON: ${it.localizedMessage ?: "Invalid format"}"
            }
        }
    }

    fun applyCustomButtonImage(controlId: String, imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(getApplication(), imageUri, cropSquare = true)
                .onSuccess { encoded ->
                    mutateEditorProfile(selectId = controlId) { current ->
                        current.copy(
                            controls = current.controls.map { ctrl ->
                                if (ctrl.id == controlId) ctrl.copy(customImageBase64 = encoded) else ctrl
                            }
                        )
                    }
                    _snackbarMessage.value = "Custom button image applied — tap Save to keep"
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to load image: ${err.localizedMessage ?: "Unsupported file"}"
                }
        }
    }

    fun applyCrosshairCustomImage(imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(getApplication(), imageUri, cropSquare = false)
                .onSuccess { encoded ->
                    val next = settings.value.crosshair.copy(
                        enabled = true,
                        style = CrosshairStyle.CUSTOM_IMAGE,
                        customImageBase64 = encoded
                    )
                    prefsRepo.updateCrosshair(next)
                    _snackbarMessage.value = "Custom crosshair image applied"
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to load crosshair: ${err.localizedMessage ?: "Unsupported file"}"
                }
        }
    }

    // Settings
    fun setVibrationMode(mode: VibrationMode) =
        viewModelScope.launch { prefsRepo.setVibrationMode(mode) }
    fun setGlobalOpacity(opacity: Float) = viewModelScope.launch { prefsRepo.setGlobalOpacity(opacity) }
    fun setGridSnapEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGridSnapEnabled(enabled) }
    fun setGridDivisions(divisions: Int) = viewModelScope.launch { prefsRepo.setGridDivisions(divisions) }
    fun setControllerNotation(notation: ControllerNotation) = viewModelScope.launch { prefsRepo.setControllerNotation(notation) }
    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { prefsRepo.setDarkTheme(dark) }
    fun setShowTelemetryOverlay(show: Boolean) = viewModelScope.launch { prefsRepo.setShowTelemetryOverlay(show) }
    fun setAutoLaunchGeForceNow(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAutoLaunchGeForceNow(enabled) }
    fun setFloatingKeyboardDefault(enabled: Boolean) = viewModelScope.launch { prefsRepo.setFloatingKeyboardDefault(enabled) }
    fun setAppLanguage(language: AppLanguage) = viewModelScope.launch { prefsRepo.setAppLanguage(language) }
    fun setTargetPlatformMode(mode: TargetPlatformMode) {
        InputBridge.setTargetMode(mode)
        viewModelScope.launch { prefsRepo.setTargetPlatformMode(mode) }
    }
    fun setInstantReplayDurationSec(seconds: Int) {
        CaptureBridge.setReplayDurationSec(seconds)
        viewModelScope.launch { prefsRepo.setInstantReplayDurationSec(seconds) }
    }
    fun setRecordingResolution(resolution: RecordingResolution) {
        CaptureBridge.setRecordingResolution(resolution)
        viewModelScope.launch { prefsRepo.setRecordingResolution(resolution) }
    }
    fun setRecordingFps(fps: RecordingFps) {
        CaptureBridge.setRecordingFps(fps)
        viewModelScope.launch { prefsRepo.setRecordingFps(fps) }
    }
    fun setGithubRepoSlug(rawInput: String) {
        val trimmed = rawInput.trim()
        val normalized = if (trimmed.isEmpty()) "" else (GitHubUpdateManager.normalizeRepoSlug(trimmed) ?: trimmed)
        viewModelScope.launch { prefsRepo.setGithubRepoSlug(normalized) }
    }
    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoCheckUpdates(enabled) }
    }

    // Personalization: App Color Scheme, Custom Fonts, Floating Bubble, Custom Test BG, What's New
    fun updateColorScheme(config: AppColorSchemeConfig) {
        viewModelScope.launch { prefsRepo.updateColorScheme(config) }
    }

    fun updateFontConfig(config: AppFontConfig) {
        viewModelScope.launch { prefsRepo.updateFontConfig(config) }
    }

    fun importCustomFont(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                CustomAssetStorage.saveCustomFontFromUri(getApplication(), uri)
            }
            res.onSuccess { (path, fontName) ->
                prefsRepo.updateFontConfig(
                    AppFontConfig(
                        preset = AppFontPreset.CUSTOM_UPLOAD,
                        customFontPath = path,
                        customFontName = fontName
                    )
                )
                _snackbarMessage.value = "Applied custom font: $fontName"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Unsupported font file (.ttf / .otf)"
            }
        }
    }

    fun updateFloatingBubble(config: FloatingBubbleConfig) {
        viewModelScope.launch { prefsRepo.updateFloatingBubble(config) }
    }

    fun importCustomBubbleIcon(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                CustomAssetStorage.saveCustomBubbleIconFromUri(getApplication(), uri)
            }
            res.onSuccess { path ->
                val current = settings.value.floatingBubble
                prefsRepo.updateFloatingBubble(
                    current.copy(
                        style = FloatingBubbleStyle.CUSTOM_IMAGE,
                        customIconPath = path
                    )
                )
                _snackbarMessage.value = "Custom floating bubble icon applied"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not load bubble icon"
            }
        }
    }

    fun importCustomTestBackground(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                CustomAssetStorage.saveCustomTestBackgroundFromUri(getApplication(), uri)
            }
            res.onSuccess { path ->
                prefsRepo.setCustomTestBgPath(path)
                _snackbarMessage.value = "Custom test background applied"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not load background image"
            }
        }
    }

    fun clearCustomTestBackground() {
        viewModelScope.launch {
            prefsRepo.setCustomTestBgPath("")
            _snackbarMessage.value = "Restored default test background"
        }
    }

    fun markWhatsNewSeen(versionCode: Int) {
        viewModelScope.launch {
            prefsRepo.setLastSeenWhatsNewVersionCode(versionCode)
        }
    }

    // GitHub Releases In-App Updater
    fun checkForUpdates(explicitRepoInput: String? = null, silent: Boolean = false) {
        val rawRepo = (explicitRepoInput ?: settings.value.githubRepoSlug)
            .ifBlank { GitHubUpdateManager.defaultBuiltInRepo() }
        val slug = GitHubUpdateManager.normalizeRepoSlug(rawRepo)
        if (slug == null) {
            if (!silent) {
                _updateState.value = UpdateUiState.Error(
                    "Enter your GitHub repository (e.g. username/NexusPad) to check for releases."
                )
            }
            return
        }

        if (!explicitRepoInput.isNullOrBlank() && explicitRepoInput.trim() != settings.value.githubRepoSlug) {
            viewModelScope.launch { prefsRepo.setGithubRepoSlug(slug) }
        }

        updateJob?.cancel()
        if (!silent) {
            _updateState.value = UpdateUiState.Checking
        }

        updateJob = viewModelScope.launch {
            val currentVersion = GitHubUpdateManager.currentVersionName()
            GitHubUpdateManager.fetchLatestRelease(slug)
                .onSuccess { release ->
                    val newer = GitHubUpdateManager.isNewerVersion(currentVersion, release.tagName)
                    if (newer) {
                        _updateState.value = UpdateUiState.UpdateAvailable(
                            currentVersion = currentVersion,
                            release = release
                        )
                        if (!silent) {
                            _snackbarMessage.value = "Update available (${release.tagName}) — download at GitHub!"
                        }
                    } else {
                        if (!silent) {
                            _updateState.value = UpdateUiState.UpToDate(
                                currentVersion = currentVersion,
                                latestTag = release.tagName,
                                checkedAtMs = System.currentTimeMillis()
                            )
                            _snackbarMessage.value = "NexusPad is up to date (v$currentVersion)"
                        }
                    }
                }
                .onFailure { err ->
                    if (err is CancellationException) throw err
                    if (!silent) {
                        _updateState.value = UpdateUiState.Error(
                            err.localizedMessage ?: "Unable to reach GitHub Releases"
                        )
                    }
                }
        }
    }

    fun downloadAvailableUpdate() {
        val release = when (val s = _updateState.value) {
            is UpdateUiState.UpdateAvailable -> s.release
            is UpdateUiState.ReadyToInstall -> s.release
            else -> return
        }
        updateJob?.cancel()
        _updateState.value = UpdateUiState.Downloading(
            release = release,
            bytesDownloaded = 0L,
            totalBytes = release.apkSizeBytes
        )
        updateJob = viewModelScope.launch {
            GitHubUpdateManager.downloadReleaseApk(
                context = getApplication(),
                release = release,
                onProgress = { downloaded, total ->
                    _updateState.value = UpdateUiState.Downloading(
                        release = release,
                        bytesDownloaded = downloaded,
                        totalBytes = total
                    )
                }
            ).onSuccess { apkFile ->
                val compatibility = withContext(Dispatchers.IO) {
                    GitHubUpdateManager.verifyApkUpgradeCompatibility(getApplication(), apkFile)
                }
                _updateState.value = UpdateUiState.ReadyToInstall(
                    release = release,
                    apkFile = apkFile,
                    compatibility = compatibility
                )
                if (compatibility is ApkUpgradeCompatibility.Compatible) {
                    installDownloadedUpdate()
                }
            }.onFailure { err ->
                if (err is CancellationException) {
                    _updateState.value = UpdateUiState.UpdateAvailable(
                        currentVersion = GitHubUpdateManager.currentVersionName(),
                        release = release
                    )
                    throw err
                }
                _updateState.value = UpdateUiState.Error(
                    err.localizedMessage ?: "Failed to download APK update"
                )
            }
        }
    }

    fun cancelUpdateDownload() {
        val current = _updateState.value
        updateJob?.cancel()
        if (current is UpdateUiState.Downloading) {
            _updateState.value = UpdateUiState.UpdateAvailable(
                currentVersion = GitHubUpdateManager.currentVersionName(),
                release = current.release
            )
            _snackbarMessage.value = "Update download cancelled"
        }
    }

    fun installDownloadedUpdate() {
        val state = _updateState.value as? UpdateUiState.ReadyToInstall ?: return
        refreshSystemStatus()
        GitHubUpdateManager.launchApkInstaller(getApplication(), state.apkFile)
            .onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Unable to launch package installer"
            }
    }

    fun resetUpdateState() {
        updateJob?.cancel()
        _updateState.value = UpdateUiState.Idle
    }

    // Crosshair HUD
    fun updateCrosshair(crosshair: CrosshairSettings) = viewModelScope.launch {
        prefsRepo.updateCrosshair(crosshair)
    }

    fun selectCrosshairPreset(preset: CrosshairPreset) = viewModelScope.launch {
        val next = preset.applyTo(settings.value.crosshair)
        prefsRepo.updateCrosshair(next)
    }

    fun resetCrosshair() = viewModelScope.launch {
        prefsRepo.updateCrosshair(CrosshairSettings(enabled = settings.value.crosshair.enabled))
        _snackbarMessage.value = "Crosshair reset to default"
    }

    // GeForce Experience Style Game Filters (Zero-Stutter Live Preview + DataStore Commit)
    fun selectFilterSlot(slot: Int) {
        GameFilterBridge.selectLiveSlot(slot)
        viewModelScope.launch {
            prefsRepo.setActiveFilterSlot(slot)
        }
    }

    fun previewFilterStyle(style: GameFilterStyle) {
        GameFilterBridge.updateLiveStyle(style)
    }

    fun updateFilterStyle(style: GameFilterStyle) {
        GameFilterBridge.updateLiveStyle(style)
        viewModelScope.launch {
            prefsRepo.updateFilterStyle(style)
        }
    }

    fun resetActiveFilterSlot() {
        val current = GameFilterBridge.liveFilterState.value
        val activeSlot = current.activeSlot
        if (activeSlot in 1..3) {
            GameFilterBridge.syncFromPersisted(current.withResetSlot(activeSlot))
            viewModelScope.launch {
                prefsRepo.resetFilterSlot(activeSlot)
                _snackbarMessage.value = "Reset Style $activeSlot to default"
            }
        }
    }

    fun applyCustomStickerImage(imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(getApplication(), imageUri, cropSquare = false)
                .onSuccess { encoded ->
                    val currentState = GameFilterBridge.liveFilterState.value
                    val slot = if (currentState.activeSlot in 1..3) currentState.activeSlot else 1
                    val updated = currentState.styleForSlot(slot).copy(
                        stickersEnabled = true,
                        stickerPreset = StickerPreset.CUSTOM_IMAGE,
                        customStickerBase64 = encoded
                    )
                    GameFilterBridge.updateLiveStyle(updated)
                    prefsRepo.updateFilterStyle(updated)
                    _snackbarMessage.value = "Custom sticker applied to Style $slot"
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to load sticker: ${err.localizedMessage ?: "Unsupported file"}"
                }
        }
    }

    // Layout Editor
    fun openEditor(profileId: String) {
        viewModelScope.launch {
            val loaded = profileRepo.getProfile(profileId) ?: activeProfile.value
            _editorState.value = EditorUiState(
                profile = loaded,
                selectedControlId = loaded?.controls?.firstOrNull()?.id,
                isPlayTestMode = false,
                hasUnsavedChanges = false,
                undoStack = emptyList()
            )
        }
    }

    fun selectControl(controlId: String?) {
        _editorState.update { it.copy(selectedControlId = controlId) }
    }

    fun togglePlayTestMode() {
        _editorState.update { it.copy(isPlayTestMode = !it.isPlayTestMode) }
    }

    fun moveControlToPercent(
        controlId: String,
        rawXPercent: Float,
        rawYPercent: Float,
        snapToGrid: Boolean,
        gridDivisions: Int
    ) {
        val divs = gridDivisions.coerceIn(4, 32).toFloat()
        val aspectDivsY = (divs * 0.66f).roundToInt().coerceIn(4, 24).toFloat()
        val clampedX = rawXPercent.coerceIn(0.02f, 0.98f)
        val clampedY = rawYPercent.coerceIn(0.03f, 0.97f)
        val finalX = if (snapToGrid) ((clampedX * divs).roundToInt() / divs).coerceIn(0.02f, 0.98f) else clampedX
        val finalY = if (snapToGrid) ((clampedY * aspectDivsY).roundToInt() / aspectDivsY).coerceIn(0.03f, 0.97f) else clampedY

        mutateEditorProfile(selectId = controlId) { current ->
            current.copy(
                controls = current.controls.map { ctrl ->
                    if (ctrl.id == controlId) ctrl.copy(xPercent = finalX, yPercent = finalY) else ctrl
                }
            )
        }
    }

    fun moveControlByDelta(
        controlId: String,
        dxPx: Float,
        dyPx: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        snapToGrid: Boolean
    ) {
        if (canvasWidthPx <= 0f || canvasHeightPx <= 0f) return
        _editorState.update { state ->
            val current = state.profile ?: return@update state
            val updatedControls = current.controls.map { ctrl ->
                if (ctrl.id != controlId) {
                    ctrl
                } else {
                    val rawX = (ctrl.xPercent + dxPx / canvasWidthPx).coerceIn(0.02f, 0.98f)
                    val rawY = (ctrl.yPercent + dyPx / canvasHeightPx).coerceIn(0.03f, 0.97f)
                    ctrl.copy(xPercent = rawX, yPercent = rawY)
                }
            }
            state.copy(
                profile = current.copy(controls = updatedControls),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateControl(updated: VirtualControl) {
        mutateEditorProfile { current ->
            current.copy(
                controls = current.controls.map { if (it.id == updated.id) updated else it }
            )
        }
    }

    fun addControl(controlType: ControlType, binding: InputBinding) {
        val defaultShape = when (controlType) {
            ControlType.ANALOG_STICK -> ControlShape.RING
            ControlType.TOUCHPAD -> ControlShape.TOUCHPAD_RECT
            ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> ControlShape.CROSS
            ControlType.SHOULDER_PADDLE -> ControlShape.PADDLE
            ControlType.BUTTON -> when (binding.domain) {
                BindingDomain.KEYBOARD -> ControlShape.KEYCAP
                BindingDomain.MOUSE -> ControlShape.KEYCAP
                else -> ControlShape.CIRCLE
            }
        }
        val defaultSize = when (controlType) {
            ControlType.TOUCHPAD -> 148f
            ControlType.ANALOG_STICK, ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> 118f
            ControlType.SHOULDER_PADDLE -> 72f
            ControlType.BUTTON -> 60f
        }
        val defaultAccent = when (binding.domain) {
            BindingDomain.CONTROLLER -> ControlAccent.LIME
            BindingDomain.MOUSE -> ControlAccent.AMBER
            BindingDomain.KEYBOARD -> ControlAccent.CYAN
            BindingDomain.COMBO -> ControlAccent.VIOLET
            BindingDomain.ANALOG_AXIS -> ControlAccent.CYAN
        }
        val newControl = VirtualControl(
            id = UUID.randomUUID().toString(),
            label = binding.codeLabel,
            controlType = controlType,
            shape = defaultShape,
            xPercent = 0.50f,
            yPercent = 0.50f,
            sizeDp = defaultSize,
            opacity = 0.85f,
            accent = defaultAccent,
            behavior = TriggerBehavior.HOLD,
            binding = binding
        )
        mutateEditorProfile(selectId = newControl.id) { current ->
            current.copy(controls = current.controls + newControl)
        }
    }

    fun duplicateSelectedControl() {
        val selected = _editorState.value.selectedControl ?: return
        val clone = selected.copy(
            id = UUID.randomUUID().toString(),
            xPercent = (selected.xPercent + 0.08f).coerceAtMost(0.92f),
            yPercent = (selected.yPercent + 0.08f).coerceAtMost(0.90f)
        )
        mutateEditorProfile(selectId = clone.id) { current ->
            current.copy(controls = current.controls + clone)
        }
    }

    fun mirrorDuplicateSelectedControl() {
        val selected = _editorState.value.selectedControl ?: return
        val mirroredX = (1f - selected.xPercent).coerceIn(0.05f, 0.95f)
        val clone = selected.copy(
            id = UUID.randomUUID().toString(),
            xPercent = mirroredX
        )
        mutateEditorProfile(selectId = clone.id) { current ->
            current.copy(controls = current.controls + clone)
        }
    }

    fun nudgeSelectedControl(dxPercent: Float, dyPercent: Float) {
        val selected = _editorState.value.selectedControl ?: return
        val updated = selected.copy(
            xPercent = (selected.xPercent + dxPercent).coerceIn(0.05f, 0.95f),
            yPercent = (selected.yPercent + dyPercent).coerceIn(0.08f, 0.92f)
        )
        updateControl(updated)
    }

    fun deleteSelectedControl() {
        val targetId = _editorState.value.selectedControlId ?: return
        mutateEditorProfile(selectId = null) { current ->
            current.copy(controls = current.controls.filterNot { it.id == targetId })
        }
    }

    fun undoEditorChange() {
        _editorState.update { state ->
            val previous = state.undoStack.lastOrNull() ?: return@update state
            state.copy(
                profile = previous,
                undoStack = state.undoStack.dropLast(1),
                hasUnsavedChanges = true
            )
        }
    }

    fun saveEditorProfile(onSaved: () -> Unit = {}) {
        val current = _editorState.value.profile ?: return
        viewModelScope.launch {
            profileRepo.saveProfile(current)
            _editorState.update { it.copy(hasUnsavedChanges = false) }
            _snackbarMessage.value = "Saved \"${current.name}\""
            onSaved()
        }
    }

    fun updateProfileMeta(name: String, gameTitle: String, genre: GameGenre) {
        mutateEditorProfile { current ->
            current.copy(
                name = name.ifBlank { current.name },
                gameTitle = gameTitle.ifBlank { current.gameTitle },
                genre = genre
            )
        }
    }

    private fun mutateEditorProfile(
        selectId: String? = _editorState.value.selectedControlId,
        transform: (ControllerProfile) -> ControllerProfile
    ) {
        _editorState.update { state ->
            val current = state.profile ?: return@update state
            val next = transform(current)
            state.copy(
                profile = next,
                selectedControlId = selectId ?: next.controls.firstOrNull()?.id,
                hasUnsavedChanges = true,
                undoStack = (state.undoStack + current).takeLast(15)
            )
        }
    }

    fun clearTelemetry() {
        InputBridge.clearTelemetry()
    }
}
