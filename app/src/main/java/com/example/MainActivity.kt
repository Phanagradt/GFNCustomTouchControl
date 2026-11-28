package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.model.AppLocalization
import com.example.model.BindingDomain
import com.example.model.ControllerProfile
import com.example.model.LocalNexusStrings
import com.example.service.OverlayControllerService
import com.example.service.ScreenCaptureService
import com.example.model.AppPlatformTheme
import com.example.ui.NexusViewModel
import com.example.ui.components.EpicBorderColor
import com.example.ui.components.EpicLauncherSidebarContent
import com.example.ui.components.EpicLeftDrawerOverlay
import com.example.ui.components.EpicSocialPanelDrawerOverlay
import com.example.ui.components.NintendoSwitchQuickMenuOverlay
import com.example.ui.components.PlatformBottomNavigationBar
import com.example.ui.components.PlatformNavItem
import com.example.ui.components.PlatformTopBar
import com.example.ui.components.Ps5ControlCenterOverlay
import com.example.ui.components.SteamBigPictureLeftMenuOverlay
import com.example.ui.components.SteamQuickAccessMenuOverlay
import com.example.ui.components.WhatsNewDialog
import com.example.ui.components.XboxGuideOverlay
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ButtonThemeCreatorScreen
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.CommunityHubScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.GameFilterScreen
import com.example.ui.screens.LayoutEditorScreen
import com.example.ui.screens.OnboardingSetupScreen
import com.example.ui.screens.PlatformStoreScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.TestArenaScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.NexusPadTheme
import com.example.update.UpdateUiState
import com.example.util.SystemIntegrations
import com.example.webplayer.XenosWebPlayerActivity

object NexusRoutes {
    const val COCKPIT = "cockpit"
    const val STORE = "store"
    const val PROFILES = "profiles"
    const val FILTERS = "filters"
    const val GALLERY = "gallery"
    const val SANDBOX = "sandbox"
    const val SETUP = "setup"
    const val EDITOR = "editor/{profileId}"
    const val WORKSHOP = "workshop/{tab}"
    const val THEME_CREATOR = "theme_creator?themeId={themeId}"
    const val USER_PROFILE = "user_profile/{userId}"
    const val ADMIN_PANEL = "admin_panel"

    fun editor(profileId: String): String = "editor/$profileId"
    fun workshop(tab: Int = 0): String = "workshop/$tab"
    fun themeCreator(themeId: String? = null): String =
        if (themeId.isNullOrBlank()) "theme_creator" else "theme_creator?themeId=$themeId"
    fun userProfile(userId: String): String = "user_profile/$userId"
}

class MainActivity : ComponentActivity() {

    private var visualsInitialTab by mutableIntStateOf(0)
    private var openVisualsTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntentExtras(intent)
        setContent {
            val viewModel: NexusViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val localizedStrings = remember(settings.appLanguage) {
                AppLocalization.stringsFor(settings.appLanguage)
            }

            CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                NexusPadTheme(
                    darkTheme = settings.darkTheme,
                    colorSchemeConfig = settings.colorScheme,
                    fontConfig = settings.fontConfig,
                    platformTheme = settings.platformTheme,
                    nintendoSwitchDarkMode = settings.nintendoSwitchDarkMode
                ) {
                    NexusPadAppShell(
                        activity = this@MainActivity,
                        viewModel = viewModel,
                        visualsInitialSection = visualsInitialTab,
                        openVisualsTrigger = openVisualsTrigger
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntentExtras(intent)
    }

    private fun consumeIntentExtras(intent: Intent?) {
        when (intent?.getStringExtra(OverlayControllerService.EXTRA_VISUALS_TAB)) {
            "crosshair" -> {
                visualsInitialTab = 1
                openVisualsTrigger++
                intent.removeExtra(OverlayControllerService.EXTRA_VISUALS_TAB)
            }
            "filter" -> {
                visualsInitialTab = 0
                openVisualsTrigger++
                intent.removeExtra(OverlayControllerService.EXTRA_VISUALS_TAB)
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
private fun NexusPadAppShell(
    activity: ComponentActivity,
    viewModel: NexusViewModel,
    visualsInitialSection: Int,
    openVisualsTrigger: Int
) {
    val strings = LocalNexusStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(openVisualsTrigger) {
        if (openVisualsTrigger > 0) {
            navController.navigate(NexusRoutes.FILTERS) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasLoadedSettings by viewModel.hasLoadedSettings.collectAsStateWithLifecycle()
    val liveFilterState by viewModel.liveFilterState.collectAsStateWithLifecycle()
    val isGpuShaderActive by viewModel.isGpuShaderActive.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val pressedIds by viewModel.pressedControlIds.collectAsStateWithLifecycle()
    val toggledIds by viewModel.toggledControlIds.collectAsStateWithLifecycle()
    val leftStick by viewModel.leftStick.collectAsStateWithLifecycle()
    val rightStick by viewModel.rightStick.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val cloudUser by viewModel.cloudUser.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
    val shareCodeState by viewModel.shareCodeState.collectAsStateWithLifecycle()
    val connectedControllers by viewModel.connectedControllers.collectAsStateWithLifecycle()
    val localButtonThemes by viewModel.localButtonThemes.collectAsStateWithLifecycle()
    val activeButtonThemeId by viewModel.activeButtonThemeId.collectAsStateWithLifecycle()
    val autoSavedThemeDraft by viewModel.autoSavedThemeDraft.collectAsStateWithLifecycle()
    val workshopThemes by viewModel.workshopThemes.collectAsStateWithLifecycle()
    val likedThemeIds by viewModel.likedThemeIds.collectAsStateWithLifecycle()
    val favoriteThemeIds by viewModel.favoriteThemeIds.collectAsStateWithLifecycle()
    val communityPosts by viewModel.communityPosts.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val likedClipIds by viewModel.likedClipIds.collectAsStateWithLifecycle()
    val myPublicProfile by viewModel.myPublicProfile.collectAsStateWithLifecycle()
    val myProfileClips by viewModel.myProfileClips.collectAsStateWithLifecycle()
    val myProfileThemes by viewModel.myProfileThemes.collectAsStateWithLifecycle()
    val myProfilePosts by viewModel.myProfilePosts.collectAsStateWithLifecycle()
    val viewedPublicProfile by viewModel.viewedPublicProfile.collectAsStateWithLifecycle()
    val viewedProfileClips by viewModel.viewedProfileClips.collectAsStateWithLifecycle()
    val viewedProfileThemes by viewModel.viewedProfileThemes.collectAsStateWithLifecycle()
    val viewedProfilePosts by viewModel.viewedProfilePosts.collectAsStateWithLifecycle()
    val isCommunityLoading by viewModel.isCommunityLoading.collectAsStateWithLifecycle()
    val isPublishingTheme by viewModel.isPublishingTheme.collectAsStateWithLifecycle()
    val cosmeticItems by viewModel.cosmeticItems.collectAsStateWithLifecycle()
    val myOwnedCosmeticIds by viewModel.myOwnedCosmeticIds.collectAsStateWithLifecycle()
    val adminSearchResults by viewModel.adminSearchResults.collectAsStateWithLifecycle()
    val adminSelectedUser by viewModel.adminSelectedUser.collectAsStateWithLifecycle()
    val adminSelectedUserOwnedIds by viewModel.adminSelectedUserOwnedIds.collectAsStateWithLifecycle()
    val isAdminBusy by viewModel.isAdminBusy.collectAsStateWithLifecycle()
    val isPrimaryAdmin = cloudUser?.isPrimaryAdmin() == true
    val hasUpdateBadge = updateState is UpdateUiState.UpdateAvailable || updateState is UpdateUiState.ReadyToInstall

    // Dynamic tabs: Store tab is ONLY active for Steam Deck, Epic Games, and Xbox themes (GeForce NOW storefronts)
    val primaryTabs = remember(strings, settings.platformTheme.hasLiveGfnStore, hasUpdateBadge) {
        buildList {
            add(PlatformNavItem(NexusRoutes.COCKPIT, strings.navCockpit, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_cockpit"))
            if (settings.platformTheme.hasLiveGfnStore) {
                add(PlatformNavItem(NexusRoutes.STORE, "Store", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_store"))
            }
            add(PlatformNavItem(NexusRoutes.PROFILES, strings.navProfiles, Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy, "nav_profiles"))
            add(PlatformNavItem(NexusRoutes.FILTERS, strings.navVisuals, Icons.Filled.Tune, Icons.Outlined.Tune, "nav_filters"))
            add(PlatformNavItem(NexusRoutes.GALLERY, strings.navGallery, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_gallery"))
            add(PlatformNavItem(NexusRoutes.SANDBOX, strings.navSandbox, Icons.Filled.TouchApp, Icons.Outlined.TouchApp, "nav_sandbox"))
            add(PlatformNavItem(NexusRoutes.SETUP, strings.navSetup, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_setup", showBadge = hasUpdateBadge))
        }
    }

    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var didTriggerAutoWhatsNew by remember { mutableStateOf(false) }
    var pendingExportProfile by remember { mutableStateOf<ControllerProfile?>(null) }
    var showSteamMenu by remember { mutableStateOf(false) }
    var showSteamQuickAccess by remember { mutableStateOf(false) }
    var steamSelectTrigger by remember { mutableIntStateOf(0) }
    var showEpicLeftDrawer by remember { mutableStateOf(false) }
    var showEpicSocialDrawer by remember { mutableStateOf(false) }
    var showXboxGuide by remember { mutableStateOf(false) }
    var xboxSelectTrigger by remember { mutableIntStateOf(0) }
    var showPs5ControlCenter by remember { mutableStateOf(false) }
    var ps5SelectTrigger by remember { mutableIntStateOf(0) }
    var showSwitchQuickMenu by remember { mutableStateOf(false) }
    var switchSelectTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.attemptSilentCloudSignIn(activity)
    }

    LaunchedEffect(hasLoadedSettings, settings.hasCompletedOnboarding, settings.lastSeenWhatsNewVersionCode) {
        if (hasLoadedSettings && settings.hasCompletedOnboarding && !didTriggerAutoWhatsNew && settings.lastSeenWhatsNewVersionCode < BuildConfig.VERSION_CODE) {
            didTriggerAutoWhatsNew = true
            showWhatsNewDialog = true
        }
    }

    // First-launch setup wizard: Sign In/Sign Up, Username & Profile Picture, and Platform UI Theme
    if (hasLoadedSettings && !settings.hasCompletedOnboarding) {
        OnboardingSetupScreen(
            cloudUser = cloudUser,
            myProfile = myPublicProfile,
            selectedPlatformTheme = settings.platformTheme,
            nintendoSwitchDarkMode = settings.nintendoSwitchDarkMode,
            onSignInWithGoogle = { viewModel.signInForCloudSave(activity) },
            onSelectPlatformTheme = viewModel::setPlatformTheme,
            onNintendoSwitchDarkModeChange = viewModel::setNintendoSwitchDarkMode,
            onUploadAvatarUri = viewModel::uploadMyProfileAvatar,
            onCompleteOnboarding = { username, bio, editedAvatarUri ->
                viewModel.completeOnboarding(username, bio, editedAvatarUri)
                viewModel.markWhatsNewSeen(BuildConfig.VERSION_CODE)
            }
        )
        return
    }

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
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    InputBridge.setAppForeground(true)
                    viewModel.refreshSystemStatus()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    InputBridge.setAppForeground(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            InputBridge.setAppForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
    val isInFullscreenSubscreen = currentRoute?.startsWith("editor/") == true ||
        currentRoute?.startsWith("workshop/") == true ||
        currentRoute?.startsWith("theme_creator") == true ||
        currentRoute?.startsWith("user_profile/") == true ||
        currentRoute == NexusRoutes.ADMIN_PANEL

    val navigatePrimaryTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(settings.platformTheme.hasLiveGfnStore, currentRoute) {
        if (!settings.platformTheme.hasLiveGfnStore && currentRoute == NexusRoutes.STORE) {
            navigatePrimaryTab(NexusRoutes.COCKPIT)
        }
    }

    val topBarDisplayName = myPublicProfile?.username?.takeIf { it.isNotBlank() }
        ?: settings.localDisplayName.takeIf { it.isNotBlank() }
        ?: cloudUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Player"

    val xboxGamerscore = remember(
        profiles.size,
        settings.pinnedGames.size,
        localButtonThemes.size,
        myProfilePosts.size,
        myProfileClips.size
    ) {
        1250 +
            profiles.size * 350 +
            settings.pinnedGames.size * 250 +
            localButtonThemes.size * 400 +
            myProfilePosts.size * 150 +
            myProfileClips.size * 200
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isSteamBigPicture = settings.platformTheme == AppPlatformTheme.STEAM_DECK
        val isEpicGames = settings.platformTheme == AppPlatformTheme.EPIC_GAMES
        val isXbox = settings.platformTheme == AppPlatformTheme.XBOX
        val isPlayStation = settings.platformTheme == AppPlatformTheme.PLAYSTATION
        val isSwitch = settings.platformTheme == AppPlatformTheme.NINTENDO_SWITCH
        val useEpicSidebar = maxWidth >= 680.dp && isEpicGames
        val useRail = maxWidth >= 680.dp && !isSteamBigPicture && !isEpicGames && !isXbox && !isPlayStation && !isSwitch

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = if (isInFullscreenSubscreen) WindowInsets(0) else WindowInsets.safeDrawing,
            topBar = {
                if (!isInFullscreenSubscreen && !useEpicSidebar) {
                    PlatformTopBar(
                        platformTheme = settings.platformTheme,
                        isPrimaryAdmin = isPrimaryAdmin,
                        avatarBase64 = myPublicProfile?.avatarBase64,
                        displayName = topBarDisplayName,
                        onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                        onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                        onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                        onOpenStore = if (settings.platformTheme.hasLiveGfnStore) {
                            { navigatePrimaryTab(NexusRoutes.STORE) }
                        } else {
                            null
                        },
                        onOpenEpicMenu = {
                            showEpicSocialDrawer = false
                            showEpicLeftDrawer = !showEpicLeftDrawer
                        },
                        onOpenEpicSocial = {
                            showEpicLeftDrawer = false
                            showEpicSocialDrawer = !showEpicSocialDrawer
                        },
                        currentRoute = currentRoute,
                        hasUpdateBadge = hasUpdateBadge,
                        isOverlayRunning = isOverlayRunning,
                        xboxGamerscore = xboxGamerscore,
                        onOpenXboxGuide = { showXboxGuide = !showXboxGuide },
                        onOpenXboxLibrary = { navigatePrimaryTab(NexusRoutes.PROFILES) },
                        onLaunchXboxCloudPlay = { XenosWebPlayerActivity.launchXboxCloud(context) },
                        onOpenXboxSettings = { navigatePrimaryTab(NexusRoutes.SETUP) },
                        onSelectPs5Games = { navigatePrimaryTab(NexusRoutes.COCKPIT) },
                        onSelectPs5Media = { navigatePrimaryTab(NexusRoutes.GALLERY) },
                        onOpenPs5Settings = { navigatePrimaryTab(NexusRoutes.SETUP) },
                        nintendoSwitchDarkMode = settings.nintendoSwitchDarkMode,
                        communityPosts = communityPosts,
                        onOpenPublicProfile = { uid -> navController.navigate(NexusRoutes.userProfile(uid)) },
                        onToggleNintendoSwitchDarkMode = {
                            viewModel.setNintendoSwitchDarkMode(!settings.nintendoSwitchDarkMode)
                        }
                    )
                }
            },
            bottomBar = {
                if (!isInFullscreenSubscreen && !useRail && !useEpicSidebar) {
                    PlatformBottomNavigationBar(
                        platformTheme = settings.platformTheme,
                        tabs = primaryTabs,
                        currentRoute = currentRoute,
                        onSelectRoute = navigatePrimaryTab,
                        onOpenSteamMenu = {
                            showSteamQuickAccess = false
                            showSteamMenu = !showSteamMenu
                        },
                        onOpenSteamQuickAccess = {
                            showSteamMenu = false
                            showSteamQuickAccess = !showSteamQuickAccess
                        },
                        onSteamPrimaryAction = {
                            if (currentRoute == NexusRoutes.COCKPIT) {
                                steamSelectTrigger++
                            } else {
                                navigatePrimaryTab(NexusRoutes.COCKPIT)
                            }
                        },
                        onSteamBackAction = {
                            when {
                                showSteamMenu -> showSteamMenu = false
                                showSteamQuickAccess -> showSteamQuickAccess = false
                                currentRoute != NexusRoutes.COCKPIT -> navigatePrimaryTab(NexusRoutes.COCKPIT)
                                else -> showSteamMenu = true
                            }
                        },
                        isOverlayRunning = isOverlayRunning,
                        onOpenXboxGuide = { showXboxGuide = !showXboxGuide },
                        onXboxPrimaryAction = {
                            if (currentRoute == NexusRoutes.COCKPIT) {
                                xboxSelectTrigger++
                            } else {
                                navigatePrimaryTab(NexusRoutes.COCKPIT)
                            }
                        },
                        onXboxToggleHud = toggleOverlayAction,
                        onXboxBackAction = {
                            when {
                                showXboxGuide -> showXboxGuide = false
                                currentRoute != NexusRoutes.COCKPIT -> navigatePrimaryTab(NexusRoutes.COCKPIT)
                                else -> showXboxGuide = true
                            }
                        },
                        onOpenPs5ControlCenter = { showPs5ControlCenter = !showPs5ControlCenter },
                        onPs5PrimaryAction = {
                            if (currentRoute == NexusRoutes.COCKPIT) {
                                ps5SelectTrigger++
                            } else {
                                navigatePrimaryTab(NexusRoutes.COCKPIT)
                            }
                        },
                        onPs5ToggleHud = toggleOverlayAction,
                        onPs5BackAction = {
                            when {
                                showPs5ControlCenter -> showPs5ControlCenter = false
                                currentRoute != NexusRoutes.COCKPIT -> navigatePrimaryTab(NexusRoutes.COCKPIT)
                                else -> showPs5ControlCenter = true
                            }
                        },
                        nintendoSwitchDarkMode = settings.nintendoSwitchDarkMode,
                        onOpenSwitchQuickMenu = { showSwitchQuickMenu = !showSwitchQuickMenu },
                        onSwitchPrimaryAction = {
                            if (currentRoute == NexusRoutes.COCKPIT) {
                                switchSelectTrigger++
                            } else {
                                navigatePrimaryTab(NexusRoutes.COCKPIT)
                            }
                        },
                        onSwitchToggleHud = toggleOverlayAction,
                        onSwitchBackAction = {
                            when {
                                showSwitchQuickMenu -> showSwitchQuickMenu = false
                                currentRoute != NexusRoutes.COCKPIT -> navigatePrimaryTab(NexusRoutes.COCKPIT)
                                else -> showSwitchQuickMenu = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInFullscreenSubscreen) Modifier else Modifier.padding(innerPadding)
                    )
            ) {
                if (!isInFullscreenSubscreen && useEpicSidebar) {
                    EpicLauncherSidebarContent(
                        currentRoute = currentRoute,
                        displayName = topBarDisplayName,
                        avatarBase64 = myPublicProfile?.avatarBase64,
                        hasUpdateBadge = hasUpdateBadge,
                        isPrimaryAdmin = isPrimaryAdmin,
                        onNavigateRoute = navigatePrimaryTab,
                        onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                        onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                        onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                        onExitEpicTheme = { viewModel.setPlatformTheme(AppPlatformTheme.XENOS_DEFAULT) },
                        modifier = Modifier
                            .width(216.dp)
                            .border(1.dp, EpicBorderColor)
                    )
                } else if (!isInFullscreenSubscreen && useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        primaryTabs.forEach { tab ->
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
                                    if (tab.route == NexusRoutes.SETUP && hasUpdateBadge) {
                                        BadgedBox(
                                            badge = { Badge(containerColor = MaterialTheme.colorScheme.primary) }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
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
                                webPlayerConfig = settings.webPlayerConfig,
                                pinnedGames = settings.pinnedGames,
                                filterState = liveFilterState,
                                updateState = updateState,
                                onDownloadUpdate = viewModel::downloadAvailableUpdate,
                                onCancelUpdateDownload = viewModel::cancelUpdateDownload,
                                onInstallUpdate = viewModel::installDownloadedUpdate,
                                onSelectWebRegion = { region ->
                                    viewModel.updateWebPlayerConfig(
                                        settings.webPlayerConfig.copy(regionPreset = region)
                                    )
                                },
                                onLaunchWebPlayer = {
                                    XenosWebPlayerActivity.launch(context)
                                },
                                onLaunchPinnedGame = { pinned ->
                                    viewModel.prepareLaunchPinnedGame(pinned) { launchUrl ->
                                        XenosWebPlayerActivity.launch(context, launchUrl)
                                    }
                                },
                                onSavePinnedGame = viewModel::savePinnedGame,
                                onDeletePinnedGame = viewModel::deletePinnedGame,
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
                                },
                                onOpenStore = if (settings.platformTheme.hasLiveGfnStore) {
                                    { navigatePrimaryTab(NexusRoutes.STORE) }
                                } else {
                                    null
                                },
                                workshopThemes = workshopThemes,
                                communityPosts = communityPosts,
                                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                                onOpenFilters = { navigatePrimaryTab(NexusRoutes.FILTERS) },
                                onOpenGallery = { navigatePrimaryTab(NexusRoutes.GALLERY) },
                                onOpenSettings = { navigatePrimaryTab(NexusRoutes.SETUP) },
                                onOpenSteamQuickAccess = {
                                    showSteamMenu = false
                                    showSteamQuickAccess = true
                                },
                                steamSelectTrigger = steamSelectTrigger,
                                cloudUser = cloudUser,
                                myProfile = myPublicProfile,
                                displayName = topBarDisplayName,
                                onSignIn = { viewModel.signInForCloudSave(activity) },
                                onOpenMyProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                                onOpenPublicProfile = { uid -> navController.navigate(NexusRoutes.userProfile(uid)) },
                                onOpenProfiles = { navigatePrimaryTab(NexusRoutes.PROFILES) },
                                xboxSelectTrigger = xboxSelectTrigger,
                                onLaunchXboxCloud = { XenosWebPlayerActivity.launchXboxCloud(context) },
                                onOpenPs5ControlCenter = { showPs5ControlCenter = true },
                                ps5SelectTrigger = ps5SelectTrigger,
                                nintendoSwitchDarkMode = settings.nintendoSwitchDarkMode,
                                onOpenSwitchQuickMenu = { showSwitchQuickMenu = true },
                                switchSelectTrigger = switchSelectTrigger
                            )
                        }

                        composable(NexusRoutes.STORE) {
                            PlatformStoreScreen(
                                platformTheme = settings.platformTheme,
                                onLaunchGeForceNowWebPlay = {
                                    XenosWebPlayerActivity.launch(context)
                                },
                                onLaunchXboxCloud = {
                                    XenosWebPlayerActivity.launchXboxCloud(context)
                                }
                            )
                        }

                        composable(NexusRoutes.PROFILES) {
                            ProfilesScreen(
                                profiles = profiles,
                                activeProfileId = activeProfile?.id,
                                cloudUser = cloudUser,
                                cloudSyncState = cloudSyncState,
                                shareCodeState = shareCodeState,
                                onCloudSignIn = { viewModel.signInForCloudSave(activity) },
                                onCloudSyncNow = viewModel::backupAllLayoutsToCloud,
                                onCreateShareCode = viewModel::createShareCodeForProfile,
                                onImportByShareCode = viewModel::importProfileByShareCode,
                                onDismissShareCode = viewModel::dismissShareCodeState,
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
                                filterState = liveFilterState,
                                isGpuShaderActive = isGpuShaderActive,
                                crosshair = settings.crosshair,
                                isOverlayRunning = isOverlayRunning,
                                initialSection = visualsInitialSection,
                                onSelectSlot = viewModel::selectFilterSlot,
                                onPreviewStyle = viewModel::previewFilterStyle,
                                onUpdateStyle = viewModel::updateFilterStyle,
                                onResetSlot = viewModel::resetActiveFilterSlot,
                                onToggleGpuShader = {
                                    if (isGpuShaderActive) {
                                        ScreenCaptureService.stopGpuFilter(context)
                                    } else {
                                        if (!SystemIntegrations.canDrawOverlays(context)) {
                                            showOverlayPermissionDialog = true
                                        } else {
                                            if (!isOverlayRunning) {
                                                OverlayControllerService.start(context)
                                            }
                                            if (liveFilterState.activeSlot == 0) {
                                                viewModel.selectFilterSlot(1)
                                            }
                                            ScreenCaptureService.toggleGpuFilter(context)
                                        }
                                    }
                                },
                                onPickStickerImage = viewModel::applyCustomStickerImage,
                                onUpdateCrosshair = viewModel::updateCrosshair,
                                onSelectCrosshairPreset = viewModel::selectCrosshairPreset,
                                onResetCrosshair = viewModel::resetCrosshair,
                                onPickCrosshairImage = viewModel::applyCrosshairCustomImage,
                                onToggleOverlay = toggleOverlayAction
                            )
                        }

                        composable(NexusRoutes.GALLERY) {
                            GalleryScreen(
                                replayDurationSec = settings.instantReplayDurationSec,
                                recordingResolution = settings.recordingResolution,
                                recordingFps = settings.recordingFps,
                                onReplayDurationChange = viewModel::setInstantReplayDurationSec,
                                onRecordingResolutionChange = viewModel::setRecordingResolution,
                                onRecordingFpsChange = viewModel::setRecordingFps,
                                onPostClipToProfile = viewModel::postGalleryClipToProfile,
                                onShowMessage = viewModel::showMessage
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
                                onClearTelemetry = viewModel::clearTelemetry,
                                onPickCustomBg = viewModel::importCustomTestBackground,
                                onResetCustomBg = viewModel::clearCustomTestBackground
                            )
                        }

                        composable(NexusRoutes.SETUP) {
                            SetupScreen(
                                settings = settings,
                                readiness = readiness,
                                updateState = updateState,
                                cloudUser = cloudUser,
                                cloudSyncState = cloudSyncState,
                                localProfileCount = profiles.size,
                                hasHardwareGyro = viewModel.hasGyroSensor,
                                connectedControllers = connectedControllers,
                                onCloudSignIn = { viewModel.signInForCloudSave(activity) },
                                onCloudBackupNow = viewModel::backupAllLayoutsToCloud,
                                onCloudRestore = viewModel::restoreLayoutsFromCloud,
                                onCloudSignOut = viewModel::signOutCloudSave,
                                onWebPlayerConfigChange = viewModel::updateWebPlayerConfig,
                                onPhysicalControllerChange = viewModel::updatePhysicalController,
                                onLaunchWebPlayer = {
                                    XenosWebPlayerActivity.launch(context)
                                },
                                onGyroAimChange = viewModel::updateGyroAim,
                                onPerfHudChange = viewModel::updatePerfHud,
                                onVibrationModeChange = viewModel::setVibrationMode,
                                onOpacityChange = viewModel::setGlobalOpacity,
                                onGridSnapChange = viewModel::setGridSnapEnabled,
                                onGridDivisionsChange = viewModel::setGridDivisions,
                                onNotationChange = viewModel::setControllerNotation,
                                onDarkThemeChange = viewModel::setDarkTheme,
                                onPlatformThemeChange = viewModel::setPlatformTheme,
                                onNintendoSwitchDarkModeChange = viewModel::setNintendoSwitchDarkMode,
                                onColorSchemeChange = viewModel::updateColorScheme,
                                onFontConfigChange = viewModel::updateFontConfig,
                                onPickCustomFont = viewModel::importCustomFont,
                                onFloatingBubbleChange = viewModel::updateFloatingBubble,
                                onPickCustomBubbleIcon = viewModel::importCustomBubbleIcon,
                                onOpenWhatsNew = { showWhatsNewDialog = true },
                                onShowTelemetryChange = viewModel::setShowTelemetryOverlay,
                                onAutoLaunchChange = viewModel::setAutoLaunchGeForceNow,
                                onFloatingKeyboardDefaultChange = viewModel::setFloatingKeyboardDefault,
                                onOpenFloatingKeyboard = {
                                    if (!SystemIntegrations.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        OverlayControllerService.startFloatingKeyboard(context)
                                    }
                                },
                                onAppLanguageChange = viewModel::setAppLanguage,
                                onTargetModeChange = viewModel::setTargetPlatformMode,
                                onRecordingResolutionChange = viewModel::setRecordingResolution,
                                onRecordingFpsChange = viewModel::setRecordingFps,
                                onGithubRepoSlugChange = viewModel::setGithubRepoSlug,
                                onAutoCheckUpdatesChange = viewModel::setAutoCheckUpdates,
                                onCheckForUpdates = { repo -> viewModel.checkForUpdates(explicitRepoInput = repo, silent = false) },
                                onDownloadUpdate = viewModel::downloadAvailableUpdate,
                                onCancelUpdateDownload = viewModel::cancelUpdateDownload,
                                onInstallUpdate = viewModel::installDownloadedUpdate,
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
                                onCommitControlPercent = { id, xPct, yPct ->
                                    viewModel.moveControlToPercent(
                                        controlId = id,
                                        rawXPercent = xPct,
                                        rawYPercent = yPct,
                                        snapToGrid = settings.gridSnapEnabled,
                                        gridDivisions = settings.gridDivisions
                                    )
                                },
                                onGridSnapChange = viewModel::setGridSnapEnabled,
                                onGridDivisionsChange = viewModel::setGridDivisions,
                                onUpdateControl = viewModel::updateControl,
                                onResizeControlScales = viewModel::resizeControlScales,
                                onPickCustomImage = viewModel::applyCustomButtonImage,
                                onPickCustomBackground = viewModel::applyCustomButtonBackground,
                                onPickCustomStroke = viewModel::applyCustomButtonStroke,
                                onAddControl = viewModel::addControl,
                                onDuplicateControl = viewModel::duplicateSelectedControl,
                                onMirrorControl = viewModel::mirrorDuplicateSelectedControl,
                                onNudgeControl = viewModel::nudgeSelectedControl,
                                onDeleteControl = viewModel::deleteSelectedControl,
                                onTogglePlayTest = viewModel::togglePlayTestMode,
                                onUndo = viewModel::undoEditorChange,
                                onSave = { viewModel.saveEditorProfile() },
                                onUpdateProfileMeta = viewModel::updateProfileMeta,
                                onPickCustomBg = viewModel::importCustomTestBackground,
                                onResetCustomBg = viewModel::clearCustomTestBackground,
                                localButtonThemes = localButtonThemes,
                                activeButtonThemeId = activeButtonThemeId,
                                onSelectButtonTheme = viewModel::setActiveButtonTheme,
                                onApplyThemeToAllButtons = viewModel::applyActiveThemeToEditorLayout,
                                onOpenThemeCreator = {
                                    navController.navigate(NexusRoutes.themeCreator())
                                },
                                onOpenThemeWorkshop = {
                                    navController.navigate(NexusRoutes.workshop(0))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = NexusRoutes.WORKSHOP,
                            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                        ) { backStackEntry ->
                            val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                            CommunityHubScreen(
                                initialTab = initialTab,
                                cloudUser = cloudUser,
                                myProfile = myPublicProfile,
                                localThemes = localButtonThemes,
                                activeThemeId = activeButtonThemeId,
                                workshopThemes = workshopThemes,
                                likedThemeIds = likedThemeIds,
                                favoriteThemeIds = favoriteThemeIds,
                                communityPosts = communityPosts,
                                likedPostIds = likedPostIds,
                                myProfileClips = myProfileClips,
                                myProfileThemes = myProfileThemes,
                                myProfilePosts = myProfilePosts,
                                likedClipIds = likedClipIds,
                                cosmeticItems = cosmeticItems,
                                myOwnedCosmeticIds = myOwnedCosmeticIds,
                                isLoading = isCommunityLoading,
                                notation = settings.controllerNotation,
                                onSignIn = { viewModel.signInForCloudSave(activity) },
                                onRefresh = viewModel::refreshWorkshopAndCommunity,
                                onClaimFreeCosmetic = viewModel::claimFreeShopItem,
                                onEquipCosmetic = viewModel::equipCosmeticItem,
                                onOpenAdminPanel = {
                                    if (isPrimaryAdmin) {
                                        navController.navigate(NexusRoutes.ADMIN_PANEL)
                                    }
                                },
                                onCreateTheme = { navController.navigate(NexusRoutes.themeCreator()) },
                                onEditTheme = { themeId -> navController.navigate(NexusRoutes.themeCreator(themeId)) },
                                onActivateTheme = viewModel::setActiveButtonTheme,
                                onDeleteLocalTheme = viewModel::deleteLocalButtonTheme,
                                onDownloadWorkshopTheme = viewModel::downloadWorkshopTheme,
                                onDeletePublishedTheme = viewModel::deletePublishedButtonTheme,
                                onToggleLikeTheme = viewModel::toggleLikeTheme,
                                onToggleFavoriteTheme = viewModel::toggleFavoriteTheme,
                                onLoadThemeComments = viewModel::loadThemeComments,
                                onAddThemeComment = viewModel::addThemeComment,
                                onDeleteThemeComment = viewModel::deleteThemeComment,
                                onCreatePost = viewModel::createCommunityPost,
                                onDeletePost = viewModel::deleteCommunityPost,
                                onToggleLikePost = viewModel::toggleLikePost,
                                onLoadPostComments = viewModel::loadPostComments,
                                onAddPostComment = viewModel::addPostComment,
                                onDeletePostComment = viewModel::deletePostComment,
                                onSaveMyProfile = viewModel::saveMyPublicProfile,
                                onUploadMyAvatar = viewModel::uploadMyProfileAvatar,
                                onRemoveMyAvatar = viewModel::removeMyProfileAvatar,
                                onDeleteClip = viewModel::deleteProfileClip,
                                onToggleLikeClip = viewModel::toggleLikeClip,
                                onOpenGalleryForClip = {
                                    navController.navigate(NexusRoutes.GALLERY) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenPublicProfile = { uid ->
                                    navController.navigate(NexusRoutes.userProfile(uid))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = NexusRoutes.THEME_CREATOR,
                            arguments = listOf(
                                navArgument("themeId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val editId = backStackEntry.arguments?.getString("themeId")
                            val initialTheme = remember(editId, localButtonThemes) {
                                editId?.let { viewModel.getLocalButtonTheme(it) }
                            }
                            ButtonThemeCreatorScreen(
                                initialTheme = initialTheme,
                                autoSavedDraft = if (editId == null) autoSavedThemeDraft else null,
                                isSignedIn = cloudUser != null,
                                isPublishing = isPublishingTheme,
                                notation = settings.controllerNotation,
                                onAutoSaveDraft = viewModel::autoSaveButtonThemeDraft,
                                onAutoSaveDraftSync = viewModel::autoSaveButtonThemeDraftSync,
                                onClearAutoSavedDraft = viewModel::clearAutoSavedThemeDraft,
                                onSaveDraft = { draft ->
                                    viewModel.saveButtonThemeDraft(draft, activate = false)
                                },
                                onTestTheme = { draft ->
                                    viewModel.saveButtonThemeDraft(draft, activate = true)
                                    val targetProfileId = activeProfile?.id ?: profiles.firstOrNull()?.id
                                    if (targetProfileId != null) {
                                        viewModel.openEditor(targetProfileId)
                                        navController.navigate(NexusRoutes.editor(targetProfileId))
                                    }
                                },
                                onPublishTheme = { theme ->
                                    viewModel.publishButtonTheme(theme) {
                                        navController.popBackStack()
                                    }
                                },
                                onRequestSignIn = { viewModel.signInForCloudSave(activity) },
                                onShowMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = NexusRoutes.USER_PROFILE,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("userId").orEmpty()
                            UserProfileScreen(
                                userId = uid,
                                currentUserUid = cloudUser?.uid,
                                profile = viewedPublicProfile,
                                themes = viewedProfileThemes,
                                clips = viewedProfileClips,
                                posts = viewedProfilePosts,
                                likedThemeIds = likedThemeIds,
                                favoriteThemeIds = favoriteThemeIds,
                                likedClipIds = likedClipIds,
                                likedPostIds = likedPostIds,
                                cosmeticItems = cosmeticItems,
                                isLoading = isCommunityLoading,
                                notation = settings.controllerNotation,
                                onLoadProfile = viewModel::loadPublicProfileBundle,
                                onDownloadWorkshopTheme = viewModel::downloadWorkshopTheme,
                                onToggleLikeTheme = viewModel::toggleLikeTheme,
                                onToggleFavoriteTheme = viewModel::toggleFavoriteTheme,
                                onDeletePublishedTheme = viewModel::deletePublishedButtonTheme,
                                onLoadThemeComments = viewModel::loadThemeComments,
                                onAddThemeComment = viewModel::addThemeComment,
                                onDeleteThemeComment = viewModel::deleteThemeComment,
                                onToggleLikeClip = viewModel::toggleLikeClip,
                                onDeleteClip = viewModel::deleteProfileClip,
                                onToggleLikePost = viewModel::toggleLikePost,
                                onDeletePost = viewModel::deleteCommunityPost,
                                onLoadPostComments = viewModel::loadPostComments,
                                onAddPostComment = viewModel::addPostComment,
                                onDeletePostComment = viewModel::deletePostComment,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NexusRoutes.ADMIN_PANEL) {
                            AdminPanelScreen(
                                cloudUser = cloudUser,
                                cosmeticItems = cosmeticItems,
                                searchedUsers = adminSearchResults,
                                selectedUser = adminSelectedUser,
                                selectedUserOwnedIds = adminSelectedUserOwnedIds,
                                isBusy = isAdminBusy,
                                onRefresh = viewModel::refreshCosmeticsAndInventory,
                                onCreateCosmeticItem = viewModel::adminCreateCosmeticItem,
                                onToggleAvailability = viewModel::adminToggleCosmeticAvailability,
                                onDeleteItem = viewModel::adminDeleteCosmeticItem,
                                onSearchUsers = viewModel::adminSearchUsers,
                                onSelectUser = viewModel::adminSelectUser,
                                onGrantItem = viewModel::adminGrantItemToSelectedUser,
                                onRevokeItem = viewModel::adminRevokeItemFromSelectedUser,
                                onUpdateUserRoles = viewModel::adminUpdateUserRoles,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        if (isSteamBigPicture && showSteamMenu) {
            SteamBigPictureLeftMenuOverlay(
                currentRoute = currentRoute,
                hasUpdateBadge = hasUpdateBadge,
                isPrimaryAdmin = isPrimaryAdmin,
                onNavigateRoute = navigatePrimaryTab,
                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                onExitBigPicture = { viewModel.setPlatformTheme(AppPlatformTheme.XENOS_DEFAULT) },
                onDismiss = { showSteamMenu = false }
            )
        }

        if (isSteamBigPicture && showSteamQuickAccess) {
            SteamQuickAccessMenuOverlay(
                activeProfile = activeProfile,
                allProfiles = profiles,
                isOverlayRunning = isOverlayRunning,
                filterState = liveFilterState,
                selectedRegion = settings.webPlayerConfig.regionPreset,
                vibrationMode = settings.vibrationMode,
                onToggleOverlay = toggleOverlayAction,
                onLaunchWebPlayer = { XenosWebPlayerActivity.launch(context) },
                onSelectProfile = viewModel::setActiveProfile,
                onSelectRegion = { region ->
                    viewModel.updateWebPlayerConfig(settings.webPlayerConfig.copy(regionPreset = region))
                },
                onVibrationModeChange = viewModel::setVibrationMode,
                onOpenFilters = { navigatePrimaryTab(NexusRoutes.FILTERS) },
                onOpenSettings = { navigatePrimaryTab(NexusRoutes.SETUP) },
                onSwitchTheme = viewModel::setPlatformTheme,
                onDismiss = { showSteamQuickAccess = false }
            )
        }

        if (isEpicGames && showEpicLeftDrawer) {
            EpicLeftDrawerOverlay(
                currentRoute = currentRoute,
                displayName = topBarDisplayName,
                avatarBase64 = myPublicProfile?.avatarBase64,
                hasUpdateBadge = hasUpdateBadge,
                isPrimaryAdmin = isPrimaryAdmin,
                onNavigateRoute = navigatePrimaryTab,
                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                onExitEpicTheme = { viewModel.setPlatformTheme(AppPlatformTheme.XENOS_DEFAULT) },
                onDismiss = { showEpicLeftDrawer = false }
            )
        }

        if (isEpicGames && showEpicSocialDrawer) {
            EpicSocialPanelDrawerOverlay(
                cloudUser = cloudUser,
                myProfile = myPublicProfile,
                displayName = topBarDisplayName,
                activeProfileName = activeProfile?.name,
                isOverlayRunning = isOverlayRunning,
                workshopThemes = workshopThemes,
                communityPosts = communityPosts,
                onSignIn = { viewModel.signInForCloudSave(activity) },
                onOpenMyProfile = {
                    showEpicSocialDrawer = false
                    navController.navigate(NexusRoutes.workshop(2))
                },
                onOpenWorkshop = {
                    showEpicSocialDrawer = false
                    navController.navigate(NexusRoutes.workshop(0))
                },
                onOpenPublicProfile = { uid ->
                    showEpicSocialDrawer = false
                    navController.navigate(NexusRoutes.userProfile(uid))
                },
                onDismiss = { showEpicSocialDrawer = false }
            )
        }

        if (isXbox && showXboxGuide) {
            XboxGuideOverlay(
                currentRoute = currentRoute,
                cloudUser = cloudUser,
                myProfile = myPublicProfile,
                displayName = topBarDisplayName,
                activeProfile = activeProfile,
                allProfiles = profiles,
                isOverlayRunning = isOverlayRunning,
                filterState = liveFilterState,
                selectedRegion = settings.webPlayerConfig.regionPreset,
                vibrationMode = settings.vibrationMode,
                hasUpdateBadge = hasUpdateBadge,
                isPrimaryAdmin = isPrimaryAdmin,
                workshopThemes = workshopThemes,
                communityPosts = communityPosts,
                onNavigateRoute = navigatePrimaryTab,
                onLaunchWebPlayer = { XenosWebPlayerActivity.launch(context) },
                onLaunchXboxCloud = { XenosWebPlayerActivity.launchXboxCloud(context) },
                onToggleOverlay = toggleOverlayAction,
                onSelectProfile = viewModel::setActiveProfile,
                onOpenEditor = { id ->
                    viewModel.openEditor(id)
                    navController.navigate(NexusRoutes.editor(id))
                },
                onSelectRegion = { region ->
                    viewModel.updateWebPlayerConfig(settings.webPlayerConfig.copy(regionPreset = region))
                },
                onVibrationModeChange = viewModel::setVibrationMode,
                onSignIn = { viewModel.signInForCloudSave(activity) },
                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                onOpenPublicProfile = { uid -> navController.navigate(NexusRoutes.userProfile(uid)) },
                onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                onSwitchTheme = viewModel::setPlatformTheme,
                onDismiss = { showXboxGuide = false }
            )
        }

        if (isPlayStation && showPs5ControlCenter) {
            Ps5ControlCenterOverlay(
                currentRoute = currentRoute,
                displayName = topBarDisplayName,
                avatarBase64 = myPublicProfile?.avatarBase64,
                activeProfile = activeProfile,
                allProfiles = profiles,
                isOverlayRunning = isOverlayRunning,
                filterState = liveFilterState,
                selectedRegion = settings.webPlayerConfig.regionPreset,
                vibrationMode = settings.vibrationMode,
                hasUpdateBadge = hasUpdateBadge,
                isPrimaryAdmin = isPrimaryAdmin,
                onNavigateRoute = navigatePrimaryTab,
                onLaunchGeForceNow = { XenosWebPlayerActivity.launch(context) },
                onLaunchXboxCloud = { XenosWebPlayerActivity.launchXboxCloud(context) },
                onToggleOverlay = toggleOverlayAction,
                onSelectProfile = viewModel::setActiveProfile,
                onOpenEditor = { id ->
                    viewModel.openEditor(id)
                    navController.navigate(NexusRoutes.editor(id))
                },
                onSelectRegion = { region ->
                    viewModel.updateWebPlayerConfig(settings.webPlayerConfig.copy(regionPreset = region))
                },
                onVibrationModeChange = viewModel::setVibrationMode,
                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                onSwitchTheme = viewModel::setPlatformTheme,
                onDismiss = { showPs5ControlCenter = false }
            )
        }

        if (isSwitch && showSwitchQuickMenu) {
            NintendoSwitchQuickMenuOverlay(
                isDarkMode = settings.nintendoSwitchDarkMode,
                currentRoute = currentRoute,
                displayName = topBarDisplayName,
                avatarBase64 = myPublicProfile?.avatarBase64,
                activeProfile = activeProfile,
                allProfiles = profiles,
                isOverlayRunning = isOverlayRunning,
                filterState = liveFilterState,
                selectedRegion = settings.webPlayerConfig.regionPreset,
                vibrationMode = settings.vibrationMode,
                isPrimaryAdmin = isPrimaryAdmin,
                onToggleDarkMode = viewModel::setNintendoSwitchDarkMode,
                onNavigateRoute = navigatePrimaryTab,
                onLaunchGeForceNow = { XenosWebPlayerActivity.launch(context) },
                onLaunchXboxCloud = { XenosWebPlayerActivity.launchXboxCloud(context) },
                onToggleOverlay = toggleOverlayAction,
                onSelectProfile = viewModel::setActiveProfile,
                onOpenEditor = { id ->
                    viewModel.openEditor(id)
                    navController.navigate(NexusRoutes.editor(id))
                },
                onSelectRegion = { region ->
                    viewModel.updateWebPlayerConfig(settings.webPlayerConfig.copy(regionPreset = region))
                },
                onVibrationModeChange = viewModel::setVibrationMode,
                onOpenWorkshop = { navController.navigate(NexusRoutes.workshop(0)) },
                onOpenProfile = { navController.navigate(NexusRoutes.workshop(2)) },
                onOpenAdminPanel = { navController.navigate(NexusRoutes.ADMIN_PANEL) },
                onSwitchTheme = viewModel::setPlatformTheme,
                onDismiss = { showSwitchQuickMenu = false }
            )
        }
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text(strings.permDialogTitle) },
            text = { Text(strings.permDialogBody) },
            confirmButton = {
                Button(onClick = {
                    showOverlayPermissionDialog = false
                    SystemIntegrations.openOverlayPermissionSettings(context)
                }) {
                    Text(strings.commonOpenSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text(strings.commonNotNow)
                }
            }
        )
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = {
                showWhatsNewDialog = false
                viewModel.markWhatsNewSeen(BuildConfig.VERSION_CODE)
            }
        )
    }
}
