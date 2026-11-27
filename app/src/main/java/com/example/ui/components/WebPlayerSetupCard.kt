package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GfnRegionPreset
import com.example.model.LocalNexusStrings
import com.example.model.WebPlayerUserAgentMode
import com.example.model.WebStreamQualityConfig
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.webplayer.PingQuality
import com.example.webplayer.RegionPingMonitor
import com.example.webplayer.RegionPingResult

/**
 * Prominent Better xCloud-style launch card displayed on CockpitScreen.
 * Allows switching Region Bypass in 1 tap and launching play.geforcenow.com directly inside XenosPad.
 */
@Composable
fun BetterGfnCockpitCard(
    config: WebStreamQualityConfig,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onLaunchWebPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val scope = rememberCoroutineScope()
    val regionPings by RegionPingMonitor.pings.collectAsStateWithLifecycle()
    val isRefreshingPings by RegionPingMonitor.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        RegionPingMonitor.refreshIfStale(scope)
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(primary.copy(alpha = 0.72f), TacticalCyan.copy(alpha = 0.55f))
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
            .testTag("cockpit_better_gfn_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.14f),
                            secondary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primary.copy(alpha = 0.20f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.webPlayerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "play.geforcenow.com • ${config.userAgentMode.shortLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TacticalCyan
                            )
                        }
                    }

                    val activePing = regionPings[config.regionPreset]
                    val activePingText = activePing?.pingMs?.let { " • ${it}ms" } ?: ""
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TacticalCyan.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (config.regionPreset.isEnabled) {
                                "REGION: ${config.regionPreset.shortCode}$activePingText"
                            } else if (activePing?.pingMs != null) {
                                "LOCAL • ${activePing.pingMs}ms"
                            } else {
                                strings.webPlayerZeroPermBadge
                            },
                            color = TacticalCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = strings.webPlayerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Region Bypass bar right on Cockpit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.webPlayerRegionHeader,
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RegionPingRefreshButton(
                            isRefreshing = isRefreshingPings,
                            onRefresh = { RegionPingMonitor.refreshNow(scope) },
                            testTag = "cockpit_refresh_region_ping"
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "FREE 1H TIER",
                                color = primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GfnRegionPreset.entries) { region ->
                        val selected = config.regionPreset == region
                        val pingResult = regionPings[region]
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectRegion(region) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (region == GfnRegionPreset.OFF) {
                                            region.displayName
                                        } else {
                                            "${region.shortCode} • ${region.displayName.substringBefore(" (")}"
                                        },
                                        fontSize = 12.sp
                                    )
                                    RegionPingPill(result = pingResult)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalCyan.copy(alpha = 0.22f),
                                selectedLabelColor = TacticalCyan
                            ),
                            modifier = Modifier.testTag("cockpit_region_${region.name.lowercase()}")
                        )
                    }
                }

                Button(
                    onClick = onLaunchWebPlayer,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_web_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.webPlayerLaunchButton,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Full Better-GFN Web Player & Built-in Region Bypass configuration card on SetupScreen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebPlayerSetupCard(
    config: WebStreamQualityConfig,
    onUpdate: (WebStreamQualityConfig) -> Unit,
    onLaunchWebPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val regionPings by RegionPingMonitor.pings.collectAsStateWithLifecycle()
    val isRefreshingPings by RegionPingMonitor.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        RegionPingMonitor.refreshIfStale(scope)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .testTag("setup_web_player_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = TacticalCyan)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.webPlayerCardHeader,
                        style = MaterialTheme.typography.labelMedium,
                        color = TacticalCyan
                    )
                    Text(
                        text = strings.webPlayerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Region Bypass selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.webPlayerRegionHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = primary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RegionPingRefreshButton(
                        isRefreshing = isRefreshingPings,
                        onRefresh = { RegionPingMonitor.refreshNow(scope) },
                        testTag = "setup_refresh_region_ping"
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "ALL FREE 1H TIER",
                            color = primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GfnRegionPreset.entries.forEach { region ->
                    val pingResult = regionPings[region]
                    FilterChip(
                        selected = config.regionPreset == region,
                        onClick = { onUpdate(config.copy(regionPreset = region)) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (region.hasFreeOneHourTier) {
                                        "${region.displayName} • ${region.tierLabel}"
                                    } else {
                                        region.displayName
                                    },
                                    fontSize = 12.sp
                                )
                                RegionPingPill(result = pingResult)
                            }
                        },
                        modifier = Modifier.testTag("setup_region_${region.name.lowercase()}")
                    )
                }
            }

            // Honest technical note explaining header bypass vs external VPN
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TacticalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.webPlayerRegionNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // User-Agent Mode selector
            Text(
                text = strings.webPlayerUserAgentLabel,
                style = MaterialTheme.typography.labelMedium,
                color = primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WebPlayerUserAgentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.userAgentMode == mode,
                        onClick = { onUpdate(config.copy(userAgentMode = mode)) },
                        label = { Text(mode.displayName, fontSize = 12.sp) },
                        modifier = Modifier.testTag("setup_ua_${mode.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Feature toggles
            WebPlayerToggleRow(
                title = strings.webPlayerSpoofLocaleTitle,
                subtitle = strings.webPlayerSpoofLocaleSub,
                checked = config.spoofTimezoneAndLocale,
                onCheckedChange = { onUpdate(config.copy(spoofTimezoneAndLocale = it)) },
                testTag = "setup_web_spoof_locale"
            )
            WebPlayerToggleRow(
                title = strings.webPlayerVirtualPadTitle,
                subtitle = strings.webPlayerVirtualPadSub,
                checked = config.enableVirtualGamepad,
                onCheckedChange = { onUpdate(config.copy(enableVirtualGamepad = it)) },
                testTag = "setup_web_virtual_pad"
            )
            WebPlayerToggleRow(
                title = strings.webPlayerStretchTitle,
                subtitle = strings.webPlayerStretchSub,
                checked = config.stretchVideoToFill,
                onCheckedChange = { onUpdate(config.copy(stretchVideoToFill = it)) },
                testTag = "setup_web_stretch_video"
            )
            WebPlayerToggleRow(
                title = strings.webPlayerStatsHudTitle,
                subtitle = strings.webPlayerStatsHudSub,
                checked = config.showWebRtcStatsHud,
                onCheckedChange = { onUpdate(config.copy(showWebRtcStatsHud = it)) },
                testTag = "setup_web_stats_hud"
            )
            WebPlayerToggleRow(
                title = "Anti-AFK / Queue Keeper",
                subtitle = "Prevents GeForce NOW idle disconnects during queues or short breaks with harmless micro keep-alive ticks",
                checked = config.antiAfkEnabled,
                onCheckedChange = { onUpdate(config.copy(antiAfkEnabled = it)) },
                testTag = "setup_web_anti_afk"
            )
            if (config.antiAfkEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Interval:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(30, 45, 60, 90).forEach { sec ->
                        FilterChip(
                            selected = config.antiAfkIntervalSec == sec,
                            onClick = { onUpdate(config.copy(antiAfkIntervalSec = sec)) },
                            label = { Text("${sec}s", fontSize = 11.sp) },
                            modifier = Modifier.testTag("setup_web_anti_afk_${sec}s")
                        )
                    }
                }
            }

            Button(
                onClick = onLaunchWebPlayer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("setup_launch_web_player_button")
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.webPlayerLaunchButton, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WebPlayerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun RegionPingPill(
    result: RegionPingResult?,
    modifier: Modifier = Modifier
) {
    val quality = result?.quality ?: PingQuality.UNKNOWN
    val color = when (quality) {
        PingQuality.EXCELLENT -> Color(0xFF4ADE80)
        PingQuality.GOOD -> TacticalCyan
        PingQuality.FAIR -> Color(0xFFFBBF24)
        PingQuality.POOR -> Color(0xFFFB7185)
        PingQuality.UNKNOWN -> Color(0xFF94A3B8)
    }
    val text = RegionPingMonitor.formatBadgeText(result)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
fun RegionPingRefreshButton(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onRefresh,
        enabled = !isRefreshing,
        modifier = modifier
            .size(28.dp)
            .testTag(testTag)
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                strokeWidth = 1.8.dp,
                color = TacticalCyan,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh server ping",
                tint = TacticalCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
