package com.example.ui

import android.app.Activity
import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.CaptureBridge
import com.example.cloud.CloudAuthResult
import com.example.cloud.CloudLayoutSyncRepository
import com.example.cloud.CloudUserInfo
import com.example.cloud.CommunityWorkshopRepository
import com.example.cloud.GoogleAuthManager
import com.example.cloud.SharedLayoutCodeResult
import com.example.data.ButtonThemeLocalStore
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.data.VibrationMode
import com.example.filter.GameFilterBridge
import com.example.input.GyroAimEngine
import com.example.input.InputBridge
import com.example.input.PhysicalControllerInfo
import com.example.input.PhysicalControllerMonitor
import com.example.model.AdminConfig
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import com.example.model.AppLanguage
import com.example.model.AppPlatformTheme
import com.example.model.BindingDomain
import com.example.model.ButtonThemePreset
import com.example.model.CapturedMediaItem
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerNotation
import com.example.model.ControllerProfile
import com.example.model.CosmeticItem
import com.example.model.CosmeticItemType
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
import com.example.model.ItemAvailability
import com.example.model.PerfHudConfig
import com.example.model.PhysicalControllerConfig
import com.example.model.PinnedGame
import com.example.model.PostComment
import com.example.model.PresetProfiles
import com.example.model.ProfileClipPost
import com.example.model.ProfileTemplate
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.StickState
import com.example.model.StickerPreset
import com.example.model.TargetPlatformMode
import com.example.model.TelemetryEvent
import com.example.model.ThemeComment
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.model.WebStreamQualityConfig
import com.example.ui.components.LayoutStudioHelper
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
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
    private val themeLocalStore = ButtonThemeLocalStore(application)
    private val cloudRepo by lazy { CloudLayoutSyncRepository(application) }
    private val communityRepo by lazy { CommunityWorkshopRepository(application) }
    private val firebaseAuth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()

    val profiles: StateFlow<List<ControllerProfile>> = profileRepo.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ControllerProfile?> = profileRepo.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<OverlaySettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverlaySettings())

    val localButtonThemes: StateFlow<List<ButtonThemePreset>> = themeLocalStore.localThemes
    val activeButtonThemeId: StateFlow<String?> = themeLocalStore.activeThemeId
    val autoSavedThemeDraft: StateFlow<ButtonThemePreset?> = themeLocalStore.autoSavedDraft
    val activeButtonTheme: StateFlow<ButtonThemePreset?> = combine(
        themeLocalStore.localThemes,
        themeLocalStore.activeThemeId
    ) { list, id ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), themeLocalStore.getActiveTheme())

    val isOverlayRunning: StateFlow<Boolean> = InputBridge.isOverlayRunning
    val isImeConnected: StateFlow<Boolean> = InputBridge.isImeConnected
    val telemetry: StateFlow<List<TelemetryEvent>> = InputBridge.telemetry
    val pressedControlIds: StateFlow<Set<String>> = InputBridge.pressedControlIds
    val toggledControlIds: StateFlow<Set<String>> = InputBridge.toggledControlIds
    val leftStick: StateFlow<StickState> = InputBridge.leftStick
    val rightStick: StateFlow<StickState> = InputBridge.rightStick
    val liveFilterState: StateFlow<GameFilterState> = GameFilterBridge.liveFilterState
    val isGpuShaderActive: StateFlow<Boolean> = GameFilterBridge.isGpuShaderActive
    val connectedControllers: StateFlow<List<PhysicalControllerInfo>> = PhysicalControllerMonitor.connectedControllers

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

    // Community Workshop, Discussions & Public Profiles state
    private val _workshopThemes = MutableStateFlow<List<ButtonThemePreset>>(emptyList())
    val workshopThemes: StateFlow<List<ButtonThemePreset>> = _workshopThemes.asStateFlow()

    private val _likedThemeIds = MutableStateFlow<Set<String>>(emptySet())
    val likedThemeIds: StateFlow<Set<String>> = _likedThemeIds.asStateFlow()

    private val _favoriteThemeIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteThemeIds: StateFlow<Set<String>> = _favoriteThemeIds.asStateFlow()

    private val _communityPosts = MutableStateFlow<List<CommunityTopicPost>>(emptyList())
    val communityPosts: StateFlow<List<CommunityTopicPost>> = _communityPosts.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _likedClipIds = MutableStateFlow<Set<String>>(emptySet())
    val likedClipIds: StateFlow<Set<String>> = _likedClipIds.asStateFlow()

    private val _myPublicProfile = MutableStateFlow<CommunityUserProfile?>(null)
    val myPublicProfile: StateFlow<CommunityUserProfile?> = _myPublicProfile.asStateFlow()

    private val _myProfileClips = MutableStateFlow<List<ProfileClipPost>>(emptyList())
    val myProfileClips: StateFlow<List<ProfileClipPost>> = _myProfileClips.asStateFlow()

    private val _myProfileThemes = MutableStateFlow<List<ButtonThemePreset>>(emptyList())
    val myProfileThemes: StateFlow<List<ButtonThemePreset>> = _myProfileThemes.asStateFlow()

    private val _myProfilePosts = MutableStateFlow<List<CommunityTopicPost>>(emptyList())
    val myProfilePosts: StateFlow<List<CommunityTopicPost>> = _myProfilePosts.asStateFlow()

    private val _viewedPublicProfile = MutableStateFlow<CommunityUserProfile?>(null)
    val viewedPublicProfile: StateFlow<CommunityUserProfile?> = _viewedPublicProfile.asStateFlow()

    private val _viewedProfileClips = MutableStateFlow<List<ProfileClipPost>>(emptyList())
    val viewedProfileClips: StateFlow<List<ProfileClipPost>> = _viewedProfileClips.asStateFlow()

    private val _viewedProfileThemes = MutableStateFlow<List<ButtonThemePreset>>(emptyList())
    val viewedProfileThemes: StateFlow<List<ButtonThemePreset>> = _viewedProfileThemes.asStateFlow()

    private val _viewedProfilePosts = MutableStateFlow<List<CommunityTopicPost>>(emptyList())
    val viewedProfilePosts: StateFlow<List<CommunityTopicPost>> = _viewedProfilePosts.asStateFlow()

    private val _isCommunityLoading = MutableStateFlow(false)
    val isCommunityLoading: StateFlow<Boolean> = _isCommunityLoading.asStateFlow()

    private val _isPublishingTheme = MutableStateFlow(false)
    val isPublishingTheme: StateFlow<Boolean> = _isPublishingTheme.asStateFlow()

    // Cosmetic Shop, Wardrobe & Restricted Admin Management state
    private val _cosmeticItems = MutableStateFlow<List<CosmeticItem>>(emptyList())
    val cosmeticItems: StateFlow<List<CosmeticItem>> = _cosmeticItems.asStateFlow()

    private val _myOwnedCosmeticIds = MutableStateFlow<Set<String>>(emptySet())
    val myOwnedCosmeticIds: StateFlow<Set<String>> = _myOwnedCosmeticIds.asStateFlow()

    private val _adminSearchResults = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val adminSearchResults: StateFlow<List<CommunityUserProfile>> = _adminSearchResults.asStateFlow()

    private val _adminSelectedUser = MutableStateFlow<CommunityUserProfile?>(null)
    val adminSelectedUser: StateFlow<CommunityUserProfile?> = _adminSelectedUser.asStateFlow()

    private val _adminSelectedUserOwnedIds = MutableStateFlow<Set<String>>(emptySet())
    val adminSelectedUserOwnedIds: StateFlow<Set<String>> = _adminSelectedUserOwnedIds.asStateFlow()

    private val _isAdminBusy = MutableStateFlow(false)
    val isAdminBusy: StateFlow<Boolean> = _isAdminBusy.asStateFlow()

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
                ensureMyPublicProfileLoaded(info)
            }
        } else {
            cloudSyncJob?.cancel()
            cloudSyncJob = null
            lastSyncedUid = null
            _cloudUser.value = null
            _myPublicProfile.value = null
            _myProfileClips.value = emptyList()
            _myProfileThemes.value = emptyList()
            _myProfilePosts.value = emptyList()
            _favoriteThemeIds.value = emptySet()
            _likedThemeIds.value = emptySet()
            _likedPostIds.value = emptySet()
            _likedClipIds.value = emptySet()
            _cosmeticItems.value = emptyList()
            _myOwnedCosmeticIds.value = emptySet()
            _adminSearchResults.value = emptyList()
            _adminSelectedUser.value = null
            _adminSelectedUserOwnedIds.value = emptySet()
            if (_cloudSyncState.value !is CloudSaveUiState.SigningIn &&
                _cloudSyncState.value !is CloudSaveUiState.Error
            ) {
                _cloudSyncState.value = CloudSaveUiState.Idle
            }
        }
    }

    init {
        PhysicalControllerMonitor.ensureStarted(application)
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

    fun applyCustomButtonBackground(controlId: String, imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(
                context = getApplication(),
                uri = imageUri,
                cropSquare = false,
                maxDimPx = 144,
                maxEncodedBytes = 32_000
            ).onSuccess { encoded ->
                mutateEditorProfile(selectId = controlId) { current ->
                    current.copy(
                        controls = current.controls.map { ctrl ->
                            if (ctrl.id == controlId) {
                                ctrl.copy(customBackgroundBase64 = encoded, showBackground = true)
                            } else {
                                ctrl
                            }
                        }
                    )
                }
                _snackbarMessage.value = "Custom button background applied"
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to load background: ${err.localizedMessage ?: "Unsupported file"}"
            }
        }
    }

    fun applyCustomButtonStroke(controlId: String, imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(
                context = getApplication(),
                uri = imageUri,
                cropSquare = false,
                maxDimPx = 144,
                maxEncodedBytes = 32_000
            ).onSuccess { encoded ->
                mutateEditorProfile(selectId = controlId) { current ->
                    current.copy(
                        controls = current.controls.map { ctrl ->
                            if (ctrl.id == controlId) {
                                ctrl.copy(customStrokeBase64 = encoded, showStroke = true)
                            } else {
                                ctrl
                            }
                        }
                    )
                }
                _snackbarMessage.value = "Custom button stroke frame applied"
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to load stroke frame: ${err.localizedMessage ?: "Unsupported file"}"
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

    fun updatePhysicalController(config: PhysicalControllerConfig) {
        viewModelScope.launch { prefsRepo.updatePhysicalController(config) }
    }

    fun savePinnedGame(game: PinnedGame) {
        viewModelScope.launch {
            prefsRepo.upsertPinnedGame(game)
            _snackbarMessage.value = "Pinned \"${game.title}\" to Cockpit Hub"
        }
    }

    fun deletePinnedGame(gameId: String) {
        viewModelScope.launch {
            prefsRepo.deletePinnedGame(gameId)
            _snackbarMessage.value = "Removed pinned game"
        }
    }

    fun prepareLaunchPinnedGame(game: PinnedGame, onReady: (String?) -> Unit) {
        viewModelScope.launch {
            if (game.profileId.isNotBlank()) {
                profileRepo.setActiveProfile(game.profileId)
            }
            val safeSlot = game.filterSlot.coerceIn(0, 3)
            GameFilterBridge.selectLiveSlot(safeSlot)
            prefsRepo.setActiveFilterSlot(safeSlot)
            val nextWebConfig = settings.value.webPlayerConfig.copy(regionPreset = game.regionPreset)
            prefsRepo.updateWebPlayerConfig(nextWebConfig)
            prefsRepo.markPinnedGamePlayed(game.id)
            onReady(game.normalizedLaunchUrl())
        }
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

    fun setPlatformTheme(theme: AppPlatformTheme) {
        viewModelScope.launch {
            prefsRepo.setPlatformTheme(theme)
            _snackbarMessage.value = "Switched interface to ${theme.displayName}"
        }
    }

    fun setNintendoSwitchDarkMode(dark: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNintendoSwitchDarkMode(dark)
            _snackbarMessage.value = if (dark) {
                "Nintendo Switch set to Basic Black (Dark)"
            } else {
                "Nintendo Switch set to Basic White (Light)"
            }
        }
    }

    fun completeOnboarding(username: String, bio: String, editedAvatarUri: Uri?) {
        viewModelScope.launch {
            val cleanName = username.trim().take(36)
            prefsRepo.setOnboardingCompleted(completed = true, displayName = cleanName)
            val user = _cloudUser.value
            if (user != null) {
                val existing = _myPublicProfile.value
                val encodedAvatar = if (editedAvatarUri != null) {
                    ButtonImageCodec.encodeUriToBase64(
                        context = getApplication(),
                        uri = editedAvatarUri,
                        cropSquare = true,
                        maxDimPx = 144,
                        maxEncodedBytes = 42_000
                    ).getOrNull()
                } else {
                    existing?.avatarBase64
                }
                communityRepo.upsertPublicProfile(
                    username = cleanName.ifBlank { existing?.username ?: user.displayName },
                    bio = bio.trim(),
                    avatarBase64 = encodedAvatar,
                    avatarUrl = existing?.avatarUrl ?: user.photoUrl
                ).onSuccess { updated ->
                    _myPublicProfile.value = updated
                }
            }
            _snackbarMessage.value = "Welcome to XenosPad${if (cleanName.isNotBlank()) ", $cleanName" else ""}!"
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
                            _snackbarMessage.value = "Update available (${release.tagName}) — tap Download & Install!"
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

    fun resizeControlScales(controlId: String, widthScale: Float, heightScale: Float) {
        val clampedW = widthScale.coerceIn(0.45f, 2.6f)
        val clampedH = heightScale.coerceIn(0.45f, 2.6f)
        mutateEditorProfile(selectId = controlId) { current ->
            current.copy(
                controls = current.controls.map { ctrl ->
                    if (ctrl.id == controlId) {
                        ctrl.copy(widthScale = clampedW, heightScale = clampedH)
                    } else {
                        ctrl
                    }
                }
            )
        }
    }

    fun applyActiveThemeToEditorLayout() {
        val theme = themeLocalStore.getActiveTheme() ?: run {
            _snackbarMessage.value = "Select a Button Theme first"
            return
        }
        mutateEditorProfile { current ->
            current.copy(
                controls = current.controls.map { ctrl -> theme.applyToControl(ctrl) }
            )
        }
        _snackbarMessage.value = "Applied \"${theme.name}\" to all buttons in layout"
    }

    fun addControl(controlType: ControlType, binding: InputBinding) {
        val activeTheme = themeLocalStore.getActiveTheme()
        val newControl = LayoutStudioHelper.createControlFromBinding(binding, activeTheme)
            .let { base ->
                if (base.controlType == controlType) base else base.copy(controlType = controlType)
            }
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

    // -------------------------------------------------------------------------
    // Button Theme Presets, Workshop, Discussions & Public Profiles
    // -------------------------------------------------------------------------

    private fun ensureMyPublicProfileLoaded(user: CloudUserInfo) {
        viewModelScope.launch {
            val loaded = communityRepo.getPublicProfile(user.uid).getOrNull()
            if (loaded != null) {
                _myPublicProfile.value = loaded
            } else {
                val created = communityRepo.upsertPublicProfile(
                    username = user.displayName.ifBlank { "XenosPad Player" },
                    bio = "",
                    avatarBase64 = null,
                    avatarUrl = user.photoUrl
                ).getOrNull()
                _myPublicProfile.value = created ?: CommunityUserProfile(
                    uid = user.uid,
                    username = user.displayName.ifBlank { "XenosPad Player" },
                    avatarUrl = user.photoUrl
                )
            }
            communityRepo.fetchAllCosmeticItems().onSuccess { _cosmeticItems.value = it }
            communityRepo.fetchOwnedItemIds(user.uid).onSuccess { _myOwnedCosmeticIds.value = it }
        }
    }

    private suspend fun requireAuthorProfile(): CommunityUserProfile? {
        val user = _cloudUser.value ?: return null
        val cached = _myPublicProfile.value
        if (cached != null && cached.uid == user.uid) return cached
        val fetched = communityRepo.getPublicProfile(user.uid).getOrNull()
        if (fetched != null) {
            _myPublicProfile.value = fetched
            return fetched
        }
        val created = communityRepo.upsertPublicProfile(
            username = user.displayName.ifBlank { "XenosPad Player" },
            bio = "",
            avatarBase64 = null,
            avatarUrl = user.photoUrl
        ).getOrNull()
        val resolved = created ?: CommunityUserProfile(
            uid = user.uid,
            username = user.displayName.ifBlank { "XenosPad Player" },
            avatarUrl = user.photoUrl
        )
        _myPublicProfile.value = resolved
        return resolved
    }

    fun getLocalButtonTheme(themeId: String): ButtonThemePreset? =
        themeLocalStore.localThemes.value.firstOrNull { it.id == themeId }

    fun autoSaveButtonThemeDraft(theme: ButtonThemePreset) {
        viewModelScope.launch {
            themeLocalStore.saveAutoDraft(theme)
        }
    }

    fun autoSaveButtonThemeDraftSync(theme: ButtonThemePreset) {
        themeLocalStore.saveAutoDraftSync(theme)
    }

    fun clearAutoSavedThemeDraft() {
        viewModelScope.launch {
            themeLocalStore.clearAutoDraft()
        }
    }

    fun saveButtonThemeDraft(theme: ButtonThemePreset, activate: Boolean = false) {
        viewModelScope.launch {
            themeLocalStore.upsertTheme(theme.copy(isDraft = true))
            themeLocalStore.clearAutoDraft()
            if (activate) {
                themeLocalStore.setActiveThemeId(theme.id)
                _snackbarMessage.value = "Draft \"${theme.name}\" saved & activated for testing"
            } else {
                _snackbarMessage.value = "Draft \"${theme.name}\" saved locally"
            }
        }
    }

    fun setActiveButtonTheme(themeId: String?) {
        viewModelScope.launch {
            themeLocalStore.setActiveThemeId(themeId)
            val active = themeLocalStore.getActiveTheme()
            _snackbarMessage.value = if (active != null) {
                "Active Button Theme: ${active.name} (auto-applied to new buttons)"
            } else {
                "Using default button style"
            }
        }
    }

    fun deleteLocalButtonTheme(themeId: String) {
        viewModelScope.launch {
            themeLocalStore.deleteTheme(themeId)
            _snackbarMessage.value = "Removed local button theme"
        }
    }

    fun downloadWorkshopTheme(theme: ButtonThemePreset) {
        viewModelScope.launch {
            val localCopy = theme.copy(isDraft = false, isPublished = true)
            themeLocalStore.upsertTheme(localCopy)
            themeLocalStore.setActiveThemeId(localCopy.id)
            communityRepo.incrementThemeDownload(theme.id)
            _workshopThemes.update { list ->
                list.map { if (it.id == theme.id) it.copy(downloadsCount = it.downloadsCount + 1) else it }
            }
            _snackbarMessage.value = "Downloaded & activated \"${theme.name}\" — new buttons will use this style!"
        }
    }

    fun publishButtonTheme(theme: ButtonThemePreset, onPublished: () -> Unit = {}) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google first to publish to the Button Theme Workshop"
            return
        }
        viewModelScope.launch {
            _isPublishingTheme.value = true
            val author = requireAuthorProfile()
            if (author == null) {
                _isPublishingTheme.value = false
                return@launch
            }
            communityRepo.publishTheme(theme, author)
                .onSuccess { published ->
                    themeLocalStore.upsertTheme(published)
                    themeLocalStore.clearAutoDraft()
                    themeLocalStore.setActiveThemeId(published.id)
                    _workshopThemes.update { list ->
                        listOf(published) + list.filterNot { it.id == published.id }
                    }
                    _myProfileThemes.update { list ->
                        listOf(published) + list.filterNot { it.id == published.id }
                    }
                    _snackbarMessage.value = "Published \"${published.name}\" to the Button Theme Workshop!"
                    onPublished()
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to publish button theme"
                }
            _isPublishingTheme.value = false
        }
    }

    fun deletePublishedButtonTheme(themeId: String) {
        viewModelScope.launch {
            communityRepo.deletePublishedTheme(themeId)
                .onSuccess {
                    _workshopThemes.update { it.filterNot { t -> t.id == themeId } }
                    _myProfileThemes.update { it.filterNot { t -> t.id == themeId } }
                    _viewedProfileThemes.update { it.filterNot { t -> t.id == themeId } }
                    _snackbarMessage.value = "Removed theme from Workshop"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not remove theme"
                }
        }
    }

    fun refreshWorkshopAndCommunity() {
        val user = _cloudUser.value ?: return
        viewModelScope.launch {
            _isCommunityLoading.value = true
            communityRepo.getPublicProfile(user.uid).onSuccess { loaded ->
                if (loaded != null) _myPublicProfile.value = loaded else requireAuthorProfile()
            }
            communityRepo.fetchAllCosmeticItems().onSuccess { _cosmeticItems.value = it }
            communityRepo.fetchOwnedItemIds(user.uid).onSuccess { _myOwnedCosmeticIds.value = it }
            communityRepo.fetchWorkshopThemes().onSuccess { _workshopThemes.value = it }
            communityRepo.fetchFavoriteThemeIds().onSuccess { _favoriteThemeIds.value = it }
            communityRepo.fetchCommunityPosts().onSuccess { _communityPosts.value = it }
            communityRepo.fetchUserClips(user.uid).onSuccess { _myProfileClips.value = it }
            communityRepo.fetchUserPublishedThemes(user.uid).onSuccess { _myProfileThemes.value = it }
            communityRepo.fetchUserPosts(user.uid).onSuccess { _myProfilePosts.value = it }
            _isCommunityLoading.value = false
        }
    }

    fun toggleLikeTheme(theme: ButtonThemePreset) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to like themes"
            return
        }
        val currentlyLiked = theme.id in _likedThemeIds.value
        viewModelScope.launch {
            communityRepo.toggleLikeTheme(theme.id, currentlyLiked)
                .onSuccess { nowLiked ->
                    _likedThemeIds.update { if (nowLiked) it + theme.id else it - theme.id }
                    val delta = if (nowLiked) 1 else -1
                    _workshopThemes.update { list ->
                        list.map {
                            if (it.id == theme.id) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it
                        }
                    }
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update like" }
        }
    }

    fun toggleFavoriteTheme(theme: ButtonThemePreset) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to favorite themes"
            return
        }
        val currentlyFav = theme.id in _favoriteThemeIds.value
        viewModelScope.launch {
            communityRepo.toggleFavoriteTheme(theme.id, currentlyFav)
                .onSuccess { nowFav ->
                    _favoriteThemeIds.update { if (nowFav) it + theme.id else it - theme.id }
                    val delta = if (nowFav) 1 else -1
                    _workshopThemes.update { list ->
                        list.map {
                            if (it.id == theme.id) it.copy(favoritesCount = (it.favoritesCount + delta).coerceAtLeast(0)) else it
                        }
                    }
                }
                .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update favorite" }
        }
    }

    fun loadThemeComments(themeId: String, onLoaded: (List<ThemeComment>) -> Unit) {
        viewModelScope.launch {
            val list = communityRepo.fetchThemeComments(themeId).getOrDefault(emptyList())
            onLoaded(list)
        }
    }

    fun addThemeComment(themeId: String, text: String, onAdded: (ThemeComment?) -> Unit) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to comment"
            onAdded(null)
            return
        }
        viewModelScope.launch {
            val author = requireAuthorProfile() ?: return@launch
            communityRepo.addThemeComment(themeId, author, text)
                .onSuccess { comment ->
                    _workshopThemes.update { list ->
                        list.map { if (it.id == themeId) it.copy(commentsCount = it.commentsCount + 1) else it }
                    }
                    onAdded(comment)
                }
                .onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Failed to post comment"
                    onAdded(null)
                }
        }
    }

    fun deleteThemeComment(themeId: String, commentId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            communityRepo.deleteThemeComment(themeId, commentId)
                .onSuccess {
                    _workshopThemes.update { list ->
                        list.map {
                            if (it.id == themeId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it
                        }
                    }
                    onDeleted()
                }
        }
    }

    fun saveMyPublicProfile(username: String, bio: String) {
        val user = _cloudUser.value ?: return
        val existing = _myPublicProfile.value
        viewModelScope.launch {
            communityRepo.upsertPublicProfile(
                username = username,
                bio = bio,
                avatarBase64 = existing?.avatarBase64,
                avatarUrl = existing?.avatarUrl ?: user.photoUrl
            ).onSuccess { updated ->
                _myPublicProfile.value = updated
                _snackbarMessage.value = "Profile updated as ${updated.username}"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not save profile"
            }
        }
    }

    fun uploadMyProfileAvatar(uri: Uri) {
        val user = _cloudUser.value ?: return
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(
                context = getApplication(),
                uri = uri,
                cropSquare = true,
                maxDimPx = 144,
                maxEncodedBytes = 42_000
            ).onSuccess { encoded ->
                val current = requireAuthorProfile()
                communityRepo.upsertPublicProfile(
                    username = current?.username ?: user.displayName,
                    bio = current?.bio.orEmpty(),
                    avatarBase64 = encoded,
                    avatarUrl = current?.avatarUrl ?: user.photoUrl
                ).onSuccess { updated ->
                    _myPublicProfile.value = updated
                    _snackbarMessage.value = "Profile picture updated!"
                }.onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Could not save profile picture"
                }
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Could not read selected image"
            }
        }
    }

    fun removeMyProfileAvatar() {
        val user = _cloudUser.value ?: return
        val current = _myPublicProfile.value ?: return
        viewModelScope.launch {
            communityRepo.upsertPublicProfile(
                username = current.username,
                bio = current.bio,
                avatarBase64 = null,
                avatarUrl = user.photoUrl
            ).onSuccess { updated ->
                _myPublicProfile.value = updated
                _snackbarMessage.value = "Profile picture removed"
            }
        }
    }

    fun loadPublicProfileBundle(userId: String) {
        viewModelScope.launch {
            _isCommunityLoading.value = true
            communityRepo.fetchAllCosmeticItems().onSuccess { _cosmeticItems.value = it }
            _viewedPublicProfile.value = communityRepo.getPublicProfile(userId).getOrNull()
            _viewedProfileThemes.value = communityRepo.fetchUserPublishedThemes(userId).getOrDefault(emptyList())
            _viewedProfileClips.value = communityRepo.fetchUserClips(userId).getOrDefault(emptyList())
            _viewedProfilePosts.value = communityRepo.fetchUserPosts(userId).getOrDefault(emptyList())
            _isCommunityLoading.value = false
        }
    }

    fun postGalleryClipToProfile(
        item: CapturedMediaItem,
        title: String,
        gameTag: String,
        caption: String
    ) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google first to post clips to your Profile"
            return
        }
        viewModelScope.launch {
            val author = requireAuthorProfile() ?: return@launch
            val extracted = withContext(Dispatchers.IO) {
                extractClipThumbnailAndStoryboard(item)
            }
            communityRepo.publishClipToProfile(
                authorProfile = author,
                title = title.ifBlank { item.fileName },
                gameTag = gameTag,
                caption = caption,
                durationMs = item.durationMs,
                thumbnailBase64 = extracted.first,
                storyboardFrames = extracted.second,
                localMediaUri = item.filePath
            ).onSuccess { clip ->
                _myProfileClips.update { listOf(clip) + it }
                _snackbarMessage.value = "Posted \"${clip.title}\" to your Profile!"
            }.onFailure {
                _snackbarMessage.value = it.localizedMessage ?: "Failed to post clip to profile"
            }
        }
    }

    private fun extractClipThumbnailAndStoryboard(item: CapturedMediaItem): Pair<String?, List<String>> {
        val file = File(item.filePath)
        if (!file.exists()) return null to emptyList()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = item.durationMs.takeIf { it > 0L }
                ?: (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L)
            val thumbBmp = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val thumbB64 = thumbBmp?.let { bmp ->
                val res = runCatching {
                    ButtonImageCodec.encodeBitmapToBase64(bmp, cropSquare = false, maxDimPx = 320, maxEncodedBytes = 55_000)
                }.getOrNull()
                bmp.recycle()
                res
            }
            val frames = mutableListOf<String>()
            val frameCount = 5
            val stepUs = if (durationMs > 500L) (durationMs * 1000L) / frameCount else 0L
            for (i in 0 until frameCount) {
                val timeUs = if (stepUs > 0L) i * stepUs else (i * 500_000L)
                val frame = runCatching {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }.getOrNull() ?: continue
                runCatching {
                    ButtonImageCodec.encodeBitmapToBase64(frame, cropSquare = false, maxDimPx = 240, maxEncodedBytes = 32_000)
                }.getOrNull()?.let { frames.add(it) }
                frame.recycle()
            }
            thumbB64 to frames
        } catch (_: Exception) {
            null to emptyList<String>()
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun deleteProfileClip(clipId: String) {
        viewModelScope.launch {
            communityRepo.deleteProfileClip(clipId).onSuccess {
                _myProfileClips.update { it.filterNot { c -> c.id == clipId } }
                _viewedProfileClips.update { it.filterNot { c -> c.id == clipId } }
                _snackbarMessage.value = "Removed clip from profile"
            }
        }
    }

    fun toggleLikeClip(clip: ProfileClipPost) {
        if (_cloudUser.value == null) return
        val liked = clip.id in _likedClipIds.value
        viewModelScope.launch {
            communityRepo.toggleLikeClip(clip.id, liked).onSuccess { nowLiked ->
                _likedClipIds.update { if (nowLiked) it + clip.id else it - clip.id }
                val delta = if (nowLiked) 1 else -1
                _myProfileClips.update { list ->
                    list.map { if (it.id == clip.id) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it }
                }
                _viewedProfileClips.update { list ->
                    list.map { if (it.id == clip.id) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it }
                }
            }
        }
    }

    fun createCommunityPost(topic: String, title: String, body: String, imageUri: Uri?) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to create posts"
            return
        }
        viewModelScope.launch {
            val author = requireAuthorProfile() ?: return@launch
            val imgB64 = if (imageUri != null) {
                ButtonImageCodec.encodeUriToBase64(
                    context = getApplication(),
                    uri = imageUri,
                    cropSquare = false,
                    maxDimPx = 360,
                    maxEncodedBytes = 85_000
                ).getOrNull()
            } else {
                null
            }
            communityRepo.createCommunityPost(author, topic, title, body, imgB64)
                .onSuccess { post ->
                    _communityPosts.update { listOf(post) + it }
                    _myProfilePosts.update { listOf(post) + it }
                    _snackbarMessage.value = "Posted to $topic!"
                }
                .onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Failed to publish post"
                }
        }
    }

    fun deleteCommunityPost(postId: String) {
        viewModelScope.launch {
            communityRepo.deleteCommunityPost(postId).onSuccess {
                _communityPosts.update { it.filterNot { p -> p.id == postId } }
                _myProfilePosts.update { it.filterNot { p -> p.id == postId } }
                _viewedProfilePosts.update { it.filterNot { p -> p.id == postId } }
                _snackbarMessage.value = "Deleted post"
            }
        }
    }

    fun toggleLikePost(post: CommunityTopicPost) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to like posts"
            return
        }
        val liked = post.id in _likedPostIds.value
        viewModelScope.launch {
            communityRepo.toggleLikePost(post.id, liked).onSuccess { nowLiked ->
                _likedPostIds.update { if (nowLiked) it + post.id else it - post.id }
                val delta = if (nowLiked) 1 else -1
                _communityPosts.update { list ->
                    list.map { if (it.id == post.id) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it }
                }
                _myProfilePosts.update { list ->
                    list.map { if (it.id == post.id) it.copy(likesCount = (it.likesCount + delta).coerceAtLeast(0)) else it }
                }
            }
        }
    }

    fun loadPostComments(postId: String, onLoaded: (List<PostComment>) -> Unit) {
        viewModelScope.launch {
            onLoaded(communityRepo.fetchPostComments(postId).getOrDefault(emptyList()))
        }
    }

    fun addPostComment(postId: String, text: String, onAdded: (PostComment?) -> Unit) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google to reply"
            onAdded(null)
            return
        }
        viewModelScope.launch {
            val author = requireAuthorProfile() ?: return@launch
            communityRepo.addPostComment(postId, author, text)
                .onSuccess { reply ->
                    _communityPosts.update { list ->
                        list.map { if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it }
                    }
                    onAdded(reply)
                }
                .onFailure {
                    _snackbarMessage.value = it.localizedMessage ?: "Failed to send reply"
                    onAdded(null)
                }
        }
    }

    fun deletePostComment(postId: String, commentId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            communityRepo.deletePostComment(postId, commentId).onSuccess {
                _communityPosts.update { list ->
                    list.map { if (it.id == postId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it }
                }
                onDeleted()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cosmetic Shop, Wardrobe & Restricted Admin Management System
    // -------------------------------------------------------------------------

    fun isCurrentUserPrimaryAdmin(): Boolean =
        AdminConfig.isPrimaryAdminEmail(_cloudUser.value?.email)

    fun refreshCosmeticsAndInventory() {
        val user = _cloudUser.value ?: return
        viewModelScope.launch {
            communityRepo.fetchAllCosmeticItems().onSuccess { _cosmeticItems.value = it }
            communityRepo.fetchOwnedItemIds(user.uid).onSuccess { _myOwnedCosmeticIds.value = it }
            communityRepo.getPublicProfile(user.uid).onSuccess { loaded ->
                if (loaded != null) _myPublicProfile.value = loaded
            }
        }
    }

    fun claimFreeShopItem(item: CosmeticItem) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google first to claim free shop items"
            return
        }
        if (item.availability != ItemAvailability.FREE_SHOP) {
            _snackbarMessage.value = "\"${item.name}\" is Exclusive and can only be granted by Admin"
            return
        }
        viewModelScope.launch {
            requireAuthorProfile()
            communityRepo.claimFreeShopItem(item.id)
                .onSuccess {
                    _myOwnedCosmeticIds.update { it + item.id }
                    _snackbarMessage.value = "Claimed \"${item.name}\" for free! You can equip it now."
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not claim item"
                }
        }
    }

    fun equipCosmeticItem(type: CosmeticItemType, itemId: String?) {
        if (_cloudUser.value == null) {
            _snackbarMessage.value = "Sign in with Google first to equip items"
            return
        }
        viewModelScope.launch {
            requireAuthorProfile()
            communityRepo.equipCosmeticItem(type, itemId)
                .onSuccess {
                    _myPublicProfile.update { current ->
                        current?.let { p ->
                            when (type) {
                                CosmeticItemType.FRAME -> p.copy(equippedFrameId = itemId)
                                CosmeticItemType.NAME_TAG -> p.copy(equippedNameTagId = itemId)
                                CosmeticItemType.VERIFICATION_ICON -> p.copy(equippedVerificationIconId = itemId)
                            }
                        }
                    }
                    _snackbarMessage.value = if (itemId == null) {
                        "Unequipped ${type.label}"
                    } else {
                        "Equipped ${type.label} on your profile"
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not equip item"
                }
        }
    }

    fun adminCreateCosmeticItem(
        name: String,
        description: String,
        type: CosmeticItemType,
        availability: ItemAvailability,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        textColorHex: Long,
        borderWidthDp: Float,
        tagText: String,
        iconPreset: String,
        imageUri: Uri?,
        onCreated: () -> Unit = {}
    ) {
        if (!isCurrentUserPrimaryAdmin()) {
            _snackbarMessage.value = "Restricted: Admin access only"
            return
        }
        viewModelScope.launch {
            _isAdminBusy.value = true
            val encodedImage = imageUri?.let { uri ->
                com.example.util.ButtonImageCodec.encodeUriToBase64(
                    context = getApplication(),
                    uri = uri,
                    cropSquare = true,
                    maxDimPx = 144,
                    maxEncodedBytes = 42 * 1024
                ).getOrNull()
            }
            communityRepo.createCosmeticItem(
                name = name,
                description = description,
                type = type,
                availability = availability,
                primaryColorHex = primaryColorHex,
                secondaryColorHex = secondaryColorHex,
                textColorHex = textColorHex,
                borderWidthDp = borderWidthDp,
                tagText = tagText,
                iconPreset = iconPreset,
                imageBase64 = encodedImage
            ).onSuccess { created ->
                _cosmeticItems.update { listOf(created) + it }
                _snackbarMessage.value = "Created ${created.type.label} \"${created.name}\" (${created.availability.shortBadge})"
                onCreated()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to create item"
            }
            _isAdminBusy.value = false
        }
    }

    fun adminToggleCosmeticAvailability(item: CosmeticItem) {
        if (!isCurrentUserPrimaryAdmin()) return
        val next = if (item.availability == ItemAvailability.FREE_SHOP) {
            ItemAvailability.EXCLUSIVE
        } else {
            ItemAvailability.FREE_SHOP
        }
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.updateCosmeticItemAvailability(item.id, next)
                .onSuccess {
                    _cosmeticItems.update { list ->
                        list.map { if (it.id == item.id) it.copy(availability = next) else it }
                    }
                    _snackbarMessage.value = "\"${item.name}\" is now ${next.label}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update availability"
                }
            _isAdminBusy.value = false
        }
    }

    fun adminDeleteCosmeticItem(item: CosmeticItem) {
        if (!isCurrentUserPrimaryAdmin()) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.deleteCosmeticItem(item.id)
                .onSuccess {
                    _cosmeticItems.update { list -> list.filterNot { it.id == item.id } }
                    _snackbarMessage.value = "Deleted \"${item.name}\""
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to delete item"
                }
            _isAdminBusy.value = false
        }
    }

    fun adminSearchUsers(query: String) {
        if (!isCurrentUserPrimaryAdmin()) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.adminSearchUsersByUsername(query)
                .onSuccess { users ->
                    _adminSearchResults.value = users
                    val selectedUid = _adminSelectedUser.value?.uid
                    if (selectedUid != null) {
                        users.firstOrNull { it.uid == selectedUid }?.let { _adminSelectedUser.value = it }
                    }
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not search users"
                }
            _isAdminBusy.value = false
        }
    }

    fun adminSelectUser(user: CommunityUserProfile?) {
        _adminSelectedUser.value = user
        if (user == null || !isCurrentUserPrimaryAdmin()) {
            _adminSelectedUserOwnedIds.value = emptySet()
            return
        }
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.fetchOwnedItemIds(user.uid)
                .onSuccess { _adminSelectedUserOwnedIds.value = it }
                .onFailure { _adminSelectedUserOwnedIds.value = emptySet() }
            _isAdminBusy.value = false
        }
    }

    fun adminGrantItemToSelectedUser(item: CosmeticItem) {
        if (!isCurrentUserPrimaryAdmin()) return
        val target = _adminSelectedUser.value ?: return
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.adminGrantItemToUser(target.uid, item.id)
                .onSuccess {
                    _adminSelectedUserOwnedIds.update { it + item.id }
                    if (target.uid == _cloudUser.value?.uid) {
                        _myOwnedCosmeticIds.update { it + item.id }
                    }
                    _snackbarMessage.value = "Granted \"${item.name}\" to ${target.username}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to grant item"
                }
            _isAdminBusy.value = false
        }
    }

    fun adminRevokeItemFromSelectedUser(item: CosmeticItem) {
        if (!isCurrentUserPrimaryAdmin()) return
        val target = _adminSelectedUser.value ?: return
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.adminRevokeItemFromUser(target.uid, item.id, target)
                .onSuccess {
                    _adminSelectedUserOwnedIds.update { it - item.id }
                    if (target.uid == _cloudUser.value?.uid) {
                        _myOwnedCosmeticIds.update { it - item.id }
                        _myPublicProfile.update { p ->
                            p?.copy(
                                equippedFrameId = if (p.equippedFrameId == item.id) null else p.equippedFrameId,
                                equippedNameTagId = if (p.equippedNameTagId == item.id) null else p.equippedNameTagId,
                                equippedVerificationIconId = if (p.equippedVerificationIconId == item.id) null else p.equippedVerificationIconId
                            )
                        }
                    }
                    _snackbarMessage.value = "Revoked \"${item.name}\" from ${target.username}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to revoke item"
                }
            _isAdminBusy.value = false
        }
    }

    fun adminUpdateUserRoles(target: CommunityUserProfile, adminRole: Boolean, verifiedRole: Boolean) {
        if (!isCurrentUserPrimaryAdmin()) return
        viewModelScope.launch {
            _isAdminBusy.value = true
            communityRepo.adminSetUserRoles(target.uid, adminRole, verifiedRole)
                .onSuccess {
                    val updated = target.copy(adminRole = adminRole, verifiedRole = verifiedRole)
                    _adminSearchResults.update { list -> list.map { if (it.uid == target.uid) updated else it } }
                    if (_adminSelectedUser.value?.uid == target.uid) {
                        _adminSelectedUser.value = updated
                    }
                    if (_myPublicProfile.value?.uid == target.uid) {
                        _myPublicProfile.value = _myPublicProfile.value?.copy(
                            adminRole = adminRole,
                            verifiedRole = verifiedRole
                        )
                    }
                    if (_viewedPublicProfile.value?.uid == target.uid) {
                        _viewedPublicProfile.value = _viewedPublicProfile.value?.copy(
                            adminRole = adminRole,
                            verifiedRole = verifiedRole
                        )
                    }
                    _snackbarMessage.value = "Updated roles for ${target.username}"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Failed to update roles"
                }
            _isAdminBusy.value = false
        }
    }
}
