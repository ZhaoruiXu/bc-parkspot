package ca.bcparkspot.app.sync

import ca.bcparkspot.app.domain.ReportPolicies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRulesTest {
    private val policy = ReportPolicies.DEFAULT
    private val window = policy.validityWindowMs
    private val refresh = SyncPolicy.reportQueryRefreshMs
    private val now = window * 10 + refresh / 3

    private fun error(kind: RemoteErrorKind, message: String? = "why") = RemoteWriteException(kind, message)

    @Test
    fun `a pending report inside the window uploads, one past it expires`() {
        assertEquals(ReportUploadDecision.UPLOAD, reportUploadDecision(now - window, now, policy))
        assertEquals(ReportUploadDecision.EXPIRE, reportUploadDecision(now - window - 1, now, policy))
    }

    @Test
    fun `retryable errors retry`() {
        assertEquals(ReportErrorOutcome.Retry, reportErrorOutcome(error(RemoteErrorKind.RETRYABLE), now, now, policy))
    }

    @Test
    fun `permission denied inside the window checks for an earlier upload, outside it expires`() {
        assertEquals(ReportErrorOutcome.CheckExisting, reportErrorOutcome(error(RemoteErrorKind.PERMISSION_DENIED), now, now, policy))
        assertEquals(ReportErrorOutcome.Expired, reportErrorOutcome(error(RemoteErrorKind.PERMISSION_DENIED), now - window - 1, now, policy))
    }

    @Test
    fun `invalid data is rejected with the server's reason`() {
        assertEquals(ReportErrorOutcome.Rejected("bad"), reportErrorOutcome(error(RemoteErrorKind.INVALID, "bad"), now, now, policy))
        assertTrue(reportErrorOutcome(error(RemoteErrorKind.INVALID, null), now, now, policy) is ReportErrorOutcome.Rejected)
    }

    @Test
    fun `an existing report is ours when its author is the current or the previous uid`() {
        assertTrue(isOwnEarlierUpload("me", "me", null))
        assertTrue(isOwnEarlierUpload("guest", "me", "guest"))
        assertFalse(isOwnEarlierUpload("someone", "me", "guest"))
        assertFalse(isOwnEarlierUpload(null, "me", null))
    }

    @Test
    fun `query lower bound floors to the refresh bucket`() {
        val bucketStart = now - Math.floorMod(now, refresh)
        assertEquals(bucketStart - window, reportQueryLowerBound(now, policy))
    }

    @Test
    fun `two subscriptions in one bucket produce an identical query`() {
        val bucketStart = now - Math.floorMod(now, refresh)
        assertEquals(reportQueryLowerBound(bucketStart, policy), reportQueryLowerBound(bucketStart + refresh - 1, policy))
        assertTrue(reportQueryLowerBound(bucketStart + refresh, policy) > reportQueryLowerBound(bucketStart, policy))
    }

    @Test
    fun `the read window covers every bucketed query`() {
        val readWindow = reportReadWindowMs(policy)
        val worstCaseNow = now - Math.floorMod(now, refresh) + refresh - 1
        assertTrue(worstCaseNow - reportQueryLowerBound(worstCaseNow, policy) <= readWindow - policy.clockSkewAllowanceMs)
        assertEquals(window + refresh + policy.clockSkewAllowanceMs, readWindow)
    }

    private fun signals(quota: Boolean = false, fromServer: Boolean = false, started: Long = now, last: Long? = null) =
        FeedSignals(quota, fromServer, started, last)

    @Test
    fun `feed status follows its priority order`() {
        val grace = SyncPolicy.liveFeedGraceMs
        assertEquals(FeedStatus.PausedForToday, feedStatus(signals(quota = true, fromServer = true), now))
        assertEquals(FeedStatus.Live, feedStatus(signals(fromServer = true), now))
        assertEquals(FeedStatus.Loading, feedStatus(signals(started = now), now + grace - 1))
        assertEquals(FeedStatus.Stale(now - grace), feedStatus(signals(started = now, last = now - grace), now + grace))
        assertEquals(FeedStatus.NeverLoaded, feedStatus(signals(started = now), now + grace))
    }
}
