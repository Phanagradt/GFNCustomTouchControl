package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloud.CloudUserInfo
import com.example.model.AppPlatformTheme
import com.example.model.CommunityUserProfile
import com.example.ui.components.ImageCropAspect
import com.example.ui.components.ImageEditorDialog
import com.example.ui.components.PlatformThemeSelectorCard
import com.example.ui.theme.GeForceLime

/**
 * First-launch onboarding wizard:
 * Step 0: Welcome + Sign In / Sign Up with Google (or Guest)
 * Step 1: Choose Username, Bio & Profile Picture (with built-in image editor)
 * Step 2: Choose Platform UI Theme (Steam Deck, Epic Games, Xbox, Switch, PS5, or Xenos)
 */
@Composable
fun OnboardingSetupScreen(
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    selectedPlatformTheme: AppPlatformTheme,
    nintendoSwitchDarkMode: Boolean = false,
    onSignInWithGoogle: () -> Unit,
    onSelectPlatformTheme: (AppPlatformTheme) -> Unit,
    onNintendoSwitchDarkModeChange: (Boolean) -> Unit = {},
    onUploadAvatarUri: (Uri) -> Unit,
    onCompleteOnboarding: (username: String, bio: String, editedAvatarUri: Uri?) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf(myProfile?.username ?: cloudUser?.displayName ?: "") }
    var bio by remember { mutableStateOf(myProfile?.bio ?: "") }
    var pendingRawAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var editedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(cloudUser?.displayName, myProfile?.username) {
        if (username.isBlank()) {
            username = myProfile?.username?.takeIf { it.isNotBlank() }
                ?: cloudUser?.displayName?.takeIf { it.isNotBlank() }
                ?: ""
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingRawAvatarUri = uri
        }
    }

    val primary = MaterialTheme.colorScheme.primary

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress Steps Header
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_xenospad_fg),
                            contentDescription = "XenosPad",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "XenosPad Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Step ${step + 1} of 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (idx <= step) primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                }
            }

            // Center Step Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    0 -> {
                        Spacer(Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_xenospad_fg),
                            contentDescription = "XenosPad Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(2.dp, primary.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                        )
                        Text(
                            text = "Welcome to XenosPad",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sign in or create your account to sync controller layouts, publish Button Themes, post gameplay clips, and customize your profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (cloudUser != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GeForceLime,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Signed in as ${cloudUser.displayName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = cloudUser.email.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = onSignInWithGoogle,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("onboarding_google_signin_btn")
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Login / Sign Up with Google", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "You can also continue as guest and sign in anytime from Settings or Profile.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Set Up Your Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pick your username and upload a profile picture (you can crop & style it before saving).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CommunityAvatar(
                                avatarBase64 = myProfile?.avatarBase64,
                                username = username.ifBlank { "Player" },
                                size = 92.dp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("onboarding_pick_avatar_btn")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (editedAvatarUri != null || !myProfile?.avatarBase64.isNullOrBlank()) "Change & Edit Picture" else "Choose Profile Picture")
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { if (it.length <= 36) username = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. GhostHunter99") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_username_input")
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 220) bio = it },
                            label = { Text("Bio (optional)") },
                            placeholder = { Text("Favorite games, GeForce NOW setup...") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_bio_input")
                        )
                    }

                    2 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose Your App Theme",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0x33F5B731),
                                shape = RoundedCornerShape(5.dp),
                                modifier = Modifier.border(1.dp, Color(0xAAF5B731), RoundedCornerShape(5.dp))
                            ) {
                                Text(
                                    text = "BETA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF5B731),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "App Themes are in Beta (still being improved). Pick Steam Big Picture, Epic, Xbox, Switch, PS5, or Xenos Default — you can switch anytime in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        PlatformThemeSelectorCard(
                            selectedTheme = selectedPlatformTheme,
                            onSelectTheme = onSelectPlatformTheme,
                            nintendoSwitchDarkMode = nintendoSwitchDarkMode,
                            onNintendoSwitchDarkModeChange = onNintendoSwitchDarkModeChange
                        )
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("onboarding_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    TextButton(
                        onClick = { onCompleteOnboarding(username.ifBlank { "Player" }, bio, editedAvatarUri) },
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text("Skip")
                    }
                }

                Spacer(Modifier.weight(1f))

                if (step < 2) {
                    Button(
                        onClick = { step += 1 },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("onboarding_next_btn")
                    ) {
                        Text(
                            text = if (step == 0 && cloudUser == null) "Continue" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            onCompleteOnboarding(
                                username.trim().ifBlank { cloudUser?.displayName ?: "XenosPad Player" },
                                bio.trim(),
                                editedAvatarUri
                            )
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("onboarding_finish_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Launch XenosPad", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    pendingRawAvatarUri?.let { rawUri ->
        ImageEditorDialog(
            sourceUri = rawUri,
            title = "Edit Profile Picture",
            defaultCropAspect = ImageCropAspect.SQUARE,
            onCancel = { pendingRawAvatarUri = null },
            onApply = { finalUri ->
                pendingRawAvatarUri = null
                editedAvatarUri = finalUri
                if (cloudUser != null) {
                    onUploadAvatarUri(finalUri)
                }
            }
        )
    }
}
