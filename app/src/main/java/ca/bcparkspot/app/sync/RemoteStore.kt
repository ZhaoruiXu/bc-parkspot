package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.entity.OwnReportEntity
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity

/** Result of a last-writer-wins upload: either our record was written, or the cloud held a newer one. */
sealed interface UpsertResult<out T> {
    data object Written : UpsertResult<Nothing>
    data class CloudNewer<T>(val cloud: T) : UpsertResult<T>
}

enum class FlushResult { FLUSHED, TIMED_OUT }

/** The cloud side of sync. Implementations throw [RemoteWriteException] on failure. */
interface RemoteStore {
    suspend fun upsertSavedLot(uid: String, row: SavedLotEntity): UpsertResult<SavedLotEntity>
    suspend fun upsertParkedCar(uid: String, row: ParkedCarEntity): UpsertResult<ParkedCarEntity>
    suspend fun createReport(uid: String, row: OwnReportEntity)
    suspend fun reportAuthor(reportId: String): String?
    suspend fun deleteCloudDataOf(uid: String)
    suspend fun flush(timeoutMs: Long): FlushResult
}
