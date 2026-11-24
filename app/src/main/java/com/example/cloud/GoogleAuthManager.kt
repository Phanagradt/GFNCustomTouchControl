package com.example.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CloudUserInfo(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?
) {
    companion object {
        fun fromFirebaseUser(user: FirebaseUser): CloudUserInfo = CloudUserInfo(
            uid = user.uid,
            displayName = user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotEmpty() }
                ?: "NexusPad Player",
            email = user.email,
            photoUrl = user.photoUrl?.toString()
        )
    }
}

sealed interface CloudAuthResult {
    data class Success(val user: CloudUserInfo) : CloudAuthResult
    data object Cancelled : CloudAuthResult
    data object NoSavedCredential : CloudAuthResult
    data class Error(val message: String) : CloudAuthResult
}

object GoogleAuthManager {

    private const val TAG = "Auth"

    fun currentUser(): CloudUserInfo? =
        runCatching { Firebase.auth.currentUser }.getOrNull()?.let(CloudUserInfo::fromFirebaseUser)

    /**
     * Non-blocking silent sign-in for returning users who already authorized NexusPad.
     * Uses a single GetGoogleIdOption per GetCredentialRequest for Android 16 (API 36) compatibility.
     */
    suspend fun attemptAutoSignIn(activity: Activity): CloudAuthResult {
        val existing = currentUser()
        if (existing != null) return CloudAuthResult.Success(existing)

        val webClientId = resolveWebClientId(activity)
            ?: return CloudAuthResult.NoSavedCredential

        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            exchangeGoogleCredentialForFirebase(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            CloudAuthResult.NoSavedCredential
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent auto sign-in cancelled", e)
            CloudAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Silent auto sign-in unavailable: ${e.message}")
            CloudAuthResult.NoSavedCredential
        } catch (e: Exception) {
            Log.w(TAG, "Silent auto sign-in skipped", e)
            CloudAuthResult.NoSavedCredential
        }
    }

    /**
     * Explicit interactive Sign Up / Log In with Google via Jetpack Credential Manager.
     * Uses a single GetSignInWithGoogleOption per GetCredentialRequest.
     */
    suspend fun signInWithGoogle(activity: Activity): CloudAuthResult {
        val webClientId = resolveWebClientId(activity)
            ?: return CloudAuthResult.Error(
                "Google Sign-In is not configured for this build (missing Web Client ID)."
            )

        return try {
            val credentialManager = CredentialManager.create(activity)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            exchangeGoogleCredentialForFirebase(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In", e)
            CloudAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google account available for sign-in", e)
            CloudAuthResult.Error(
                "No Google account found on this device. Add a Google account in Android Settings and try again."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In credential error", e)
            val raw = e.errorMessage?.toString() ?: e.localizedMessage.orEmpty()
            val friendly = if (raw.contains("28436") || raw.contains("DEVELOPER_ERROR") || raw.contains("10")) {
                "Google Sign-In failed: APK signing certificate does not match the registered Firebase SHA-1."
            } else {
                raw.ifBlank { "Google Sign-In failed. Please try again." }
            }
            CloudAuthResult.Error(friendly)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            CloudAuthResult.Error(e.localizedMessage ?: "Sign-In failed. Please try again.")
        }
    }

    /**
     * Non-cancellable sign-out so cancelling ViewModel coroutines never leaves an active session behind.
     */
    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            runCatching {
                CredentialManager.create(context.applicationContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { err ->
                Log.w(TAG, "CredentialManager clearCredentialState skipped", err)
            }
            runCatching {
                Firebase.auth.signOut()
            }.onFailure { err ->
                Log.e(TAG, "Firebase signOut failed", err)
            }
        }
    }

    private suspend fun exchangeGoogleCredentialForFirebase(
        credential: androidx.credentials.Credential
    ): CloudAuthResult {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return CloudAuthResult.Error("Unsupported credential type returned by Google Sign-In.")
        }
        val idToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Invalid Google ID token response", e)
            return CloudAuthResult.Error("Invalid Google ID token response.")
        }

        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
        val firebaseUser = authResult.user
            ?: return CloudAuthResult.Error("Signed in, but Firebase user session was empty.")
        return CloudAuthResult.Success(CloudUserInfo.fromFirebaseUser(firebaseUser))
    }

    private fun resolveWebClientId(context: Context): String? {
        return runCatching {
            context.getString(R.string.default_web_client_id).trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
