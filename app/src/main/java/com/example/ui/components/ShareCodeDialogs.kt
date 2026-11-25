package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.CloudLayoutSyncRepository
import com.example.model.LocalNexusStrings
import com.example.ui.ShareCodeUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import kotlinx.coroutines.delay

@Composable
fun ShareCodeExportDialog(
    state: ShareCodeUiState,
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val clipboard = LocalClipboardManager.current
    val primary = MaterialTheme.colorScheme.primary
    var copied by remember(state) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = primary
            )
        },
        title = { Text(strings.shareCodeCreateTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.shareCodeCreateSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {
                    !isSignedIn -> {
                        Surface(
                            color = TacticalCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.shareCodeSignInRequired,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = onSignInClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("share_code_signin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.cloudSaveSignInButton)
                                }
                            }
                        }
                    }

                    state is ShareCodeUiState.Creating -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.4.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(strings.shareCodeGenerating, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    state is ShareCodeUiState.Created -> {
                        var remainingSec by remember(state.result.expiresAtMs) {
                            mutableLongStateOf(((state.result.expiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
                        }
                        LaunchedEffect(state.result.expiresAtMs) {
                            while (remainingSec > 0L) {
                                delay(1000L)
                                remainingSec = ((state.result.expiresAtMs - System.currentTimeMillis()) / 1000L)
                                    .coerceAtLeast(0L)
                            }
                        }
                        val minPart = remainingSec / 60L
                        val secPart = remainingSec % 60L

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, primary, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = state.result.profileName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.result.shareCode.chunked(3).joinToString(" "),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 3.sp,
                                    color = primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("generated_share_code_text")
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = TacticalCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${strings.shareCodeExpiresNote} (%d:%02d)".format(minPart, secPart),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TacticalCyan
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(state.result.shareCode))
                                copied = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("copy_share_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (copied) strings.shareCodeCopied else strings.shareCodeCopyButton)
                        }
                    }

                    state is ShareCodeUiState.Error -> {
                        ShareCodeErrorBox(state.message)
                    }

                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_share_code_dialog_button")
            ) {
                Text(strings.commonClose)
            }
        }
    )
}

@Composable
fun ShareCodeImportDialog(
    state: ShareCodeUiState,
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    onImportCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    var codeText by remember { mutableStateOf("") }
    val normalized = CloudLayoutSyncRepository.normalizeShareCode(codeText)
    val isImporting = state is ShareCodeUiState.Importing

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = primary
            )
        },
        title = { Text(strings.shareCodeImportTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = strings.shareCodeImportSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSignedIn) {
                    Surface(
                        color = TacticalCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.shareCodeSignInRequired,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = onSignInClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("import_code_signin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(strings.cloudSaveSignInButton)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { raw ->
                            codeText = CloudLayoutSyncRepository.normalizeShareCode(raw).take(10)
                        },
                        label = { Text(strings.shareCodeInputLabel) },
                        singleLine = true,
                        enabled = !isImporting,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_code_input_field")
                    )

                    if (isImporting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(strings.shareCodeImporting, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (state is ShareCodeUiState.Error) {
                        ShareCodeErrorBox(state.message)
                    }
                }
            }
        },
        confirmButton = {
            if (isSignedIn) {
                Button(
                    onClick = { onImportCode(normalized) },
                    enabled = !isImporting && CloudLayoutSyncRepository.isValidShareCode(normalized),
                    modifier = Modifier.testTag("confirm_import_share_code_button")
                ) {
                    Text(strings.shareCodeImportButton)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.commonCancel)
            }
        }
    )
}

@Composable
private fun ShareCodeErrorBox(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MacroCoral
            )
        }
    }
}
