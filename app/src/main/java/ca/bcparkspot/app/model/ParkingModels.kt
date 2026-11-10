package ca.bcparkspot.app.model

import java.time.LocalDate
import java.time.MonthDay

enum class ParkRegion(val displayName: String) {
    SEA_TO_SKY("Sea-to-Sky (Squamish / Whistler)"),
    METRO_VANCOUVER("Metro Vancouver"),
    FRASER_VALLEY("Fraser Valley & Coquitlam")
}

enum class ParkAuthority(val displayName: String, val officialDomain: String) {
    BC_PARKS("BC Parks", "bcparks.ca"),
    BC_HYDRO("BC Hydro Recreation", "bchydro.com"),
    METRO_VANCOUVER("Metro Vancouver Regional Parks", "metrovancouver.org"),
    MUNICIPAL("District / Municipal", "dnv.org")
}

enum class RecreationType(val displayName: String, val iconName: String) {
    ALPINE_HIKING("Alpine Hiking", "hiking"),
    BEACH_SWIMMING("Beach & Swimming", "pool"),
    BOATING_PADDLING("Boat Launch & Paddling", "kayaking"),
    SNOWSHOE_WINTER("Winter & Snowshoe", "ac_unit"),
    NATURE_WALK("Nature Walk & Picnic", "park")
}

enum class AvailabilityStatus(val label: String) {
    AVAILABLE("Available"),
    FILLING_FAST("Filling Fast"),
    FULL("Full"),
    CLOSED("Gate Closed"),
    UNKNOWN("Unknown / Baseline")
}

object BCHolidaySchedule {
    fun getGoodFriday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        val easterSunday = LocalDate.of(year, month, day)
        return easterSunday.minusDays(2)
    }

    fun isBCHoliday(date: LocalDate): Boolean {
        val month = date.monthValue
        val day = date.dayOfMonth

        // Good Friday (falls on Friday preceding Easter Sunday, e.g. Mar 26, 2027)
        if (date == getGoodFriday(date.year)) return true

        // Fixed date statutory holidays in BC
        if (month == 1 && day == 1) return true   // New Year's Day
        if (month == 7 && day == 1) return true   // Canada Day
        if (month == 9 && day == 30) return true  // National Day for Truth and Reconciliation
        if (month == 11 && day == 11) return true // Remembrance Day
        if (month == 12 && day == 25) return true // Christmas Day
        if (month == 12 && day == 26) return true // Boxing Day

        // Floating Mondays in BC:
        // Family Day: 3rd Monday in February (between Feb 15 and Feb 21)
        if (month == 2 && date.dayOfWeek == java.time.DayOfWeek.MONDAY && (day in 15..21)) return true
        // Victoria Day: Monday preceding May 25 (between May 18 and May 24)
        if (month == 5 && date.dayOfWeek == java.time.DayOfWeek.MONDAY && (day in 18..24)) return true
        // BC Day: 1st Monday in August (between Aug 1 and Aug 7)
        if (month == 8 && date.dayOfWeek == java.time.DayOfWeek.MONDAY && (day in 1..7)) return true
        // Labour Day: 1st Monday in September (between Sept 1 and Sept 7)
        if (month == 9 && date.dayOfWeek == java.time.DayOfWeek.MONDAY && (day in 1..7)) return true
        // Thanksgiving: 2nd Monday in October (between Oct 8 and Oct 14)
        if (month == 10 && date.dayOfWeek == java.time.DayOfWeek.MONDAY && (day in 8..14)) return true

        return false
    }
}

data class PassRulePeriod(
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
    val daysOfWeek: Set<java.time.DayOfWeek>? = null, // null means all days of the week
    val includesHolidays: Boolean = true,
    val cutoffTime: java.time.LocalTime? = null, // e.g. LocalTime.of(15, 30) -> no pass required after 3:30 pm
    val startTime: java.time.LocalTime? = null,
    val description: String = ""
) {
    fun isPassRequired(date: LocalDate, time: java.time.LocalTime): Boolean {
        val start = MonthDay.of(startMonth, startDay)
        val end = MonthDay.of(endMonth, endDay)
        val current = MonthDay.from(date)

        val inSeason = if (start <= end) current in start..end else (current >= start || current <= end)
        if (!inSeason) return false

        val dayMatches = daysOfWeek == null ||
            date.dayOfWeek in daysOfWeek ||
            (includesHolidays && BCHolidaySchedule.isBCHoliday(date))

        if (!dayMatches) return false

        if (cutoffTime != null && time >= cutoffTime) return false
        if (startTime != null && time < startTime) return false

        return true
    }
}

data class ParkClosure(
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
    val reason: String
) {
    fun isClosed(date: LocalDate): Boolean {
        val start = MonthDay.of(startMonth, startDay)
        val end = MonthDay.of(endMonth, endDay)
        val current = MonthDay.from(date)
        return if (start <= end) current in start..end else (current >= start || current <= end)
    }
}

enum class CrowdFreshness {
    ACTIVE,   // Within 90 minutes
    STALE,    // 90 minutes to 180 minutes (3 hours)
    EXPIRED   // Over 180 minutes (fallback to guidebook baseline)
}

data class CrowdConsensus(
    val lotId: String,
    val status: AvailabilityStatus,
    val freshness: CrowdFreshness,
    val distinctHikerCount: Int,
    val latestReportMinutesAgo: Long,
    val latestComment: String? = null,
    val spotsEstimate: Int? = null
) {
    val displaySummary: String
        get() = when {
            freshness == CrowdFreshness.EXPIRED || status == AvailabilityStatus.UNKNOWN -> {
                if (distinctHikerCount == 1) {
                    "Guidebook Baseline (1 report - quorum of 2 required to confirm)"
                } else {
                    "Guidebook Baseline (No recent confirmed crowd reports)"
                }
            }
            freshness == CrowdFreshness.ACTIVE -> {
                "Hikers confirm ${status.label} ($distinctHikerCount hikers • ${latestReportMinutesAgo}m ago)"
            }
            freshness == CrowdFreshness.STALE -> {
                "Likely ${status.label} ($distinctHikerCount hikers • reported ${latestReportMinutesAgo}m ago • may have changed)"
            }
            else -> "Guidebook Baseline"
        }
}

enum class ParkingFeeType(val label: String) {
    FREE("Free Public Parking"),
    PAID_SEASONAL("Pay Parking (Seasonal)"),
    PAID_HOURLY("Pay Parking ($/hr)"),
    FREE_HYDRO_PASS("Free (Online Pass Req.)")
}

enum class CellSignal(val label: String) {
    FULL_LTE("Full LTE/5G"),
    WEAK_CELL("Spotty / 3G Only"),
    NO_SERVICE("No Cell Service")
}

enum class PassRequirementStatus {
    REQUIRED,
    NOT_REQUIRED,
    UNKNOWN
}

data class ParkingLot(
    val id: String,
    val parkId: String,
    val parkName: String,
    val lotName: String,
    val region: ParkRegion,
    val authority: ParkAuthority,
    val type: RecreationType,
    val totalSpots: Int? = null, // Nullable: Joffre passes are per person, not per parking stall
    val isDayUsePassRequired: Boolean,
    val passInfo: String,
    val passRulePeriods: List<PassRulePeriod> = emptyList(),
    val closures: List<ParkClosure> = emptyList(),
    val feeType: ParkingFeeType,
    val feeDescription: String,
    val gateHours: String,
    val isGateOpen: Boolean = true,
    val evChargingSpots: Int = 0,
    val isRvDashAllowed: Boolean = true,
    val isDogFriendly: Boolean = true,
    val hasWashrooms: Boolean = true,
    val hasBearProofBins: Boolean = true,
    val cellSignal: CellSignal = CellSignal.FULL_LTE,
    val latitude: Double,
    val longitude: Double,
    val highwayAccess: String,
    val overflowLotId: String? = null,
    val overflowLotName: String? = null,
    val overflowDistanceKm: Double? = null,
    val proTips: String = "",
    val sourceUrl: String = "https://bcparks.ca/reservations/day-use-passes/",
    val verifiedAt: String? = null,
    val crowdConsensus: CrowdConsensus? = null
) {
    val effectiveStatus: AvailabilityStatus
        get() {
            val consensus = crowdConsensus ?: return AvailabilityStatus.UNKNOWN
            return if (consensus.freshness == CrowdFreshness.EXPIRED) {
                AvailabilityStatus.UNKNOWN
            } else {
                consensus.status
            }
        }

    fun getActiveClosure(today: LocalDate = LocalDate.now()): ParkClosure? {
        return closures.firstOrNull { it.isClosed(today) }
    }

    fun getPassState(
        today: LocalDate = LocalDate.now(),
        time: java.time.LocalTime = java.time.LocalTime.now()
    ): PassRequirementStatus {
        if (!isDayUsePassRequired) return PassRequirementStatus.NOT_REQUIRED
        if (getActiveClosure(today) != null) return PassRequirementStatus.NOT_REQUIRED // Park is closed

        if (passRulePeriods.isEmpty()) {
            return PassRequirementStatus.UNKNOWN // No published pass dates / TBA
        }

        return if (passRulePeriods.any { it.isPassRequired(today, time) }) {
            PassRequirementStatus.REQUIRED
        } else {
            PassRequirementStatus.NOT_REQUIRED
        }
    }

    fun isPassRequiredToday(
        today: LocalDate = LocalDate.now(),
        time: java.time.LocalTime = java.time.LocalTime.now()
    ): Boolean {
        return getPassState(today, time) == PassRequirementStatus.REQUIRED
    }
}

data class NavigationRoute(
    val originName: String,
    val destinationLot: ParkingLot,
    val totalDistanceKm: Double,
    val estimatedMinutes: Int,
    val roadConditions: String,
    val alternativeLots: List<ParkingLot> = emptyList()
)

data class CommunityReport(
    val id: String,
    val userId: String = "",
    val lotId: String,
    val reporterName: String,
    val minutesAgo: Int,
    val statusReported: AvailabilityStatus,
    val comment: String,
    val spotsEstimate: Int? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false
)

data class UserLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
