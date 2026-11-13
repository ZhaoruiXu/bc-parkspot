package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.domain.LotCandidate
import ca.bcparkspot.app.domain.NearestResult
import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.domain.ReportPolicy
import ca.bcparkspot.app.domain.crowdSummaryFor
import ca.bcparkspot.app.domain.bcLocalTime
import ca.bcparkspot.app.domain.findNearestOpenSpot as selectNearest
import ca.bcparkspot.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lot data, the shared reports feed and the starting town. The user's own data lives in LocalStore. */
class ParkingRepository(
    private val policy: ReportPolicy = ReportPolicies.DEFAULT,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _rawParkingLots = MutableStateFlow<List<ParkingLot>>(BCParksDataSource.getInitialParkingLots())
    
    private val _communityReports = MutableStateFlow<List<CommunityReport>>(emptyList())
    val communityReports: StateFlow<List<CommunityReport>> = _communityReports.asStateFlow()

    private val _parkingLots = MutableStateFlow<List<ParkingLot>>(computeConsensusLots(_rawParkingLots.value, _communityReports.value))
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

    private val _currentUserLocation = MutableStateFlow(BCParksDataSource.sampleUserLocations[0])
    val currentUserLocation: StateFlow<UserLocation> = _currentUserLocation.asStateFlow()

    fun setUserLocation(location: UserLocation) {
        _currentUserLocation.value = location
    }

    private fun computeConsensusLots(
        baseLots: List<ParkingLot>,
        reports: List<CommunityReport>,
        nowMs: Long = clock()
    ): List<ParkingLot> {
        val shared = reports.groupBy { it.lotId }
        val bcNow = bcLocalTime(nowMs)
        return baseLots.map { lot ->
            lot.copy(
                crowd = crowdSummaryFor(shared[lot.id].orEmpty(), nowMs, policy),
                prediction = lot.predictionAt(bcNow),
                today = lot.todayAt(bcNow)
            )
        }
    }

    /** 1.1: the device's own reports that Room still holds as pending never change a lot's status. */
    fun updateCommunityReports(serverReports: List<CommunityReport>, pendingOwnIds: Set<String> = emptySet()) {
        val shared = serverReports.filter { it.id !in pendingOwnIds }
        _communityReports.value = shared
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, shared)
    }

    fun recalculateLotConsensus() {
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, _communityReports.value)
    }

    fun findNearestOpenSpot(
        origin: UserLocation,
        targetParkId: String? = null
    ): Pair<NearestResult, NavigationRoute?> {
        val nowMs = clock()
        val lots = computeConsensusLots(_rawParkingLots.value, _communityReports.value, nowMs)
        _parkingLots.value = lots
        val candidates = lots.map { lot ->
            val driveMinutes = BCParksDataSource.estimateDriveMinutes(origin, lot)
            val arrival = bcLocalTime(nowMs + TimeUnit.MINUTES.toMillis(driveMinutes.toLong()))
            LotCandidate(lot.id, lot.parkId, driveMinutes, lot.crowd.state, lot.predictionAt(arrival)?.outlook)
        }
        val result = selectNearest(candidates, targetParkId)
        val chosenId = when (result) {
            is NearestResult.Confirmed -> result.candidate.lotId
            is NearestResult.Fallback -> result.candidate.lotId
            NearestResult.NothingQualifies -> null
        }
        val route = lots.firstOrNull { it.id == chosenId }?.let { BCParksDataSource.generateRoute(origin, it, lots) }
        return result to route
    }

    fun getRouteToLot(
        origin: UserLocation,
        destinationLot: ParkingLot
    ): NavigationRoute {
        return BCParksDataSource.generateRoute(origin, destinationLot, _parkingLots.value)
    }
}
