package ca.bcparkspot.app.data.remote

import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.CommunityReport
import com.google.firebase.Timestamp
import java.util.Date

/** Firestore field names, shared by the client and the rules test. */
object CloudFields {
    const val USERS = "users"
    const val SAVED_LOTS = "savedLots"
    const val PARKED_VEHICLES = "parkedVehicles"
    const val REPORTS = "communityReports"

    const val LOT_ID = "lotId"
    const val PARK_NAME = "parkName"
    const val LOT_NAME = "lotName"
    const val REGION = "region"
    const val USER_NOTES = "userNotes"
    const val SAVED_AT = "savedAt"
    const val UPDATED_AT = "updatedAt"
    const val DELETED = "deleted"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"
    const val STALL_NUMBER = "stallNumber"
    const val NOTES = "notes"
    const val IS_EXACT_GPS = "isExactGpsLocation"
    const val PARKED_AT = "parkedAt"
    const val USER_ID = "userId"
    const val REPORTER_NAME = "reporterName"
    const val STATUS = "status"
    const val COMMENT = "comment"
    const val SPOTS_ESTIMATE = "spotsEstimate"
    const val OBSERVED_AT = "observedAt"
    const val CREATED_AT = "createdAt"
}

private fun ts(ms: Long) = Timestamp(Date(ms))
private fun Any?.millis(): Long? = (this as? Timestamp)?.toDate()?.time
private fun Any?.str(): String? = this as? String
private fun Any?.num(): Double? = (this as? Number)?.toDouble()

fun SavedLotEntity.toCloud(): Map<String, Any> = mapOf(
    CloudFields.LOT_ID to lotId,
    CloudFields.PARK_NAME to parkName,
    CloudFields.LOT_NAME to lotName,
    CloudFields.REGION to region,
    CloudFields.USER_NOTES to userNotes,
    CloudFields.SAVED_AT to ts(savedAtTimestamp),
    CloudFields.UPDATED_AT to ts(updatedAt),
    CloudFields.DELETED to deleted
)

fun savedLotFromCloud(owner: String, data: Map<String, Any?>): SavedLotEntity? {
    val lotId = data[CloudFields.LOT_ID].str() ?: return null
    val updatedAt = data[CloudFields.UPDATED_AT].millis() ?: return null
    return SavedLotEntity(
        ownerUid = owner,
        lotId = lotId,
        parkName = data[CloudFields.PARK_NAME].str().orEmpty(),
        lotName = data[CloudFields.LOT_NAME].str().orEmpty(),
        region = data[CloudFields.REGION].str().orEmpty(),
        userNotes = data[CloudFields.USER_NOTES].str().orEmpty(),
        savedAtTimestamp = data[CloudFields.SAVED_AT].millis() ?: updatedAt,
        updatedAt = updatedAt,
        deleted = data[CloudFields.DELETED] as? Boolean ?: false
    )
}

fun ParkedCarEntity.toCloud(): Map<String, Any> = mapOf(
    CloudFields.PARK_NAME to parkName,
    CloudFields.LOT_NAME to lotName,
    CloudFields.LATITUDE to latitude,
    CloudFields.LONGITUDE to longitude,
    CloudFields.STALL_NUMBER to stallNumber,
    CloudFields.NOTES to notes,
    CloudFields.IS_EXACT_GPS to isExactGpsLocation,
    CloudFields.PARKED_AT to ts(timestamp),
    CloudFields.UPDATED_AT to ts(updatedAt),
    CloudFields.DELETED to deleted
)

fun parkedCarFromCloud(owner: String, data: Map<String, Any?>): ParkedCarEntity? {
    val updatedAt = data[CloudFields.UPDATED_AT].millis() ?: return null
    return ParkedCarEntity(
        ownerUid = owner,
        parkName = data[CloudFields.PARK_NAME].str().orEmpty(),
        lotName = data[CloudFields.LOT_NAME].str().orEmpty(),
        latitude = data[CloudFields.LATITUDE].num() ?: return null,
        longitude = data[CloudFields.LONGITUDE].num() ?: return null,
        stallNumber = data[CloudFields.STALL_NUMBER].str().orEmpty(),
        notes = data[CloudFields.NOTES].str().orEmpty(),
        timestamp = data[CloudFields.PARKED_AT].millis() ?: updatedAt,
        isExactGpsLocation = data[CloudFields.IS_EXACT_GPS] as? Boolean ?: false,
        updatedAt = updatedAt,
        deleted = data[CloudFields.DELETED] as? Boolean ?: false
    )
}

/** The created-at field is filled in by the server; the caller adds it as a server timestamp. */
fun OwnReportEntity.toCloud(uid: String): Map<String, Any> = buildMap {
    put(CloudFields.USER_ID, uid)
    put(CloudFields.REPORTER_NAME, reporterName)
    put(CloudFields.LOT_ID, lotId)
    put(CloudFields.PARK_NAME, parkName)
    put(CloudFields.STATUS, status)
    put(CloudFields.COMMENT, comment)
    spotsEstimate?.let { put(CloudFields.SPOTS_ESTIMATE, it) }
    put(CloudFields.OBSERVED_AT, ts(observedAt))
}

fun reportFromCloud(id: String, data: Map<String, Any?>): CommunityReport? {
    val observedAt = data[CloudFields.OBSERVED_AT].millis() ?: return null
    val status = data[CloudFields.STATUS].str()?.let { name -> AvailabilityStatus.entries.firstOrNull { it.name == name } }
        ?: return null
    return CommunityReport(
        id = id,
        userId = data[CloudFields.USER_ID].str().orEmpty(),
        lotId = data[CloudFields.LOT_ID].str() ?: return null,
        reporterName = data[CloudFields.REPORTER_NAME].str().orEmpty(),
        statusReported = status,
        comment = data[CloudFields.COMMENT].str().orEmpty(),
        spotsEstimate = (data[CloudFields.SPOTS_ESTIMATE] as? Number)?.toInt(),
        timestampMs = observedAt
    )
}
