package ca.bcparkspot.app.domain

import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.AvailabilityStatus.AVAILABLE
import ca.bcparkspot.app.model.AvailabilityStatus.CLOSED
import ca.bcparkspot.app.model.AvailabilityStatus.FILLING_FAST
import ca.bcparkspot.app.model.AvailabilityStatus.FULL
import ca.bcparkspot.app.model.AvailabilityStatus.UNKNOWN
import ca.bcparkspot.app.model.CommunityReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LotStatusTest {
    private val policy = ReportPolicies.DEFAULT
    private val now = policy.validityWindowMs * 10
    private val minute = 60_000L
    private var seq = 0

    private fun report(user: String, status: AvailabilityStatus, ageMs: Long, comment: String = "note") =
        CommunityReport(
            id = "r${seq++}",
            userId = user,
            lotId = "lot",
            reporterName = user,
            statusReported = status,
            comment = comment,
            timestampMs = now - ageMs
        )

    @Test
    fun `no reports is NoRecentReports`() {
        assertEquals(LotState.NoRecentReports, classifyLot(emptyList(), now, policy))
    }

    @Test
    fun `one report is unconfirmed with its status, count and age`() {
        val state = classifyLot(listOf(report("a", FULL, 12 * minute)), now, policy)
        assertEquals(LotState.Unconfirmed(FULL, 1, 12 * minute), state)
        assertTrue(state.leansClosed())
        assertFalse(state.isOfferable())
    }

    @Test
    fun `two users on the open side confirm the cautious status`() {
        val state = classifyLot(listOf(report("a", AVAILABLE, 20 * minute), report("b", FILLING_FAST, 5 * minute)), now, policy)
        assertEquals(LotState.Confirmed(FILLING_FAST, 2, 5 * minute), state)
        assertTrue(state.isConfirmedOpen())
    }

    @Test
    fun `two users on the closed side confirm closed when either says closed`() {
        val state = classifyLot(listOf(report("a", FULL, 20 * minute), report("b", CLOSED, 30 * minute)), now, policy)
        assertEquals(LotState.Confirmed(CLOSED, 2, 20 * minute), state)
        assertFalse(state.isOfferable())
    }

    @Test
    fun `latest report per user only, so one user never meets quorum alone`() {
        val reports = (1..5).map { report("a", AVAILABLE, it * minute) }
        assertEquals(LotState.Unconfirmed(AVAILABLE, 1, minute), classifyLot(reports, now, policy))
    }

    @Test
    fun `a user who changes their mind counts only with the newer status`() {
        val reports = listOf(report("a", AVAILABLE, 60 * minute), report("a", FULL, 10 * minute), report("b", FULL, 20 * minute))
        assertEquals(LotState.Confirmed(FULL, 2, 10 * minute), classifyLot(reports, now, policy))
    }

    @Test
    fun `a newer report from the other side blocks confirmation`() {
        val reports = listOf(report("a", AVAILABLE, 60 * minute), report("b", AVAILABLE, 50 * minute), report("c", FULL, 5 * minute))
        assertEquals(LotState.Mixed(FULL, 3, 5 * minute), classifyLot(reports, now, policy))
    }

    @Test
    fun `an older minority report does not block confirmation`() {
        val reports = listOf(report("c", FULL, 90 * minute), report("a", AVAILABLE, 20 * minute), report("b", AVAILABLE, 10 * minute))
        assertEquals(LotState.Confirmed(AVAILABLE, 3, 10 * minute), classifyLot(reports, now, policy))
    }

    @Test
    fun `a two against two tie is mixed even when the newest report is open`() {
        val reports = listOf(
            report("a", AVAILABLE, 10 * minute), report("b", AVAILABLE, 40 * minute),
            report("c", FULL, 20 * minute), report("d", FULL, 30 * minute)
        )
        val state = classifyLot(reports, now, policy)
        assertEquals(LotState.Mixed(AVAILABLE, 4, 10 * minute), state)
        assertFalse(state.isOfferable())
    }

    @Test
    fun `an even split is mixed and never offered`() {
        val reports = listOf(report("a", AVAILABLE, 20 * minute), report("b", FULL, 30 * minute))
        val state = classifyLot(reports, now, policy)
        assertEquals(LotState.Mixed(AVAILABLE, 2, 20 * minute), state)
        assertFalse(state.isOfferable())
    }

    @Test
    fun `reports older than the validity window never count`() {
        val reports = listOf(report("a", AVAILABLE, policy.validityWindowMs + 1), report("b", AVAILABLE, policy.validityWindowMs + minute))
        assertEquals(LotState.NoRecentReports, classifyLot(reports, now, policy))
    }

    @Test
    fun `a report exactly at the window edge still counts`() {
        assertEquals(1, classifyLot(listOf(report("a", AVAILABLE, policy.validityWindowMs)), now, policy).reporterCount)
    }

    @Test
    fun `reports from beyond the clock skew allowance in the future are ignored`() {
        val future = report("a", AVAILABLE, -(policy.clockSkewAllowanceMs + 1))
        val nearFuture = report("b", AVAILABLE, -policy.clockSkewAllowanceMs)
        val state = classifyLot(listOf(future, nearFuture), now, policy)
        assertEquals(1, state.reporterCount)
        assertEquals(0L, state.newestAgeMs)
    }

    @Test
    fun `unknown status reports are ignored`() {
        assertEquals(LotState.NoRecentReports, classifyLot(listOf(report("a", UNKNOWN, minute)), now, policy))
    }

    @Test
    fun `freshness uses the active window`() {
        assertTrue(LotState.Unconfirmed(AVAILABLE, 1, policy.activeWindowMs).isFresh(policy))
        assertFalse(LotState.Unconfirmed(AVAILABLE, 1, policy.activeWindowMs + 1).isFresh(policy))
        assertFalse(LotState.NoRecentReports.isFresh(policy))
    }

    @Test
    fun `only confirmed lots report a confirmed status`() {
        assertEquals(UNKNOWN, LotState.Unconfirmed(AVAILABLE, 1, 0).confirmedStatus)
        assertEquals(AVAILABLE, LotState.Confirmed(AVAILABLE, 2, 0).confirmedStatus)
        assertNull(LotState.NoRecentReports.newestStatus)
    }

    @Test
    fun `summary carries the newest counted comment`() {
        val reports = listOf(report("a", AVAILABLE, 30 * minute, "older"), report("b", AVAILABLE, 5 * minute, ""), report("c", AVAILABLE, policy.validityWindowMs + 1, "expired"))
        assertEquals("older", crowdSummaryFor(reports, now, policy).latestComment)
    }
}
