package com.example.cloud

import android.content.Context
import com.example.R
import com.example.model.AdminConfig
import com.example.model.ButtonStyleOverride
import com.example.model.ButtonThemePreset
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.model.CosmeticItem
import com.example.model.CosmeticItemType
import com.example.model.ItemAvailability
import com.example.model.PostComment
import com.example.model.ProfileClipPost
import com.example.model.ThemeComment
import com.example.model.VerificationIconPreset
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.UUID

@JsonClass(generateAdapter = true)
internal data class SerializedThemePayload(
    val defaultStyle: ButtonStyleOverride = ButtonStyleOverride(),
    val buttonStyles: Map<String, ButtonStyleOverride> = emptyMap()
)

class CommunityWorkshopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val themePayloadAdapter = moshi.adapter(SerializedThemePayload::class.java)

    private fun requireUid(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "Please sign in with Google to use the Workshop & Community." }
        return uid
    }

    private fun requirePrimaryAdminUid(): String {
        val user = auth.currentUser
        val uid = user?.uid
        check(!uid.isNullOrBlank()) { "Please sign in with Google." }
        check(AdminConfig.isPrimaryAdminEmail(user.email)) {
            "Restricted: Admin Panel is only accessible to ${AdminConfig.PRIMARY_ADMIN_EMAIL}."
        }
        return uid
    }

    // -------------------------------------------------------------------------
    // Public Profiles (/public_profiles/{userId})
    // -------------------------------------------------------------------------

    suspend fun getPublicProfile(userId: String): Result<CommunityUserProfile?> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeUid = sanitizeId(userId)
        val path = "$COL_PUBLIC_PROFILES/$safeUid"
        return try {
            val doc = db.collection(COL_PUBLIC_PROFILES).document(safeUid).get().await()
            if (!doc.exists()) return Result.success(null)
            val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return Result.success(null)
            Result.success(decodePublicProfile(safeUid, data))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun upsertPublicProfile(
        username: String,
        bio: String,
        avatarBase64: String?,
        avatarUrl: String?
    ): Result<CommunityUserProfile> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val path = "$COL_PUBLIC_PROFILES/$uid"
        val docRef = db.collection(COL_PUBLIC_PROFILES).document(uid)
        val cleanName = username.trim().ifEmpty { "XenosPad Player" }.take(36)
        val cleanNameLower = cleanName.lowercase()
        val cleanBio = bio.trim().take(220)
        val cleanAvatarB64 = avatarBase64?.takeIf { it.isNotBlank() && it.length <= MAX_AVATAR_B64 }
        val cleanAvatarUrl = avatarUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

        return try {
            val existing = docRef.get().await()
            val existingProfile = existing.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { decodePublicProfile(uid, it) }
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any?>(
                    "username" to cleanName,
                    "usernameLower" to cleanNameLower,
                    "bio" to cleanBio,
                    "avatarBase64" to cleanAvatarB64,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatarUrl != null) {
                    updates["avatarUrl"] = cleanAvatarUrl
                }
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "username" to cleanName,
                    "usernameLower" to cleanNameLower,
                    "bio" to cleanBio,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatarB64 != null) payload["avatarBase64"] = cleanAvatarB64
                if (cleanAvatarUrl != null) payload["avatarUrl"] = cleanAvatarUrl
                docRef.set(payload).await()
            }
            Result.success(
                CommunityUserProfile(
                    uid = uid,
                    username = cleanName,
                    bio = cleanBio,
                    avatarBase64 = cleanAvatarB64,
                    avatarUrl = cleanAvatarUrl ?: existingProfile?.avatarUrl,
                    adminRole = existingProfile?.adminRole ?: false,
                    verifiedRole = existingProfile?.verifiedRole ?: false,
                    equippedFrameId = existingProfile?.equippedFrameId,
                    equippedNameTagId = existingProfile?.equippedNameTagId,
                    equippedVerificationIconId = existingProfile?.equippedVerificationIconId,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Cosmetic Shop, Wardrobe & Admin Management System
    // -------------------------------------------------------------------------

    suspend fun fetchAllCosmeticItems(): Result<List<CosmeticItem>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_COSMETIC_ITEMS)
                .limit(100)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodeCosmeticItem(doc.id, d)
            }.sortedByDescending { it.createdAtMs }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_COSMETIC_ITEMS)
            Result.failure(e)
        }
    }

    suspend fun createCosmeticItem(
        name: String,
        description: String,
        type: CosmeticItemType,
        availability: ItemAvailability,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        textColorHex: Long,
        borderWidthDp: Float,
        tagText: String,
        iconPreset: String,
        imageBase64: String?
    ): Result<CosmeticItem> {
        val adminUid = runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val itemId = "item_${type.name.lowercase()}_${UUID.randomUUID().toString().take(8)}"
        val cleanName = name.trim().ifEmpty { "Custom ${type.label}" }.take(50)
        val cleanDesc = description.trim().take(220)
        val cleanTag = tagText.trim().take(24)
        val cleanPreset = iconPreset.trim().ifEmpty { VerificationIconPreset.VERIFIED.wireId }.take(32)
        val cleanImg = imageBase64?.takeIf { it.isNotBlank() && it.length <= MAX_COSMETIC_IMG_B64 }
        val cleanBorder = borderWidthDp.coerceIn(0.5f, 8.0f).toDouble()

        return try {
            val payload = mutableMapOf<String, Any>(
                "name" to cleanName,
                "description" to cleanDesc,
                "type" to type.name,
                "availability" to availability.name,
                "primaryColorHex" to primaryColorHex,
                "secondaryColorHex" to secondaryColorHex,
                "textColorHex" to textColorHex,
                "borderWidthDp" to cleanBorder,
                "tagText" to cleanTag,
                "iconPreset" to cleanPreset,
                "createdByUid" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanImg != null) payload["imageBase64"] = cleanImg

            db.collection(COL_COSMETIC_ITEMS).document(itemId).set(payload).await()
            val nowMs = System.currentTimeMillis()
            Result.success(
                CosmeticItem(
                    id = itemId,
                    name = cleanName,
                    description = cleanDesc,
                    type = type,
                    availability = availability,
                    primaryColorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex,
                    textColorHex = textColorHex,
                    borderWidthDp = cleanBorder.toFloat(),
                    tagText = cleanTag,
                    iconPreset = cleanPreset,
                    imageBase64 = cleanImg,
                    createdByUid = adminUid,
                    createdAtMs = nowMs,
                    updatedAtMs = nowMs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "$COL_COSMETIC_ITEMS/$itemId")
            Result.failure(e)
        }
    }

    suspend fun updateCosmeticItemAvailability(
        itemId: String,
        availability: ItemAvailability
    ): Result<Unit> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(itemId)
        val path = "$COL_COSMETIC_ITEMS/$safeId"
        return try {
            db.collection(COL_COSMETIC_ITEMS).document(safeId).update(
                mapOf(
                    "availability" to availability.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCosmeticItem(itemId: String): Result<Unit> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(itemId)
        val path = "$COL_COSMETIC_ITEMS/$safeId"
        return try {
            db.collection(COL_COSMETIC_ITEMS).document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchOwnedItemIds(userId: String): Result<Set<String>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeUid = sanitizeId(userId)
        val path = "$COL_PUBLIC_PROFILES/$safeUid/owned_items"
        return try {
            val snap = db.collection(COL_PUBLIC_PROFILES)
                .document(safeUid)
                .collection("owned_items")
                .limit(100)
                .get()
                .await()
            Result.success(snap.documents.map { it.id }.toSet())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun claimFreeShopItem(itemId: String): Result<Unit> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeItemId = sanitizeId(itemId)
        val path = "$COL_PUBLIC_PROFILES/$uid/owned_items/$safeItemId"
        return try {
            db.collection(COL_PUBLIC_PROFILES)
                .document(uid)
                .collection("owned_items")
                .document(safeItemId)
                .set(
                    mapOf(
                        "itemId" to safeItemId,
                        "grantedByAdmin" to false,
                        "grantedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun equipCosmeticItem(type: CosmeticItemType, itemId: String?): Result<Unit> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val fieldKey = when (type) {
            CosmeticItemType.FRAME -> "equippedFrameId"
            CosmeticItemType.NAME_TAG -> "equippedNameTagId"
            CosmeticItemType.VERIFICATION_ICON -> "equippedVerificationIconId"
        }
        val cleanId = itemId?.let { sanitizeId(it) }
        val path = "$COL_PUBLIC_PROFILES/$uid"
        return try {
            db.collection(COL_PUBLIC_PROFILES)
                .document(uid)
                .update(
                    mapOf(
                        fieldKey to cleanId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSearchUsersByUsername(query: String): Result<List<CommunityUserProfile>> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val q = query.trim().lowercase()
        return try {
            val snap = db.collection(COL_PUBLIC_PROFILES)
                .limit(50)
                .get()
                .await()
            val all = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodePublicProfile(doc.id, d)
            }
            val filtered = if (q.isEmpty()) {
                all.sortedByDescending { it.updatedAtMs }
            } else {
                all.filter {
                    it.username.lowercase().contains(q) || it.uid.lowercase().contains(q)
                }.sortedWith(
                    compareByDescending<CommunityUserProfile> { it.username.lowercase().startsWith(q) }
                        .thenBy { it.username.lowercase() }
                )
            }
            Result.success(filtered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_PUBLIC_PROFILES)
            Result.failure(e)
        }
    }

    suspend fun adminGrantItemToUser(targetUserId: String, itemId: String): Result<Unit> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val safeUid = sanitizeId(targetUserId)
        val safeItemId = sanitizeId(itemId)
        val path = "$COL_PUBLIC_PROFILES/$safeUid/owned_items/$safeItemId"
        return try {
            db.collection(COL_PUBLIC_PROFILES)
                .document(safeUid)
                .collection("owned_items")
                .document(safeItemId)
                .set(
                    mapOf(
                        "itemId" to safeItemId,
                        "grantedByAdmin" to true,
                        "grantedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminRevokeItemFromUser(
        targetUserId: String,
        itemId: String,
        currentProfile: CommunityUserProfile?
    ): Result<Unit> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val safeUid = sanitizeId(targetUserId)
        val safeItemId = sanitizeId(itemId)
        val path = "$COL_PUBLIC_PROFILES/$safeUid/owned_items/$safeItemId"
        return try {
            db.collection(COL_PUBLIC_PROFILES)
                .document(safeUid)
                .collection("owned_items")
                .document(safeItemId)
                .delete()
                .await()

            // If user currently has this revoked item equipped, clear that slot
            val clearUpdates = mutableMapOf<String, Any?>()
            if (currentProfile?.equippedFrameId == safeItemId) clearUpdates["equippedFrameId"] = null
            if (currentProfile?.equippedNameTagId == safeItemId) clearUpdates["equippedNameTagId"] = null
            if (currentProfile?.equippedVerificationIconId == safeItemId) clearUpdates["equippedVerificationIconId"] = null
            if (clearUpdates.isNotEmpty()) {
                clearUpdates["updatedAt"] = FieldValue.serverTimestamp()
                runCatching {
                    db.collection(COL_PUBLIC_PROFILES).document(safeUid).update(clearUpdates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSetUserRoles(
        targetUserId: String,
        adminRole: Boolean,
        verifiedRole: Boolean
    ): Result<Unit> {
        runCatching { requirePrimaryAdminUid() }.getOrElse { return Result.failure(it) }
        val safeUid = sanitizeId(targetUserId)
        val path = "$COL_PUBLIC_PROFILES/$safeUid"
        return try {
            db.collection(COL_PUBLIC_PROFILES)
                .document(safeUid)
                .update(
                    mapOf(
                        "adminRole" to adminRole,
                        "verifiedRole" to verifiedRole,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Button Theme Workshop (/button_themes/{themeId})
    // -------------------------------------------------------------------------

    suspend fun fetchWorkshopThemes(): Result<List<ButtonThemePreset>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_BUTTON_THEMES)
                .whereEqualTo("published", true)
                .limit(50)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodeThemeDoc(doc.id, data)
            }.sortedByDescending { it.updatedAtMs }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_BUTTON_THEMES)
            Result.failure(e)
        }
    }

    suspend fun fetchUserPublishedThemes(ownerUid: String): Result<List<ButtonThemePreset>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_BUTTON_THEMES)
                .whereEqualTo("ownerId", sanitizeId(ownerUid))
                .whereEqualTo("published", true)
                .limit(50)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodeThemeDoc(doc.id, data)
            }.sortedByDescending { it.updatedAtMs }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_BUTTON_THEMES)
            Result.failure(e)
        }
    }

    suspend fun publishTheme(
        theme: ButtonThemePreset,
        authorProfile: CommunityUserProfile
    ): Result<ButtonThemePreset> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(theme.id.ifBlank { "theme_${UUID.randomUUID().toString().take(10)}" })
        val path = "$COL_BUTTON_THEMES/$safeId"
        val docRef = db.collection(COL_BUTTON_THEMES).document(safeId)

        val serializedJson = encodeThemePayloadWithBudget(theme.defaultStyle, theme.buttonStyles)
        if (serializedJson.length > MAX_THEME_JSON) {
            return Result.failure(IllegalArgumentException("Theme images are too large for cloud upload. Remove a few high-res icons and retry."))
        }

        val cleanName = theme.name.trim().ifEmpty { "Custom Button Theme" }.take(60)
        val cleanGame = theme.gameTag.trim().ifEmpty { "Universal" }.take(50)
        val cleanDesc = theme.description.trim().take(300)
        val cleanCat = theme.category.trim().ifEmpty { "Custom" }.take(30)
        val cleanAuthor = authorProfile.username.trim().ifEmpty { "Player" }.take(40)
        val cleanAvatar = authorProfile.avatarBase64?.takeIf { it.length <= MAX_AVATAR_B64 }

        return try {
            val existing = docRef.get().await()
            if (existing.exists() && existing.getString("ownerId") == uid) {
                val updates = mutableMapOf<String, Any?>(
                    "authorName" to cleanAuthor,
                    "authorAvatarBase64" to cleanAvatar,
                    "name" to cleanName,
                    "gameTag" to cleanGame,
                    "description" to cleanDesc,
                    "category" to cleanCat,
                    "themeJson" to serializedJson,
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val createId = if (existing.exists()) "theme_${UUID.randomUUID().toString().take(10)}" else safeId
                val targetRef = db.collection(COL_BUTTON_THEMES).document(createId)
                val payload = mutableMapOf<String, Any>(
                    "ownerId" to uid,
                    "authorName" to cleanAuthor,
                    "name" to cleanName,
                    "gameTag" to cleanGame,
                    "description" to cleanDesc,
                    "category" to cleanCat,
                    "themeJson" to serializedJson,
                    "published" to true,
                    "likesCount" to 0,
                    "favoritesCount" to 0,
                    "downloadsCount" to 0,
                    "commentsCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatar != null) payload["authorAvatarBase64"] = cleanAvatar
                targetRef.set(payload).await()
                return Result.success(
                    theme.copy(
                        id = createId,
                        name = cleanName,
                        gameTag = cleanGame,
                        description = cleanDesc,
                        category = cleanCat,
                        ownerId = uid,
                        authorName = cleanAuthor,
                        authorAvatarBase64 = cleanAvatar,
                        isDraft = false,
                        isPublished = true,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
            Result.success(
                theme.copy(
                    id = safeId,
                    name = cleanName,
                    gameTag = cleanGame,
                    description = cleanDesc,
                    category = cleanCat,
                    ownerId = uid,
                    authorName = cleanAuthor,
                    authorAvatarBase64 = cleanAvatar,
                    isDraft = false,
                    isPublished = true,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePublishedTheme(themeId: String): Result<Unit> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(themeId)
        val path = "$COL_BUTTON_THEMES/$safeId"
        return try {
            db.collection(COL_BUTTON_THEMES).document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun isThemeLiked(themeId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val safeId = sanitizeId(themeId)
        return runCatching {
            db.collection(COL_BUTTON_THEMES).document(safeId)
                .collection("likes").document(uid).get().await().exists()
        }.getOrDefault(false)
    }

    suspend fun toggleLikeTheme(themeId: String, currentlyLiked: Boolean): Result<Boolean> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(themeId)
        val themeRef = db.collection(COL_BUTTON_THEMES).document(safeId)
        val likeRef = themeRef.collection("likes").document(uid)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                runCatching { themeRef.update("likesCount", FieldValue.increment(-1L)).await() }
                Result.success(false)
            } else {
                likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                themeRef.update("likesCount", FieldValue.increment(1L)).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "$COL_BUTTON_THEMES/$safeId/likes/$uid")
            Result.failure(e)
        }
    }

    suspend fun fetchFavoriteThemeIds(): Result<Set<String>> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid/favorite_themes"
        return try {
            val snap = db.collection("users").document(uid).collection("favorite_themes").get().await()
            Result.success(snap.documents.map { it.id }.toSet())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteTheme(themeId: String, currentlyFavorited: Boolean): Result<Boolean> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(themeId)
        val favRef = db.collection("users").document(uid).collection("favorite_themes").document(safeId)
        val themeRef = db.collection(COL_BUTTON_THEMES).document(safeId)
        return try {
            if (currentlyFavorited) {
                favRef.delete().await()
                runCatching { themeRef.update("favoritesCount", FieldValue.increment(-1L)).await() }
                Result.success(false)
            } else {
                favRef.set(
                    mapOf(
                        "userId" to uid,
                        "themeId" to safeId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                runCatching { themeRef.update("favoritesCount", FieldValue.increment(1L)).await() }
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/$uid/favorite_themes/$safeId")
            Result.failure(e)
        }
    }

    suspend fun incrementThemeDownload(themeId: String) {
        if (auth.currentUser == null) return
        val safeId = sanitizeId(themeId)
        runCatching {
            db.collection(COL_BUTTON_THEMES).document(safeId)
                .update("downloadsCount", FieldValue.increment(1L))
                .await()
        }
    }

    suspend fun fetchThemeComments(themeId: String): Result<List<ThemeComment>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(themeId)
        val path = "$COL_BUTTON_THEMES/$safeId/comments"
        return try {
            val snap = db.collection(COL_BUTTON_THEMES).document(safeId)
                .collection("comments")
                .limit(50)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                ThemeComment(
                    id = doc.id,
                    themeId = safeId,
                    authorUid = (d["authorUid"] as? String).orEmpty(),
                    authorName = (d["authorName"] as? String)?.ifBlank { "Player" } ?: "Player",
                    authorAvatarBase64 = (d["authorAvatarBase64"] as? String)?.takeIf { it.isNotBlank() },
                    text = (d["text"] as? String).orEmpty(),
                    createdAtMs = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.createdAtMs }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addThemeComment(
        themeId: String,
        authorProfile: CommunityUserProfile,
        text: String
    ): Result<ThemeComment> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val cleanText = text.trim().take(400)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val safeThemeId = sanitizeId(themeId)
        val commentId = "c_${UUID.randomUUID().toString().take(10)}"
        val themeRef = db.collection(COL_BUTTON_THEMES).document(safeThemeId)
        val cleanAuthor = authorProfile.username.trim().ifEmpty { "Player" }.take(40)
        val cleanAvatar = authorProfile.avatarBase64?.takeIf { it.length <= MAX_AVATAR_B64 }

        return try {
            val payload = mutableMapOf<String, Any>(
                "authorUid" to uid,
                "authorName" to cleanAuthor,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanAvatar != null) payload["authorAvatarBase64"] = cleanAvatar
            themeRef.collection("comments").document(commentId).set(payload).await()
            runCatching { themeRef.update("commentsCount", FieldValue.increment(1L)).await() }
            Result.success(
                ThemeComment(
                    id = commentId,
                    themeId = safeThemeId,
                    authorUid = uid,
                    authorName = cleanAuthor,
                    authorAvatarBase64 = cleanAvatar,
                    text = cleanText,
                    createdAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "$COL_BUTTON_THEMES/$safeThemeId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun deleteThemeComment(themeId: String, commentId: String): Result<Unit> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeThemeId = sanitizeId(themeId)
        val safeCommentId = sanitizeId(commentId)
        val themeRef = db.collection(COL_BUTTON_THEMES).document(safeThemeId)
        return try {
            themeRef.collection("comments").document(safeCommentId).delete().await()
            runCatching { themeRef.update("commentsCount", FieldValue.increment(-1L)).await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "$COL_BUTTON_THEMES/$safeThemeId/comments/$safeCommentId")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Profile Clips (/profile_clips/{clipId})
    // -------------------------------------------------------------------------

    suspend fun fetchUserClips(ownerUid: String): Result<List<ProfileClipPost>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_PROFILE_CLIPS)
                .whereEqualTo("ownerId", sanitizeId(ownerUid))
                .limit(30)
                .get()
                .await()
            val clips = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodeClipDoc(doc.id, d)
            }.sortedByDescending { it.createdAtMs }
            Result.success(clips)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_PROFILE_CLIPS)
            Result.failure(e)
        }
    }

    suspend fun publishClipToProfile(
        authorProfile: CommunityUserProfile,
        title: String,
        gameTag: String,
        caption: String,
        durationMs: Long,
        thumbnailBase64: String?,
        storyboardFrames: List<String>,
        localMediaUri: String?
    ): Result<ProfileClipPost> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val clipId = "clip_${UUID.randomUUID().toString().take(10)}"
        val cleanTitle = title.trim().ifEmpty { "Gameplay Clip" }.take(80)
        val cleanGame = gameTag.trim().take(50)
        val cleanCaption = caption.trim().take(260)
        val cleanAuthor = authorProfile.username.trim().ifEmpty { "Player" }.take(40)
        val cleanAvatar = authorProfile.avatarBase64?.takeIf { it.length <= MAX_AVATAR_B64 }
        val cleanThumb = thumbnailBase64?.takeIf { it.length <= 95_000 }
        val cleanFrames = storyboardFrames.take(6).filter { it.length <= 55_000 }
        val cleanLocalUri = localMediaUri?.take(300)

        return try {
            val payload = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "authorName" to cleanAuthor,
                "title" to cleanTitle,
                "gameTag" to cleanGame,
                "caption" to cleanCaption,
                "durationMs" to durationMs.coerceAtLeast(0L),
                "storyboardFrames" to cleanFrames,
                "likesCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanAvatar != null) payload["authorAvatarBase64"] = cleanAvatar
            if (cleanThumb != null) payload["thumbnailBase64"] = cleanThumb
            if (cleanLocalUri != null) payload["localMediaUri"] = cleanLocalUri

            db.collection(COL_PROFILE_CLIPS).document(clipId).set(payload).await()
            Result.success(
                ProfileClipPost(
                    id = clipId,
                    ownerId = uid,
                    authorName = cleanAuthor,
                    authorAvatarBase64 = cleanAvatar,
                    title = cleanTitle,
                    gameTag = cleanGame,
                    caption = cleanCaption,
                    durationMs = durationMs.coerceAtLeast(0L),
                    thumbnailBase64 = cleanThumb,
                    storyboardFrames = cleanFrames,
                    localMediaUri = cleanLocalUri,
                    likesCount = 0,
                    createdAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "$COL_PROFILE_CLIPS/$clipId")
            Result.failure(e)
        }
    }

    suspend fun deleteProfileClip(clipId: String): Result<Unit> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(clipId)
        return try {
            db.collection(COL_PROFILE_CLIPS).document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "$COL_PROFILE_CLIPS/$safeId")
            Result.failure(e)
        }
    }

    suspend fun toggleLikeClip(clipId: String, currentlyLiked: Boolean): Result<Boolean> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(clipId)
        val clipRef = db.collection(COL_PROFILE_CLIPS).document(safeId)
        val likeRef = clipRef.collection("likes").document(uid)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                runCatching { clipRef.update("likesCount", FieldValue.increment(-1L)).await() }
                Result.success(false)
            } else {
                likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                clipRef.update("likesCount", FieldValue.increment(1L)).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "$COL_PROFILE_CLIPS/$safeId/likes/$uid")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Community Posts / Conversations & Topics (/community_posts/{postId})
    // -------------------------------------------------------------------------

    suspend fun fetchCommunityPosts(): Result<List<CommunityTopicPost>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_COMMUNITY_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .get()
                .await()
            val posts = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodePostDoc(doc.id, d)
            }
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_COMMUNITY_POSTS)
            Result.failure(e)
        }
    }

    suspend fun fetchUserPosts(ownerUid: String): Result<List<CommunityTopicPost>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        return try {
            val snap = db.collection(COL_COMMUNITY_POSTS)
                .whereEqualTo("ownerId", sanitizeId(ownerUid))
                .limit(40)
                .get()
                .await()
            val posts = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                decodePostDoc(doc.id, d)
            }.sortedByDescending { it.createdAtMs }
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, COL_COMMUNITY_POSTS)
            Result.failure(e)
        }
    }

    suspend fun createCommunityPost(
        authorProfile: CommunityUserProfile,
        topic: String,
        title: String,
        body: String,
        imageBase64: String?
    ): Result<CommunityTopicPost> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val cleanTitle = title.trim().take(100)
        val cleanBody = body.trim().take(1500)
        if (cleanTitle.isEmpty() || cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title and message are required."))
        }
        val cleanTopic = topic.trim().ifEmpty { "General" }.take(40)
        val cleanAuthor = authorProfile.username.trim().ifEmpty { "Player" }.take(40)
        val cleanAvatar = authorProfile.avatarBase64?.takeIf { it.length <= MAX_AVATAR_B64 }
        val cleanImage = imageBase64?.takeIf { it.length <= 125_000 }
        val postId = "post_${UUID.randomUUID().toString().take(10)}"

        return try {
            val payload = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "authorName" to cleanAuthor,
                "topic" to cleanTopic,
                "title" to cleanTitle,
                "body" to cleanBody,
                "likesCount" to 0,
                "commentsCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanAvatar != null) payload["authorAvatarBase64"] = cleanAvatar
            if (cleanImage != null) payload["imageBase64"] = cleanImage

            db.collection(COL_COMMUNITY_POSTS).document(postId).set(payload).await()
            Result.success(
                CommunityTopicPost(
                    id = postId,
                    ownerId = uid,
                    authorName = cleanAuthor,
                    authorAvatarBase64 = cleanAvatar,
                    topic = cleanTopic,
                    title = cleanTitle,
                    body = cleanBody,
                    imageBase64 = cleanImage,
                    likesCount = 0,
                    commentsCount = 0,
                    createdAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "$COL_COMMUNITY_POSTS/$postId")
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityPost(postId: String): Result<Unit> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(postId)
        return try {
            db.collection(COL_COMMUNITY_POSTS).document(safeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "$COL_COMMUNITY_POSTS/$safeId")
            Result.failure(e)
        }
    }

    suspend fun isPostLiked(postId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val safeId = sanitizeId(postId)
        return runCatching {
            db.collection(COL_COMMUNITY_POSTS).document(safeId)
                .collection("likes").document(uid).get().await().exists()
        }.getOrDefault(false)
    }

    suspend fun toggleLikePost(postId: String, currentlyLiked: Boolean): Result<Boolean> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(postId)
        val postRef = db.collection(COL_COMMUNITY_POSTS).document(safeId)
        val likeRef = postRef.collection("likes").document(uid)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                runCatching { postRef.update("likesCount", FieldValue.increment(-1L)).await() }
                Result.success(false)
            } else {
                likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                postRef.update("likesCount", FieldValue.increment(1L)).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "$COL_COMMUNITY_POSTS/$safeId/likes/$uid")
            Result.failure(e)
        }
    }

    suspend fun fetchPostComments(postId: String): Result<List<PostComment>> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeId(postId)
        return try {
            val snap = db.collection(COL_COMMUNITY_POSTS).document(safeId)
                .collection("comments")
                .limit(50)
                .get()
                .await()
            val list = snap.documents.mapNotNull { doc ->
                val d = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return@mapNotNull null
                PostComment(
                    id = doc.id,
                    postId = safeId,
                    authorUid = (d["authorUid"] as? String).orEmpty(),
                    authorName = (d["authorName"] as? String)?.ifBlank { "Player" } ?: "Player",
                    authorAvatarBase64 = (d["authorAvatarBase64"] as? String)?.takeIf { it.isNotBlank() },
                    text = (d["text"] as? String).orEmpty(),
                    createdAtMs = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
                )
            }.sortedBy { it.createdAtMs }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, "$COL_COMMUNITY_POSTS/$safeId/comments")
            Result.failure(e)
        }
    }

    suspend fun addPostComment(
        postId: String,
        authorProfile: CommunityUserProfile,
        text: String
    ): Result<PostComment> {
        val uid = runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val cleanText = text.trim().take(400)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Reply cannot be empty."))
        val safePostId = sanitizeId(postId)
        val commentId = "r_${UUID.randomUUID().toString().take(10)}"
        val postRef = db.collection(COL_COMMUNITY_POSTS).document(safePostId)
        val cleanAuthor = authorProfile.username.trim().ifEmpty { "Player" }.take(40)
        val cleanAvatar = authorProfile.avatarBase64?.takeIf { it.length <= MAX_AVATAR_B64 }

        return try {
            val payload = mutableMapOf<String, Any>(
                "authorUid" to uid,
                "authorName" to cleanAuthor,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (cleanAvatar != null) payload["authorAvatarBase64"] = cleanAvatar
            postRef.collection("comments").document(commentId).set(payload).await()
            runCatching { postRef.update("commentsCount", FieldValue.increment(1L)).await() }
            Result.success(
                PostComment(
                    id = commentId,
                    postId = safePostId,
                    authorUid = uid,
                    authorName = cleanAuthor,
                    authorAvatarBase64 = cleanAvatar,
                    text = cleanText,
                    createdAtMs = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "$COL_COMMUNITY_POSTS/$safePostId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun deletePostComment(postId: String, commentId: String): Result<Unit> {
        runCatching { requireUid() }.getOrElse { return Result.failure(it) }
        val safePostId = sanitizeId(postId)
        val safeCommentId = sanitizeId(commentId)
        val postRef = db.collection(COL_COMMUNITY_POSTS).document(safePostId)
        return try {
            postRef.collection("comments").document(safeCommentId).delete().await()
            runCatching { postRef.update("commentsCount", FieldValue.increment(-1L)).await() }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "$COL_COMMUNITY_POSTS/$safePostId/comments/$safeCommentId")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun encodeThemePayloadWithBudget(
        defaultStyle: ButtonStyleOverride,
        buttonStyles: Map<String, ButtonStyleOverride>
    ): String {
        val raw = themePayloadAdapter.toJson(SerializedThemePayload(defaultStyle, buttonStyles))
        if (raw.length <= MAX_THEME_JSON) return raw
        // Trim lowest-priority extra styles if total JSON exceeds 700 KB
        var budget = MAX_THEME_JSON - 60_000
        val kept = linkedMapOf<String, ButtonStyleOverride>()
        for ((k, v) in buttonStyles) {
            val est = (v.customImageBase64?.length ?: 0) +
                (v.customBackgroundBase64?.length ?: 0) +
                (v.customStrokeBase64?.length ?: 0) + 320
            if (est <= budget) {
                kept[k] = v
                budget -= est
            }
        }
        return themePayloadAdapter.toJson(SerializedThemePayload(defaultStyle, kept))
    }

    private fun decodeThemeDoc(id: String, d: Map<String, Any?>): ButtonThemePreset? {
        val name = (d["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val json = (d["themeJson"] as? String).orEmpty()
        val parsed = runCatching {
            if (json.isNotBlank()) themePayloadAdapter.fromJson(json) else null
        }.getOrNull() ?: SerializedThemePayload()
        return ButtonThemePreset(
            id = id,
            name = name,
            gameTag = (d["gameTag"] as? String)?.ifBlank { "Universal" } ?: "Universal",
            description = (d["description"] as? String).orEmpty(),
            category = (d["category"] as? String)?.ifBlank { "Custom" } ?: "Custom",
            ownerId = (d["ownerId"] as? String).orEmpty(),
            authorName = (d["authorName"] as? String)?.ifBlank { "Player" } ?: "Player",
            authorAvatarBase64 = (d["authorAvatarBase64"] as? String)?.takeIf { it.isNotBlank() },
            isDraft = false,
            isPublished = (d["published"] as? Boolean) ?: true,
            defaultStyle = parsed.defaultStyle,
            buttonStyles = parsed.buttonStyles,
            likesCount = ((d["likesCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            favoritesCount = ((d["favoritesCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            downloadsCount = ((d["downloadsCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            commentsCount = ((d["commentsCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            updatedAtMs = (d["updatedAt"] as? Timestamp)?.toDate()?.time
                ?: (d["createdAt"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis()
        )
    }

    private fun decodeClipDoc(id: String, d: Map<String, Any?>): ProfileClipPost? {
        val title = (d["title"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val rawFrames = (d["storyboardFrames"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return ProfileClipPost(
            id = id,
            ownerId = (d["ownerId"] as? String).orEmpty(),
            authorName = (d["authorName"] as? String)?.ifBlank { "Player" } ?: "Player",
            authorAvatarBase64 = (d["authorAvatarBase64"] as? String)?.takeIf { it.isNotBlank() },
            title = title,
            gameTag = (d["gameTag"] as? String).orEmpty(),
            caption = (d["caption"] as? String).orEmpty(),
            durationMs = ((d["durationMs"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
            thumbnailBase64 = (d["thumbnailBase64"] as? String)?.takeIf { it.isNotBlank() },
            storyboardFrames = rawFrames,
            localMediaUri = (d["localMediaUri"] as? String)?.takeIf { it.isNotBlank() },
            likesCount = ((d["likesCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            createdAtMs = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    private fun decodePostDoc(id: String, d: Map<String, Any?>): CommunityTopicPost? {
        val title = (d["title"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return CommunityTopicPost(
            id = id,
            ownerId = (d["ownerId"] as? String).orEmpty(),
            authorName = (d["authorName"] as? String)?.ifBlank { "Player" } ?: "Player",
            authorAvatarBase64 = (d["authorAvatarBase64"] as? String)?.takeIf { it.isNotBlank() },
            topic = (d["topic"] as? String)?.ifBlank { "General" } ?: "General",
            title = title,
            body = (d["body"] as? String).orEmpty(),
            imageBase64 = (d["imageBase64"] as? String)?.takeIf { it.isNotBlank() },
            likesCount = ((d["likesCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            commentsCount = ((d["commentsCount"] as? Number)?.toInt() ?: 0).coerceAtLeast(0),
            createdAtMs = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    private fun decodePublicProfile(uid: String, data: Map<String, Any?>): CommunityUserProfile {
        return CommunityUserProfile(
            uid = uid,
            username = (data["username"] as? String)?.ifBlank { "Player" } ?: "Player",
            bio = (data["bio"] as? String).orEmpty(),
            avatarBase64 = (data["avatarBase64"] as? String)?.takeIf { it.isNotBlank() },
            avatarUrl = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() },
            adminRole = (data["adminRole"] as? Boolean) ?: false,
            verifiedRole = (data["verifiedRole"] as? Boolean) ?: false,
            equippedFrameId = (data["equippedFrameId"] as? String)?.takeIf { it.isNotBlank() },
            equippedNameTagId = (data["equippedNameTagId"] as? String)?.takeIf { it.isNotBlank() },
            equippedVerificationIconId = (data["equippedVerificationIconId"] as? String)?.takeIf { it.isNotBlank() },
            updatedAtMs = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    private fun decodeCosmeticItem(id: String, d: Map<String, Any?>): CosmeticItem? {
        val name = (d["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return CosmeticItem(
            id = id,
            name = name,
            description = (d["description"] as? String).orEmpty(),
            type = CosmeticItemType.fromWire(d["type"] as? String),
            availability = ItemAvailability.fromWire(d["availability"] as? String),
            primaryColorHex = (d["primaryColorHex"] as? Number)?.toLong() ?: 0xFF8CE000L,
            secondaryColorHex = (d["secondaryColorHex"] as? Number)?.toLong() ?: 0xFF00E5FFL,
            textColorHex = (d["textColorHex"] as? Number)?.toLong() ?: 0xFFFFFFFFL,
            borderWidthDp = ((d["borderWidthDp"] as? Number)?.toFloat() ?: 3.0f).coerceIn(0.5f, 8.0f),
            tagText = (d["tagText"] as? String).orEmpty(),
            iconPreset = (d["iconPreset"] as? String)?.ifBlank { VerificationIconPreset.VERIFIED.wireId }
                ?: VerificationIconPreset.VERIFIED.wireId,
            imageBase64 = (d["imageBase64"] as? String)?.takeIf { it.isNotBlank() },
            createdByUid = (d["createdByUid"] as? String).orEmpty(),
            createdAtMs = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAtMs = (d["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        private const val COL_PUBLIC_PROFILES = "public_profiles"
        private const val COL_COSMETIC_ITEMS = "cosmetic_items"
        private const val COL_BUTTON_THEMES = "button_themes"
        private const val COL_PROFILE_CLIPS = "profile_clips"
        private const val COL_COMMUNITY_POSTS = "community_posts"
        private const val MAX_AVATAR_B64 = 68_000
        private const val MAX_COSMETIC_IMG_B64 = 62_000
        private const val MAX_THEME_JSON = 710_000
        private val VALID_ID = Regex("^[a-zA-Z0-9_\\-]+$")

        fun sanitizeId(raw: String): String {
            val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
            return if (cleaned.isNotEmpty() && VALID_ID.matches(cleaned)) cleaned else "id_${raw.hashCode().toUInt()}"
        }
    }
}
