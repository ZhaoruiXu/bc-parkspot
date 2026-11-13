package ca.bcparkspot.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.BCHolidaySchedule
import ca.bcparkspot.app.model.CommunityReport
import ca.bcparkspot.app.model.PassRequirementStatus
import ca.bcparkspot.app.ui.viewmodel.ParkingViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.content.Context
import org.junit.Assert.*
import ca.bcparkspot.app.data.local.entity.ReportSyncState
import ca.bcparkspot.app.testutil.awaitUntil
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParkRulesTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("ca.bcparkspot.app")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key-for-test")
                    .build()
            )
        }
    }

    private val testLotId = "garibaldi_rubble_creek"

    @Test
    fun garibaldiRubbleCreek_evaluatesHolidaysAndWeekdays() {
        val rubbleCreek = BCParksDataSource.getInitialParkingLots().first { it.id == "garibaldi_rubble_creek" }

        // Summer Saturday July 18, 2026 -> Pass required
        assertTrue(rubbleCreek.isPassRequiredToday(LocalDate.of(2026, 7, 18)))

        // Summer Wednesday July 22, 2026 (non-holiday) -> Pass NOT required
        assertFalse(rubbleCreek.isPassRequiredToday(LocalDate.of(2026, 7, 22)))

        // Canada Day Wednesday July 1, 2026 (statutory holiday) -> Pass required!
        val canadaDay = LocalDate.of(2026, 7, 1)
        assertTrue("Pass must be required on statutory holiday Wednesday (Canada Day)", rubbleCreek.isPassRequiredToday(canadaDay))
    }

    @Test
    fun garibaldiCheakamus_evaluatesPeakDailyAndShoulderFriMon() {
        val cheakamus = BCParksDataSource.getInitialParkingLots().first { it.id == "cheakamus_lake_lot" }

        // Peak season Wednesday July 15, 2026 -> Daily rule: pass required!
        assertTrue("Cheakamus daily rule: Wednesday in July requires pass", cheakamus.isPassRequiredToday(LocalDate.of(2026, 7, 15)))

        // Shoulder season Wednesday Sept 16, 2026 -> Fri-Mon rule: pass NOT required!
        assertFalse("Cheakamus shoulder rule: Wednesday in Sept does NOT require pass", cheakamus.isPassRequiredToday(LocalDate.of(2026, 9, 16)))

        // Shoulder season Saturday Sept 19, 2026 -> Fri-Mon rule: pass required!
        assertTrue("Cheakamus shoulder rule: Saturday in Sept requires pass", cheakamus.isPassRequiredToday(LocalDate.of(2026, 9, 19)))
    }

    @Test
    fun joffreLakes_evaluatesDailyPassAndReconciliationClosures() {
        val joffre = BCParksDataSource.getInitialParkingLots().first { it.id == "joffre_lakes_main" }

        // Regular summer day: July 10, 2026 -> Pass required
        assertTrue(joffre.isPassRequiredToday(LocalDate.of(2026, 7, 10)))

        // Cultural celebration closure: June 23, 2026 -> Closed!
        val closureJune = joffre.getActiveClosure(LocalDate.of(2026, 6, 23))
        assertNotNull("Park must be closed June 20-27 for Indigenous celebration", closureJune)
        assertFalse("Pass is not required when park is closed", joffre.isPassRequiredToday(LocalDate.of(2026, 6, 23)))

        // Fall reconciliation closure: September 15, 2026 -> Closed!
        val closureSept = joffre.getActiveClosure(LocalDate.of(2026, 9, 15))
        assertNotNull("Park must be closed Sept 8-30 for reconciliation/recovery", closureSept)
        assertFalse("Pass is not required when park is closed", joffre.isPassRequiredToday(LocalDate.of(2026, 9, 15)))

        // Joffre total spots must be null (person quota, not vehicle stalls)
        assertNull("Joffre stall count must be null as passes are per person", joffre.totalSpots)
    }

    @Test
    fun goldenEars_evaluatesTimeOfDayCutoff() {
        val alouette = BCParksDataSource.getInitialParkingLots().first { it.id == "golden_ears_alouette" }
        val saturday = LocalDate.of(2026, 7, 18)

        // 2:00 PM on Saturday -> Pass required
        assertTrue(alouette.isPassRequiredToday(saturday, LocalTime.of(14, 0)))

        // 3:31 PM on Saturday -> Pass NOT required (cutoff at 3:30 PM)
        assertFalse(alouette.isPassRequiredToday(saturday, LocalTime.of(15, 31)))
    }

    @Test
    fun mountSeymour_evaluatesWinterOnlyAndTimeOfDay() {
        val seymourP1 = BCParksDataSource.getInitialParkingLots().first { it.id == "seymour_lower_p1" }

        // While 2026-27 dates are TBA, pass requirement must not be computed
        assertEquals("Winter 2026-27 pass dates TBA - check bcparks.ca", seymourP1.passInfo)
        assertFalse(seymourP1.isPassRequiredToday(LocalDate.of(2027, 1, 16), LocalTime.of(10, 0)))

        // Verify official 2025-26 seasonal rule period logic: daily Dec 19 - Jan 4; weekends & holidays Jan 9 - Mar 29, 7 AM - 4 PM
        val periodDailyPeak = ca.bcparkspot.app.model.PassRulePeriod(
            startMonth = 12, startDay = 19, endMonth = 1, endDay = 4,
            daysOfWeek = null, includesHolidays = true, cutoffTime = LocalTime.of(16, 0)
        )
        val periodWeekends = ca.bcparkspot.app.model.PassRulePeriod(
            startMonth = 1, startDay = 9, endMonth = 3, endDay = 29,
            daysOfWeek = setOf(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY),
            includesHolidays = true, cutoffTime = LocalTime.of(16, 0)
        )

        // Dec 24, 2025 (Wednesday during daily peak) at 10 AM -> Pass required
        assertTrue(periodDailyPeak.isPassRequired(LocalDate.of(2025, 12, 24), LocalTime.of(10, 0)))

        // Jan 17, 2026 (Saturday during weekends window) at 10 AM -> Pass required
        assertTrue(periodWeekends.isPassRequired(LocalDate.of(2026, 1, 17), LocalTime.of(10, 0)))

        // Jan 17, 2026 after 4:00 PM -> Cutoff: Pass NOT required
        assertFalse(periodWeekends.isPassRequired(LocalDate.of(2026, 1, 17), LocalTime.of(16, 30)))

        // Jan 21, 2026 (Wednesday non-holiday) -> Weekday exempt: Pass NOT required
        assertFalse(periodWeekends.isPassRequired(LocalDate.of(2026, 1, 21), LocalTime.of(10, 0)))
    }

    @Test
    fun viewModel_commentValidation_rejectsEmptyAndOver500Chars() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ParkingViewModel(app)

        // Empty comment
        viewModel.submitCommunityReport(
            lotId = testLotId,
            status = AvailabilityStatus.AVAILABLE,
            comment = "   ",
            spots = 10
        )
        assertEquals(
            "A comment needs 3 to 500 characters.",
            viewModel.uiState.value.userNotification
        )

        // 2 characters comment (under minimum 3)
        viewModel.submitCommunityReport(
            lotId = testLotId,
            status = AvailabilityStatus.AVAILABLE,
            comment = "ok",
            spots = 10
        )
        assertEquals(
            "A comment needs 3 to 500 characters.",
            viewModel.uiState.value.userNotification
        )

        // 501 characters comment
        val longComment = "A".repeat(501)
        viewModel.submitCommunityReport(
            lotId = testLotId,
            status = AvailabilityStatus.AVAILABLE,
            comment = longComment,
            spots = 10
        )
        assertEquals(
            "A comment needs 3 to 500 characters.",
            viewModel.uiState.value.userNotification
        )

        // Valid comment
        viewModel.submitCommunityReport(
            lotId = testLotId,
            status = AvailabilityStatus.AVAILABLE,
            comment = "Parked in main lot, ample space at 9:00 AM.",
            spots = 25
        )
        val saved = app.getString(ca.bcparkspot.app.R.string.report_saved)
        awaitUntil { viewModel.uiState.value.userNotification == saved }
        val stored = runBlocking { AppGraph.from(app).local.dao.ownReportRows(AppGraph.from(app).accounts.ownerUid.value) }
        assertTrue(stored.any { it.lotId == testLotId && it.syncState == ReportSyncState.PENDING })
    }

    @Test
    fun testGoodFriday_inBCHolidaySchedule() {
        // Good Friday in 2027 falls on March 26, 2027 (inside Seymour pass window)
        val goodFriday2027 = LocalDate.of(2027, 3, 26)
        assertTrue("March 26, 2027 is Good Friday in BC", BCHolidaySchedule.isBCHoliday(goodFriday2027))

        // Good Friday in 2026 falls on April 3, 2026
        val goodFriday2026 = LocalDate.of(2026, 4, 3)
        assertTrue("April 3, 2026 is Good Friday in BC", BCHolidaySchedule.isBCHoliday(goodFriday2026))

        // A regular Friday should not be a holiday
        val regularFriday = LocalDate.of(2027, 3, 19)
        assertFalse("March 19, 2027 is not a BC statutory holiday", BCHolidaySchedule.isBCHoliday(regularFriday))
    }

    @Test
    fun testMountSeymour_passRequirementNotComputedWhenDatesTBA() {
        val lots = BCParksDataSource.getInitialParkingLots()
        val seymourP1 = lots.first { it.id == "seymour_lower_p1" }
        assertEquals("Winter 2026-27 pass dates TBA - check bcparks.ca", seymourP1.passInfo)
        assertTrue("passRulePeriods must be empty when dates are TBA", seymourP1.passRulePeriods.isEmpty())

        // Pass state should be UNKNOWN while dates are TBA
        val winterSaturday = LocalDate.of(2027, 1, 16)
        val afternoon = LocalTime.of(13, 0)
        assertEquals(
            "Pass requirement state must be UNKNOWN while dates are TBA",
            PassRequirementStatus.UNKNOWN,
            seymourP1.getPassState(winterSaturday, afternoon)
        )
        assertFalse("Pass is not required when state is UNKNOWN", seymourP1.isPassRequiredToday(winterSaturday, afternoon))
    }

    @Test
    fun allParkingLots_haveOfficialSourcesAndVerifiedDates() {
        val lots = BCParksDataSource.getInitialParkingLots()
        assertEquals(18, lots.size)

        lots.forEach { lot ->
            assertTrue("SourceUrl must start with https:// for ${lot.id}", lot.sourceUrl.startsWith("https://"))
            lot.verifiedAt?.let { date ->
                assertTrue("VerifiedAt must match YYYY-MM-DD pattern for ${lot.id}", date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            }
            if (lot.isDayUsePassRequired && !lot.sourceUrl.contains("bchydro.com")) {
                assertTrue("Pass lot ${lot.id} must point to official day-use passes page", lot.sourceUrl.contains("reservations/day-use-passes"))
            }
        }
    }

    @Test
    fun testMapCardLogic_pinTapSelectsLotWithoutOpeningDetails() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = ParkingViewModel(app)

        val lots = BCParksDataSource.getInitialParkingLots()
        val testLot = lots.first { it.id == "garibaldi_rubble_creek" }

        // Start in MAP view mode
        viewModel.setExploreViewMode(ca.bcparkspot.app.ui.screens.ExploreViewMode.MAP)
        assertEquals(ca.bcparkspot.app.ui.screens.ExploreViewMode.MAP, viewModel.uiState.value.exploreViewMode)
        assertNull(viewModel.uiState.value.selectedMapLot)
        assertNull(viewModel.uiState.value.selectedLot)

        // 1. Tapping a pin: sets selectedMapLot only, selectedLot remains null (details NOT opened)
        viewModel.selectMapLot(testLot)
        assertEquals("Tapping pin must set selectedMapLot to display bottom card", testLot, viewModel.uiState.value.selectedMapLot)
        assertNull("Tapping pin must NOT open details screen: selectedLot must remain null", viewModel.uiState.value.selectedLot)

        // 2. Dismissing bottom card: clears selectedMapLot, details remain unopened
        viewModel.selectMapLot(null)
        assertNull("Dismissing card clears selectedMapLot", viewModel.uiState.value.selectedMapLot)
        assertNull("SelectedLot still remains null", viewModel.uiState.value.selectedLot)

        // 3. User taps 'Trailhead Details' button: sets selectedLot, opening details
        viewModel.selectMapLot(testLot)
        viewModel.selectLot(testLot)
        assertEquals("Trailhead Details button opens details screen", testLot, viewModel.uiState.value.selectedLot)

        // 4. Pressing back from details: clears selectedLot, returns to ExploreViewMode.MAP
        viewModel.selectLot(null)
        assertNull("Back from details clears selectedLot", viewModel.uiState.value.selectedLot)
        assertEquals("Returning from details screen preserves MAP mode", ca.bcparkspot.app.ui.screens.ExploreViewMode.MAP, viewModel.uiState.value.exploreViewMode)
    }
}
