package ca.bcparkspot.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ca.bcparkspot.app.domain.CLOSING_TIME_NOT_PUBLISHED
import ca.bcparkspot.app.domain.hoursOn
import ca.bcparkspot.app.domain.isOpenAt
import ca.bcparkspot.app.domain.mayBeClosedAt
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

class FillPatternsTest {
    private val lotIds = BCParksDataSource.getInitialParkingLots().map { it.id }.toSet()

    @Test
    fun `every pattern belongs to a real lot`() {
        val unknown = FillPatterns.BY_LOT.keys - lotIds
        assertTrue("patterns for unknown lots: $unknown", unknown.isEmpty())
    }

    @Test
    fun `every gate window belongs to a real lot`() {
        val unknown = GateWindows.BY_LOT.keys - lotIds
        assertTrue("gate hours for unknown lots: $unknown", unknown.isEmpty())
    }

    @Test
    fun `every piece of advice cites an https source and says something`() {
        FillPatterns.BY_LOT.forEach { (id, advice) ->
            assertTrue(id, advice.sourceUrl.startsWith("https://"))
            assertTrue(id, advice.arriveBySummerWeekends != null || advice.busyInSummer)
        }
    }

    private val clockTime = Regex("""(\d{1,2}):(\d{2}) (AM|PM)""")
    private fun parseClock(match: MatchResult): LocalTime {
        val (hour, minute, half) = match.destructured
        return LocalTime.parse("${hour.padStart(2, '0')}:$minute $half", DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
    }

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private fun shown(time: LocalTime) = time.format(clockFormat)

    @Test
    fun `Buntzen follows BC Hydro's 2026 closing table`() {
        val gate = GateWindows.BY_LOT.getValue("buntzen_lake_main")
        assertEquals(LocalTime.of(16, 30), gate.hoursOn(LocalDate.of(2026, Month.NOVEMBER, 12))?.closes)
        assertEquals(LocalTime.of(20, 0), gate.hoursOn(LocalDate.of(2026, Month.JULY, 1))?.closes)
        assertEquals(LocalTime.of(18, 30), gate.hoursOn(LocalDate.of(2026, Month.OCTOBER, 1))?.closes)
        assertEquals("the November row runs to January 31", LocalTime.of(16, 30), gate.hoursOn(LocalDate.of(2027, Month.JANUARY, 28))?.closes)
    }

    private fun closesOn(lotId: String, month: Month, day: Int): LocalTime? =
        GateWindows.BY_LOT.getValue(lotId).hoursOn(LocalDate.of(YEAR, month, day))?.closes
    private fun opensOn(lotId: String, month: Month, day: Int): LocalTime? =
        GateWindows.BY_LOT.getValue(lotId).hoursOn(LocalDate.of(YEAR, month, day))?.opens

    @Test
    fun `each gate table matches its operator's published hours`() {
        assertEquals("Golden Ears summer", LocalTime.of(23, 0), closesOn("golden_ears_alouette", Month.JULY, 1))
        assertEquals("Golden Ears winter", LocalTime.of(19, 0), closesOn("golden_ears_gold_creek", Month.NOVEMBER, 12))
        assertEquals("Seymour summer opens", LocalTime.of(6, 0), opensOn("seymour_lower_p1", Month.JULY, 1))
        assertEquals("Seymour winter opens", LocalTime.of(7, 0), opensOn("seymour_lower_p5", Month.JANUARY, 15))
        assertEquals("Chief winter campground gate", LocalTime.of(23, 0), closesOn("stawamus_chief_lot", Month.DECEMBER, 1))
        assertEquals("Shannon summer, the earlier of its two stated closings", LocalTime.of(22, 0), closesOn("shannon_falls_lot", Month.JULY, 1))
        assertEquals("Shannon winter", LocalTime.of(22, 0), closesOn("shannon_falls_lot", Month.JANUARY, 15))
        assertEquals("Alice midsummer", LocalTime.of(22, 0), closesOn("alice_lake_main", Month.JULY, 1))
        assertEquals("Belcarra now", LocalTime.of(18, 0), closesOn("belcarra_picnic_area", Month.NOVEMBER, 12))
        assertEquals("Belcarra summer exception", LocalTime.of(21, 0), closesOn("sasamat_white_pine", Month.JULY, 1))
        assertEquals("Belcarra January", LocalTime.of(17, 0), closesOn("belcarra_picnic_area", Month.JANUARY, 15))
    }

    @Test
    fun `Alice Lake's gate is closed all day through the snow season`() {
        val gate = GateWindows.BY_LOT.getValue("alice_lake_main")
        assertFalse(gate.isOpenAt(LocalDateTime.of(YEAR, Month.JANUARY, 15, 12, 0)))
        assertTrue(gate.isOpenAt(LocalDateTime.of(YEAR, Month.MARCH, 2, 12, 0)))
    }

    @Test
    fun `Lynn Canyon may be closed after 6 pm but is never claimed shut by unpublished hours`() {
        val gate = GateWindows.BY_LOT.getValue("lynn_canyon_park")
        val evening = LocalDateTime.of(YEAR, Month.JULY, 1, 20, 0)
        assertTrue(gate.isOpenAt(evening))
        assertTrue(gate.mayBeClosedAt(evening))
    }

    @Test
    fun `the gate hours text shows every time the gate windows use, and lots without a window say none is published`() {
        BCParksDataSource.getInitialParkingLots().forEach { lot ->
            val gate = GateWindows.BY_LOT[lot.id]
            if (gate == null) {
                assertTrue(lot.id, lot.gateHours.contains(NO_HOURS_PUBLISHED))
            } else {
                val times = (gate.seasons.flatMap { listOfNotNull(it.hours?.opens, it.hours?.closes) } + listOfNotNull(gate.earliestClose))
                    .filter { it != CLOSING_TIME_NOT_PUBLISHED }
                times.forEach { assertTrue("${lot.id} ${shown(it)}", lot.gateHours.contains(shown(it))) }
            }
        }
    }

    private companion object {
        const val YEAR = 2026
        const val NO_HOURS_PUBLISHED = "gate hours published"
    }
}
