package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CloudProfileRepository
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.data.RedeemedSharedLayout
import com.example.data.SharedCodeTicket
import com.example.data.VibrationMode
import com.example.input.InputBridge
import com.example.model.BindingDomain
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerNotation
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.PresetProfiles
import com.example.model.ProfileTemplate
import com.example.model.StickState
import com.example.model.TelemetryEvent
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.util.ButtonImageCodec
import com.example.util.SystemIntegrations
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

data class SystemReadiness(
    val canDrawOverlays: Boolean = false,
    val isImeEnabled: Boolean = false,
    val isImeSelected: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isGeForceNowInstalled: Boolean = false
)

data class AccountUiState(
    val uid: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val isSyncing: Boolean = false,
    val syncedLayoutCount: Int = 0,
    val lastSyncTimeMs: Long? = null,
    val syncError: String? = null
)

sealed interface ShareCodeUiState {
    data object Idle : ShareCodeUiState
    data class Generating(val profileName: String) : ShareCodeUiState
    data class Ready(val ticket: SharedCodeTicket) : ShareCodeUiState
    data class Error(val message: String) : ShareCodeUiState
}

sealed interface ImportCodeUiState {
    data object Idle : ImportCodeUiState
    data class Loading(val code: String) : ImportCodeUiState
    data class Success(val redeemed: RedeemedSharedLayout) : ImportCodeUiState
    data class Error(val message: String) : ImportCodeUiState
}

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

class NexusViewModel(
    private val appContext: Context,
    private val profileRepo: ProfileRepository,
    private val prefsRepo: PreferencesRepository,
    private val cloudRepo: CloudProfileRepository,
    private val currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    userPhotoUrl: String?
) : ViewModel() {

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

    private val _readiness = MutableStateFlow(SystemReadiness())
    val readiness: StateFlow<SystemReadiness> = _readiness.asStateFlow()

    private val _accountState = MutableStateFlow(
        AccountUiState(
            uid = currentUserId,
            displayName = userDisplayName.ifBlank { "NexusPad Player" },
            email = userEmail,
            photoUrl = userPhotoUrl,
            isSyncing = true
        )
    )
    val accountState: StateFlow<AccountUiState> = _accountState.asStateFlow()

    private val _shareCodeState = MutableStateFlow<ShareCodeUiState>(ShareCodeUiState.Idle)
    val shareCodeState: StateFlow<ShareCodeUiState> = _shareCodeState.asStateFlow()

    private val _importCodeState = MutableStateFlow<ImportCodeUiState>(ImportCodeUiState.Idle)
    val importCodeState: StateFlow<ImportCodeUiState> = _importCodeState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var cloudSyncJob: Job? = null
    private var initialCloudUploadDone = false

    init {
        refreshSystemStatus()
        startCloudAccountAndSync(userDisplayName, userPhotoUrl)
    }

    private fun startCloudAccountAndSync(displayName: String, photoUrl: String?) {
        cloudSyncJob?.cancel()
        cloudSyncJob = viewModelScope.launch {
            profileRepo.ensureSeeded()
            cloudRepo.ensureUserAccount(displayName = displayName, photoUrl = photoUrl)
                .onFailure { err ->
                    _accountState.update {
                        it.copy(syncError = err.localizedMessage ?: "Account sync failed")
                    }
                }

            cloudRepo.observeCloudLayouts(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    _accountState.update {
                        it.copy(
                            isSyncing = false,
                            syncError = root.localizedMessage ?: "Cloud sync paused"
                        )
                    }
                }
                .collect { cloudLayouts ->
                    profileRepo.upsertFromCloud(cloudLayouts)

                    if (!initialCloudUploadDone) {
                        initialCloudUploadDone = true
                        val cloudById = cloudLayouts.associateBy { it.id }
                        val localList = profileRepo.getAllProfilesOnce()
                        for (local in localList) {
                            val remote = cloudById[local.id]
                            val shouldUpload = remote == null ||
                                (local.updatedAt > ProfileRepository.STARTER_BASELINE_MS && local.updatedAt > remote.updatedAt)
                            if (shouldUpload) {
                                cloudRepo.upsertCloudLayout(local)
                            }
                        }
                    }

                    _accountState.update {
                        it.copy(
                            isSyncing = false,
                            syncedLayoutCount = cloudLayouts.size,
                            lastSyncTimeMs = System.currentTimeMillis(),
                            syncError = null
                        )
                    }
                }
        }
    }

    fun prepareForSignOut() {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
    }

    fun syncNow() {
        viewModelScope.launch {
            _accountState.update { it.copy(isSyncing = true, syncError = null) }
            val localProfiles = profileRepo.getAllProfilesOnce()
            var anyError: String? = null
            for (profile in localProfiles) {
                cloudRepo.upsertCloudLayout(profile).onFailure { err ->
                    anyError = err.localizedMessage ?: "Sync error"
                }
            }
            _accountState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTimeMs = System.currentTimeMillis(),
                    syncError = anyError
                )
            }
            _snackbarMessage.value = if (anyError == null) {
                "All ${localProfiles.size} layouts synced to NexusPad Cloud"
            } else {
                "Cloud sync warning: $anyError"
            }
        }
    }

    fun refreshSystemStatus() {
        _readiness.value = SystemReadiness(
            canDrawOverlays = SystemIntegrations.canDrawOverlays(appContext),
            isImeEnabled = SystemIntegrations.isImeEnabled(appContext),
            isImeSelected = SystemIntegrations.isImeSelected(appContext),
            hasNotificationPermission = SystemIntegrations.hasNotificationPermission(appContext),
            isGeForceNowInstalled = SystemIntegrations.isGeForceNowInstalled(appContext)
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
            val all = profileRepo.getAllProfilesOnce()
            all.filter { it.id == id || it.isActive }.forEach { profile ->
                cloudRepo.upsertCloudLayout(profile.copy(isActive = profile.id == id))
            }
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
            cloudRepo.upsertCloudLayout(saved.copy(isActive = true))
            onCreated(saved.id)
        }
    }

    fun duplicateProfile(source: ControllerProfile) {
        viewModelScope.launch {
            val copy = profileRepo.duplicateProfile(source)
            cloudRepo.upsertCloudLayout(copy)
            _snackbarMessage.value = "Duplicated \"${copy.name}\""
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            profileRepo.deleteProfile(id)
            cloudRepo.deleteCloudLayout(id)
            _snackbarMessage.value = "Profile removed"
        }
    }

    fun exportProfileJson(profile: ControllerProfile): String =
        profileRepo.exportProfileJson(profile)

    fun importProfileJson(json: String) {
        viewModelScope.launch {
            val result = profileRepo.importProfileJson(json)
            result.onSuccess { imported ->
                cloudRepo.upsertCloudLayout(imported)
                _snackbarMessage.value = "Imported \"${imported.name}\" (${imported.controls.size} controls)"
            }.onFailure {
                _snackbarMessage.value = "Could not import JSON: ${it.localizedMessage ?: "Invalid format"}"
            }
        }
    }

    // 5-Minute Expiring Share Code Flow
    fun generateShareCode(profile: ControllerProfile) {
        viewModelScope.launch {
            _shareCodeState.value = ShareCodeUiState.Generating(profile.name)
            val author = _accountState.value.displayName
            cloudRepo.createShareCode(profile, author)
                .onSuccess { ticket ->
                    _shareCodeState.value = ShareCodeUiState.Ready(ticket)
                }
                .onFailure { err ->
                    _shareCodeState.value = ShareCodeUiState.Error(
                        err.localizedMessage ?: "Failed to generate share code"
                    )
                }
        }
    }

    fun resetShareCodeState() {
        _shareCodeState.value = ShareCodeUiState.Idle
    }

    fun redeemShareCode(rawCode: String) {
        val trimmed = rawCode.trim()
        if (trimmed.isBlank()) {
            _importCodeState.value = ImportCodeUiState.Error("Please enter a share code")
            return
        }
        viewModelScope.launch {
            _importCodeState.value = ImportCodeUiState.Loading(trimmed.uppercase())
            cloudRepo.redeemShareCode(trimmed)
                .onSuccess { redeemed ->
                    val saved = profileRepo.saveProfile(redeemed.profile)
                    cloudRepo.upsertCloudLayout(saved)
                    _importCodeState.value = ImportCodeUiState.Success(redeemed.copy(profile = saved))
                    _snackbarMessage.value = "Saved \"${saved.name}\" permanently from ${redeemed.authorName}!"
                }
                .onFailure { err ->
                    _importCodeState.value = ImportCodeUiState.Error(
                        err.localizedMessage ?: "Could not import share code"
                    )
                }
        }
    }

    fun resetImportCodeState() {
        _importCodeState.value = ImportCodeUiState.Idle
    }

    // Custom Button Image Upload
    fun applyCustomButtonImage(controlId: String, imageUri: Uri) {
        viewModelScope.launch {
            ButtonImageCodec.encodeUriToBase64(appContext, imageUri)
                .onSuccess { encoded ->
                    mutateEditorProfile(selectId = controlId) { current ->
                        current.copy(
                            controls = current.controls.map { ctrl ->
                                if (ctrl.id == controlId) ctrl.copy(customImageBase64 = encoded) else ctrl
                            }
                        )
                    }
                    _snackbarMessage.value = "Custom button image applied — tap Save to sync"
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Failed to load image: ${err.localizedMessage ?: "Invalid file"}"
                }
        }
    }

    // Settings
    fun setVibrationMode(mode: VibrationMode) =
        viewModelScope.launch { prefsRepo.setVibrationMode(mode) }
    fun setGlobalOpacity(opacity: Float) = viewModelScope.launch { prefsRepo.setGlobalOpacity(opacity) }
    fun setGridSnapEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGridSnapEnabled(enabled) }
    fun setControllerNotation(notation: ControllerNotation) = viewModelScope.launch { prefsRepo.setControllerNotation(notation) }
    fun setDarkTheme(dark: Boolean) = viewModelScope.launch { prefsRepo.setDarkTheme(dark) }
    fun setShowTelemetryOverlay(show: Boolean) = viewModelScope.launch { prefsRepo.setShowTelemetryOverlay(show) }
    fun setAutoLaunchGeForceNow(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAutoLaunchGeForceNow(enabled) }

    // Layout Editor
    fun openEditor(profileId: String) {
        viewModelScope.launch {
            val loaded = profileRepo.getProfile(profileId) ?: activeProfile.first()
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
                    val rawX = (ctrl.xPercent + dxPx / canvasWidthPx).coerceIn(0.05f, 0.95f)
                    val rawY = (ctrl.yPercent + dyPx / canvasHeightPx).coerceIn(0.08f, 0.92f)
                    val snappedX = if (snapToGrid) ((rawX * 50f).roundToInt() / 50f) else rawX
                    val snappedY = if (snapToGrid) ((rawY * 50f).roundToInt() / 50f) else rawY
                    ctrl.copy(xPercent = snappedX, yPercent = snappedY)
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
            _editorState.update { it.copy(profile = saved, hasUnsavedChanges = false) }
            cloudRepo.upsertCloudLayout(saved)
                .onSuccess {
                    _snackbarMessage.value = "Saved & backed up \"${saved.name}\""
                }
                .onFailure {
                    _snackbarMessage.value = "Saved locally (cloud sync retry later)"
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

    override fun onCleared() {
        cloudSyncJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val cloudRepo: CloudProfileRepository,
        private val uid: String,
        private val displayName: String,
        private val email: String?,
        private val photoUrl: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = NexusDatabase.getInstance(appCtx)
            return NexusViewModel(
                appContext = appCtx,
                profileRepo = ProfileRepository(db.profileDao()),
                prefsRepo = PreferencesRepository(appCtx),
                cloudRepo = cloudRepo,
                currentUserId = uid,
                userDisplayName = displayName,
                userEmail = email,
                userPhotoUrl = photoUrl
            ) as T
        }
    }
}
