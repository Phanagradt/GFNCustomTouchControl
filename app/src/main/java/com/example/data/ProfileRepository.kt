package com.example.data

import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.PresetProfiles
import com.example.model.VirtualControl
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProfileRepository(private val profileDao: ProfileDao) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val controlListType =
        Types.newParameterizedType(List::class.java, VirtualControl::class.java)
    private val controlListAdapter = moshi.adapter<List<VirtualControl>>(controlListType)
    private val profileAdapter = moshi.adapter(ControllerProfile::class.java)

    val profiles: Flow<List<ControllerProfile>> =
        profileDao.observeAllProfiles().map { list -> list.mapNotNull { it.toDomain() } }

    val activeProfile: Flow<ControllerProfile?> =
        profileDao.observeActiveProfile().map { it?.toDomain() }

    suspend fun ensureSeeded() {
        if (profileDao.countProfiles() == 0) {
            val starters = PresetProfiles.createStarterProfiles()
                .map { it.copy(updatedAt = STARTER_BASELINE_MS).toEntity() }
            profileDao.insertAll(starters)
        }
    }

    suspend fun getAllProfilesOnce(): List<ControllerProfile> =
        profiles.first()

    suspend fun getProfile(id: String): ControllerProfile? =
        profileDao.getProfileById(id)?.toDomain()

    suspend fun saveProfile(profile: ControllerProfile, preserveTimestamp: Boolean = false): ControllerProfile {
        val updated = if (preserveTimestamp) profile else profile.copy(updatedAt = System.currentTimeMillis())
        profileDao.insertProfile(updated.toEntity())
        return updated
    }

    suspend fun upsertFromCloud(cloudProfiles: List<ControllerProfile>) {
        if (cloudProfiles.isEmpty()) return
        val localById = getAllProfilesOnce().associateBy { it.id }
        val toInsert = mutableListOf<ProfileEntity>()
        var cloudActiveId: String? = null

        for (cloud in cloudProfiles) {
            val local = localById[cloud.id]
            val shouldApply = local == null ||
                local.updatedAt <= STARTER_BASELINE_MS ||
                cloud.updatedAt > local.updatedAt
            if (shouldApply) {
                toInsert += cloud.toEntity()
                if (cloud.isActive) {
                    cloudActiveId = cloud.id
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            profileDao.insertAll(toInsert)
        }
        if (cloudActiveId != null) {
            profileDao.setActiveProfile(cloudActiveId)
        }
    }

    suspend fun setActiveProfile(id: String) {
        profileDao.setActiveProfile(id)
    }

    suspend fun duplicateProfile(source: ControllerProfile): ControllerProfile {
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} (Copy)",
            isActive = false,
            updatedAt = System.currentTimeMillis(),
            controls = source.controls.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        profileDao.insertProfile(copy.toEntity())
        return copy
    }

    suspend fun deleteProfile(id: String) {
        val target = profileDao.getProfileById(id)
        profileDao.deleteProfile(id)
        if (target?.isActive == true) {
            val count = profileDao.countProfiles()
            if (count == 0) {
                ensureSeeded()
            } else {
                val firstRemaining = getAllProfilesOnce().firstOrNull()
                if (firstRemaining != null) {
                    profileDao.setActiveProfile(firstRemaining.id)
                }
            }
        }
    }

    fun exportProfileJson(profile: ControllerProfile): String =
        profileAdapter.indent("  ").toJson(profile)

    suspend fun importProfileJson(json: String): Result<ControllerProfile> = runCatching {
        val parsed = profileAdapter.fromJson(json)
            ?: throw IllegalArgumentException("Invalid controller profile JSON")
        val imported = parsed.copy(
            id = UUID.randomUUID().toString(),
            isActive = false,
            updatedAt = System.currentTimeMillis(),
            controls = parsed.controls.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        profileDao.insertProfile(imported.toEntity())
        imported
    }

    private fun ControllerProfile.toEntity(): ProfileEntity = ProfileEntity(
        id = id,
        name = name,
        gameTitle = gameTitle,
        genre = genre.name,
        controlsJson = controlListAdapter.toJson(controls),
        globalOpacity = globalOpacity,
        gridSnap = gridSnap,
        isActive = isActive,
        updatedAt = updatedAt
    )

    private fun ProfileEntity.toDomain(): ControllerProfile? {
        val parsedControls = runCatching {
            controlListAdapter.fromJson(controlsJson)
        }.getOrNull() ?: emptyList()

        val parsedGenre = runCatching { GameGenre.valueOf(genre) }.getOrDefault(GameGenre.CUSTOM)
        return ControllerProfile(
            id = id,
            name = name,
            gameTitle = gameTitle,
            genre = parsedGenre,
            controls = parsedControls,
            globalOpacity = globalOpacity,
            gridSnap = gridSnap,
            isActive = isActive,
            updatedAt = updatedAt
        )
    }

    companion object {
        const val STARTER_BASELINE_MS = 1_000_000L
    }
}
