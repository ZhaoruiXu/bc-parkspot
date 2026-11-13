package ca.bcparkspot.app.sync

import ca.bcparkspot.app.data.local.LocalStore
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.data.local.entity.needsUpload
import ca.bcparkspot.app.domain.ReportPolicy
import kotlinx.coroutines.withTimeout

enum class SyncRunResult { DONE, RETRY }

/**
 * One upload pass for the current account (4.7 to 4.12). Room keeps every pending change; a row is marked
 * synced only after the server acknowledged it, and only if it did not change meanwhile.
 */
class SyncEngine(
    private val local: LocalStore,
    private val remote: RemoteStore,
    private val policy: ReportPolicy,
    private val clock: () -> Long,
    private val writeTimeoutMs: Long = SyncPolicy.workerWriteTimeoutMs,
    private val onCompleted: (Long) -> Unit = {}
) {
    private val dao get() = local.dao

    suspend fun syncOnce(uid: String): SyncRunResult {
        var retry = false
        suspend fun attempt(onRefused: suspend (String) -> Unit, block: suspend () -> Unit) {
            try {
                withTimeout(writeTimeoutMs) { block() }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                retry = true
            } catch (e: RemoteWriteException) {
                if (e.kind == RemoteErrorKind.RETRYABLE) retry = true else onRefused(e.message ?: "Refused by the server")
            }
        }

        dao.savedLotRows(uid).filter { it.needsUpload && it.syncError == null }.forEach { row ->
            attempt(onRefused = { reason -> dao.setSavedLotError(uid, row.lotId, reason) }) {
                when (val result = remote.upsertSavedLot(uid, row)) {
                    UpsertResult.Written -> dao.markSavedLotSynced(uid, row.lotId, row.version)
                    is UpsertResult.CloudNewer -> local.applyCloudSavedLotIfUnchanged(uid, row.version, result.cloud)
                }
            }
        }
        dao.parkedCarRow(uid)?.takeIf { it.needsUpload && it.syncError == null }?.let { row ->
            attempt(onRefused = { reason -> dao.setParkedCarError(uid, reason) }) {
                when (val result = remote.upsertParkedCar(uid, row)) {
                    UpsertResult.Written -> dao.markParkedCarSynced(uid, row.version)
                    is UpsertResult.CloudNewer -> local.applyCloudCarIfUnchanged(uid, row.version, result.cloud)
                }
            }
        }
        dao.ownReportRows(uid).filter { it.syncState == ReportSyncState.PENDING }.forEach { row ->
            val now = clock()
            if (reportUploadDecision(row.observedAt, now, policy) == ReportUploadDecision.EXPIRE) {
                dao.setReportState(row.id, ReportSyncState.EXPIRED, null)
                return@forEach
            }
            try {
                withTimeout(writeTimeoutMs) { remote.createReport(uid, row) }
                dao.setReportState(row.id, ReportSyncState.SYNCED, null)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                retry = true
            } catch (e: RemoteWriteException) {
                when (val outcome = reportErrorOutcome(e, row.observedAt, clock(), policy)) {
                    ReportErrorOutcome.Retry -> retry = true
                    ReportErrorOutcome.Expired -> dao.setReportState(row.id, ReportSyncState.EXPIRED, null)
                    is ReportErrorOutcome.Rejected -> dao.setReportState(row.id, ReportSyncState.REJECTED, outcome.reason)
                    ReportErrorOutcome.CheckExisting -> {
                        val author = try { remote.reportAuthor(row.id) } catch (re: RemoteWriteException) { retry = true; return@forEach }
                        if (isOwnEarlierUpload(author, uid, row.previousOwnerUid)) {
                            dao.setReportState(row.id, ReportSyncState.SYNCED, null)
                        } else {
                            dao.setReportState(row.id, ReportSyncState.REJECTED, e.message ?: "Rejected by the server")
                        }
                    }
                }
            }
        }
        local.purgeTombstones(uid, tombstonePurgeBefore(clock(), SyncPolicy.tombstoneRetentionMs))
        if (!retry) onCompleted(clock())
        return if (retry) SyncRunResult.RETRY else SyncRunResult.DONE
    }
}
