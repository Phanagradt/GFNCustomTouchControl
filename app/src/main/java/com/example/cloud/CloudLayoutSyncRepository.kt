package com.example.cloud

import android.content.Context
import android.view.KeyEvent
import com.example.R
import com.example.data.CloudAppearanceSnapshot
import com.example.data.ProfileRepository
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.TouchpadMode
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

data class CloudMergePlan(
    val toSaveLocally: List<ControllerProfile>,
    val toPushToCloud: List<ControllerProfile>,
    val preferredActiveProfileId: String?
)

data class CloudSyncSummary(
    val uploadedCount: Int,
    val restoredCount: Int,
    val totalCloudCount: Int,
    val syncedAppearance: Boolean = false,
    val syncedAtMs: Long = System.currentTimeMillis()
)

data class SharedLayoutCodeResult(
    val shareCode: String,
    val profileName: String,
    val expiresAtMs: Long
)

data class FetchedSharedLayout(
    val shareCode: String,
    val authorName: String,
    val profile: ControllerProfile
)

class CloudLayoutSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthenticatedUid(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google before accessing Cloud Save." }
        return uid
    }

    /**
     * Upserts /users/{uid} in strict 1:1 parity with firestore.rules.
     * Creates with createdAt + updatedAt server timestamps, or updates only mutable profile fields.
     */
    suspend fun upsertUserAccount(user: CloudUserInfo): Result<Unit> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        check(uid == user.uid) { "Signed-in user mismatch." }
        val path = "users/$uid"
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        val safeName = user.displayName.trim().ifEmpty { "XenosPad Player" }.take(MAX_NAME_LENGTH)
        val safePhoto = user.photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_PHOTO_URL_LENGTH)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    updates["photoUrl"] = safePhoto
                }
                docRef.update(updates).await()
            } else {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    createPayload["photoUrl"] = safePhoto
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Pushes a single ControllerProfile to /users/{uid}/layouts/{safeLayoutId}.
     * Preserves immutable userId & createdAt on updates per firestore.rules.
     */
    suspend fun pushLayout(profile: ControllerProfile): Result<Unit> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeLayoutId(profile.id)
        val path = "users/$uid/layouts/$safeId"
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_LAYOUTS)
            .document(safeId)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = toUpdatePayload(profile)
                docRef.update(updatePayload).await()
            } else {
                val createPayload = toCreatePayload(uid, profile)
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Deletes /users/{uid}/layouts/{safeLayoutId}.
     */
    suspend fun deleteCloudLayout(layoutId: String): Result<Unit> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeLayoutId(layoutId)
        val path = "users/$uid/layouts/$safeId"
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_LAYOUTS)
            .document(safeId)

        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Fetches all cloud-saved layouts for the currently signed-in user.
     */
    suspend fun fetchAllCloudLayouts(): Result<List<ControllerProfile>> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid/layouts"
        return try {
            val snapshot = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(COLLECTION_LAYOUTS)
                .get()
                .await()
            val decoded = snapshot.documents.mapNotNull { doc ->
                val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                fromFirestoreDocumentMap(doc.id, data)
            }
            Result.success(decoded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Pushes Filters, Crosshair, Color Scheme, Font Preset, Floating Bubble, and Dark Theme
     * to /users/{uid}/settings/preferences in strict 1:1 parity with firestore.rules.
     */
    suspend fun pushAppearanceSettings(snapshot: CloudAppearanceSnapshot): Result<Unit> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid/settings/$DOC_PREFERENCES"
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_SETTINGS)
            .document(DOC_PREFERENCES)

        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(toSettingsUpdatePayload(snapshot)).await()
            } else {
                docRef.set(toSettingsCreatePayload(uid, snapshot)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Fetches cloud-saved Filters, Crosshair, and Theme settings for the signed-in user.
     * Returns Result.success(null) if no backup exists yet.
     */
    suspend fun fetchAppearanceSettings(): Result<CloudAppearanceSnapshot?> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid/settings/$DOC_PREFERENCES"
        val docRef = db.collection(COLLECTION_USERS)
            .document(uid)
            .collection(COLLECTION_SETTINGS)
            .document(DOC_PREFERENCES)

        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(data?.let { fromSettingsDocumentMap(it) })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Creates a temporary 5-minute 6-character share code in /shared_layouts/{shareCode}.
     * Matches firestore.rules exactly: 5-minute expiresAt under the 6-minute server ceiling.
     */
    suspend fun publishSharedLayout(
        profile: ControllerProfile,
        authorName: String
    ): Result<SharedLayoutCodeResult> {
        val uid = runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        var lastError: Exception? = null

        repeat(3) {
            val code = generateShareCode()
            val path = "$COLLECTION_SHARED_LAYOUTS/$code"
            val docRef = db.collection(COLLECTION_SHARED_LAYOUTS).document(code)
            val expiresAtMs = System.currentTimeMillis() + SHARE_CODE_LIFETIME_MS
            val payload = toSharedLayoutCreatePayload(
                ownerUid = uid,
                authorName = authorName,
                profile = profile,
                expiresAtMs = expiresAtMs
            )

            try {
                val existing = docRef.get().await()
                if (!existing.exists()) {
                    docRef.set(payload).await()
                    return Result.success(
                        SharedLayoutCodeResult(
                            shareCode = code,
                            profileName = profile.name,
                            expiresAtMs = expiresAtMs
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
                lastError = e
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Could not generate a unique share code."))
    }

    /**
     * Fetches a shared layout by its 6-character share code from /shared_layouts/{code}.
     * Returns Result.success(null) if the code does not exist or has expired.
     */
    suspend fun fetchSharedLayoutByCode(rawCode: String): Result<FetchedSharedLayout?> {
        runCatching { requireAuthenticatedUid() }.getOrElse { return Result.failure(it) }
        val code = normalizeShareCode(rawCode)
        if (!isValidShareCode(code)) {
            return Result.success(null)
        }
        val path = "$COLLECTION_SHARED_LAYOUTS/$code"
        val docRef = db.collection(COLLECTION_SHARED_LAYOUTS).document(code)

        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return Result.success(null)
            }
            val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.success(null)
            val expiresAt = (data["expiresAt"] as? Timestamp)?.toDate()?.time
            if (expiresAt != null && expiresAt <= System.currentTimeMillis()) {
                return Result.success(null)
            }
            val decoded = fromSharedLayoutDocumentMap(code, data)
            Result.success(decoded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Expired documents from other users fail rules with PERMISSION_DENIED — treat gracefully as expired/not found.
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, path)
            val msg = e.message.orEmpty()
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Real-time observation flow scoped strictly to the authenticated user's layouts subcollection.
     * Callers must only collect this when userId matches an authenticated session.
     */
    fun observeCloudLayouts(userId: String): Flow<List<ControllerProfile>> =
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_LAYOUTS)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                    fromFirestoreDocumentMap(doc.id, data)
                }
            }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_LAYOUTS = "layouts"
        private const val COLLECTION_SETTINGS = "settings"
        private const val COLLECTION_SHARED_LAYOUTS = "shared_layouts"
        private const val DOC_PREFERENCES = "preferences"
        private const val MAX_ID_LENGTH = 128
        private const val MAX_NAME_LENGTH = 80
        private const val MAX_GAME_TITLE_LENGTH = 100
        private const val MAX_PHOTO_URL_LENGTH = 500
        private const val MAX_CONTROLS_PER_LAYOUT = 40
        private const val MAX_COMBINED_IMAGE_BASE64_CHARS = 700_000
        const val SHARE_CODE_LIFETIME_MS = 5 * 60 * 1000L

        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
        private val VALID_SHARE_CODE_REGEX = Regex("^[A-Z0-9]{6,16}$")
        // Unambiguous uppercase alphanumeric characters (excludes 0/O and 1/I)
        private const val SHARE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val secureRandom = SecureRandom()

        fun generateShareCode(length: Int = 6): String {
            val safeLen = length.coerceIn(6, 12)
            return buildString(safeLen) {
                repeat(safeLen) {
                    append(SHARE_CODE_ALPHABET[secureRandom.nextInt(SHARE_CODE_ALPHABET.length)])
                }
            }
        }

        fun normalizeShareCode(raw: String): String =
            raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(16)

        fun isValidShareCode(code: String): Boolean = VALID_SHARE_CODE_REGEX.matches(code)

        /**
         * Ensures layout document IDs strictly match firestore.rules isValidId():
         * 1..128 characters matching ^[a-zA-Z0-9_\-]+$
         */
        fun sanitizeLayoutId(rawId: String): String {
            val cleaned = rawId.trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(MAX_ID_LENGTH)
            return if (cleaned.isNotEmpty() && VALID_ID_REGEX.matches(cleaned)) {
                cleaned
            } else {
                "layout_${rawId.hashCode().toUInt()}"
            }
        }

        /**
         * Exact create payload matching firestore.rules /users/{userId}/layouts/{layoutId} allow create.
         * Never includes an "id" key inside the document body.
         */
        fun toCreatePayload(uid: String, profile: ControllerProfile): Map<String, Any> = mapOf(
            "userId" to uid,
            "name" to profile.name.trim().ifEmpty { "Custom Layout" }.take(MAX_NAME_LENGTH),
            "gameTitle" to profile.gameTitle.trim().ifEmpty { "Cloud Game" }.take(MAX_GAME_TITLE_LENGTH),
            "genre" to profile.genre.name.take(32),
            "globalOpacity" to profile.globalOpacity.toDouble().coerceIn(0.2, 1.0),
            "gridSnap" to profile.gridSnap,
            "active" to profile.isActive,
            "controls" to serializeControls(profile.controls),
            "clientUpdatedAt" to profile.updatedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        /**
         * Exact update payload matching firestore.rules allow update affectedKeys whitelist.
         * Immutable userId and createdAt are intentionally omitted.
         */
        fun toUpdatePayload(profile: ControllerProfile): Map<String, Any> = mapOf(
            "name" to profile.name.trim().ifEmpty { "Custom Layout" }.take(MAX_NAME_LENGTH),
            "gameTitle" to profile.gameTitle.trim().ifEmpty { "Cloud Game" }.take(MAX_GAME_TITLE_LENGTH),
            "genre" to profile.genre.name.take(32),
            "globalOpacity" to profile.globalOpacity.toDouble().coerceIn(0.2, 1.0),
            "gridSnap" to profile.gridSnap,
            "active" to profile.isActive,
            "controls" to serializeControls(profile.controls),
            "clientUpdatedAt" to profile.updatedAt.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        /**
         * Exact create payload matching firestore.rules /users/{userId}/settings/{settingId} allow create.
         */
        fun toSettingsCreatePayload(uid: String, snapshot: CloudAppearanceSnapshot): Map<String, Any> = mapOf(
            "userId" to uid,
            "gameFilterJson" to snapshot.gameFilterJson.take(160_000),
            "crosshairJson" to snapshot.crosshairJson.take(160_000),
            "colorSchemeJson" to snapshot.colorSchemeJson.take(8_000),
            "fontConfigJson" to snapshot.fontConfigJson.take(4_000),
            "floatingBubbleJson" to snapshot.floatingBubbleJson.take(4_000),
            "darkTheme" to snapshot.darkTheme,
            "clientUpdatedAt" to snapshot.clientUpdatedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        /**
         * Exact update payload matching firestore.rules /users/{userId}/settings/{settingId} allow update.
         */
        fun toSettingsUpdatePayload(snapshot: CloudAppearanceSnapshot): Map<String, Any> = mapOf(
            "gameFilterJson" to snapshot.gameFilterJson.take(160_000),
            "crosshairJson" to snapshot.crosshairJson.take(160_000),
            "colorSchemeJson" to snapshot.colorSchemeJson.take(8_000),
            "fontConfigJson" to snapshot.fontConfigJson.take(4_000),
            "floatingBubbleJson" to snapshot.floatingBubbleJson.take(4_000),
            "darkTheme" to snapshot.darkTheme,
            "clientUpdatedAt" to snapshot.clientUpdatedAt.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        fun fromSettingsDocumentMap(data: Map<String, Any?>): CloudAppearanceSnapshot? {
            val gameFilterJson = data["gameFilterJson"] as? String ?: return null
            val crosshairJson = data["crosshairJson"] as? String ?: return null
            val colorSchemeJson = data["colorSchemeJson"] as? String ?: return null
            val fontConfigJson = data["fontConfigJson"] as? String ?: return null
            val floatingBubbleJson = data["floatingBubbleJson"] as? String ?: return null
            val darkTheme = (data["darkTheme"] as? Boolean) ?: true
            val clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong()?.coerceAtLeast(1L)
                ?: System.currentTimeMillis()

            return CloudAppearanceSnapshot(
                gameFilterJson = gameFilterJson,
                crosshairJson = crosshairJson,
                colorSchemeJson = colorSchemeJson,
                fontConfigJson = fontConfigJson,
                floatingBubbleJson = floatingBubbleJson,
                darkTheme = darkTheme,
                clientUpdatedAt = clientUpdatedAt
            )
        }

        /**
         * Exact create payload matching firestore.rules /shared_layouts/{shareCode} allow create.
         * Never includes an "id" key inside the document body.
         */
        fun toSharedLayoutCreatePayload(
            ownerUid: String,
            authorName: String,
            profile: ControllerProfile,
            expiresAtMs: Long = System.currentTimeMillis() + SHARE_CODE_LIFETIME_MS
        ): Map<String, Any> = mapOf(
            "ownerId" to ownerUid,
            "authorName" to authorName.trim().ifEmpty { "XenosPad Player" }.take(MAX_NAME_LENGTH),
            "name" to profile.name.trim().ifEmpty { "Shared Layout" }.take(MAX_NAME_LENGTH),
            "gameTitle" to profile.gameTitle.trim().ifEmpty { "Cloud Game" }.take(MAX_GAME_TITLE_LENGTH),
            "genre" to profile.genre.name.take(32),
            "globalOpacity" to profile.globalOpacity.toDouble().coerceIn(0.2, 1.0),
            "gridSnap" to profile.gridSnap,
            "controls" to serializeControls(profile.controls),
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to Timestamp(Date(expiresAtMs))
        )

        fun fromSharedLayoutDocumentMap(
            shareCode: String,
            data: Map<String, Any?>
        ): FetchedSharedLayout? {
            val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val authorName = (data["authorName"] as? String)?.trim()?.ifEmpty { "XenosPad Player" }
                ?: "XenosPad Player"
            val gameTitle = (data["gameTitle"] as? String)?.trim()?.ifEmpty { "Cloud Game" }
                ?: "Cloud Game"
            val genreName = data["genre"] as? String
            val genre = GameGenre.entries.firstOrNull { it.name == genreName } ?: GameGenre.CUSTOM
            val globalOpacity = ((data["globalOpacity"] as? Number)?.toFloat() ?: 0.85f).coerceIn(0.2f, 1.0f)
            val gridSnap = (data["gridSnap"] as? Boolean) ?: true
            val rawControls = data["controls"] as? List<*> ?: emptyList<Any?>()
            val controls = rawControls.mapNotNull { entry ->
                (entry as? Map<*, *>)?.let { mapToControl(it) }
            }.map { it.copy(id = "ctrl_${UUID.randomUUID().toString().take(8)}") }

            val profile = ControllerProfile(
                id = "shared_${UUID.randomUUID().toString().take(8)}",
                name = name,
                gameTitle = gameTitle,
                genre = genre,
                controls = controls,
                globalOpacity = globalOpacity,
                gridSnap = gridSnap,
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            return FetchedSharedLayout(
                shareCode = shareCode,
                authorName = authorName,
                profile = profile
            )
        }

        fun serializeControls(controls: List<VirtualControl>): List<Map<String, Any>> {
            var remainingImageBudget = MAX_COMBINED_IMAGE_BASE64_CHARS
            fun budgetImage(raw: String?): String? {
                val clean = raw?.takeIf { it.isNotBlank() } ?: return null
                return if (clean.length <= remainingImageBudget) {
                    remainingImageBudget -= clean.length
                    clean
                } else {
                    null
                }
            }
            return controls.take(MAX_CONTROLS_PER_LAYOUT).map { ctrl ->
                controlToMap(
                    ctrl = ctrl,
                    customImageOverride = budgetImage(ctrl.customImageBase64),
                    customBackgroundOverride = budgetImage(ctrl.customBackgroundBase64),
                    customStrokeOverride = budgetImage(ctrl.customStrokeBase64)
                )
            }
        }

        fun controlToMap(
            ctrl: VirtualControl,
            customImageOverride: String? = ctrl.customImageBase64,
            customBackgroundOverride: String? = ctrl.customBackgroundBase64,
            customStrokeOverride: String? = ctrl.customStrokeBase64
        ): Map<String, Any> {
            val bindingMap = mapOf<String, Any>(
                "id" to ctrl.binding.id,
                "domain" to ctrl.binding.domain.name,
                "primaryKeyCode" to ctrl.binding.primaryKeyCode,
                "secondaryKeyCode" to ctrl.binding.secondaryKeyCode,
                "modifierMetaState" to ctrl.binding.modifierMetaState,
                "codeLabel" to ctrl.binding.codeLabel,
                "commandTitle" to ctrl.binding.commandTitle,
                "axisPair" to ctrl.binding.axisPair.name
            )
            val result = mutableMapOf<String, Any>(
                "id" to ctrl.id,
                "label" to ctrl.label,
                "controlType" to ctrl.controlType.name,
                "shape" to ctrl.shape.name,
                "xPercent" to ctrl.xPercent.toDouble(),
                "yPercent" to ctrl.yPercent.toDouble(),
                "sizeDp" to ctrl.sizeDp.toDouble(),
                "opacity" to ctrl.opacity.toDouble(),
                "accent" to ctrl.accent.name,
                "behavior" to ctrl.behavior.name,
                "turboRateHz" to ctrl.turboRateHz,
                "deadzone" to ctrl.deadzone.toDouble(),
                "sensitivity" to ctrl.sensitivity.toDouble(),
                "hapticOnPress" to ctrl.hapticOnPress,
                "invertY" to ctrl.invertY,
                "touchpadMode" to ctrl.touchpadMode.name,
                "touchpadTapToClickEnabled" to ctrl.touchpadTapToClickEnabled,
                "touchpadTwoFingerRightClickEnabled" to ctrl.touchpadTwoFingerRightClickEnabled,
                "dragToAimEnabled" to ctrl.dragToAimEnabled,
                "dragAimMode" to ctrl.dragAimMode.name,
                "dragAimSensitivity" to ctrl.dragAimSensitivity.toDouble(),
                "dragAimInvertY" to ctrl.dragAimInvertY,
                "widthScale" to ctrl.widthScale.toDouble(),
                "heightScale" to ctrl.heightScale.toDouble(),
                "customImageScale" to ctrl.customImageScale.toDouble(),
                "customImageOpacity" to ctrl.customImageOpacity.toDouble(),
                "showBackground" to ctrl.showBackground,
                "showStroke" to ctrl.showStroke,
                "strokeWidthDp" to ctrl.strokeWidthDp.toDouble(),
                "customBackgroundOpacity" to ctrl.customBackgroundOpacity.toDouble(),
                "customStrokeOpacity" to ctrl.customStrokeOpacity.toDouble(),
                "binding" to bindingMap
            )
            ctrl.customBackgroundColorHex?.let { result["customBackgroundColorHex"] = it }
            ctrl.customStrokeColorHex?.let { result["customStrokeColorHex"] = it }
            if (!customImageOverride.isNullOrBlank()) {
                result["customImageBase64"] = customImageOverride
            }
            if (!customBackgroundOverride.isNullOrBlank()) {
                result["customBackgroundBase64"] = customBackgroundOverride
            }
            if (!customStrokeOverride.isNullOrBlank()) {
                result["customStrokeBase64"] = customStrokeOverride
            }
            return result
        }

        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreDocumentMap(documentId: String, data: Map<String, Any?>): ControllerProfile? {
            if (documentId.isBlank()) return null
            val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val gameTitle = (data["gameTitle"] as? String)?.trim()?.ifEmpty { "Cloud Game" } ?: "Cloud Game"
            val genreName = data["genre"] as? String
            val genre = GameGenre.entries.firstOrNull { it.name == genreName } ?: GameGenre.CUSTOM
            val globalOpacity = ((data["globalOpacity"] as? Number)?.toFloat() ?: 0.85f).coerceIn(0.2f, 1.0f)
            val gridSnap = (data["gridSnap"] as? Boolean) ?: true
            val isActive = (data["active"] as? Boolean) ?: false
            val clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong()?.coerceAtLeast(1L)
                ?: System.currentTimeMillis()

            val rawControls = data["controls"] as? List<*> ?: emptyList<Any?>()
            val controls = rawControls.mapNotNull { entry ->
                (entry as? Map<*, *>)?.let { mapToControl(it) }
            }

            return ControllerProfile(
                id = documentId,
                name = name,
                gameTitle = gameTitle,
                genre = genre,
                controls = controls,
                globalOpacity = globalOpacity,
                gridSnap = gridSnap,
                isActive = isActive,
                updatedAt = clientUpdatedAt
            )
        }

        fun mapToControl(map: Map<*, *>): VirtualControl? {
            val id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            val label = (map["label"] as? String) ?: "BTN"
            val controlType = ControlType.entries.firstOrNull { it.name == map["controlType"] } ?: ControlType.BUTTON
            val shape = ControlShape.entries.firstOrNull { it.name == map["shape"] } ?: ControlShape.CIRCLE
            val xPercent = ((map["xPercent"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.02f, 0.98f)
            val yPercent = ((map["yPercent"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.03f, 0.97f)
            val sizeDp = ((map["sizeDp"] as? Number)?.toFloat() ?: 64f).coerceIn(28f, 260f)
            val opacity = ((map["opacity"] as? Number)?.toFloat() ?: 0.82f).coerceIn(0.15f, 1.0f)
            val accent = ControlAccent.entries.firstOrNull { it.name == map["accent"] } ?: ControlAccent.LIME
            val behavior = TriggerBehavior.entries.firstOrNull { it.name == map["behavior"] } ?: TriggerBehavior.HOLD
            val turboRateHz = ((map["turboRateHz"] as? Number)?.toInt() ?: 10).coerceIn(2, 30)
            val deadzone = ((map["deadzone"] as? Number)?.toFloat() ?: 0.12f).coerceIn(0.0f, 0.5f)
            val sensitivity = ((map["sensitivity"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.2f, 6.0f)
            val hapticOnPress = (map["hapticOnPress"] as? Boolean) ?: true
            val invertY = (map["invertY"] as? Boolean) ?: false
            val touchpadMode = TouchpadMode.entries.firstOrNull { it.name == map["touchpadMode"] } ?: TouchpadMode.MOUSE
            val touchpadTapToClickEnabled = (map["touchpadTapToClickEnabled"] as? Boolean) ?: true
            val touchpadTwoFingerRightClickEnabled = (map["touchpadTwoFingerRightClickEnabled"] as? Boolean) ?: true
            val dragToAimEnabled = (map["dragToAimEnabled"] as? Boolean) ?: false
            val dragAimMode = TouchpadMode.entries.firstOrNull { it.name == map["dragAimMode"] } ?: TouchpadMode.MOUSE
            val dragAimSensitivity = ((map["dragAimSensitivity"] as? Number)?.toFloat() ?: 1.4f).coerceIn(0.4f, 6.0f)
            val dragAimInvertY = (map["dragAimInvertY"] as? Boolean) ?: false
            val customImageBase64 = (map["customImageBase64"] as? String)?.takeIf { it.isNotBlank() }
            val widthScale = ((map["widthScale"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.45f, 2.6f)
            val heightScale = ((map["heightScale"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.45f, 2.6f)
            val customImageScale = ((map["customImageScale"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.30f, 1.80f)
            val customImageOpacity = ((map["customImageOpacity"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.10f, 1.0f)
            val showBackground = (map["showBackground"] as? Boolean) ?: true
            val showStroke = (map["showStroke"] as? Boolean) ?: true
            val strokeWidthDp = ((map["strokeWidthDp"] as? Number)?.toFloat() ?: 1.8f).coerceIn(0.5f, 6.0f)
            val customBackgroundColorHex = (map["customBackgroundColorHex"] as? Number)?.toLong()
            val customStrokeColorHex = (map["customStrokeColorHex"] as? Number)?.toLong()
            val customBackgroundBase64 = (map["customBackgroundBase64"] as? String)?.takeIf { it.isNotBlank() }
            val customBackgroundOpacity = ((map["customBackgroundOpacity"] as? Number)?.toFloat() ?: 0.90f).coerceIn(0.10f, 1.0f)
            val customStrokeBase64 = (map["customStrokeBase64"] as? String)?.takeIf { it.isNotBlank() }
            val customStrokeOpacity = ((map["customStrokeOpacity"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.10f, 1.0f)

            val rawBinding = map["binding"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val binding = InputBinding(
                id = (rawBinding["id"] as? String)?.takeIf { it.isNotBlank() } ?: "bind_$id",
                domain = BindingDomain.entries.firstOrNull { it.name == rawBinding["domain"] }
                    ?: BindingDomain.CONTROLLER,
                primaryKeyCode = (rawBinding["primaryKeyCode"] as? Number)?.toInt() ?: KeyEvent.KEYCODE_BUTTON_A,
                secondaryKeyCode = (rawBinding["secondaryKeyCode"] as? Number)?.toInt() ?: KeyEvent.KEYCODE_UNKNOWN,
                modifierMetaState = (rawBinding["modifierMetaState"] as? Number)?.toInt() ?: 0,
                codeLabel = (rawBinding["codeLabel"] as? String)?.ifBlank { label } ?: label,
                commandTitle = (rawBinding["commandTitle"] as? String)?.ifBlank { label } ?: label,
                axisPair = AxisPair.entries.firstOrNull { it.name == rawBinding["axisPair"] } ?: AxisPair.NONE
            )

            return VirtualControl(
                id = id,
                label = label,
                controlType = controlType,
                shape = shape,
                xPercent = xPercent,
                yPercent = yPercent,
                sizeDp = sizeDp,
                opacity = opacity,
                accent = accent,
                behavior = behavior,
                turboRateHz = turboRateHz,
                deadzone = deadzone,
                sensitivity = sensitivity,
                hapticOnPress = hapticOnPress,
                invertY = invertY,
                touchpadMode = touchpadMode,
                touchpadTapToClickEnabled = touchpadTapToClickEnabled,
                touchpadTwoFingerRightClickEnabled = touchpadTwoFingerRightClickEnabled,
                dragToAimEnabled = dragToAimEnabled,
                dragAimMode = dragAimMode,
                dragAimSensitivity = dragAimSensitivity,
                dragAimInvertY = dragAimInvertY,
                customImageBase64 = customImageBase64,
                widthScale = widthScale,
                heightScale = heightScale,
                customImageScale = customImageScale,
                customImageOpacity = customImageOpacity,
                showBackground = showBackground,
                showStroke = showStroke,
                strokeWidthDp = strokeWidthDp,
                customBackgroundColorHex = customBackgroundColorHex,
                customStrokeColorHex = customStrokeColorHex,
                customBackgroundBase64 = customBackgroundBase64,
                customBackgroundOpacity = customBackgroundOpacity,
                customStrokeBase64 = customStrokeBase64,
                customStrokeOpacity = customStrokeOpacity,
                binding = binding
            )
        }

        /**
         * Pure bidirectional merge planner used on sign-in / update reinstall.
         * Protects cloud backups from being clobbered by freshly seeded starter presets.
         */
        fun planMerge(
            localProfiles: List<ControllerProfile>,
            cloudProfiles: List<ControllerProfile>,
            forceCloudWins: Boolean = false
        ): CloudMergePlan {
            val localById = localProfiles.associateBy { sanitizeLayoutId(it.id) }
            val cloudById = cloudProfiles.associateBy { sanitizeLayoutId(it.id) }

            val toSaveLocally = mutableListOf<ControllerProfile>()
            val toPushToCloud = mutableListOf<ControllerProfile>()

            for ((safeId, cloud) in cloudById) {
                val local = localById[safeId]
                val shouldRestore = forceCloudWins ||
                    local == null ||
                    cloud.updatedAt > local.updatedAt ||
                    (local.updatedAt <= ProfileRepository.STARTER_SEED_TIMESTAMP &&
                        cloud.updatedAt >= ProfileRepository.STARTER_SEED_TIMESTAMP)
                if (shouldRestore) {
                    toSaveLocally += cloud
                } else if (local != null && local.updatedAt > cloud.updatedAt) {
                    toPushToCloud += local
                }
            }

            if (!forceCloudWins) {
                for ((safeId, local) in localById) {
                    if (safeId !in cloudById) {
                        val isUntouchedSeed = local.updatedAt <= ProfileRepository.STARTER_SEED_TIMESTAMP &&
                            local.id.startsWith("preset_")
                        // If cloud is empty, back up everything (including presets).
                        // If cloud already has layouts, only push user-created or user-edited layouts.
                        if (cloudById.isEmpty() || !isUntouchedSeed) {
                            toPushToCloud += local
                        }
                    }
                }
            }

            val cloudActiveId = cloudProfiles.firstOrNull { it.isActive }?.id
            val preferredActive = if (cloudActiveId != null && (forceCloudWins || toSaveLocally.any { it.id == cloudActiveId })) {
                cloudActiveId
            } else {
                null
            }

            return CloudMergePlan(
                toSaveLocally = toSaveLocally,
                toPushToCloud = toPushToCloud,
                preferredActiveProfileId = preferredActive
            )
        }
    }
}

