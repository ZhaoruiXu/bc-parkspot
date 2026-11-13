package ca.bcparkspot.app.ui.prediction

import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.Prediction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class PredictionUiTest {
    private val sevenAm = LocalTime.of(7, 0)
    private val source = "https://www.example.org/page"

    @Test
    fun `each outlook's detail line restates its source, with the time where there is one`() {
        assertEquals(DetailText(R.string.pattern_park_closed), patternDetailText(Prediction(Outlook.PARK_CLOSED, source)))
        assertEquals(DetailText(R.string.pattern_gate_opens, sevenAm), patternDetailText(Prediction(Outlook.GATE_CLOSED, source, gateOpens = sevenAm)))
        assertEquals(DetailText(R.string.pattern_arrive_by, sevenAm), patternDetailText(Prediction(Outlook.ARRIVE_BY, source, arriveBy = sevenAm)))
        assertEquals(DetailText(R.string.pattern_pass_day), patternDetailText(Prediction(Outlook.PASS_DAY, source)))
        assertEquals(DetailText(R.string.pattern_summer_busy), patternDetailText(Prediction(Outlook.SUMMER_BUSY, source)))
    }

    @Test
    fun `every outlook has its own label and icon`() {
        assertEquals(Outlook.entries.size, Outlook.entries.map(::outlookLabelRes).toSet().size)
        assertEquals(Outlook.entries.size, Outlook.entries.map(::outlookIcon).toSet().size)
    }

    @Test
    fun `a source is shown by its site name`() {
        assertEquals("example.org", sourceHost(source))
        assertEquals("not a url", sourceHost("not a url"))
    }

    @Test
    fun `clock times follow the locale`() {
        assertEquals("7:00 AM", formatClockTime(sevenAm, Locale.US).replace(' ', ' '))
    }
}
