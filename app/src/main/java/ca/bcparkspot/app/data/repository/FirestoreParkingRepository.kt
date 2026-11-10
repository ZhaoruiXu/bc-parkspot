package ca.bcparkspot.app.data.repository

import android.content.Context
import ca.bcparkspot.app.R
import ca.bcparkspot.app.data.remote.OperationType
import ca.bcparkspot.app.data.remote.handleFirestoreError
import ca.bcparkspot.app.data.remote.model.FirestoreCommunityReport
import ca.bcparkspot.app.data.remote.model.FirestoreParkedVehicle
import ca.bcparkspot.app.data.remote.model.FirestoreSavedLot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreParkingRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Constructor resolving named database ID from string resources with safe fallback
    constructor(context: Context) : this(
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Throwable) {
            FirebaseFirestore.getInstance()
        },
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Saved Lots ---
    fun observeSavedLots(userId: String): Flow<List<FirestoreSavedLot>> {
        val path = "users/$userId/savedLots"
        return firestore.collection("users")
            .document(userId)
            .collection("savedLots")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(FirestoreSavedLot::class.java) }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveLot(
        userId: String,
        lotId: String,
        parkName: String,
        lotName: String,
        region: String,
        userNotes: String
    ) {
        val currentUid = requireUserId()
        if (currentUid != userId) throw SecurityException("User ID mismatch")

        val path = "users/$userId/savedLots/$lotId"
        val lotRef = firestore.collection("users")
            .document(userId)
            .collection("savedLots")
            .document(lotId)

        val payload = mapOf(
            "lotId" to lotId,
            "parkName" to parkName,
            "lotName" to lotName,
            "region" to region,
            "userNotes" to userNotes,
            "savedAt" to FieldValue.serverTimestamp()
        )

        try {
            lotRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun deleteSavedLot(userId: String, lotId: String) {
        val currentUid = requireUserId()
        if (currentUid != userId) throw SecurityException("User ID mismatch")

        val path = "users/$userId/savedLots/$lotId"
        try {
            firestore.collection("users")
                .document(userId)
                .collection("savedLots")
                .document(lotId)
                .delete()
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // --- Parked Vehicle Location ---
    fun observeParkedVehicle(userId: String): Flow<FirestoreParkedVehicle?> {
        val path = "users/$userId/parkedVehicles/current"
        return firestore.collection("users")
            .document(userId)
            .collection("parkedVehicles")
            .document("current")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(FirestoreParkedVehicle::class.java)
                } else null
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun saveParkedVehicle(
        userId: String,
        parkName: String,
        lotName: String,
        latitude: Double,
        longitude: Double,
        stallNumber: String,
        notes: String,
        isExactGpsLocation: Boolean = false
    ) {
        val currentUid = requireUserId()
        if (currentUid != userId) throw SecurityException("User ID mismatch")

        val path = "users/$userId/parkedVehicles/current"
        val payload = mapOf(
            "parkName" to parkName,
            "lotName" to lotName,
            "latitude" to latitude,
            "longitude" to longitude,
            "stallNumber" to stallNumber,
            "notes" to notes,
            "isExactGpsLocation" to isExactGpsLocation,
            "parkedAt" to FieldValue.serverTimestamp()
        )

        try {
            firestore.collection("users")
                .document(userId)
                .collection("parkedVehicles")
                .document("current")
                .set(payload)
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun clearParkedVehicle(userId: String) {
        val currentUid = requireUserId()
        if (currentUid != userId) throw SecurityException("User ID mismatch")

        val path = "users/$userId/parkedVehicles/current"
        try {
            firestore.collection("users")
                .document(userId)
                .collection("parkedVehicles")
                .document("current")
                .delete()
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // --- Community Live Reports ---
    fun observeCommunityReports(): Flow<List<FirestoreCommunityReport>> {
        val path = "communityReports"
        return firestore.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FirestoreCommunityReport::class.java)?.copy(id = doc.id)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun postCommunityReport(
        lotId: String,
        parkName: String,
        status: String,
        comment: String,
        spotsEstimate: Int?,
        reporterName: String
    ): String {
        val currentUid = requireUserId()
        val path = "communityReports"
        val reportsCol = firestore.collection(path)
        val newDoc = reportsCol.document()

        val payload = mutableMapOf<String, Any?>(
            "userId" to currentUid,
            "reporterName" to reporterName.ifBlank { "BC Hiker" },
            "lotId" to lotId,
            "parkName" to parkName,
            "status" to status,
            "comment" to comment,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (spotsEstimate != null) {
            payload["spotsEstimate"] = spotsEstimate
        }

        try {
            newDoc.set(payload).await()
            return newDoc.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "${path}/${newDoc.id}")
            throw e
        }
    }

    suspend fun deleteUserData(userId: String) {
        val currentUid = requireUserId()
        if (currentUid != userId) throw SecurityException("User ID mismatch")

        try {
            // 1. Delete saved lots subcollection documents
            val savedLots = firestore.collection("users").document(userId).collection("savedLots").get().await()
            for (doc in savedLots.documents) {
                doc.reference.delete().await()
            }
            // 2. Delete parked vehicles
            val vehicles = firestore.collection("users").document(userId).collection("parkedVehicles").get().await()
            for (doc in vehicles.documents) {
                doc.reference.delete().await()
            }
            // 3. Delete user's public community reports
            val reports = firestore.collection("communityReports").whereEqualTo("userId", userId).get().await()
            for (doc in reports.documents) {
                doc.reference.delete().await()
            }
            // 4. Delete user root doc
            firestore.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
            throw e
        }
    }
}
