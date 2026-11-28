package com.example.ui.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.capture.CaptureBridge
import com.example.cloud.CloudUserInfo
import com.example.data.VibrationMode
import com.example.model.ButtonThemePreset
import com.example.model.CommandCatalog
import com.example.model.CommunityConstants
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.model.ControllerNotation
import com.example.model.PostComment
import com.example.model.ProfileClipPost
import com.example.model.ThemeComment
import com.example.ui.components.LayoutStudioHelper
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.util.ButtonImageCodec
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunityAvatar(
    avatarBase64: String?,
    username: String,
    size: Dp = 36.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val bmp: ImageBitmap? = remember(avatarBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(avatarBase64)
    }
    val initials = remember(username) {
        username.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "P" }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(borderColor.copy(alpha = 0.18f))
            .border(1.5.dp, borderColor.copy(alpha = 0.8f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "$username avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                color = borderColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHubScreen(
    initialTab: Int,
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    localThemes: List<ButtonThemePreset>,
    activeThemeId: String?,
    workshopThemes: List<ButtonThemePreset>,
    likedThemeIds: Set<String>,
    favoriteThemeIds: Set<String>,
    communityPosts: List<CommunityTopicPost>,
    likedPostIds: Set<String>,
    myProfileClips: List<ProfileClipPost>,
    myProfileThemes: List<ButtonThemePreset>,
    myProfilePosts: List<CommunityTopicPost>,
    likedClipIds: Set<String>,
    isLoading: Boolean,
    notation: ControllerNotation,
    onSignIn: () -> Unit,
    onRefresh: () -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (String) -> Unit,
    onActivateTheme: (String?) -> Unit,
    onDeleteLocalTheme: (String) -> Unit,
    onDownloadWorkshopTheme: (ButtonThemePreset) -> Unit,
    onDeletePublishedTheme: (String) -> Unit,
    onToggleLikeTheme: (ButtonThemePreset) -> Unit,
    onToggleFavoriteTheme: (ButtonThemePreset) -> Unit,
    onLoadThemeComments: (String, (List<ThemeComment>) -> Unit) -> Unit,
    onAddThemeComment: (String, String, (ThemeComment?) -> Unit) -> Unit,
    onDeleteThemeComment: (String, String, () -> Unit) -> Unit,
    onCreatePost: (topic: String, title: String, body: String, imageUri: Uri?) -> Unit,
    onDeletePost: (String) -> Unit,
    onToggleLikePost: (CommunityTopicPost) -> Unit,
    onLoadPostComments: (String, (List<PostComment>) -> Unit) -> Unit,
    onAddPostComment: (String, String, (PostComment?) -> Unit) -> Unit,
    onDeletePostComment: (String, String, () -> Unit) -> Unit,
    onSaveMyProfile: (username: String, bio: String) -> Unit,
    onUploadMyAvatar: (Uri) -> Unit,
    onRemoveMyAvatar: () -> Unit,
    onDeleteClip: (String) -> Unit,
    onToggleLikeClip: (ProfileClipPost) -> Unit,
    onOpenGalleryForClip: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var inspectingTheme by remember { mutableStateOf<ButtonThemePreset?>(null) }
    var inspectingPost by remember { mutableStateOf<CommunityTopicPost?>(null) }
    var viewingClip by remember { mutableStateOf<ProfileClipPost?>(null) }
    var showNewPostDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cloudUser?.uid) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Button Theme Workshop"
                            1 -> "Community Discussions"
                            else -> myProfile?.username ?: "My Profile"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("community_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("community_refresh_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    Triple(0, "Workshop", Icons.Default.Palette),
                    Triple(1, "Posts", Icons.Default.Forum),
                    Triple(2, "Profile", Icons.Default.Person)
                )
                tabs.forEachIndexed { idx, (tabIdx, label, icon) ->
                    SegmentedButton(
                        selected = selectedTab == tabIdx,
                        onClick = { selectedTab = tabIdx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("community_tab_${label.lowercase()}")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            when (selectedTab) {
                0 -> WorkshopTabContent(
                    cloudUser = cloudUser,
                    localThemes = localThemes,
                    activeThemeId = activeThemeId,
                    workshopThemes = workshopThemes,
                    likedThemeIds = likedThemeIds,
                    favoriteThemeIds = favoriteThemeIds,
                    isLoading = isLoading,
                    notation = notation,
                    onSignIn = onSignIn,
                    onCreateTheme = onCreateTheme,
                    onEditTheme = onEditTheme,
                    onActivateTheme = onActivateTheme,
                    onDeleteLocalTheme = onDeleteLocalTheme,
                    onInspectTheme = { inspectingTheme = it },
                    onToggleLikeTheme = onToggleLikeTheme,
                    onToggleFavoriteTheme = onToggleFavoriteTheme,
                    onOpenAuthorProfile = onOpenPublicProfile
                )

                1 -> DiscussionsTabContent(
                    cloudUser = cloudUser,
                    posts = communityPosts,
                    likedPostIds = likedPostIds,
                    isLoading = isLoading,
                    onSignIn = onSignIn,
                    onNewPostClick = { showNewPostDialog = true },
                    onInspectPost = { inspectingPost = it },
                    onToggleLikePost = onToggleLikePost,
                    onDeletePost = onDeletePost,
                    onOpenAuthorProfile = onOpenPublicProfile
                )

                2 -> ProfileContentBody(
                    isOwnProfile = true,
                    currentUserUid = cloudUser?.uid,
                    profile = myProfile,
                    localDraftThemes = localThemes.filter { it.ownerId != "builtin" },
                    publishedThemes = myProfileThemes,
                    clips = myProfileClips,
                    posts = myProfilePosts,
                    likedClipIds = likedClipIds,
                    likedPostIds = likedPostIds,
                    isLoading = isLoading,
                    notation = notation,
                    onSignIn = onSignIn,
                    onEditProfileClick = { showEditProfileDialog = true },
                    onCreateTheme = onCreateTheme,
                    onEditDraftTheme = onEditTheme,
                    onInspectTheme = { inspectingTheme = it },
                    onPostClipFromGallery = onOpenGalleryForClip,
                    onViewClip = { viewingClip = it },
                    onToggleLikeClip = onToggleLikeClip,
                    onDeleteClip = onDeleteClip,
                    onNewPostClick = { showNewPostDialog = true },
                    onInspectPost = { inspectingPost = it },
                    onToggleLikePost = onToggleLikePost,
                    onDeletePost = onDeletePost
                )
            }
        }
    }

    inspectingTheme?.let { theme ->
        val liveTheme = workshopThemes.firstOrNull { it.id == theme.id } ?: theme
        ThemeDetailSheet(
            theme = liveTheme,
            currentUserUid = cloudUser?.uid,
            isLiked = liveTheme.id in likedThemeIds,
            isFavorited = liveTheme.id in favoriteThemeIds,
            notation = notation,
            onDismiss = { inspectingTheme = null },
            onDownloadAndActivate = {
                onDownloadWorkshopTheme(liveTheme)
                inspectingTheme = null
            },
            onToggleLike = { onToggleLikeTheme(liveTheme) },
            onToggleFavorite = { onToggleFavoriteTheme(liveTheme) },
            onDeletePublished = {
                onDeletePublishedTheme(liveTheme.id)
                inspectingTheme = null
            },
            onOpenAuthorProfile = { uid ->
                inspectingTheme = null
                onOpenPublicProfile(uid)
            },
            onLoadComments = { cb -> onLoadThemeComments(liveTheme.id, cb) },
            onAddComment = { txt, cb -> onAddThemeComment(liveTheme.id, txt, cb) },
            onDeleteComment = { cid, cb -> onDeleteThemeComment(liveTheme.id, cid, cb) }
        )
    }

    inspectingPost?.let { post ->
        val livePost = communityPosts.firstOrNull { it.id == post.id } ?: post
        PostCommentsSheet(
            post = livePost,
            currentUserUid = cloudUser?.uid,
            isLiked = livePost.id in likedPostIds,
            onDismiss = { inspectingPost = null },
            onToggleLike = { onToggleLikePost(livePost) },
            onOpenAuthorProfile = { uid ->
                inspectingPost = null
                onOpenPublicProfile(uid)
            },
            onLoadComments = { cb -> onLoadPostComments(livePost.id, cb) },
            onAddComment = { txt, cb -> onAddPostComment(livePost.id, txt, cb) },
            onDeleteComment = { cid, cb -> onDeletePostComment(livePost.id, cid, cb) }
        )
    }

    viewingClip?.let { clip ->
        ClipPlayerDialog(
            clip = clip,
            isOwner = clip.ownerId == cloudUser?.uid,
            isLiked = clip.id in likedClipIds,
            onToggleLike = { onToggleLikeClip(clip) },
            onDelete = {
                onDeleteClip(clip.id)
                viewingClip = null
            },
            onDismiss = { viewingClip = null }
        )
    }

    if (showNewPostDialog) {
        NewDiscussionPostDialog(
            onDismiss = { showNewPostDialog = false },
            onPublish = { topic, title, body, imgUri ->
                onCreatePost(topic, title, body, imgUri)
                showNewPostDialog = false
            }
        )
    }

    if (showEditProfileDialog && myProfile != null) {
        EditProfileDialog(
            profile = myProfile,
            onDismiss = { showEditProfileDialog = false },
            onSaveText = { username, bio ->
                onSaveMyProfile(username, bio)
                showEditProfileDialog = false
            },
            onPickAvatar = onUploadMyAvatar,
            onRemoveAvatar = onRemoveMyAvatar
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    currentUserUid: String?,
    profile: CommunityUserProfile?,
    themes: List<ButtonThemePreset>,
    clips: List<ProfileClipPost>,
    posts: List<CommunityTopicPost>,
    likedThemeIds: Set<String>,
    favoriteThemeIds: Set<String>,
    likedClipIds: Set<String>,
    likedPostIds: Set<String>,
    isLoading: Boolean,
    notation: ControllerNotation,
    onLoadProfile: (String) -> Unit,
    onDownloadWorkshopTheme: (ButtonThemePreset) -> Unit,
    onToggleLikeTheme: (ButtonThemePreset) -> Unit,
    onToggleFavoriteTheme: (ButtonThemePreset) -> Unit,
    onDeletePublishedTheme: (String) -> Unit,
    onLoadThemeComments: (String, (List<ThemeComment>) -> Unit) -> Unit,
    onAddThemeComment: (String, String, (ThemeComment?) -> Unit) -> Unit,
    onDeleteThemeComment: (String, String, () -> Unit) -> Unit,
    onToggleLikeClip: (ProfileClipPost) -> Unit,
    onDeleteClip: (String) -> Unit,
    onToggleLikePost: (CommunityTopicPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onLoadPostComments: (String, (List<PostComment>) -> Unit) -> Unit,
    onAddPostComment: (String, String, (PostComment?) -> Unit) -> Unit,
    onDeletePostComment: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var inspectingTheme by remember { mutableStateOf<ButtonThemePreset?>(null) }
    var inspectingPost by remember { mutableStateOf<CommunityTopicPost?>(null) }
    var viewingClip by remember { mutableStateOf<ProfileClipPost?>(null) }

    LaunchedEffect(userId) {
        onLoadProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = profile?.username ?: "Creator Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            ProfileContentBody(
                isOwnProfile = currentUserUid != null && currentUserUid == userId,
                currentUserUid = currentUserUid,
                profile = profile,
                localDraftThemes = emptyList(),
                publishedThemes = themes,
                clips = clips,
                posts = posts,
                likedClipIds = likedClipIds,
                likedPostIds = likedPostIds,
                isLoading = isLoading,
                notation = notation,
                onSignIn = {},
                onEditProfileClick = {},
                onCreateTheme = {},
                onEditDraftTheme = {},
                onInspectTheme = { inspectingTheme = it },
                onPostClipFromGallery = {},
                onViewClip = { viewingClip = it },
                onToggleLikeClip = onToggleLikeClip,
                onDeleteClip = onDeleteClip,
                onNewPostClick = {},
                onInspectPost = { inspectingPost = it },
                onToggleLikePost = onToggleLikePost,
                onDeletePost = onDeletePost
            )
        }
    }

    inspectingTheme?.let { theme ->
        ThemeDetailSheet(
            theme = theme,
            currentUserUid = currentUserUid,
            isLiked = theme.id in likedThemeIds,
            isFavorited = theme.id in favoriteThemeIds,
            notation = notation,
            onDismiss = { inspectingTheme = null },
            onDownloadAndActivate = {
                onDownloadWorkshopTheme(theme)
                inspectingTheme = null
            },
            onToggleLike = { onToggleLikeTheme(theme) },
            onToggleFavorite = { onToggleFavoriteTheme(theme) },
            onDeletePublished = {
                onDeletePublishedTheme(theme.id)
                inspectingTheme = null
            },
            onOpenAuthorProfile = {},
            onLoadComments = { cb -> onLoadThemeComments(theme.id, cb) },
            onAddComment = { txt, cb -> onAddThemeComment(theme.id, txt, cb) },
            onDeleteComment = { cid, cb -> onDeleteThemeComment(theme.id, cid, cb) }
        )
    }

    inspectingPost?.let { post ->
        PostCommentsSheet(
            post = post,
            currentUserUid = currentUserUid,
            isLiked = post.id in likedPostIds,
            onDismiss = { inspectingPost = null },
            onToggleLike = { onToggleLikePost(post) },
            onOpenAuthorProfile = {},
            onLoadComments = { cb -> onLoadPostComments(post.id, cb) },
            onAddComment = { txt, cb -> onAddPostComment(post.id, txt, cb) },
            onDeleteComment = { cid, cb -> onDeletePostComment(post.id, cid, cb) }
        )
    }

    viewingClip?.let { clip ->
        ClipPlayerDialog(
            clip = clip,
            isOwner = clip.ownerId == currentUserUid,
            isLiked = clip.id in likedClipIds,
            onToggleLike = { onToggleLikeClip(clip) },
            onDelete = {
                onDeleteClip(clip.id)
                viewingClip = null
            },
            onDismiss = { viewingClip = null }
        )
    }
}

@Composable
private fun WorkshopTabContent(
    cloudUser: CloudUserInfo?,
    localThemes: List<ButtonThemePreset>,
    activeThemeId: String?,
    workshopThemes: List<ButtonThemePreset>,
    likedThemeIds: Set<String>,
    favoriteThemeIds: Set<String>,
    isLoading: Boolean,
    notation: ControllerNotation,
    onSignIn: () -> Unit,
    onCreateTheme: () -> Unit,
    onEditTheme: (String) -> Unit,
    onActivateTheme: (String?) -> Unit,
    onDeleteLocalTheme: (String) -> Unit,
    onInspectTheme: (ButtonThemePreset) -> Unit,
    onToggleLikeTheme: (ButtonThemePreset) -> Unit,
    onToggleFavoriteTheme: (ButtonThemePreset) -> Unit,
    onOpenAuthorProfile: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredWorkshop = remember(workshopThemes, search, selectedCategory, favoriteThemeIds) {
        workshopThemes.filter { t ->
            val catOk = when (selectedCategory) {
                "All" -> true
                "Favorites" -> t.id in favoriteThemeIds
                else -> t.category.equals(selectedCategory, ignoreCase = true)
            }
            val q = search.trim().lowercase()
            val qOk = q.isEmpty() ||
                t.name.lowercase().contains(q) ||
                t.gameTag.lowercase().contains(q) ||
                t.authorName.lowercase().contains(q)
            catOk && qOk
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create Theme CTA Banner
        item(key = "create_banner") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Button Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Style every button with custom icons, strokes & backgrounds. Draft & test first, then upload to the Workshop!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onCreateTheme,
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("workshop_create_theme_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Local Drafts & Installed Themes strip
        item(key = "local_strip") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Drafts & Installed Presets (${localThemes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeThemeId != null) {
                        TextButton(onClick = { onActivateTheme(null) }) {
                            Text("Use Default", fontSize = 12.sp)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(localThemes, key = { it.id }) { theme ->
                        val isActive = theme.id == activeThemeId
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            modifier = Modifier
                                .width(210.dp)
                                .border(
                                    width = if (isActive) 1.8.dp else 1.dp,
                                    color = if (isActive) GeForceLime else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isActive) {
                                        Surface(color = GeForceLime.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GeForceLime,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (theme.isDraft) {
                                        Surface(color = TurboAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "DRAFT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TurboAmber,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                ThemeMiniPreviewRow(theme = theme, notation = notation)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onActivateTheme(if (isActive) null else theme.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Text(if (isActive) "Active" else "Activate", fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = { onEditTheme(theme.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = TacticalCyan)
                                    }
                                    if (theme.ownerId != "builtin") {
                                        IconButton(
                                            onClick = { onDeleteLocalTheme(theme.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MacroCoral)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cloud Workshop Header + Search + Category Chips
        item(key = "workshop_controls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Community Button Theme Workshop",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search DBD, FPS, creator username...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cats = listOf("All", "Favorites") + CommunityConstants.THEME_CATEGORIES.filterNot { it == "All" }
                    items(cats) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        if (cloudUser == null) {
            item(key = "workshop_signin") {
                SignInRequiredCard(
                    title = "Sign In to Browse & Publish Workshop Themes",
                    subtitle = "Download community button themes, like, favorite, comment, and publish your own creations.",
                    onSignIn = onSignIn
                )
            }
        } else if (isLoading && workshopThemes.isEmpty()) {
            item(key = "workshop_loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredWorkshop.isEmpty()) {
            item(key = "workshop_empty") {
                EmptyStateCard(
                    title = "No Workshop Themes Yet",
                    subtitle = "Be the first creator to publish a Button Theme to the Workshop!"
                )
            }
        } else {
            items(filteredWorkshop, key = { it.id }) { theme ->
                WorkshopThemeCard(
                    theme = theme,
                    isLiked = theme.id in likedThemeIds,
                    isFavorited = theme.id in favoriteThemeIds,
                    notation = notation,
                    onClick = { onInspectTheme(theme) },
                    onToggleLike = { onToggleLikeTheme(theme) },
                    onToggleFavorite = { onToggleFavoriteTheme(theme) },
                    onAuthorClick = { if (theme.ownerId.isNotBlank()) onOpenAuthorProfile(theme.ownerId) }
                )
            }
        }
    }
}

@Composable
private fun ThemeMiniPreviewRow(
    theme: ButtonThemePreset,
    notation: ControllerNotation
) {
    val sampleIds = remember(theme.id, theme.buttonStyles.keys) {
        val preferred = theme.buttonStyles.keys.take(3)
        val fallback = listOf("key_space", "mouse_left", "btn_a")
        (preferred + fallback).distinct().take(3)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0F18))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        sampleIds.forEach { bid ->
            val binding = remember(bid) { CommandCatalog.findById(bid) }
            val ctrl = remember(theme, binding) {
                LayoutStudioHelper.createControlFromBinding(binding, theme)
                    .copy(sizeDp = 46f, widthScale = 1f, heightScale = 1f)
            }
            VirtualControlCanvasItem(
                control = ctrl,
                notation = notation,
                globalOpacity = 1f,
                vibrationMode = VibrationMode.NONE,
                isEditMode = true,
                isSelected = false,
                isPressed = false,
                isToggled = false,
                onSelect = {},
                onDragDelta = { _, _ -> }
            )
        }
    }
}

@Composable
private fun WorkshopThemeCard(
    theme: ButtonThemePreset,
    isLiked: Boolean,
    isFavorited: Boolean,
    notation: ControllerNotation,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAuthorClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("workshop_theme_card_${theme.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommunityAvatar(
                    avatarBase64 = theme.authorAvatarBase64,
                    username = theme.authorName,
                    size = 38.dp,
                    onClick = onAuthorClick
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "by ${theme.authorName} • ${theme.gameTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalCyan,
                        modifier = Modifier.clickable(onClick = onAuthorClick)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = theme.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            ThemeMiniPreviewRow(theme = theme, notation = notation)

            if (theme.description.isNotBlank()) {
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleLike)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MacroCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${theme.likesCount}", fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleFavorite)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) TurboAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${theme.favoritesCount}", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("${theme.commentsCount}", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = GeForceLime)
                        Spacer(Modifier.width(3.dp))
                        Text("${theme.downloadsCount}", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "Open →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DiscussionsTabContent(
    cloudUser: CloudUserInfo?,
    posts: List<CommunityTopicPost>,
    likedPostIds: Set<String>,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onNewPostClick: () -> Unit,
    onInspectPost: (CommunityTopicPost) -> Unit,
    onToggleLikePost: (CommunityTopicPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenAuthorProfile: (String) -> Unit
) {
    var selectedTopic by remember { mutableStateOf("All") }
    val filteredPosts = remember(posts, selectedTopic) {
        if (selectedTopic == "All") posts else posts.filter { it.topic.equals(selectedTopic, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "topics_header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Topics & Conversations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { if (cloudUser == null) onSignIn() else onNewPostClick() },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("discussions_new_post_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Post")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CommunityConstants.DISCUSSION_TOPICS) { topic ->
                        FilterChip(
                            selected = selectedTopic == topic,
                            onClick = { selectedTopic = topic },
                            label = { Text(topic) }
                        )
                    }
                }
            }
        }

        if (cloudUser == null) {
            item(key = "posts_signin") {
                SignInRequiredCard(
                    title = "Sign In to Join Discussions",
                    subtitle = "Start topics, share layouts & DBD tips, and reply to other XenosPad players.",
                    onSignIn = onSignIn
                )
            }
        } else if (isLoading && posts.isEmpty()) {
            item(key = "posts_loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredPosts.isEmpty()) {
            item(key = "posts_empty") {
                EmptyStateCard(
                    title = "No Discussions Yet",
                    subtitle = "Start a conversation about button themes, game filters, or your favorite setups!"
                )
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                DiscussionPostCard(
                    post = post,
                    isOwner = post.ownerId == cloudUser.uid,
                    isLiked = post.id in likedPostIds,
                    onClick = { onInspectPost(post) },
                    onToggleLike = { onToggleLikePost(post) },
                    onDelete = { onDeletePost(post.id) },
                    onAuthorClick = { if (post.ownerId.isNotBlank()) onOpenAuthorProfile(post.ownerId) }
                )
            }
        }
    }
}

@Composable
private fun DiscussionPostCard(
    post: CommunityTopicPost,
    isOwner: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val imgBmp = remember(post.imageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(post.imageBase64)
    }
    val dateLabel = remember(post.createdAtMs) {
        SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(post.createdAtMs))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommunityAvatar(
                    avatarBase64 = post.authorAvatarBase64,
                    username = post.authorName,
                    size = 34.dp,
                    onClick = onAuthorClick
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onAuthorClick)
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = TacticalCyan.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = post.topic,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (imgBmp != null) {
                Image(
                    bitmap = imgBmp,
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleLike)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) MacroCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${post.likesCount}", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TacticalCyan)
                        Spacer(Modifier.width(4.dp))
                        Text("${post.commentsCount} replies", fontSize = 12.sp, color = TacticalCyan)
                    }
                }

                if (isOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Post", tint = MacroCoral, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContentBody(
    isOwnProfile: Boolean,
    currentUserUid: String?,
    profile: CommunityUserProfile?,
    localDraftThemes: List<ButtonThemePreset>,
    publishedThemes: List<ButtonThemePreset>,
    clips: List<ProfileClipPost>,
    posts: List<CommunityTopicPost>,
    likedClipIds: Set<String>,
    likedPostIds: Set<String>,
    isLoading: Boolean,
    notation: ControllerNotation,
    onSignIn: () -> Unit,
    onEditProfileClick: () -> Unit,
    onCreateTheme: () -> Unit,
    onEditDraftTheme: (String) -> Unit,
    onInspectTheme: (ButtonThemePreset) -> Unit,
    onPostClipFromGallery: () -> Unit,
    onViewClip: (ProfileClipPost) -> Unit,
    onToggleLikeClip: (ProfileClipPost) -> Unit,
    onDeleteClip: (String) -> Unit,
    onNewPostClick: () -> Unit,
    onInspectPost: (CommunityTopicPost) -> Unit,
    onToggleLikePost: (CommunityTopicPost) -> Unit,
    onDeletePost: (String) -> Unit
) {
    if (isOwnProfile && currentUserUid == null) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
            SignInRequiredCard(
                title = "Create Your XenosPad Profile",
                subtitle = "Set your custom username & profile picture, post recorded clips from your Gallery, publish Button Themes, and join discussions.",
                onSignIn = onSignIn
            )
        }
        return
    }

    var profileSubTab by remember { mutableIntStateOf(0) } // 0=Clips, 1=Themes, 2=Posts

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header
        item(key = "profile_header") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CommunityAvatar(
                            avatarBase64 = profile?.avatarBase64,
                            username = profile?.username ?: "Player",
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.username ?: "XenosPad Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile?.bio?.ifBlank { "No bio set yet." } ?: "Loading profile...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isOwnProfile) {
                            OutlinedButton(
                                onClick = onEditProfileClick,
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("profile_edit_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatBadge(count = clips.size, label = "Clips")
                        ProfileStatBadge(count = publishedThemes.size, label = "Themes")
                        ProfileStatBadge(count = posts.size, label = "Posts")
                    }
                }
            }
        }

        // Sub-tabs: Clips | Themes | Posts
        item(key = "profile_subtabs") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = profileSubTab == 0,
                    onClick = { profileSubTab = 0 },
                    label = { Text("Clips (${clips.size})") },
                    modifier = Modifier.testTag("profile_subtab_clips")
                )
                FilterChip(
                    selected = profileSubTab == 1,
                    onClick = { profileSubTab = 1 },
                    label = { Text("Themes (${publishedThemes.size})") },
                    modifier = Modifier.testTag("profile_subtab_themes")
                )
                FilterChip(
                    selected = profileSubTab == 2,
                    onClick = { profileSubTab = 2 },
                    label = { Text("Posts (${posts.size})") },
                    modifier = Modifier.testTag("profile_subtab_posts")
                )
            }
        }

        if (isLoading && profile == null) {
            item(key = "prof_loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            when (profileSubTab) {
                0 -> {
                    if (isOwnProfile) {
                        item(key = "post_clip_cta") {
                            OutlinedButton(
                                onClick = onPostClipFromGallery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("profile_post_clip_from_gallery_btn")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = TacticalCyan)
                                Spacer(Modifier.width(8.dp))
                                Text("Post a Recorded Clip from Gallery")
                            }
                        }
                    }
                    if (clips.isEmpty()) {
                        item(key = "clips_empty") {
                            EmptyStateCard(
                                title = "No Posted Clips Yet",
                                subtitle = "Record gameplay with XenosPad — clips save to your Gallery first, then you can post your highlights here!"
                            )
                        }
                    } else {
                        items(clips, key = { it.id }) { clip ->
                            ProfileClipCard(
                                clip = clip,
                                isOwner = clip.ownerId == currentUserUid,
                                isLiked = clip.id in likedClipIds,
                                onClick = { onViewClip(clip) },
                                onToggleLike = { onToggleLikeClip(clip) },
                                onDelete = { onDeleteClip(clip.id) }
                            )
                        }
                    }
                }

                1 -> {
                    if (isOwnProfile) {
                        item(key = "profile_create_theme") {
                            OutlinedButton(
                                onClick = onCreateTheme,
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create New Button Theme")
                            }
                        }
                    }
                    if (isOwnProfile && localDraftThemes.any { it.isDraft }) {
                        item(key = "drafts_header") {
                            Text("Local Drafts (Not Yet Published)", style = MaterialTheme.typography.labelMedium, color = TurboAmber, fontWeight = FontWeight.Bold)
                        }
                        items(localDraftThemes.filter { it.isDraft }, key = { "draft_${it.id}" }) { draft ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(draft.name, fontWeight = FontWeight.Bold)
                                        Text("${draft.gameTag} • ${draft.buttonStyles.size} custom buttons", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(onClick = { onEditDraftTheme(draft.id) }) {
                                        Text("Edit / Publish", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    if (publishedThemes.isEmpty()) {
                        item(key = "themes_empty") {
                            EmptyStateCard(
                                title = "No Published Themes Yet",
                                subtitle = "Themes created and uploaded to the Workshop appear here."
                            )
                        }
                    } else {
                        items(publishedThemes, key = { it.id }) { theme ->
                            WorkshopThemeCard(
                                theme = theme,
                                isLiked = false,
                                isFavorited = false,
                                notation = notation,
                                onClick = { onInspectTheme(theme) },
                                onToggleLike = { onInspectTheme(theme) },
                                onToggleFavorite = { onInspectTheme(theme) },
                                onAuthorClick = {}
                            )
                        }
                    }
                }

                2 -> {
                    if (isOwnProfile) {
                        item(key = "profile_new_post") {
                            OutlinedButton(
                                onClick = onNewPostClick,
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Start a Conversation / Topic")
                            }
                        }
                    }
                    if (posts.isEmpty()) {
                        item(key = "profile_posts_empty") {
                            EmptyStateCard(
                                title = "No Conversations Yet",
                                subtitle = "Topic posts and discussions created by this user will appear here."
                            )
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            DiscussionPostCard(
                                post = post,
                                isOwner = post.ownerId == currentUserUid,
                                isLiked = post.id in likedPostIds,
                                onClick = { onInspectPost(post) },
                                onToggleLike = { onToggleLikePost(post) },
                                onDelete = { onDeletePost(post.id) },
                                onAuthorClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatBadge(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileClipCard(
    clip: ProfileClipPost,
    isOwner: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbBmp = remember(clip.thumbnailBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(clip.thumbnailBase64)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0A0F18)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBmp != null) {
                    Image(
                        bitmap = thumbBmp,
                        contentDescription = clip.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0A0F18))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Clip", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                if (clip.gameTag.isNotBlank()) {
                    Surface(
                        color = Color(0xCC0A0F18),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = clip.gameTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (clip.durationMs > 0L) {
                    Surface(
                        color = Color(0xCC0A0F18),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = CaptureBridge.formatDurationMs(clip.durationMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(clip.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (clip.caption.isNotBlank()) {
                        Text(
                            text = clip.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Clip",
                            tint = if (isLiked) MacroCoral else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${clip.likesCount}", fontSize = 12.sp)
                    if (isOwner) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Clip", tint = MacroCoral)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDetailSheet(
    theme: ButtonThemePreset,
    currentUserUid: String?,
    isLiked: Boolean,
    isFavorited: Boolean,
    notation: ControllerNotation,
    onDismiss: () -> Unit,
    onDownloadAndActivate: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeletePublished: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onLoadComments: ((List<ThemeComment>) -> Unit) -> Unit,
    onAddComment: (String, (ThemeComment?) -> Unit) -> Unit,
    onDeleteComment: (String, () -> Unit) -> Unit
) {
    var comments by remember(theme.id) { mutableStateOf<List<ThemeComment>>(emptyList()) }
    var commentDraft by remember { mutableStateOf("") }
    var loadingComments by remember(theme.id) { mutableStateOf(true) }

    LaunchedEffect(theme.id) {
        onLoadComments { loaded ->
            comments = loaded
            loadingComments = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommunityAvatar(
                    avatarBase64 = theme.authorAvatarBase64,
                    username = theme.authorName,
                    size = 44.dp,
                    onClick = { if (theme.ownerId.isNotBlank()) onOpenAuthorProfile(theme.ownerId) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(theme.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Created by ${theme.authorName} • ${theme.gameTag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TacticalCyan,
                        modifier = Modifier.clickable {
                            if (theme.ownerId.isNotBlank()) onOpenAuthorProfile(theme.ownerId)
                        }
                    )
                }
                if (currentUserUid != null && currentUserUid == theme.ownerId) {
                    IconButton(onClick = onDeletePublished) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Unpublish Theme", tint = MacroCoral)
                    }
                }
            }

            if (theme.description.isNotBlank()) {
                Text(theme.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            ThemeMiniPreviewRow(theme = theme, notation = notation)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDownloadAndActivate,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("workshop_download_activate_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download & Use", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onToggleLike,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MacroCoral else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${theme.likesCount}")
                }
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) TurboAmber else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider()

            Text("Comments (${comments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { if (it.length <= 400) commentDraft = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val txt = commentDraft.trim()
                        if (txt.isNotEmpty()) {
                            commentDraft = ""
                            onAddComment(txt) { created ->
                                if (created != null) comments = listOf(created) + comments
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (loadingComments) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (comments.isEmpty()) {
                Text("No comments yet. Leave feedback for ${theme.authorName}!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            CommunityAvatar(
                                avatarBase64 = c.authorAvatarBase64,
                                username = c.authorName,
                                size = 30.dp,
                                onClick = { if (c.authorUid.isNotBlank()) onOpenAuthorProfile(c.authorUid) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(c.text, style = MaterialTheme.typography.bodySmall)
                            }
                            if (c.authorUid == currentUserUid || theme.ownerId == currentUserUid) {
                                IconButton(
                                    onClick = {
                                        onDeleteComment(c.id) {
                                            comments = comments.filterNot { it.id == c.id }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroCoral, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCommentsSheet(
    post: CommunityTopicPost,
    currentUserUid: String?,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onLoadComments: ((List<PostComment>) -> Unit) -> Unit,
    onAddComment: (String, (PostComment?) -> Unit) -> Unit,
    onDeleteComment: (String, () -> Unit) -> Unit
) {
    var comments by remember(post.id) { mutableStateOf<List<PostComment>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var loading by remember(post.id) { mutableStateOf(true) }

    LaunchedEffect(post.id) {
        onLoadComments { loaded ->
            comments = loaded
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommunityAvatar(
                    avatarBase64 = post.authorAvatarBase64,
                    username = post.authorName,
                    size = 38.dp,
                    onClick = { if (post.ownerId.isNotBlank()) onOpenAuthorProfile(post.ownerId) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${post.authorName} • ${post.topic}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MacroCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(post.body, style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 400) draft = it },
                    placeholder = { Text("Write a reply...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val txt = draft.trim()
                        if (txt.isNotEmpty()) {
                            draft = ""
                            onAddComment(txt) { created ->
                                if (created != null) comments = comments + created
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (comments.isEmpty()) {
                Text("No replies yet — start the conversation!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            CommunityAvatar(
                                avatarBase64 = c.authorAvatarBase64,
                                username = c.authorName,
                                size = 28.dp,
                                onClick = { if (c.authorUid.isNotBlank()) onOpenAuthorProfile(c.authorUid) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(c.text, style = MaterialTheme.typography.bodySmall)
                            }
                            if (c.authorUid == currentUserUid || post.ownerId == currentUserUid) {
                                IconButton(
                                    onClick = {
                                        onDeleteComment(c.id) {
                                            comments = comments.filterNot { it.id == c.id }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroCoral, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipPlayerDialog(
    clip: ProfileClipPost,
    isOwner: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasLocalFile = remember(clip.localMediaUri) {
        !clip.localMediaUri.isNullOrBlank() && File(clip.localMediaUri).exists()
    }
    val decodedFrames = remember(clip.id, clip.storyboardFrames, clip.thumbnailBase64) {
        val list = clip.storyboardFrames.mapNotNull {
            ButtonImageCodec.decodeBase64ToImageBitmap(it)
        }
        if (list.isNotEmpty()) list else listOfNotNull(
            ButtonImageCodec.decodeBase64ToImageBitmap(clip.thumbnailBase64)
        )
    }
    var frameIndex by remember(clip.id) { mutableIntStateOf(0) }

    LaunchedEffect(hasLocalFile, decodedFrames.size) {
        if (!hasLocalFile && decodedFrames.size > 1) {
            while (true) {
                delay(650L)
                frameIndex = (frameIndex + 1) % decodedFrames.size
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF5080B10),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clip.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${clip.authorName}${if (clip.gameTag.isNotBlank()) " • ${clip.gameTag}" else ""}",
                            color = TacticalCyan,
                            fontSize = 12.sp
                        )
                    }
                    Row {
                        IconButton(onClick = onToggleLike) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) MacroCoral else Color.White
                            )
                        }
                        if (isOwner) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroCoral)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasLocalFile) {
                        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                        DisposableEffect(clip.localMediaUri) {
                            onDispose { runCatching { videoViewRef?.stopPlayback() } }
                        }
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    videoViewRef = this
                                    val mc = MediaController(ctx)
                                    mc.setAnchorView(this)
                                    setMediaController(mc)
                                    setVideoPath(clip.localMediaUri)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (decodedFrames.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = decodedFrames[frameIndex.coerceIn(0, decodedFrames.lastIndex)],
                                contentDescription = clip.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Text(
                                text = "Highlight Reel Preview (${frameIndex + 1}/${decodedFrames.size}) • ${CaptureBridge.formatDurationMs(clip.durationMs)}",
                                color = Color(0xFF9FB0C7),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text("Clip preview unavailable", color = Color.White)
                    }
                }

                if (clip.caption.isNotBlank()) {
                    Text(
                        text = clip.caption,
                        color = Color(0xFFD0DCEB),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NewDiscussionPostDialog(
    onDismiss: () -> Unit,
    onPublish: (topic: String, title: String, body: String, imageUri: Uri?) -> Unit
) {
    var topic by remember { mutableStateOf("Dead by Daylight") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedImageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Discussion Post") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CommunityConstants.DISCUSSION_TOPICS.filterNot { it == "All" }) { t ->
                        FilterChip(
                            selected = topic == t,
                            onClick = { topic = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Topic Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 1500) body = it },
                    label = { Text("Share tips, questions or showcase...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedImageUri != null) "Image Attached ✓" else "Attach Screenshot (Optional)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(topic, title, body, pickedImageUri) },
                enabled = title.trim().isNotEmpty() && body.trim().isNotEmpty()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditProfileDialog(
    profile: CommunityUserProfile,
    onDismiss: () -> Unit,
    onSaveText: (username: String, bio: String) -> Unit,
    onPickAvatar: (Uri) -> Unit,
    onRemoveAvatar: () -> Unit
) {
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickAvatar(uri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Community Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CommunityAvatar(
                    avatarBase64 = profile.avatarBase64,
                    username = username.ifBlank { "Player" },
                    size = 72.dp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload Photo")
                    }
                    if (!profile.avatarBase64.isNullOrBlank()) {
                        TextButton(onClick = onRemoveAvatar) {
                            Text("Remove", color = MacroCoral)
                        }
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 36) username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 220) bio = it },
                    label = { Text("Bio (favorite games, setup...)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveText(username, bio) },
                enabled = username.trim().isNotEmpty()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SignInRequiredCard(
    title: String,
    subtitle: String,
    onSignIn: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Sign in with Google", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
