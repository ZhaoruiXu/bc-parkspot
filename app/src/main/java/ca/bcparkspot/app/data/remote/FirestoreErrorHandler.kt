package ca.bcparkspot.app.data.remote

import android.util.Log
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    // Non-identifying error code: extract class name and sanitized message without any PII
    val errorCode = exception.javaClass.simpleName
    val sanitizedPath = path?.substringBefore("/") ?: "unknown"

    val errorInfoJson = JSONObject().apply {
        put("errorCode", errorCode)
        put("operationType", operationType.value)
        put("collection", sanitizedPath)
    }

    val jsonString = errorInfoJson.toString()
    // Safe logging without user email, UID, or identity
    Log.e("FirestoreError", "Operation ${operationType.value} failed on $sanitizedPath: $errorCode")
    return jsonString
}

