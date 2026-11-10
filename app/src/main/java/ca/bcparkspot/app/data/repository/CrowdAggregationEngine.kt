package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.CommunityReport
import ca.bcparkspot.app.model.CrowdConsensus
import ca.bcparkspot.app.model.CrowdFreshness

object CrowdAggregationEngine {

    const val ACTIVE_WINDOW_MS = 90 * 60 * 1000L        // 90 minutes (Fresh)
    const val MAX_VALIDITY_WINDOW_MS = 180 * 60 * 1000L  // 180 minutes (3 hours, Hard Expiration)
    const val MIN_QUORUM_USERS = 2                      // Quorum requirement: at least 2 distinct users

    fun computeConsensus(
        lotId: String,
        reports: List<CommunityReport>,
        nowMs: Long = System.currentTimeMillis()
    ): CrowdConsensus? {
        val lotReports = reports.filter { it.lotId == lotId }
        if (lotReports.isEmpty()) return null

        // 1. Filter out reports older than 180 minutes
        val validReports = lotReports.filter {
            val ageMs = (nowMs - it.timestampMs).coerceAtLeast(0)
            ageMs <= MAX_VALIDITY_WINDOW_MS
        }
        if (validReports.isEmpty()) {
            return CrowdConsensus(
                lotId = lotId,
                status = AvailabilityStatus.UNKNOWN,
                freshness = CrowdFreshness.EXPIRED,
                distinctHikerCount = 0,
                latestReportMinutesAgo = 999
            )
        }

        // 2. Anti-spam: Deduplicate by userId, keeping each distinct user's most recent report
        val distinctUserReports = validReports
            .sortedByDescending { it.timestampMs }
            .distinctBy { if (it.userId.isNotBlank()) it.userId else it.id }

        val mostRecent = distinctUserReports.maxByOrNull { it.timestampMs } ?: return null
        val latestAgeMs = (nowMs - mostRecent.timestampMs).coerceAtLeast(0)
        val latestAgeMinutes = latestAgeMs / (60 * 1000L)

        val freshness = if (latestAgeMs <= ACTIVE_WINDOW_MS) {
            CrowdFreshness.ACTIVE
        } else {
            CrowdFreshness.STALE
        }

        // 3. Quorum check: A status should ONLY win when at least 2 distinct users report that same status.
        // Otherwise, if no status reaches quorum (e.g. 1 CLOSED + 1 AVAILABLE), status remains UNKNOWN.
        val usersPerStatus = distinctUserReports
            .filter { it.statusReported != AvailabilityStatus.UNKNOWN }
            .groupBy { it.statusReported }
            .mapValues { entry -> entry.value.map { it.userId.ifBlank { it.id } }.distinct().size }

        val eligibleStatuses = usersPerStatus.filter { it.value >= MIN_QUORUM_USERS }.keys

        if (eligibleStatuses.isEmpty()) {
            return CrowdConsensus(
                lotId = lotId,
                status = AvailabilityStatus.UNKNOWN,
                freshness = freshness,
                distinctHikerCount = distinctUserReports.size,
                latestReportMinutesAgo = latestAgeMinutes,
                latestComment = mostRecent.comment.takeIf { it.isNotBlank() },
                spotsEstimate = mostRecent.spotsEstimate
            )
        }

        // 4. Time-weighted vote: recent reports carry highest weight among eligible statuses
        val weights = mutableMapOf<AvailabilityStatus, Double>()
        eligibleStatuses.forEach { weights[it] = 0.0 }

        distinctUserReports.forEach { rep ->
            if (rep.statusReported in eligibleStatuses) {
                val ageMs = (nowMs - rep.timestampMs).coerceAtLeast(0)
                val weight = 1.0 - (ageMs.toDouble() / MAX_VALIDITY_WINDOW_MS) * 0.7
                weights[rep.statusReported] = (weights[rep.statusReported] ?: 0.0) + weight
            }
        }

        val winningStatus = weights.maxByOrNull { it.value }?.key ?: AvailabilityStatus.UNKNOWN

        return CrowdConsensus(
            lotId = lotId,
            status = winningStatus,
            freshness = freshness,
            distinctHikerCount = distinctUserReports.size,
            latestReportMinutesAgo = latestAgeMinutes,
            latestComment = mostRecent.comment.takeIf { it.isNotBlank() },
            spotsEstimate = mostRecent.spotsEstimate
        )
    }
}
