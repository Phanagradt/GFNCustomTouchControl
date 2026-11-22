package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

class CloudProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val random = SecureRandom()

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google before accessing NexusPad Cloud.")

    suspend fun ensureUserAccount(displayName: String, photoUrl: String?): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().ifBlank { "NexusPad Player" }.take(80)
            val safePhoto = photoUrl?.trim()?.take(500)?.takeIf { it.isNotBlank() }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = buildMap<String, Any> {
                    put("displayName", safeName)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeCloudLayouts(expectedUid: String): Flow<List<ControllerProfile>> = flow {
        val uid = requireUserId()
        if (uid != expectedUid) {
            throw IllegalStateException("Active user mismatch while observing cloud layouts")
        }
        val path = "users/$uid/layouts"
        emitAll(
            db.collection("users").document(uid).collection("layouts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudUserLayout::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map(CloudLayoutMapper::toDomain)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertCloudLayout(profile: ControllerProfile): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/layouts/${profile.id}"
        return try {
            val uid = requireUserId()
            val safeId = CloudLayoutMapper.sanitizeId(profile.id)
            val safeName = profile.name.trim().ifBlank { "Untitled Layout" }.take(80)
            val safeGame = profile.gameTitle.trim().ifBlank { "GeForce NOW" }.take(100)
            val safeControls = profile.controls.take(40).map(CloudLayoutMapper::controlToMap)
            val safeOpacity = profile.globalOpacity.toDouble().coerceIn(0.2, 1.0)

            val docRef = db.collection("users").document(uid).collection("layouts").document(safeId)
            docPath = docRef.path
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "gameTitle" to safeGame,
                    "genre" to profile.genre.name,
                    "globalOpacity" to safeOpacity,
                    "gridSnap" to profile.gridSnap,
                    "active" to profile.isActive,
                    "controls" to safeControls,
                    "clientUpdatedAt" to profile.updatedAt.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to safeName,
                    "gameTitle" to safeGame,
                    "genre" to profile.genre.name,
                    "globalOpacity" to safeOpacity,
                    "gridSnap" to profile.gridSnap,
                    "active" to profile.isActive,
                    "controls" to safeControls,
                    "clientUpdatedAt" to profile.updatedAt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudLayout(layoutId: String): Result<Unit> {
        val safeId = CloudLayoutMapper.sanitizeId(layoutId)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/layouts/$safeId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("layouts").document(safeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createShareCode(profile: ControllerProfile, authorName: String): Result<SharedCodeTicket> {
        var docPath = "shared_layouts"
        return try {
            val uid = requireUserId()
            val safeAuthor = authorName.trim().ifBlank { "NexusPad Player" }.take(80)
            val safeName = profile.name.trim().ifBlank { "Shared Layout" }.take(80)
            val safeGame = profile.gameTitle.trim().ifBlank { "GeForce NOW" }.take(100)
            val safeControls = profile.controls.take(40).map(CloudLayoutMapper::controlToMap)
            val safeOpacity = profile.globalOpacity.toDouble().coerceIn(0.2, 1.0)

            var chosenCode = ""
            var expiresAtMs = 0L
            for (attempt in 0 until 5) {
                val candidate = generateShareCode()
                val ref = db.collection("shared_layouts").document(candidate)
                docPath = ref.path
                val existing = try {
                    ref.get().await().exists()
                } catch (e: FirebaseFirestoreException) {
                    // Existing expired document triggers PERMISSION_DENIED on read -> pick fresh code
                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) true else throw e
                }
                if (!existing) {
                    chosenCode = candidate
                    expiresAtMs = System.currentTimeMillis() + SHARE_TTL_MS
                    val payload = mapOf(
                        "ownerId" to uid,
                        "authorName" to safeAuthor,
                        "name" to safeName,
                        "gameTitle" to safeGame,
                        "genre" to profile.genre.name,
                        "globalOpacity" to safeOpacity,
                        "gridSnap" to profile.gridSnap,
                        "controls" to safeControls,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "expiresAt" to Timestamp(Date(expiresAtMs))
                    )
                    ref.set(payload).await()
                    break
                }
            }
            if (chosenCode.isEmpty()) {
                throw IllegalStateException("Could not allocate a fresh share code. Please try again.")
            }
            Result.success(
                SharedCodeTicket(
                    code = chosenCode,
                    profileName = safeName,
                    expiresAtMs = expiresAtMs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun redeemShareCode(rawCode: String): Result<RedeemedSharedLayout> {
        val code = CloudLayoutMapper.sanitizeShareCode(rawCode)
        if (!CloudLayoutMapper.isValidShareCodeFormat(code)) {
            return Result.failure(IllegalArgumentException("Enter a valid 6+ character share code (letters & numbers)."))
        }
        val docRef = db.collection("shared_layouts").document(code)
        val docPath = docRef.path
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Share code \"$code\" was not found."))
            }
            val shared = snapshot.toObject(
                CloudSharedLayout::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Invalid shared layout format.")

            val expiresMs = shared.expiresAt?.toDate()?.time ?: 0L
            if (expiresMs > 0L && expiresMs <= System.currentTimeMillis()) {
                return Result.failure(IllegalStateException("This share code expired after 5 minutes. Ask your friend for a new code."))
            }

            val genre = enumValues<GameGenre>().firstOrNull { it.name == shared.genre } ?: GameGenre.CUSTOM
            val controls = shared.controls.mapNotNull(CloudLayoutMapper::mapToControl).map { ctrl ->
                ctrl.copy(id = UUID.randomUUID().toString())
            }
            val importedProfile = ControllerProfile(
                id = UUID.randomUUID().toString(),
                name = shared.name.ifBlank { "Shared Layout" },
                gameTitle = shared.gameTitle.ifBlank { "GeForce NOW" },
                genre = genre,
                controls = controls,
                globalOpacity = shared.globalOpacity.toFloat().coerceIn(0.2f, 1.0f),
                gridSnap = shared.gridSnap,
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(
                RedeemedSharedLayout(
                    code = code,
                    authorName = shared.authorName.ifBlank { "Friend" },
                    profile = importedProfile
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.GET, docPath)
            val msg = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Share code \"$code\" has expired (5-minute limit reached). Ask your friend to generate a new code."
            } else {
                e.localizedMessage ?: "Failed to redeem share code"
            }
            Result.failure(IllegalStateException(msg, e))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    private fun generateShareCode(): String {
        // Avoid ambiguous 0/O and 1/I characters for easy voice/chat sharing
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }

    companion object {
        const val SHARE_TTL_MS = 5L * 60L * 1000L
    }
}
