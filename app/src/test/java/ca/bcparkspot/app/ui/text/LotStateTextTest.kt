package ca.bcparkspot.app.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.model.AvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LotStateTextTest {
    private val res = ApplicationProvider.getApplicationContext<Context>().resources
    private val policy = ReportPolicies.DEFAULT
    private val twelveMinutes = TimeUnit.MINUTES.toMillis(12)

    @Test
    fun `single report reads as not confirmed`() {
        assertEquals("1 report, not confirmed: Full", stateHeadline(res, LotState.Unconfirmed(AvailabilityStatus.FULL, 1, 0)))
    }

    @Test
    fun `several reports use the plural`() {
        val state = LotState.Mixed(AvailabilityStatus.AVAILABLE, 3, twelveMinutes)
        assertEquals("Mixed reports, latest: Available", stateHeadline(res, state))
        assertEquals("3 people reported, newest 12 min ago", stateDetail(res, state, policy))
    }

    @Test
    fun `no reports names the window`() {
        assertEquals("No recent reports", stateHeadline(res, LotState.NoRecentReports))
        assertEquals("No crowd reports in the last ${ReportPolicies.VALIDITY_WINDOW_HOURS} hours", stateDetail(res, LotState.NoRecentReports, policy))
    }

    @Test
    fun `card summary does not repeat itself`() {
        assertEquals("No crowd reports in the last ${ReportPolicies.VALIDITY_WINDOW_HOURS} hours", stateSummary(res, LotState.NoRecentReports, policy))
        assertEquals("1 report, not confirmed: Full · 1 person reported, newest 12 min ago", stateSummary(res, LotState.Unconfirmed(AvailabilityStatus.FULL, 1, twelveMinutes), policy))
    }

    @Test
    fun `ages read as just now, minutes and hours`() {
        assertEquals("just now", formatAge(res, 0))
        assertEquals("12 min ago", formatAge(res, twelveMinutes))
        assertEquals("1 hour ago", formatAge(res, TimeUnit.HOURS.toMillis(1)))
        assertEquals("2 hours ago", formatAge(res, TimeUnit.HOURS.toMillis(2)))
    }

    @Test
    fun `the no-confirmed message is rendered from the policy constants`() {
        assertEquals(
            "No confirmed open spots. A lot counts as open when at least ${ReportPolicies.MIN_QUORUM_USERS} people report it open within ${ReportPolicies.VALIDITY_WINDOW_HOURS} hours, they outnumber full or closed reports, and no newer report says full or closed.",
            noConfirmedMessage(res, policy)
        )
    }
}
