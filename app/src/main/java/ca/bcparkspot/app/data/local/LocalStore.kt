package ca.bcparkspot.app.data.local

import androidx.room.withTransaction
import ca.bcparkspot.app.data.local.entity.LOCAL_DEVICE_OWNER
import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.sync.AdoptOutcome
import ca.bcparkspot.app.sync.CloudApply
import ca.bcparkspot.app.sync.CloudRecordMeta
import ca.bcparkspot.app.sync.cloudApply
import ca.bcparkspot.app.sync.adoptOutcome
import ca.bcparkspot.app.sync.adoptedVersions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private fun OwnReportEntity.movesWithAccount(nowMs: Long, windowMs: Long): Boolean =
    syncState == ReportSyncState.PENDING || (syncState == ReportSyncState.SYNCED && nowMs - observedAt <= windowMs)

/** Room is the source of truth for the user's own data (4.1). Every user action lands here first. */
class LocalStore(private val db: AppDatabase) {
    internal val dao = db.parkingDao()

    fun savedLots(owner: String): Flow<List<SavedLotEntity>> = dao.observeSavedLots(owner)
    fun parkedCar(owner: String): Flow<ParkedCarEntity?> = dao.observeParkedCar(owner)
    fun ownReports(owner: String): Flow<List<OwnReportEntity>> = dao.observeOwnReports(owner)
    fun pendingReportIds(): Flow<List<String>> = dao.observePendingReportIds()

    fun pendingChangeCount(owner: String): Flow<Int> = combine(
        dao.observePendingSavedLotCount(owner),
        dao.observePendingCarCount(owner),
        dao.observePendingReportCount(owner)
    ) { lots, car, reports -> lots + car + reports }

    suspend fun saveLot(owner: String, lotId: String, parkName: String, lotName: String, region: String, notes: String, nowMs: Long) {
        db.withTransaction {
            val existing = dao.savedLot(owner, lotId)
            dao.upsertSavedLot(
                existing?.copy(
                    parkName = parkName, lotName = lotName, region = region, userNotes = notes,
                    savedAtTimestamp = nowMs, updatedAt = nowMs, deleted = false, version = existing.version + 1, syncError = null
                ) ?: SavedLotEntity(owner, lotId, parkName, lotName, region, notes, nowMs, nowMs)
            )
        }
    }

    /** Unsave writes a tombstone so the delete wins on every device; a row the cloud never saw is just removed. */
    suspend fun unsaveLot(owner: String, lotId: String, nowMs: Long) {
        db.withTransaction {
            val existing = dao.savedLot(owner, lotId) ?: return@withTransaction
            if (existing.syncedVersion == 0L) {
                dao.removeSavedLot(owner, lotId)
            } else {
                dao.upsertSavedLot(existing.copy(deleted = true, updatedAt = nowMs, version = existing.version + 1, syncError = null))
            }
        }
    }

    suspend fun pinCar(owner: String, car: ParkedCarEntity) {
        db.withTransaction {
            val existing = dao.parkedCarRow(owner)
            dao.upsertParkedCar(
                car.copy(
                    ownerUid = owner,
                    deleted = false,
                    syncError = null,
                    version = (existing?.version ?: 0L) + 1,
                    syncedVersion = existing?.syncedVersion ?: 0L
                )
            )
        }
    }

    suspend fun clearCar(owner: String, nowMs: Long) {
        db.withTransaction {
            val existing = dao.parkedCarRow(owner) ?: return@withTransaction
            if (existing.syncedVersion == 0L) {
                dao.removeParkedCar(owner)
            } else {
                dao.upsertParkedCar(existing.copy(deleted = true, updatedAt = nowMs, version = existing.version + 1, syncError = null))
            }
        }
    }

    /** 4.8 and 4.9: the cloud held a newer record; take it, unless the user changed the row during the upload. */
    suspend fun applyCloudSavedLotIfUnchanged(owner: String, uploadedVersion: Long, cloud: SavedLotEntity) {
        db.withTransaction {
            val current = dao.savedLot(owner, cloud.lotId) ?: return@withTransaction
            if (current.version != uploadedVersion) return@withTransaction
            dao.upsertSavedLot(cloud.copy(ownerUid = owner, version = uploadedVersion, syncedVersion = uploadedVersion, syncError = null))
        }
    }

    suspend fun applyCloudCarIfUnchanged(owner: String, uploadedVersion: Long, cloud: ParkedCarEntity) {
        db.withTransaction {
            val current = dao.parkedCarRow(owner) ?: return@withTransaction
            if (current.version != uploadedVersion) return@withTransaction
            dao.upsertParkedCar(cloud.copy(ownerUid = owner, version = uploadedVersion, syncedVersion = uploadedVersion, syncError = null))
        }
    }

    /** 4.13: a record from the saved-lot listener. */
    suspend fun applyCloudSavedLot(owner: String, cloud: SavedLotEntity, fromCache: Boolean) {
        db.withTransaction {
            val local = dao.savedLot(owner, cloud.lotId)
            if (cloudApply(local, CloudRecordMeta(cloud.updatedAt, cloud.deleted, fromCache)) == CloudApply.IGNORE) return@withTransaction
            val version = local?.version ?: 1L
            dao.upsertSavedLot(cloud.copy(ownerUid = owner, version = version, syncedVersion = version, syncError = null))
        }
    }

    suspend fun applyCloudCar(owner: String, cloud: ParkedCarEntity, fromCache: Boolean) {
        db.withTransaction {
            val local = dao.parkedCarRow(owner)
            if (cloudApply(local, CloudRecordMeta(cloud.updatedAt, cloud.deleted, fromCache)) == CloudApply.IGNORE) return@withTransaction
            val version = local?.version ?: 1L
            dao.upsertParkedCar(cloud.copy(ownerUid = owner, version = version, syncedVersion = version, syncError = null))
        }
    }

    fun syncErrorCount(owner: String): Flow<Int> =
        combine(dao.observeSavedLotErrorCount(owner), dao.observeCarErrorCount(owner)) { a, b -> a + b }

    suspend fun clearSyncErrors(owner: String) {
        db.withTransaction {
            dao.clearSavedLotErrors(owner)
            dao.clearCarError(owner)
        }
    }

    suspend fun addReport(report: OwnReportEntity) = dao.upsertOwnReport(report.copy(syncState = ReportSyncState.PENDING))

    suspend fun discardReport(id: String) = dao.removeOwnReport(id)

    /** 4.5: move every row of [from] into [target], resolving key clashes with the 4.5a merge. */
    suspend fun adopt(from: String, target: String) {
        if (from == target) return
        db.withTransaction {
            dao.savedLotRows(from).forEach { row ->
                val existing = dao.savedLot(target, row.lotId)
                val (version, synced) = adoptedVersions(existing)
                when (adoptOutcome(row, existing)) {
                    AdoptOutcome.MOVE, AdoptOutcome.INCOMING_WINS ->
                        dao.upsertSavedLot(row.copy(ownerUid = target, version = version, syncedVersion = synced, syncError = null))
                    AdoptOutcome.EXISTING_WINS ->
                        dao.upsertSavedLot(existing!!.copy(version = maxOf(existing.version, version)))
                }
                dao.removeSavedLot(from, row.lotId)
            }
            dao.parkedCarRow(from)?.let { row ->
                val existing = dao.parkedCarRow(target)
                val (version, synced) = adoptedVersions(existing)
                when (adoptOutcome(row, existing)) {
                    AdoptOutcome.MOVE, AdoptOutcome.INCOMING_WINS ->
                        dao.upsertParkedCar(row.copy(ownerUid = target, version = version, syncedVersion = synced, syncError = null))
                    AdoptOutcome.EXISTING_WINS ->
                        dao.upsertParkedCar(existing!!.copy(version = maxOf(existing.version, version)))
                }
                dao.removeParkedCar(from)
            }
            dao.ownReportRows(from).filter { it.syncState == ReportSyncState.PENDING }
                .forEach { dao.upsertOwnReport(it.copy(ownerUid = target)) }
        }
    }

    suspend fun adoptDeviceRows(target: String) = adopt(LOCAL_DEVICE_OWNER, target)

    /** 4.16 step 1: what the guest would bring into the Google account. */
    suspend fun movableItemCount(guest: String, nowMs: Long, windowMs: Long): Int {
        val lots = dao.savedLotRows(guest).count { !it.deleted }
        val car = if (dao.parkedCarRow(guest)?.deleted == false) 1 else 0
        val reports = dao.ownReportRows(guest).count { it.movesWithAccount(nowMs, windowMs) }
        return lots + car + reports
    }

    /**
     * 4.16 step 3: park the guest's rows under the device owner, all needing upload. Synced reports still
     * inside the window go back to pending with the same id, so they are re-shared under the new account.
     */
    suspend fun parkGuestRowsForMove(guest: String, nowMs: Long, windowMs: Long) {
        db.withTransaction {
            dao.savedLotRows(guest).forEach {
                dao.upsertSavedLot(it.copy(ownerUid = LOCAL_DEVICE_OWNER, version = 1, syncedVersion = 0, previousOwnerUid = guest, syncError = null))
                dao.removeSavedLot(guest, it.lotId)
            }
            dao.parkedCarRow(guest)?.let {
                dao.upsertParkedCar(it.copy(ownerUid = LOCAL_DEVICE_OWNER, version = 1, syncedVersion = 0, previousOwnerUid = guest, syncError = null))
                dao.removeParkedCar(guest)
            }
            dao.ownReportRows(guest).filter { it.movesWithAccount(nowMs, windowMs) }.forEach {
                dao.upsertOwnReport(it.copy(ownerUid = LOCAL_DEVICE_OWNER, syncState = ReportSyncState.PENDING, rejectReason = null, previousOwnerUid = guest))
            }
        }
    }

    /** 4.16 rollback: give the parked rows back to the guest; they still need upload, so nothing deleted is lost. */
    suspend fun returnParkedRowsTo(guest: String) = adopt(LOCAL_DEVICE_OWNER, guest)

    suspend fun deleteEverythingOf(owner: String) {
        db.withTransaction {
            dao.deleteSavedLotsOf(owner)
            dao.deleteParkedCarOf(owner)
            dao.deleteOwnReportsOf(owner)
        }
    }

    /** 4.18: after sign-out, keep only the rows that still need upload; they reappear when the account returns. */
    suspend fun dropSettledRowsOf(owner: String) {
        db.withTransaction {
            dao.deleteSyncedSavedLotsOf(owner)
            dao.deleteSyncedParkedCarOf(owner)
            dao.deleteSettledReportsOf(owner)
        }
    }

    suspend fun purgeTombstones(owner: String, before: Long) {
        db.withTransaction {
            dao.purgeSavedLotTombstones(owner, before)
            dao.purgeParkedCarTombstone(owner, before)
        }
    }
}
