package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.input.InputBridge
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.CommandCatalog
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
import com.example.util.SystemIntegrations
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
    val hasNotificationPermission: Boolean = true,
    val isGeForceNowInstalled: Boolean = false
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

    private val _readiness = MutableStateFlow(SystemReadiness())
    val readiness: StateFlow<SystemReadiness> = _readiness.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepo.ensureSeeded()
        }
        refreshSystemStatus()
    }

    fun refreshSystemStatus() {
        val ctx = getApplication<Application>()
        _readiness.value = SystemReadiness(
            canDrawOverlays = SystemIntegrations.canDrawOverlays(ctx),
            isImeEnabled = SystemIntegrations.isImeEnabled(ctx),
            isImeSelected = SystemIntegrations.isImeSelected(ctx),
            hasNotificationPermission = SystemIntegrations.hasNotificationPermission(ctx),
            isGeForceNowInstalled = SystemIntegrations.isGeForceNowInstalled(ctx)
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

    // Settings
    fun setVibrationMode(mode: com.example.data.VibrationMode) =
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
