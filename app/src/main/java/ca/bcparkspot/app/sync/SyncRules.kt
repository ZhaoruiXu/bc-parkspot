package ca.bcparkspot.app.sync

import ca.bcparkspot.app.domain.ReportPolicy
import java.util.concurrent.TimeUnit

object SyncPolicy {
    const val WORKER_WRITE_TIMEOUT_SECONDS = 30L
    const val FLUSH_TIMEOUT_SECONDS = 15L
    const val SIGN_OUT_SYNC_TIMEOUT_SECONDS = 20L
    const val TOMBSTONE_RETENTION_DAYS = 30L
    const val REPORT_QUERY_REFRESH_MINUTES = 15L
    const val LIVE_FEED_GRACE_SECONDS = 10L
    const val BACKOFF_INITIAL_SECONDS = 30L

    val workerWriteTimeoutMs = TimeUnit.SECONDS.toMillis(WORKER_WRITE_TIMEOUT_SECONDS)
    val flushTimeoutMs = TimeUnit.SECONDS.toMillis(FLUSH_TIMEOUT_SECONDS)
    val signOutSyncTimeoutMs = TimeUnit.SECONDS.toMillis(SIGN_OUT_SYNC_TIMEOUT_SECONDS)
    val tombstoneRetentionMs = TimeUnit.DAYS.toMillis(TOMBSTONE_RETENTION_DAYS)
    val reportQueryRefreshMs = TimeUnit.MINUTES.toMillis(REPORT_QUERY_REFRESH_MINUTES)
    val liveFeedGraceMs = TimeUnit.SECONDS.toMillis(LIVE_FEED_GRACE_SECONDS)
}

/** How a failed remote write should be handled. */
enum class RemoteErrorKind { RETRYABLE, PERMISSION_DENIED, INVALID }

class RemoteWriteException(val kind: RemoteErrorKind, message: String?) : Exception(message)

enum class ReportUploadDecision { UPLOAD, EXPIRE }

/** 4.12: a pending report already older than the window is never uploaded. */
fun reportUploadDecision(observedAt: Long, nowMs: Long, policy: ReportPolicy): ReportUploadDecision =
    if (nowMs - observedAt > policy.validityWindowMs) ReportUploadDecision.EXPIRE else ReportUploadDecision.UPLOAD

sealed interface ReportErrorOutcome {
    data object Retry : ReportErrorOutcome
    data object Expired : ReportErrorOutcome
    /** The write was refused; read the document to see whether it was already shared (4.10). */
    data object CheckExisting : ReportErrorOutcome
    data class Rejected(val reason: String) : ReportErrorOutcome
}

/** 4.10 and 4.11. */
fun reportErrorOutcome(error: RemoteWriteException, observedAt: Long, nowMs: Long, policy: ReportPolicy): ReportErrorOutcome =
    when (error.kind) {
        RemoteErrorKind.RETRYABLE -> ReportErrorOutcome.Retry
        RemoteErrorKind.PERMISSION_DENIED ->
            if (reportUploadDecision(observedAt, nowMs, policy) == ReportUploadDecision.EXPIRE) ReportErrorOutcome.Expired
            else ReportErrorOutcome.CheckExisting
        RemoteErrorKind.INVALID -> ReportErrorOutcome.Rejected(error.message ?: "Rejected by the server")
    }

/** 4.10: a refused create is a duplicate of our own earlier upload when the stored author is us. */
fun isOwnEarlierUpload(storedAuthor: String?, currentUid: String, previousOwnerUid: String?): Boolean =
    storedAuthor != null && (storedAuthor == currentUid || storedAuthor == previousOwnerUid)

/** 1.6: the report query's lower bound is fixed within each refresh bucket, so resubscribing in a bucket reuses the query. */
fun reportQueryLowerBound(nowMs: Long, policy: ReportPolicy, refreshMs: Long = SyncPolicy.reportQueryRefreshMs): Long =
    nowMs - Math.floorMod(nowMs, refreshMs) - policy.validityWindowMs

/** 4.21: the server lets clients read reports this far back, so every bucketed query is allowed. */
fun reportReadWindowMs(policy: ReportPolicy, refreshMs: Long = SyncPolicy.reportQueryRefreshMs): Long =
    policy.validityWindowMs + refreshMs + policy.clockSkewAllowanceMs

sealed interface FeedStatus {
    data object Live : FeedStatus
    data object Loading : FeedStatus
    data object PausedForToday : FeedStatus
    data class Stale(val lastServerSnapshotAt: Long) : FeedStatus
    data object NeverLoaded : FeedStatus
}

data class FeedSignals(
    val quotaExhausted: Boolean,
    val lastSnapshotFromServer: Boolean,
    val listenerStartedAt: Long,
    val lastServerSnapshotAt: Long?
)

/** 1.5: one message, in priority order. */
fun feedStatus(signals: FeedSignals, nowMs: Long, graceMs: Long = SyncPolicy.liveFeedGraceMs): FeedStatus = when {
    signals.quotaExhausted -> FeedStatus.PausedForToday
    signals.lastSnapshotFromServer -> FeedStatus.Live
    nowMs - signals.listenerStartedAt < graceMs -> FeedStatus.Loading
    signals.lastServerSnapshotAt != null -> FeedStatus.Stale(signals.lastServerSnapshotAt)
    else -> FeedStatus.NeverLoaded
}
