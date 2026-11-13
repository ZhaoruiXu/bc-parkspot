package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.CLOSING_TIME_NOT_PUBLISHED
import ca.bcparkspot.app.domain.GateHours
import ca.bcparkspot.app.domain.GateHoursForDay
import ca.bcparkspot.app.domain.GateSeason
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay

/**
 * Each lot's usual gate hours as its operator publishes them, checked on 2026-11-12, matching the gateHours text
 * shown in BCParksDataSource. Lots whose operator publishes no gate hours are left out, so the app claims nothing
 * about them. Seasonal dates that the source gives only approximately are noted where they are set.
 */
object GateWindows {
    private val SIX_AM: LocalTime = LocalTime.of(6, 0)
    private val SEVEN_AM: LocalTime = LocalTime.of(7, 0)
    private val EIGHT_AM: LocalTime = LocalTime.of(8, 0)
    private val FOUR_THIRTY_PM: LocalTime = LocalTime.of(16, 30)
    private val FIVE_PM: LocalTime = LocalTime.of(17, 0)
    private val FIVE_THIRTY_PM: LocalTime = LocalTime.of(17, 30)
    private val SIX_PM: LocalTime = LocalTime.of(18, 0)
    private val SIX_THIRTY_PM: LocalTime = LocalTime.of(18, 30)
    private val SEVEN_PM: LocalTime = LocalTime.of(19, 0)
    private val SEVEN_THIRTY_PM: LocalTime = LocalTime.of(19, 30)
    private val EIGHT_PM: LocalTime = LocalTime.of(20, 0)
    private val NINE_PM: LocalTime = LocalTime.of(21, 0)
    private val TEN_PM: LocalTime = LocalTime.of(22, 0)
    private val ELEVEN_PM: LocalTime = LocalTime.of(23, 0)

    private fun season(month: Month, day: Int, opens: LocalTime, closes: LocalTime) =
        GateSeason(MonthDay.of(month, day), GateHoursForDay(opens, closes))
    private fun closedAllDay(month: Month, day: Int) = GateSeason(MonthDay.of(month, day), null, closedAllDay = true)

    /** bcparks.ca/golden-ears-park: "April to mid-October, 7 am to 11 pm, and mid-October to March, 7 am to 7 pm". Mid-October is taken as the 15th. */
    private val GOLDEN_EARS = GateHours(listOf(
        season(Month.APRIL, 1, SEVEN_AM, ELEVEN_PM),
        season(Month.OCTOBER, 15, SEVEN_AM, SEVEN_PM)
    ))

    /**
     * bcparks.ca/mount-seymour-park: the gate above parking lot 2 closes nightly 10 pm to 6 am year-round, and
     * from November to mid-April the road gate at the base closes 10 pm to 7 am. Mid-April is taken as the 15th.
     */
    private val MOUNT_SEYMOUR = GateHours(listOf(
        season(Month.APRIL, 15, SIX_AM, TEN_PM),
        season(Month.NOVEMBER, 1, SEVEN_AM, TEN_PM)
    ))

    /**
     * bcparks.ca/stawamus-chief-park publishes a seasonal table for the Chief Apron lot, and for the campground lot
     * this app lists "The gate is locked between 11 pm and 7 am during the winter season". The campground gate is
     * used for the winter months the apron table leaves open all day.
     */
    private val CHIEF_CAMPGROUND = GateHours(listOf(
        season(Month.MARCH, 15, SEVEN_AM, NINE_PM),
        season(Month.MAY, 8, SEVEN_AM, ELEVEN_PM),
        season(Month.SEPTEMBER, 16, SEVEN_AM, NINE_PM),
        season(Month.OCTOBER, 16, SEVEN_AM, ELEVEN_PM)
    ))

    /**
     * bcparks.ca/shannon-falls-park gives a seasonal table and also says "The gate is closed between 10 pm and 7 am";
     * where they differ the earlier closing is used.
     */
    private val SHANNON_FALLS = GateHours(listOf(
        season(Month.MARCH, 15, SEVEN_AM, NINE_PM),
        season(Month.MAY, 8, SEVEN_AM, TEN_PM),
        season(Month.SEPTEMBER, 16, SEVEN_AM, NINE_PM),
        season(Month.OCTOBER, 16, SEVEN_AM, TEN_PM)
    ))

    /**
     * bcparks.ca/alice-lake-park, whose dates it calls approximate: open from snowmelt ("usually March", taken as
     * March 1) to first snowfall ("usually late December", taken as December 20), and "closed 24 hrs when there is
     * snow on the ground", taken as the time between.
     */
    private val ALICE_LAKE = GateHours(listOf(
        season(Month.MARCH, 1, EIGHT_AM, EIGHT_PM),
        season(Month.MAY, 1, SEVEN_AM, NINE_PM),
        season(Month.JUNE, 16, SEVEN_AM, TEN_PM),
        season(Month.SEPTEMBER, 15, EIGHT_AM, EIGHT_PM),
        closedAllDay(Month.DECEMBER, 20)
    ))

    /**
     * BC Hydro's 2026 Buntzen Lake table, whose last row runs "November 2 to January 31" at 4:30 pm: gates open at
     * 8 am and close at a time whose dates shift by a few days each year, so the table needs checking every year.
     */
    private val BUNTZEN = GateHours(listOf(
        season(Month.FEBRUARY, 1, EIGHT_AM, FIVE_PM),
        season(Month.FEBRUARY, 16, EIGHT_AM, FIVE_THIRTY_PM),
        season(Month.MARCH, 2, EIGHT_AM, SIX_PM),
        season(Month.MARCH, 9, EIGHT_AM, SEVEN_PM),
        season(Month.MARCH, 23, EIGHT_AM, SEVEN_THIRTY_PM),
        season(Month.APRIL, 13, EIGHT_AM, EIGHT_PM),
        season(Month.AUGUST, 24, EIGHT_AM, SEVEN_THIRTY_PM),
        season(Month.SEPTEMBER, 14, EIGHT_AM, SEVEN_PM),
        season(Month.SEPTEMBER, 28, EIGHT_AM, SIX_THIRTY_PM),
        season(Month.OCTOBER, 12, EIGHT_AM, SIX_PM),
        season(Month.OCTOBER, 26, EIGHT_AM, FIVE_THIRTY_PM),
        season(Month.NOVEMBER, 2, EIGHT_AM, FOUR_THIRTY_PM)
    ))

    /** Metro Vancouver's regional park hours, with the earlier summer close (9 pm, not 10 pm) it lists for təmtəmíxʷtən/Belcarra. */
    private val BELCARRA = GateHours(listOf(
        season(Month.JANUARY, 1, SEVEN_AM, FIVE_PM),
        season(Month.FEBRUARY, 3, SEVEN_AM, SIX_PM),
        season(Month.MARCH, 6, SEVEN_AM, EIGHT_PM),
        season(Month.APRIL, 7, SEVEN_AM, NINE_PM),
        season(Month.AUGUST, 25, SEVEN_AM, NINE_PM),
        season(Month.SEPTEMBER, 8, SEVEN_AM, EIGHT_PM),
        season(Month.OCTOBER, 6, SEVEN_AM, SEVEN_PM),
        season(Month.NOVEMBER, 3, SEVEN_AM, SIX_PM)
    ))

    /**
     * The Lynn Canyon Ecology Centre: "Currently the parking lot gates are open from 7 am to 6 pm", and "Gate closing
     * hours change seasonally" with no later time published, so after 6 pm the gate may be closed.
     */
    private val LYNN_CANYON = GateHours.daily(SEVEN_AM, CLOSING_TIME_NOT_PUBLISHED, earliestClose = SIX_PM)

    val BY_LOT: Map<String, GateHours> = mapOf(
        "golden_ears_alouette" to GOLDEN_EARS,
        "golden_ears_gold_creek" to GOLDEN_EARS,
        "golden_ears_west_canyon" to GOLDEN_EARS,
        "seymour_lower_p1" to MOUNT_SEYMOUR,
        "seymour_lower_p5" to MOUNT_SEYMOUR,
        "stawamus_chief_lot" to CHIEF_CAMPGROUND,
        "shannon_falls_lot" to SHANNON_FALLS,
        "alice_lake_main" to ALICE_LAKE,
        "buntzen_lake_main" to BUNTZEN,
        "belcarra_picnic_area" to BELCARRA,
        "sasamat_white_pine" to BELCARRA,
        "lynn_canyon_park" to LYNN_CANYON
    )
}
