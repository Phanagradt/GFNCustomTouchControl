package com.example.data

import androidx.annotation.Keep
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudUserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudUserLayout(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val gameTitle: String = "",
    val genre: String = GameGenre.CUSTOM.name,
    val globalOpacity: Double = 0.85,
    val gridSnap: Boolean = true,
    val active: Boolean = false,
    val controls: List<Map<String, Any>> = emptyList(),
    val clientUpdatedAt: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSharedLayout(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val authorName: String = "",
    val name: String = "",
    val gameTitle: String = "",
    val genre: String = GameGenre.CUSTOM.name,
    val globalOpacity: Double = 0.85,
    val gridSnap: Boolean = true,
    val controls: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

data class SharedCodeTicket(
    val code: String,
    val profileName: String,
    val expiresAtMs: Long
)

data class RedeemedSharedLayout(
    val code: String,
    val authorName: String,
    val profile: ControllerProfile
)

object CloudLayoutMapper {

    fun sanitizeShareCode(raw: String): String =
        raw.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")

    fun isValidShareCodeFormat(code: String): Boolean =
        code.length in 6..16 && code.matches(Regex("^[A-Z0-9]+$"))

    fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "layout_default" }

    fun controlToMap(control: VirtualControl): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to control.id,
            "label" to control.label.take(24),
            "controlType" to control.controlType.name,
            "shape" to control.shape.name,
            "xPercent" to control.xPercent.toDouble(),
            "yPercent" to control.yPercent.toDouble(),
            "sizeDp" to control.sizeDp.toDouble(),
            "opacity" to control.opacity.toDouble(),
            "accent" to control.accent.name,
            "behavior" to control.behavior.name,
            "turboRateHz" to control.turboRateHz.toLong(),
            "deadzone" to control.deadzone.toDouble(),
            "sensitivity" to control.sensitivity.toDouble(),
            "hapticOnPress" to control.hapticOnPress,
            "invertY" to control.invertY,
            "binding" to bindingToMap(control.binding)
        )
        control.customImageBase64?.takeIf { it.isNotBlank() }?.let {
            map["customImageBase64"] = it
        }
        return map
    }

    private fun bindingToMap(binding: InputBinding): Map<String, Any> = mapOf(
        "id" to binding.id,
        "domain" to binding.domain.name,
        "primaryKeyCode" to binding.primaryKeyCode.toLong(),
        "secondaryKeyCode" to binding.secondaryKeyCode.toLong(),
        "modifierMetaState" to binding.modifierMetaState.toLong(),
        "codeLabel" to binding.codeLabel,
        "commandTitle" to binding.commandTitle,
        "axisPair" to binding.axisPair.name
    )

    fun toDomain(doc: CloudUserLayout): ControllerProfile {
        val parsedGenre = enumValues<GameGenre>().firstOrNull { it.name == doc.genre } ?: GameGenre.CUSTOM
        return ControllerProfile(
            id = doc.id.ifBlank { "cloud_layout" },
            name = doc.name.ifBlank { "Cloud Layout" },
            gameTitle = doc.gameTitle.ifBlank { "GeForce NOW" },
            genre = parsedGenre,
            controls = doc.controls.mapNotNull(::mapToControl),
            globalOpacity = doc.globalOpacity.toFloat().coerceIn(0.2f, 1.0f),
            gridSnap = doc.gridSnap,
            isActive = doc.active,
            updatedAt = if (doc.clientUpdatedAt > 0L) doc.clientUpdatedAt else (doc.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
        )
    }

    fun mapToControl(raw: Map<String, Any>): VirtualControl? {
        val id = (raw["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val bindingRaw = (raw["binding"] as? Map<*, *>)?.entries
            ?.mapNotNull { (k, v) -> (k as? String)?.let { ks -> v?.let { ks to it } } }
            ?.toMap() ?: emptyMap()
        val binding = mapToBinding(bindingRaw) ?: return null

        val typeName = raw["controlType"] as? String
        val shapeName = raw["shape"] as? String
        val accentName = raw["accent"] as? String
        val behaviorName = raw["behavior"] as? String

        return VirtualControl(
            id = id,
            label = (raw["label"] as? String)?.ifBlank { binding.codeLabel } ?: binding.codeLabel,
            controlType = enumValues<ControlType>().firstOrNull { it.name == typeName } ?: ControlType.BUTTON,
            shape = enumValues<ControlShape>().firstOrNull { it.name == shapeName } ?: ControlShape.CIRCLE,
            xPercent = ((raw["xPercent"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.05f, 0.95f),
            yPercent = ((raw["yPercent"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.08f, 0.92f),
            sizeDp = ((raw["sizeDp"] as? Number)?.toFloat() ?: 64f).coerceIn(48f, 240f),
            opacity = ((raw["opacity"] as? Number)?.toFloat() ?: 0.85f).coerceIn(0.2f, 1.0f),
            accent = enumValues<ControlAccent>().firstOrNull { it.name == accentName } ?: ControlAccent.LIME,
            behavior = enumValues<TriggerBehavior>().firstOrNull { it.name == behaviorName } ?: TriggerBehavior.HOLD,
            turboRateHz = ((raw["turboRateHz"] as? Number)?.toInt() ?: 10).coerceIn(2, 25),
            deadzone = ((raw["deadzone"] as? Number)?.toFloat() ?: 0.12f).coerceIn(0.02f, 0.40f),
            sensitivity = ((raw["sensitivity"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.5f, 2.2f),
            hapticOnPress = (raw["hapticOnPress"] as? Boolean) ?: true,
            invertY = (raw["invertY"] as? Boolean) ?: false,
            customImageBase64 = (raw["customImageBase64"] as? String)?.takeIf { it.isNotBlank() },
            binding = binding
        )
    }

    private fun mapToBinding(raw: Map<String, Any>): InputBinding? {
        val id = (raw["id"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val domainName = raw["domain"] as? String
        val axisName = raw["axisPair"] as? String
        return InputBinding(
            id = id,
            domain = enumValues<BindingDomain>().firstOrNull { it.name == domainName } ?: BindingDomain.KEYBOARD,
            primaryKeyCode = (raw["primaryKeyCode"] as? Number)?.toInt() ?: 0,
            secondaryKeyCode = (raw["secondaryKeyCode"] as? Number)?.toInt() ?: 0,
            modifierMetaState = (raw["modifierMetaState"] as? Number)?.toInt() ?: 0,
            codeLabel = (raw["codeLabel"] as? String) ?: "KEY",
            commandTitle = (raw["commandTitle"] as? String) ?: "Custom Key",
            axisPair = enumValues<AxisPair>().firstOrNull { it.name == axisName } ?: AxisPair.NONE
        )
    }
}
