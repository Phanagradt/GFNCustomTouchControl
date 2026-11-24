package com.example.cloud

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

object FirestoreErrorHandler {

    private const val TAG = "FirestoreError"

    fun handleFirestoreError(
        exception: Exception,
        operationType: OperationType,
        path: String?
    ): String {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        val providerArray = JSONArray()
        currentUser?.providerData?.forEach { info ->
            providerArray.put(
                JSONObject().apply {
                    put("providerId", info.providerId)
                    put("displayName", info.displayName ?: JSONObject.NULL)
                    put("email", info.email ?: JSONObject.NULL)
                    put("photoUrl", info.photoUrl?.toString() ?: JSONObject.NULL)
                }
            )
        }

        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid ?: JSONObject.NULL)
            put("email", currentUser?.email ?: JSONObject.NULL)
            put("emailVerified", currentUser?.isEmailVerified ?: false)
            put("isAnonymous", currentUser?.isAnonymous ?: false)
            put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
            put("providerInfo", providerArray)
        }

        val payload = JSONObject().apply {
            put("error", exception.message ?: exception.toString())
            put("operationType", operationType.value)
            put("path", path ?: JSONObject.NULL)
            put("authInfo", authInfo)
        }

        val serialized = payload.toString()
        Log.e(TAG, serialized, exception)
        return serialized
    }
}
