package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.BusyAdvice
import java.time.LocalTime

/**
 * What public sources say about when each lot is busy, checked against the sources on 2026-11-12.
 * A lot is listed only for what a source we opened states about it.
 */
object FillPatterns {
    /**
     * Vancouver Trails names Joffre, Garibaldi, Cypress and Seymour among lots that fill early on weekends,
     * and advises arriving by 7am on summer weekends, before the lots are full.
     */
    private val ARRIVE_BY_ON_SUMMER_WEEKENDS: LocalTime = LocalTime.of(7, 0)

    private const val VANCOUVER_TRAILS = "https://www.vancouvertrails.com/faq/driving-directions/"
    /** A 2022 North Shore Local report: during "the busy summer season" parking is maxed out at Lynn Canyon Park. */
    private const val NORTH_SHORE_PARKING = "https://thenorthshorelocal.com/parking-maxed-out-at-lynn-valley-deep-cove-parks"

    private val SUMMER_WEEKEND_ARRIVAL = BusyAdvice(ARRIVE_BY_ON_SUMMER_WEEKENDS, false, VANCOUVER_TRAILS)
    private val SUMMER_PARKING_TROUBLE = BusyAdvice(null, true, NORTH_SHORE_PARKING)

    val BY_LOT: Map<String, BusyAdvice> = mapOf(
        "garibaldi_rubble_creek" to SUMMER_WEEKEND_ARRIVAL,
        "joffre_lakes_main" to SUMMER_WEEKEND_ARRIVAL,
        "seymour_lower_p1" to SUMMER_WEEKEND_ARRIVAL,
        "seymour_lower_p5" to SUMMER_WEEKEND_ARRIVAL,
        "cypress_downhill_lot" to SUMMER_WEEKEND_ARRIVAL,
        "lynn_canyon_park" to SUMMER_PARKING_TROUBLE
    )
}
