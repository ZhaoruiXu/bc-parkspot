package ca.bcparkspot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.ZonedDateTime

class PredictionTest {
    private val arriveBy = LocalTime.of(7, 0)
    private val guide = BusyAdvice(arriveBy, false, "https://guide.example")
    private val news = BusyAdvice(null, true, "https://news.example")
    private val opens = LocalTime.of(6, 0)
    private val closes = LocalTime.of(21, 0)
    private val gate = GateHours.daily(opens, closes)

    private val summerSaturday = LocalDate.of(2026, 7, 11)
    private val summerTuesday = LocalDate.of(2026, 7, 14)
    private val septemberSaturday = LocalDate.of(2026, 9, 12)
    private val winterTuesday = LocalDate.of(2026, 1, 13)
    private val midday = LocalTime.NOON

    private fun facts(advice: BusyAdvice? = null, closed: Boolean = false, pass: Boolean = false, gate: GateHours? = null) =
        LotFacts(advice, gate, closed, pass)
    private fun at(date: LocalDate, time: LocalTime = midday) = LocalDateTime.of(date, time)

    @Test
    fun `no source and no rule means nothing is claimed`() {
        assertNull(predictOutlook(facts(), at(summerSaturday)))
        assertNull(predictOutlook(facts(gate = gate), at(summerSaturday)))
    }

    @Test
    fun `the arrive-by advice applies on summer weekends only, all day, citing the guide`() {
        val p = predictOutlook(facts(guide), at(summerSaturday, LocalTime.of(20, 0)))
        assertEquals(Prediction(Outlook.ARRIVE_BY, guide.sourceUrl, arriveBy = arriveBy), p)
        assertNull(predictOutlook(facts(guide), at(summerTuesday)))
        assertNull(predictOutlook(facts(guide), at(septemberSaturday)))
    }

    @Test
    fun `busy summer season applies in the summer months only, citing the news report`() {
        assertEquals(Prediction(Outlook.SUMMER_BUSY, news.sourceUrl), predictOutlook(facts(news), at(summerTuesday)))
        assertNull(predictOutlook(facts(news), at(septemberSaturday)))
    }

    @Test
    fun `a pass day comes from the lot's bundled rules and carries no web link`() {
        assertEquals(Prediction(Outlook.PASS_DAY, null), predictOutlook(facts(pass = true), at(winterTuesday)))
    }

    @Test
    fun `the most decisive fact wins - closure, then gate, then arrive-by, then pass day, then summer`() {
        val all = facts(guide.copy(busyInSummer = true), closed = true, pass = true, gate = gate)
        assertEquals(Outlook.PARK_CLOSED, predictOutlook(all, at(summerSaturday))?.outlook)
        assertNull("closures come from the app's own lot details", predictOutlook(all, at(summerSaturday))?.sourceUrl)
        val night = at(summerSaturday, closes)
        val gated = predictOutlook(all.copy(parkClosed = false), night)
        assertEquals(Outlook.GATE_CLOSED, gated?.outlook)
        assertEquals(opens, gated?.gateOpens)
        assertNull("gate hours come from the app's own lot details, not a web page", gated?.sourceUrl)
        assertEquals(Outlook.ARRIVE_BY, predictOutlook(all.copy(parkClosed = false), at(summerSaturday))?.outlook)
        assertEquals(Outlook.PASS_DAY, predictOutlook(all.copy(parkClosed = false), at(summerTuesday))?.outlook)
        assertEquals(Outlook.SUMMER_BUSY, predictOutlook(all.copy(parkClosed = false, passRequired = false), at(summerTuesday))?.outlook)
    }

    @Test
    fun `only a closure or a gate that is, may be or is about to be shut blocks a lot`() {
        assertEquals(setOf(Outlook.PARK_CLOSED, Outlook.GATE_CLOSED, Outlook.GATE_MAY_BE_CLOSED, Outlook.GATE_CLOSING_SOON), BLOCKING_OUTLOOKS)
    }

    @Test
    fun `summer weekends are Saturdays and Sundays in June, July and August`() {
        assertTrue(isSummerWeekend(summerSaturday))
        assertTrue(isSummerWeekend(summerSaturday.plusDays(1)))
        assertFalse(isSummerWeekend(summerTuesday))
        assertFalse(isSummerWeekend(septemberSaturday))
    }

    private val winterEarly = LocalTime.of(17, 30)
    private val seasonalGate = GateHours(listOf(
        GateSeason(MonthDay.of(Month.FEBRUARY, 1), GateHoursForDay(opens, closes)),
        GateSeason(MonthDay.of(Month.JUNE, 1), null),
        GateSeason(MonthDay.of(Month.DECEMBER, 1), GateHoursForDay(opens, winterEarly))
    ))

    @Test
    fun `gate hours are open from opening up to but not including closing, and absent means always open`() {
        assertTrue(gate.isOpenAt(at(summerTuesday, opens)))
        assertFalse(gate.isOpenAt(at(summerTuesday, closes)))
        assertFalse(gate.isOpenAt(at(summerTuesday, opens.minusMinutes(1))))
        assertTrue((null as GateHours?).isOpenAt(at(summerTuesday, LocalTime.MIDNIGHT)))
    }

    @Test
    fun `each gate season applies until the next and the last wraps over the new year`() {
        assertFalse("January wraps to the December season", seasonalGate.isOpenAt(at(winterTuesday, winterEarly)))
        assertEquals(winterEarly, seasonalGate.hoursOn(winterTuesday)?.closes)
        assertEquals(closes, seasonalGate.hoursOn(LocalDate.of(2026, Month.MARCH, 1))?.closes)
        assertNull("an all-day season has no hours", seasonalGate.hoursOn(summerTuesday))
        assertTrue(seasonalGate.isOpenAt(at(summerTuesday, LocalTime.MIDNIGHT)))
    }

    @Test
    fun `a gate that closes earlier on unpublished dates may be closed from its earliest closing time`() {
        val varies = GateHours.daily(opens, closes, earliestClose = winterEarly)
        val evening = at(summerTuesday, winterEarly)
        assertFalse(varies.mayBeClosedAt(at(summerTuesday, winterEarly.minusMinutes(1))))
        assertTrue(varies.mayBeClosedAt(evening))
        assertFalse("past the latest close it is simply shut", varies.mayBeClosedAt(at(summerTuesday, closes)))
        assertFalse((null as GateHours?).mayBeClosedAt(evening))
        assertEquals(
            Prediction(Outlook.GATE_MAY_BE_CLOSED, null, gateClosesAsEarlyAs = winterEarly),
            predictOutlook(facts(gate = varies), evening)
        )
        assertTrue(Outlook.GATE_MAY_BE_CLOSED in BLOCKING_OUTLOOKS)
    }

    @Test
    fun `a gate closing within the minimum time on site is closing soon and blocks the lot`() {
        val justInside = closes.minus(MIN_TIME_BEFORE_GATE_CLOSES)
        assertEquals(
            Prediction(Outlook.GATE_CLOSING_SOON, null, gateCloses = closes),
            predictOutlook(facts(gate = gate), at(summerTuesday, justInside))
        )
        assertNull(gate.closingSoonAt(at(summerTuesday, justInside.minusMinutes(1))))
        assertNull("a shut gate is closed, not closing soon", gate.closingSoonAt(at(summerTuesday, closes)))
        assertTrue(Outlook.GATE_CLOSING_SOON in BLOCKING_OUTLOOKS)
    }

    @Test
    fun `an unpublished closing time never counts as closing soon, but an earliest close does`() {
        val unknown = GateHours.daily(opens, CLOSING_TIME_NOT_PUBLISHED, earliestClose = winterEarly)
        assertEquals(winterEarly, unknown.closingSoonAt(at(summerTuesday, winterEarly.minusMinutes(1))))
        assertNull(GateHours.daily(opens, CLOSING_TIME_NOT_PUBLISHED).closingSoonAt(at(summerTuesday, LocalTime.of(23, 30))))
    }

    @Test
    fun `a season closed all day keeps the gate shut and names no opening time`() {
        val snow = GateHours(listOf(GateSeason(MonthDay.of(Month.JANUARY, 1), null, closedAllDay = true)))
        assertFalse(snow.isOpenAt(at(winterTuesday)))
        assertEquals(Prediction(Outlook.GATE_CLOSED, null), predictOutlook(facts(gate = snow), at(winterTuesday)))
    }

    @Test
    fun `BC local time is read in BC wherever the phone is`() {
        val bcMorning = ZonedDateTime.of(summerSaturday, arriveBy, BC_TIME_ZONE)
        assertEquals(bcMorning.toLocalDateTime(), bcLocalTime(bcMorning.toInstant().toEpochMilli()))
    }
}
