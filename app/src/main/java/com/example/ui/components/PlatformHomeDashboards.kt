package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppPlatformTheme
import com.example.model.ControllerProfile
import com.example.model.PinnedGame
import com.example.ui.theme.JetBrainsMonoFontFamily

/**
 * Authentic platform-specific Home/Cockpit hero deck for Steam Deck Gaming Mode,
 * Epic Games Launcher, Xbox Dashboard, Nintendo Switch Home, and PlayStation 5 Home.
 * Every button is wired to real XenosPad features (Overlay, GeForce NOW Web Play,
 * Layout Studio, Pinned Games, and Live Storefront when supported).
 */
@Composable
fun PlatformHeroDashboardCard(
    platformTheme: AppPlatformTheme,
    activeProfile: ControllerProfile?,
    pinnedGames: List<PinnedGame>,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onOpenStore: (() -> Unit)? = null
) {
    when (platformTheme) {
        AppPlatformTheme.STEAM_DECK -> SteamDeckHomeDeck(
            activeProfile = activeProfile,
            pinnedGames = pinnedGames,
            isOverlayRunning = isOverlayRunning,
            onToggleOverlay = onToggleOverlay,
            onOpenEditor = onOpenEditor,
            onLaunchWebPlayer = onLaunchWebPlayer,
            onLaunchPinnedGame = onLaunchPinnedGame,
            onOpenStore = onOpenStore
        )
        AppPlatformTheme.EPIC_GAMES -> EpicHomeHeroDeck(
            activeProfile = activeProfile,
            isOverlayRunning = isOverlayRunning,
            onToggleOverlay = onToggleOverlay,
            onOpenEditor = onOpenEditor,
            onLaunchWebPlayer = onLaunchWebPlayer,
            onOpenStore = onOpenStore
        )
        AppPlatformTheme.XBOX -> XboxHomeHeroDeck(
            activeProfile = activeProfile,
            pinnedGames = pinnedGames,
            isOverlayRunning = isOverlayRunning,
            onToggleOverlay = onToggleOverlay,
            onOpenEditor = onOpenEditor,
            onLaunchWebPlayer = onLaunchWebPlayer,
            onLaunchPinnedGame = onLaunchPinnedGame,
            onOpenStore = onOpenStore
        )
        AppPlatformTheme.NINTENDO_SWITCH -> SwitchHomeTileStrip(
            activeProfile = activeProfile,
            pinnedGames = pinnedGames,
            isOverlayRunning = isOverlayRunning,
            onToggleOverlay = onToggleOverlay,
            onOpenEditor = onOpenEditor,
            onLaunchWebPlayer = onLaunchWebPlayer,
            onLaunchPinnedGame = onLaunchPinnedGame
        )
        AppPlatformTheme.PLAYSTATION -> PlayStationHomeHeroDeck(
            activeProfile = activeProfile,
            isOverlayRunning = isOverlayRunning,
            onToggleOverlay = onToggleOverlay,
            onOpenEditor = onOpenEditor,
            onLaunchWebPlayer = onLaunchWebPlayer
        )
        AppPlatformTheme.XENOS_DEFAULT -> Unit
    }
}

@Composable
private fun SteamDeckHomeDeck(
    activeProfile: ControllerProfile?,
    pinnedGames: List<PinnedGame>,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onOpenStore: (() -> Unit)?
) {
    val steamCyan = Color(0xFF1A9FFF)
    val steamSky = Color(0xFF66C0F4)
    val steamGreen = Color(0xFF59BF40)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Steam Deck Big Picture Recent / Featured Strip
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171D25)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, steamCyan.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                .testTag("steam_deck_home_hero")
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit),
                    contentDescription = "Steam Deck Gaming Mode",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x660E141B), Color(0xCC0E141B), Color(0xFA171D25))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xCC1E2837),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, steamSky.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "RECENTLY PLAYED • STEAM DECK MODE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = steamSky,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = if (isOverlayRunning) steamGreen.copy(alpha = 0.25f) else Color(0xAA171D25),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isOverlayRunning) "HUD LIVE" else "READY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverlayRunning) steamGreen else Color(0xFFD0DCEB),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = activeProfile?.name ?: "XenosPad Deck Layout",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${activeProfile?.controls?.size ?: 0} mapped controls • GeForce NOW Steam Deck Control Shell",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C7D9)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onLaunchWebPlayer,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = steamGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.weight(1.2f).height(44.dp).testTag("steam_deck_play_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PLAY GFN", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = onToggleOverlay,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = steamCyan),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(
                                if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isOverlayRunning) "Stop" else "Overlay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenEditor,
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Layout", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (onOpenStore != null) {
                        Surface(
                            color = Color(0xFF1E2D40),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, steamCyan.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                                .clickable(onClick = onOpenStore)
                                .testTag("steam_deck_open_store_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = steamSky, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Browse Steam Store (Great on Deck)",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text("OPEN →", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = steamSky, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Steam Deck Horizontal Library Carousel if user has pinned games
        if (pinnedGames.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(pinnedGames, key = { it.id }) { game ->
                    Surface(
                        color = Color(0xFF1E2837),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .width(156.dp)
                            .border(1.5.dp, Color(0xFF3D4F68), RoundedCornerShape(4.dp))
                            .clickable { onLaunchPinnedGame(game) }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = game.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${game.regionPreset.shortCode} • Tap to Play",
                                color = steamSky,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpicHomeHeroDeck(
    activeProfile: ControllerProfile?,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onOpenStore: (() -> Unit)?
) {
    val epicBlue = Color(0xFF0078F2)
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181D)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(8.dp))
            .testTag("epic_home_hero")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK LAUNCH • EPIC MODE",
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = epicBlue
                )
                if (onOpenStore != null) {
                    Surface(
                        color = epicBlue.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable(onClick = onOpenStore)
                    ) {
                        Text(
                            text = "EPIC STORE →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = epicBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = activeProfile?.name ?: "XenosPad Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLaunchWebPlayer,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = epicBlue),
                    modifier = Modifier.weight(1.2f).height(46.dp)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("LAUNCH GFN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onToggleOverlay,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(if (isOverlayRunning) "Stop HUD" else "Start HUD", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onOpenEditor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun XboxHomeHeroDeck(
    activeProfile: ControllerProfile?,
    pinnedGames: List<PinnedGame>,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onOpenStore: (() -> Unit)?
) {
    val xboxGreen = Color(0xFF107C10)
    val xboxBright = Color(0xFF52B043)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, xboxBright, RoundedCornerShape(10.dp))
            .testTag("xbox_home_hero")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF113811), Color(0xFF1A1A1A))))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("XBOX CLOUD DASHBOARD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = xboxBright)
                if (onOpenStore != null) {
                    Surface(
                        color = xboxGreen,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(onClick = onOpenStore)
                    ) {
                        Text(
                            text = "XBOX STORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = activeProfile?.name ?: "Jump Back In",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLaunchWebPlayer,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = xboxGreen),
                    modifier = Modifier.weight(1.2f).height(46.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play on GFN", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onToggleOverlay,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(if (isOverlayRunning) "Stop HUD" else "Overlay")
                }
                OutlinedButton(
                    onClick = onOpenEditor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
            }
            if (pinnedGames.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pinnedGames, key = { it.id }) { g ->
                        Surface(
                            color = Color(0xFF262626),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(136.dp)
                                .border(1.dp, xboxBright.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onLaunchPinnedGame(g) }
                        ) {
                            Text(
                                text = g.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchHomeTileStrip(
    activeProfile: ControllerProfile?,
    pinnedGames: List<PinnedGame>,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit
) {
    val joyBlue = Color(0xFF00C3E3)
    val joyRed = Color(0xFFE60012)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("switch_home_hero")
    ) {
        Text(
            text = activeProfile?.name ?: "XenosPad Home",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = joyBlue
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "switch_gfn_tile") {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(joyBlue, joyRed)))
                        .border(3.dp, joyBlue, RoundedCornerShape(8.dp))
                        .clickable(onClick = onLaunchWebPlayer)
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Text("GeForce NOW", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text("Web Play", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
            item(key = "switch_overlay_tile") {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(2.dp, if (isOverlayRunning) joyRed else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleOverlay)
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Icon(
                            if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = if (isOverlayRunning) joyRed else joyBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(if (isOverlayRunning) "Stop Overlay" else "Start Overlay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${activeProfile?.controls?.size ?: 0} controls", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "switch_editor_tile") {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenEditor)
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFFD000), modifier = Modifier.size(26.dp))
                        Text("Layout Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Customize keys", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(pinnedGames, key = { it.id }) { g ->
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.5.dp, joyBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onLaunchPinnedGame(g) }
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(g.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Launch →", fontSize = 11.sp, color = joyBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayStationHomeHeroDeck(
    activeProfile: ControllerProfile?,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit,
    onLaunchWebPlayer: () -> Unit
) {
    val psBlue = Color(0xFF0070D1)
    val psGlow = Color(0xFF5BB2FF)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1328)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, psGlow.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .testTag("ps5_home_hero")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.radialGradient(listOf(Color(0xFF1A3C7A), Color(0xFF0A1328), Color(0xFF050A14))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PLAYSTATION 5 MODE",
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = psGlow,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = activeProfile?.name ?: "XenosPad Control Center",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onLaunchWebPlayer,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1.2f).height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onToggleOverlay,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = psBlue),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text(if (isOverlayRunning) "Stop HUD" else "Overlay", fontWeight = FontWeight.SemiBold)
                    }
                    Surface(
                        color = Color(0x44FFFFFF),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable(onClick = onOpenEditor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Layout", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
