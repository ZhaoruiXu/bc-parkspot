package ca.bcparkspot.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {
    // Saved lots
    @Query("SELECT * FROM saved_lots WHERE ownerUid = :owner AND deleted = 0 ORDER BY savedAtTimestamp DESC")
    fun observeSavedLots(owner: String): Flow<List<SavedLotEntity>>

    @Query("SELECT * FROM saved_lots WHERE ownerUid = :owner")
    suspend fun savedLotRows(owner: String): List<SavedLotEntity>

    @Query("SELECT * FROM saved_lots WHERE ownerUid = :owner AND lotId = :lotId")
    suspend fun savedLot(owner: String, lotId: String): SavedLotEntity?

    @Upsert
    suspend fun upsertSavedLot(row: SavedLotEntity)

    @Query("DELETE FROM saved_lots WHERE ownerUid = :owner AND lotId = :lotId")
    suspend fun removeSavedLot(owner: String, lotId: String)

    @Query("UPDATE saved_lots SET syncedVersion = :version, syncError = NULL WHERE ownerUid = :owner AND lotId = :lotId AND version = :version")
    suspend fun markSavedLotSynced(owner: String, lotId: String, version: Long): Int

    @Query("UPDATE saved_lots SET syncError = :error WHERE ownerUid = :owner AND lotId = :lotId")
    suspend fun setSavedLotError(owner: String, lotId: String, error: String?)

    @Query("DELETE FROM saved_lots WHERE ownerUid = :owner AND deleted = 1 AND version = syncedVersion AND updatedAt < :before")
    suspend fun purgeSavedLotTombstones(owner: String, before: Long): Int

    // Car pin
    @Query("SELECT * FROM parked_car WHERE ownerUid = :owner AND deleted = 0")
    fun observeParkedCar(owner: String): Flow<ParkedCarEntity?>

    @Query("SELECT * FROM parked_car WHERE ownerUid = :owner")
    suspend fun parkedCarRow(owner: String): ParkedCarEntity?

    @Upsert
    suspend fun upsertParkedCar(row: ParkedCarEntity)

    @Query("DELETE FROM parked_car WHERE ownerUid = :owner")
    suspend fun removeParkedCar(owner: String)

    @Query("UPDATE parked_car SET syncedVersion = :version, syncError = NULL WHERE ownerUid = :owner AND version = :version")
    suspend fun markParkedCarSynced(owner: String, version: Long): Int

    @Query("UPDATE parked_car SET syncError = :error WHERE ownerUid = :owner")
    suspend fun setParkedCarError(owner: String, error: String?)

    @Query("DELETE FROM parked_car WHERE ownerUid = :owner AND deleted = 1 AND version = syncedVersion AND updatedAt < :before")
    suspend fun purgeParkedCarTombstone(owner: String, before: Long): Int

    // Own reports
    @Query("SELECT * FROM own_reports WHERE ownerUid = :owner ORDER BY observedAt DESC")
    fun observeOwnReports(owner: String): Flow<List<OwnReportEntity>>

    @Query("SELECT * FROM own_reports WHERE ownerUid = :owner")
    suspend fun ownReportRows(owner: String): List<OwnReportEntity>

    @Query("SELECT * FROM own_reports WHERE id = :id")
    suspend fun ownReport(id: String): OwnReportEntity?

    @Query("SELECT id FROM own_reports WHERE syncState = 'PENDING'")
    fun observePendingReportIds(): Flow<List<String>>

    @Upsert
    suspend fun upsertOwnReport(row: OwnReportEntity)

    @Query("UPDATE own_reports SET syncState = :state, rejectReason = :reason WHERE id = :id")
    suspend fun setReportState(id: String, state: ReportSyncState, reason: String?)

    @Query("DELETE FROM own_reports WHERE id = :id")
    suspend fun removeOwnReport(id: String)

    @Query("SELECT COUNT(*) FROM saved_lots WHERE ownerUid = :owner AND syncError IS NOT NULL")
    fun observeSavedLotErrorCount(owner: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM parked_car WHERE ownerUid = :owner AND syncError IS NOT NULL")
    fun observeCarErrorCount(owner: String): Flow<Int>

    @Query("UPDATE saved_lots SET syncError = NULL WHERE ownerUid = :owner")
    suspend fun clearSavedLotErrors(owner: String)

    @Query("UPDATE parked_car SET syncError = NULL WHERE ownerUid = :owner")
    suspend fun clearCarError(owner: String)

    // Whole-account operations
    @Query("SELECT COUNT(*) FROM saved_lots WHERE ownerUid = :owner AND version > syncedVersion AND syncError IS NULL")
    fun observePendingSavedLotCount(owner: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM parked_car WHERE ownerUid = :owner AND version > syncedVersion AND syncError IS NULL")
    fun observePendingCarCount(owner: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM own_reports WHERE ownerUid = :owner AND syncState = 'PENDING'")
    fun observePendingReportCount(owner: String): Flow<Int>

    @Query("DELETE FROM saved_lots WHERE ownerUid = :owner")
    suspend fun deleteSavedLotsOf(owner: String)

    @Query("DELETE FROM parked_car WHERE ownerUid = :owner")
    suspend fun deleteParkedCarOf(owner: String)

    @Query("DELETE FROM own_reports WHERE ownerUid = :owner")
    suspend fun deleteOwnReportsOf(owner: String)

    @Query("DELETE FROM saved_lots WHERE ownerUid = :owner AND version = syncedVersion")
    suspend fun deleteSyncedSavedLotsOf(owner: String)

    @Query("DELETE FROM parked_car WHERE ownerUid = :owner AND version = syncedVersion")
    suspend fun deleteSyncedParkedCarOf(owner: String)

    @Query("DELETE FROM own_reports WHERE ownerUid = :owner AND syncState != 'PENDING'")
    suspend fun deleteSettledReportsOf(owner: String)
}
