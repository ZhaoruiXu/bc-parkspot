package ca.bcparkspot.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Owner of rows written before any account exists (first launch offline). Adopted by the first uid that appears. */
const val LOCAL_DEVICE_OWNER = "local-device-owner"

/** The single car pin a user has; its Firestore document id. */
const val CAR_PIN_DOCUMENT_ID = "current"

interface SyncedRecord {
    val updatedAt: Long
    val deleted: Boolean
    val version: Long
    val syncedVersion: Long
}

val SyncedRecord.needsUpload: Boolean get() = version > syncedVersion

@Entity(tableName = "saved_lots", primaryKeys = ["ownerUid", "lotId"])
data class SavedLotEntity(
    val ownerUid: String,
    val lotId: String,
    val parkName: String,
    val lotName: String,
    val region: String,
    val userNotes: String = "",
    val savedAtTimestamp: Long,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
    override val version: Long = 1,
    override val syncedVersion: Long = 0,
    val previousOwnerUid: String? = null,
    /** Set when the server refused this row for a reason a retry will not fix (4.11). */
    val syncError: String? = null
) : SyncedRecord

@Entity(tableName = "parked_car")
data class ParkedCarEntity(
    @PrimaryKey val ownerUid: String,
    val parkName: String,
    val lotName: String,
    val latitude: Double,
    val longitude: Double,
    val stallNumber: String = "",
    val notes: String = "",
    val timestamp: Long,
    val isExactGpsLocation: Boolean = false,
    override val updatedAt: Long,
    override val deleted: Boolean = false,
    override val version: Long = 1,
    override val syncedVersion: Long = 0,
    val previousOwnerUid: String? = null,
    /** Set when the server refused this row for a reason a retry will not fix (4.11). */
    val syncError: String? = null
) : SyncedRecord

enum class ReportSyncState { PENDING, SYNCED, REJECTED, EXPIRED }

@Entity(tableName = "own_reports", indices = [Index("ownerUid")])
data class OwnReportEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val lotId: String,
    val parkName: String,
    val reporterName: String,
    val status: String,
    val comment: String,
    val spotsEstimate: Int? = null,
    val observedAt: Long,
    val syncState: ReportSyncState = ReportSyncState.PENDING,
    val rejectReason: String? = null,
    val previousOwnerUid: String? = null
)
