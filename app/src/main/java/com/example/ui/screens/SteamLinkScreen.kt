package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerProfile
import com.example.model.DiscoveredPcHost
import com.example.model.PcHostPingStatus
import com.example.model.PcStreamClient
import com.example.model.SavedPcHost
import com.example.pclink.PcLinkDiscoveryEngine
import com.example.ui.SystemReadiness
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TurboAmber
import kotlinx.coroutines.launch
import java.util.UUID

private val SteamLinkBlue = Color(0xFF1A9FFF)
private val SteamLinkCyan = Color(0xFF66C0F4)
private val SteamLinkGreen = Color(0xFF59BF40)
private val SteamLinkHeroTop = Color(0xFF132942)
private val SteamLinkHeroMid = Color(0xFF0E1B2B)
private val SteamLinkHeroBottom = Color(0xFF0A111A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteamLinkScreen(
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    savedHosts: List<SavedPcHost>,
    readiness: SystemReadiness,
    isOverlayRunning: Boolean,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onToggleOverlay: () -> Unit,
    onLaunchClientWithOverlay: (client: PcStreamClient, customUrl: String?) -> Unit,
    onLaunchSavedHost: (SavedPcHost) -> Unit,
    onSaveHost: (SavedPcHost) -> Unit,
    onDeleteHost: (String) -> Unit,
    onWakeHost: (SavedPcHost) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefreshStatus: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val scope = rememberCoroutineScope()
    var localIp by remember { mutableStateOf<String?>(null) }
    var isScanningLan by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var hasCompletedScan by remember { mutableStateOf(false) }
    var discoveredHosts by remember { mutableStateOf<List<DiscoveredPcHost>>(emptyList()) }
    var isPingingAll by remember { mutableStateOf(false) }
    val pingStatuses = remember { mutableStateMapOf<String, PcHostPingStatus>() }

    var editingHost by remember { mutableStateOf<SavedPcHost?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }

    fun refreshLocalIpAndPings() {
        onRefreshStatus()
        scope.launch {
            localIp = PcLinkDiscoveryEngine.getLocalIpv4Address()
            if (savedHosts.isNotEmpty()) {
                isPingingAll = true
                savedHosts.forEach { host ->
                    pingStatuses[host.id] = PcLinkDiscoveryEngine.pingPcHost(host)
                }
                isPingingAll = false
            }
        }
    }

    LaunchedEffect(savedHosts.size) {
        refreshLocalIpAndPings()
    }

    fun startLanScan() {
        if (isScanningLan) return
        scope.launch {
            isScanningLan = true
            scanProgress = 0f
            hasCompletedScan = false
            localIp = PcLinkDiscoveryEngine.getLocalIpv4Address()
            discoveredHosts = PcLinkDiscoveryEngine.scanLanForGamingPcs { progress ->
                scanProgress = progress
            }
            hasCompletedScan = true
            isScanningLan = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Steam Link · My PC",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = localIp?.let { "Wi-Fi LAN • $it" } ?: "Connect phone & PC to same Wi-Fi",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = if (localIp != null) SteamLinkCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("steam_link_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshLocalIpAndPings() },
                        enabled = !isPingingAll,
                        modifier = Modifier.testTag("steam_link_refresh_button")
                    ) {
                        if (isPingingAll) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh PC Status"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            editingHost = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("steam_link_add_pc_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add PC or Laptop"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. HERO QUICK-LAUNCH CARD
                item(key = "steam_link_hero") {
                    SteamLinkHeroCard(
                        activeProfile = activeProfile,
                        readiness = readiness,
                        isOverlayRunning = isOverlayRunning,
                        onLaunchSteamLink = {
                            onLaunchClientWithOverlay(PcStreamClient.STEAM_LINK, null)
                        },
                        onLaunchMoonlight = {
                            onLaunchClientWithOverlay(PcStreamClient.MOONLIGHT, null)
                        },
                        onLaunchParsec = {
                            onLaunchClientWithOverlay(PcStreamClient.PARSEC, null)
                        },
                        onToggleOverlay = onToggleOverlay,
                        onEditActiveProfile = { activeProfile?.let { onOpenEditor(it.id) } }
                    )
                }

                // 2. ACTIVE TOUCH LAYOUT QUICK PICKER
                if (allProfiles.isNotEmpty()) {
                    item(key = "steam_link_profile_picker") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOUCH CONTROLLER LAYOUT FOR PC",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                activeProfile?.let {
                                    Text(
                                        text = "${it.controls.size} controls",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allProfiles, key = { it.id }) { profile ->
                                    val selected = profile.id == activeProfile?.id
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectProfile(profile.id) },
                                        label = {
                                            Text(
                                                text = profile.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Gamepad,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SteamLinkBlue.copy(alpha = 0.2f),
                                            selectedLabelColor = SteamLinkCyan,
                                            selectedLeadingIconColor = SteamLinkCyan
                                        ),
                                        modifier = Modifier.testTag("steam_link_profile_chip_${profile.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. SAVED PCS & LAPTOPS
                item(key = "saved_pcs_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MY PCS & LAPTOPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Save your desktop or laptop for 1-tap touch HUD + streaming",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                editingHost = null
                                showAddEditDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("steam_link_add_pc_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add PC")
                        }
                    }
                }

                if (savedHosts.isEmpty()) {
                    item(key = "saved_pcs_empty") {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SteamLinkBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Laptop,
                                        contentDescription = null,
                                        tint = SteamLinkCyan,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "No PCs or Laptops Saved Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scan your Wi-Fi below to auto-detect a PC running Steam or Sunshine, or add your PC's IP manually to track ping & use Wake-on-LAN.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(savedHosts, key = { it.id }) { host ->
                        val linkedProfileName = allProfiles.firstOrNull { it.id == host.linkedProfileId }?.name
                        val ping = pingStatuses[host.id]
                        SavedPcHostCard(
                            host = host,
                            linkedProfileName = linkedProfileName,
                            pingStatus = ping,
                            onPlay = { onLaunchSavedHost(host) },
                            onWake = { onWakeHost(host) },
                            onEdit = {
                                editingHost = host
                                showAddEditDialog = true
                            },
                            onDelete = { onDeleteHost(host.id) }
                        )
                    }
                }

                // 4. REAL LOCAL WI-FI LAN SCANNER
                item(key = "lan_scanner_card") {
                    LanScannerCard(
                        localIp = localIp,
                        isScanning = isScanningLan,
                        progress = scanProgress,
                        hasCompletedScan = hasCompletedScan,
                        discoveredHosts = discoveredHosts,
                        savedHosts = savedHosts,
                        onStartScan = { startLanScan() },
                        onSaveDiscovered = { discovered ->
                            val newHost = SavedPcHost(
                                id = UUID.randomUUID().toString(),
                                name = discovered.hostname,
                                hostAddress = discovered.ipAddress,
                                client = discovered.suggestedClient,
                                linkedProfileId = activeProfile?.id.orEmpty(),
                                autoStartOverlay = true
                            )
                            onSaveHost(newHost)
                        }
                    )
                }

                // 5. TOUCH SCREEN READINESS FOR STEAM LINK
                item(key = "pc_touch_readiness") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TOUCH OVERLAY READINESS FOR STEAM LINK",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        PcReadinessItem(
                            icon = Icons.Default.Layers,
                            title = "Overlay Permission",
                            subtitle = if (readiness.canDrawOverlays) {
                                "XenosPad can draw your touch controls over Steam Link"
                            } else {
                                "Required to float custom touch controls on top of Steam Link"
                            },
                            isReady = readiness.canDrawOverlays,
                            actionLabel = if (readiness.canDrawOverlays) "READY" else "GRANT",
                            onClick = onRequestOverlayPermission,
                            testTag = "steam_link_readiness_overlay"
                        )
                        PcReadinessItem(
                            icon = Icons.Default.Keyboard,
                            title = "XenosPad Gamepad IME",
                            subtitle = when {
                                readiness.isImeSelected -> "Active — sending virtual controller & keyboard inputs to PC"
                                readiness.isImeEnabled -> "Enabled — tap to select XenosPad IME for PC games"
                                else -> "Enable XenosPad IME to send gamepad/WASD keys to Steam Link"
                            },
                            isReady = readiness.isImeSelected,
                            actionLabel = when {
                                readiness.isImeSelected -> "ACTIVE"
                                readiness.isImeEnabled -> "SELECT"
                                else -> "ENABLE"
                            },
                            onClick = onOpenImeSettings,
                            testTag = "steam_link_readiness_ime"
                        )
                        PcReadinessItem(
                            icon = Icons.Default.Mouse,
                            title = "Drag-Aim & Mouse Engine",
                            subtitle = if (readiness.isAccessibilityEnabled) {
                                "Active — touch drag-aim & gyro mouse work in PC shooters"
                            } else {
                                "Enable Accessibility service for camera drag-aim & mouse clicks"
                            },
                            isReady = readiness.isAccessibilityEnabled,
                            actionLabel = if (readiness.isAccessibilityEnabled) "READY" else "ENABLE",
                            onClick = onOpenAccessibilitySettings,
                            testTag = "steam_link_readiness_accessibility"
                        )
                        PcReadinessItem(
                            icon = Icons.Default.CastConnected,
                            title = "Steam Link Android App",
                            subtitle = if (readiness.isSteamLinkInstalled) {
                                "Installed — ready to stream your PC/laptop screen"
                            } else {
                                "Not installed yet — tap to get official Valve Steam Link on Play Store"
                            },
                            isReady = readiness.isSteamLinkInstalled,
                            actionLabel = if (readiness.isSteamLinkInstalled) "INSTALLED" else "INSTALL",
                            onClick = { onLaunchClientWithOverlay(PcStreamClient.STEAM_LINK, null) },
                            testTag = "steam_link_readiness_app"
                        )
                    }
                }

                // 6. QUICK SETUP GUIDE (HOW TO USE XENOSPAD TOUCH CONTROLS OVER STEAM LINK)
                item(key = "steam_link_guide") {
                    SteamLinkSetupGuideCard()
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditPcHostDialog(
            initialHost = editingHost,
            profiles = allProfiles,
            defaultProfileId = activeProfile?.id.orEmpty(),
            onDismiss = {
                showAddEditDialog = false
                editingHost = null
            },
            onSave = { host ->
                onSaveHost(host)
                showAddEditDialog = false
                editingHost = null
            }
        )
    }
}

@Composable
private fun SteamLinkHeroCard(
    activeProfile: ControllerProfile?,
    readiness: SystemReadiness,
    isOverlayRunning: Boolean,
    onLaunchSteamLink: () -> Unit,
    onLaunchMoonlight: () -> Unit,
    onLaunchParsec: () -> Unit,
    onToggleOverlay: () -> Unit,
    onEditActiveProfile: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SteamLinkHeroBottom),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = SteamLinkCyan.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.large
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SteamLinkHeroTop, SteamLinkHeroMid, SteamLinkHeroBottom)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SteamLinkBlue.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(
                                1.dp,
                                SteamLinkCyan.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CastConnected,
                                    contentDescription = null,
                                    tint = SteamLinkCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PC REMOTE PLAY",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SteamLinkCyan
                                )
                            }
                        }

                        Surface(
                            color = if (isOverlayRunning) SteamLinkGreen.copy(alpha = 0.2f) else Color(0xFF1A2535),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isOverlayRunning) "TOUCH HUD LIVE" else "HUD READY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverlayRunning) SteamLinkGreen else Color(0xFFB5C7DB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditActiveProfile,
                        enabled = activeProfile != null,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("steam_link_edit_layout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Touch Controls",
                            tint = SteamLinkCyan
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Play Your PC with XenosPad Touch Screen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Stream games from your Windows, Mac, or Linux PC via Steam Link while floating your custom XenosPad layout (${activeProfile?.name ?: "Default"}), gyro aim, macros & GPU filters on top.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8CCE0)
                    )
                }

                Button(
                    onClick = onLaunchSteamLink,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteamLinkGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("steam_link_launch_primary_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (readiness.isSteamLinkInstalled) {
                            "Start Touch HUD & Open Steam Link"
                        } else {
                            "Install Steam Link & Start Touch HUD"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                // Secondary quick actions: Toggle HUD Only, Moonlight, Parsec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleOverlay,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(44.dp)
                            .testTag("steam_link_toggle_hud_only_btn")
                    ) {
                        Icon(
                            imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (isOverlayRunning) MacroCoral else SteamLinkCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isOverlayRunning) "Stop HUD" else "HUD Only",
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onLaunchMoonlight,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("steam_link_launch_moonlight_btn")
                    ) {
                        Text(
                            text = if (readiness.isMoonlightInstalled) "Moonlight" else "Moonlight+",
                            color = SteamLinkCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onLaunchParsec,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("steam_link_launch_parsec_btn")
                    ) {
                        Text(
                            text = if (readiness.isParsecInstalled) "Parsec" else "Parsec+",
                            color = Color(0xFFD5E4F5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPcHostCard(
    host: SavedPcHost,
    linkedProfileName: String?,
    pingStatus: PcHostPingStatus?,
    onPlay: () -> Unit,
    onWake: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOnline = pingStatus?.reachable == true
    val statusColor = when {
        pingStatus == null -> MaterialTheme.colorScheme.onSurfaceVariant
        isOnline -> SteamLinkGreen
        else -> TurboAmber
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOnline) SteamLinkGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
            .testTag("saved_pc_card_${host.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SteamLinkBlue.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = SteamLinkCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = host.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = SteamLinkBlue.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = host.client.shortLabel.uppercase(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SteamLinkCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = buildString {
                            append(host.hostAddress.ifBlank { "Auto-discover via app" })
                            if (!linkedProfileName.isNullOrBlank()) {
                                append("  •  Layout: ")
                                append(linkedProfileName)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = pingStatus?.detail ?: "Checking LAN reachability...",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit PC",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete PC",
                            tint = MacroCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SteamLinkBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("saved_pc_play_${host.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (host.autoStartOverlay) {
                            "Play + Touch HUD"
                        } else {
                            "Launch ${host.client.shortLabel}"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (host.macAddress.isNotBlank()) {
                    OutlinedButton(
                        onClick = onWake,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("saved_pc_wake_${host.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Wake PC",
                            tint = SteamLinkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Wake", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanScannerCard(
    localIp: String?,
    isScanning: Boolean,
    progress: Float,
    hasCompletedScan: Boolean,
    discoveredHosts: List<DiscoveredPcHost>,
    savedHosts: List<SavedPcHost>,
    onStartScan: () -> Unit,
    onSaveDiscovered: (DiscoveredPcHost) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SteamLinkBlue.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = SteamLinkCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Wi-Fi Gaming PC Scanner",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (localIp != null) {
                                "Scans local subnet for Steam Remote Play & Sunshine"
                            } else {
                                "Join the same Wi-Fi network as your PC/laptop"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onStartScan,
                    enabled = !isScanning && localIp != null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("steam_link_scan_lan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (isScanning) "Scanning..." else "Scan Wi-Fi")
                }
            }

            if (isScanning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SteamLinkCyan
                    )
                    Text(
                        text = "Probing local LAN ports 27036 (Steam) & 47989 (Sunshine)… ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (discoveredHosts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    discoveredHosts.forEach { host ->
                        val alreadySaved = savedHosts.any {
                            it.hostAddress.equals(host.ipAddress, ignoreCase = true)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = host.hostname,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${host.ipAddress} • ${host.detectedService} • ${host.rttMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = SteamLinkCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (alreadySaved) {
                                    Text(
                                        text = "SAVED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SteamLinkGreen
                                    )
                                } else {
                                    TextButton(onClick = { onSaveDiscovered(host) }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (hasCompletedScan && !isScanning) {
                Text(
                    text = "No active Steam Remote Play or Sunshine hosts responded on this Wi-Fi subnet. Make sure Steam is running on your PC with Remote Play enabled, or tap Add PC to enter your IP manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PcReadinessItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = if (isReady) SteamLinkGreen else TurboAmber
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SteamLinkSetupGuideCard() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SteamLinkCyan.copy(alpha = 0.35f), MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SteamLinkCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "3-Step PC Touch Screen Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            GuideStepRow(
                stepNumber = "1",
                title = "Enable Remote Play on your PC / Laptop",
                body = "Open Steam on your PC → Settings → Remote Play → Enable Remote Play (or install Sunshine for Moonlight)."
            )
            GuideStepRow(
                stepNumber = "2",
                title = "Disable Steam Link's Built-in Touch Buttons",
                body = "In Steam Link on Android → Settings → Controller → set Touch Controller to Disabled so XenosPad's custom buttons don't overlap."
            )
            GuideStepRow(
                stepNumber = "3",
                title = "Launch with XenosPad Touch HUD",
                body = "Pick your custom button layout above and tap Start Touch HUD & Open Steam Link. Use the floating XenosPad bubble anytime to edit buttons, toggle Gyro Aim, or apply Filters."
            )
        }
    }
}

@Composable
private fun GuideStepRow(
    stepNumber: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SteamLinkBlue.copy(alpha = 0.2f))
                .border(1.dp, SteamLinkCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SteamLinkCyan
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddEditPcHostDialog(
    initialHost: SavedPcHost?,
    profiles: List<ControllerProfile>,
    defaultProfileId: String,
    onDismiss: () -> Unit,
    onSave: (SavedPcHost) -> Unit
) {
    var name by remember(initialHost) { mutableStateOf(initialHost?.name ?: "") }
    var hostAddress by remember(initialHost) { mutableStateOf(initialHost?.hostAddress ?: "") }
    var macAddress by remember(initialHost) { mutableStateOf(initialHost?.macAddress ?: "") }
    var client by remember(initialHost) { mutableStateOf(initialHost?.client ?: PcStreamClient.STEAM_LINK) }
    var customWebUrl by remember(initialHost) { mutableStateOf(initialHost?.customWebUrl ?: "") }
    var linkedProfileId by remember(initialHost) {
        mutableStateOf(initialHost?.linkedProfileId?.ifBlank { defaultProfileId } ?: defaultProfileId)
    }
    var autoStartOverlay by remember(initialHost) {
        mutableStateOf(initialHost?.autoStartOverlay ?: true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialHost == null) "Add Gaming PC / Laptop" else "Edit PC Profile")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("PC / Laptop Name") },
                    placeholder = { Text("My Gaming Laptop") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pc_dialog_name_input")
                )

                OutlinedTextField(
                    value = hostAddress,
                    onValueChange = { hostAddress = it },
                    label = { Text("Local IP or Hostname") },
                    placeholder = { Text("192.168.1.50") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pc_dialog_ip_input")
                )

                Text(
                    text = "Streaming App",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PcStreamClient.entries) { option ->
                        FilterChip(
                            selected = client == option,
                            onClick = { client = option },
                            label = { Text(option.shortLabel) }
                        )
                    }
                }

                if (client == PcStreamClient.WEB_STREAM) {
                    OutlinedTextField(
                        value = customWebUrl,
                        onValueChange = { customWebUrl = it },
                        label = { Text("PC Web Stream URL") },
                        placeholder = { Text("http://192.168.1.50:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("MAC Address (Optional for Wake-on-LAN)") },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (profiles.isNotEmpty()) {
                    Text(
                        text = "Linked Touch Controller Layout",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(profiles, key = { it.id }) { profile ->
                            FilterChip(
                                selected = linkedProfileId == profile.id,
                                onClick = { linkedProfileId = profile.id },
                                label = {
                                    Text(
                                        text = profile.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Start Touch Overlay HUD",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Float XenosPad controls automatically when launching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStartOverlay,
                        onCheckedChange = { autoStartOverlay = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim().ifBlank { "My Gaming PC" }
                    onSave(
                        SavedPcHost(
                            id = initialHost?.id ?: UUID.randomUUID().toString(),
                            name = trimmedName,
                            hostAddress = hostAddress.trim(),
                            macAddress = macAddress.trim().uppercase(),
                            client = client,
                            customWebUrl = customWebUrl.trim(),
                            linkedProfileId = linkedProfileId,
                            autoStartOverlay = autoStartOverlay
                        )
                    )
                },
                enabled = name.isNotBlank() || hostAddress.isNotBlank(),
                modifier = Modifier.testTag("pc_dialog_save_button")
            ) {
                Text("Save PC")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
