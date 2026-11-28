package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AppPlatformTheme

private data class StoreQuickCategory(
    val label: String,
    val url: String
)

private fun storeCategoriesFor(theme: AppPlatformTheme): List<StoreQuickCategory> = when (theme) {
    AppPlatformTheme.STEAM_DECK -> listOf(
        StoreQuickCategory("Featured", "https://store.steampowered.com/"),
        StoreQuickCategory("Great on Deck", "https://store.steampowered.com/greatondeck"),
        StoreQuickCategory("Free to Play", "https://store.steampowered.com/genre/Free%20to%20Play/"),
        StoreQuickCategory("Top Sellers", "https://store.steampowered.com/charts/topselling/global"),
        StoreQuickCategory("Specials", "https://store.steampowered.com/specials")
    )
    AppPlatformTheme.EPIC_GAMES -> listOf(
        StoreQuickCategory("Discover", "https://store.epicgames.com/en-US/"),
        StoreQuickCategory("Free Games", "https://store.epicgames.com/en-US/free-games"),
        StoreQuickCategory("Browse", "https://store.epicgames.com/en-US/browse"),
        StoreQuickCategory("Top Played", "https://store.epicgames.com/en-US/browse?sortBy=currentPrice&sortDir=ASC")
    )
    AppPlatformTheme.XBOX -> listOf(
        StoreQuickCategory("All Games", "https://www.xbox.com/en-US/games/all-games"),
        StoreQuickCategory("Game Pass", "https://www.xbox.com/en-US/xbox-game-pass/games"),
        StoreQuickCategory("Free to Play", "https://www.xbox.com/en-US/games/free-to-play"),
        StoreQuickCategory("Deals", "https://www.xbox.com/en-US/games/all-games?cat=onsale")
    )
    else -> emptyList()
}

private fun buildStoreSearchUrl(theme: AppPlatformTheme, query: String): String {
    val encoded = Uri.encode(query.trim())
    return when (theme) {
        AppPlatformTheme.STEAM_DECK -> "https://store.steampowered.com/search/?term=$encoded"
        AppPlatformTheme.EPIC_GAMES -> "https://store.epicgames.com/en-US/browse?q=$encoded"
        AppPlatformTheme.XBOX -> "https://www.xbox.com/en-US/search?q=$encoded"
        else -> "https://store.steampowered.com/search/?term=$encoded"
    }
}

/**
 * Real embedded storefront browser for GeForce NOW-supported platforms:
 * Steam Store (Steam Deck theme), Epic Games Store (Epic theme), and Xbox Store (Xbox theme).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformStoreScreen(
    platformTheme: AppPlatformTheme,
    onLaunchGeForceNowWebPlay: () -> Unit,
    onLaunchXboxCloud: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!platformTheme.hasLiveGfnStore) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Store Available on Steam, Epic & Xbox Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Switch to Steam Deck, Epic Games, or Xbox theme in Settings to browse GeForce NOW-compatible games directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val categories = remember(platformTheme) { storeCategoriesFor(platformTheme) }
    val homeUrl = remember(platformTheme) {
        platformTheme.storeHomeUrl ?: categories.firstOrNull()?.url ?: "https://store.steampowered.com/"
    }

    var searchQuery by remember(platformTheme) { mutableStateOf("") }
    var activeCategoryUrl by remember(platformTheme) { mutableStateOf(homeUrl) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(platformTheme) {
        webViewRef?.loadUrl(homeUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            }
        }
    }

    fun submitSearch() {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            val url = buildStoreSearchUrl(platformTheme, trimmed)
            activeCategoryUrl = url
            webViewRef?.loadUrl(url)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Store Chrome Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Platform Store Title + Nav Controls + GeForce NOW Launch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = platformTheme.storeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(34.dp).testTag("store_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.size(34.dp).testTag("store_forward_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            activeCategoryUrl = homeUrl
                            webViewRef?.loadUrl(homeUrl)
                        },
                        modifier = Modifier.size(34.dp).testTag("store_home_btn")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Store Home", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.size(34.dp).testTag("store_reload_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                    }
                    val isXboxStorePlay = platformTheme == AppPlatformTheme.XBOX && onLaunchXboxCloud != null
                    Button(
                        onClick = if (isXboxStorePlay) {
                            onLaunchXboxCloud!!
                        } else {
                            onLaunchGeForceNowWebPlay
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isXboxStorePlay) Color(0xFF107C10) else primary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("store_launch_gfn_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (isXboxStorePlay) "Play xCloud" else "Play GFN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Row 2: Search Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search ${platformTheme.storeTitle} games...",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                        shape = RoundedCornerShape(platformTheme.cardCornerDp.coerceIn(6, 12).dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("store_search_input")
                    )
                    Button(
                        onClick = { submitSearch() },
                        enabled = searchQuery.isNotBlank(),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("store_search_go_btn")
                    ) {
                        Text("Go", fontWeight = FontWeight.Bold)
                    }
                }

                // Row 3: Quick Categories
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = activeCategoryUrl == cat.url,
                            onClick = {
                                activeCategoryUrl = cat.url
                                webViewRef?.loadUrl(cat.url)
                            },
                            label = { Text(cat.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }

        // Real Store WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    setBackgroundColor(android.graphics.Color.parseColor("#0E141B"))
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            // Keep navigation inside the embedded store for HTTP/HTTPS pages
                            return !(url.startsWith("https://") || url.startsWith("http://"))
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            loadProgress = newProgress
                        }
                    }
                    loadUrl(homeUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("platform_store_webview")
        )
    }
}
