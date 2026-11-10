package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.model.*
import java.time.DayOfWeek
import java.time.LocalTime

object BCParksDataSource {

    val sampleUserLocations = listOf(
        UserLocation("Downtown Vancouver (Waterfront)", 49.2859, -123.1113),
        UserLocation("North Vancouver (Lonsdale)", 49.3108, -123.0805),
        UserLocation("Squamish (Hwy 99 Corridor)", 49.7016, -123.1558),
        UserLocation("Burnaby / New Westminster", 49.2276, -123.0076),
        UserLocation("Tri-Cities (Coquitlam)", 49.2789, -122.7915),
        UserLocation("Whistler Village", 50.1163, -122.9574)
    )

    fun getInitialParkingLots(): List<ParkingLot> = listOf(
        // 1. Garibaldi - Rubble Creek (Day-Use Pass required Fri-Mon & statutory holidays)
        ParkingLot(
            id = "garibaldi_rubble_creek",
            parkId = "garibaldi",
            parkName = "Garibaldi Provincial Park",
            lotName = "Rubble Creek Trailhead (Garibaldi Lake & Black Tusk)",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 165,
            isDayUsePassRequired = true,
            passInfo = "Mandatory vehicle day-use pass required June 12 to Oct 12, Friday through Monday and statutory holidays. No pass required Tuesday to Thursday.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 6,
                    startDay = 12,
                    endMonth = 10,
                    endDay = 12,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                    includesHolidays = true,
                    description = "June 12 – Oct 12: Friday to Monday and BC statutory holidays"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Parking",
            gateHours = "Open 24/7 (Upper gate closed if winter snow unplowed)",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = true,
            isDogFriendly = false,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.NO_SERVICE,
            latitude = 49.9575,
            longitude = -123.1215,
            highwayAccess = "Hwy 99 (Sea-to-Sky) -> Daisy Lake Rd exit 33km north of Squamish",
            overflowLotId = "cheakamus_lake_lot",
            overflowLotName = "Cheakamus Lake Trailhead Lot",
            overflowDistanceKm = 14.2,
            proTips = "No cellular reception once leaving Hwy 99. Download day-pass QR code before leaving Squamish or Whistler.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 2. Garibaldi - Cheakamus Lake (Daily June 13 to Sept 7, then Fri-Mon to Oct 12)
        ParkingLot(
            id = "cheakamus_lake_lot",
            parkId = "garibaldi",
            parkName = "Garibaldi Provincial Park",
            lotName = "Cheakamus Lake Trailhead (Forest Service Road)",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 55,
            isDayUsePassRequired = true,
            passInfo = "Mandatory vehicle day-use pass required daily June 13 to Sept 7; then Friday through Monday and statutory holidays from Sept 8 to Oct 12.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 6,
                    startDay = 13,
                    endMonth = 9,
                    endDay = 7,
                    daysOfWeek = null, // Daily
                    includesHolidays = true,
                    description = "Peak: Daily June 13 to Sept 7"
                ),
                PassRulePeriod(
                    startMonth = 9,
                    startDay = 8,
                    endMonth = 10,
                    endDay = 12,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                    includesHolidays = true,
                    description = "Shoulder: Sept 8 to Oct 12, Friday through Monday and statutory holidays"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free with Valid Day-Pass",
            gateHours = "Open 24/7 (Seasonal FSR - unmaintained in winter)",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = false,
            isDogFriendly = false,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.NO_SERVICE,
            latitude = 50.0468,
            longitude = -123.0035,
            highwayAccess = "BC-99 at Function Junction -> Cheakamus Lake FSR (8km rough gravel)",
            overflowLotId = "garibaldi_rubble_creek",
            overflowLotName = "Rubble Creek Main Trailhead Lot",
            overflowDistanceKm = 14.2,
            proTips = "8 km unpaved Forest Service Road with potholes. High clearance vehicle recommended.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 3. Joffre Lakes (Per-person trail pass May 11 to Oct 25, daily; cultural closures June 20-27 and Sept 8-30; no stall claim)
        ParkingLot(
            id = "joffre_lakes_main",
            parkId = "joffre_lakes",
            parkName = "Joffre Lakes Provincial Park",
            lotName = "Upper Highway Main Lot (Lower, Middle & Upper Lakes)",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = null, // Passes are per person, not per vehicle; vehicle parking is unticketed and limited
            isDayUsePassRequired = true,
            passInfo = "Mandatory individual trail pass required May 11 to Oct 25, daily. Full park closures in effect June 20-27 and Sept 8-30. Passes are per person, not per vehicle.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 5,
                    startDay = 11,
                    endMonth = 10,
                    endDay = 25,
                    daysOfWeek = null, // Daily
                    includesHolidays = true,
                    description = "May 11 to Oct 25: Daily individual trail pass"
                )
            ),
            closures = listOf(
                ParkClosure(
                    startMonth = 6,
                    startDay = 20,
                    endMonth = 6,
                    endDay = 27,
                    reason = "Lil'wat and N'Quatqua National Indigenous Peoples Day and Cultural Practice Closure"
                ),
                ParkClosure(
                    startMonth = 9,
                    startDay = 8,
                    endMonth = 9,
                    endDay = 30,
                    reason = "Truth and Reconciliation, Fall Harvest, and Environmental Restoration Closure"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Parking with Valid Trail Pass",
            gateHours = "Daylight hours (7:00 AM – 9:00 PM)",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = false,
            isDogFriendly = false,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.NO_SERVICE,
            latitude = 50.36969,
            longitude = -122.49928,
            highwayAccess = "Hwy 99 (Duffey Lake Rd) ~60km east of Pemberton",
            overflowLotId = "joffre_lower_overflow",
            overflowLotName = "Joffre Lower Gravel Lot",
            overflowDistanceKm = 0.6,
            proTips = "Pass quota is per person, not per vehicle. Zero tolerance roadside towing on Hwy 99. Park closes completely June 20-27 and Sept 8-30.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 4. Golden Ears - Alouette Lake (South Beach)
        ParkingLot(
            id = "golden_ears_alouette",
            parkId = "golden_ears",
            parkName = "Golden Ears Provincial Park",
            lotName = "Alouette Lake South Beach Lot & Lower Falls Trail",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.BEACH_SWIMMING,
            totalSpots = 400,
            isDayUsePassRequired = true,
            passInfo = "Vehicle day-use pass required May 15 to Sept 7, Friday to Monday and statutory holidays. No pass required after 3:30 PM.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 5,
                    startDay = 15,
                    endMonth = 9,
                    endDay = 7,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                    includesHolidays = true,
                    cutoffTime = LocalTime.of(15, 30),
                    description = "May 15 to Sept 7: Fri-Mon & holidays, 7:00 AM - 3:30 PM"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free with Valid Vehicle Pass",
            gateHours = "7:00 AM – 11:00 PM (Winter gate closes 5:30 PM)",
            isGateOpen = true,
            evChargingSpots = 4,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.WEAK_CELL,
            latitude = 49.2974,
            longitude = -122.4831,
            highwayAccess = "232 St -> 132 Ave -> Golden Ears Pkwy in Maple Ridge",
            overflowLotId = "golden_ears_gold_creek",
            overflowLotName = "Gold Creek Trailhead Lot",
            overflowDistanceKm = 3.5,
            proTips = "Capacity ~400 vehicles. No pass needed after 3:30 PM. Screenshot pass QR code before park boundary as cell signal drops.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 5. Golden Ears - Gold Creek
        ParkingLot(
            id = "golden_ears_gold_creek",
            parkId = "golden_ears",
            parkName = "Golden Ears Provincial Park",
            lotName = "Gold Creek Trailhead Lot (Lower Falls & East Canyon)",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 120,
            isDayUsePassRequired = true,
            passInfo = "Vehicle day-use pass required May 15 to Sept 7, Friday to Monday and statutory holidays. No pass required after 3:30 PM.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 5,
                    startDay = 15,
                    endMonth = 9,
                    endDay = 7,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                    includesHolidays = true,
                    cutoffTime = LocalTime.of(15, 30),
                    description = "May 15 to Sept 7: Fri-Mon & holidays, 7:00 AM - 3:30 PM"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free with Valid Vehicle Pass",
            gateHours = "7:00 AM – 11:00 PM",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.NO_SERVICE,
            latitude = 49.33372,
            longitude = -122.45743,
            highwayAccess = "Golden Ears Pkwy past Alouette Lake campground entrance",
            overflowLotId = "golden_ears_alouette",
            overflowLotName = "Alouette Lake South Beach Lot",
            overflowDistanceKm = 3.5,
            proTips = "Capacity ~120 vehicles. Access to Gold Creek Falls. Passes strictly checked at park portal.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 6. Golden Ears - West Canyon
        ParkingLot(
            id = "golden_ears_west_canyon",
            parkId = "golden_ears",
            parkName = "Golden Ears Provincial Park",
            lotName = "West Canyon Trailhead Lot (Golden Ears Peak Route)",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 80,
            isDayUsePassRequired = true,
            passInfo = "Vehicle day-use pass required May 15 to Sept 7, Friday to Monday and statutory holidays. No pass required after 3:30 PM.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 5,
                    startDay = 15,
                    endMonth = 9,
                    endDay = 7,
                    daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                    includesHolidays = true,
                    cutoffTime = LocalTime.of(15, 30),
                    description = "May 15 to Sept 7: Fri-Mon & holidays, 7:00 AM - 3:30 PM"
                )
            ),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free with Valid Vehicle Pass",
            gateHours = "7:00 AM – 11:00 PM",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = false,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.NO_SERVICE,
            latitude = 49.32729,
            longitude = -122.46290,
            highwayAccess = "Golden Ears Pkwy -> West Canyon access spur",
            overflowLotId = "golden_ears_alouette",
            overflowLotName = "Alouette Lake South Beach Lot",
            overflowDistanceKm = 4.1,
            proTips = "Capacity ~80 vehicles. Starting trailhead for Alder Flats and Golden Ears peak backcountry hikers.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 7. Mount Seymour - Lot P1 Lower (Winter pass: 2026-27 dates TBA, free trail users)
        ParkingLot(
            id = "seymour_lower_p1",
            parkId = "mount_seymour",
            parkName = "Mount Seymour Provincial Park",
            lotName = "Lot P1 Lower (Provincial Park Trail Users - Dog Mountain)",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.SNOWSHOE_WINTER,
            totalSpots = 150,
            isDayUsePassRequired = true,
            passInfo = "Winter 2026-27 pass dates TBA - check bcparks.ca",
            passRulePeriods = emptyList(),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Trailhead Parking",
            gateHours = "7:00 AM – 10:00 PM (Lower gate locked at 10 PM)",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3582,
            longitude = -122.9512,
            highwayAccess = "Mount Seymour Parkway -> Mount Seymour Rd (KM 10)",
            overflowLotId = "seymour_lower_p5",
            overflowLotName = "Lower Lot P5 Trailhead",
            overflowDistanceKm = 1.2,
            proTips = "Designated provincial trail lot (free access). Winter tire regulations legally enforced Oct 1 to Apr 30 on Mount Seymour Road.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 8. Mount Seymour - Lower Lot P5
        ParkingLot(
            id = "seymour_lower_p5",
            parkId = "mount_seymour",
            parkName = "Mount Seymour Provincial Park",
            lotName = "Lower Lot P5 (Provincial Trail Parking)",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.SNOWSHOE_WINTER,
            totalSpots = 110,
            isDayUsePassRequired = true,
            passInfo = "Winter 2026-27 pass dates TBA - check bcparks.ca",
            passRulePeriods = emptyList(),
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Trail Parking",
            gateHours = "7:00 AM – 10:00 PM",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3621,
            longitude = -122.9501,
            highwayAccess = "Mount Seymour Parkway -> Mount Seymour Rd (KM 11.5)",
            overflowLotId = "seymour_lower_p1",
            overflowLotName = "Mount Seymour P1 Lower Lot",
            overflowDistanceKm = 1.2,
            proTips = "Official provincial park user pass lot. Ski resort upper lots P2/P3 are reserved for resort lift ticket holders.",
            sourceUrl = "https://bcparks.ca/reservations/day-use-passes/",
            verifiedAt = "2026-10-28"
        ),

        // 9. Cypress Provincial Park
        ParkingLot(
            id = "cypress_downhill_lot",
            parkId = "cypress",
            parkName = "Cypress Provincial Park",
            lotName = "Black Mountain & Bowen Lookout Trailhead (Lot 2)",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.SNOWSHOE_WINTER,
            totalSpots = 450,
            isDayUsePassRequired = false,
            passInfo = "Summer hiking requires no day-use pass. Winter backcountry trail access ticket required for Black Mountain.",
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Provincial Park",
            gateHours = "6:00 AM – 11:00 PM",
            isGateOpen = true,
            evChargingSpots = 6,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3958,
            longitude = -123.2038,
            highwayAccess = "Hwy 1 Exit 8 -> Cypress Bowl Rd (15km paved mountain parkway)",
            overflowLotId = "stawamus_chief_lot",
            overflowLotName = "Stawamus Chief Trailhead Lot",
            overflowDistanceKm = 38.0,
            proTips = "Lookout viewpoint lot fills first on clear sunset evenings. Winter tires mandatory.",
            sourceUrl = "https://bcparks.ca/cypress-park/"
        ),

        // 10. Stawamus Chief Provincial Park
        ParkingLot(
            id = "stawamus_chief_lot",
            parkId = "stawamus_chief",
            parkName = "Stawamus Chief Provincial Park",
            lotName = "Chief Campground & Peaks Trailhead Lot",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 110,
            isDayUsePassRequired = false,
            passInfo = "Day-use pass not currently required. Parking is first-come, first-served.",
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free (Provincial Park)",
            gateHours = "Open 24/7",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = false,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.67924,
            longitude = -123.15468,
            highwayAccess = "Direct access off BC-99 just south of Squamish",
            overflowLotId = "shannon_falls_lot",
            overflowLotName = "Shannon Falls Main Lot (Connector Trail)",
            overflowDistanceKm = 1.1,
            proTips = "Fills up by 8:30 AM on sunny summer weekends. Use the flat pedestrian connector from Shannon Falls if full.",
            sourceUrl = "https://bcparks.ca/stawamus-chief-park/"
        ),

        // 11. Buntzen Lake Recreation Area (BC Hydro Pass)
        ParkingLot(
            id = "buntzen_lake_main",
            parkId = "buntzen_lake",
            parkName = "Buntzen Lake Recreation Area",
            lotName = "Anmore Main Beach & Diez Vistas Trailhead",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.BC_HYDRO,
            type = RecreationType.BEACH_SWIMMING,
            totalSpots = 580,
            isDayUsePassRequired = true,
            passInfo = "Mandatory Free BC Hydro Parking Pass required May 1 – Sept 30. Vehicle plate tied to reservation.",
            passRulePeriods = listOf(
                PassRulePeriod(
                    startMonth = 5,
                    startDay = 1,
                    endMonth = 9,
                    endDay = 30,
                    daysOfWeek = null,
                    includesHolidays = true,
                    description = "May 1 – Sept 30: Daily BC Hydro online reservation required"
                )
            ),
            feeType = ParkingFeeType.FREE_HYDRO_PASS,
            feeDescription = "Free with BC Hydro Reservation",
            gateHours = "8:00 AM – 8:00 PM (Gates strictly locked at 8 PM)",
            isGateOpen = true,
            evChargingSpots = 4,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.WEAK_CELL,
            latitude = 49.3417,
            longitude = -122.8617,
            highwayAccess = "Ioco Rd -> Sunnyside Rd (Anmore)",
            overflowLotId = "sasamat_white_pine",
            overflowLotName = "Sasamat Lake White Pine Beach Lot",
            overflowDistanceKm = 4.2,
            proTips = "Passes open daily at 7:00 AM for the following day on bchydro.com. Gate security scans passes at entry kiosk.",
            sourceUrl = "https://www.bchydro.com/community/recreation_areas/buntzen_lake.html",
            verifiedAt = "2026-10-28"
        ),

        // 12. Alice Lake Provincial Park
        ParkingLot(
            id = "alice_lake_main",
            parkId = "alice_lake",
            parkName = "Alice Lake Provincial Park",
            lotName = "Four Lakes Loop & Day Use Beach Lot",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.NATURE_WALK,
            totalSpots = 180,
            isDayUsePassRequired = false,
            passInfo = "No day-use pass required. Day parking is free until 11 PM.",
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Provincial Park",
            gateHours = "7:00 AM – 11:00 PM",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.7825,
            longitude = -123.1208,
            highwayAccess = "BC-99 Exit Alice Lake Rd 11km north of downtown Squamish",
            overflowLotId = "stawamus_chief_lot",
            overflowLotName = "Stawamus Chief Trailhead Lot",
            overflowDistanceKm = 12.8,
            proTips = "Great sheltered family alternative when Garibaldi passes sell out.",
            sourceUrl = "https://bcparks.ca/alice-lake-park/"
        ),

        // 13. Lynn Canyon Park (Municipal Pay Parking)
        ParkingLot(
            id = "lynn_canyon_park",
            parkId = "lynn_canyon",
            parkName = "Lynn Canyon Park (Suspension Bridge)",
            lotName = "Suspension Bridge & Ecology Centre Lot",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.MUNICIPAL,
            type = RecreationType.NATURE_WALK,
            totalSpots = 120,
            isDayUsePassRequired = false,
            passInfo = "No day-use pass required. Municipal pay parking in effect.",
            feeType = ParkingFeeType.PAID_HOURLY,
            feeDescription = "$3.00 / hour (March 1 – Oct 31, 8 AM - 6 PM)",
            gateHours = "7:00 AM – 9:00 PM",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = false,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3435,
            longitude = -123.0189,
            highwayAccess = "Lynn Valley Rd -> Peters Rd in North Vancouver",
            overflowLotId = "deep_cove_panorama",
            overflowLotName = "Deep Cove Panorama Park Lot",
            overflowDistanceKm = 8.1,
            proTips = "Pay via PayByPhone app or ticket kiosks. 4-hour parking maximum enforced.",
            sourceUrl = "https://www.dnv.org/parks/lynn-canyon-park/"
        ),

        // 14. Deep Cove / Quarry Rock
        ParkingLot(
            id = "deep_cove_panorama",
            parkId = "deep_cove",
            parkName = "Deep Cove / Quarry Rock",
            lotName = "Panorama Park & Quarry Rock Trailhead",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.MUNICIPAL,
            type = RecreationType.ALPINE_HIKING,
            totalSpots = 85,
            isDayUsePassRequired = false,
            passInfo = "No pass required. Seasonal pay parking in effect.",
            feeType = ParkingFeeType.PAID_HOURLY,
            feeDescription = "$3.00 / hour (March 1 – Oct 31)",
            gateHours = "6:00 AM – 10:00 PM",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = false,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3256,
            longitude = -122.9492,
            highwayAccess = "Deep Cove Rd -> Gallant Ave -> Panorama Dr",
            overflowLotId = "lynn_canyon_park",
            overflowLotName = "Lynn Canyon Ecology Centre Lot",
            overflowDistanceKm = 8.1,
            proTips = "Panorama Dr is very tight. Park at Myrtle Park lot on Cove Cliff Rd and take the 7-minute forest path to the village.",
            sourceUrl = "https://www.dnv.org/parks/deep-cove"
        ),

        // 15. Belcarra Regional Park
        ParkingLot(
            id = "belcarra_picnic_area",
            parkId = "belcarra",
            parkName = "Belcarra Regional Park",
            lotName = "Belcarra Picnic Area & Admiralty Point Trailhead",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.METRO_VANCOUVER,
            type = RecreationType.BOATING_PADDLING,
            totalSpots = 210,
            isDayUsePassRequired = false,
            passInfo = "No pass required. Seasonal pay parking in effect.",
            feeType = ParkingFeeType.PAID_SEASONAL,
            feeDescription = "$2.50 / hour (April 1 – Sept 30)",
            gateHours = "7:00 AM – 9:00 PM",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.WEAK_CELL,
            latitude = 49.3175,
            longitude = -122.9268,
            highwayAccess = "Belcarra Bay Rd -> Tum-tumay-whueton Dr",
            overflowLotId = "sasamat_white_pine",
            overflowLotName = "Sasamat Lake White Pine Beach Lot",
            overflowDistanceKm = 5.6,
            proTips = "Boat launch gets crowded early. Pay parking is enforced 7 days a week in season.",
            sourceUrl = "https://metrovancouver.org/services/regional-parks/park/belcarra-regional-park"
        ),

        // 16. Shannon Falls Provincial Park
        ParkingLot(
            id = "shannon_falls_lot",
            parkId = "shannon_falls",
            parkName = "Shannon Falls Provincial Park",
            lotName = "Main Falls Viewing & Sea to Sky Gondola Base Lot",
            region = ParkRegion.SEA_TO_SKY,
            authority = ParkAuthority.BC_PARKS,
            type = RecreationType.NATURE_WALK,
            totalSpots = 140,
            isDayUsePassRequired = false,
            passInfo = "No pass required. Free public day parking.",
            feeType = ParkingFeeType.FREE,
            feeDescription = "Free Public Provincial Park",
            gateHours = "Daylight hours (6:00 AM – 10:00 PM)",
            isGateOpen = true,
            evChargingSpots = 4,
            isRvDashAllowed = true,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.6698,
            longitude = -123.1558,
            highwayAccess = "Direct exit off BC-99 northbound/southbound",
            overflowLotId = "stawamus_chief_lot",
            overflowLotName = "Stawamus Chief Trailhead Lot",
            overflowDistanceKm = 1.1,
            proTips = "Ideal flat overflow parking for Stawamus Chief with a paved 10-minute forest connector path.",
            sourceUrl = "https://bcparks.ca/shannon-falls-park/"
        ),

        // 17. Whytecliff Park
        ParkingLot(
            id = "whytecliff_park_lot",
            parkId = "whytecliff",
            parkName = "Whytecliff Park (Marine Sanctuary)",
            lotName = "Whytecliff Beach & Scuba Diver Lot",
            region = ParkRegion.METRO_VANCOUVER,
            authority = ParkAuthority.MUNICIPAL,
            type = RecreationType.BEACH_SWIMMING,
            totalSpots = 95,
            isDayUsePassRequired = false,
            passInfo = "No pass required. Municipal seasonal pay parking.",
            feeType = ParkingFeeType.PAID_SEASONAL,
            feeDescription = "$3.50 / hour (May 1 – Oct 15)",
            gateHours = "6:00 AM – 10:00 PM",
            isGateOpen = true,
            evChargingSpots = 0,
            isRvDashAllowed = false,
            isDogFriendly = true,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3719,
            longitude = -123.2925,
            highwayAccess = "Marine Dr -> Citadel Dr in Horseshoe Bay",
            overflowLotId = "lynn_canyon_park",
            overflowLotName = "Lynn Canyon Ecology Centre Lot",
            overflowDistanceKm = 24.0,
            proTips = "Canada's first Marine Protected Area. Prime scuba diving loading spots.",
            sourceUrl = "https://westvancouver.ca/parks-recreation/parks/whytecliff-park"
        ),

        // 18. Sasamat Lake - White Pine Beach
        ParkingLot(
            id = "sasamat_white_pine",
            parkId = "sasamat_lake",
            parkName = "Belcarra - White Pine Beach",
            lotName = "Sasamat Lake Main Beach Lot",
            region = ParkRegion.FRASER_VALLEY,
            authority = ParkAuthority.METRO_VANCOUVER,
            type = RecreationType.BEACH_SWIMMING,
            totalSpots = 260,
            isDayUsePassRequired = false,
            passInfo = "No pass required. Metro Vancouver seasonal pay parking.",
            feeType = ParkingFeeType.PAID_SEASONAL,
            feeDescription = "$2.50 / hour (April 1 – Sept 30)",
            gateHours = "7:00 AM – 9:00 PM (Gates locked at 9 PM)",
            isGateOpen = true,
            evChargingSpots = 2,
            isRvDashAllowed = false,
            isDogFriendly = false,
            hasWashrooms = true,
            hasBearProofBins = true,
            cellSignal = CellSignal.FULL_LTE,
            latitude = 49.3245,
            longitude = -122.8791,
            highwayAccess = "Bedwell Bay Rd -> White Pine Beach Rd",
            overflowLotId = "buntzen_lake_main",
            overflowLotName = "Buntzen Lake Recreation Area",
            overflowDistanceKm = 4.2,
            proTips = "Warmest swimming lake in the region. Fills completely on warm summer days before 10 AM.",
            sourceUrl = "https://metrovancouver.org/services/regional-parks/park/belcarra-regional-park"
        )
    )

    fun getInitialCommunityReports(): List<CommunityReport> = emptyList()

    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val straightLineKm = r * c
        return Math.round(straightLineKm * 1.28 * 10.0) / 10.0
    }

    fun calculateDrivingMinutes(distanceKm: Double, isMountainHighway: Boolean = true): Int {
        val avgSpeedKmh = if (isMountainHighway) 65.0 else 50.0
        val baseHours = distanceKm / avgSpeedKmh
        return (baseHours * 60).toInt().coerceAtLeast(4)
    }

    fun generateRoute(
        origin: UserLocation,
        destination: ParkingLot,
        allLots: List<ParkingLot>
    ): NavigationRoute {
        val distance = calculateDistanceKm(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude
        )
        val isMountain = destination.region == ParkRegion.SEA_TO_SKY
        val estMinutes = calculateDrivingMinutes(distance, isMountain)

        val roadCond = when {
            destination.highwayAccess.contains("FSR", ignoreCase = true) || destination.highwayAccess.contains("gravel", ignoreCase = true) ->
                "Forest Service Road (Gravel & potholes; high clearance vehicle recommended)"
            destination.region == ParkRegion.SEA_TO_SKY ->
                "BC-99 Sea-to-Sky Highway (Paved multi-lane; winter tires/chains mandatory Oct 1 – Apr 30)"
            else ->
                "Paved Regional Highway and municipal access road"
        }

        val alternatives = allLots.filter {
            it.id != destination.id &&
            (it.parkId == destination.parkId || it.region == destination.region) &&
            it.effectiveStatus != AvailabilityStatus.FULL &&
            it.effectiveStatus != AvailabilityStatus.CLOSED
        }.take(3)

        return NavigationRoute(
            originName = origin.name,
            destinationLot = destination,
            totalDistanceKm = distance,
            estimatedMinutes = estMinutes,
            roadConditions = roadCond,
            alternativeLots = alternatives
        )
    }
}
