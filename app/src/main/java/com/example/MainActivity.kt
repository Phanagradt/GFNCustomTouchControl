package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.input.InputBridge
import com.example.model.BindingDomain
import com.example.model.ControllerProfile
import com.example.service.OverlayControllerService
import com.example.ui.NexusViewModel
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.GameFilterScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.TestArenaScreen
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.NexusPadTheme
import com.example.util.SystemIntegrations

object NexusRoutes {
    const val COCKPIT = "cockpit"
    const val PROFILES = "profiles"
    const val FILTERS = "filters"
    const val SANDBOX = "sandbox"
    const val SETUP = "setup"
    const val EDITOR = "editor/{profileId}"

    fun editor(profileId: String): String = "editor/$profileId"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PrimaryTabs = listOf(
    NavTab(NexusRoutes.COCKPIT, "Cockpit", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_cockpit"),
    NavTab(NexusRoutes.PROFILES, "Profiles", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy, "nav_profiles"),
    NavTab(NexusRoutes.FILTERS, "Filters", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_filters"),
    NavTab(NexusRoutes.SANDBOX, "Sandbox", Icons.Filled.TouchApp, Icons.Outlined.TouchApp, "nav_sandbox"),
    NavTab(NexusRoutes.SETUP, "Setup", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_setup")
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NexusViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            NexusPadTheme(darkTheme = settings.darkTheme) {
                NexusPadAppShell(viewModel = viewModel)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        val isExternalKey = event.deviceId > 0 && event.keyCode != KeyEvent.KEYCODE_BACK
        if ((isGamepad || isExternalKey) && (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP)) {
            if (event.repeatCount == 0) {
                InputBridge.recordPhysicalInput(
                    keyCode = event.keyCode,
                    actionName = if (event.action == KeyEvent.ACTION_DOWN) "HW_DOWN" else "HW_UP",
                    detail = if (isGamepad) "Gamepad" else "Keyboard",
                    domain = if (isGamepad) BindingDomain.CONTROLLER else BindingDomain.KEYBOARD
                )
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NexusPadAppShell(viewModel: NexusViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val pressedIds by viewModel.pressedControlIds.collectAsStateWithLifecycle()
    val toggledIds by viewModel.toggledControlIds.collectAsStateWithLifecycle()
    val leftStick by viewModel.leftStick.collectAsStateWithLifecycle()
    val rightStick by viewModel.rightStick.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var pendingExportProfile by remember { mutableStateOf<ControllerProfile?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemStatus()
        if (granted && SystemIntegrations.canDrawOverlays(context)) {
            OverlayControllerService.start(context)
            if (settings.autoLaunchGeForceNow) {
                SystemIntegrations.launchGeForceNowOrStore(context)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!content.isNullOrBlank()) {
                viewModel.importProfileJson(content)
            } else {
                viewModel.showMessage("Could not read selected JSON file")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val profile = pendingExportProfile
        pendingExportProfile = null
        if (uri != null && profile != null) {
            val json = viewModel.exportProfileJson(profile)
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray())
                }
            }.isSuccess
            viewModel.showMessage(if (saved) "Exported \"${profile.name}\"" else "Failed to export profile")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val toggleOverlayAction = {
        if (isOverlayRunning) {
            OverlayControllerService.stop(context)
        } else if (!SystemIntegrations.canDrawOverlays(context)) {
            showOverlayPermissionDialog = true
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !SystemIntegrations.hasNotificationPermission(context)
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                OverlayControllerService.start(context)
                if (settings.autoLaunchGeForceNow) {
                    SystemIntegrations.launchGeForceNowOrStore(context)
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInEditor = currentRoute?.startsWith("editor/") == true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (!isInEditor) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = GeForceLime,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "NexusPad",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!isInEditor && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInEditor) Modifier else Modifier.padding(innerPadding)
                    )
            ) {
                if (!isInEditor && useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = NexusRoutes.COCKPIT
                    ) {
                        composable(NexusRoutes.COCKPIT) {
                            CockpitScreen(
                                activeProfile = activeProfile,
                                allProfiles = profiles,
                                readiness = readiness,
                                isOverlayRunning = isOverlayRunning,
                                vibrationMode = settings.vibrationMode,
                                onVibrationModeChange = viewModel::setVibrationMode,
                                onToggleOverlay = toggleOverlayAction,
                                onSelectProfile = viewModel::setActiveProfile,
                                onOpenEditor = { id ->
                                    viewModel.openEditor(id)
                                    navController.navigate(NexusRoutes.editor(id))
                                },
                                onOpenSandbox = {
                                    navController.navigate(NexusRoutes.SANDBOX) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onRequestOverlayPermission = {
                                    SystemIntegrations.openOverlayPermissionSettings(context)
                                },
                                onOpenImeSettings = {
                                    if (readiness.isImeEnabled) {
                                        SystemIntegrations.showInputMethodPicker(context)
                                    } else {
                                        SystemIntegrations.openInputMethodSettings(context)
                                    }
                                },
                                onOpenAccessibilitySettings = {
                                    SystemIntegrations.openAccessibilitySettings(context)
                                },
                                onLaunchGeForceNow = {
                                    val launched = SystemIntegrations.launchGeForceNowOrStore(context)
                                    if (!launched) {
                                        viewModel.showMessage("GeForce NOW not installed — opening Play Store")
                                    }
                                }
                            )
                        }

                        composable(NexusRoutes.PROFILES) {
                            ProfilesScreen(
                                profiles = profiles,
                                activeProfileId = activeProfile?.id,
                                onSetActive = viewModel::setActiveProfile,
                                onEditProfile = { id ->
                                    viewModel.openEditor(id)
                                    navController.navigate(NexusRoutes.editor(id))
                                },
                                onDuplicateProfile = viewModel::duplicateProfile,
                                onDeleteProfile = viewModel::deleteProfile,
                                onCreateProfile = { name, game, genre, template ->
                                    viewModel.createProfile(name, game, genre, template) { newId ->
                                        viewModel.openEditor(newId)
                                        navController.navigate(NexusRoutes.editor(newId))
                                    }
                                },
                                onImportJsonClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                                },
                                onExportProfileClick = { profile ->
                                    pendingExportProfile = profile
                                    val safeFileName = profile.name
                                        .lowercase()
                                        .replace(Regex("[^a-z0-9]+"), "_")
                                        .trim('_')
                                        .ifEmpty { "nexuspad_profile" }
                                    exportLauncher.launch("$safeFileName.json")
                                }
                            )
                        }

                        composable(NexusRoutes.FILTERS) {
                            GameFilterScreen(
                                filterState = settings.gameFilter,
                                isOverlayRunning = isOverlayRunning,
                                onSelectSlot = viewModel::selectFilterSlot,
                                onUpdateStyle = viewModel::updateFilterStyle,
                                onResetSlot = viewModel::resetActiveFilterSlot,
                                onToggleOverlay = toggleOverlayAction
                            )
                        }

                        composable(NexusRoutes.SANDBOX) {
                            TestArenaScreen(
                                activeProfile = activeProfile,
                                allProfiles = profiles,
                                settings = settings,
                                isOverlayRunning = isOverlayRunning,
                                pressedIds = pressedIds,
                                toggledIds = toggledIds,
                                leftStick = leftStick,
                                rightStick = rightStick,
                                telemetry = telemetry,
                                onSelectProfile = viewModel::setActiveProfile,
                                onEditProfile = { id ->
                                    viewModel.openEditor(id)
                                    navController.navigate(NexusRoutes.editor(id))
                                },
                                onToggleOverlay = toggleOverlayAction,
                                onClearTelemetry = viewModel::clearTelemetry
                            )
                        }

                        composable(NexusRoutes.SETUP) {
                            SetupScreen(
                                settings = settings,
                                readiness = readiness,
                                onVibrationModeChange = viewModel::setVibrationMode,
                                onOpacityChange = viewModel::setGlobalOpacity,
                                onGridSnapChange = viewModel::setGridSnapEnabled,
                                onNotationChange = viewModel::setControllerNotation,
                                onDarkThemeChange = viewModel::setDarkTheme,
                                onShowTelemetryChange = viewModel::setShowTelemetryOverlay,
                                onAutoLaunchChange = viewModel::setAutoLaunchGeForceNow,
                                onRequestOverlayPermission = {
                                    SystemIntegrations.openOverlayPermissionSettings(context)
                                },
                                onOpenImeSettings = {
                                    SystemIntegrations.openInputMethodSettings(context)
                                },
                                onShowImePicker = {
                                    SystemIntegrations.showInputMethodPicker(context)
                                },
                                onOpenAccessibilitySettings = {
                                    SystemIntegrations.openAccessibilitySettings(context)
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            )
                        }

                        composable(
                            route = NexusRoutes.EDITOR,
                            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val profileId = backStackEntry.arguments?.getString("profileId")
                            LaunchedEffect(profileId) {
                                if (profileId != null && editorState.profile?.id != profileId) {
                                    viewModel.openEditor(profileId)
                                }
                            }
                            LayoutEditorScreen(
                                editorState = editorState,
                                settings = settings,
                                pressedIds = pressedIds,
                                toggledIds = toggledIds,
                                latestTelemetry = telemetry.firstOrNull(),
                                onSelectControl = viewModel::selectControl,
                                onMoveControl = { id, dx, dy, w, h ->
                                    viewModel.moveControlByDelta(
                                        controlId = id,
                                        dxPx = dx,
                                        dyPx = dy,
                                        canvasWidthPx = w,
                                        canvasHeightPx = h,
                                        snapToGrid = settings.gridSnapEnabled
                                    )
                                },
                                onUpdateControl = viewModel::updateControl,
                                onPickCustomImage = viewModel::applyCustomButtonImage,
                                onAddControl = viewModel::addControl,
                                onDuplicateControl = viewModel::duplicateSelectedControl,
                                onMirrorControl = viewModel::mirrorDuplicateSelectedControl,
                                onNudgeControl = viewModel::nudgeSelectedControl,
                                onDeleteControl = viewModel::deleteSelectedControl,
                                onTogglePlayTest = viewModel::togglePlayTestMode,
                                onUndo = viewModel::undoEditorChange,
                                onSave = { viewModel.saveEditorProfile() },
                                onUpdateProfileMeta = viewModel::updateProfileMeta,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Enable Floating Game Overlay") },
            text = {
                Text(
                    "To display your custom virtual controller buttons and keyboard hotkeys on top of GeForce NOW, grant NexusPad the \"Display over other apps\" permission in Android Settings."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showOverlayPermissionDialog = false
                    SystemIntegrations.openOverlayPermissionSettings(context)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }
}
