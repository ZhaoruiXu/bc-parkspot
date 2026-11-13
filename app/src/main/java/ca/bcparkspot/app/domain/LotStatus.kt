package ca.bcparkspot.app.domain

import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.CommunityReport
import ca.bcparkspot.app.model.CrowdSummary
import java.util.concurrent.TimeUnit

data class ReportPolicy(
    val validityWindowMs: Long,
    val activeWindowMs: Long,
    val clockSkewAllowanceMs: Long,
    val minQuorumUsers: Int
)

object ReportPolicies {
    const val VALIDITY_WINDOW_HOURS = 3L
    const val ACTIVE_WINDOW_MINUTES = 90L
    const val CLOCK_SKEW_ALLOWANCE_MINUTES = 5L
    const val MIN_QUORUM_USERS = 2

    val DEFAULT = ReportPolicy(
        validityWindowMs = TimeUnit.HOURS.toMillis(VALIDITY_WINDOW_HOURS),
        activeWindowMs = TimeUnit.MINUTES.toMillis(ACTIVE_WINDOW_MINUTES),
        clockSkewAllowanceMs = TimeUnit.MINUTES.toMillis(CLOCK_SKEW_ALLOWANCE_MINUTES),
        minQuorumUsers = MIN_QUORUM_USERS
    )
}

enum class ReportSide { OPEN, CLOSED }

fun sideOf(status: AvailabilityStatus): ReportSide? = when (status) {
    AvailabilityStatus.AVAILABLE, AvailabilityStatus.FILLING_FAST -> ReportSide.OPEN
    AvailabilityStatus.FULL, AvailabilityStatus.CLOSED -> ReportSide.CLOSED
    AvailabilityStatus.UNKNOWN -> null
}

sealed interface LotState {
    val reporterCount: Int
    val newestAgeMs: Long?

    data class Confirmed(
        val status: AvailabilityStatus,
        override val reporterCount: Int,
        override val newestAgeMs: Long
    ) : LotState

    data class Mixed(
        val newestStatus: AvailabilityStatus,
        override val reporterCount: Int,
        override val newestAgeMs: Long
    ) : LotState

    data class Unconfirmed(
        val newestStatus: AvailabilityStatus,
        override val reporterCount: Int,
        override val newestAgeMs: Long
    ) : LotState

    data object NoRecentReports : LotState {
        override val reporterCount = 0
        override val newestAgeMs: Long? = null
    }
}

val LotState.newestStatus: AvailabilityStatus?
    get() = when (this) {
        is LotState.Confirmed -> status
        is LotState.Mixed -> newestStatus
        is LotState.Unconfirmed -> newestStatus
        LotState.NoRecentReports -> null
    }

val LotState.confirmedStatus: AvailabilityStatus
    get() = (this as? LotState.Confirmed)?.status ?: AvailabilityStatus.UNKNOWN

fun LotState.isConfirmedOpen(): Boolean =
    this is LotState.Confirmed && sideOf(status) == ReportSide.OPEN

fun LotState.leansClosed(): Boolean =
    (this is LotState.Unconfirmed || this is LotState.Mixed) && newestStatus?.let(::sideOf) == ReportSide.CLOSED

/** A lot that may be suggested to a driver: confirmed open, a lone open-side report, or no reports at all. */
fun LotState.isOfferable(): Boolean = when (this) {
    is LotState.Confirmed -> sideOf(status) == ReportSide.OPEN
    is LotState.Unconfirmed -> sideOf(newestStatus) == ReportSide.OPEN
    is LotState.Mixed -> false
    LotState.NoRecentReports -> true
}

fun LotState.isFresh(policy: ReportPolicy): Boolean =
    newestAgeMs?.let { it <= policy.activeWindowMs } ?: false

private fun reporterKey(report: CommunityReport): String = report.userId.ifBlank { report.id }

fun countedReports(reports: List<CommunityReport>, nowMs: Long, policy: ReportPolicy): List<CommunityReport> =
    reports
        .filter { sideOf(it.statusReported) != null }
        .filter { nowMs - it.timestampMs <= policy.validityWindowMs }
        .filter { it.timestampMs - nowMs <= policy.clockSkewAllowanceMs }
        .groupBy(::reporterKey)
        .map { (_, byUser) -> byUser.maxWith(compareBy<CommunityReport> { it.timestampMs }.thenBy { it.id }) }

private fun cautiousStatus(side: ReportSide, reports: List<CommunityReport>): AvailabilityStatus {
    val statuses = reports.map { it.statusReported }.toSet()
    return when (side) {
        ReportSide.OPEN -> if (AvailabilityStatus.FILLING_FAST in statuses) AvailabilityStatus.FILLING_FAST else AvailabilityStatus.AVAILABLE
        ReportSide.CLOSED -> if (AvailabilityStatus.CLOSED in statuses) AvailabilityStatus.CLOSED else AvailabilityStatus.FULL
    }
}

fun classifyLot(reports: List<CommunityReport>, nowMs: Long, policy: ReportPolicy = ReportPolicies.DEFAULT): LotState {
    val counted = countedReports(reports, nowMs, policy)
    if (counted.isEmpty()) return LotState.NoRecentReports

    val newest = counted.maxWith(compareBy<CommunityReport> { it.timestampMs }.thenBy { it.id })
    val newestAgeMs = (nowMs - newest.timestampMs).coerceAtLeast(0)
    val users = counted.size
    if (users < policy.minQuorumUsers) {
        return LotState.Unconfirmed(newest.statusReported, users, newestAgeMs)
    }

    val bySide = counted.groupBy { sideOf(it.statusReported)!! }
    val confirmedSide = ReportSide.entries.firstOrNull { side ->
        val mine = bySide[side].orEmpty()
        val other = bySide[ReportSide.entries.first { it != side }].orEmpty()
        val myNewest = mine.maxOfOrNull { it.timestampMs }
        myNewest != null &&
            mine.size >= policy.minQuorumUsers &&
            mine.size > other.size &&
            other.none { it.timestampMs > myNewest }
    }
    return if (confirmedSide != null) {
        LotState.Confirmed(cautiousStatus(confirmedSide, bySide.getValue(confirmedSide)), users, newestAgeMs)
    } else {
        LotState.Mixed(newest.statusReported, users, newestAgeMs)
    }
}

fun crowdSummaryFor(reports: List<CommunityReport>, nowMs: Long, policy: ReportPolicy = ReportPolicies.DEFAULT): CrowdSummary {
    val newestComment = countedReports(reports, nowMs, policy)
        .sortedWith(compareByDescending<CommunityReport> { it.timestampMs }.thenByDescending { it.id })
        .firstOrNull { it.comment.isNotBlank() }
        ?.comment
    return CrowdSummary(classifyLot(reports, nowMs, policy), newestComment)
}
