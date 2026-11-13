package ca.bcparkspot.app.ui.prediction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.domain.GateToday
import ca.bcparkspot.app.domain.LotCandidate
import ca.bcparkspot.app.domain.LotState
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.Prediction
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.PassRequirementStatus
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
    fun `today's tiles give short values and a colour that matches their meaning`() {
        fun gate(g: GateToday) = gateText(res, g, Locale.US).let { it.copy(value = it.value.replace('\u202F', ' '), note = it.note?.replace('\u202F', ' ')) }
        assertEquals(FactText("8 AM – 4:30 PM"), gate(GateToday.Hours(LocalTime.of(8, 0), LocalTime.of(16, 30))))
        assertEquals(FactText("From 7 AM", "Can close by 6 PM"), gate(GateToday.ClosesAsEarlyAs(LocalTime.of(7, 0), LocalTime.of(18, 0))))
        assertEquals(FactText("Not published"), gate(GateToday.NoHoursPublished))
        val hours = GateToday.Hours(LocalTime.of(7, 0), LocalTime.of(18, 0))
        assertEquals(FactTone.GOOD, gateTone(hours, openNow = true))
        assertEquals("a gate shut right now never looks green", FactTone.SHUT, gateTone(hours, openNow = false))
        assertEquals(FactTone.GOOD, gateTone(GateToday.OpenAllDay, openNow = true))
        assertEquals(FactTone.HEADS_UP, gateTone(GateToday.ClosesAsEarlyAs(LocalTime.of(7, 0), LocalTime.of(18, 0)), openNow = true))
        assertEquals(FactTone.SHUT, gateTone(GateToday.ClosedAllDay, openNow = false))
        assertEquals(FactTone.UNKNOWN, gateTone(GateToday.NoHoursPublished, openNow = true))
        assertEquals(FactText("Not needed now"), passText(res, PassRequirementStatus.NOT_REQUIRED))
        assertEquals(FactTone.HEADS_UP, passTone(PassRequirementStatus.REQUIRED))
        assertEquals(
            PassRequirementStatus.entries.size,
            PassRequirementStatus.entries.map { passText(res, it).value }.toSet().size
        )
    }

    @Test
    fun `compact times drop whole-hour minutes on a 12-hour clock only`() {
        assertEquals("8 AM", formatCompactClockTime(LocalTime.of(8, 0), Locale.US).replace('\u202F', ' '))
        assertEquals("4:30 PM", formatCompactClockTime(LocalTime.of(16, 30), Locale.US).replace('\u202F', ' '))
        assertEquals("08:00", formatCompactClockTime(LocalTime.of(8, 0), Locale.GERMANY))
    }

    @Test
    fun `only gate outlooks drop their detail beside the gate tile`() {
        val dropped = Outlook.entries.filter { detailRepeatsGateTile(it) }.toSet()
        assertEquals(setOf(Outlook.GATE_CLOSED, Outlook.GATE_MAY_BE_CLOSED, Outlook.GATE_CLOSING_SOON), dropped)
    }

    @Test
    fun `a shut gate or closure looks shut and every other public line is a heads-up`() {
        assertEquals(FactTone.SHUT, outlookTone(Outlook.GATE_CLOSED))
        assertEquals(FactTone.SHUT, outlookTone(Outlook.PARK_CLOSED))
        assertEquals(FactTone.HEADS_UP, outlookTone(Outlook.GATE_CLOSING_SOON))
    }

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
            "Vancouver Trails: arrive by 7:00 AM on summer weekends",
            patternDetail(res, early, Locale.US).replace(' ', ' ')
        )
    }
}
