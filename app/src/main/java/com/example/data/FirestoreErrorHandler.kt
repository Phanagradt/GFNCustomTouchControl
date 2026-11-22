package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?
): Exception {
    val auth = Firebase.auth
    val user = auth.currentUser
    val payload = buildString {
        append("{")
        append("\"error\":\"${(error.localizedMessage ?: error.message ?: "Unknown Firestore error").replace("\"", "'")}\",")
        append("\"operationType\":\"${operationType.name.lowercase()}\",")
        append("\"path\":\"${path ?: "unknown"}\",")
        append("\"userId\":\"${user?.uid ?: "unauthenticated"}\",")
        append("\"emailPresent\":${!user?.email.isNullOrBlank()}")
        append("}")
    }
    Log.e("FirestoreError", payload, error)
    return error
}
