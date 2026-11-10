package ca.bcparkspot.app.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreSavedLot(
    val lotId: String = "",
    val parkName: String = "",
    val lotName: String = "",
    val region: String = "",
    val userNotes: String = "",
    @ServerTimestamp val savedAt: Timestamp? = null
)

data class FirestoreParkedVehicle(
    val parkName: String = "",
    val lotName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val stallNumber: String = "",
    val notes: String = "",
    val isExactGpsLocation: Boolean = false,
    @ServerTimestamp val parkedAt: Timestamp? = null
)

data class FirestoreCommunityReport(
    val id: String = "",
    val userId: String = "",
    val reporterName: String = "",
    val lotId: String = "",
    val parkName: String = "",
    val status: String = "AVAILABLE",
    val comment: String = "",
    val spotsEstimate: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
