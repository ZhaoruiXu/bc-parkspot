package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.BC_TIME_ZONE
import ca.bcparkspot.app.domain.BLOCKING_OUTLOOKS
import ca.bcparkspot.app.domain.GateToday
import ca.bcparkspot.app.domain.NearestResult
import ca.bcparkspot.app.domain.Outlook
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.model.ParkingLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

class ParkingRepositoryNearestTest {
    private fun bcMs(at: LocalDateTime) = ZonedDateTime.of(at, BC_TIME_ZONE).toInstant().toEpochMilli()
    private val lots = BCParksDataSource.getInitialParkingLots()
    private fun lot(id: String): ParkingLot = lots.first { it.id == id }

    private val summerSaturday = LocalDate.of(2026, 7, 11)
    private val joffreClosureDay = LocalDate.of(2026, 9, 12)
    private val winterWeeknight = LocalDate.of(2026, 11, 12)
    /** Mount Seymour's summer gate opens at 6 am. */
    private val beforeSeymourGate = LocalTime.of(5, 30)
    private val SEYMOUR = "seymour_lower_p1"

    @Test
    fun `lot facts come from the bundled gate hours, closures, pass rules and advice`() {
        val seymour = lot(SEYMOUR)
        assertEquals(Outlook.GATE_CLOSED, seymour.predictionAt(LocalDateTime.of(summerSaturday, beforeSeymourGate))?.outlook)
        assertEquals(Outlook.ARRIVE_BY, seymour.predictionAt(LocalDateTime.of(summerSaturday, LocalTime.NOON))?.outlook)
        assertNull("no gate hours are published for Joffre", lot("joffre_lakes_main").predictionAt(LocalDateTime.of(winterWeeknight, LocalTime.MIDNIGHT)))
        assertEquals(Outlook.PARK_CLOSED, lot("joffre_lakes_main").predictionAt(LocalDateTime.of(joffreClosureDay, LocalTime.NOON))?.outlook)
        assertNull(lot("stawamus_chief_lot").predictionAt(LocalDateTime.of(winterWeeknight, LocalTime.NOON)))
    }

    @Test
    fun `the pick is never blocked when the driver would arrive, and it opens a route`() {
        listOf(
            LocalDateTime.of(winterWeeknight, LocalTime.of(2, 0)),
            LocalDateTime.of(summerSaturday, LocalTime.of(19, 45)),
            LocalDateTime.of(joffreClosureDay, LocalTime.NOON)
        ).forEach { now ->
            val repo = ParkingRepository(ReportPolicies.DEFAULT) { bcMs(now) }
            val origin = repo.currentUserLocation.value
            val (result, route) = repo.findNearestOpenSpot(origin)
            assertFalse(now.toString(), result is NearestResult.NothingQualifies)
            val picked = lot(route!!.destinationLot.id)
            val arrival = now.plusMinutes(BCParksDataSource.estimateDriveMinutes(origin, picked).toLong())
            assertFalse(now.toString(), picked.predictionAt(arrival)?.outlook in BLOCKING_OUTLOOKS)
        }
    }

    @Test
    fun `every lot carries today's published gate and pass facts`() {
        val now = LocalDateTime.of(winterWeeknight, LocalTime.NOON)
        val repo = ParkingRepository(ReportPolicies.DEFAULT) { bcMs(now) }
        val byId = repo.parkingLots.value.associateBy { it.id }
        assertEquals(GateToday.ClosesAsEarlyAs(LocalTime.of(7, 0), LocalTime.of(18, 0)), byId.getValue("lynn_canyon_park").today?.gate)
        assertEquals(GateToday.NoHoursPublished, byId.getValue("joffre_lakes_main").today?.gate)
        assertEquals(GateToday.Hours(LocalTime.of(8, 0), LocalTime.of(16, 30)), byId.getValue("buntzen_lake_main").today?.gate)
        assertTrue(byId.values.all { it.today != null })
        assertTrue("noon in November: Buntzen is open", byId.getValue("buntzen_lake_main").today!!.gateOpenNow)
        val evening = ParkingRepository(ReportPolicies.DEFAULT) { bcMs(LocalDateTime.of(winterWeeknight, LocalTime.of(22, 0))) }
        assertFalse("10 pm in November: Buntzen is shut", evening.parkingLots.value.first { it.id == "buntzen_lake_main" }.today!!.gateOpenNow)
    }

    @Test
    fun `cards show the outlook at the moment of the last refresh, computed in BC time`() {
        var now = LocalDateTime.of(summerSaturday, beforeSeymourGate)
        val repo = ParkingRepository(ReportPolicies.DEFAULT) { bcMs(now) }
        assertEquals(Outlook.GATE_CLOSED, repo.parkingLots.value.first { it.id == SEYMOUR }.prediction?.outlook)
        now = LocalDateTime.of(summerSaturday, LocalTime.NOON)
        repo.recalculateLotConsensus()
        assertEquals(Outlook.ARRIVE_BY, repo.parkingLots.value.first { it.id == SEYMOUR }.prediction?.outlook)
    }
}
