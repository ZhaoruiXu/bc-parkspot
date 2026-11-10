package ca.bcparkspot.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.bcparkspot.app.data.local.AppDatabase
import ca.bcparkspot.app.data.local.entity.ParkedCarEntity
import ca.bcparkspot.app.data.local.entity.SavedLotEntity
import ca.bcparkspot.app.data.repository.BCParksDataSource
import ca.bcparkspot.app.data.repository.FirestoreParkingRepository
import ca.bcparkspot.app.data.repository.ParkingRepository
import ca.bcparkspot.app.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ca.bcparkspot.app.ui.screens.ExploreViewMode

data class ParkingUiState(
    val searchQuery: String = "",
    val selectedRegion: ParkRegion? = null,
    val selectedRecreationType: RecreationType? = null,
    val filterAvailableOnly: Boolean = false,
    val filterNoPassNeeded: Boolean = false,
    val filterFreeParkingOnly: Boolean = false,
    val filterEvCharging: Boolean = false,
    val filterDogFriendly: Boolean = false,
    val selectedLot: ParkingLot? = null,
    val activeNavigationRoute: NavigationRoute? = null,
    val isReportingDialogVisible: Boolean = false,
    val isSaveCarSheetVisible: Boolean = false,
    val isAuthDialogVisible: Boolean = false,
    val isPrivacyDialogVisible: Boolean = false,
    val isDisclaimerVisible: Boolean = false,
    val isRefreshing: Boolean = false,
    val userNotification: String? = null,
    val exploreViewMode: ExploreViewMode = ExploreViewMode.LIST,
    val selectedMapLot: ParkingLot? = null
)

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    internal val localRepository = ParkingRepository(database.parkingDao())
    private val firestoreRepository: FirestoreParkingRepository? = try {
        FirestoreParkingRepository(application)
    } catch (e: Throwable) {
        null
    }
    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Throwable) {
        null
    }

    fun updateCommunityReportsForTesting(reports: List<CommunityReport>) {
        localRepository.updateCommunityReports(reports)
    }

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    val parkingLots: StateFlow<List<ParkingLot>> = localRepository.parkingLots
    val currentUserLocation: StateFlow<UserLocation> = localRepository.currentUserLocation
    private val _deviceGpsLocation = MutableStateFlow<UserLocation?>(null)
    val deviceGpsLocation: StateFlow<UserLocation?> = _deviceGpsLocation.asStateFlow()
    val savedLots: StateFlow<List<SavedLotEntity>> = localRepository.savedLots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val parkedCar: StateFlow<ParkedCarEntity?> = localRepository.parkedCar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Merged community reports from Firestore (when authenticated) and local repository
    private val _firestoreReports = MutableStateFlow<List<CommunityReport>>(emptyList())
    val communityReports: StateFlow<List<CommunityReport>> = combine(
        localRepository.communityReports,
        _firestoreReports
    ) { local, firestore ->
        (firestore + local).distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var firestoreSyncJob: Job? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            startFirestoreSync(user.uid)
        } else {
            firestoreSyncJob?.cancel()
            _firestoreReports.value = emptyList()
        }
    }

    init {
        // Listen to Firebase Auth state if available
        auth?.addAuthStateListener(authStateListener)

        // 60-second periodic ticker: recalculates crowd consensus decay (90m stale, 180m expired)
        // and automatically refreshes selectedLot without requiring manual interaction
        viewModelScope.launch {
            while (this.isActive) {
                delay(60_000L)
                localRepository.recalculateLotConsensus()
                _uiState.update { current ->
                    val refreshedSelected = current.selectedLot?.let { sel ->
                        parkingLots.value.firstOrNull { it.id == sel.id } ?: sel
                    }
                    current.copy(selectedLot = refreshedSelected)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth?.removeAuthStateListener(authStateListener)
        firestoreSyncJob?.cancel()
    }

    private fun startFirestoreSync(userId: String) {
        val repo = firestoreRepository ?: return
        firestoreSyncJob?.cancel()
        firestoreSyncJob = viewModelScope.launch {
            // 1. Observe public live reports with distinct user tracking
            launch {
                repo.observeCommunityReports()
                    .catch { /* handled in repository */ }
                    .collect { remoteReports ->
                        val mapped = remoteReports.map { doc ->
                            val createdAt = doc.createdAt?.toDate()
                            val (reportMs, ageMin) = if (createdAt != null) {
                                val ms = createdAt.time
                                val age = ((System.currentTimeMillis() - ms) / (60 * 1000L)).coerceAtLeast(0).toInt()
                                Pair(ms, age)
                            } else {
                                // Missing timestamp treated as expired / unknown
                                Pair(0L, 999)
                            }
                            CommunityReport(
                                id = doc.id.ifBlank { "remote_${System.currentTimeMillis()}" },
                                userId = doc.userId,
                                lotId = doc.lotId,
                                reporterName = doc.reporterName,
                                minutesAgo = ageMin,
                                statusReported = try {
                                    AvailabilityStatus.valueOf(doc.status)
                                } catch (e: Exception) {
                                    AvailabilityStatus.UNKNOWN
                                },
                                comment = doc.comment,
                                spotsEstimate = doc.spotsEstimate,
                                timestampMs = reportMs,
                                isPendingSync = false
                            )
                        }
                        _firestoreReports.value = mapped
                        localRepository.updateCommunityReports(mapped)
                    }
            }

            // 2. Sync remote saved lots into local Room database for offline resilience
            launch {
                repo.observeSavedLots(userId)
                    .catch { /* handled */ }
                    .collect { remoteLots ->
                        remoteLots.forEach { rLot ->
                            database.parkingDao().insertSavedLot(
                                SavedLotEntity(
                                    lotId = rLot.lotId,
                                    parkName = rLot.parkName,
                                    lotName = rLot.lotName,
                                    region = rLot.region,
                                    userNotes = rLot.userNotes
                                )
                            )
                        }
                    }
            }

            // 3. Sync remote parked vehicle if available (preserving isExactGpsLocation!)
            launch {
                repo.observeParkedVehicle(userId)
                    .catch { /* handled */ }
                    .collect { remoteCar ->
                        if (remoteCar != null) {
                            database.parkingDao().saveParkedCar(
                                ParkedCarEntity(
                                    id = 1,
                                    parkName = remoteCar.parkName,
                                    lotName = remoteCar.lotName,
                                    latitude = remoteCar.latitude,
                                    longitude = remoteCar.longitude,
                                    stallNumber = remoteCar.stallNumber,
                                    notes = remoteCar.notes,
                                    isExactGpsLocation = remoteCar.isExactGpsLocation
                                )
                            )
                        }
                    }
            }
        }
    }

    // Derived filtered lots
    val filteredLots: StateFlow<List<ParkingLot>> = combine(
        localRepository.parkingLots,
        _uiState,
        currentUserLocation
    ) { lots, state, userLoc ->
        lots.filter { lot ->
            val matchesQuery = state.searchQuery.isBlank() ||
                    lot.parkName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.lotName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.region.displayName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.authority.displayName.contains(state.searchQuery, ignoreCase = true) ||
                    lot.proTips.contains(state.searchQuery, ignoreCase = true)

            val matchesRegion = state.selectedRegion == null || lot.region == state.selectedRegion
            val matchesType = state.selectedRecreationType == null || lot.type == state.selectedRecreationType
            val matchesAvailable = !state.filterAvailableOnly || (lot.effectiveStatus == AvailabilityStatus.AVAILABLE || lot.effectiveStatus == AvailabilityStatus.FILLING_FAST)
            val matchesPass = !state.filterNoPassNeeded || lot.getPassState() == PassRequirementStatus.NOT_REQUIRED
            val matchesFree = !state.filterFreeParkingOnly || lot.feeType == ParkingFeeType.FREE || lot.feeType == ParkingFeeType.FREE_HYDRO_PASS
            val matchesEv = !state.filterEvCharging || lot.evChargingSpots > 0
            val matchesDog = !state.filterDogFriendly || lot.isDogFriendly

            matchesQuery && matchesRegion && matchesType && matchesAvailable &&
                    matchesPass && matchesFree && matchesEv && matchesDog
        }.sortedBy { lot ->
            BCParksDataSource.calculateDistanceKm(
                userLoc.latitude, userLoc.longitude, lot.latitude, lot.longitude
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectRegion(region: ParkRegion?) {
        _uiState.update { it.copy(selectedRegion = region) }
    }

    fun selectRecreationType(type: RecreationType?) {
        _uiState.update { it.copy(selectedRecreationType = type) }
    }

    fun toggleFilterAvailableOnly() {
        _uiState.update { it.copy(filterAvailableOnly = !it.filterAvailableOnly) }
    }

    fun toggleFilterNoPassNeeded() {
        _uiState.update { it.copy(filterNoPassNeeded = !it.filterNoPassNeeded) }
    }

    fun toggleFilterFreeParking() {
        _uiState.update { it.copy(filterFreeParkingOnly = !it.filterFreeParkingOnly) }
    }

    fun toggleFilterEvCharging() {
        _uiState.update { it.copy(filterEvCharging = !it.filterEvCharging) }
    }

    fun toggleFilterDogFriendly() {
        _uiState.update { it.copy(filterDogFriendly = !it.filterDogFriendly) }
    }

    fun selectLot(lot: ParkingLot?) {
        _uiState.update { it.copy(selectedLot = lot) }
    }

    fun setExploreViewMode(mode: ExploreViewMode) {
        _uiState.update { it.copy(exploreViewMode = mode) }
    }

    fun selectMapLot(lot: ParkingLot?) {
        _uiState.update { it.copy(selectedMapLot = lot) }
    }

    fun updateDeviceGpsLocation(location: UserLocation?) {
        _deviceGpsLocation.value = location
    }

    fun startNavigation(lot: ParkingLot) {
        val route = localRepository.getRouteToLot(currentUserLocation.value, lot)
        _uiState.update {
            it.copy(
                activeNavigationRoute = route,
                selectedLot = lot,
                userNotification = "Viewing directions to ${lot.lotName}"
            )
        }
    }

    fun findNearestOpenSpot(targetParkId: String? = null) {
        val result = localRepository.findNearestOpenSpot(currentUserLocation.value, targetParkId)
        if (result != null) {
            val (nearestLot, route) = result
            _uiState.update {
                it.copy(
                    selectedLot = nearestLot,
                    activeNavigationRoute = route,
                    userNotification = "Nearest open spot: ${nearestLot.lotName}"
                )
            }
        } else {
            _uiState.update {
                it.copy(userNotification = "All lots in this area currently full or closed. Check overflow suggestions!")
            }
        }
    }

    fun clearActiveRoute() {
        _uiState.update { it.copy(activeNavigationRoute = null) }
    }

    fun refreshLiveAvailability() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            localRepository.recalculateLotConsensus()
            delay(400)
            val msg = if (_currentUser.value != null) {
                "Crowd status refreshed from Firestore"
            } else {
                "Loaded offline guide & reports. Sign in with Google to sync live reports."
            }
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    userNotification = msg
                )
            }
        }
    }

    fun setUserLocation(location: UserLocation) {
        localRepository.setUserLocation(location)
        _uiState.value.selectedLot?.let { lot ->
            if (_uiState.value.activeNavigationRoute != null) {
                val updatedRoute = localRepository.getRouteToLot(location, lot)
                _uiState.update { it.copy(activeNavigationRoute = updatedRoute) }
            }
        }
    }

    fun toggleSaveLot(lot: ParkingLot) {
        viewModelScope.launch {
            val isSaved = savedLots.value.any { it.lotId == lot.id }
            val user = _currentUser.value

            // 1. Local Room DB update (always available offline)
            localRepository.toggleSaveLot(lot, isSaved)

            // 2. Cloud Firestore sync (if signed in)
            if (user != null) {
                try {
                    if (isSaved) {
                        firestoreRepository?.deleteSavedLot(user.uid, lot.id)
                    } else {
                        firestoreRepository?.saveLot(
                            userId = user.uid,
                            lotId = lot.id,
                            parkName = lot.parkName,
                            lotName = lot.lotName,
                            region = lot.region.displayName,
                            userNotes = lot.proTips
                        )
                    }
                } catch (e: Exception) {
                    // Fail gracefully to local storage
                }
            }

            val msg = if (isSaved) "Removed from saved" else "Saved offline" + if (user != null) " & synced to cloud" else ""
            _uiState.update { it.copy(userNotification = msg) }
        }
    }

    fun showCommunityReportDialog(visible: Boolean) {
        _uiState.update { it.copy(isReportingDialogVisible = visible) }
    }

    fun showSaveCarSheet(visible: Boolean) {
        _uiState.update { it.copy(isSaveCarSheetVisible = visible) }
    }

    fun showAuthDialog(visible: Boolean) {
        _uiState.update { it.copy(isAuthDialogVisible = visible) }
    }

    fun showDisclaimer(visible: Boolean) {
        _uiState.update { it.copy(isDisclaimerVisible = visible) }
    }

    fun showPrivacyDialog(visible: Boolean) {
        _uiState.update { it.copy(isPrivacyDialogVisible = visible) }
    }

    fun submitCommunityReport(
        lotId: String,
        reporterName: String,
        status: AvailabilityStatus,
        comment: String,
        spots: Int?
    ) {
        val trimmedComment = comment.trim()
        if (trimmedComment.length < 3 || trimmedComment.length > 500) {
            _uiState.update {
                it.copy(userNotification = "Report submission failed: Comment must be between 3 and 500 characters.")
            }
            return
        }

        viewModelScope.launch {
            val user = _currentUser.value
            val lot = parkingLots.value.firstOrNull { it.id == lotId }
            val parkName = lot?.parkName ?: "BC Park"
            val uid = user?.uid ?: "anon_${System.currentTimeMillis()}"

            // Local immediate update into consensus engine (marked pending sync if user is signed in)
            val tempId = localRepository.submitLocalReport(
                lotId = lotId,
                userId = uid,
                reporterName = reporterName,
                status = status,
                comment = trimmedComment,
                spots = spots,
                isPendingSync = user != null
            )

            if (user != null) {
                try {
                    firestoreRepository?.postCommunityReport(
                        lotId = lotId,
                        parkName = parkName,
                        status = status.name,
                        comment = trimmedComment,
                        spotsEstimate = spots,
                        reporterName = reporterName.ifBlank { user.displayName ?: "BC Hiker" }
                    )
                    localRepository.markReportSynced(tempId)
                    _uiState.update {
                        it.copy(
                            isReportingDialogVisible = false,
                            userNotification = "Thank you! Community report synced to live cloud feed."
                        )
                    }
                } catch (e: Exception) {
                    // Report is preserved locally marked as pending sync
                    _uiState.update {
                        it.copy(
                            isReportingDialogVisible = false,
                            userNotification = "Cloud sync failed: ${e.localizedMessage ?: "Network error"}. Report retained locally."
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isReportingDialogVisible = false,
                        userNotification = "Saved offline report on this device. Sign in with Google to sync live reports to cloud."
                    )
                }
            }
        }
    }

    fun saveParkedCar(
        stall: String,
        notes: String,
        deviceLocation: android.location.Location? = null
    ) {
        val currentLot = _uiState.value.selectedLot ?: parkingLots.value.first()
        val user = _currentUser.value

        val isGpsFix = deviceLocation != null
        val lat = deviceLocation?.latitude ?: currentLot.latitude
        val lng = deviceLocation?.longitude ?: currentLot.longitude
        val formattedNotes = if (isGpsFix) {
            notes
        } else {
            if (notes.isBlank()) "Lot pin, not your exact car location"
            else "$notes (Lot pin, not your exact car location)"
        }

        viewModelScope.launch {
            // Local Room save
            localRepository.saveParkedCarLocation(
                parkName = currentLot.parkName,
                lotName = currentLot.lotName,
                latitude = lat,
                longitude = lng,
                stallNumber = stall,
                notes = formattedNotes,
                isExactGpsLocation = isGpsFix
            )

            // Cloud Firestore sync (preserving isExactGpsLocation!)
            if (user != null) {
                try {
                    firestoreRepository?.saveParkedVehicle(
                        userId = user.uid,
                        parkName = currentLot.parkName,
                        lotName = currentLot.lotName,
                        latitude = lat,
                        longitude = lng,
                        stallNumber = stall,
                        notes = formattedNotes,
                        isExactGpsLocation = isGpsFix
                    )
                } catch (e: Exception) {
                    // Fallback to offline
                }
            }

            val notificationMsg = if (isGpsFix) {
                "Vehicle saved with device GPS coordinates (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
            } else {
                "Vehicle saved (Using lot pin; exact GPS was unavailable)"
            }

            _uiState.update {
                it.copy(
                    isSaveCarSheetVisible = false,
                    userNotification = notificationMsg
                )
            }
        }
    }

    fun clearParkedCar() {
        val user = _currentUser.value
        viewModelScope.launch {
            localRepository.clearParkedCar()
            if (user != null) {
                try {
                    firestoreRepository?.clearParkedVehicle(user.uid)
                } catch (e: Exception) {
                    // Fallback
                }
            }
            _uiState.update { it.copy(userNotification = "Parked car pin cleared") }
        }
    }

    fun deleteUserAccountAndData(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth?.currentUser
        if (user == null) {
            onError("No authenticated user to delete.")
            return
        }

        val uid = user.uid
        viewModelScope.launch {
            try {
                // 1. Delete Firestore private data, parked vehicle, and community reports
                // Do NOT swallow Firestore error!
                firestoreRepository?.deleteUserData(uid)

                // 2. Delete Firebase Auth user profile
                try {
                    user.delete().await()
                } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    val msg = "Security re-authentication required. Please sign out, sign in with Google again, and retry account deletion."
                    _uiState.update { it.copy(userNotification = msg) }
                    onError(msg)
                    return@launch
                }

                // 3. Clear local Room cached data only after cloud deletion succeeds
                localRepository.clearAllLocalUserData()

                _currentUser.value = null
                _uiState.update {
                    it.copy(
                        isAuthDialogVisible = false,
                        userNotification = "Your account, saved lots, vehicle location, and community reports have been permanently deleted."
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                val err = "Account deletion aborted: ${e.localizedMessage ?: "Failed to delete cloud data. Local data preserved."}"
                _uiState.update { it.copy(userNotification = err) }
                onError(err)
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}
