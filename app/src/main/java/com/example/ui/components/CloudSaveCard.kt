package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloud.CloudUserInfo
import com.example.model.LocalNexusStrings
import com.example.ui.CloudSaveUiState
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSaveAccountCard(
    user: CloudUserInfo?,
    syncState: CloudSaveUiState,
    localProfileCount: Int,
    onSignInClick: () -> Unit,
    onBackupNowClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isBusy = syncState is CloudSaveUiState.SigningIn || syncState is CloudSaveUiState.Syncing

    val borderColor = when {
        syncState is CloudSaveUiState.Error -> MacroCoral
        user != null -> primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.large)
            .testTag("cloud_save_account_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (user != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (user != null) primary else TacticalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.cloudSaveTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (user != null) primary else TacticalCyan
                    )
                    Text(
                        text = strings.cloudSaveSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (user == null) {
                Button(
                    onClick = onSignInClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cloud_save_google_signin_button")
                ) {
                    if (syncState is CloudSaveUiState.SigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(strings.cloudSaveSigningIn)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.cloudSaveSignInButton)
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val initial = remember(user.displayName) {
                            user.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.2f))
                                .border(1.dp, primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    color = primary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = strings.cloudSaveActiveBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!user.email.isNullOrBlank()) {
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val statusText = when (syncState) {
                                is CloudSaveUiState.Syncing -> strings.cloudSaveSyncing
                                is CloudSaveUiState.Synced -> {
                                    val timeStr = formatSyncTime(syncState.syncedAtMs)
                                    "${syncState.cloudLayoutCount} ${strings.cloudSaveBackedUpSuffix} • $timeStr"
                                }
                                else -> "$localProfileCount ${strings.cloudSaveBackedUpSuffix} • ${strings.cloudSaveAutoSaveHint}"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onBackupNowClick,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cloud_save_backup_now_button")
                    ) {
                        if (syncState is CloudSaveUiState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.cloudSaveBackupNow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onRestoreClick,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cloud_save_restore_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.cloudSaveRestore,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.cloudSaveSignOutNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onSignOutClick,
                        enabled = !isBusy,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("cloud_save_signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MacroCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(strings.cloudSaveSignOut, color = MacroCoral)
                    }
                }
            }

            if (syncState is CloudSaveUiState.Error) {
                Surface(
                    color = MacroCoral.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MacroCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = syncState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MacroCoral
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSaveCompactBanner(
    user: CloudUserInfo?,
    syncState: CloudSaveUiState,
    onSignInClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val isBusy = syncState is CloudSaveUiState.SigningIn || syncState is CloudSaveUiState.Syncing

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (user != null) primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("profiles_cloud_save_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (user != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                contentDescription = null,
                tint = if (user != null) primary else TacticalCyan,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                if (user == null) {
                    Text(
                        text = strings.cloudSaveSignedOutLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.cloudSaveSignedOutHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when (syncState) {
                        is CloudSaveUiState.Syncing -> strings.cloudSaveSyncing
                        is CloudSaveUiState.Synced -> "${syncState.cloudLayoutCount} ${strings.cloudSaveBackedUpSuffix}"
                        is CloudSaveUiState.Error -> syncState.message
                        else -> strings.cloudSaveAutoSaveHint
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (syncState is CloudSaveUiState.Error) MacroCoral else primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (user == null) {
                Button(
                    onClick = onSignInClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("profiles_cloud_signin_button")
                ) {
                    if (syncState is CloudSaveUiState.SigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(strings.cloudSaveSignInShort)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSyncNowClick,
                    enabled = !isBusy,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("profiles_cloud_sync_button")
                ) {
                    if (syncState is CloudSaveUiState.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(strings.cloudSaveSyncShort)
                }
            }
        }
    }
}

private fun formatSyncTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return runCatching {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
    }.getOrDefault("")
}
