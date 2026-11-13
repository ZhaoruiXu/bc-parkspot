package ca.bcparkspot.app.domain

import ca.bcparkspot.app.model.PassRequirementStatus
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.ZoneId

/**
 * What public sources say about when a lot is busy. Each field is set only where a cited source states it,
 * in the source's own terms; the app never turns a source's silence into a claim that a lot is quiet.
 */
data class BusyAdvice(
    /** A time a source says to arrive by on summer weekends, before the lot fills. */
    val arriveBySummerWeekends: LocalTime?,
    /** Whether a source reports parking maxed out during the summer season. */
    val busyInSummer: Boolean,
    val sourceUrl: String
)

/** Calendar summer, the season the sources name. */
val SUMMER_MONTHS: Set<Month> = setOf(Month.JUNE, Month.JULY, Month.AUGUST)

/** The time a gate opens and the time it closes on one day. */
data class GateHoursForDay(val opens: LocalTime, val closes: LocalTime)

/**
 * From [from] each year until the next season starts, the gate keeps [hours]; with no hours it stays open all day,
 * unless [closedAllDay].
 */
data class GateSeason(val from: MonthDay, val hours: GateHoursForDay?, val closedAllDay: Boolean = false)

/** Stands in for a closing time the source does not publish, so the gate is never claimed shut by its hours alone. */
val CLOSING_TIME_NOT_PUBLISHED: LocalTime = LocalTime.MAX

/**
 * The least time the app wants a hiker to have on site before a gate closes; a lot closing sooner after arrival is
 * not suggested. The app's own margin for parking and getting back, not a published rule.
 */
val MIN_TIME_BEFORE_GATE_CLOSES: Duration = Duration.ofHours(1)

private val START_OF_YEAR: MonthDay = MonthDay.of(Month.JANUARY, 1)

/**
 * When a lot's gate is usually open, in BC local time, season by season; the last season wraps over the new year.
 * Where the gate closes earlier on dates that are not published, [earliestClose] is the earliest stated closing
 * time. A lot without gate hours is treated as always open.
 */
data class GateHours(val seasons: List<GateSeason>, val earliestClose: LocalTime? = null) {
    init {
        require(seasons.isNotEmpty())
        seasons.forEach { season ->
            season.hours?.let { require(it.opens < it.closes) }
            require(!season.closedAllDay || season.hours == null)
        }
    }

    companion object {
        /** The same hours every day of the year. */
        fun daily(opens: LocalTime, closes: LocalTime, earliestClose: LocalTime? = null): GateHours =
            GateHours(listOf(GateSeason(START_OF_YEAR, GateHoursForDay(opens, closes))), earliestClose)
    }
}

fun GateHours.seasonOn(date: LocalDate): GateSeason {
    val sorted = seasons.sortedBy { it.from }
    val day = MonthDay.from(date)
    return sorted.lastOrNull { it.from <= day } ?: sorted.last()
}

/** The gate's hours on [date], or null when it keeps no hours that day. */
fun GateHours.hoursOn(date: LocalDate): GateHoursForDay? = seasonOn(date).hours

fun GateHours?.isOpenAt(at: LocalDateTime): Boolean {
    if (this == null) return true
    val season = seasonOn(at.toLocalDate())
    if (season.closedAllDay) return false
    val hours = season.hours ?: return true
    val time = at.toLocalTime()
    return time >= hours.opens && time < hours.closes
}

/**
 * When the gate is open at [at] but closes, or can close, within [MIN_TIME_BEFORE_GATE_CLOSES], the time it
 * closes; otherwise null.
 */
fun GateHours?.closingSoonAt(at: LocalDateTime): LocalTime? {
    if (this == null || !isOpenAt(at) || mayBeClosedAt(at)) return null
    val time = at.toLocalTime()
    val closes = listOfNotNull(earliestClose, hoursOn(at.toLocalDate())?.closes)
        .filter { it != CLOSING_TIME_NOT_PUBLISHED && it > time }
        .minOrNull() ?: return null
    return closes.takeIf { Duration.between(time, it) <= MIN_TIME_BEFORE_GATE_CLOSES }
}

/** True when the gate is open by its usual hours but past the earliest time it is stated to close on some days. */
fun GateHours?.mayBeClosedAt(at: LocalDateTime): Boolean {
    val earliest = this?.earliestClose ?: return false
    return isOpenAt(at) && at.toLocalTime() >= earliest
}

/** What a lot's gate does on one day, as its operator publishes it. */
sealed interface GateToday {
    /** The operator publishes no gate hours, so the app claims none. */
    data object NoHoursPublished : GateToday
    data object OpenAllDay : GateToday
    data object ClosedAllDay : GateToday
    data class Hours(val opens: LocalTime, val closes: LocalTime) : GateToday
    /** The latest closing time is not published; the gate can close as early as [earliestClose]. */
    data class ClosesAsEarlyAs(val opens: LocalTime, val earliestClose: LocalTime) : GateToday
}

/** The published facts a first visitor can rely on before anyone has reported on a lot. */
data class TodayAtLot(
    val gate: GateToday,
    val pass: PassRequirementStatus,
    /** Whether the gate is open by its usual hours at that moment, and not past its earliest stated close. */
    val gateOpenNow: Boolean
)

fun GateHours?.today(date: LocalDate): GateToday {
    if (this == null) return GateToday.NoHoursPublished
    val season = seasonOn(date)
    val hours = season.hours
    return when {
        season.closedAllDay -> GateToday.ClosedAllDay
        hours == null -> GateToday.OpenAllDay
        earliestClose != null -> GateToday.ClosesAsEarlyAs(hours.opens, earliestClose)
        else -> GateToday.Hours(hours.opens, hours.closes)
    }
}

/** Everything the app knows about a lot at one moment, apart from hiker reports. */
data class LotFacts(
    val advice: BusyAdvice?,
    val gate: GateHours?,
    val parkClosed: Boolean,
    val passRequired: Boolean
)

/** Most decisive first; a lot shows only the first that applies. */
enum class Outlook { PARK_CLOSED, GATE_CLOSED, GATE_MAY_BE_CLOSED, GATE_CLOSING_SOON, ARRIVE_BY, PASS_DAY, SUMMER_BUSY }

/** Outlooks that rule a lot out: nobody can park there at that moment, or the gate may be shut or about to shut. */
val BLOCKING_OUTLOOKS: Set<Outlook> =
    setOf(Outlook.PARK_CLOSED, Outlook.GATE_CLOSED, Outlook.GATE_MAY_BE_CLOSED, Outlook.GATE_CLOSING_SOON)

/** Outlooks where a cited source calls the lot busy at that moment. */
val BUSY_OUTLOOKS: Set<Outlook> = setOf(Outlook.ARRIVE_BY, Outlook.SUMMER_BUSY)

data class Prediction(
    val outlook: Outlook,
    /** The public page behind the line, or null when it comes from the lot details bundled with the app. */
    val sourceUrl: String?,
    /** The sourced arrive-by time, for [Outlook.ARRIVE_BY]. */
    val arriveBy: LocalTime? = null,
    /** When the gate usually opens, for [Outlook.GATE_CLOSED]. */
    val gateOpens: LocalTime? = null,
    /** The earliest time the gate is stated to close, for [Outlook.GATE_MAY_BE_CLOSED]. */
    val gateClosesAsEarlyAs: LocalTime? = null,
    /** When the gate closes or can close, for [Outlook.GATE_CLOSING_SOON]. */
    val gateCloses: LocalTime? = null
)

/** Every lot is in BC, so patterns are read in BC local time wherever the phone is. */
val BC_TIME_ZONE: ZoneId = ZoneId.of("America/Vancouver")

fun bcLocalTime(nowMs: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMs), BC_TIME_ZONE)

fun isSummerWeekend(date: LocalDate): Boolean =
    date.month in SUMMER_MONTHS && (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY)

/** What to tell a hiker about a lot at [at] from sources alone, or null when no source says anything about it. */
fun predictOutlook(facts: LotFacts, at: LocalDateTime): Prediction? {
    val advice = facts.advice
    val arriveBy = advice?.arriveBySummerWeekends
    return when {
        facts.parkClosed -> Prediction(Outlook.PARK_CLOSED, null)
        !facts.gate.isOpenAt(at) -> Prediction(Outlook.GATE_CLOSED, null, gateOpens = facts.gate?.hoursOn(at.toLocalDate())?.opens)
        facts.gate.mayBeClosedAt(at) -> Prediction(Outlook.GATE_MAY_BE_CLOSED, null, gateClosesAsEarlyAs = facts.gate?.earliestClose)
        facts.gate.closingSoonAt(at) != null -> Prediction(Outlook.GATE_CLOSING_SOON, null, gateCloses = facts.gate.closingSoonAt(at))
        arriveBy != null && isSummerWeekend(at.toLocalDate()) -> Prediction(Outlook.ARRIVE_BY, advice.sourceUrl, arriveBy = arriveBy)
        facts.passRequired -> Prediction(Outlook.PASS_DAY, null)
        advice != null && advice.busyInSummer && at.month in SUMMER_MONTHS -> Prediction(Outlook.SUMMER_BUSY, advice.sourceUrl)
        else -> null
    }
}
