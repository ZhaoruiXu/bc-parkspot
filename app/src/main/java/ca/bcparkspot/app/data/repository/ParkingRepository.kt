package ca.bcparkspot.app.data.repository

import ca.bcparkspot.app.data.local.dao.ParkingDao
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParkingRepository(
    private val parkingDao: ParkingDao
) {
    private val _rawParkingLots = MutableStateFlow<List<ParkingLot>>(BCParksDataSource.getInitialParkingLots())
    
    private val _pendingLocalReports = MutableStateFlow<List<CommunityReport>>(emptyList())
    private val _communityReports = MutableStateFlow<List<CommunityReport>>(emptyList())
    val communityReports: StateFlow<List<CommunityReport>> = _communityReports.asStateFlow()

    private val _parkingLots = MutableStateFlow<List<ParkingLot>>(computeConsensusLots(_rawParkingLots.value, _communityReports.value))
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

    private val _currentUserLocation = MutableStateFlow(BCParksDataSource.sampleUserLocations[0])
    val currentUserLocation: StateFlow<UserLocation> = _currentUserLocation.asStateFlow()

    // Room Database Flows
    val savedLots: Flow<List<SavedLotEntity>> = parkingDao.getAllSavedLots()
    val parkedCar: Flow<ParkedCarEntity?> = parkingDao.getParkedCar()

    fun isLotSaved(lotId: String): Flow<Boolean> = parkingDao.isLotSaved(lotId)

    fun setUserLocation(location: UserLocation) {
        _currentUserLocation.value = location
    }

    private fun computeConsensusLots(
        baseLots: List<ParkingLot>,
        reports: List<CommunityReport>
    ): List<ParkingLot> {
        return baseLots.map { lot ->
            val consensus = CrowdAggregationEngine.computeConsensus(lot.id, reports)
            lot.copy(crowdConsensus = consensus)
        }
    }

    fun updateCommunityReports(remoteReports: List<CommunityReport>) {
        val merged = (_pendingLocalReports.value + remoteReports).distinctBy { it.id }
        _communityReports.value = merged
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, merged)
    }

    fun markReportSynced(tempId: String) {
        _pendingLocalReports.value = _pendingLocalReports.value.filter { it.id != tempId }
        val currentReports = _communityReports.value.filter { it.id != tempId }
        _communityReports.value = currentReports
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, currentReports)
    }

    suspend fun toggleSaveLot(lot: ParkingLot, isSaved: Boolean) {
        if (isSaved) {
            parkingDao.deleteSavedLot(lot.id)
        } else {
            parkingDao.insertSavedLot(
                SavedLotEntity(
                    lotId = lot.id,
                    parkName = lot.parkName,
                    lotName = lot.lotName,
                    region = lot.region.displayName,
                    userNotes = lot.proTips
                )
            )
        }
    }

    suspend fun saveParkedCarLocation(
        parkName: String,
        lotName: String,
        latitude: Double,
        longitude: Double,
        stallNumber: String,
        notes: String,
        isExactGpsLocation: Boolean = false
    ) {
        parkingDao.saveParkedCar(
            ParkedCarEntity(
                id = 1,
                parkName = parkName,
                lotName = lotName,
                latitude = latitude,
                longitude = longitude,
                stallNumber = stallNumber,
                notes = notes,
                timestamp = System.currentTimeMillis(),
                isExactGpsLocation = isExactGpsLocation
            )
        )
    }

    suspend fun clearParkedCar() {
        parkingDao.clearParkedCar()
    }

    suspend fun clearAllLocalUserData() {
        parkingDao.clearAllSavedLots()
        parkingDao.clearParkedCar()
    }

    fun submitLocalReport(
        lotId: String,
        userId: String,
        reporterName: String,
        status: AvailabilityStatus,
        comment: String,
        spots: Int?,
        isPendingSync: Boolean = true
    ): String {
        val tempId = "pending_${System.currentTimeMillis()}"
        val newReport = CommunityReport(
            id = tempId,
            userId = userId,
            lotId = lotId,
            reporterName = reporterName.ifBlank { "Park Visitor" },
            minutesAgo = 0,
            statusReported = status,
            comment = comment,
            spotsEstimate = spots,
            timestampMs = System.currentTimeMillis(),
            isPendingSync = isPendingSync
        )
        _pendingLocalReports.value = listOf(newReport) + _pendingLocalReports.value
        _communityReports.value = listOf(newReport) + _communityReports.value
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, _communityReports.value)
        return tempId
    }

    fun recalculateLotConsensus() {
        _parkingLots.value = computeConsensusLots(_rawParkingLots.value, _communityReports.value)
    }

    fun findNearestOpenSpot(
        origin: UserLocation,
        targetParkId: String? = null
    ): Pair<ParkingLot, NavigationRoute>? {
        // Honest availability: ONLY lots with confirmed AVAILABLE or FILLING_FAST status
        // Unknown / baseline or Full / Closed lots are strictly excluded from confirmed open spots
        val candidateLots = _parkingLots.value.filter { lot ->
            val matchesPark = targetParkId == null || lot.parkId == targetParkId
            matchesPark && (lot.effectiveStatus == AvailabilityStatus.AVAILABLE || lot.effectiveStatus == AvailabilityStatus.FILLING_FAST)
        }

        if (candidateLots.isEmpty()) {
            val anyConfirmedOpen = _parkingLots.value.filter {
                it.effectiveStatus == AvailabilityStatus.AVAILABLE || it.effectiveStatus == AvailabilityStatus.FILLING_FAST
            }
            val nearest = anyConfirmedOpen.minByOrNull { lot ->
                BCParksDataSource.calculateDistanceKm(origin.latitude, origin.longitude, lot.latitude, lot.longitude)
            } ?: return null

            val route = BCParksDataSource.generateRoute(origin, nearest, _parkingLots.value)
            return Pair(nearest, route)
        }

        val nearest = candidateLots.minByOrNull { lot ->
            BCParksDataSource.calculateDistanceKm(origin.latitude, origin.longitude, lot.latitude, lot.longitude)
        } ?: return null

        val route = BCParksDataSource.generateRoute(origin, nearest, _parkingLots.value)
        return Pair(nearest, route)
    }

    fun getRouteToLot(
        origin: UserLocation,
        destinationLot: ParkingLot
    ): NavigationRoute {
        return BCParksDataSource.generateRoute(origin, destinationLot, _parkingLots.value)
    }
}
