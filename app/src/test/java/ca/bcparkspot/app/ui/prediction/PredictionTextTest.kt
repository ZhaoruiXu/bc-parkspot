package ca.bcparkspot.app.ui.prediction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.domain.LotCandidate
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.model.AvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PredictionTextTest {
    private val res = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `a fallback pick shows what sources say after its report status`() {
        val busy = LotCandidate("lot", "park", 1, LotState.NoRecentReports, Outlook.PASS_DAY)
        assertEquals("No recent reports; Day-use pass day", fallbackStatus(res, busy))
        assertEquals("No recent reports", fallbackStatus(res, busy.copy(outlook = null)))
    }

    @Test
    fun `a fallback pick with a real report never adds source advice`() {
        val reported = LotCandidate("lot", "park", 1, LotState.Unconfirmed(AvailabilityStatus.FILLING_FAST, 1, 0), Outlook.SUMMER_BUSY)
        assertEquals("1 report, not confirmed: Filling Fast", fallbackStatus(res, reported))
    }

    @Test
    fun `detail lines format their times`() {
        val gated = Prediction(Outlook.GATE_CLOSED, "https://example.org", gateOpens = LocalTime.of(7, 0))
        assertEquals("Gate usually opens at 7:00 AM", patternDetail(res, gated, Locale.US).replace(' ', ' '))
        val early = Prediction(Outlook.ARRIVE_BY, "https://example.org", arriveBy = LocalTime.of(7, 0))
        assertEquals(
            "Vancouver Trails says to arrive by 7:00 AM on summer weekends, before lots fill",
            patternDetail(res, early, Locale.US).replace(' ', ' ')
        )
    }
}
