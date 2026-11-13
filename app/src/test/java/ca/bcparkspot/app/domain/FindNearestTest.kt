package ca.bcparkspot.app.domain

import ca.bcparkspot.app.model.AvailabilityStatus.AVAILABLE
import ca.bcparkspot.app.model.AvailabilityStatus.CLOSED
import ca.bcparkspot.app.model.AvailabilityStatus.FILLING_FAST
import ca.bcparkspot.app.model.AvailabilityStatus.FULL
import org.junit.Assert.assertEquals
import org.junit.Test

class FindNearestTest {
    private fun lot(id: String, minutes: Int, state: LotState, park: String = "park") = LotCandidate(id, park, minutes, state)

    private val confirmedOpen = LotState.Confirmed(AVAILABLE, 2, 0)
    private val confirmedFull = LotState.Confirmed(FULL, 2, 0)
    private val singleOpen = LotState.Unconfirmed(AVAILABLE, 1, 0)
    private val singleFull = LotState.Unconfirmed(FULL, 1, 0)
    private val mixedOpen = LotState.Mixed(AVAILABLE, 6, 0)

    @Test
    fun `picks the nearest confirmed open lot`() {
        val result = findNearestOpenSpot(listOf(lot("far", 40, confirmedOpen), lot("near", 10, confirmedOpen), lot("nearest", 5, singleOpen)))
        assertEquals(NearestResult.Confirmed(lot("near", 10, confirmedOpen)), result)
    }

    @Test
    fun `filling fast counts as open`() {
        val filling = LotState.Confirmed(FILLING_FAST, 2, 0)
        assertEquals(NearestResult.Confirmed(lot("a", 10, filling)), findNearestOpenSpot(listOf(lot("a", 10, filling))))
    }

    @Test
    fun `drive time ties go to more reporters, then the newer report, then lot id`() {
        val more = LotState.Confirmed(AVAILABLE, 3, 30)
        val newer = LotState.Confirmed(AVAILABLE, 2, 10)
        val older = LotState.Confirmed(AVAILABLE, 2, 20)
        assertEquals("m", (findNearestOpenSpot(listOf(lot("o", 10, older), lot("m", 10, more))) as NearestResult.Confirmed).candidate.lotId)
        assertEquals("n", (findNearestOpenSpot(listOf(lot("o", 10, older), lot("n", 10, newer))) as NearestResult.Confirmed).candidate.lotId)
        assertEquals("a", (findNearestOpenSpot(listOf(lot("b", 10, older), lot("a", 10, older))) as NearestResult.Confirmed).candidate.lotId)
    }

    @Test
    fun `fallback prefers a lone open report over a nearer unreported lot`() {
        val result = findNearestOpenSpot(listOf(lot("none", 5, LotState.NoRecentReports), lot("single", 30, singleOpen)))
        assertEquals(NearestResult.Fallback(lot("single", 30, singleOpen)), result)
    }

    @Test
    fun `fallback uses unreported lots when there is no open-side report`() {
        val result = findNearestOpenSpot(listOf(lot("far", 50, LotState.NoRecentReports), lot("near", 5, LotState.NoRecentReports)))
        assertEquals(NearestResult.Fallback(lot("near", 5, LotState.NoRecentReports)), result)
    }

    @Test
    fun `never offers full, closed, full-leaning or mixed lots`() {
        val lots = listOf(
            lot("full", 1, confirmedFull),
            lot("closed", 2, LotState.Confirmed(CLOSED, 2, 0)),
            lot("leansFull", 3, singleFull),
            lot("mixed", 4, mixedOpen)
        )
        assertEquals(NearestResult.NothingQualifies, findNearestOpenSpot(lots))
    }

    @Test
    fun `a park search stays in the park before looking elsewhere`() {
        val lots = listOf(lot("other", 1, confirmedOpen, park = "other"), lot("mine", 20, confirmedOpen, park = "mine"))
        assertEquals("mine", (findNearestOpenSpot(lots, "mine") as NearestResult.Confirmed).candidate.lotId)
    }

    @Test
    fun `a park search with nothing confirmed in the park uses a confirmed lot elsewhere before a fallback`() {
        val lots = listOf(lot("other", 30, confirmedOpen, park = "other"), lot("mine", 1, LotState.NoRecentReports, park = "mine"))
        assertEquals("other", (findNearestOpenSpot(lots, "mine") as NearestResult.Confirmed).candidate.lotId)
    }

    @Test
    fun `a park search with only a fallback in the park keeps it over a lone open report elsewhere`() {
        val lots = listOf(lot("other", 5, singleOpen, park = "other"), lot("mine", 40, LotState.NoRecentReports, park = "mine"))
        assertEquals(NearestResult.Fallback(lot("mine", 40, LotState.NoRecentReports, park = "mine")), findNearestOpenSpot(lots, "mine"))
    }

    @Test
    fun `no lots at all is nothing qualifies`() {
        assertEquals(NearestResult.NothingQualifies, findNearestOpenSpot(emptyList()))
    }

    private val none = LotState.NoRecentReports
    private fun predicted(id: String, minutes: Int, outlook: Outlook?, state: LotState = none) =
        LotCandidate(id, "park", minutes, state, outlook)

    @Test
    fun `with no reports anywhere, a lot no source calls busy beats a nearer busy one`() {
        val quiet = predicted("quiet", 20, null)
        val result = findNearestOpenSpot(listOf(predicted("summer", 5, Outlook.SUMMER_BUSY), quiet))
        assertEquals(NearestResult.Fallback(quiet), result)
    }

    @Test
    fun `a pass day is a rule, not a busy claim, so it does not push a lot down`() {
        val passDay = predicted("passDay", 5, Outlook.PASS_DAY)
        assertEquals(NearestResult.Fallback(passDay), findNearestOpenSpot(listOf(passDay, predicted("quiet", 20, null))))
        assertEquals(setOf(Outlook.ARRIVE_BY, Outlook.SUMMER_BUSY), BUSY_OUTLOOKS)
    }

    @Test
    fun `busy unreported lots are still offered when nothing better is reachable`() {
        val busy = predicted("busy", 5, Outlook.SUMMER_BUSY)
        val early = predicted("early", 2, Outlook.ARRIVE_BY)
        assertEquals(NearestResult.Fallback(early), findNearestOpenSpot(listOf(busy, early)))
    }

    @Test
    fun `source advice never outranks a real open report`() {
        val single = lot("single", 30, singleOpen)
        assertEquals(NearestResult.Fallback(single), findNearestOpenSpot(listOf(predicted("guess", 5, null), single)))
    }

    @Test
    fun `no tier offers a closed park or a shut gate, not even a confirmed open lot`() {
        val elsewhere = lot("elsewhere", 30, LotState.NoRecentReports)
        listOf(Outlook.PARK_CLOSED, Outlook.GATE_CLOSED).forEach { blocked ->
            assertEquals(NearestResult.Fallback(elsewhere), findNearestOpenSpot(listOf(predicted("confirmed", 1, blocked, confirmedOpen), elsewhere)))
            assertEquals(NearestResult.Fallback(elsewhere), findNearestOpenSpot(listOf(predicted("single", 1, blocked, singleOpen), elsewhere)))
            assertEquals(NearestResult.NothingQualifies, findNearestOpenSpot(listOf(predicted("unreported", 1, blocked))))
        }
    }
}
