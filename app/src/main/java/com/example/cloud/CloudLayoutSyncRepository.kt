package com.example.cloud

import android.content.Context
import android.view.KeyEvent
import com.example.R
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

data class CloudMergePlan(
    val toSaveLocally: List<ControllerProfile>,
    val toPushToCloud: List<ControllerProfile>,
    val preferredActiveProfileId: String?
)

data class CloudSyncSummary(
    val uploadedCount: Int,
    val restoredCount: Int,
    val totalCloudCount: Int,
    val syncedAtMs: Long = System.currentTimeMillis()
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
        val safeName = user.displayName.trim().ifEmpty { "NexusPad Player" }.take(MAX_NAME_LENGTH)
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
        private const val MAX_ID_LENGTH = 128
        private const val MAX_NAME_LENGTH = 80
        private const val MAX_GAME_TITLE_LENGTH = 100
        private const val MAX_PHOTO_URL_LENGTH = 500
        private const val MAX_CONTROLS_PER_LAYOUT = 40
        private const val MAX_COMBINED_IMAGE_BASE64_CHARS = 700_000

        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

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

        fun serializeControls(controls: List<VirtualControl>): List<Map<String, Any>> {
            var remainingImageBudget = MAX_COMBINED_IMAGE_BASE64_CHARS
            return controls.take(MAX_CONTROLS_PER_LAYOUT).map { ctrl ->
                val rawImage = ctrl.customImageBase64?.takeIf { it.isNotBlank() }
                val allowedImage = if (rawImage != null && rawImage.length <= remainingImageBudget) {
                    remainingImageBudget -= rawImage.length
                    rawImage
                } else {
                    null
                }
                controlToMap(ctrl, allowedImage)
            }
        }

        fun controlToMap(ctrl: VirtualControl, customImageOverride: String? = ctrl.customImageBase64): Map<String, Any> {
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
                "binding" to bindingMap
            )
            if (!customImageOverride.isNullOrBlank()) {
                result["customImageBase64"] = customImageOverride
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
            val sensitivity = ((map["sensitivity"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.2f, 3.0f)
            val hapticOnPress = (map["hapticOnPress"] as? Boolean) ?: true
            val invertY = (map["invertY"] as? Boolean) ?: false
            val touchpadMode = TouchpadMode.entries.firstOrNull { it.name == map["touchpadMode"] } ?: TouchpadMode.MOUSE
            val customImageBase64 = (map["customImageBase64"] as? String)?.takeIf { it.isNotBlank() }

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
                customImageBase64 = customImageBase64,
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
