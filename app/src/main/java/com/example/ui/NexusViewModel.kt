package com.example.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.CaptureBridge
import com.example.cloud.CloudAuthResult
import com.example.cloud.CloudLayoutSyncRepository
import com.example.cloud.CloudUserInfo
import com.example.cloud.GoogleAuthManager
import com.example.cloud.SharedLayoutCodeResult
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.data.VibrationMode
import com.example.filter.GameFilterBridge
import com.example.input.GyroAimEngine
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
import com.example.model.GyroAimConfig
import com.example.model.InputBinding
import com.example.model.PerfHudConfig
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
import com.example.model.WebStreamQualityConfig
import com.example.update.ApkUpgradeCompatibility
import com.example.update.GitHubUpdateManager
import com.example.update.UpdateUiState
import com.example.util.ButtonImageCodec
import com.example.util.SystemIntegrations
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

sealed interface CloudSaveUiState {
    data object Idle : CloudSaveUiState
    data object SigningIn : CloudSaveUiState
    data object Syncing : CloudSaveUiState
    data class Synced(val cloudLayoutCount: Int, val syncedAtMs: Long) : CloudSaveUiState
    data class Error(val message: String) : CloudSaveUiState
}

sealed interface ShareCodeUiState {
    data object Idle : ShareCodeUiState
    data object Creating : ShareCodeUiState
    data class Created(val result: SharedLayoutCodeResult) : ShareCodeUiState
    data object Importing : ShareCodeUiState
    data class Imported(val profileName: String, val authorName: String) : ShareCodeUiState
    data class Error(val message: String) : ShareCodeUiState
}

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
    private val cloudRepo by lazy { CloudLayoutSyncRepository(application) }
    private val firebaseAuth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()

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

    val hasGyroSensor: Boolean = GyroAimEngine.hasGyroscope(application)

    private val _cloudUser = MutableStateFlow<CloudUserInfo?>(GoogleAuthManager.currentUser())
    val cloudUser: StateFlow<CloudUserInfo?> = _cloudUser.asStateFlow()

    private val _cloudSyncState = MutableStateFlow<CloudSaveUiState>(CloudSaveUiState.Idle)
    val cloudSyncState: StateFlow<CloudSaveUiState> = _cloudSyncState.asStateFlow()

    private val _shareCodeState = MutableStateFlow<ShareCodeUiState>(ShareCodeUiState.Idle)
    val shareCodeState: StateFlow<ShareCodeUiState> = _shareCodeState.asStateFlow()

    private var updateJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var appearancePushJob: Job? = null
    private var shareCodeJob: Job? = null
    private var lastSyncedUid: String? = null
    private var didAutoCheckUpdates = false
    private var didAttemptSilentSignIn = false

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val fbUser = auth.currentUser
        if (fbUser != null) {
            val info = CloudUserInfo.fromFirebaseUser(fbUser)
            _cloudUser.value = info
            if (lastSyncedUid != info.uid) {
                lastSyncedUid = info.uid
                syncLayoutsWithCloud(user = info, forceCloudWins = false, showToast = false)
            }
        } else {
            cloudSyncJob?.cancel()
            cloudSyncJob = null
            lastSyncedUid = null
            _cloudUser.value = null
            if (_cloudSyncState.value !is CloudSaveUiState.SigningIn &&
                _cloudSyncState.value !is CloudSaveUiState.Error
            ) {
                _cloudSyncState.value = CloudSaveUiState.Idle
            }
        }
    }

    init {
        viewModelScope.launch {
            profileRepo.ensureSeeded()
        }
        firebaseAuth?.addAuthStateListener(authStateListener)
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

    override fun onCleared() {
        super.onCleared()
        firebaseAuth?.removeAuthStateListener(authStateListener)
        cloudSyncJob?.cancel()
        appearancePushJob?.cancel()
        shareCodeJob?.cancel()
        updateJob?.cancel()
    }

    // Optional Google Sign-In & Cloud Layout Save (zero Firestore calls when signed out)
    fun attemptSilentCloudSignIn(activity: Activity) {
        if (didAttemptSilentSignIn || _cloudUser.value != null) return
        didAttemptSilentSignIn = true
        viewModelScope.launch {
            when (val result = GoogleAuthManager.attemptAutoSignIn(activity)) {
                is CloudAuthResult.Success -> {
                    _cloudUser.value = result.user
                    if (lastSyncedUid != result.user.uid) {
                        lastSyncedUid = result.user.uid
                        syncLayoutsWithCloud(user = result.user, forceCloudWins = false, showToast = false)
                    }
                }
                else -> Unit
            }
        }
    }

    fun signInForCloudSave(activity: Activity) {
        if (_cloudSyncState.value is CloudSaveUiState.SigningIn) return
        _cloudSyncState.value = CloudSaveUiState.SigningIn
        viewModelScope.launch {
            when (val result = GoogleAuthManager.signInWithGoogle(activity)) {
                is CloudAuthResult.Success -> {
                    _cloudUser.value = result.user
                    lastSyncedUid = result.user.uid
                    syncLayoutsWithCloud(user = result.user, forceCloudWins = false, showToast = true)
                }
                CloudAuthResult.Cancelled, CloudAuthResult.NoSavedCredential -> {
                    _cloudSyncState.value = CloudSaveUiState.Idle
                }
                is CloudAuthResult.Error -> {
                    _cloudSyncState.value = CloudSaveUiState.Error(result.message)
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun signOutCloudSave() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        lastSyncedUid = null
        viewModelScope.launch {
            GoogleAuthManager.signOut(getApplication())
            _cloudUser.value = null
            _cloudSyncState.value = CloudSaveUiState.Idle
            _snackbarMessage.value = "Signed out of Cloud Save — local layouts kept on this device"
        }
    }

    fun backupAllLayoutsToCloud() {
        val user = _cloudUser.value ?: return
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            _cloudSyncState.value = CloudSaveUiState.Syncing
            profileRepo.ensureSeeded()
            val accountRes = cloudRepo.upsertUserAccount(user)
            if (accountRes.isFailure) {
                val msg = accountRes.exceptionOrNull()?.localizedMessage ?: "Cloud Save failed"
                _cloudSyncState.value = CloudSaveUiState.Error(msg)
                _snackbarMessage.value = msg
                return@launch
            }
            val localProfiles = profileRepo.getAllProfilesOnce()
            var pushedCount = 0
            var firstError: Throwable? = null
            for (profile in localProfiles) {
                val res = cloudRepo.pushLayout(profile)
                if (res.isSuccess) {
                    pushedCount++
                } else if (firstError == null) {
                    firstError = res.exceptionOrNull()
                }
            }
            val appearanceSnapshot = prefsRepo.exportCloudAppearanceSnapshot()
            cloudRepo.pushAppearanceSettings(appearanceSnapshot)

            if (firstError != null && pushedCount == 0) {
                val msg = firstError.localizedMessage ?: "Could not back up layouts to cloud"
                _cloudSyncState.value = CloudSaveUiState.Error(msg)
                _snackbarMessage.value = msg
            } else {
                val now = System.currentTimeMillis()
                _cloudSyncState.value = CloudSaveUiState.Synced(
                    cloudLayoutCount = pushedCount,
                    syncedAtMs = now
                )
                _snackbarMessage.value = "Backed up $pushedCount layouts + filters & theme to Cloud Save"
            }
        }
    }

    fun restoreLayoutsFromCloud() {
        val user = _cloudUser.value ?: return
        syncLayoutsWithCloud(user = user, forceCloudWins = true, showToast = true)
    }

    private fun syncLayoutsWithCloud(
        user: CloudUserInfo,
        forceCloudWins: Boolean,
        showToast: Boolean
    ) {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            _cloudSyncState.value = CloudSaveUiState.Syncing
            profileRepo.ensureSeeded()

            val accountRes = cloudRepo.upsertUserAccount(user)
            if (accountRes.isFailure) {
                val msg = accountRes.exceptionOrNull()?.localizedMessage ?: "Cloud Save account sync failed"
                _cloudSyncState.value = CloudSaveUiState.Error(msg)
                if (showToast) _snackbarMessage.value = msg
                return@launch
            }

            val cloudRes = cloudRepo.fetchAllCloudLayouts()
            val cloudProfiles = cloudRes.getOrElse { err ->
                val msg = err.localizedMessage ?: "Failed to read Cloud Save layouts"
                _cloudSyncState.value = CloudSaveUiState.Error(msg)
                if (showToast) _snackbarMessage.value = msg
                return@launch
            }

            val localProfiles = profileRepo.getAllProfilesOnce()
            val plan = CloudLayoutSyncRepository.planMerge(
                localProfiles = localProfiles,
                cloudProfiles = cloudProfiles,
                forceCloudWins = forceCloudWins
            )

            for (restored in plan.toSaveLocally) {
                profileRepo.upsertCloudRestoredProfile(restored)
            }
            if (plan.preferredActiveProfileId != null) {
                profileRepo.setActiveProfile(plan.preferredActiveProfileId)
            }

            var uploadedCount = 0
            for (local in plan.toPushToCloud) {
                if (cloudRepo.pushLayout(local).isSuccess) {
                    uploadedCount++
                }
            }

            // Also sync Filters, Crosshair, Color Scheme, Font Preset, Floating Bubble & Dark Theme
            var restoredAppearance = false
            val localAppearance = prefsRepo.exportCloudAppearanceSnapshot()
            val cloudAppearance = cloudRepo.fetchAppearanceSettings().getOrNull()
            if (cloudAppearance != null &&
                (forceCloudWins ||
                    localAppearance.clientUpdatedAt <= 0L ||
                    cloudAppearance.clientUpdatedAt > localAppearance.clientUpdatedAt)
            ) {
                prefsRepo.applyCloudAppearanceSnapshot(cloudAppearance)
                restoredAppearance = true
            } else if (!forceCloudWins &&
                (cloudAppearance == null || localAppearance.clientUpdatedAt > cloudAppearance.clientUpdatedAt)
            ) {
                cloudRepo.pushAppearanceSettings(localAppearance)
            }

            val totalCloudIds = (cloudProfiles.map { CloudLayoutSyncRepository.sanitizeLayoutId(it.id) } +
                plan.toPushToCloud.map { CloudLayoutSyncRepository.sanitizeLayoutId(it.id) }).toSet()
            _cloudSyncState.value = CloudSaveUiState.Synced(
                cloudLayoutCount = totalCloudIds.size,
                syncedAtMs = System.currentTimeMillis()
            )

            if (showToast || plan.toSaveLocally.isNotEmpty() || restoredAppearance) {
                _snackbarMessage.value = when {
                    plan.toSaveLocally.isNotEmpty() && uploadedCount > 0 ->
                        "Cloud Save: restored ${plan.toSaveLocally.size}, backed up $uploadedCount layouts + theme"
                    plan.toSaveLocally.isNotEmpty() || restoredAppearance ->
                        "Restored ${plan.toSaveLocally.size} layouts, filters & theme from Cloud Save"
                    uploadedCount > 0 ->
                        "Backed up $uploadedCount layouts, filters & theme to Cloud Save"
                    else ->
                        "Cloud Save up to date (${totalCloudIds.size} layouts + theme)"
                }
            }
        }
    }

    private fun scheduleCloudAppearancePush() {
        if (_cloudUser.value == null) return
        appearancePushJob?.cancel()
        appearancePushJob = viewModelScope.launch {
            delay(1200L)
            if (_cloudUser.value == null) return@launch
            val snapshot = prefsRepo.exportCloudAppearanceSnapshot()
            cloudRepo.pushAppearanceSettings(snapshot)
        }
    }

    fun createShareCodeForProfile(profile: ControllerProfile) {
        val user = _cloudUser.value
        if (user == null) {
            _shareCodeState.value = ShareCodeUiState.Error(
                "Sign in with Google first to create a 6-digit share code."
            )
            return
        }
        shareCodeJob?.cancel()
        _shareCodeState.value = ShareCodeUiState.Creating
        shareCodeJob = viewModelScope.launch {
            cloudRepo.publishSharedLayout(profile, user.displayName)
                .onSuccess { result ->
                    _shareCodeState.value = ShareCodeUiState.Created(result)
                }
                .onFailure { err ->
                    _shareCodeState.value = ShareCodeUiState.Error(
                        err.localizedMessage ?: "Could not generate share code"
                    )
                }
        }
    }

    fun importProfileByShareCode(rawCode: String) {
        if (_cloudUser.value == null) {
            _shareCodeState.value = ShareCodeUiState.Error(
                "Sign in with Google first to import a 6-digit share code."
            )
            return
        }
        shareCodeJob?.cancel()
        _shareCodeState.value = ShareCodeUiState.Importing
        shareCodeJob = viewModelScope.launch {
            cloudRepo.fetchSharedLayoutByCode(rawCode)
                .onSuccess { fetched ->
                    if (fetched == null) {
                        _shareCodeState.value = ShareCodeUiState.Error(
                            "Code not found or expired. Ask your friend for a fresh 6-digit code."
                        )
                    } else {
                        val saved = profileRepo.saveProfile(fetched.profile)
                        pushProfileToCloudIfSignedIn(saved)
                        _shareCodeState.value = ShareCodeUiState.Imported(
                            profileName = saved.name,
                            authorName = fetched.authorName
                        )
                        _snackbarMessage.value = "Imported \"${saved.name}\" from ${fetched.authorName}"
                    }
                }
                .onFailure { err ->
                    _shareCodeState.value = ShareCodeUiState.Error(
                        err.localizedMessage ?: "Failed to fetch shared layout"
                    )
                }
        }
    }

    fun dismissShareCodeState() {
        shareCodeJob?.cancel()
        _shareCodeState.value = ShareCodeUiState.Idle
    }

    private fun pushProfileToCloudIfSignedIn(profile: ControllerProfile) {
        if (_cloudUser.value == null) return
        viewModelScope.launch {
            cloudRepo.pushLayout(profile).onSuccess {
                val currentCount = (profiles.value.size).coerceAtLeast(1)
                _cloudSyncState.value = CloudSaveUiState.Synced(
                    cloudLayoutCount = currentCount,
                    syncedAtMs = System.currentTimeMillis()
                )
            }
        }
    }

    private fun deleteProfileFromCloudIfSignedIn(profileId: String) {
        if (_cloudUser.value == null) return
        viewModelScope.launch {
            cloudRepo.deleteCloudLayout(profileId).onSuccess {
                val remainingCount = profiles.value.count { it.id != profileId }
                _cloudSyncState.value = CloudSaveUiState.Synced(
                    cloudLayoutCount = remainingCount,
                    syncedAtMs = System.currentTimeMillis()
                )
            }
        }
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
            profileRepo.getProfile(id)?.let { pushProfileToCloudIfSignedIn(it) }
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
            val saved = profileRepo.saveProfile(created)
            profileRepo.setActiveProfile(saved.id)
            pushProfileToCloudIfSignedIn(saved.copy(isActive = true))
            onCreated(saved.id)
        }
    }

    fun duplicateProfile(source: ControllerProfile) {
        viewModelScope.launch {
            val copy = profileRepo.duplicateProfile(source)
            pushProfileToCloudIfSignedIn(copy)
            _snackbarMessage.value = "Duplicated \"${copy.name}\""
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            profileRepo.deleteProfile(id)
            deleteProfileFromCloudIfSignedIn(id)
            _snackbarMessage.value = "Profile removed"
        }
    }

    fun exportProfileJson(profile: ControllerProfile): String =
        profileRepo.exportProfileJson(profile)

    fun importProfileJson(json: String) {
        viewModelScope.launch {
            val result = profileRepo.importProfileJson(json)
            result.onSuccess { imported ->
                pushProfileToCloudIfSignedIn(imported)
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
    fun setDarkTheme(dark: Boolean) = viewModelScope.launch {
        prefsRepo.setDarkTheme(dark)
        scheduleCloudAppearancePush()
    }
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

    // Gyroscope Aiming & Performance HUD
    fun updateGyroAim(config: GyroAimConfig) {
        viewModelScope.launch { prefsRepo.updateGyroAim(config) }
    }

    fun updatePerfHud(config: PerfHudConfig) {
        viewModelScope.launch { prefsRepo.updatePerfHud(config) }
    }

    fun updateWebPlayerConfig(config: WebStreamQualityConfig) {
        viewModelScope.launch { prefsRepo.updateWebPlayerConfig(config) }
    }

    // Personalization: App Color Scheme, Custom Fonts, Floating Bubble, Custom Test BG, What's New
    fun updateColorScheme(config: AppColorSchemeConfig) {
        viewModelScope.launch {
            prefsRepo.updateColorScheme(config)
            scheduleCloudAppearancePush()
        }
    }

    fun updateFontConfig(config: AppFontConfig) {
        viewModelScope.launch {
            prefsRepo.updateFontConfig(config)
            scheduleCloudAppearancePush()
        }
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
        viewModelScope.launch {
            prefsRepo.updateFloatingBubble(config)
            scheduleCloudAppearancePush()
        }
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
                    "Enter your GitHub repository (e.g. username/XenosPad) to check for releases."
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
                            _snackbarMessage.value = "XenosPad is up to date (v$currentVersion)"
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
        scheduleCloudAppearancePush()
    }

    fun selectCrosshairPreset(preset: CrosshairPreset) = viewModelScope.launch {
        val next = preset.applyTo(settings.value.crosshair)
        prefsRepo.updateCrosshair(next)
        scheduleCloudAppearancePush()
    }

    fun resetCrosshair() = viewModelScope.launch {
        prefsRepo.updateCrosshair(CrosshairSettings(enabled = settings.value.crosshair.enabled))
        scheduleCloudAppearancePush()
        _snackbarMessage.value = "Crosshair reset to default"
    }

    // GeForce Experience Style Game Filters (Zero-Stutter Live Preview + DataStore Commit)
    fun selectFilterSlot(slot: Int) {
        GameFilterBridge.selectLiveSlot(slot)
        viewModelScope.launch {
            prefsRepo.setActiveFilterSlot(slot)
            scheduleCloudAppearancePush()
        }
    }

    fun previewFilterStyle(style: GameFilterStyle) {
        GameFilterBridge.updateLiveStyle(style)
    }

    fun updateFilterStyle(style: GameFilterStyle) {
        GameFilterBridge.updateLiveStyle(style)
        viewModelScope.launch {
            prefsRepo.updateFilterStyle(style)
            scheduleCloudAppearancePush()
        }
    }

    fun resetActiveFilterSlot() {
        val current = GameFilterBridge.liveFilterState.value
        val activeSlot = current.activeSlot
        if (activeSlot in 1..3) {
            GameFilterBridge.syncFromPersisted(current.withResetSlot(activeSlot))
            viewModelScope.launch {
                prefsRepo.resetFilterSlot(activeSlot)
                scheduleCloudAppearancePush()
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
                    scheduleCloudAppearancePush()
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
            val saved = profileRepo.saveProfile(current)
            pushProfileToCloudIfSignedIn(saved)
            _editorState.update { it.copy(profile = saved, hasUnsavedChanges = false) }
            _snackbarMessage.value = if (_cloudUser.value != null) {
                "Saved \"${saved.name}\" & backed up to Cloud"
            } else {
                "Saved \"${saved.name}\""
            }
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
